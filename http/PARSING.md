# 🔍 **PARSING E PROCESSAMENTO HTTP**

**Análise Detalhada do HTTPRequestParser e HTTPResponseBuilder**

---

## 6. **ENDPOINTS E APIs**

### 🛣️ **HTTP Endpoints Disponíveis**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP API ENDPOINTS                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  METHOD │  ENDPOINT           │  DESCRIPTION                    │
│  ─────  │  ─────────────────  │  ───────────────────────────    │
│  GET    │  /health            │  Health Check do Sistema       │
│  GET    │  /sensor/status     │  Status do Gateway IoT         │
│  POST   │  /sensor/data       │  Envio de Dados de Sensores    │
│  POST   │  /sensor/register   │  Registro de Novos Sensores    │
│  POST   │  /iot/sensor/data   │  Alternativa para dados        │
│         │                     │                                 │
│  📍 BASE URL: http://localhost:8081                             │
│  🔧 Content-Type: application/json                              │
│  🌐 CORS: Enabled (Access-Control-Allow-Origin: *)            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 📊 **API Examples**

#### 🟢 **Health Check (GET /health)**

```http
GET /health HTTP/1.1
Host: localhost:8081
Accept: application/json
```

**Response:**
```json
{
  "status": "UP",
  "service": "IoT-Gateway",
  "timestamp": "2025-09-30T14:30:15.123"
}
```

#### 📤 **Send Sensor Data (POST /sensor/data)**

**JSON Format:**
```http
POST /sensor/data HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Content-Length: 156

{
  "sensorId": "TEMP_001",
  "type": "TEMPERATURE",
  "location": "Lab-A",
  "value": 25.6,
  "versionVector": {"TEMP_001": 5}
}
```

**Query Parameters Format:**
```http
POST /sensor/data?sensorId=TEMP_001&type=TEMPERATURE&location=Lab-A&value=25.6&versionVector=TEMP_001:5 HTTP/1.1
Host: localhost:8081
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "messageId": "msg-12345-abcde",
  "sensor": "TEMP_001",
  "type": "SENSOR_DATA",
  "timestamp": "2025-09-30T14:30:15.123",
  "processed": true,
  "versionVector": {"TEMP_001": 6}
}
```

---

## 7. **MESSAGE PROCESSING**

### 🔍 **HTTPRequestParser - Análise Detalhada**

```java
/**
 * Parser para requisições HTTP IoT
 * Implementa BUILDER PATTERN para construção de HTTPRequest
 */
public class HTTPRequestParser {
    
    /**
     * DATA TRANSFER OBJECT para requisições HTTP
     */
    public static class HTTPRequest {
        public String method;                    // GET, POST, PUT, DELETE
        public String path;                      // /sensor/data, /health
        public String version;                   // HTTP/1.1
        public Map<String, String> headers;     // Content-Type, etc.
        public String body;                      // JSON payload
        public boolean isValid = false;          // Validation flag
    }
    
    /**
     * PARSER PATTERN - HTTP Request parsing
     */
    public HTTPRequest parseRequest(BufferedReader reader) throws IOException {
        HTTPRequest request = new HTTPRequest();
        
        // 1. Parse Request Line: GET /path HTTP/1.1
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.trim().isEmpty()) {
            return request; // Invalid request
        }
        
        StringTokenizer tokenizer = new StringTokenizer(requestLine);
        if (tokenizer.countTokens() >= 2) {
            request.method = tokenizer.nextToken();     // GET/POST
            request.path = tokenizer.nextToken();       // /sensor/data  
            if (tokenizer.hasMoreTokens()) {
                request.version = tokenizer.nextToken(); // HTTP/1.1
            }
        }
        
        // 2. Parse Headers
        String headerLine;
        int contentLength = 0;
        while ((headerLine = reader.readLine()) != null && !headerLine.trim().isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                request.headers.put(headerName.toLowerCase(), headerValue);
                
                // Extract Content-Length for body parsing
                if ("content-length".equals(headerName.toLowerCase())) {
                    try {
                        contentLength = Integer.parseInt(headerValue);
                    } catch (NumberFormatException e) {
                        // Ignore invalid content-length
                    }
                }
            }
        }
        
        // 3. Parse Body (if Content-Length > 0)
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            
            // Read exactly contentLength bytes
            while (totalRead < contentLength) {
                int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) break; // EOF
                totalRead += read;
            }
            
            request.body = new String(bodyChars, 0, totalRead);
        }
        
        request.isValid = true;
        return request;
    }
}
```

### 🔄 **HTTP to IoT Conversion**

```java
/**
 * Converte HTTP request para IoTMessage
 * Implementa ADAPTER PATTERN
 */
public IoTMessage parseToIoTMessage(HTTPRequest request) {
    if (!request.isValid) {
        return null;
    }
    
    try {
        // 1. STRATEGY PATTERN - Determinar tipo por endpoint
        MessageType messageType;
        if (request.path.contains("/register")) {
            messageType = MessageType.SENSOR_REGISTER;
        } else if (request.path.contains("/data")) {
            messageType = MessageType.SENSOR_DATA;
        } else {
            return null; // Endpoint não suportado
        }
        
        // 2. EXTRACTION PATTERN - Extrair parâmetros
        String sensorId = extractParameter(request, "sensor", "sensorId");
        String sensorTypeStr = extractParameter(request, "type", "sensorType");
        String location = extractParameter(request, "location", "lab");
        String valueStr = extractParameter(request, "value", "reading");
        String versionVectorStr = extractParameter(request, "versionVector", "vv");
        
        // 3. VALIDATION
        if (sensorId == null || sensorTypeStr == null) {
            return null;
        }
        
        // 4. TYPE CONVERSION
        double value = parseDoubleValue(valueStr);
        ConcurrentHashMap<String, Integer> versionVector = parseVersionVector(versionVectorStr);
        
        // 5. VERSION VECTOR UPDATE
        versionVector.compute(sensorId, (k, v) -> (v == null) ? 1 : v + 1);
        
        // 6. FACTORY PATTERN - Create IoTMessage
        String content = location != null ? location : "HTTP-Client";
        IoTMessage message = new IoTMessage(sensorId, messageType, content, 
                            value, sensorTypeStr, versionVector);
        
        System.out.println("✅ [HTTP] Mensagem IoT criada - Sensor: " + sensorId + 
                         ", VV: " + versionVector + ", Timestamp: " + message.getTimestamp());
        
        return message;
        
    } catch (Exception e) {
        System.err.println("❌ Erro ao parsear HTTP para IoTMessage: " + e.getMessage());
        return null;
    }
}
```

### 🧩 **Version Vector Parsing**

```java
/**
 * Parseia Version Vector de múltiplos formatos
 * Implementa FLEXIBLE PARSING PATTERN
 */
private ConcurrentHashMap<String, Integer> parseVersionVector(String vvStr) {
    ConcurrentHashMap<String, Integer> vv = new ConcurrentHashMap<>();
    
    if (vvStr == null || vvStr.trim().isEmpty()) {
        return vv;
    }
    
    try {
        // STRATEGY 1: JSON Format - {"sensor1":5,"sensor2":3}
        if (vvStr.trim().startsWith("{")) {
            parseJsonVersionVector(vvStr, vv);
        } 
        // STRATEGY 2: Simple Format - sensor1:5,sensor2:3
        else {
            parseSimpleVersionVector(vvStr, vv);
        }
        
    } catch (Exception e) {
        System.err.println("⚠️ [HTTP] Erro ao parsear Version Vector '" + vvStr + "': " + e.getMessage());
        // Retorna VV vazio em caso de erro
    }
    
    return vv;
}

/**
 * Parse JSON Version Vector: {"sensor1":5,"sensor2":3}
 */
private void parseJsonVersionVector(String vvStr, ConcurrentHashMap<String, Integer> vv) {
    String jsonContent = vvStr.trim()
        .replaceFirst("^\\{", "")
        .replaceFirst("\\}$", "");
        
    if (!jsonContent.isEmpty()) {
        String[] pairs = jsonContent.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("\"", "");
                String value = keyValue[1].trim().replaceAll("\"", "");
                vv.put(key, Integer.parseInt(value));
            }
        }
    }
}

/**
 * Parse Simple Version Vector: sensor1:5,sensor2:3
 */
private void parseSimpleVersionVector(String vvStr, ConcurrentHashMap<String, Integer> vv) {
    String[] pairs = vvStr.split(",");
    for (String pair : pairs) {
        String[] keyValue = pair.split(":");
        if (keyValue.length == 2) {
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            vv.put(key, Integer.parseInt(value));
        }
    }
}
```

### 📤 **Parameter Extraction**

```java
/**
 * Extrai parâmetro do body JSON ou query string
 * Implementa FALLBACK STRATEGY PATTERN
 */
private String extractParameter(HTTPRequest request, String... paramNames) {
    // STRATEGY 1: Try JSON Body first
    if (request.body != null && !request.body.trim().isEmpty()) {
        String bodyParam = extractFromJsonBody(request.body, paramNames);
        if (bodyParam != null) {
            return bodyParam;
        }
    }
    
    // STRATEGY 2: Try Query String as fallback
    if (request.path.contains("?")) {
        String queryParam = extractFromQueryString(request.path, paramNames);
        if (queryParam != null) {
            return queryParam;
        }
    }
    
    return null; // Parameter not found
}

/**
 * Extrai parâmetro do JSON body (simple JSON parsing)
 */
private String extractFromJsonBody(String body, String... paramNames) {
    for (String paramName : paramNames) {
        String pattern = "\"" + paramName + "\"";
        int index = body.indexOf(pattern);
        if (index >= 0) {
            int valueStart = body.indexOf(":", index);
            if (valueStart >= 0) {
                valueStart = body.indexOf("\"", valueStart);
                if (valueStart >= 0) {
                    int valueEnd = body.indexOf("\"", valueStart + 1);
                    if (valueEnd > valueStart) {
                        return body.substring(valueStart + 1, valueEnd);
                    }
                }
            }
        }
    }
    return null;
}

/**
 * Extrai parâmetro da query string
 */
private String extractFromQueryString(String path, String... paramNames) {
    String queryString = path.substring(path.indexOf("?") + 1);
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
    return null;
}
```

---

**📝 Documentação HTTP criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0 - Parte 3  
**🎯 Foco:** Endpoints, APIs e Message Processing