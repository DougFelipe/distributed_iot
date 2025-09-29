package br.ufrn.dimap.communication.http;

/**
 * Constantes HTTP para comunicação IoT
 * Baseado no exemplo do professor em WebServer.java
 */
public class HTTPProtocolConstants {
    
    // Status Codes HTTP
    public static final int HTTP_OK = 200;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_INTERNAL_ERROR = 500;
    
    // Headers HTTP
    public static final String HEADER_CONTENT_TYPE = "Content-Type: application/json\r\n";
    public static final String HEADER_SERVER = "Server: IoT-Gateway\r\n";
    public static final String HEADER_CORS = "Access-Control-Allow-Origin: *\r\n";
    public static final String HEADER_CONNECTION_CLOSE = "Connection: close\r\n";
    
    // Status Lines
    public static final String STATUS_200 = "HTTP/1.1 200 OK\r\n";
    public static final String STATUS_400 = "HTTP/1.1 400 Bad Request\r\n";
    public static final String STATUS_405 = "HTTP/1.1 405 Method Not Allowed\r\n";
    public static final String STATUS_500 = "HTTP/1.1 500 Internal Server Error\r\n";
    
    // IoT Endpoints
    public static final String ENDPOINT_SENSOR_REGISTER = "/iot/sensor/register";
    public static final String ENDPOINT_SENSOR_DATA = "/iot/sensor/data";
    public static final String ENDPOINT_HEALTH = "/health";
    
    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    
    private HTTPProtocolConstants() {
        // Utility class
    }
}