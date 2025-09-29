package br.ufrn.dimap.communication.http;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTMessage.MessageType;
import br.ufrn.dimap.core.IoTSensor.SensorType;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Parser para requisições HTTP IoT
 * Baseado no exemplo ClientHandler.java do professor
 */
public class HTTPRequestParser {
    
    public static class HTTPRequest {
        public String method;
        public String path;
        public String version;
        public Map<String, String> headers = new HashMap<>();
        public String body;
        public boolean isValid = false;
    }
    
    /**
     * Parse da requisição HTTP completa
     */
    public HTTPRequest parseRequest(BufferedReader reader) throws IOException {
        HTTPRequest request = new HTTPRequest();
        
        // Parse primeira linha: GET /path HTTP/1.1
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) {
            return request;
        }
        
        StringTokenizer tokenizer = new StringTokenizer(requestLine);
        if (tokenizer.countTokens() >= 2) {
            request.method = tokenizer.nextToken();
            request.path = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                request.version = tokenizer.nextToken();
            }
        }
        
        // Parse headers
        String headerLine;
        int contentLength = 0;
        while ((headerLine = reader.readLine()) != null && !headerLine.trim().isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                request.headers.put(headerName.toLowerCase(), headerValue);
                
                if ("content-length".equals(headerName.toLowerCase())) {
                    try {
                        contentLength = Integer.parseInt(headerValue);
                    } catch (NumberFormatException e) {
                        // Ignore invalid content-length
                    }
                }
            }
        }
        
        // Parse body se houver Content-Length
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            request.body = new String(bodyChars, 0, totalRead);
        }
        
        request.isValid = true;
        return request;
    }
    
    /**
     * Converte HTTP request para IoTMessage
     * Suporte para formato JSON simples ou query parameters
     */
    public IoTMessage parseToIoTMessage(HTTPRequest request) {
        if (!request.isValid) {
            return null;
        }
        
        try {
            // Determinar tipo de mensagem pelo endpoint
            MessageType messageType;
            if (request.path.contains("/register")) {
                messageType = MessageType.SENSOR_REGISTER;
            } else if (request.path.contains("/data")) {
                messageType = MessageType.SENSOR_DATA;
            } else {
                return null;
            }
            
            // Parse simple JSON body ou form data
            String sensorId = extractParameter(request, "sensor", "sensorId");
            String sensorTypeStr = extractParameter(request, "type", "sensorType");
            String location = extractParameter(request, "location", "lab");
            String valueStr = extractParameter(request, "value", "reading");
            
            if (sensorId == null || sensorTypeStr == null) {
                return null;
            }
            
            // Validar sensorType (será usado como string no IoTMessage)
            try {
                SensorType.valueOf(sensorTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sensorTypeStr = "TEMPERATURE"; // default se inválido
            }
            
            double value = 0.0;
            if (valueStr != null) {
                try {
                    value = Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    value = 0.0;
                }
            }
            
            String content = location != null ? location : "HTTP-Client";
            
            return new IoTMessage(sensorId, messageType, content, 
                                value, sensorTypeStr, 
                                new java.util.concurrent.ConcurrentHashMap<>());
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao parsear HTTP para IoTMessage: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrai parâmetro do body JSON simples ou query string
     */
    private String extractParameter(HTTPRequest request, String... paramNames) {
        // Try body first (simple JSON-like)
        if (request.body != null && !request.body.trim().isEmpty()) {
            for (String paramName : paramNames) {
                String pattern = "\"" + paramName + "\"";
                int index = request.body.indexOf(pattern);
                if (index >= 0) {
                    int valueStart = request.body.indexOf(":", index);
                    if (valueStart >= 0) {
                        valueStart = request.body.indexOf("\"", valueStart);
                        if (valueStart >= 0) {
                            int valueEnd = request.body.indexOf("\"", valueStart + 1);
                            if (valueEnd > valueStart) {
                                return request.body.substring(valueStart + 1, valueEnd);
                            }
                        }
                    }
                }
            }
        }
        
        // Try query string
        if (request.path.contains("?")) {
            String queryString = request.path.substring(request.path.indexOf("?") + 1);
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    for (String paramName : paramNames) {
                        if (paramName.equals(keyValue[0])) {
                            return keyValue[1];
                        }
                    }
                }
            }
        }
        
        return null;
    }
}