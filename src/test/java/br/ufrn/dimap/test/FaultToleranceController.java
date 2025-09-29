package br.ufrn.dimap.test;

import br.ufrn.dimap.patterns.singleton.IoTGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Controlador para Testes de Tolerância a Falhas
 * 
 * Permite simular falhas e recuperações durante apresentação
 * para demonstrar os critérios de avaliação:
 * 
 * 1. Sistema funcionando (zero erros)
 * 2. Simular falhas (aumento da taxa de erro)
 * 3. Criar novas instâncias (diminuição da taxa de erro)
 */
public class FaultToleranceController {
    private static final Logger logger = LoggerFactory.getLogger(FaultToleranceController.class);
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        IoTGateway gateway = IoTGateway.getInstance();
        
        System.out.println("\n🛡️ CONTROLADOR DE TOLERÂNCIA A FALHAS");
        System.out.println("=====================================");
        System.out.println("Para testes durante apresentação JMeter");
        System.out.println();
        
        while (true) {
            System.out.println("\n📋 Opções disponíveis:");
            System.out.println("1. 📊 Status do sistema");
            System.out.println("2. 💥 Simular falha DATA_RECEIVER_1");
            System.out.println("3. 💥 Simular falha DATA_RECEIVER_2");
            System.out.println("4. 🆕 Criar nova instância DATA_RECEIVER_3 (porta 9093)");
            System.out.println("5. 🆕 Criar nova instância DATA_RECEIVER_4 (porta 9094)");
            System.out.println("6. 🏥 Health check manual");
            System.out.println("0. ❌ Sair");
            System.out.print("\n👉 Escolha uma opção: ");
            
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1":
                    showSystemStatus(gateway);
                    break;
                case "2":
                    simulateFailure(gateway, "DATA_RECEIVER_1");
                    break;
                case "3":
                    simulateFailure(gateway, "DATA_RECEIVER_2");
                    break;
                case "4":
                    createNewInstance(gateway, "DATA_RECEIVER_3", 9093);
                    break;
                case "5":
                    createNewInstance(gateway, "DATA_RECEIVER_4", 9094);
                    break;
                case "6":
                    performHealthCheck(gateway);
                    break;
                case "0":
                    System.out.println("👋 Controlador encerrado");
                    return;
                default:
                    System.out.println("⚠️ Opção inválida");
            }
        }
    }
    
    private static void showSystemStatus(IoTGateway gateway) {
        System.out.println("\n📊 STATUS DO SISTEMA:");
        System.out.println("=====================");
        System.out.println("Gateway ID: " + gateway.getGatewayId());
        System.out.println("Ativo: " + gateway.isActive());
        System.out.println("Sensores registrados: " + gateway.getRegisteredSensorsCount());
        System.out.println("Data Receivers: " + gateway.getRegisteredReceiversCount());
        System.out.println("Total de mensagens: " + gateway.getTotalMessages());
        System.out.println("\n📈 Estatísticas detalhadas:");
        System.out.println(gateway.getDetailedStats());
    }
    
    private static void simulateFailure(IoTGateway gateway, String receiverId) {
        System.out.println("\n💥 SIMULANDO FALHA: " + receiverId);
        System.out.println("===========================");
        
        try {
            gateway.simulateReceiverFailure(receiverId);
            System.out.println("✅ Falha simulada com sucesso");
            System.out.println("📊 Monitore o JMeter - taxa de erro deve AUMENTAR");
            System.out.println("📝 Receptores restantes: " + gateway.getRegisteredReceiversCount());
            
        } catch (Exception e) {
            System.out.println("❌ Erro ao simular falha: " + e.getMessage());
        }
    }
    
    private static void createNewInstance(IoTGateway gateway, String receiverId, int port) {
        System.out.println("\n🆕 CRIANDO NOVA INSTÂNCIA: " + receiverId);
        System.out.println("==============================");
        
        try {
            boolean success = gateway.createNewReceiverInstance(receiverId, port);
            
            if (success) {
                System.out.println("✅ Nova instância criada com sucesso");
                System.out.println("📊 Monitore o JMeter - taxa de erro deve DIMINUIR");
                System.out.println("📝 Total de receptores: " + gateway.getRegisteredReceiversCount());
            } else {
                System.out.println("❌ Falha ao criar nova instância");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erro ao criar instância: " + e.getMessage());
        }
    }
    
    private static void performHealthCheck(IoTGateway gateway) {
        System.out.println("\n🏥 EXECUTANDO HEALTH CHECK");
        System.out.println("==========================");
        
        try {
            gateway.performHealthCheck();
            System.out.println("✅ Health check executado - verifique os logs");
            
        } catch (Exception e) {
            System.out.println("❌ Erro no health check: " + e.getMessage());
        }
    }
}