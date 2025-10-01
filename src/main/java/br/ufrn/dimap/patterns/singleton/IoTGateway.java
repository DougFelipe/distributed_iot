package br.ufrn.dimap.patterns.singleton;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.ReceiverStrategy;
import br.ufrn.dimap.patterns.strategy.RoundRobinReceiverStrategy;
import br.ufrn.dimap.patterns.observer.IoTObserver;
import br.ufrn.dimap.patterns.replication.DataReplicationManager;
import br.ufrn.dimap.components.DataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Singleton Pattern - API Gateway IoT como ponto único de entrada
 * 
 * Características:
 * - Instância única do coordenador central
 * - Thread-safe (double-checked locking)
 * - Integração com Strategy Pattern para protocolos
 * - Observer Pattern para monitoramento
 * - Proxy Pattern para roteamento
 * 
 * Responsabilidades:
 * - Coordenação central do sistema IoT
 * - Registro e descoberta de sensores
 * - Roteamento de mensagens
 * - Monitoramento de saúde do sistema
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public class IoTGateway {
    private static final Logger logger = LoggerFactory.getLogger(IoTGateway.class);
    
    // Singleton instance com double-checked locking
    private static volatile IoTGateway instance;
    private static final Object lock = new Object();
    
    // Strategy Pattern - Protocolo de comunicação atual
    private CommunicationStrategy communicationStrategy;
    
    // Estado do gateway
    private final String gatewayId;
    private final ConcurrentHashMap<String, IoTSensor> registeredSensors;
    private final ConcurrentHashMap<String, LocalDateTime> lastHeartbeat;
    private final AtomicLong totalMessages;
    private final List<IoTObserver> observers;
    private volatile boolean active;
    
    // Version Vector global do sistema
    private final ConcurrentHashMap<String, Long> globalVersionVector;
    
    // PROXY PATTERN - Lista de Data Receivers (Instâncias B Stateful)
    private final List<DataReceiver> dataReceivers;
    private ReceiverStrategy receiverStrategy;
    
    // REPLICACAO DE DADOS - Sistema de replicação entre Data Receivers
    private DataReplicationManager replicationManager;
    
    /**
     * Construtor privado para Singleton
     */
    private IoTGateway() {
        this.gatewayId = "IOT-GATEWAY-" + System.currentTimeMillis();
        this.registeredSensors = new ConcurrentHashMap<>();
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.totalMessages = new AtomicLong(0);
        this.observers = new ArrayList<>();
        this.globalVersionVector = new ConcurrentHashMap<>();
        this.dataReceivers = new ArrayList<>();
        this.receiverStrategy = new RoundRobinReceiverStrategy();
        this.replicationManager = new DataReplicationManager(dataReceivers);
        this.active = false;
        
        logger.info("🏭 IoT Gateway Singleton criado: {} (PROXY para Data Receivers)", gatewayId);
    }
    
    /**
     * Obtém a instância única do Gateway (Thread-safe)
     */
    public static IoTGateway getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new IoTGateway();
                }
            }
        }
        return instance;
    }
    
    /**
     * Configura a estratégia de comunicação (Strategy Pattern)
     */
    public void setCommunicationStrategy(CommunicationStrategy strategy) {
        this.communicationStrategy = strategy;
        logger.info("🔧 Estratégia de comunicação configurada: {}", strategy.getProtocolName());
    }
    
    /**
     * Inicia o Gateway IoT
     */
    public synchronized void start(int port) throws Exception {
        if (active) {
            logger.warn("⚠️ Gateway IoT já está ativo");
            return;
        }
        
        if (communicationStrategy == null) {
            throw new IllegalStateException("Estratégia de comunicação não configurada");
        }
        
        // Iniciar estratégia de comunicação
        communicationStrategy.startServer(port);
        
        // Iniciar sistema de replicação de dados
        if (!dataReceivers.isEmpty()) {
            replicationManager.start();
            logger.info("🔄 Sistema de replicação iniciado para {} Data Receivers", dataReceivers.size());
        }
        
        active = true;
        
        logger.info("🚀 IoT Gateway Singleton iniciado na porta {} usando {}", 
                   port, communicationStrategy.getProtocolName());
        logger.info("📡 Gateway ID: {}", gatewayId);
    }
    
    /**
     * Para o Gateway IoT
     */
    public synchronized void stop() {
        if (!active) {
            return;
        }
        
        if (communicationStrategy != null) {
            communicationStrategy.stopServer();
        }
        
        // Parar sistema de replicação
        if (replicationManager != null) {
            replicationManager.stop();
            logger.info("🔄 Sistema de replicação parado");
        }
        
        active = false;
        logger.info("🛑 IoT Gateway Singleton parado: {}", gatewayId);
    }
    
    /**
     * Verifica se o Gateway está ativo
     */
    public boolean isRunning() {
        return active;
    }
    
    /**
     * Registra um sensor IoT no gateway
     */
    public boolean registerSensor(IoTSensor sensor, String host, int port) {
        if (!active) {
            logger.warn("⚠️ Tentativa de registro com Gateway inativo");
            return false;
        }
        
        String sensorId = sensor.getSensorId();
        registeredSensors.put(sensorId, sensor);
        lastHeartbeat.put(sensorId, LocalDateTime.now());
        
        // Inicializar Version Vector para o sensor
        globalVersionVector.put(sensorId, 0L);
        
        logger.info("✅ Sensor registrado: {} tipo: {} endereço: {}:{}", 
                   sensorId, sensor.getType(), host, port);
        
        // Notificar observers
        notifyObservers("SENSOR_REGISTERED", sensor);
        
        return true;
    }
    
    /**
     * Registra um Data Receiver no Gateway (Instância B Stateful)
     */
    public synchronized boolean registerDataReceiver(DataReceiver receiver) {
        if (dataReceivers.contains(receiver)) {
            logger.warn("⚠️ Data Receiver {} já registrado", receiver.getReceiverId());
            return false;
        }
        
        dataReceivers.add(receiver);
        
        // Adicionar ao sistema de replicação
        if (replicationManager != null) {
            replicationManager.addDataReceiver(receiver);
        }
        
        logger.info("✅ Data Receiver registrado: {} na porta {} (Total: {})", 
                   receiver.getReceiverId(), receiver.getPort(), dataReceivers.size());
        
        // Notificar observers
        notifyObservers("RECEIVER_REGISTERED", receiver);
        
        return true;
    }
    
    /**
     * Remove Data Receiver
     */
    public synchronized boolean unregisterDataReceiver(DataReceiver receiver) {
        boolean removed = dataReceivers.remove(receiver);
        if (removed) {
            // Remover do sistema de replicação
            if (replicationManager != null) {
                replicationManager.removeDataReceiver(receiver);
            }
            
            logger.info("🗑️ Data Receiver removido: {} (Total: {})", 
                       receiver.getReceiverId(), dataReceivers.size());
            notifyObservers("RECEIVER_UNREGISTERED", receiver);
        }
        return removed;
    }
    
    /**
     * Trata falha de Data Receiver com tentativa de recuperação automática
     */
    private void handleReceiverFailureWithRecovery(DataReceiver failedReceiver, IoTMessage message) {
        logger.warn("⚠️ [TOLERÂNCIA_FALHAS] Detectada falha no receptor {}", failedReceiver.getReceiverId());
        
        // Notificar Strategy sobre a falha
        receiverStrategy.handleReceiverFailure(failedReceiver, dataReceivers);
        
        // Tentar rotear para outro receptor disponível
        DataReceiver alternativeReceiver = receiverStrategy.selectReceiver(message, dataReceivers);
        
        if (alternativeReceiver != null && !alternativeReceiver.equals(failedReceiver)) {
            logger.info("🔄 [FAILOVER] Tentando rotear para receptor alternativo: {}", 
                       alternativeReceiver.getReceiverId());
            
            boolean recoverySuccess = routeMessageToDataReceiver(message, alternativeReceiver);
            
            if (recoverySuccess) {
                logger.info("✅ [FAILOVER] Mensagem {} recuperada com sucesso via {}", 
                           message.getMessageId(), alternativeReceiver.getReceiverId());
            } else {
                logger.error("❌ [FAILOVER] Falha na recuperação - Mensagem {} perdida", 
                            message.getMessageId());
            }
        } else {
            logger.error("❌ [FAILOVER] Nenhum receptor alternativo disponível - Sistema degradado");
        }
        
        // Notificar observers sobre falha do sistema
        notifyObservers("RECEIVER_FAILURE", failedReceiver);
    }
    
    /**
     * Simula falha de um Data Receiver específico (para testes)
     */
    public void simulateReceiverFailure(String receiverId) {
        DataReceiver receiver = dataReceivers.stream()
                .filter(r -> r.getReceiverId().equals(receiverId))
                .findFirst()
                .orElse(null);
        
        if (receiver != null) {
            logger.warn("💥 [TESTE_FALHAS] Simulando falha do receptor {}", receiverId);
            receiver.simulateFailure();
            
            // Remover temporariamente da lista ativa
            dataReceivers.remove(receiver);
            
            logger.warn("⚠️ [TESTE_FALHAS] Receptor {} removido da lista ativa (Total: {})", 
                       receiverId, dataReceivers.size());
        }
    }
    
    /**
     * Cria nova instância de Data Receiver para recuperação
     */
    public boolean createNewReceiverInstance(String receiverId, int port) {
        try {
            logger.info("🆕 [RECUPERAÇÃO] Criando nova instância do receptor {} na porta {}", receiverId, port);
            
            DataReceiver newReceiver = new DataReceiver(receiverId, port);
            newReceiver.start();
            
            // Registrar nova instância
            boolean registered = registerDataReceiver(newReceiver);
            
            if (registered) {
                logger.info("✅ [RECUPERAÇÃO] Nova instância {} criada e registrada com sucesso", receiverId);
                return true;
            } else {
                logger.error("❌ [RECUPERAÇÃO] Falha ao registrar nova instância {}", receiverId);
                newReceiver.stop();
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ [RECUPERAÇÃO] Erro ao criar nova instância {}: {}", receiverId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica saúde de todos os Data Receivers
     */
    public void performHealthCheck() {
        logger.debug("🏥 [HEALTH_CHECK] Verificando saúde de {} receptores", dataReceivers.size());
        
        int healthy = 0;
        int unhealthy = 0;
        
        for (DataReceiver receiver : new ArrayList<>(dataReceivers)) {
            if (receiver.isHealthy()) {
                healthy++;
            } else {
                unhealthy++;
                logger.warn("⚠️ [HEALTH_CHECK] Receptor {} não está saudável", receiver.getReceiverId());
            }
        }
        
        logger.info("🏥 [HEALTH_CHECK] Status: {} saudáveis, {} com problemas", healthy, unhealthy);
        
        if (unhealthy > 0) {
            logger.warn("⚠️ [SYSTEM_STATUS] Sistema operando em modo degradado - {}/{} receptores ativos", 
                       healthy, dataReceivers.size());
        }
    }
    
    /**
     * Remove sensor do registry
     */
    public void unregisterSensor(String sensorId) {
        IoTSensor removed = registeredSensors.remove(sensorId);
        lastHeartbeat.remove(sensorId);
        globalVersionVector.remove(sensorId);
        
        if (removed != null) {
            logger.info("🗑️ Sensor removido: {}", sensorId);
            notifyObservers("SENSOR_UNREGISTERED", removed);
        }
    }
    
    /**
     * Proxy Pattern - Roteia mensagem para sensor específico
     */
    public boolean routeMessageToSensor(String sensorId, IoTMessage message, String host, int port) {
        if (!active || communicationStrategy == null) {
            return false;
        }
        
        IoTSensor sensor = registeredSensors.get(sensorId);
        if (sensor == null) {
            logger.warn("⚠️ Tentativa de roteamento para sensor inexistente: {}", sensorId);
            return false;
        }
        
        // Atualizar Version Vector
        updateVersionVector(message);
        
        // Log do roteamento (Proxy Pattern)
        logger.debug("🔄 Proxy: Roteando mensagem para sensor {} ({}:{})", sensorId, host, port);
        
        return communicationStrategy.sendMessage(message, host, port);
    }
    
    /**
     * PROXY PATTERN - Roteia mensagem para Data Receivers (Instâncias B)
     * Gateway NÃO processa dados diretamente - apenas roteia
     * @return true se roteamento foi bem-sucedido, false caso contrário
     */
    public boolean routeToDataReceiver(IoTMessage message, String senderHost, int senderPort) {
        totalMessages.incrementAndGet();
        
        // Atualizar heartbeat do sensor
        if (message.getSenderId() != null) {
            lastHeartbeat.put(message.getSenderId(), LocalDateTime.now());
        }
        
        logger.info("🔄 [PROXY] Mensagem recebida de {}:{} - Sensor: {} - Tipo: {} - Roteando para Data Receiver...", 
                   senderHost, senderPort, message.getSensorId(), message.getType());
        
        // STRATEGY PATTERN - Selecionar Data Receiver
        DataReceiver selectedReceiver = receiverStrategy.selectReceiver(message, dataReceivers);
        
        if (selectedReceiver == null) {
            logger.error("❌ [PROXY] ERRO: Nenhum Data Receiver disponível para mensagem {}", message.getMessageId());
            return false;
        }
        
        // Rotear para o Data Receiver selecionado
        boolean success = routeMessageToDataReceiver(message, selectedReceiver);
        
        if (success) {
            logger.info("✅ [PROXY] Mensagem {} roteada para {} - Sensor: {} Valor: {:.2f}", 
                       message.getMessageId(), selectedReceiver.getReceiverId(), 
                       message.getSensorId(), message.getSensorValue());
        } else {
            logger.error("❌ [PROXY] Falha ao rotear mensagem {} para {}", 
                        message.getMessageId(), selectedReceiver.getReceiverId());
            
            // Tratar falha do receptor
            receiverStrategy.handleReceiverFailure(selectedReceiver, dataReceivers);
            
            // Notificar observers sobre roteamento
            notifyObservers("MESSAGE_ROUTED", message);
            return false;
        }
        
        // Notificar observers sobre roteamento
        notifyObservers("MESSAGE_ROUTED", message);
        return true;
    }
    
    /**
     * Roteia mensagem para Data Receiver específico - SEMPRE via UDP interno
     * Data Receivers sempre usam UDP para comunicação interna independente do protocolo de entrada
     */
    private boolean routeMessageToDataReceiver(IoTMessage message, DataReceiver receiver) {
        try {
            if (!receiver.isRunning()) {
                logger.warn("⚠️ [PROXY] Data Receiver {} não está ativo", receiver.getReceiverId());
                return false;
            }
            
            // CORREÇÃO: Usar método direto do receiver ao invés de UDP
            // Data Receivers processam mensagens diretamente
            boolean processed = receiver.processMessage(message);
            
            if (processed) {
                logger.debug("✅ [PROXY] Mensagem {} processada por {}", 
                            message.getMessageId(), receiver.getReceiverId());
                return true;
            } else {
                logger.warn("⚠️ [PROXY] Falha ao processar mensagem {} em {}", 
                           message.getMessageId(), receiver.getReceiverId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ [PROXY] Erro ao rotear para {}: {}", receiver.getReceiverId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Atualiza Version Vector global
     */
    private void updateVersionVector(IoTMessage message) {
        if (message.getVersionVector() != null) {
            // Merge dos version vectors (converter Integer para Long)
            message.getVersionVector().forEach((senderId, version) -> {
                globalVersionVector.merge(senderId, version.longValue(), Long::max);
            });
        }
    }
    
    /**
     * Observer Pattern - Adiciona observer
     */
    public void addObserver(IoTObserver observer) {
        synchronized (observers) {
            observers.add(observer);
            logger.debug("👁️ Observer adicionado: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Observer Pattern - Remove observer
     */
    public void removeObserver(IoTObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }
    
    /**
     * Notifica todos os observers
     */
    private void notifyObservers(String eventType, Object eventData) {
        synchronized (observers) {
            for (IoTObserver observer : observers) {
                try {
                    observer.onIoTEvent(eventType, eventData);
                } catch (Exception e) {
                    logger.error("❌ Erro ao notificar observer: {}", e.getMessage());
                }
            }
        }
    }
    
    // Getters para estatísticas
    public String getGatewayId() { return gatewayId; }
    public int getRegisteredSensorsCount() { return registeredSensors.size(); }
    public int getRegisteredReceiversCount() { return dataReceivers.size(); }
    public long getTotalMessages() { return totalMessages.get(); }
    public boolean isActive() { return active; }
    public ConcurrentHashMap<String, Long> getGlobalVersionVector() { return new ConcurrentHashMap<>(globalVersionVector); }
    public List<DataReceiver> getDataReceivers() { return new ArrayList<>(dataReceivers); }
    
    /**
     * Estatísticas detalhadas do Gateway (PROXY)
     */
    public String getDetailedStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("IoT Gateway Singleton Stats (PROXY PATTERN):\n");
        sb.append(String.format("  Gateway ID: %s\n", gatewayId));
        sb.append(String.format("  Active: %s\n", active));
        sb.append(String.format("  Protocol: %s\n", communicationStrategy != null ? communicationStrategy.getProtocolName() : "NONE"));
        sb.append(String.format("  Strategy: %s\n", receiverStrategy.getStrategyName()));
        sb.append(String.format("  Registered Sensors: %d\n", registeredSensors.size()));
        sb.append(String.format("  Data Receivers: %d\n", dataReceivers.size()));
        sb.append(String.format("  Total Messages: %d\n", totalMessages.get()));
        sb.append(String.format("  Observers: %d\n", observers.size()));
        sb.append(String.format("  Version Vector: %s\n", globalVersionVector.toString()));
        
        // Status dos Data Receivers
        if (!dataReceivers.isEmpty()) {
            sb.append("  Data Receivers Status:\n");
            for (DataReceiver receiver : dataReceivers) {
                sb.append(String.format("    %s: %s (Port: %d, Messages: %d)\n", 
                         receiver.getReceiverId(), 
                         receiver.isRunning() ? "ACTIVE" : "INACTIVE",
                         receiver.getPort(),
                         receiver.getTotalMessages()));
            }
        }
        
        // Status do sistema de replicação
        if (replicationManager != null && replicationManager.isActive()) {
            sb.append(String.format("  %s\n", replicationManager.getReplicationStats()));
        }
        
        return sb.toString();
    }
}