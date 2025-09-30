package br.ufrn.dimap.communication.tcp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.patterns.singleton.IoTGateway;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler para conexões TCP de clientes IoT.
 * Processa mensagens de sensores com compatibilidade JMeter.
 * 
 * Baseado nos exemplos do professor:
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
     * Processa a conexão com o cliente.
     * Modo compatível com JMeter TCP Sampler (fecha após primeira mensagem).
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
                writer.flush();
                logger.info("Cliente TCP " + clientAddress + " solicitou desconexão");
                break;
            }
            
            // Processar mensagem IoT
            processIoTMessage(inputLine, writer, clientAddress);
            
            // Para compatibilidade com JMeter TCP Sampler, fechamos após processar mensagem
            if (inputLine.startsWith("SENSOR_") || inputLine.startsWith("HEARTBEAT")) {
                logger.info("Fechando conexão após processar mensagem IoT para compatibilidade JMeter: " + inputLine.substring(0, Math.min(inputLine.length(), 20)));
                break;
            }
        }
    }
    
    /**
     * Processa uma mensagem IoT recebida via TCP.
     * Segue o padrão estabelecido nos exemplos do professor.
     */
    private void processIoTMessage(String message, PrintWriter writer, String clientAddress) {
        try {
            logger.info("🔄 [TCP] Mensagem recebida de " + clientAddress + ": " + message);
            
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
                writer.flush(); // Força o envio da resposta
                
                logger.info("Mensagem TCP processada com sucesso para " + clientAddress + 
                           " - Sensor: " + iotMessage.getSensorId() + 
                           ", Tipo: " + iotMessage.getType() + 
                           " - Resposta enviada: " + response);
                           
                logger.info("🔄 [TCP] Mensagem processada: " + iotMessage.getMessageId() + 
                           " - Sensor: " + iotMessage.getSensorId() + 
                           " - Tipo: " + iotMessage.getType() + 
                           " - VV: " + iotMessage.getVersionVector() + 
                           " - Origem: " + clientAddress);
                
            } else {
                // Resposta de erro para mensagem inválida
                String errorResponse = TCPProtocolConstants.formatErrorResponse("UNKNOWN", "UNKNOWN", "INVALID_MESSAGE_FORMAT");
                writer.println(errorResponse);
                writer.flush();
                
                logger.warning("Formato de mensagem TCP inválido de " + clientAddress + ": " + message + 
                              " - Resposta de erro enviada: " + errorResponse);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar mensagem TCP de " + clientAddress + ": " + message, e);
            
            // Resposta de erro para falha no processamento
            try {
                String errorResponse = TCPProtocolConstants.formatErrorResponse("UNKNOWN", "UNKNOWN", "PROCESSING_ERROR");
                writer.println(errorResponse);
                writer.flush();
                
                logger.warning("Erro no processamento - Resposta de erro enviada: " + errorResponse);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Erro ao enviar resposta de erro para " + clientAddress, ex);
            }
        }
    }
    
    /**
     * Para o handler e limpa recursos.
     */
    public void stop() {
        isRunning.set(false);
        closeClientSocket();
    }
    
    /**
     * Fecha o socket do cliente de forma segura.
     */
    private void closeClientSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erro ao fechar socket do cliente: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se o handler está ativo.
     */
    public boolean isRunning() {
        return isRunning.get() && clientSocket != null && !clientSocket.isClosed();
    }
    
    /**
     * Obtém informações do cliente conectado.
     */
    public String getClientInfo() {
        if (clientSocket != null) {
            return clientSocket.getRemoteSocketAddress().toString();
        }
        return "N/A";
    }
}