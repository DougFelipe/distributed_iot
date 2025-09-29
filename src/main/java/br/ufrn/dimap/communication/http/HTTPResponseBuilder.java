package br.ufrn.dimap.communication.http;

import br.ufrn.dimap.core.IoTMessage;

import java.io.DataOutputStream;
import java.io.IOException;

import static br.ufrn.dimap.communication.http.HTTPProtocolConstants.*;

/**
 * Builder para respostas HTTP IoT
 * Baseado no exemplo ClientHandler.java do professor
 */
public class HTTPResponseBuilder {
    
    /**
     * Envia resposta HTTP de sucesso com dados IoT
     */
    public void sendSuccessResponse(DataOutputStream out, IoTMessage processedMessage) throws IOException {
        String jsonResponse = buildSuccessJson(processedMessage);
        sendResponse(out, HTTP_OK, STATUS_200, jsonResponse);
    }
    
    /**
     * Envia resposta HTTP de erro
     */
    public void sendErrorResponse(DataOutputStream out, int statusCode, String errorMessage) throws IOException {
        String statusLine = getStatusLine(statusCode);
        String jsonResponse = buildErrorJson(statusCode, errorMessage);
        sendResponse(out, statusCode, statusLine, jsonResponse);
    }
    
    /**
     * Envia resposta HTTP de health check
     */
    public void sendHealthResponse(DataOutputStream out, boolean healthy) throws IOException {
        String jsonResponse = healthy ? 
            "{\"status\":\"UP\",\"service\":\"IoT-Gateway\"}" :
            "{\"status\":\"DOWN\",\"service\":\"IoT-Gateway\"}";
        int statusCode = healthy ? HTTP_OK : HTTP_INTERNAL_ERROR;
        String statusLine = getStatusLine(statusCode);
        sendResponse(out, statusCode, statusLine, jsonResponse);
    }
    
    /**
     * Envia resposta Method Not Allowed para métodos não suportados
     */
    public void sendMethodNotAllowedResponse(DataOutputStream out) throws IOException {
        String jsonResponse = buildErrorJson(HTTP_METHOD_NOT_ALLOWED, "Only POST method allowed for IoT endpoints");
        sendResponse(out, HTTP_METHOD_NOT_ALLOWED, STATUS_405, jsonResponse);
    }
    
    /**
     * Método interno para enviar resposta HTTP completa
     */
    private void sendResponse(DataOutputStream out, int statusCode, String statusLine, String jsonResponse) 
            throws IOException {
        try {
            // Status line
            out.writeBytes(statusLine);
            
            // Headers
            out.writeBytes(HEADER_SERVER);
            out.writeBytes(HEADER_CONTENT_TYPE);
            out.writeBytes(HEADER_CORS);
            out.writeBytes(HEADER_CONNECTION_CLOSE);
            out.writeBytes("Content-Length: " + jsonResponse.getBytes().length + "\r\n");
            
            // Empty line between headers and body
            out.writeBytes("\r\n");
            
            // Body
            out.writeBytes(jsonResponse);
            out.flush();
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao enviar resposta HTTP: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Constrói JSON de resposta de sucesso
     */
    private String buildSuccessJson(IoTMessage message) {
        return String.format(
            "{\"status\":\"SUCCESS\",\"messageId\":\"%s\",\"sensor\":\"%s\",\"type\":\"%s\",\"timestamp\":\"%s\",\"processed\":true}",
            message.getMessageId(),
            message.getSensorId(), 
            message.getType().name(),
            message.getTimestamp().toString()
        );
    }
    
    /**
     * Constrói JSON de resposta de erro
     */
    private String buildErrorJson(int statusCode, String errorMessage) {
        return String.format(
            "{\"status\":\"ERROR\",\"code\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}",
            statusCode,
            errorMessage,
            java.time.LocalDateTime.now().toString()
        );
    }
    
    /**
     * Obtém status line para código de status
     */
    private String getStatusLine(int statusCode) {
        switch (statusCode) {
            case HTTP_OK:
                return STATUS_200;
            case HTTP_BAD_REQUEST:
                return STATUS_400;
            case HTTP_METHOD_NOT_ALLOWED:
                return STATUS_405;
            case HTTP_INTERNAL_ERROR:
                return STATUS_500;
            default:
                return STATUS_500;
        }
    }
}