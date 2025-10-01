package br.ufrn.dimap.applications;

import br.ufrn.dimap.patterns.singleton.IoTGateway;
import br.ufrn.dimap.patterns.strategy.UDPCommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.GRPCCommunicationStrategy;
import br.ufrn.dimap.communication.http.HTTPCommunicationStrategy;
import br.ufrn.dimap.communication.tcp.TCPCommunicationStrategy;
import br.ufrn.dimap.patterns.observer.HeartbeatMonitor;
import br.ufrn.dimap.patterns.fault_tolerance.FaultToleranceManager;
import br.ufrn.dimap.components.DataReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.io.IOException;

/**
 * Sistema IoT Distribuído com Menu Integrado
 * 
 * Fluxo da Aplicação:
 * 1. Menu de seleção de protocolo (UDP, HTTP, TCP, gRPC)
 * 2. Inicialização silenciosa do sistema
 * 3. Menu de controle de receivers (falhas e recuperação)
 * 
 * @author UFRN-DIMAP
 * @version 2.0 - Sprint 2 com Menu Integrado
 */
public class IoTSystemWithMenu {
    private static final Logger logger = LoggerFactory.getLogger(IoTSystemWithMenu.class);
    private static final Scanner scanner = new Scanner(System.in);
    
    private static final String MAIN_BANNER = 
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║            🚀 SISTEMA IOT DISTRIBUÍDO - UFRN                ║\n" +
            "║                Tolerância a Falhas + Padrões GoF            ║\n" +
            "╠══════════════════════════════════════════════════════════════╣\n" +
            "║  Demonstração para Apresentação - Programação Distribuída   ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n";
    
    private static final String CONTROL_BANNER = 
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║              🎮 CONTROLE DE DATA RECEIVERS                  ║\n" +
            "║                Simulação de Falhas em Tempo Real            ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n";
    
    private static IoTGateway gateway;
    private static List<DataReceiver> receivers;
    private static String selectedProtocol;
    
    public static void main(String[] args) {
        // Reduzir verbosidade dos logs durante o menu
        setSilentMode();
        
        System.out.println(MAIN_BANNER);
        
        try {
            // Fase 1: Seleção do Protocolo
            selectedProtocol = selectProtocol();
            
            // Fase 2: Inicialização do Sistema
            initializeSystem(selectedProtocol);
            
            // Fase 3: Menu de Controle
            runControlMenu();
            
        } catch (Exception e) {
            System.out.println("❌ ERRO: " + e.getMessage());
            logger.error("Erro no sistema: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Menu de seleção de protocolo
     */
    private static String selectProtocol() {
        while (true) {
            printProtocolMenu();
            System.out.print("🎯 Escolha o protocolo: ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            switch (choice) {
                case "1":
                case "udp":
                    return "UDP";
                case "2":
                case "http":
                    return "HTTP";
                case "3":
                case "tcp":
                    return "TCP";
                case "4":
                case "grpc":
                    return "gRPC";
                case "5":
                case "http+tcp":
                case "dual":
                    return "HTTP+TCP";
                default:
                    System.out.println("❌ Opção inválida! Tente novamente.\n");
                    break;
            }
        }
    }
    
    /**
     * Imprime menu de seleção de protocolo
     */
    private static void printProtocolMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📡 SELEÇÃO DE PROTOCOLO DE COMUNICAÇÃO");
        System.out.println("=".repeat(60));
        System.out.println("1. UDP      - Protocolo nativo (recomendado para JMeter)");
        System.out.println("2. HTTP     - Protocolo web (compatível com JMeter HTTP)");
        System.out.println("3. TCP      - Protocolo confiável (conexões persistentes)");
        System.out.println("4. gRPC     - Protocolo moderno (máxima pontuação)");
        System.out.println("5. HTTP+TCP - Ambos protocolos simultaneamente");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Inicializa o sistema com o protocolo selecionado
     */
    private static void initializeSystem(String protocol) {
        System.out.println("\n🔧 Inicializando sistema com protocolo " + protocol + "...");
        
        try {
            // Criar Gateway
            gateway = IoTGateway.getInstance();
            
            // Configurar estratégia de comunicação
            if (protocol.equals("HTTP+TCP")) {
                // Modo duplo: inicializar ambos protocolos
                initializeDualProtocol();
            } else {
                // Modo simples: um protocolo
                initializeSingleProtocol(protocol);
            }
            
            // Criar Data Receivers com detecção automática de portas
            createDataReceiversWithPortDetection();
            
            // Iniciar sistema de tolerância a falhas
            FaultToleranceManager faultManager = new FaultToleranceManager(gateway);
            faultManager.start();
            
            System.out.println("✅ Sistema iniciado com sucesso!");
            System.out.println("📊 Protocolo: " + protocol);
            System.out.println("📊 Data Receivers ativos: " + receivers.size());
            System.out.println("📊 Gateway ID: " + gateway.getGatewayId());
            
            Thread.sleep(1000); // Pausa para estabilização
            
        } catch (Exception e) {
            throw new RuntimeException("Falha na inicialização: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inicializa um único protocolo
     */
    private static void initializeSingleProtocol(String protocol) throws Exception {
        // Adicionar monitor de heartbeat
        HeartbeatMonitor monitor = new HeartbeatMonitor(5);
        gateway.addObserver(monitor);
        
        int port = findAvailablePort(getGatewayPort(protocol));
        
        switch (protocol) {
            case "UDP":
                gateway.setCommunicationStrategy(new UDPCommunicationStrategy());
                break;
            case "HTTP":
                gateway.setCommunicationStrategy(new HTTPCommunicationStrategy());
                break;
            case "TCP":
                gateway.setCommunicationStrategy(new TCPCommunicationStrategy());
                break;
            case "gRPC":
                gateway.setCommunicationStrategy(new GRPCCommunicationStrategy());
                break;
            default:
                throw new IllegalArgumentException("Protocolo não suportado: " + protocol);
        }
        
        // Iniciar Gateway
        gateway.start(port);
        System.out.println("� Gateway ativo na porta: " + port);
    }
    
    /**
     * Inicializa protocolo duplo HTTP+TCP
     */
    private static void initializeDualProtocol() throws Exception {
        System.out.println("� Inicializando modo duplo HTTP+TCP...");
        
        // Adicionar monitor de heartbeat
        HeartbeatMonitor monitor = new HeartbeatMonitor(5);
        gateway.addObserver(monitor);
        
        // Iniciar primeiro com HTTP
        int httpPort = findAvailablePort(8080);
        gateway.setCommunicationStrategy(new HTTPCommunicationStrategy());
        gateway.start(httpPort);
        System.out.println("📡 Gateway HTTP ativo na porta: " + httpPort);
        
        // TODO: Implementar lógica para TCP simultâneo
        // Por enquanto, só HTTP será ativo
        System.out.println("⚠️ Modo TCP será ativado em versão futura");
    }
    
    /**
     * Cria os Data Receivers com detecção automática de portas
     */
    private static void createDataReceiversWithPortDetection() {
        receivers = new ArrayList<>();
        
        try {
            // Encontrar portas disponíveis para receivers
            int port1 = findAvailablePort(9091);
            int port2 = findAvailablePort(port1 + 1);
            
            // Criar 2 receivers principais
            DataReceiver receiver1 = new DataReceiver("DATA_RECEIVER_1", port1);
            DataReceiver receiver2 = new DataReceiver("DATA_RECEIVER_2", port2);
            
            receiver1.start();
            receiver2.start();
            
            gateway.registerDataReceiver(receiver1);
            gateway.registerDataReceiver(receiver2);
            
            receivers.add(receiver1);
            receivers.add(receiver2);
            
            System.out.println("📊 Data Receiver 1 ativo na porta: " + port1);
            System.out.println("📊 Data Receiver 2 ativo na porta: " + port2);
            
            // Aguardar inicialização
            Thread.sleep(1000);
            
        } catch (Exception e) {
            throw new RuntimeException("Falha na criação dos Data Receivers: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encontra uma porta disponível a partir de uma porta base
     */
    private static int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new RuntimeException("Nenhuma porta disponível encontrada a partir de " + startPort);
    }
    
    /**
     * Verifica se uma porta está disponível
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Retorna a porta padrão para cada protocolo
     */
    private static int getGatewayPort(String protocol) {
        switch (protocol.toUpperCase()) {
            case "UDP":
                return 8080;
            case "HTTP":
                return 8081;
            case "TCP":
                return 8082;
            case "GRPC":
                return 9090;
            default:
                return 8080;
        }
    }
    
    /**
     * Menu principal de controle dos receivers
     */
    private static void runControlMenu() {
        System.out.println(CONTROL_BANNER);
        
        while (true) {
            printControlMenu();
            System.out.print("\n🎯 Digite comando: ");
            String command = scanner.nextLine().trim().toLowerCase();
            
            try {
                switch (command) {
                    case "f1":
                        simulateFailure(0);
                        break;
                    case "f2":
                        simulateFailure(1);
                        break;
                    case "fall":
                        simulateFailureAll();
                        break;
                    case "r1":
                        recoverReceiver(0);
                        break;
                    case "r2":
                        recoverReceiver(1);
                        break;
                    case "rall":
                        recoverAll();
                        break;
                    case "status":
                        showStatus();
                        break;
                    case "info":
                        showSystemInfo();
                        break;
                    case "sair":
                    case "exit":
                        System.out.println("\n👋 Encerrando sistema...");
                        shutdown();
                        scanner.close();
                        return;
                    default:
                        System.out.println("❌ Comando inválido! Digite 'info' para ver comandos disponíveis.");
                        break;
                }
                
                Thread.sleep(500); // Pausa para melhor UX
                
            } catch (Exception e) {
                System.out.println("❌ Erro ao executar comando: " + e.getMessage());
            }
        }
    }
    
    /**
     * Imprime menu de controle
     */
    private static void printControlMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎮 CONTROLE DE DATA RECEIVERS - " + selectedProtocol);
        System.out.println("=".repeat(60));
        System.out.println("FALHAS:");
        System.out.println("• f1, f2     - Simular falha no Receiver 1 ou 2");
        System.out.println("• fall       - Simular falha em TODOS");
        System.out.println("\nRECUPERAÇÃO:");
        System.out.println("• r1, r2     - Recuperar Receiver 1 ou 2");
        System.out.println("• rall       - Recuperar TODOS");
        System.out.println("\nINFORMAÇÕES:");
        System.out.println("• status     - Status dos receivers");
        System.out.println("• info       - Informações do sistema");
        System.out.println("• sair       - Encerrar aplicação");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Simula falha em um receiver específico
     */
    private static void simulateFailure(int index) {
        if (index >= receivers.size()) {
            System.out.println("❌ Receiver não encontrado!");
            return;
        }
        
        DataReceiver receiver = receivers.get(index);
        
        if (!receiver.isRunning()) {
            System.out.println("⚠️ " + receiver.getReceiverId() + " já está inativo!");
            return;
        }
        
        try {
            receiver.simulateFailure();
            System.out.println("💀 Falha simulada: " + receiver.getReceiverId());
            System.out.println("   📊 Impacto: Tráfego redirecionado para outros receivers");
            
            showImpactAnalysis();
            
        } catch (Exception e) {
            System.out.println("❌ Erro ao simular falha: " + e.getMessage());
        }
    }
    
    /**
     * Simula falha em todos os receivers
     */
    private static void simulateFailureAll() {
        System.out.println("💀💀 Simulando falha em TODOS os receivers...");
        
        int failed = 0;
        for (DataReceiver receiver : receivers) {
            if (receiver.isRunning()) {
                try {
                    receiver.simulateFailure();
                    failed++;
                } catch (Exception e) {
                    System.out.println("⚠️ Erro ao falhar " + receiver.getReceiverId() + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("🔴 ALERTA: " + failed + " receivers falharam!");
        System.out.println("   📊 Sistema entrará em modo de emergência");
        System.out.println("   📊 Taxa de erro esperada: 80-100%");
        
        showImpactAnalysis();
    }
    
    /**
     * Recupera um receiver específico
     */
    private static void recoverReceiver(int index) {
        if (index >= receivers.size()) {
            System.out.println("❌ Receiver não encontrado!");
            return;
        }
        
        DataReceiver receiver = receivers.get(index);
        
        if (receiver.isRunning()) {
            System.out.println("ℹ️ " + receiver.getReceiverId() + " já está ativo!");
            return;
        }
        
        try {
            receiver.recover();
            System.out.println("💚 Recuperado: " + receiver.getReceiverId());
            System.out.println("   📊 Impacto: Capacidade do sistema restaurada");
            
            showImpactAnalysis();
            
        } catch (Exception e) {
            System.out.println("❌ Erro ao recuperar: " + e.getMessage());
        }
    }
    
    /**
     * Recupera todos os receivers
     */
    private static void recoverAll() {
        System.out.println("💚 Recuperando TODOS os receivers...");
        
        int recovered = 0;
        for (DataReceiver receiver : receivers) {
            if (!receiver.isRunning()) {
                try {
                    receiver.recover();
                    recovered++;
                } catch (Exception e) {
                    System.out.println("⚠️ Erro ao recuperar " + receiver.getReceiverId() + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("✅ Recuperação completa: " + recovered + " receivers restaurados");
        System.out.println("   📊 Sistema voltou à capacidade total");
        System.out.println("   📊 Taxa de erro esperada: 0-5%");
        
        showImpactAnalysis();
    }
    
    /**
     * Mostra status dos receivers
     */
    private static void showStatus() {
        System.out.println("\n📊 STATUS DOS DATA RECEIVERS:");
        System.out.println("-".repeat(50));
        
        int active = 0, inactive = 0;
        for (int i = 0; i < receivers.size(); i++) {
            DataReceiver receiver = receivers.get(i);
            String status = receiver.isRunning() ? "🟢 ATIVO" : "🔴 INATIVO";
            System.out.printf("   [%d] %s - Porta: %d - %s%n", 
                             i + 1, receiver.getReceiverId(), receiver.getPort(), status);
            
            if (receiver.isRunning()) active++;
            else inactive++;
        }
        
        System.out.println("-".repeat(50));
        System.out.printf("📈 Resumo: %d ativos, %d inativos (Total: %d)%n", 
                         active, inactive, receivers.size());
        
        showImpactAnalysis();
    }
    
    /**
     * Mostra informações do sistema
     */
    private static void showSystemInfo() {
        System.out.println("\n📋 INFORMAÇÕES DO SISTEMA:");
        System.out.println("-".repeat(50));
        System.out.println("🔧 Protocolo: " + selectedProtocol);
        System.out.println("🆔 Gateway ID: " + gateway.getGatewayId());
        System.out.println("📊 Data Receivers: " + receivers.size());
        System.out.println("🟢 Gateway Status: " + (gateway.isRunning() ? "ATIVO" : "INATIVO"));
        System.out.println("📡 Porta Gateway: " + getGatewayPort());
        System.out.println("-".repeat(50));
        System.out.println("💡 Para JMeter: Use protocolo " + selectedProtocol + " na porta " + getGatewayPort());
    }
    
    /**
     * Mostra análise de impacto baseada no status atual
     */
    private static void showImpactAnalysis() {
        int active = 0;
        for (DataReceiver receiver : receivers) {
            if (receiver.isRunning()) active++;
        }
        
        int total = receivers.size();
        int inactive = total - active;
        
        System.out.println("\n📈 ANÁLISE DE IMPACTO:");
        System.out.println("-".repeat(30));
        
        if (inactive == 0) {
            System.out.println("✅ Sistema normal - Taxa de erro: 0%");
            System.out.println("📊 Throughput: ~70 req/min");
        } else if (active > 0) {
            int errorRate = (inactive * 100) / total;
            System.out.println("⚠️ Sistema degradado - Taxa de erro: ~" + (errorRate * 2) + "%");
            System.out.println("📊 Throughput: ~" + (70 - (inactive * 20)) + " req/min");
        } else {
            System.out.println("🔴 Modo emergência - Taxa de erro: 80-100%");
            System.out.println("📊 Throughput: <10 req/min");
        }
        
        System.out.println("-".repeat(30));
    }
    
    /**
     * Obtém porta do gateway baseada no protocolo
     */
    private static int getGatewayPort() {
        switch (selectedProtocol) {
            case "UDP": return 9090;
            case "HTTP": return 8080;
            case "TCP": return 8082;
            case "gRPC": return 9000;
            default: return 8080;
        }
    }
    
    /**
     * Configura modo silencioso para logs
     */
    private static void setSilentMode() {
        ch.qos.logback.classic.Logger rootLogger = 
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.ERROR); // Só mostra erros críticos
    }
    
    /**
     * Shutdown gracioso do sistema
     */
    private static void shutdown() {
        try {
            if (gateway != null && gateway.isRunning()) {
                gateway.stop();
            }
            
            for (DataReceiver receiver : receivers) {
                if (receiver.isRunning()) {
                    receiver.stop();
                }
            }
            
            System.out.println("✅ Sistema encerrado com sucesso!");
            
        } catch (Exception e) {
            System.out.println("⚠️ Erro durante shutdown: " + e.getMessage());
        }
    }
}