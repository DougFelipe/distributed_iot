package br.ufrn.dimap.applications;

import br.ufrn.dimap.patterns.singleton.IoTGateway;
import br.ufrn.dimap.components.DataReceiver;
import br.ufrn.dimap.patterns.observer.HeartbeatMonitor;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.UDPCommunicationStrategy;
import br.ufrn.dimap.communication.http.HTTPCommunicationStrategy;
import br.ufrn.dimap.communication.tcp.TCPCommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.GRPCCommunicationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 🚀 LAUNCHER MULTI-PROTOCOLO - Sistema IoT Distribuído
 * 
 * ===============================================================================
 * 📋 INSTRUÇÕES DE USO:
 * ===============================================================================
 * 
 * Terminal 1 (HTTP):
 *   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="HTTP"
 * 
 * Terminal 2 (TCP):
 *   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="TCP"
 * 
 * Terminal 3 (UDP):
 *   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="UDP"
 * 
 * Terminal 4 (GRPC):
 *   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="GRPC"
 * 
 * ===============================================================================
 * 🔧 CONFIGURAÇÃO DE PORTAS POR PROTOCOLO:
 * ===============================================================================
 * 
 * HTTP:  Gateway=8081, Receivers=9001,9002  (Isolado para testes simultâneos)
 * TCP:   Gateway=8082, Receivers=9003,9004  (Isolado para testes simultâneos)
 * UDP:   Gateway=9090, Receivers=9091,9092  (Portas originais do sistema)
 * GRPC:  Gateway=9090, Receivers=9091,9092  (Portas originais do sistema)
 * 
 * ⚠️  IMPORTANTE: UDP e GRPC usam as mesmas portas (executar separadamente)
 * ✅  HTTP e TCP podem executar simultaneamente (portas isoladas)
 * 
 * ===============================================================================
 */
public class IoTMultiProtocolLauncher {
    private static final Logger log = LoggerFactory.getLogger(IoTMultiProtocolLauncher.class);
    
    // Configuração de portas - Diferenciada apenas para HTTP e TCP
    private static final int HTTP_GATEWAY_PORT = 8081;
    private static final List<Integer> HTTP_RECEIVER_PORTS = Arrays.asList(9001, 9002);
    
    private static final int TCP_GATEWAY_PORT = 8082;
    private static final List<Integer> TCP_RECEIVER_PORTS = Arrays.asList(9003, 9004);
    
    // UDP e GRPC usam as portas originais do sistema (9090-9092)
    private static final int UDP_GATEWAY_PORT = 9090;
    private static final List<Integer> UDP_RECEIVER_PORTS = Arrays.asList(9091, 9092);
    
    private static final int GRPC_GATEWAY_PORT = 9090;
    private static final List<Integer> GRPC_RECEIVER_PORTS = Arrays.asList(9091, 9092);

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String protocol = args[0].toUpperCase();
        
        try {
            switch (protocol) {
                case "HTTP":
                    launchProtocol("HTTP", HTTP_GATEWAY_PORT, HTTP_RECEIVER_PORTS);
                    break;
                case "TCP":
                    launchProtocol("TCP", TCP_GATEWAY_PORT, TCP_RECEIVER_PORTS);
                    break;
                case "UDP":
                    launchProtocol("UDP", UDP_GATEWAY_PORT, UDP_RECEIVER_PORTS);
                    break;
                case "GRPC":
                    launchProtocol("GRPC", GRPC_GATEWAY_PORT, GRPC_RECEIVER_PORTS);
                    break;
                default:
                    log.error("❌ Protocolo inválido: {}. Use: HTTP, TCP, UDP ou GRPC", protocol);
                    printUsage();
                    return;
            }
        } catch (Exception e) {
            log.error("❌ Erro fatal no sistema {}: {}", protocol, e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void launchProtocol(String protocol, int gatewayPort, List<Integer> receiverPorts) {
        printBanner(protocol, gatewayPort, receiverPorts);
        
        // 1. Obter Gateway Singleton
        IoTGateway gateway = IoTGateway.getInstance();
        log.info("✅ Singleton Pattern: Gateway IoT obtido");
        
        // 2. Configurar protocolo específico
        configureProtocol(gateway, protocol, gatewayPort);
        
        // 3. Iniciar Gateway
        try {
            gateway.start(gatewayPort);
            log.info("✅ Gateway IoT iniciado na porta {}", gatewayPort);
        } catch (Exception e) {
            log.error("❌ Erro ao iniciar Gateway: {}", e.getMessage());
            throw new RuntimeException("Falha ao iniciar Gateway", e);
        }
        
        // 4. Criar Data Receivers com portas específicas
        createDataReceivers(gateway, receiverPorts, protocol);
        
        // 5. Configurar tolerância a falhas
        setupFaultTolerance(gateway);
        
        // 6. Imprimir resumo final
        printSystemSummary(protocol, gatewayPort, receiverPorts);
        
        // 7. Manter sistema rodando
        keepSystemRunning(protocol);
    }

    private static void configureProtocol(IoTGateway gateway, String protocol, int port) {
        log.info("🔧 Protocolo definido: {}", protocol);
        log.info("🔧 Configurando estratégia de comunicação: {}", protocol);
        
        CommunicationStrategy strategy;
        switch (protocol) {
            case "HTTP":
                strategy = new HTTPCommunicationStrategy();
                break;
            case "TCP":
                strategy = new TCPCommunicationStrategy();
                break;
            case "UDP":
                UDPCommunicationStrategy udpStrategy = new UDPCommunicationStrategy();
                
                // Configurar callback para roteamento (PROXY PATTERN)
                udpStrategy.setMessageProcessor((message, host, senderPort) -> {
                    // PROXY PATTERN - Gateway roteia mensagens para Data Receivers
                    boolean success = gateway.routeToDataReceiver(message, host, senderPort);
                    
                    // Enviar resposta UDP para JMeter (importante para zero erros)
                    if (success) {
                        udpStrategy.sendSuccessResponse(message, host, senderPort);
                    } else {
                        udpStrategy.sendErrorResponse(message, host, senderPort, "No available receivers");
                    }
                });
                
                strategy = udpStrategy;
                break;
            case "GRPC":
                GRPCCommunicationStrategy grpcStrategy = new GRPCCommunicationStrategy();
                
                // Configurar callback para roteamento (PROXY PATTERN)
                grpcStrategy.setMessageProcessor((message, host) -> {
                    // PROXY PATTERN - Gateway roteia mensagens para Data Receivers
                    boolean success = gateway.routeToDataReceiver(message, host, port);
                    log.debug("🔄 [gRPC] Mensagem roteada: {} (sucesso: {})", message.getSensorId(), success);
                });
                
                strategy = grpcStrategy;
                break;
            default:
                throw new IllegalArgumentException("Protocolo não suportado: " + protocol);
        }
        
        gateway.setCommunicationStrategy(strategy);
        log.info("✅ Estratégia {} configurada na porta {}", protocol, port);
        
        // Informações específicas do protocolo
        switch (protocol) {
            case "HTTP":
                log.info("🌐 Endpoints HTTP disponíveis:");
                log.info("   POST /sensor/data - Envio de dados de sensores");
                log.info("   GET  /sensor/status - Status do sistema");
                log.info("   GET  /health - Health check");
                break;
            case "TCP":
                log.info("🔌 Servidor TCP aguardando conexões persistentes");
                log.info("📝 Formato de mensagem: SENSOR_DATA|sensor_id|type|location|timestamp|value");
                break;
            case "UDP":
                log.info("📡 Servidor UDP aguardando datagramas");
                log.info("📝 Formato de mensagem: SENSOR_DATA|sensor_id|type|location|timestamp|value");
                break;
            case "GRPC":
                log.info("🔗 Servidor gRPC aguardando requisições");
                log.info("📝 Proto service: IoTService com métodos SendSensorData e GetStatus");
                break;
        }
        
        log.info("✅ Strategy Pattern: Protocolo {} configurado", protocol);
    }

    private static void createDataReceivers(IoTGateway gateway, List<Integer> ports, String protocol) {
        log.info("🏗️ Criando Data Receivers (Instâncias B Stateful) para {}...", protocol);
        
        for (int i = 0; i < ports.size(); i++) {
            String receiverId = String.format("DATA_RECEIVER_%s_%d", protocol, (i + 1));
            int port = ports.get(i);
            
            try {
                DataReceiver receiver = new DataReceiver(receiverId, port);
                receiver.start();
                gateway.registerDataReceiver(receiver);
                log.info("✅ Data Receiver {} iniciado na porta {}", receiverId, port);
            } catch (Exception e) {
                log.error("❌ Erro ao criar Data Receiver {} na porta {}: {}", receiverId, port, e.getMessage());
                throw new RuntimeException("Falha ao iniciar Data Receiver", e);
            }
        }
        
        log.info("✅ Data Receivers iniciados nas portas {}", ports);
        log.info("✅ Data Receivers registrados no Gateway (PROXY)");
    }

    private static void setupFaultTolerance(IoTGateway gateway) {
        log.info("🛡️ Iniciando Fault Tolerance Manager...");
        
        // Adicionar HeartbeatMonitor (Observer Pattern)
        HeartbeatMonitor heartbeatMonitor = new HeartbeatMonitor(30); // 30 segundos timeout
        gateway.addObserver(heartbeatMonitor);
        log.info("✅ Observer Pattern: HeartbeatMonitor adicionado");
        
        // Sistema está configurado para tolerância a falhas (já integrado no Gateway)
        log.info("✅ Tolerância a falhas ativada com recuperação automática");
    }

    private static void printBanner(String protocol, int gatewayPort, List<Integer> receiverPorts) {
        log.info("");
        log.info("===============================================================================");
        log.info("      SISTEMA IoT DISTRIBUÍDO - PROTOCOLO {} ", protocol);
        log.info("                   UFRN - DIMAP - Multi-Protocol");
        log.info("===============================================================================");
        log.info("🚀 Iniciando Sistema IoT Distribuído com Protocolo {}...", protocol);
        log.info("🔧 Gateway Port: {}", gatewayPort);
        log.info("🔧 Receiver Ports: {}", receiverPorts);
    }

    private static void printSystemSummary(String protocol, int gatewayPort, List<Integer> receiverPorts) {
        log.info("✅ Sistema IoT Distribuído {} iniciado com sucesso!", protocol);
        log.info("📊 Arquitetura {} Implementada:", protocol);
        log.info("   🔸 Gateway: Porta {} ({})", gatewayPort, protocol);
        log.info("   🔸 Data Receivers: Portas {} (Stateful)", receiverPorts);
        log.info("   🔸 Padrões GoF: Singleton, Strategy, Observer, Proxy");
        log.info("   🔸 Tolerância a Falhas: Recuperação automática ativa");
        log.info("🧪 PRONTO PARA TESTES JMETER {} na porta {}", protocol, gatewayPort);
    }

    private static void keepSystemRunning(String protocol) {
        log.info("🔄 Sistema {} executando. Use Ctrl+C para parar.", protocol);
        
        // Adicionar shutdown hook para limpeza
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("🛑 Encerrando sistema {}...", protocol);
            // Aqui você pode adicionar limpeza específica se necessário
        }));
        
        // Manter sistema rodando
        try {
            while (true) {
                TimeUnit.SECONDS.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("🛑 Sistema {} interrompido.", protocol);
        }
    }

    private static void printUsage() {
        System.out.println("===============================================================================");
        System.out.println("      LAUNCHER MULTI-PROTOCOLO - Sistema IoT Distribuído");
        System.out.println("===============================================================================");
        System.out.println("Uso: java IoTMultiProtocolLauncher <PROTOCOLO>");
        System.out.println("");
        System.out.println("Protocolos disponíveis:");
        System.out.println("  HTTP  - HTTP REST API (porta 8081) - Isolado");
        System.out.println("  TCP   - TCP Socket    (porta 8082) - Isolado");
        System.out.println("  UDP   - UDP Datagram  (porta 9090) - Original");
        System.out.println("  GRPC  - gRPC Service  (porta 9090) - Original");
        System.out.println("");
        System.out.println("⚠️  NOTA: UDP e GRPC usam as mesmas portas (executar separadamente)");
        System.out.println("✅  HTTP e TCP podem executar simultaneamente");
        System.out.println("");
        System.out.println("Exemplos:");
        System.out.println("  Terminal 1: mvn exec:java -Dexec.mainClass=\"br.ufrn.dimap.applications.IoTMultiProtocolLauncher\" -Dexec.args=\"HTTP\"");
        System.out.println("  Terminal 2: mvn exec:java -Dexec.mainClass=\"br.ufrn.dimap.applications.IoTMultiProtocolLauncher\" -Dexec.args=\"TCP\"");
        System.out.println("===============================================================================");
    }
}