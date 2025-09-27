package br.ufrn.dimap.applications;

import br.ufrn.dimap.communication.native_udp.NativeUDPIoTClient;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Demonstração de cliente IoT interativo
 * Baseado nos exemplos UDP nativos
 */
public class IoTClientDemo {
    private static final Logger logger = LoggerFactory.getLogger(IoTClientDemo.class);
    
    public static void main(String[] args) {
        logger.info("🤖 DEMONSTRAÇÃO - Cliente IoT UDP Nativo");
        logger.info("===============================================");
        
        Scanner scanner = new Scanner(System.in);
        NativeUDPIoTClient client = null;
        
        try {
            // Configuração do sensor
            System.out.print("Digite o ID do sensor (ex: TEMP_001): ");
            String sensorId = scanner.nextLine().trim();
            if (sensorId.isEmpty()) sensorId = "DEMO_SENSOR_001";
            
            System.out.println("Tipos de sensores disponíveis:");
            IoTSensor.SensorType[] types = IoTSensor.SensorType.values();
            for (int i = 0; i < types.length; i++) {
                System.out.printf("%d - %s (%s)\n", i + 1, types[i], types[i].getUnit());
            }
            
            System.out.print("Escolha o tipo (1-" + types.length + "): ");
            int typeChoice = scanner.nextInt();
            scanner.nextLine(); // consumir newline
            
            IoTSensor.SensorType sensorType = (typeChoice > 0 && typeChoice <= types.length) 
                ? types[typeChoice - 1] 
                : IoTSensor.SensorType.TEMPERATURE;
            
            System.out.print("Digite a localização (ex: SALA_A): ");
            String location = scanner.nextLine().trim();
            if (location.isEmpty()) location = "DEMO_LOCATION";
            
            System.out.print("Endereço do servidor (localhost): ");
            String serverHost = scanner.nextLine().trim();
            if (serverHost.isEmpty()) serverHost = "localhost";
            
            System.out.print("Porta do servidor (9090): ");
            String portStr = scanner.nextLine().trim();
            int serverPort = portStr.isEmpty() ? 9090 : Integer.parseInt(portStr);
            
            // Criar e iniciar cliente
            client = new NativeUDPIoTClient(sensorId, sensorType, location, serverHost, serverPort);
            client.start();
            
            // Menu interativo
            showMenu();
            
            while (true) {
                System.out.print("\nComando: ");
                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "1":
                    case "data":
                        logger.info("📊 Enviando dados do sensor...");
                        // O cliente já envia dados automaticamente
                        break;
                        
                    case "2":
                    case "heartbeat":
                        logger.info("💓 Forçando envio de heartbeat...");
                        // O heartbeat é automático, mas podemos mostrar status
                        break;
                        
                    case "3":
                    case "discovery":
                        logger.info("🔍 Enviando solicitação de descoberta...");
                        client.sendDiscoveryRequest();
                        break;
                        
                    case "4":
                    case "sync":
                        logger.info("🔄 Enviando solicitação de sincronização...");
                        client.sendSyncRequest();
                        break;
                        
                    case "5":
                    case "status":
                        showClientStatus(client);
                        break;
                        
                    case "6":
                    case "menu":
                        showMenu();
                        break;
                        
                    case "7":
                    case "quit":
                    case "exit":
                        logger.info("👋 Encerrando cliente...");
                        return;
                        
                    default:
                        System.out.println("⚠️ Comando inválido. Digite 'menu' para ver opções.");
                }
                
                // Pequena pausa para não sobrecarregar
                Thread.sleep(500);
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro no cliente demo: {}", e.getMessage(), e);
        } finally {
            if (client != null) {
                client.stop();
            }
            scanner.close();
        }
    }
    
    private static void showMenu() {
        System.out.println("\n📋 MENU DE COMANDOS:");
        System.out.println("1 - Enviar dados do sensor");
        System.out.println("2 - Enviar heartbeat");
        System.out.println("3 - Solicitar descoberta");
        System.out.println("4 - Sincronizar version vector");
        System.out.println("5 - Mostrar status");
        System.out.println("6 - Mostrar este menu");
        System.out.println("7 - Sair");
    }
    
    private static void showClientStatus(NativeUDPIoTClient client) {
        System.out.println("\n📊 STATUS DO CLIENTE:");
        System.out.println("   🔸 ID: " + client.getClientId());
        System.out.println("   🔸 Sensor ID: " + client.getSensor().getSensorId());
        System.out.println("   🔸 Tipo: " + client.getSensor().getType());
        System.out.println("   🔸 Localização: " + client.getSensor().getLocation());
        System.out.println("   🔸 Valor atual: " + String.format("%.2f %s", 
                         client.getSensor().getCurrentValue(),
                         client.getSensor().getType().getUnit()));
        System.out.println("   🔸 Status: " + client.getSensor().getStatus());
        System.out.println("   🔸 Ativo: " + (client.isRunning() ? "✅ SIM" : "❌ NÃO"));
        System.out.println("   🔸 Version Vector: " + client.getSensor().getVersionVector());
        System.out.println("   🔸 Último update: " + client.getSensor().getLastUpdate());
        System.out.println("   🔸 Último heartbeat: " + client.getSensor().getLastHeartbeat());
    }
}