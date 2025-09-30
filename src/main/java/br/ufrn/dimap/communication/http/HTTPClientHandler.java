package br.ufrn.dimap.communication.http;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.patterns.singleton.IoTGateway;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Handler para conexões HTTP IoT
 * Baseado no exemplo ClientHandler.java do professor com adaptações para IoT
 */
public class HTTPClientHandler implements Runnable {
    private final Socket clientSocket;
    private final HTTPRequestParser parser;
    private final HTTPResponseBuilder responseBuilder;
    private final IoTGateway gateway;
    
    public HTTPClientHandler(Socket clientSocket, IoTGateway gateway) {
        this.clientSocket = clientSocket;
        this.parser = new HTTPRequestParser();
        this.responseBuilder = new HTTPResponseBuilder();
        this.gateway = gateway;
    }
    
    @Override
    public void run() {
        System.out.println("🌐 HTTPClientHandler iniciado para " + clientSocket.getRemoteSocketAddress());
        
        try {
            handleRequest();
        } catch (Exception e) {
            System.err.println("❌ Erro no HTTPClientHandler: " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("❌ Erro ao fechar socket: " + e.getMessage());
            }
        }
        
        System.out.println("🌐 HTTPClientHandler finalizado para " + clientSocket.getRemoteSocketAddress());
    }
    
    private void handleRequest() throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            
            // Parse da requisição HTTP
            HTTPRequestParser.HTTPRequest request = parser.parseRequest(in);
            
            if (!request.isValid) {
                responseBuilder.sendErrorResponse(out, 400, "Invalid HTTP request");
                return;
            }
            
            System.out.println("📥 HTTP " + request.method + " " + request.path + " de " + 
                             clientSocket.getRemoteSocketAddress());
            
            // Roteamento por método e path
            if ("GET".equals(request.method)) {
                handleGetRequest(request, out);
            } else if ("POST".equals(request.method)) {
                handlePostRequest(request, out);
            } else {
                responseBuilder.sendMethodNotAllowedResponse(out);
            }
            
        } catch (IOException e) {
            System.err.println("❌ Erro de I/O no HTTPClientHandler: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Processa requisições GET (principalmente health check)
     */
    private void handleGetRequest(HTTPRequestParser.HTTPRequest request, DataOutputStream out) 
            throws IOException {
        
        if (request.path.equals("/health") || request.path.equals("/")) {
            // Health check endpoint - SEMPRE retorna healthy
            responseBuilder.sendHealthResponse(out, true);
            
        } else {
            // Endpoint não encontrado
            responseBuilder.sendErrorResponse(out, 404, "Endpoint not found: " + request.path);
        }
    }
    
    /**
     * Processa requisições POST (dados IoT)
     */
    private void handlePostRequest(HTTPRequestParser.HTTPRequest request, DataOutputStream out) 
            throws IOException {
        
        // Verificar se é endpoint IoT válido
        if (!isIoTEndpoint(request.path)) {
            responseBuilder.sendErrorResponse(out, 404, "IoT endpoint not found: " + request.path);
            return;
        }
        
        // Parse da mensagem IoT
        IoTMessage iotMessage = parser.parseToIoTMessage(request);
        
        if (iotMessage == null) {
            responseBuilder.sendErrorResponse(out, 400, "Invalid IoT message format");
            return;
        }
        
        try {
            // Processar mensagem através do Gateway (PROXY pattern)
            System.out.println("🔄 [HTTP-PROXY] Processando mensagem: " + iotMessage.getMessageId() + 
                             " - Sensor: " + iotMessage.getSensorId() + 
                             " - Tipo: " + iotMessage.getType());
            
            // Simular processamento pelo gateway
            // Na implementação real, seria: gateway.processMessage(iotMessage)
            boolean processed = true; // Simplificado para esta versão
            
            if (processed) {
                responseBuilder.sendSuccessResponse(out, iotMessage);
                System.out.println("✅ [HTTP-PROXY] Mensagem processada com sucesso: " + iotMessage.getMessageId());
            } else {
                responseBuilder.sendErrorResponse(out, 500, "Failed to process IoT message");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar mensagem IoT: " + e.getMessage());
            responseBuilder.sendErrorResponse(out, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se o path é um endpoint IoT válido
     */
    private boolean isIoTEndpoint(String path) {
        return path.startsWith("/iot/") || 
               path.contains("/sensor") || 
               path.equals("/register") || 
               path.equals("/data");
    }
}