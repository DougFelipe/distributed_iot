import java.net.*;
import java.io.*;

public class TesteUDPSimples {
    public static void main(String[] args) {
        try {
            // Criando socket UDP
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(5000); // 5 segundos timeout
            
            // Preparando mensagem
            String mensagem = "SENSOR_DATA|SENSOR_TESTE_JAVA|TEMPERATURE|Lab-DIMAP-JAVA|" + System.currentTimeMillis() + "|25.0";
            byte[] buffer = mensagem.getBytes();
            
            // Enviando para o sistema
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 9090);
            
            System.out.println("🚀 Enviando: " + mensagem);
            long inicio = System.currentTimeMillis();
            socket.send(packet);
            
            // Recebendo resposta
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);
            
            long fim = System.currentTimeMillis();
            String resposta = new String(responsePacket.getData(), 0, responsePacket.getLength());
            
            System.out.println("✅ Resposta: " + resposta);
            System.out.println("⏱️ Tempo: " + (fim - inicio) + "ms");
            
            // Verificando se contém SUCCESS
            if (resposta.contains("SUCCESS")) {
                System.out.println("🎉 TESTE PASSOU! Sistema funciona perfeitamente!");
            } else {
                System.out.println("❌ TESTE FALHOU! Resposta inesperada.");
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.out.println("❌ ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}