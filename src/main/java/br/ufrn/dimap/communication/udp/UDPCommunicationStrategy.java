package br.ufrn.dimap.communication.udp;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementação da estratégia de comunicação UDP.
 * Implementa o padrão Strategy para comunicação via protocolo UDP.
 */
public class UDPCommunicationStrategy implements CommunicationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(UDPCommunicationStrategy.class);
    private static final int BUFFER_SIZE = 4096;
    
    private final ObjectMapper objectMapper;
    private final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private DatagramSocket serverSocket;
    private MessageHandler messageHandler;
    private int serverPort;
    
    public UDPCommunicationStrategy() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void startServer(int port) throws Exception {
        if (serverRunning.get()) {
            throw new IllegalStateException("Servidor UDP já está rodando na porta " + serverPort);
        }
        
        try {
            serverSocket = new DatagramSocket(port);
            serverPort = port;
            serverRunning.set(true);
            
            // Inicia thread para escutar mensagens
            executorService.submit(this::listenForMessages);
            
            logger.info("Servidor UDP iniciado na porta {}", port);
            
        } catch (SocketException e) {
            throw new Exception("Erro ao iniciar servidor UDP na porta " + port, e);
        }
    }
    
    @Override
    public void stopServer() {
        if (!serverRunning.get()) {
            return;
        }
        
        serverRunning.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        executorService.shutdown();
        logger.info("Servidor UDP parado (porta {})", serverPort);
    }
    
    @Override
    public boolean sendMessage(Message message, String targetHost, int targetPort) {
        try {
            // Serializa a mensagem para JSON
            String jsonMessage = objectMapper.writeValueAsString(message);
            byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);
            
            // Cria o pacote UDP
            InetAddress address = InetAddress.getByName(targetHost);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, targetPort);
            
            // Envia o pacote
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
                logger.debug("Mensagem UDP enviada para {}:{} - Type: {}", 
                    targetHost, targetPort, message.getType());
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem UDP para {}:{} - {}", 
                targetHost, targetPort, e.getMessage());
            return false;
        }
    }
    
    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
        logger.debug("MessageHandler configurado para UDP");
    }
    
    @Override
    public String getProtocolName() {
        return "UDP";
    }
    
    @Override
    public boolean isServerRunning() {
        return serverRunning.get();
    }
    
    /**
     * Thread que escuta mensagens UDP continuamente
     */
    private void listenForMessages() {
        logger.info("Iniciando escuta de mensagens UDP na porta {}", serverPort);
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (serverRunning.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                
                // Processa a mensagem em thread separada
                executorService.submit(() -> processReceivedPacket(packet));
                
            } catch (SocketTimeoutException e) {
                // Timeout normal - continua escutando
            } catch (IOException e) {
                if (serverRunning.get()) {
                    logger.error("Erro ao receber mensagem UDP: {}", e.getMessage());
                }
            }
        }
        
        logger.info("Parou de escutar mensagens UDP");
    }
    
    /**
     * Processa um pacote UDP recebido
     */
    private void processReceivedPacket(DatagramPacket packet) {
        try {
            String jsonData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            Message message = objectMapper.readValue(jsonData, Message.class);
            
            String senderHost = packet.getAddress().getHostAddress();
            int senderPort = packet.getPort();
            
            logger.debug("Mensagem UDP recebida de {}:{} - Type: {}", 
                senderHost, senderPort, message.getType());
            
            // Chama o handler se configurado
            if (messageHandler != null) {
                messageHandler.handleMessage(message, senderHost, senderPort);
            } else {
                logger.warn("Mensagem recebida mas nenhum handler configurado");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem UDP recebida: {}", e.getMessage());
        }
    }
}