package br.ufrn.dimap.applications;

import br.ufrn.dimap.components.DataReceiver;
import br.ufrn.dimap.patterns.singleton.IoTGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

/**
 * Menu interativo simplificado para gerenciamento do Sistema IoT Distribuído
 */
public class IoTInteractiveMenu {
    private static final Logger logger = LoggerFactory.getLogger(IoTInteractiveMenu.class);
    private final Scanner scanner;
    private boolean systemRunning = false;
    private String currentProtocol = null;

    public IoTInteractiveMenu() {
        this.scanner = new Scanner(System.in);
    }

    public void start() throws Exception {
        while (true) {
            try {
                if (!systemRunning) {
                    showMainMenu();
                } else {
                    showRunningSystemMenu();
                }
                
                int choice = getUserChoice();
                handleMenuChoice(choice);
                
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
                logger.error("Erro no menu interativo", e);
                waitForUser();
            }
        }
    }

    private void showMainMenu() {
        System.out.println("\n================================================================================");
        System.out.println("MENU PRINCIPAL - Sistema IoT Distribuído");
        System.out.println("================================================================================");
        System.out.println("1. Iniciar com HTTP + TCP (Portas 8081 e 8082)");
        System.out.println("2. Iniciar com gRPC (Porta 9090)");
        System.out.println("3. Iniciar com UDP (Porta 9090)");
        System.out.println("0. Sair");
        System.out.println("================================================================================");
        System.out.print("Escolha uma opção: ");
    }

    private void showRunningSystemMenu() {
        System.out.println("\n================================================================================");
        System.out.println("MENU PRINCIPAL - Sistema IoT Distribuído");
        System.out.println("================================================================================");
        System.out.println("STATUS: Sistema rodando com protocolo " + currentProtocol);
        System.out.println("1. Gerenciar Data Receivers");
        System.out.println("2. Status do Sistema");
        System.out.println("3. Reiniciar com Novo Protocolo");
        System.out.println("9. Parar Sistema");
        System.out.println("0. Sair");
        System.out.println("================================================================================");
        System.out.print("Escolha uma opção: ");
    }

    private void handleMenuChoice(int choice) throws Exception {
        if (!systemRunning) {
            switch (choice) {
                case 1:
                    startHttpTcpProtocols();
                    break;
                case 2:
                    startGrpcProtocol();
                    break;
                case 3:
                    startUdpProtocol();
                    break;
                case 0:
                    exitApplication();
                    break;
                default:
                    System.out.println("Opção inválida! Tente novamente.");
                    waitForUser();
                    break;
            }
        } else {
            switch (choice) {
                case 1:
                    manageDataReceivers();
                    break;
                case 2:
                    showSystemStatus();
                    break;
                case 3:
                    restartWithNewProtocol();
                    break;
                case 9:
                    stopSystem();
                    break;
                case 0:
                    exitApplication();
                    break;
                default:
                    System.out.println("Opção inválida! Tente novamente.");
                    waitForUser();
                    break;
            }
        }
    }

    private void startHttpTcpProtocols() throws Exception {
        System.out.println("\nIniciando sistema com HTTP + TCP...");
        
        // Iniciar HTTP em terminal separado
        startProtocolInNewTerminal("HTTP", "8081");
        Thread.sleep(2000);
        
        // Iniciar TCP em terminal separado
        startProtocolInNewTerminal("TCP", "8082");
        Thread.sleep(2000);
        
        currentProtocol = "HTTP + TCP";
        systemRunning = true;
        
        System.out.println("Sistema iniciado com sucesso!");
        System.out.println("HTTP Server em execução: http://localhost:8081 (Terminal separado)");
        System.out.println("TCP Server em execução: localhost:8082 (Terminal separado)");
        System.out.println("Data Receivers em execução nas portas 9091 e 9092");
        waitForUser();
    }

    private void startGrpcProtocol() throws Exception {
        System.out.println("\nIniciando sistema com gRPC...");
        
        // Iniciar o sistema em terminal separado
        startProtocolInNewTerminal("GRPC", "9090");
        
        // Aguardar um pouco para o sistema inicializar
        Thread.sleep(2000);
        
        currentProtocol = "gRPC";
        systemRunning = true;
        
        logger.info("Sistema iniciado com protocolo gRPC");
        System.out.println("Sistema iniciado com sucesso!");
        System.out.println("Protocolo ativo: gRPC");
        System.out.println("Servidor gRPC em execução em terminal separado na porta 9090");
        System.out.println("Data Receivers em execução nas portas 9091 e 9092");
        waitForUser();
    }

    private void startUdpProtocol() throws Exception {
        System.out.println("\nIniciando sistema com UDP...");
        
        // Iniciar o sistema em terminal separado
        startProtocolInNewTerminal("UDP", "9090");
        
        // Aguardar um pouco para o sistema inicializar
        Thread.sleep(2000);
        
        currentProtocol = "UDP";
        systemRunning = true;
        
        logger.info("Sistema iniciado com protocolo UDP");
        System.out.println("Sistema iniciado com sucesso!");
        System.out.println("Protocolo ativo: UDP");
        System.out.println("Servidor UDP em execução em terminal separado na porta 9090");
        System.out.println("Data Receivers em execução nas portas 9091 e 9092");
        waitForUser();
    }

    private void startProtocolInNewTerminal(String protocol, String port) {
        try {
            // Criar um arquivo batch temporário para evitar problemas de escape
            String tempBatFile = "temp_" + protocol.toLowerCase() + ".bat";
            java.io.FileWriter writer = new java.io.FileWriter(tempBatFile);
            writer.write("@echo off\n");
            writer.write("cd /d D:\\distribuida\n");
            writer.write("java -cp \"target/classes;target/lib/*\" br.ufrn.dimap.applications.IoTDistributedSystem " + protocol + "\n");
            writer.write("pause\n");
            writer.close();
            
            // Executar o arquivo batch em novo terminal
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", tempBatFile);
            pb.start();
            
            System.out.println("Servidor " + protocol + " iniciado em novo terminal na porta " + port);
            
            // Agendar remoção do arquivo temporário após 10 segundos
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    try {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempBatFile));
                    } catch (Exception e) {
                        // Ignorar erros de limpeza
                    }
                }
            }, 10000);
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor " + protocol + ": " + e.getMessage());
        }
    }

    private void manageDataReceivers() {
        System.out.println("\n=== GERENCIAMENTO DE DATA RECEIVERS ===");
        
        if (!systemRunning) {
            System.out.println("Sistema não está rodando!");
            waitForUser();
            return;
        }

        System.out.println("Data Receivers ativos no sistema:");
        if ("UDP".equals(currentProtocol) || "gRPC".equals(currentProtocol)) {
            System.out.println("1. DATA_RECEIVER_1 - ATIVO (Porta: 9091) [Terminal separado]");
            System.out.println("2. DATA_RECEIVER_2 - ATIVO (Porta: 9092) [Terminal separado]");
        } else if ("HTTP + TCP".equals(currentProtocol)) {
            System.out.println("1. DATA_RECEIVER_1 - ATIVO (Porta: 9091) [Terminal separado]");
            System.out.println("2. DATA_RECEIVER_2 - ATIVO (Porta: 9092) [Terminal separado]");
        }

        System.out.println("\nNOTA: Esta é uma demonstração do menu de gerenciamento.");
        System.out.println("Os Data Receivers reais estão executando no terminal do servidor principal.");
        System.out.println("Para gerenciamento real, acesse diretamente o terminal do servidor.");

        System.out.println("\nAções de Demonstração:");
        System.out.println("A. Simular adição de Data Receiver");
        System.out.println("R. Simular remoção de Data Receiver");
        System.out.println("S. Simular toggle de Data Receiver");
        System.out.println("0. Voltar");
        System.out.print("\nEscolha: ");

        String choice = scanner.nextLine().trim().toUpperCase();
        switch (choice) {
            case "A":
                addDataReceiver();
                break;
            case "R":
                removeDataReceiver();
                break;
            case "S":
                toggleDataReceiver();
                break;
            case "0":
                /* Voltar */
                break;
            default:
                System.out.println("Opção inválida!");
                waitForUser();
                break;
        }
    }

    private void addDataReceiver() {
        System.out.print("Digite a porta para o novo Data Receiver (simulação): ");
        try {
            int port = Integer.parseInt(scanner.nextLine().trim());
            String receiverId = "DATA_RECEIVER_DEMO_" + System.currentTimeMillis();
            
            System.out.println("🔧 SIMULAÇÃO: Data Receiver " + receiverId + " seria adicionado na porta " + port);
            System.out.println("   No sistema real, isso seria feito no terminal do servidor principal.");
            
        } catch (NumberFormatException e) {
            System.out.println("Erro: Digite um número válido!");
        }
        waitForUser();
    }

    private void removeDataReceiver() {
        System.out.print("Digite o número do Data Receiver para remover (1-2): ");
        try {
            int index = Integer.parseInt(scanner.nextLine().trim());
            
            if (index >= 1 && index <= 2) {
                String receiverId = "DATA_RECEIVER_" + index;
                System.out.println("🔧 SIMULAÇÃO: " + receiverId + " seria removido do sistema.");
                System.out.println("   No sistema real, isso seria feito no terminal do servidor principal.");
            } else {
                System.out.println("Número inválido! Use 1 ou 2.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Erro: Digite um número válido!");
        }
        waitForUser();
    }

    private void toggleDataReceiver() {
        System.out.print("Digite o número do Data Receiver para parar/iniciar (1-2): ");
        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Entrada vazia!");
                waitForUser();
                return;
            }
            
            int index = Integer.parseInt(input);
            
            if (index >= 1 && index <= 2) {
                String receiverId = "DATA_RECEIVER_" + index;
                boolean currentState = Math.random() > 0.5; // Simula estado aleatório
                String action = currentState ? "parado" : "iniciado";
                String newState = currentState ? "PARADO" : "ATIVO";
                
                System.out.println("🔧 SIMULAÇÃO: " + receiverId + " foi " + action + " (Status: " + newState + ")");
                System.out.println("   No sistema real, isso seria feito no terminal do servidor principal.");
            } else {
                System.out.println("Número inválido! Use 1 ou 2.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Erro: Digite um número válido!");
        }
        waitForUser();
    }

    private void showSystemStatus() {
        System.out.println("\n=== STATUS DO SISTEMA ===");
        System.out.println("Protocolo atual: " + currentProtocol);
        System.out.println("Sistema Status: " + (systemRunning ? "ATIVO" : "PARADO"));
        
        if (systemRunning) {
            System.out.println("\n--- Servidores em Terminais Separados ---");
            
            if ("UDP".equals(currentProtocol)) {
                System.out.println("  - Servidor UDP: ATIVO (Porta 9090)");
                System.out.println("  - Data Receiver 1: ATIVO (Porta 9091)");
                System.out.println("  - Data Receiver 2: ATIVO (Porta 9092)");
            } else if ("gRPC".equals(currentProtocol)) {
                System.out.println("  - Servidor gRPC: ATIVO (Porta 9090)");
                System.out.println("  - Data Receiver 1: ATIVO (Porta 9091)");
                System.out.println("  - Data Receiver 2: ATIVO (Porta 9092)");
            } else if ("HTTP + TCP".equals(currentProtocol)) {
                System.out.println("  - Servidor HTTP: ATIVO (Porta 8081)");
                System.out.println("  - Servidor TCP: ATIVO (Porta 8082)");
                System.out.println("  - Data Receiver 1: ATIVO (Porta 9091)");
                System.out.println("  - Data Receiver 2: ATIVO (Porta 9092)");
            }
            
            System.out.println("\nTodos os servidores estão executando em terminais separados.");
            System.out.println("Use os menus de gerenciamento para demonstrar funcionalidades.");
        } else {
            System.out.println("Nenhum servidor ativo no momento.");
        }
        
        waitForUser();
    }

    private void restartWithNewProtocol() throws Exception {
        System.out.print("\nReiniciar com novo protocolo? Isso irá parar o sistema atual. Continuar? (s/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if ("s".equals(confirm) || "sim".equals(confirm)) {
            System.out.println("\nParando sistema...");
            stopSystem();
            System.out.println("Sistema parado com sucesso!");
            waitForUser();
        }
    }

    private void stopSystem() throws Exception {
        System.out.println("\nParando sistema...");
        
        IoTGateway gateway = IoTGateway.getInstance();
        if (gateway != null && gateway.isRunning()) {
            List<DataReceiver> receivers = gateway.getDataReceivers();
            
            // Parar todos os Data Receivers
            for (DataReceiver receiver : receivers) {
                if (receiver.isRunning()) {
                    receiver.stop();
                    logger.info("Data Receiver {} parado", receiver.getId());
                }
            }
            
            // Parar o Gateway
            gateway.stop();
            logger.info("Gateway parado");
        }
        
        // Sistema será parado via processamento dos terminais
        
        systemRunning = false;
        currentProtocol = null;
        
        System.out.println("Sistema parado com sucesso!");
        waitForUser();
    }

    private void exitApplication() {
        try {
            if (systemRunning) {
                stopSystem();
            }
        } catch (Exception e) {
            logger.error("Erro ao parar sistema antes de sair", e);
        }
        
        System.out.println("\nEncerrando aplicação...");
        scanner.close();
        System.exit(0);
    }

    private int getUserChoice() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1; // Opção inválida
        }
    }



    private void waitForUser() {
        System.out.print("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }
}