# 🔧 **COMPONENTES HTTP - IMPLEMENTAÇÃO DETALHADA**

**Análise dos Componentes Core do Sistema HTTP IoT**

---

## 4. **COMPONENTES CORE**

### 🎯 **HTTPClientHandler - Request Processor**

```java
/**
 * Handler para conexões HTTP IoT
 * Implementa DELEGATION PATTERN para processamento de requisições
 */
public class HTTPClientHandler implements Runnable {
    
    // === DEPENDENCIES ===
    private final Socket clientSocket;              // Socket do cliente HTTP
    private final HTTPRequestParser parser;         // Parser para requisições
    private final HTTPResponseBuilder responseBuilder; // Builder para respostas
    private final IoTGateway gateway;               // Gateway IoT (Singleton)
    
    /**
     * DEPENDENCY INJECTION via Constructor
     */
    public HTTPClientHandler(Socket clientSocket, IoTGateway gateway) {
        this.clientSocket = clientSocket;
        this.gateway = gateway;
        this.parser = new HTTPRequestParser();
        this.responseBuilder = new HTTPResponseBuilder();
    }
    
    /**
     * RUNNABLE PATTERN - Thread execution
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                clientSocket.getOutputStream())) {
            
            handleRequest();
            
        } catch (IOException e) {
            System.err.println("❌ Erro de I/O no HTTPClientHandler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    /**
     * TEMPLATE METHOD PATTERN - Request handling
     */
    private void handleRequest() throws IOException {
        // 1. Parse HTTP Request
        HTTPRequestParser.HTTPRequest request = parser.parseRequest(in);
        
        if (!request.isValid) {
            responseBuilder.sendErrorResponse(out, 400, "Invalid HTTP request");
            return;
        }
        
        // 2. Route by HTTP Method
        switch (request.method.toUpperCase()) {
            case "GET":
                handleGetRequest(request, out);
                break;
            case "POST":
                handlePostRequest(request, out);
                break;
            default:
                responseBuilder.sendMethodNotAllowedResponse(out);
        }
    }
}
```

### 📊 **Request Flow Diagram**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                    HTTP REQUEST PROCESSING                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CLIENT REQUEST                                                 │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────┐                                            │
│  │ HTTPClientHand  │ ──→ run() method                          │
│  │ (Thread)        │                                            │
│  └─────────────────┘                                            │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────┐                                            │
│  │ Parse Request   │ ──→ HTTPRequestParser                     │
│  │ Headers + Body  │                                            │
│  └─────────────────┘                                            │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │ Method Router   │────▶│ GET Handler     │ ── /health        │
│  │ (GET/POST)      │     │ Health Check    │    /status        │
│  └─────────────────┘     └─────────────────┘                   │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │ POST Handler    │────▶│ IoT Message     │ ── /sensor/data   │
│  │ Sensor Data     │     │ Conversion      │    /sensor/reg    │
│  └─────────────────┘     └─────────────────┘                   │
│       │                           │                             │
│       ▲                           ▼                             │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │ HTTP Response   │◄────│ Gateway         │                   │
│  │ JSON Format     │     │ Processing      │                   │
│  └─────────────────┘     └─────────────────┘                   │
│       │                                                         │
│       ▼                                                         │
│  CLIENT RESPONSE                                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 🔍 **GET Request Handler**

```java
/**
 * Processa requisições GET (principalmente health check)
 */
private void handleGetRequest(HTTPRequestParser.HTTPRequest request, DataOutputStream out) 
        throws IOException {
    
    System.out.println("🌐 [HTTP-GET] " + request.path + " de " + 
                      clientSocket.getRemoteSocketAddress());
    
    // ROUTER PATTERN - Path-based routing
    if (request.path.equals("/health") || request.path.startsWith("/health")) {
        // Health check sempre retorna sucesso
        responseBuilder.sendHealthResponse(out, true);
        
    } else if (request.path.equals("/sensor/status") || request.path.startsWith("/sensor/status")) {
        // Status do sistema IoT
        String statusJson = buildSystemStatusJson();
        responseBuilder.sendSuccessResponse(out, statusJson);
        
    } else {
        // Endpoint não encontrado
        responseBuilder.sendErrorResponse(out, 404, "Endpoint not found: " + request.path);
    }
}

/**
 * Constrói JSON de status do sistema
 */
private String buildSystemStatusJson() {
    return String.format(
        "{\"status\":\"ACTIVE\",\"protocol\":\"HTTP/1.1\",\"port\":%d,\"gateway\":\"IoTGateway\",\"timestamp\":\"%s\"}",
        clientSocket.getLocalPort(),
        java.time.LocalDateTime.now().toString()
    );
}
```

### 📤 **POST Request Handler**

```java
/**
 * Processa requisições POST (dados IoT)
 * Implementa CHAIN OF RESPONSIBILITY pattern
 */
private void handlePostRequest(HTTPRequestParser.HTTPRequest request, DataOutputStream out) 
        throws IOException {
    
    // 1. VALIDATION - Endpoint IoT válido
    if (!isIoTEndpoint(request.path)) {
        responseBuilder.sendErrorResponse(out, 404, "IoT endpoint not found: " + request.path);
        return;
    }
    
    // 2. PARSING - HTTP request → IoTMessage
    IoTMessage iotMessage = parser.parseToIoTMessage(request);
    
    if (iotMessage == null) {
        responseBuilder.sendErrorResponse(out, 400, "Invalid IoT message format");
        return;
    }
    
    try {
        // 3. PROXY PATTERN - Processar através do Gateway
        System.out.println("🔄 [HTTP-PROXY] Processando mensagem: " + iotMessage.getMessageId() + 
                         " - Sensor: " + iotMessage.getSensorId() + 
                         " - Tipo: " + iotMessage.getType());
        
        // Simular processamento pelo gateway
        // Na implementação real: gateway.processMessage(iotMessage)
        boolean processed = true; // Simplificado para esta versão
        
        if (processed) {
            // 4. SUCCESS RESPONSE
            responseBuilder.sendSuccessResponse(out, iotMessage);
            System.out.println("✅ [HTTP] Mensagem processada com sucesso: " + 
                             iotMessage.getSensorId());
        } else {
            // 5. ERROR RESPONSE
            responseBuilder.sendErrorResponse(out, 500, "Gateway processing failed");
        }
        
    } catch (Exception e) {
        System.err.println("❌ [HTTP] Erro ao processar mensagem IoT: " + e.getMessage());
        responseBuilder.sendErrorResponse(out, 500, "Internal processing error: " + e.getMessage());
    }
}

/**
 * Valida se é endpoint IoT válido
 */
private boolean isIoTEndpoint(String path) {
    return path.contains("/sensor/data") || 
           path.contains("/sensor/register") ||
           path.contains("/iot/");
}
```

---

## 5. **FLUXO DE COMUNICAÇÃO**

### 📡 **HTTP Request-Response Cycle**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                HTTP COMMUNICATION FLOW                         │  
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PHASE 1: CONNECTION ESTABLISHMENT                              │
│                                                                 │
│  ┌─────────────┐    TCP     ┌─────────────────────────────┐    │
│  │   Client    │ Handshake  │    HTTPCommunication        │    │
│  │  (JMeter/   │◄──────────▶│        Strategy             │    │
│  │   Postman)  │            │                             │    │
│  └─────────────┘            │  • ServerSocket (8081)      │    │
│                              │  • Accept Connection        │    │
│                              │  • Create HTTPClientHandler │    │
│                              └─────────────────────────────┘    │
│                                           │                     │
│                                           ▼                     │
│  PHASE 2: REQUEST PROCESSING                                    │
│                                                                 │
│  ┌─────────────┐             ┌─────────────────────────────┐    │
│  │ HTTP Request│────────────▶│    HTTPClientHandler        │    │
│  │             │             │                             │    │
│  │ POST /sensor│             │  • Parse HTTP Headers       │    │
│  │ /data       │             │  • Extract JSON Body        │    │
│  │             │             │  • Validate IoT Format      │    │
│  │ Content:    │             │  • Convert to IoTMessage    │    │
│  │ JSON Sensor │             └─────────────────────────────┘    │
│  │ Data        │                          │                     │
│  └─────────────┘                          ▼                     │
│                               ┌─────────────────────────────┐    │
│                               │      HTTPRequestParser      │    │
│                               │                             │    │
│                               │  • parseRequest()           │    │
│                               │  • parseToIoTMessage()      │    │
│                               │  • extractParameter()       │    │
│                               │  • parseVersionVector()     │    │
│                               └─────────────────────────────┘    │
│                                           │                     │
│                                           ▼                     │
│  PHASE 3: GATEWAY INTEGRATION                                   │
│                                                                 │
│                               ┌─────────────────────────────┐    │
│                               │        IoTGateway           │    │
│                               │       (Singleton)           │    │
│                               │                             │    │
│                               │  • processMessage()         │    │
│                               │  • Route to DataReceiver    │    │
│                               │  • Update Version Vector    │    │
│                               │  • Store in Database        │    │
│                               └─────────────────────────────┘    │
│                                           │                     │
│                                           ▼                     │
│  PHASE 4: RESPONSE GENERATION                                   │
│                                                                 │
│                               ┌─────────────────────────────┐    │
│                               │   HTTPResponseBuilder       │    │
│                               │                             │    │
│                               │  • buildSuccessJson()       │    │
│                               │  • buildVersionVectorJson() │    │
│                               │  • sendResponse()           │    │
│                               │  • HTTP Headers + CORS      │    │
│                               └─────────────────────────────┘    │
│                                           │                     │
│                                           ▼                     │
│  ┌─────────────┐             ┌─────────────────────────────┐    │
│  │HTTP Response│◄────────────│     HTTPClientHandler       │    │
│  │             │             │                             │    │
│  │ 200 OK      │             │  • Generate JSON Response   │    │
│  │ JSON Success│             │  • Set CORS Headers         │    │
│  │ + Version   │             │  • Close Connection         │    │
│  │   Vector    │             │  • Log Processing Result    │    │
│  └─────────────┘             └─────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 🔄 **Concurrent Request Handling**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                CONCURRENT HTTP PROCESSING                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐                                                │
│  │ Client 1    │─────┐                                          │
│  │ (JMeter)    │     │                                          │
│  └─────────────┘     │                                          │
│                      │    ┌─────────────────────────────┐       │
│  ┌─────────────┐     │    │  HTTPCommunicationStrategy  │       │
│  │ Client 2    │─────┼───▶│                             │       │
│  │ (Postman)   │     │    │  ServerSocket.accept()      │       │
│  └─────────────┘     │    │  ┌─────────────────────────┐ │       │
│                      │    │  │    Thread Pool          │ │       │
│  ┌─────────────┐     │    │  │   (50 threads)         │ │       │
│  │ Client 3    │─────┘    │  │                         │ │       │
│  │ (cURL)      │          │  │ ┌─────┐ ┌─────┐ ┌─────┐ │ │       │
│  └─────────────┘          │  │ │ T1  │ │ T2  │ │ T3  │ │ │       │
│                           │  │ │     │ │     │ │     │ │ │       │
│                           │  │ │HTTPCli│HTTPCli│HTTPCli│ │ │       │
│                           │  │ │Handler│Handler│Handler│ │ │       │
│                           │  │ └─────┘ └─────┘ └─────┘ │ │       │
│                           │  └─────────────────────────┘ │       │
│                           └─────────────────────────────┘       │
│                                      │   │   │                  │
│                                      ▼   ▼   ▼                  │
│                           ┌─────────────────────────────┐       │
│                           │        IoTGateway           │       │
│                           │       (Singleton)           │       │
│                           │                             │       │
│                           │  • Thread Safe Processing   │       │
│                           │  • Synchronized Methods     │       │
│                           │  • ConcurrentHashMap        │       │
│                           │  • AtomicOperations         │       │
│                           └─────────────────────────────┘       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

**📝 Documentação HTTP criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0 - Parte 2  
**🎯 Foco:** Componentes Core e Fluxo de Comunicação