package br.ufrn.dimap.applications;

import br.ufrn.dimap.patterns.singleton.IoTGateway;
import br.ufrn.dimap.patterns.strategy.UDPCommunicationStrategy;
import br.ufrn.dimap.patterns.observer.HeartbeatMonitor;

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
    private static final int HEARTBEAT_TIMEOUT = 30; // segundos
    
    private static volatile boolean running = true;
    private static ScheduledExecutorService scheduler;
    
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
            IoTGateway gateway = IoTGateway.getInstance();
            logger.info("✅ Singleton Pattern: Gateway IoT obtido");
            
            // 2. STRATEGY PATTERN - Configurar estratégia UDP
            UDPCommunicationStrategy udpStrategy = new UDPCommunicationStrategy();
            
            // Configurar callback para processar mensagens
            udpStrategy.setMessageProcessor((message, host, port) -> {
                // PROXY PATTERN - Gateway processa e roteia mensagens
                gateway.processIncomingMessage(message, host, port);
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
            
            // 5. Aguardar inicialização
            Thread.sleep(2000);
            
            // 6. Criar sensores IoT de teste
            createTestSensors(gateway);
            
            // 7. Configurar monitoramento periódico
            setupPeriodicMonitoring(gateway, heartbeatMonitor);
            
            logger.info("✅ Sistema IoT Distribuído iniciado com sucesso!");
            logger.info("📊 Padrões GoF implementados:");
            logger.info("   🔸 Singleton: Gateway como instância única");
            logger.info("   🔸 Strategy: Protocolo UDP selecionável");
            logger.info("   🔸 Observer: Monitoramento de heartbeat");
            logger.info("   🔸 Proxy: Gateway roteia para sensores");
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
     * Cria sensores IoT de teste
     */
    private static void createTestSensors(IoTGateway gateway) throws InterruptedException {
        logger.info("🤖 Criando sensores IoT de teste...");
        
        IoTSensor.SensorType[] sensorTypes = {
            IoTSensor.SensorType.TEMPERATURE,
            IoTSensor.SensorType.HUMIDITY,
            IoTSensor.SensorType.PRESSURE,
            IoTSensor.SensorType.LIGHT,
            IoTSensor.SensorType.MOTION
        };
        
        String[] sensorNames = {
            "TEMP_SENSOR_01",
            "HUMIDITY_SENSOR_01", 
            "PRESSURE_SENSOR_01",
            "LIGHT_SENSOR_01",
            "MOTION_SENSOR_01"
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
        
        logger.info("🏭 {} sensores IoT criados e iniciados", sensorTypes.length);
    }
    
    /**
     * Configura monitoramento periódico
     */
    private static void setupPeriodicMonitoring(IoTGateway gateway, HeartbeatMonitor monitor) {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Monitor-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        
        // Estatísticas do Gateway a cada 30 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.info("📊 Status do Sistema IoT:");
                logger.info("   🔸 Sensores registrados: {}", gateway.getRegisteredSensorsCount());
                logger.info("   🔸 Mensagens processadas: {}", gateway.getTotalMessages());
                logger.info("   🔸 Gateway ativo: {}", gateway.isActive());
                logger.info("   🔸 Version Vector: {}", gateway.getGlobalVersionVector());
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
        
        // Estatísticas detalhadas a cada 60 segundos
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.info("📈 Estatísticas Detalhadas:");
                logger.info("\n{}", gateway.getDetailedStats());
                logger.info("\n{}", monitor.getMonitoringStats());
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