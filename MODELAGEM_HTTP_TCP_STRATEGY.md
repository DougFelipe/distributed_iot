# 🌐 Modelagem HTTP/TCP Strategy - Sistema IoT Distribuído

## 📋 Análise dos Exemplos do Professor

### 🔍 **Exemplo HTTP** (`exemplo_http/ProjetoHTTP/src/br/imd/ufrn/`)

**Arquitetura Identificada:**
- **WebServer.java**: Servidor HTTP simples com thread pool (porta 8080)
- **ClientHandler.java**: Processador de requisições HTTP (Runnable)
- **HTTPClient.java**: Cliente HTTP básico para testes
- **NIOWebServer.java**: Versão NIO com seletor não-bloqueante (porta 9999)

**Padrões Observados:**
- ✅ **Thread-per-connection** no WebServer
- ✅ **HTTP/1.0 e HTTP/1.1** suportados
- ✅ **Response headers** estruturados (Content-Type, Content-Length)
- ✅ **Status codes** (200 OK, 405 Method Not Allowed, 404 Not Found)
- ✅ **NIO** para alta concorrência

### 🔍 **Exemplo TCP** (`exemplo_tcp/src/ufrn/imd/br/`)

**Arquitetura Identificada:**
- **TCPServer.java**: Servidor básico com ObjectInputStream/ObjectOutputStream
- **TCPClient.java**: Cliente TCP com serialização Java
- **ProcessPayload.java**: Processador de mensagens de negócio
- **Banco.java**: Modelo de dados com ConcurrentHashMap
- **TCPServerRaw.java**: Versão raw text (BufferedReader/PrintWriter)
- **TCPServerRawNThreadPlatform.java**: Pool de threads tradicionais
- **TCPServerRawNThreadVirtual.java**: Virtual threads (Java 21)

**Padrões Observados:**
- ✅ **Protocol parsing** com StringTokenizer
- ✅ **Thread safety** com ConcurrentHashMap
- ✅ **Resource management** com try-with-resources
- ✅ **Payload processing** separado da comunicação
- ✅ **Múltiplas estratégias de threading**

---

## 🏗️ **Modelagem da Integração HTTP/TCP Strategy**

### 📁 **Nova Estrutura de Diretórios**

```
src/main/java/br/ufrn/dimap/
├── communication/
│   ├── http/                    # 🆕 Módulo HTTP
│   │   ├── HTTPCommunicationStrategy.java
│   │   ├── HTTPClientHandler.java
│   │   ├── HTTPRequestParser.java
│   │   ├── HTTPResponseBuilder.java
│   │   └── HTTPProtocolConstants.java
│   ├── tcp/                     # 🆕 Módulo TCP
│   │   ├── TCPCommunicationStrategy.java
│   │   ├── TCPClientHandler.java
│   │   ├── TCPMessageProcessor.java
│   │   └── TCPProtocolConstants.java
│   └── native_udp/              # ✅ Existente
│       └── UDPCommunicationStrategy.java
├── patterns/strategy/           # ✅ Existente
│   ├── CommunicationStrategy.java
│   └── ReceiverStrategy.java
└── applications/                # ✅ Existente
    └── IoTDistributedSystem.java
```

---

## 🌐 **1. HTTP Communication Strategy**

### **HTTPCommunicationStrategy.java**
```java
public class HTTPCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    
    // Implementa padrão Observer para IoT Gateway
    // Suporte para HTTP/1.1 com Keep-Alive
    // Thread pool configurável (virtual threads)
}
```

### **HTTPClientHandler.java**
```java
public class HTTPClientHandler implements Runnable {
    private final Socket clientSocket;
    private final HTTPRequestParser parser;
    private final HTTPResponseBuilder responseBuilder;
    
    // Processa requisições HTTP POST/GET
    // Converte HTTP payload para IoTMessage
    // Envia resposta HTTP estruturada
}
```

### **HTTPRequestParser.java**
```java
public class HTTPRequestParser {
    // Parse HTTP headers (Content-Type, Content-Length)
    // Extrai IoT payload do body JSON/XML
    // Valida formato de mensagem IoT
    // Suporte para SENSOR_DATA e SENSOR_REGISTER
}
```

### **HTTPResponseBuilder.java**
```java
public class HTTPResponseBuilder {
    // Constrói responses HTTP/1.1 padronizadas
    // Status codes: 200 OK, 400 Bad Request, 500 Internal Error
    // Headers: Content-Type application/json, CORS
    // Body: JSON com success/error e dados IoT
}
```

---

## 🔌 **2. TCP Communication Strategy**

### **TCPCommunicationStrategy.java**
```java
public class TCPCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;
    private ExecutorService virtualThreadExecutor;
    private volatile boolean running = false;
    
    // Implementa TCP com virtual threads (Java 21)
    // Suporte para raw text e serialização Java
    // Connection pooling e timeout management
}
```

### **TCPClientHandler.java**
```java
public class TCPClientHandler implements Runnable {
    private final Socket clientSocket;
    private final TCPMessageProcessor processor;
    
    // Processa conexões TCP com try-with-resources
    // Suporte para BufferedReader/PrintWriter
    // Timeout configurável por conexão
}
```

### **TCPMessageProcessor.java**
```java
public class TCPMessageProcessor {
    // Parse de mensagens format "SENSOR_DATA|sensor|type|location|timestamp|value"
    // Compatibilidade com protocolo UDP existente
    // Validation e error handling robusto
}
```

---

## ⚙️ **3. Integração com Sistema Existente**

### **CommunicationStrategy.java** (Interface - Atualizar)
```java
public interface CommunicationStrategy {
    void startServer(int port) throws Exception;
    void stopServer();
    void sendMessage(IoTMessage message, String targetAddress, int targetPort);
    void setMessageHandler(MessageHandler handler);
    
    // 🆕 Novos métodos para HTTP/TCP
    default boolean supportsKeepAlive() { return false; }
    default int getMaxConcurrentConnections() { return 100; }
    default String getProtocolName() { return "UNKNOWN"; }
}
```

### **IoTDistributedSystem.java** (Atualizar)
```java
public class IoTDistributedSystem {
    // 🆕 Enum para seleção de protocolo
    public enum Protocol { UDP, HTTP, TCP }
    
    // 🆕 Factory method para estratégias
    private CommunicationStrategy createStrategy(Protocol protocol) {
        return switch (protocol) {
            case UDP -> new UDPCommunicationStrategy();
            case HTTP -> new HTTPCommunicationStrategy();
            case TCP -> new TCPCommunicationStrategy();
        };
    }
}
```

---

## 📊 **4. Mapeamento de Protocolos IoT**

### **Formato de Mensagem Unificado**
```
UDP:  "SENSOR_DATA|sensor_id|type|location|timestamp|value"
HTTP: POST /iot/sensor
      Content-Type: application/json
      {"type":"SENSOR_DATA","sensor":"sensor_id","dataType":"TEMPERATURE",...}
      
TCP:  "SENSOR_DATA|sensor_id|type|location|timestamp|value\n"
```

### **Responses Padronizadas**
```
UDP:  "SUCCESS|IOT-MSG-123|SENSOR_1|PROCESSED"
HTTP: 200 OK
      {"status":"SUCCESS","messageId":"IOT-MSG-123","sensor":"SENSOR_1"}
      
TCP:  "SUCCESS|IOT-MSG-123|SENSOR_1|PROCESSED\n"
```

---

## 🧪 **5. Compatibilidade com JMeter**

### **HTTP Strategy** 
- ✅ **JMeter HTTP Samplers** nativos
- ✅ **JSON Assertions** para validar responses
- ✅ **Response time measurement** preciso
- ✅ **Load testing** com milhares de conexões

### **TCP Strategy**
- ✅ **JMeter TCP Samplers** disponíveis
- ✅ **Connection reuse** para performance
- ✅ **Custom protocol support**

---

## 🎯 **6. Benefícios da Implementação**

### **Para Desenvolvimento:**
- ✅ **Flexibilidade**: Troca de protocolo em tempo de execução  
- ✅ **Testing**: JMeter nativo para HTTP/TCP
- ✅ **Performance**: Virtual threads e NIO onde aplicável
- ✅ **Monitoring**: Métricas específicas por protocolo

### **Para Apresentação:**
- ✅ **Demo HTTP**: Interface web simples para mostrar dados
- ✅ **Load Testing**: JMeter visual com gráficos
- ✅ **Fault Tolerance**: Mostrar falhas de conectividade HTTP/TCP
- ✅ **Scalability**: Demonstrar diferenças de performance entre protocolos

---

## 📋 **7. Cronograma de Implementação**

### **Fase 1: TCP Strategy** (Prioridade Alta)
1. ✅ TCPCommunicationStrategy básica
2. ✅ TCPClientHandler com virtual threads
3. ✅ Integração com sistema existente
4. ✅ Testes JMeter TCP

### **Fase 2: HTTP Strategy** (Prioridade Média)
1. ✅ HTTPCommunicationStrategy básica
2. ✅ HTTP request/response parsing
3. ✅ JSON payload support
4. ✅ Testes JMeter HTTP

### **Fase 3: Otimizações** (Prioridade Baixa)
1. ⚡ Connection pooling
2. ⚡ Protocol negotiation
3. ⚡ Metrics e monitoring
4. ⚡ Web interface demo

---

## 🔧 **8. Configurações de Aplicação**

### **application.properties** (Atualizar)
```properties
# Comunicação Strategy
iot.communication.protocol=UDP
iot.communication.http.port=8080
iot.communication.tcp.port=8081
iot.communication.udp.port=9090

# Threading
iot.communication.http.threads.max=300
iot.communication.tcp.use-virtual-threads=true
iot.communication.connection.timeout=5000

# JMeter Integration
iot.testing.jmeter.enabled=true
iot.testing.jmeter.results.path=jmeter/results/
```

---

## 🎪 **9. Demo e Apresentação**

### **Cenário de Demonstração:**
1. **Sistema iniciado com UDP** (baseline)
2. **Switch para HTTP** durante execução
3. **JMeter load test** com gráficos visuais
4. **Switch para TCP** mostrando diferenças de performance
5. **Fault tolerance** desligando receivers

### **Métricas a Apresentar:**
- 📊 **Throughput** por protocolo (msg/s)
- ⏱️ **Latency** comparativa (UDP vs HTTP vs TCP)
- 🔧 **Resource usage** (memory, threads)
- ⚡ **Scalability** (concurrent connections)

---

**Esta modelagem permite implementar HTTP/TCP como novas strategies mantendo a arquitetura GoF existente, com foco na compatibilidade JMeter e demonstração visual das capacidades do sistema.**