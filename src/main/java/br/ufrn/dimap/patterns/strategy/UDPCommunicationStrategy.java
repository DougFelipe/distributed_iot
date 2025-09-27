package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Strategy Pattern - Implementação UDP para comunicação IoT
 * 
 * Implementa comunicação nativa UDP baseada no sistema funcional existente.
 * Integra com o NativeUDPIoTServer mantendo compatibilidade total.
 * 
 * Características:
 * - Serialização nativa Java
 * - Thread-safe com ExecutorService
 * - Integração com Version Vector
 * - Logs estruturados
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public class UDPCommunicationStrategy implements CommunicationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(UDPCommunicationStrategy.class);
    
    private DatagramSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, IoTSensor> connectedSensors = new ConcurrentHashMap<>();
    
    // Callback para processar mensagens recebidas
    private MessageProcessor messageProcessor;
    
    /**
     * Interface para callback de processamento de mensagens
     */
    public interface MessageProcessor {
        void processMessage(IoTMessage message, String senderHost, int senderPort);
    }
    
    public UDPCommunicationStrategy() {
        this.executorService = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "UDP-Strategy-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Define o processador de mensagens
     */
    public void setMessageProcessor(MessageProcessor processor) {
        this.messageProcessor = processor;
    }
    
    @Override
    public void startServer(int port) throws Exception {
        if (running.get()) {
            logger.warn("🔄 Servidor UDP Strategy já está executando na porta {}", port);
            return;
        }
        
        try {
            serverSocket = new DatagramSocket(port);
            running.set(true);
            
            logger.info("🚀 UDP Strategy Server iniciado na porta {}", port);
            
            // Thread para receber mensagens
            executorService.submit(() -> {
                byte[] buffer = new byte[65536];
                
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);
                        
                        // Processar mensagem em thread separada
                        executorService.submit(() -> handleReceivedPacket(packet));
                        
                    } catch (SocketTimeoutException e) {
                        // Timeout normal, continuar
                    } catch (Exception e) {
                        if (running.get()) {
                            logger.error("❌ Erro ao receber pacote UDP: {}", e.getMessage());
                        }
                    }
                }
            });
            
            // Configurar timeout para recepção
            serverSocket.setSoTimeout(1000);
            
        } catch (Exception e) {
            running.set(false);
            logger.error("❌ Erro ao iniciar servidor UDP Strategy: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public void stopServer() {
        if (!running.get()) {
            return;
        }
        
        logger.info("🛑 Parando UDP Strategy Server...");
        running.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("✅ UDP Strategy Server parado com sucesso");
    }
    
    @Override
    public boolean sendMessage(IoTMessage message, String host, int port) {
        if (!running.get()) {
            logger.warn("⚠️ Tentativa de envio com servidor UDP Strategy parado");
            return false;
        }
        
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            // Serializar mensagem
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();
            
            byte[] data = baos.toByteArray();
            
            // Enviar pacote
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            clientSocket.send(packet);
            
            logger.debug("📤 Mensagem UDP enviada para {}:{} - Tipo: {}", 
                         host, port, message.getType());
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar mensagem UDP para {}:{}: {}", 
                         host, port, e.getMessage());
            return false;
        }
    }
    
    @Override
    public void processMessage(IoTMessage message, String senderHost, int senderPort) {
        // Delegação para o processador configurado
        if (messageProcessor != null) {
            messageProcessor.processMessage(message, senderHost, senderPort);
        } else {
            logger.debug("📨 Mensagem recebida de {}:{} - Tipo: {} (sem processador)",
                         senderHost, senderPort, message.getType());
        }
    }
    
    @Override
    public String getProtocolName() {
        return "UDP";
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Manipula pacote recebido via UDP
     */
    private void handleReceivedPacket(DatagramPacket packet) {
        try {
            // Deserializar mensagem
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);
            IoTMessage message = (IoTMessage) ois.readObject();
            
            String senderHost = packet.getAddress().getHostAddress();
            int senderPort = packet.getPort();
            
            logger.debug("📬 Pacote UDP recebido de {}:{} - Tipo: {} [Código: {}] - Sensor: {} - Valor: {} {} - Timestamp: {}", 
                         senderHost, senderPort, message.getType(), message.getType().getCode(), 
                         message.getSensorId(), message.getSensorValue(), message.getSensorType(),
                         message.getTimestamp());
            
            // Processar mensagem
            processMessage(message, senderHost, senderPort);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao processar pacote UDP: {}", e.getMessage());
        }
    }
    
    /**
     * Retorna estatísticas do servidor UDP
     */
    public String getStats() {
        return String.format("UDP Strategy - Running: %s, Sensors: %d", 
                           running.get(), connectedSensors.size());
    }
}