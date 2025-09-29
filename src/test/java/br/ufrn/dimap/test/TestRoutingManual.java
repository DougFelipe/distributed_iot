package br.ufrn.dimap.test;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Teste simples para validar roteamento Gateway -> Data Receivers
 */
public class TestRoutingManual {
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Iniciando teste manual de roteamento...");
        
        // Criar mensagem de teste
        ConcurrentHashMap<String, Integer> vv = new ConcurrentHashMap<>();
        vv.put("TEMP_SENSOR_01", 1);
        
        IoTMessage testMessage = new IoTMessage(
            "TEMP_SENSOR_01",
            IoTMessage.MessageType.SENSOR_DATA,
            "Teste de roteamento",
            25.5,
            IoTSensor.SensorType.TEMPERATURE.name(),
            vv
        );
        
        // Enviar para Gateway (porta 9090)
        try (DatagramSocket socket = new DatagramSocket()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(testMessage);
            
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, 
                InetAddress.getByName("localhost"), 9090
            );
            
            socket.send(packet);
            System.out.println("✅ Mensagem enviada para Gateway (9090): " + testMessage.getMessageId());
            
            // Aguardar um pouco
            Thread.sleep(2000);
            
            System.out.println("🔍 Verifique os logs do sistema para ver o roteamento!");
        }
    }
}