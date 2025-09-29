package br.ufrn.dimap.communication.tcp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.patterns.singleton.IoTGateway;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manipulador de cliente TCP baseado nos exemplos do professor.
 * Processa conexões TCP de clientes de forma thread-safe.
 * 
 * Inspirado em:
 * - exemplo_tcp/TCPServer.java (processamento de sockets)
 * - exemplo_tcp/ProcessPayload.java (lógica de processamento)
 * - exemplo_tcp_virtual_threads/TCPServerVirtualThreads.java (gerenciamento de conexões)
 */
public class TCPClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(TCPClientHandler.class.getName());
    
    private final Socket clientSocket;
    private final IoTGateway gateway;
    private final TCPMessageProcessor messageProcessor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    
    // Configurações de timeout baseadas nos exemplos do professor
    private static final int SO_TIMEOUT = 30000; // 30 segundos
    
    public TCPClientHandler(Socket clientSocket, IoTGateway gateway) {
        this.clientSocket = clientSocket;
        this.gateway = gateway;
        this.messageProcessor = new TCPMessageProcessor();
        
        try {
            // Configurar timeout do socket
            clientSocket.setSoTimeout(SO_TIMEOUT);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erro ao configurar timeout do socket: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        logger.info("Iniciando processamento de cliente TCP: " + clientAddress);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            processClientConnection(reader, writer, clientAddress);
            
        } catch (SocketTimeoutException e) {
            logger.log(Level.INFO, "Timeout na conexão TCP com cliente: " + clientAddress);
        } catch (IOException e) {
            if (isRunning.get()) {
                logger.log(Level.WARNING, "Erro de I/O na conexão TCP com cliente " + clientAddress + ": " + e.getMessage());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro inesperado no processamento do cliente TCP " + clientAddress, e);
        } finally {
            closeClientSocket();
            logger.info("Conexão TCP com cliente " + clientAddress + " finalizada");
        }
    }
    
    /**
     * Processa a conexão com o cliente de forma contínua.
     * Baseado no padrão dos exemplos do professor para manter conexão ativa.
     */
    private void processClientConnection(BufferedReader reader, PrintWriter writer, String clientAddress) 
            throws IOException {
        
        String inputLine;
        while (isRunning.get() && (inputLine = reader.readLine()) != null) {
            inputLine = inputLine.trim();
            
            if (inputLine.isEmpty()) {
                continue;
            }
            
            // Processar comando de desconexão
            if ("DISCONNECT".equalsIgnoreCase(inputLine) || "EXIT".equalsIgnoreCase(inputLine)) {
                writer.println("OK|DISCONNECTED");
                logger.info("Cliente TCP " + clientAddress + " solicitou desconexão");
                break;
            }
            
            // Processar mensagem IoT
            processIoTMessage(inputLine, writer, clientAddress);
        }
    }
    
    /**
     * Processa uma mensagem IoT recebida via TCP.
     * Segue o padrão estabelecido nos exemplos do professor.
     */
    private void processIoTMessage(String message, PrintWriter writer, String clientAddress) {
        try {
            logger.fine("Processando mensagem TCP de " + clientAddress + ": " + message);
            
            // Processar mensagem usando o processador TCP
            IoTMessage iotMessage = messageProcessor.processIncomingMessage(message, clientAddress);
            
            if (iotMessage != null) {
                // Rotear mensagem através do gateway
                if (gateway != null) {
                    gateway.routeToDataReceiver(iotMessage, clientAddress, 0);
                }
                
                // Gerar resposta
                String response = messageProcessor.generateResponse(iotMessage, true);
                writer.println(response);
                
                logger.info("Mensagem TCP processada com sucesso para " + clientAddress + 
                           " - Sensor: " + iotMessage.getSensorId() + 
                           ", Tipo: " + iotMessage.getType());
            } else {
                // Mensagem inválida
                String errorResponse = messageProcessor.generateErrorResponse("INVALID_MESSAGE_FORMAT");
                writer.println(errorResponse);
                logger.warning("Formato de mensagem TCP inválido de " + clientAddress + ": " + message);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar mensagem TCP de " + clientAddress, e);
            
            // Enviar resposta de erro
            try {
                String errorResponse = messageProcessor.generateErrorResponse("PROCESSING_ERROR");
                writer.println(errorResponse);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Erro ao enviar resposta de erro TCP", ex);
            }
        }
    }
    
    /**
     * Para o processamento do cliente de forma segura.
     */
    public void stop() {
        isRunning.set(false);
        closeClientSocket();
    }
    
    /**
     * Fecha o socket do cliente de forma segura.
     */
    private void closeClientSocket() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Erro ao fechar socket do cliente TCP: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica se o cliente ainda está conectado.
     */
    public boolean isConnected() {
        return clientSocket != null && 
               clientSocket.isConnected() && 
               !clientSocket.isClosed() && 
               isRunning.get();
    }
    
    /**
     * Obtém informações sobre o cliente.
     */
    public String getClientInfo() {
        if (clientSocket != null) {
            return clientSocket.getRemoteSocketAddress().toString();
        }
        return "Cliente desconhecido";
    }
}