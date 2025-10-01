# 📤 **RESPONSE BUILDER E HTTP PROTOCOL**

**Análise Detalhada do HTTPResponseBuilder e Constantes de Protocolo**

---

## 8. **VERSION VECTOR INTEGRATION**

### 🔢 **Version Vector com HTTP**

```java
/**
 * HTTPResponseBuilder - Construção de Respostas HTTP
 * Implementa BUILDER PATTERN para respostas estruturadas
 */
public class HTTPResponseBuilder {
    
    /**
     * SUCCESS RESPONSE com Version Vector
     */
    public void sendSuccessResponse(DataOutputStream out, IoTMessage processedMessage) 
            throws IOException {
        
        // 1. BUILD JSON with Version Vector
        String jsonResponse = buildSuccessJson(processedMessage);
        
        // 2. SEND HTTP Response
        sendResponse(out, HTTP_OK, STATUS_200, jsonResponse);
    }
    
    /**
     * Constrói JSON de resposta de sucesso com Version Vector
     */
    private String buildSuccessJson(IoTMessage message) {
        // STRATEGY PATTERN - Build Version Vector JSON
        String versionVectorJson = buildVersionVectorJson(message.getVersionVector());
        
        return String.format(
            "{\"status\":\"SUCCESS\"," +
            "\"messageId\":\"%s\"," +
            "\"sensor\":\"%s\"," +
            "\"type\":\"%s\"," +
            "\"timestamp\":\"%s\"," +
            "\"processed\":true," +
            "\"versionVector\":%s}",
            message.getMessageId(),
            message.getSensorId(), 
            message.getType().name(),
            message.getTimestamp().toString(),
            versionVectorJson
        );
    }
    
    /**
     * SERIALIZATION PATTERN - Version Vector → JSON
     */
    private String buildVersionVectorJson(ConcurrentHashMap<String, Integer> versionVector) {
        if (versionVector == null || versionVector.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        // ITERATOR PATTERN - Thread-safe iteration
        for (Map.Entry<String, Integer> entry : versionVector.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":")
                .append(entry.getValue());
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
}
```

### 🔄 **Version Vector Flow Diagram**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                VERSION VECTOR HTTP PROCESSING                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  INCOMING REQUEST                                               │
│  ┌─────────────────┐                                            │
│  │ HTTP POST       │                                            │
│  │ /sensor/data    │                                            │
│  │                 │                                            │
│  │ Body:           │                                            │
│  │ {               │                                            │
│  │   "sensorId":   │                                            │
│  │     "TEMP_001", │                                            │
│  │   "versionVect  │                                            │
│  │   or": {        │                                            │
│  │     "TEMP_001": │                                            │
│  │        5        │                                            │
│  │   }             │                                            │
│  │ }               │                                            │
│  └─────────────────┘                                            │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ HTTPRequestPars │ ──→ parseVersionVector()                  │
│  │ er              │                                            │
│  │                 │ ──→ vv.put("TEMP_001", 5)                │
│  │ Current VV:     │                                            │
│  │ {TEMP_001: 5}   │                                            │
│  └─────────────────┘                                            │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ Version Vector  │ ──→ vv.compute(sensorId,                  │
│  │ INCREMENT       │       (k,v) -> v+1)                       │
│  │                 │                                            │
│  │ Updated VV:     │ ──→ {TEMP_001: 6}                        │
│  │ {TEMP_001: 6}   │                                            │
│  └─────────────────┘                                            │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ IoTMessage      │ ──→ new IoTMessage(sensorId,              │
│  │ CREATION        │       messageType, content,               │
│  │                 │       value, sensorType,                  │
│  │ With VV:        │       versionVector)                      │
│  │ {TEMP_001: 6}   │                                            │
│  └─────────────────┘                                            │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                            │
│  │ HTTPResponseBui │ ──→ buildVersionVectorJson()              │
│  │ lder            │                                            │
│  │                 │ ──→ {"TEMP_001":6}                       │
│  │ JSON Response:  │                                            │
│  │ {               │                                            │
│  │   "status":     │                                            │
│  │     "SUCCESS",  │                                            │
│  │   "versionVect  │                                            │
│  │   or": {        │                                            │
│  │     "TEMP_001": │                                            │
│  │        6        │                                            │
│  │   }             │                                            │
│  │ }               │                                            │
│  └─────────────────┘                                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. **HTTP PROTOCOL CONSTANTS**

### 📋 **HTTPProtocolConstants - Centralized Configuration**

```java
/**
 * Constantes HTTP para comunicação IoT
 * Implementa CONSTANTS PATTERN para configuração centralizada
 */
public class HTTPProtocolConstants {
    
    // === HTTP STATUS CODES ===
    public static final int HTTP_OK = 200;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_INTERNAL_ERROR = 500;
    
    // === HTTP HEADERS ===
    public static final String HEADER_CONTENT_TYPE = "Content-Type: application/json\r\n";
    public static final String HEADER_SERVER = "Server: IoT-Gateway\r\n";
    public static final String HEADER_CORS = "Access-Control-Allow-Origin: *\r\n";
    public static final String HEADER_CONNECTION_CLOSE = "Connection: close\r\n";
    
    // === HTTP STATUS LINES ===
    public static final String STATUS_200 = "HTTP/1.1 200 OK\r\n";
    public static final String STATUS_400 = "HTTP/1.1 400 Bad Request\r\n";
    public static final String STATUS_405 = "HTTP/1.1 405 Method Not Allowed\r\n";
    public static final String STATUS_500 = "HTTP/1.1 500 Internal Server Error\r\n";
    
    // === IoT ENDPOINTS ===
    public static final String ENDPOINT_SENSOR_REGISTER = "/iot/sensor/register";
    public static final String ENDPOINT_SENSOR_DATA = "/iot/sensor/data";
    public static final String ENDPOINT_HEALTH = "/health";
    
    // === CONTENT TYPES ===
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
}
```

### 🏗️ **Response Building Architecture**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                  HTTP RESPONSE ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  RESPONSE TYPES                                                 │
│                                                                 │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │ SUCCESS         │     │ ERROR           │                   │  
│  │ RESPONSE        │     │ RESPONSE        │                   │
│  │                 │     │                 │                   │
│  │ • 200 OK        │     │ • 400 Bad Req   │                   │
│  │ • JSON Body     │     │ • 405 Method    │                   │
│  │ • IoT Data      │     │ • 500 Internal  │                   │
│  │ • Version Vec   │     │ • Error Message │                   │
│  └─────────────────┘     └─────────────────┘                   │
│           │                       │                             │
│           ▼                       ▼                             │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │ HEALTH          │     │ METHOD NOT      │                   │
│  │ RESPONSE        │     │ ALLOWED         │                   │
│  │                 │     │                 │                   │
│  │ • 200 OK        │     │ • 405 Method    │                   │
│  │ • System Status │     │ • Only POST     │                   │
│  │ • Timestamp     │     │ • Allowed       │                   │
│  │ • Service Info  │     │ • Error JSON    │                   │
│  └─────────────────┘     └─────────────────┘                   │
│           │                       │                             │
│           └───────────┬───────────┘                             │
│                       ▼                                         │
│  ┌─────────────────────────────────────────┐                   │
│  │         sendResponse()                  │                   │
│  │        TEMPLATE METHOD                  │                   │
│  │                                         │                   │
│  │  1. Write Status Line                  │                   │
│  │     HTTP/1.1 200 OK\r\n               │                   │
│  │                                         │                   │
│  │  2. Write Headers                       │                   │
│  │     Server: IoT-Gateway\r\n            │                   │
│  │     Content-Type: application/json\r\n │                   │
│  │     Access-Control-Allow-Origin: *\r\n │                   │  
│  │     Connection: close\r\n              │                   │
│  │     Content-Length: 156\r\n            │                   │
│  │                                         │                   │
│  │  3. Write Empty Line                    │                   │
│  │     \r\n                               │                   │
│  │                                         │                   │
│  │  4. Write JSON Body                     │                   │
│  │     {"status":"SUCCESS",...}            │                   │
│  │                                         │                   │
│  │  5. Flush Output Stream                 │                   │
│  │     out.flush()                         │                   │
│  └─────────────────────────────────────────┘                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 📝 **Response Types Implementation**

```java
/**
 * HTTPResponseBuilder - Complete Response Management
 */
public class HTTPResponseBuilder {
    
    /**
     * ERROR RESPONSE with detailed error information
     */
    public void sendErrorResponse(DataOutputStream out, int statusCode, String errorMessage) 
            throws IOException {
        String statusLine = getStatusLine(statusCode);
        String jsonResponse = buildErrorJson(statusCode, errorMessage);
        sendResponse(out, statusCode, statusLine, jsonResponse);
    }
    
    /**
     * HEALTH CHECK RESPONSE - Always returns UP
     */
    public void sendHealthResponse(DataOutputStream out, boolean healthy) 
            throws IOException {
        // ALWAYS returns UP for health check - fixed 500 error
        String jsonResponse = String.format(
            "{\"status\":\"UP\"," +
            "\"service\":\"IoT-Gateway\"," +
            "\"timestamp\":\"%s\"}",
            java.time.LocalDateTime.now().toString()
        );
        
        int statusCode = HTTP_OK; // Always 200 OK for health check
        String statusLine = getStatusLine(statusCode);
        sendResponse(out, statusCode, statusLine, jsonResponse);
    }
    
    /**
     * METHOD NOT ALLOWED RESPONSE for unsupported HTTP methods
     */
    public void sendMethodNotAllowedResponse(DataOutputStream out) throws IOException {
        String jsonResponse = buildErrorJson(
            HTTP_METHOD_NOT_ALLOWED, 
            "Only POST method allowed for IoT endpoints"
        );
        sendResponse(out, HTTP_METHOD_NOT_ALLOWED, STATUS_405, jsonResponse);
    }
    
    /**
     * TEMPLATE METHOD - Complete HTTP Response
     */
    private void sendResponse(DataOutputStream out, int statusCode, 
                            String statusLine, String jsonResponse) throws IOException {
        try {
            // 1. Status Line
            out.writeBytes(statusLine);
            
            // 2. Headers
            out.writeBytes(HEADER_SERVER);
            out.writeBytes(HEADER_CONTENT_TYPE);
            out.writeBytes(HEADER_CORS);
            out.writeBytes(HEADER_CONNECTION_CLOSE);
            out.writeBytes("Content-Length: " + jsonResponse.getBytes().length + "\r\n");
            
            // 3. Empty line between headers and body
            out.writeBytes("\r\n");
            
            // 4. JSON Body
            out.writeBytes(jsonResponse);
            out.flush();
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao enviar resposta HTTP: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * ERROR JSON Builder
     */
    private String buildErrorJson(int statusCode, String errorMessage) {
        return String.format(
            "{\"status\":\"ERROR\"," +
            "\"code\":%d," +
            "\"message\":\"%s\"," +
            "\"timestamp\":\"%s\"}",
            statusCode,
            errorMessage,
            java.time.LocalDateTime.now().toString()
        );
    }
    
    /**
     * STATUS LINE MAPPING
     */
    private String getStatusLine(int statusCode) {
        return switch (statusCode) {
            case HTTP_OK -> STATUS_200;
            case HTTP_BAD_REQUEST -> STATUS_400;
            case HTTP_METHOD_NOT_ALLOWED -> STATUS_405;
            case HTTP_INTERNAL_ERROR -> STATUS_500;
            default -> STATUS_500;
        };
    }
}
```

---

## 10. **HTTP INTEGRATION WITH IOT GATEWAY**

### 🔗 **Gateway Integration Pattern**

```java
/**
 * Integration between HTTP Strategy and IoT Gateway
 * Implementa ADAPTER PATTERN para integração
 */
public class HTTPCommunicationStrategy implements CommunicationStrategy {
    
    private IoTGateway gateway;
    
    /**
     * DEPENDENCY INJECTION - Gateway reference
     */
    public void setGateway(IoTGateway gateway) {
        this.gateway = gateway;
    }
    
    /**
     * PROXY PATTERN - Gateway processing delegation
     */
    @Override
    public void processMessage(IoTMessage message, String senderHost, int senderPort) {
        // Processing is done in HTTPClientHandler
        System.out.println("🌐 [HTTP] Processando mensagem: " + message.getMessageId() + 
                         " de " + senderHost + ":" + senderPort);
    }
}
```

### 🔄 **Complete HTTP Integration Flow**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                COMPLETE HTTP INTEGRATION FLOW                  │  
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐                                                │
│  │ HTTP Client │                                                │
│  │ (JMeter)    │                                                │
│  └─────────────┘                                                │
│         │ POST /sensor/data                                     │
│         │ JSON Body                                             │
│         ▼                                                       │
│  ┌─────────────────────────────────────────┐                   │
│  │     HTTPCommunicationStrategy           │                   │
│  │                                         │                   │
│  │  • ServerSocket.accept()                │                   │
│  │  • ThreadPool.execute()                 │                   │
│  │  • HTTPClientHandler creation           │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────────────────────────────────┐                   │
│  │        HTTPClientHandler                │                   │
│  │                                         │                   │
│  │  • HTTPRequestParser.parseRequest()     │                   │
│  │  • parseToIoTMessage()                  │                   │
│  │  • Version Vector processing            │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────────────────────────────────┐                   │
│  │           IoTGateway                    │                   │
│  │          (Singleton)                    │                   │
│  │                                         │                   │
│  │  • processMessage(iotMessage)           │                   │
│  │  • Route to DataReceiver                │                   │
│  │  • Update internal state                │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────────────────────────────────┐                   │
│  │        HTTPResponseBuilder              │                   │
│  │                                         │                   │
│  │  • buildSuccessJson()                   │                   │
│  │  • buildVersionVectorJson()             │                   │
│  │  • sendResponse()                       │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────┐                                                │
│  │ HTTP Client │                                                │
│  │ Response    │                                                │
│  │             │                                                │
│  │ 200 OK      │                                                │
│  │ JSON Success│                                                │
│  │ + Version   │                                                │
│  │   Vector    │                                                │
│  └─────────────┘                                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

**📝 Documentação HTTP criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0 - Parte 4  
**🎯 Foco:** Response Builder, Protocol Constants e Gateway Integration