package br.ufrn.dimap.patterns.fault_tolerance;

import br.ufrn.dimap.components.DataReceiver;
import br.ufrn.dimap.patterns.singleton.IoTGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gerenciador de Tolerância a Falhas com Recuperação Automática
 * 
 * Responsabilidades:
 * - Monitoramento contínuo de Data Receivers
 * - Detecção automática de falhas
 * - Recuperação automática de instâncias
 * - Rebalanceamento de carga
 * 
 * Para apresentação JMeter:
 * - Zero erros em operação normal
 * - Aumento de erros quando instâncias falham
 * - Diminuição de erros quando instâncias se recuperam
 * 
 * @author UFRN-DIMAP
 * @version 1.0 - Tolerância a Falhas Real
 */
public class FaultToleranceManager {
    private static final Logger logger = LoggerFactory.getLogger(FaultToleranceManager.class);
    
    private final IoTGateway gateway;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean active = new AtomicBoolean(false);
    
    // Configurações de recuperação
    private static final int HEALTH_CHECK_INTERVAL = 5; // segundos
    private static final int RECOVERY_DELAY = 10; // segundos
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    
    // Backup receivers para recuperação automática
    private final ConcurrentLinkedQueue<DataReceiverConfig> backupConfigs;
    
    public static class DataReceiverConfig {
        public final String receiverId;
        public final int port;
        public final int maxRecoveryAttempts;
        public int currentAttempts = 0;
        
        public DataReceiverConfig(String receiverId, int port) {
            this.receiverId = receiverId;
            this.port = port;
            this.maxRecoveryAttempts = MAX_RECOVERY_ATTEMPTS;
        }
    }
    
    public FaultToleranceManager(IoTGateway gateway) {
        this.gateway = gateway;
        this.scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "FaultTolerance-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        this.backupConfigs = new ConcurrentLinkedQueue<>();
        
        // Configurar backup receivers para recuperação automática
        backupConfigs.offer(new DataReceiverConfig("DATA_RECEIVER_BACKUP_1", 9093));
        backupConfigs.offer(new DataReceiverConfig("DATA_RECEIVER_BACKUP_2", 9094));
        backupConfigs.offer(new DataReceiverConfig("DATA_RECEIVER_BACKUP_3", 9095));
        
        logger.info("🛡️ Fault Tolerance Manager criado com {} backup configs", backupConfigs.size());
    }
    
    /**
     * Inicia monitoramento de tolerância a falhas
     */
    public void start() {
        if (active.get()) {
            logger.warn("⚠️ Fault Tolerance Manager já está ativo");
            return;
        }
        
        active.set(true);
        
        // Health check periódico de Data Receivers
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                logger.error("❌ Erro no health check: {}", e.getMessage());
            }
        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
        
        // Monitoramento de carga e balanceamento
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                monitorLoadBalancing();
            } catch (Exception e) {
                logger.error("❌ Erro no monitoramento de carga: {}", e.getMessage());
            }
        }, 15, 15, TimeUnit.SECONDS);
        
        logger.info("🛡️ Fault Tolerance Manager iniciado");
    }
    
    /**
     * Health check de todos os Data Receivers
     */
    private void performHealthCheck() {
        var receivers = gateway.getDataReceivers();
        
        for (DataReceiver receiver : receivers) {
            if (!receiver.isRunning()) {
                logger.warn("⚠️ [FAULT_TOLERANCE] Data Receiver {} inativo - Iniciando recuperação", 
                           receiver.getReceiverId());
                
                scheduleRecovery(receiver);
            }
        }
        
        // Verificar se há receivers suficientes
        long activeReceivers = receivers.stream().filter(DataReceiver::isRunning).count();
        
        if (activeReceivers < 2) {
            logger.warn("⚠️ [FAULT_TOLERANCE] Apenas {} receivers ativos - Criando backup", activeReceivers);
            createBackupReceiver();
        }
        
        logger.debug("🛡️ Health check executado: {}/{} receivers ativos", 
                    activeReceivers, receivers.size());
    }
    
    /**
     * Programa recuperação de um Data Receiver
     */
    private void scheduleRecovery(DataReceiver failedReceiver) {
        scheduler.schedule(() -> {
            try {
                logger.info("🔄 [RECOVERY] Tentando recuperar {}", failedReceiver.getReceiverId());
                
                // Tentar reiniciar o receiver existente
                if (attemptRestartReceiver(failedReceiver)) {
                    logger.info("✅ [RECOVERY] {} recuperado com sucesso", failedReceiver.getReceiverId());
                } else {
                    logger.warn("⚠️ [RECOVERY] Falha na recuperação de {} - Criando substituto", 
                               failedReceiver.getReceiverId());
                    
                    // Remover receiver falho e criar substituto
                    gateway.unregisterDataReceiver(failedReceiver);
                    createReplacementReceiver(failedReceiver);
                }
                
            } catch (Exception e) {
                logger.error("❌ [RECOVERY] Erro na recuperação: {}", e.getMessage());
            }
        }, RECOVERY_DELAY, TimeUnit.SECONDS);
    }
    
    /**
     * Tenta reiniciar um Data Receiver existente
     */
    private boolean attemptRestartReceiver(DataReceiver receiver) {
        try {
            if (!receiver.isRunning()) {
                receiver.start();
                Thread.sleep(2000); // Aguardar inicialização
                
                return receiver.isRunning();
            }
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Erro ao reiniciar receiver {}: {}", receiver.getReceiverId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Cria receiver substituto para um que falhou
     */
    private void createReplacementReceiver(DataReceiver failedReceiver) {
        DataReceiverConfig config = backupConfigs.poll();
        
        if (config == null) {
            logger.error("❌ [RECOVERY] Sem configurações de backup disponíveis");
            return;
        }
        
        config.currentAttempts++;
        
        if (config.currentAttempts > config.maxRecoveryAttempts) {
            logger.error("❌ [RECOVERY] Máximo de tentativas excedido para {}", config.receiverId);
            return;
        }
        
        try {
            DataReceiver newReceiver = new DataReceiver(config.receiverId, config.port);
            newReceiver.start();
            
            // Registrar no gateway
            if (gateway.registerDataReceiver(newReceiver)) {
                logger.info("✅ [RECOVERY] Receiver substituto {} criado na porta {}", 
                           config.receiverId, config.port);
            } else {
                logger.error("❌ [RECOVERY] Falha ao registrar receiver substituto {}", config.receiverId);
                newReceiver.stop();
            }
            
        } catch (SocketException e) {
            logger.error("❌ [RECOVERY] Erro ao criar receiver substituto {}: {}", 
                        config.receiverId, e.getMessage());
            
            // Retornar config para nova tentativa
            backupConfigs.offer(config);
        }
    }
    
    /**
     * Cria receiver de backup quando há poucos ativos
     */
    private void createBackupReceiver() {
        DataReceiverConfig config = backupConfigs.poll();
        
        if (config == null) {
            logger.warn("⚠️ [BACKUP] Sem configurações de backup disponíveis");
            return;
        }
        
        try {
            DataReceiver backupReceiver = new DataReceiver(config.receiverId, config.port);
            backupReceiver.start();
            
            if (gateway.registerDataReceiver(backupReceiver)) {
                logger.info("✅ [BACKUP] Backup receiver {} criado na porta {}", 
                           config.receiverId, config.port);
            } else {
                logger.error("❌ [BACKUP] Falha ao registrar backup receiver {}", config.receiverId);
                backupReceiver.stop();
                backupConfigs.offer(config); // Retornar para uso futuro
            }
            
        } catch (SocketException e) {
            logger.error("❌ [BACKUP] Erro ao criar backup receiver: {}", e.getMessage());
            backupConfigs.offer(config); // Retornar para uso futuro
        }
    }
    
    /**
     * Monitora balanceamento de carga
     */
    private void monitorLoadBalancing() {
        var receivers = gateway.getDataReceivers();
        
        if (receivers.isEmpty()) {
            logger.error("❌ [LOAD_BALANCE] Nenhum Data Receiver disponível!");
            return;
        }
        
        // Calcular estatísticas de carga
        long totalMessages = receivers.stream().mapToLong(DataReceiver::getTotalMessages).sum();
        double avgMessages = totalMessages / (double) receivers.size();
        
        logger.debug("📊 [LOAD_BALANCE] Média de mensagens por receiver: {:.1f}", avgMessages);
        
        // Detectar desbalanceamento significativo
        boolean isUnbalanced = receivers.stream()
            .anyMatch(r -> Math.abs(r.getTotalMessages() - avgMessages) > avgMessages * 0.5);
        
        if (isUnbalanced) {
            logger.warn("⚠️ [LOAD_BALANCE] Desbalanceamento detectado - Rebalanceando...");
            // Estratégia Round Robin já cuida do rebalanceamento automático
        }
    }
    
    /**
     * Para o gerenciador de tolerância a falhas
     */
    public void stop() {
        logger.info("🛑 Parando Fault Tolerance Manager...");
        active.set(false);
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("✅ Fault Tolerance Manager parado");
    }
    
    // Getters para monitoramento
    public boolean isActive() { return active.get(); }
    public int getBackupConfigsAvailable() { return backupConfigs.size(); }
    
    /**
     * Força criação de backup receiver (para testes de apresentação)
     */
    public void forceCreateBackup() {
        logger.info("🧪 [TEST] Criação forçada de backup receiver");
        createBackupReceiver();
    }
    
    /**
     * Simula falha de receiver (para testes de apresentação)
     */
    public void simulateReceiverFailure() {
        var receivers = gateway.getDataReceivers();
        if (!receivers.isEmpty()) {
            DataReceiver receiver = receivers.get(0);
            logger.info("🧪 [TEST] Simulando falha do receiver {}", receiver.getReceiverId());
            receiver.stop();
        }
    }
}