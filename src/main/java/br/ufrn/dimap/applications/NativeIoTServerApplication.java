package br.ufrn.dimap.applications;

import br.ufrn.dimap.communication.native_udp.NativeUDPIoTServer;
import br.ufrn.dimap.communication.native_udp.NativeUDPIoTClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aplicação principal com servidor UDP nativo para sensores IoT
 * Mantém servidor HTTP para testes JMeter + UDP nativo para produção
 * 
 * Implementação baseada nos exemplos UDP nativos com:
 * - Version Vector para ordenação causal
 * - Padrões GoF (Observer/Strategy)
 * - Serialização nativa Java
 * - ConcurrentHashMap para thread-safety
 * - Logs profissionais
 */
public class NativeIoTServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(NativeIoTServerApplication.class);
    
    private static final String BANNER = 
            "===============================================================================\n" +
            "          SISTEMA IoT NATIVO UDP - SENSORES COM VERSION VECTOR\n" +
            "                   UFRN - DIMAP - Sistemas Distribuídos\n" +
            "===============================================================================";
    
    private static final int UDP_PORT = 9090;
    
    private static volatile boolean running = true;
    private static NativeUDPIoTServer udpServer;
    private static ScheduledExecutorService scheduler;
    
    public static void main(String[] args) {
        // Configurar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Recebido sinal de shutdown. Encerrando graciosamente...");
            shutdown();
        }));
        
        logger.info("\n{}", BANNER);
        logger.info("🚀 Iniciando Sistema IoT Nativo...");
        
        try {
            // Inicializar servidor UDP nativo para IoT
            udpServer = new NativeUDPIoTServer(UDP_PORT);
            Thread udpThread = new Thread(() -> {
                try {
                    udpServer.start();
                } catch (Exception e) {
                    logger.error("❌ Erro no servidor UDP IoT: {}", e.getMessage(), e);
                }
            });
            udpThread.setDaemon(true);
            udpThread.start();
            
            // Aguardar servidor UDP inicializar
            Thread.sleep(2000);
            
            // Aguardar servidores iniciarem
            Thread.sleep(3000);
            
            // Inicializar clientes de teste (sensores simulados)
            logger.info("🤖 Criando sensores IoT de teste...");
            NativeUDPIoTClient.createTestClients("localhost", UDP_PORT, 5);
            
            // Configurar scheduler para monitoramento
            scheduler = Executors.newScheduledThreadPool(2);
            
            // Status do sistema a cada 45 segundos
            scheduler.scheduleAtFixedRate(() -> {
                if (running) {
                    logSystemStatus();
                }
            }, 30, 45, TimeUnit.SECONDS);
            
            // Estatísticas detalhadas a cada 2 minutos
            scheduler.scheduleAtFixedRate(() -> {
                if (running) {
                    logDetailedStatistics();
                }
            }, 60, 120, TimeUnit.SECONDS);
            
            logger.info("✅ Sistema IoT nativo iniciado com sucesso!");
            logger.info("📡 Servidor UDP IoT: localhost:{} (Implementação nativa)", UDP_PORT);
            logger.info("🔧 Protocolos suportados:");
            logger.info("   🔸 UDP nativo com serialização Java");
            logger.info("   🔸 Version Vector para ordenação causal");
            logger.info("   🔸 ConcurrentHashMap para thread-safety");
            logger.info("🤖 Sensores IoT simulados ativos");
            logger.info("🔄 Sistema executando em modo produção. Use Ctrl+C para parar.");
            
            // Loop principal
            while (running) {
                Thread.sleep(5000);
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro fatal no sistema: {}", e.getMessage(), e);
        } finally {
            shutdown();
        }
    }
    
    private static void logSystemStatus() {
        try {
            int registeredSensors = udpServer != null ? udpServer.getRegisteredSensorsCount() : 0;
            long totalMessages = udpServer != null ? udpServer.getTotalMessages() : 0;
            long errors = udpServer != null ? udpServer.getErrorCount() : 0;
            
            logger.info("📊 Status do Sistema IoT:");
            logger.info("   🔸 Sensores registrados: {}", registeredSensors);
            logger.info("   🔸 Mensagens processadas: {}", totalMessages);
            logger.info("   🔸 Threads ativas: {}", Thread.activeCount());
            logger.info("   🔸 Memória utilizada: {}MB", getMemoryUsageMB());
            logger.info("   🔸 Taxa de erro: {:.2f}%", 
                       totalMessages > 0 ? (errors * 100.0 / totalMessages) : 0.0);
            
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao obter status: {}", e.getMessage());
        }
    }
    
    private static void logDetailedStatistics() {
        try {
            if (udpServer != null) {
                var sensors = udpServer.getRegisteredSensors();
                long activeSensors = sensors.values().stream()
                    .mapToLong(sensor -> sensor.isHealthy() ? 1 : 0)
                    .sum();
                
                logger.info("📈 Estatísticas Detalhadas:");
                logger.info("   🔸 Total de sensores: {}", sensors.size());
                logger.info("   🔸 Sensores ativos: {}", activeSensors);
                logger.info("   🔸 Sensores inativos: {}", sensors.size() - activeSensors);
                logger.info("   🔸 Tipos de sensores:");
                
                sensors.values().forEach(sensor -> {
                    logger.info("      • {}: {} = {:.2f} {} ({})",
                               sensor.getSensorId(),
                               sensor.getType(),
                               sensor.getCurrentValue(),
                               sensor.getType().getUnit(),
                               sensor.getStatus());
                });
                
                logger.info("   🔸 Servidor UDP: {} mensagens processadas", 
                           udpServer.getTotalMessages());
                logger.info("   🔸 Sistema operacional há {}min", getUptimeMinutes());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao obter estatísticas: {}", e.getMessage());
        }
    }
    
    private static void shutdown() {
        logger.info("🔄 Iniciando processo de shutdown...");
        running = false;
        
        // Parar scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                logger.info("✅ Scheduler encerrado");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Parar servidor UDP IoT
        if (udpServer != null) {
            try {
                udpServer.stop();
                logger.info("✅ Servidor UDP IoT encerrado");
            } catch (Exception e) {
                logger.warn("⚠️ Erro ao encerrar servidor UDP: {}", e.getMessage());
            }
        }
        

        
        logger.info("🏁 Sistema IoT nativo encerrado com sucesso!");
        
        // Estatísticas finais
        if (udpServer != null) {
            logger.info("📊 Estatísticas finais:");
            logger.info("   🔸 Total de mensagens: {}", udpServer.getTotalMessages());
            logger.info("   🔸 Total de erros: {}", udpServer.getErrorCount());
            logger.info("   🔸 Uptime: {}min", getUptimeMinutes());
        }
    }
    
    private static long getUptimeMinutes() {
        return (System.currentTimeMillis() - startTime) / (1000 * 60);
    }
    
    private static long getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    private static final long startTime = System.currentTimeMillis();
}