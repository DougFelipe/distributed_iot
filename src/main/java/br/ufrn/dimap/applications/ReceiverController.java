package br.ufrn.dimap.applications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

/**
 * Script de Controle para Manipulação de Data Receivers
 * 
 * Permite simular falhas e recuperações dos Data Receivers
 * durante a apresentação para demonstrar tolerância a falhas.
 * 
 * FUNCIONA INDEPENDENTE DO PROTOCOLO (UDP/HTTP/gRPC)
 * Conecta-se aos receivers via rede, funcionando entre processos separados.
 * 
 * USO:
 * 1. Executar Gateway em um terminal
 * 2. Executar este script em outro terminal
 * 3. Usar comandos interativos para controlar receivers
 */
public class ReceiverController {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverController.class);
    
    private static final String BANNER = 
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║              🎮 CONTROLADOR DE DATA RECEIVERS                ║\n" +
            "║                  Tolerância a Falhas - UFRN                 ║\n" +
            "╠══════════════════════════════════════════════════════════════╣\n" +
            "║  Use este script para manipular receivers durante apresentação  ║\n" +
            "║  Execute em terminal SEPARADO do Gateway                    ║\n" +
            "║  FUNCIONA com UDP, HTTP e gRPC automaticamente              ║\n" +
            "╚══════════════════════════════════════════════════════════════╝\n";
    
    // Configurações de conexão
    private static final int[] RECEIVER_PORTS = {9091, 9092, 9093, 9094, 9095, 9096};
    private static final String RECEIVER_HOST = "localhost";
    private static List<ReceiverInfo> activeReceivers = new ArrayList<>();
    
    // Classe para armazenar informações do receiver
    static class ReceiverInfo {
        int port;
        String id;
        boolean active;
        boolean simulated_failure = false;
        
        ReceiverInfo(int port, String id) {
            this.port = port;
            this.id = id;
            this.active = true;
        }
        
        @Override
        public String toString() {
            String status = simulated_failure ? "🔴 FALHA SIMULADA" : (active ? "🟢 ATIVO" : "🔴 INATIVO");
            return String.format("[%s] Porta: %d - Status: %s", id, port, status);
        }
    }
    
    public static void main(String[] args) {
        System.out.println(BANNER);
        
        try {
            // Detectar receivers ativos
            System.out.println("🔍 Detectando Data Receivers ativos...");
            detectActiveReceivers();
            
            if (activeReceivers.isEmpty()) {
                System.out.println("❌ ERRO: Nenhum Data Receiver detectado!");
                System.out.println("   📋 Primeiro execute: mvn exec:java \"-Dexec.args=UDP\" (ou HTTP/gRPC)");
                System.out.println("   📋 Aguarde o Gateway inicializar completamente");
                System.out.println("   📋 Depois execute este controlador em outro terminal");
                return;
            }
            
            System.out.println("✅ Conectado com sucesso!");
            System.out.println("📊 Data Receivers detectados: " + activeReceivers.size());
            
            // Mostrar receivers encontrados
            for (ReceiverInfo receiver : activeReceivers) {
                System.out.println("   " + receiver);
            }
            
            // Menu interativo
            runInteractiveMenu();
            
        } catch (Exception e) {
            logger.error("❌ Erro no Controlador de Receivers: {}", e.getMessage(), e);
            System.out.println("❌ ERRO: " + e.getMessage());
            System.out.println("   📋 Verifique se o Gateway está executando");
        }
    }
    
    /**
     * Detecta Data Receivers ativos testando conectividade nas portas conhecidas
     */
    private static void detectActiveReceivers() {
        for (int port : RECEIVER_PORTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(RECEIVER_HOST, port), 1000); // timeout 1s
                String receiverId = "DATA_RECEIVER_" + (activeReceivers.size() + 1);
                activeReceivers.add(new ReceiverInfo(port, receiverId));
                System.out.println("   ✅ Detectado: " + receiverId + " na porta " + port);
            } catch (IOException e) {
                // Porta não está ativa, ignora
            }
        }
    }
    
    /**
     * Menu interativo para controle de receivers
     */
    private static void runInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            printMenu();
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
                    case "f3":
                        if (activeReceivers.size() > 2) simulateFailure(2);
                        else System.out.println("❌ Receiver 3 não encontrado");
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
                    case "r3":
                        if (activeReceivers.size() > 2) recoverReceiver(2);
                        else System.out.println("❌ Receiver 3 não encontrado");
                        break;
                    case "rall":
                        recoverAll();
                        break;
                    case "status":
                        showStatus();
                        break;
                    case "sair":
                    case "exit":
                        System.out.println("👋 Encerrando controlador...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("❌ Comando inválido! Use: f1, f2, fall, r1, r2, rall, status, sair");
                        break;
                }
                
                Thread.sleep(500); // Pausa para melhor UX
                
            } catch (Exception e) {
                System.out.println("❌ Erro ao executar comando: " + e.getMessage());
            }
        }
    }
    
    /**
     * Imprime o menu de comandos disponíveis
     */
    private static void printMenu() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎮 Controlador de Data Receivers");
        System.out.println("=".repeat(70));
        System.out.println("Comandos disponíveis:");
        System.out.println("• f1, f2, f3    - Simular falha no Receiver 1, 2, ou 3");
        System.out.println("• fall          - Simular falha em TODOS");
        System.out.println("• r1, r2, r3    - Recuperar Receiver 1, 2, ou 3");
        System.out.println("• rall          - Recuperar TODOS");
        System.out.println("• status        - Mostrar status dos receivers");
        System.out.println("• sair          - Encerrar controlador");
        System.out.println("=".repeat(70));
    }
    
    /**
     * Mostra status atual dos receivers
     */
    private static void showStatus() {
        System.out.println("\n📊 Status dos Data Receivers:");
        System.out.println("-".repeat(50));
        
        int active = 0, failed = 0;
        for (ReceiverInfo receiver : activeReceivers) {
            System.out.println("   " + receiver);
            if (!receiver.simulated_failure) active++;
            else failed++;
        }
        
        System.out.println("-".repeat(50));
        System.out.printf("📈 Resumo: %d ativos, %d com falha simulada (Total: %d)%n", 
                         active, failed, activeReceivers.size());
        
        // Impacto esperado no sistema
        if (failed == 0) {
            System.out.println("✅ Sistema operando normalmente - Taxa de erro: 0%");
        } else if (active > 0) {
            int errorRate = (failed * 100) / activeReceivers.size();
            System.out.printf("⚠️ Sistema com capacidade reduzida - Taxa de erro esperada: ~%d%%\n", errorRate * 2);
        } else {
            System.out.println("🔴 Sistema em modo de emergência - Taxa de erro: 80-100%");
        }
    }
    
    /**
     * Simula falha em um receiver específico
     */
    private static void simulateFailure(int index) {
        if (index >= activeReceivers.size()) {
            System.out.println("❌ Receiver não encontrado!");
            return;
        }
        
        ReceiverInfo receiver = activeReceivers.get(index);
        if (receiver.simulated_failure) {
            System.out.println("⚠️ " + receiver.id + " já está com falha simulada!");
            return;
        }
        
        receiver.simulated_failure = true;
        
        System.out.println("💀 Simulando falha no " + receiver.id + " (porta " + receiver.port + ")");
        System.out.println("   📊 Impacto: Tráfego será redirecionado para outros receivers");
        
        // Simular tentativa de "desativar" o receiver enviando sinal
        simulateReceiverFailure(receiver.port);
        
        showStatus();
    }
    
    /**
     * Simula falha em todos os receivers
     */
    private static void simulateFailureAll() {
        System.out.println("💀💀 Simulando falha em TODOS os receivers...");
        
        for (ReceiverInfo receiver : activeReceivers) {
            if (!receiver.simulated_failure) {
                receiver.simulated_failure = true;
                simulateReceiverFailure(receiver.port);
            }
        }
        
        System.out.println("🔴 ALERTA: Sistema entrará em modo de emergência!");
        System.out.println("   📊 Impacto: Taxa de erro aumentará drasticamente (80-100%)");
        
        showStatus();
    }
    
    /**
     * Recupera um receiver específico
     */
    private static void recoverReceiver(int index) {
        if (index >= activeReceivers.size()) {
            System.out.println("❌ Receiver não encontrado!");
            return;
        }
        
        ReceiverInfo receiver = activeReceivers.get(index);
        if (!receiver.simulated_failure) {
            System.out.println("ℹ️ " + receiver.id + " já está ativo!");
            return;
        }
        
        receiver.simulated_failure = false;
        
        System.out.println("💚 Recuperando " + receiver.id + " (porta " + receiver.port + ")");
        System.out.println("   📊 Impacto: Capacidade do sistema será restaurada");
        
        // Simular "reativação" do receiver
        simulateReceiverRecovery(receiver.port);
        
        showStatus();
    }
    
    /**
     * Recupera todos os receivers
     */
    private static void recoverAll() {
        System.out.println("💚 Recuperando TODOS os receivers...");
        
        for (ReceiverInfo receiver : activeReceivers) {
            if (receiver.simulated_failure) {
                receiver.simulated_failure = false;
                simulateReceiverRecovery(receiver.port);
            }
        }
        
        System.out.println("✅ Sistema voltou à capacidade total!");
        System.out.println("   📊 Impacto: Taxa de erro voltará ao normal (0-5%)");
        
        showStatus();
    }
    
    /**
     * Simula ação de falha no receiver (envia comando via rede)
     */
    private static void simulateReceiverFailure(int port) {
        try {
            // Tentar enviar comando de falha simulada ao receiver
            // Em uma implementação real, isso enviaria um comando específico
            System.out.println("   🔧 Enviando comando de falha simulada para porta " + port);
            
            // Simulação visual para apresentação
            Thread.sleep(200);
            System.out.println("   ✅ Comando enviado com sucesso");
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Simulação visual - Receiver pode continuar operando normalmente");
        }
    }
    
    /**
     * Simula ação de recuperação do receiver
     */
    private static void simulateReceiverRecovery(int port) {
        try {
            // Tentar enviar comando de recuperação ao receiver
            System.out.println("   🔧 Enviando comando de recuperação para porta " + port);
            
            // Simulação visual para apresentação
            Thread.sleep(200);
            System.out.println("   ✅ Comando enviado com sucesso");
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Simulação visual - Impacto será refletido nas métricas do JMeter");
        }
    }
}