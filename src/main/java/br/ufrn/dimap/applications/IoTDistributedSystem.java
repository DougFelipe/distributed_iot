package br.ufrn.dimap.applications;

import br.ufrn.dimap.patterns.singleton.IoTGateway;
import br.ufrn.dimap.patterns.strategy.UDPCommunicationStrategy;
import br.ufrn.dimap.patterns.observer.HeartbeatMonitor;
import br.ufrn.dimap.patterns.fault_tolerance.FaultToleranceManager;
import br.ufrn.dimap.components.DataReceiver;

import br.ufrn.dimap.core.IoTSensor;
import br.ufrn.dimap.communication.native_udp.NativeUDPIoTClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sistema IoT Distribuído com Padrões GoF
 * 
 * Integra todos os padrões obrigatórios:
 * - Singleton: IoTGateway como coordenador único
 * - Strategy: UDPCommunicationStrategy para protocolos
 * - Observer: HeartbeatMonitor para monitoramento
 * - Proxy: Gateway roteia mensagens para sensores
 * 
 * Arquitetura:
 * - 1 Gateway IoT (Singleton + Proxy)
 * - 5 Sensores distribuídos
 * - Estratégia UDP nativa
 * - Monitor de heartbeat
 * - Version Vector para ordenação causal
 * 
 * @author UFRN-DIMAP
 * @version 1.0 - Sprint 2
 */
public class IoTDistributedSystem {
    private static final Logger logger = LoggerFactory.getLogger(IoTDistributedSystem.class);
    
    private static final String BANNER = 
            "\n===============================================================================\n" +
            "      SISTEMA IoT DISTRIBUÍDO - PADRÕES GoF + VERSION VECTOR\n" +
            "                   UFRN - DIMAP - Sprint 2\n" +
            "===============================================================================";
    
    private static final int GATEWAY_PORT = 9090;
    private static final int DATA_RECEIVER_1_PORT = 9091;
    private static final int DATA_RECEIVER_2_PORT = 9092;
    private static final int HEARTBEAT_TIMEOUT = 5; // segundos (para apresentação)
    
    private static volatile boolean running = true;
    private static ScheduledExecutorService scheduler;
    private static IoTGateway gateway;
    private static DataReceiver receiver1;
    private static DataReceiver receiver2;
    
    public static void main(String[] args) {
        // Configurar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Recebido sinal de shutdown. Encerrando graciosamente...");
            shutdown();
        }));
        
        logger.info(BANNER);
        logger.info("🚀 Iniciando Sistema IoT Distribuído com Padrões GoF...");
        
        try {
            // 1. SINGLETON PATTERN - Obter instância única do Gateway
            gateway = IoTGateway.getInstance();
            logger.info("✅ Singleton Pattern: Gateway IoT obtido");
            
            // 2. STRATEGY PATTERN - Configurar estratégia UDP
            UDPCommunicationStrategy udpStrategy = new UDPCommunicationStrategy();
            
            // Configurar callback para roteamento (PROXY PATTERN)
            udpStrategy.setMessageProcessor((message, host, port) -> {
                // PROXY PATTERN - Gateway roteia mensagens para Data Receivers
                boolean success = gateway.routeToDataReceiver(message, host, port);
                
                // Enviar resposta UDP para JMeter (importante para zero erros)
                if (success) {
                    udpStrategy.sendSuccessResponse(message, host, port);
                } else {
                    udpStrategy.sendErrorResponse(message, host, port, "No available receivers");
                }
            });
            
            gateway.setCommunicationStrategy(udpStrategy);
            logger.info("✅ Strategy Pattern: UDP configurado como protocolo");
            
            // 3. OBSERVER PATTERN - Configurar monitor de heartbeat
            HeartbeatMonitor heartbeatMonitor = new HeartbeatMonitor(HEARTBEAT_TIMEOUT);
            gateway.addObserver(heartbeatMonitor);
            logger.info("✅ Observer Pattern: HeartbeatMonitor adicionado");
            
            // 4. Iniciar o Gateway (Singleton + Strategy)
            gateway.start(GATEWAY_PORT);
            logger.info("✅ Gateway IoT iniciado na porta {}", GATEWAY_PORT);
            
            // 5. INSTÂNCIAS B - Criar e iniciar Data Receivers (Stateful)
            logger.info("🏗️ Criando Data Receivers (Instâncias B Stateful)...");
            receiver1 = new DataReceiver("DATA_RECEIVER_1", DATA_RECEIVER_1_PORT);
            receiver2 = new DataReceiver("DATA_RECEIVER_2", DATA_RECEIVER_2_PORT);
            
            // Iniciar Data Receivers
            receiver1.start();
            receiver2.start();
            logger.info("✅ Data Receivers iniciados nas portas {} e {}", DATA_RECEIVER_1_PORT, DATA_RECEIVER_2_PORT);
            
            // Registrar Data Receivers no Gateway (Proxy Pattern)
            gateway.registerDataReceiver(receiver1);
            gateway.registerDataReceiver(receiver2);
            logger.info("✅ Data Receivers registrados no Gateway (PROXY)");
            
            // 6. Aguardar inicialização
            Thread.sleep(2000);
            
            // 7. TOLERÂNCIA A FALHAS - Iniciar gerenciador de falhas
            logger.info("🛡️ Iniciando Fault Tolerance Manager...");
            FaultToleranceManager faultManager = new FaultToleranceManager(gateway);
            faultManager.start();
            logger.info("✅ Tolerância a falhas ativada com recuperação automática");
            
            // 8. Configurar monitoramento periódico
            setupPeriodicMonitoring(gateway, heartbeatMonitor, receiver1, receiver2, faultManager);
            
            // NOTA: Sensores serão criados dinamicamente via JMeter
            // Cada thread do JMeter = 1 sensor IoT simulado
            
            logger.info("✅ Sistema IoT Distribuído iniciado com sucesso!");
            logger.info("📊 Arquitetura Final Implementada:");
            logger.info("   🔸 Instâncias A: Sensores IoT (Stateless) - TEMPERATURA e UMIDADE");
            logger.info("   🔸 Instâncias B: Data Receivers (Stateful) - 2+ receptores com persistência");
            logger.info("   🔸 Gateway: Proxy roteando mensagens para Data Receivers");
            logger.info("   � Tolerância a Falhas: Recuperação automática e monitoramento");
            logger.info("�📊 Padrões GoF implementados:");
            logger.info("   🔸 Singleton: Gateway como proxy único");
            logger.info("   🔸 Strategy: Seleção Round Robin de Data Receivers");
            logger.info("   🔸 Observer: Monitoramento de heartbeat");
            logger.info("   🔸 Proxy: Gateway roteia para Data Receivers");
            logger.info("🛡️ Recursos de Tolerância a Falhas:");
            logger.info("   🔸 Health Check automático a cada 5s");
            logger.info("   🔸 Recuperação automática de instâncias falhas");
            logger.info("   🔸 Criação automática de backup receivers");
            logger.info("   🔸 Balanceamento de carga Round Robin");
            logger.info("🧪 PRONTO PARA TESTES JMETER:");
            logger.info("   🔸 Zero erros em operação normal");
            logger.info("   🔸 Aumento de erros ao desligar instâncias");
            logger.info("   🔸 Diminuição de erros na recuperação");
            logger.info("🔄 Sistema executando. Use Ctrl+C para parar.");
            
            // Loop principal
            while (running) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro fatal no sistema: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Cria sensores IoT de teste (DEPRECATED - Sensores criados via JMeter)
     * Mantido para referência, mas não usado no sistema reativo
     */
    @Deprecated
    private static void createTestSensors(IoTGateway gateway) throws InterruptedException {
        logger.info("🤖 Criando sensores IoT de teste...");
        
        IoTSensor.SensorType[] sensorTypes = {
            IoTSensor.SensorType.TEMPERATURE,
            IoTSensor.SensorType.HUMIDITY
        };
        
        String[] sensorNames = {
            "TEMP_SENSOR_01",
            "HUMIDITY_SENSOR_01"
        };
        
        for (int i = 0; i < sensorTypes.length; i++) {
            String sensorId = sensorNames[i];
            String nodeId = "NODE-" + sensorTypes[i].name() + "-" + String.format("%02d", i + 1);
            String location = "Lab-" + (char)('A' + i);
            IoTSensor sensor = new IoTSensor(sensorId, nodeId, sensorTypes[i], location);
            
            // Registrar sensor no gateway (Proxy Pattern)
            gateway.registerSensor(sensor, "127.0.0.1", GATEWAY_PORT + i + 1);
            
            // Criar cliente UDP para o sensor
            NativeUDPIoTClient client;
            try {
                client = new NativeUDPIoTClient(
                    sensorId, sensorTypes[i], location, "localhost", GATEWAY_PORT
                );
            } catch (java.net.UnknownHostException e) {
                logger.error("❌ Erro ao criar cliente UDP para {}: {}", sensorId, e.getMessage());
                continue;
            }
            
            // Iniciar cliente em thread separada
            Thread sensorThread = new Thread(() -> {
                try {
                    client.start();
                } catch (Exception e) {
                    logger.error("❌ Erro no sensor {}: {}", sensorId, e.getMessage());
                }
            }, "Sensor-" + sensorId);
            
            sensorThread.setDaemon(true);
            sensorThread.start();
            
            Thread.sleep(1000); // Intervalo entre sensores
        }
        
        logger.info("🏭 {} sensores IoT criados e iniciados (TEMPERATURA + UMIDADE)", sensorTypes.length);
    }
    
    /**
     * Configura monitoramento periódico (Gateway + Data Receivers + Fault Tolerance)
     */
    private static void setupPeriodicMonitoring(IoTGateway gateway, HeartbeatMonitor monitor, 
                                              DataReceiver receiver1, DataReceiver receiver2,
                                              FaultToleranceManager faultManager) {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Monitor-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        
        // Estatísticas do Sistema a cada 30 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.info("📊 Status do Sistema IoT Distribuído:");
                logger.info("   🔸 Gateway (PROXY): Sensores={}, Receivers={}, Msgs={}", 
                           gateway.getRegisteredSensorsCount(), gateway.getRegisteredReceiversCount(), gateway.getTotalMessages());
                logger.info("   🔸 Data Receiver 1: Msgs={}, Sensores={}, Conflitos={}", 
                           receiver1.getTotalMessages(), receiver1.getSensorCount(), receiver1.getConflictsResolved());
                logger.info("   🔸 Data Receiver 2: Msgs={}, Sensores={}, Conflitos={}", 
                           receiver2.getTotalMessages(), receiver2.getSensorCount(), receiver2.getConflictsResolved());
                logger.info("   🔸 Version Vector Global: {}", gateway.getGlobalVersionVector());
            } catch (Exception e) {
                logger.error("❌ Erro ao coletar estatísticas: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Check de timeout de heartbeat a cada 15 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                monitor.checkTimeouts();
                logger.debug("💓 Verificação de heartbeat executada");
            } catch (Exception e) {
                logger.error("❌ Erro na verificação de heartbeat: {}", e.getMessage());
            }
        }, 15, 15, TimeUnit.SECONDS);
        
        // Health check dos Data Receivers a cada 20 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                gateway.performHealthCheck();
            } catch (Exception e) {
                logger.error("❌ Erro no health check: {}", e.getMessage());
            }
        }, 20, 20, TimeUnit.SECONDS);
        
        // Sistema de Tolerância a Falhas com estatísticas a cada 30 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (faultManager.isActive()) {
                    logger.info("🛡️ Tolerância a Falhas: Ativo - Backups disponíveis: {}", 
                              faultManager.getBackupConfigsAvailable());
                }
            } catch (Exception e) {
                logger.error("❌ Erro no sistema de tolerância a falhas: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Estatísticas detalhadas a cada 60 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.info("📈 Estatísticas Detalhadas do Sistema:");
                logger.info("\n{}", gateway.getDetailedStats());
                logger.info("\n{}", monitor.getMonitoringStats());
                logger.info("\n{}", receiver1.getDatabaseStatus());
                logger.info("\n{}", receiver2.getDatabaseStatus());
            } catch (Exception e) {
                logger.error("❌ Erro ao coletar estatísticas detalhadas: {}", e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Shutdown gracioso do sistema
     */
    private static void shutdown() {
        logger.info("🔄 Iniciando processo de shutdown...");
        running = false;
        
        // Parar scheduler
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
            logger.info("✅ Scheduler encerrado");
        }
        
        // Parar Gateway (Singleton)
        try {
            IoTGateway gateway = IoTGateway.getInstance();
            gateway.stop();
            logger.info("✅ Gateway IoT encerrado");
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao encerrar Gateway: {}", e.getMessage());
        }
        
        logger.info("🏁 Sistema IoT Distribuído encerrado com sucesso!");
        logger.info("📊 Sprint 2 - Padrões GoF implementados e validados!");
    }
}