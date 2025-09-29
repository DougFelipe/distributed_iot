package br.ufrn.dimap.communication.tcp;

/**
 * Constantes TCP para comunicação IoT
 * Baseado nos exemplos do professor em TCPServer.java
 */
public class TCPProtocolConstants {
    
    // Separadores de protocolo (compatível com UDP)
    public static final String FIELD_SEPARATOR = "|";
    public static final String LINE_TERMINATOR = "\n";
    
    // Tipos de mensagem (compatível com UDP)
    public static final String MSG_SENSOR_REGISTER = "SENSOR_REGISTER";
    public static final String MSG_SENSOR_DATA = "SENSOR_DATA";
    public static final String MSG_HEARTBEAT = "HEARTBEAT";
    
    // Respostas padrão
    public static final String RESPONSE_SUCCESS = "SUCCESS";
    public static final String RESPONSE_ERROR = "ERROR";
    public static final String RESPONSE_PROCESSED = "PROCESSED";
    
    // Configurações de conexão
    public static final int DEFAULT_SOCKET_TIMEOUT = 5000; // 5 segundos
    public static final int DEFAULT_BACKLOG = 300;
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    
    // Thread pool
    public static final int DEFAULT_THREAD_POOL_SIZE = 50;
    public static final int MAX_THREAD_POOL_SIZE = 300;
    
    // Formato de resposta TCP (compatível com UDP)
    public static final String RESPONSE_FORMAT = "%s|%s|%s|%s"; // STATUS|MSG_ID|SENSOR_ID|ACTION
    
    private TCPProtocolConstants() {
        // Utility class
    }
    
    /**
     * Formatar resposta de sucesso TCP
     */
    public static String formatSuccessResponse(String messageId, String sensorId) {
        return String.format(RESPONSE_FORMAT, RESPONSE_SUCCESS, messageId, sensorId, RESPONSE_PROCESSED);
    }
    
    /**
     * Formatar resposta de erro TCP
     */
    public static String formatErrorResponse(String messageId, String sensorId, String error) {
        return String.format(RESPONSE_FORMAT, RESPONSE_ERROR, messageId, sensorId, error);
    }
}