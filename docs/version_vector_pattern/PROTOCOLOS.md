# Integração com Protocolos de Comunicação - Version Vector Multiprotocolo

## Conceito de Integração Multiprotocolo

O Version Vector no sistema IoT demonstra **flexibilidade arquitetural** ao integrar-se **naturalmente** com múltiplos protocolos de comunicação. Cada protocolo transporta e preserva **informação causal** de forma apropriada às suas características técnicas, mantendo **consistência semântica** independente do meio de transporte.

## Arquitetura Multiprotocolo com Version Vector

```
                    ┌─────────────────────────────────┐
                    │         IoT Message             │
                    │    (Version Vector Core)        │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │    Protocol Adaptation Layer    │
                    └─┬─────┬──────────┬─────────────┬─┘
                      │     │          │             │
              ┌───────▼──┐ ┌▼────┐ ┌──▼──────┐ ┌───▼──────┐
              │   UDP    │ │HTTP │ │  TCP    │ │   gRPC   │
              │          │ │     │ │         │ │          │
              │ Native   │ │JSON │ │ Binary  │ │ Protocol │
              │ Binary   │ │Headers│ │ Stream │ │ Buffers  │
              │          │ │     │ │         │ │          │
              │ Port:    │ │Port:│ │ Port:   │ │ Port:    │
              │ 9091     │ │8080 │ │ 9092    │ │ 9090     │
              └──────────┘ └─────┘ └─────────┘ └──────────┘
                    │         │         │           │
                    ▼         ▼         ▼           ▼
              ┌──────────────────────────────────────────────┐
              │           DataReceiver                       │
              │     (Protocol-Agnostic Processing)          │  
              │                                             │
              │ Version Vector merge independente           │
              │ do protocolo de origem                      │
              └──────────────────────────────────────────────┘
```

## UDP - Version Vector Nativo

### Transporte Binário Direto

O **protocolo UDP** transporta o Version Vector como **parte nativa** da mensagem serializada:

```java
// IoTMessage com Version Vector integrado
public class IoTMessage implements Serializable {
    private final ConcurrentHashMap<String, Integer> versionVector;
    
    // Serialização nativa Java
    // Version Vector é parte integral da mensagem
    // Sem overhead de conversão
}
```

### Processo de Envio UDP

```
Sensor cria mensagem ──► VV={SENSOR_01=5}
         │
         ▼
Serialização Java ──► ObjectOutputStream.writeObject()
         │
         ▼  
UDP Datagram ──► [Header|IoTMessage_bytes|VV_embedded]
         │
         ▼
Rede UDP ──► Transporte direto, sem intermediários
         │
         ▼
DataReceiver ──► ObjectInputStream.readObject()
         │
         ▼
Version Vector ──► Disponível imediatamente para merge
```

**Vantagens UDP**:
- **Zero overhead** de conversão
- **Performance máxima** para Version Vector
- **Simplicidade** de implementação
- **Broadcast natural** para sincronização

### Exemplo de Log UDP

```
💓 [UDP] Heartbeat atualizado: SENSOR_01 (total: 1,247) - VV={SENSOR_01=5, DR_1=12}
🔍 [DATA_RECEIVER_1] VERSION VECTOR ATUALIZADO: SENSOR_01 → {SENSOR_01=6, DR1=13, DR2=8}
```

## HTTP - Version Vector via Headers

### Headers Customizados para Causalidade

O **protocolo HTTP** transporta Version Vector através de **headers customizados**:

```java
// GatewayHTTPRequestHandler.java
private void addVersionVectorHeaders(HttpExchange exchange, IoTMessage message) {
    HttpHeaders responseHeaders = exchange.getResponseHeaders();
    
    // Version Vector como JSON header
    String vvJson = objectMapper.writeValueAsString(message.getVersionVector());
    responseHeaders.set("X-Version-Vector", vvJson);
    
    // Informação causal preservada no protocolo stateless
    responseHeaders.set("X-Causal-Context", message.getSensorId());
}
```

### Estrutura HTTP com Version Vector

```
POST /api/sensor/data HTTP/1.1
Host: iot-gateway:8080
Content-Type: application/json
X-Version-Vector: {"SENSOR_01":5,"DATA_RECEIVER_1":12}
X-Causal-Context: SENSOR_01

{
  "sensor_id": "SENSOR_01",
  "sensor_value": 23.5,
  "sensor_type": "TEMPERATURE", 
  "timestamp": "2025-10-01T10:30:15"
}

HTTP/1.1 200 OK
X-Version-Vector: {"SENSOR_01":6,"DATA_RECEIVER_1":13,"DATA_RECEIVER_2":8}
X-Processing-Node: DATA_RECEIVER_1
Content-Type: application/json

{
  "status": "success",
  "message": "Data processed",
  "updated_version_vector": {"SENSOR_01":6,"DR_1":13,"DR_2":8}
}
```

**Vantagens HTTP**:
- **RESTful semantics** preservadas
- **Cacheable responses** com informação causal
- **Web compatibility** total
- **Debugging friendly** via headers visíveis

### Processamento HTTP do Version Vector

```java
private void processHTTPMessage(HttpExchange exchange) throws IOException {
    // Extrair Version Vector dos headers
    String vvHeader = exchange.getRequestHeaders().getFirst("X-Version-Vector");
    ConcurrentHashMap<String, Integer> versionVector = parseVersionVector(vvHeader);
    
    // Criar IoTMessage com VV restaurado
    IoTMessage message = createMessageFromHTTP(exchange, versionVector);
    
    // Processamento normal (protocol-agnostic)
    DataReceiver receiver = gateway.selectReceiver(message);
    receiver.processMessage(message);
    
    // Resposta com VV atualizado
    addVersionVectorHeaders(exchange, message);
}
```

## TCP - Version Vector em Stream Binário

### Protocolo Orientado a Conexão

O **protocolo TCP** mantém **conexões persistentes** e transporta Version Vector em **streams binários**:

```java
// TCPCommunicationStrategy.java  
private void sendMessage(IoTMessage message, OutputStream outputStream) {
    try {
        // Serialização binária incluindo Version Vector
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(message); // VV incluído automaticamente
        oos.flush();
        
        logger.debug("📤 [TCP] Mensagem enviada: {} VV={}", 
                    message.getSensorId(), message.getVersionVector());
    } catch (IOException e) {
        logger.error("❌ [TCP] Erro no envio: {}", e.getMessage());
    }
}
```

### Conexões Persistentes com Estado Causal

```
Cliente TCP ←→ Gateway TCP Server
    │              │
    │─── Conexão ───│ (socket persistente)
    │              │
    │── Msg1 ──────→│ VV={SENSOR_01=1}
    │←── ACK ───────│ VV={SENSOR_01=1, GATEWAY=1}
    │              │
    │── Msg2 ──────→│ VV={SENSOR_01=2}  
    │←── ACK ───────│ VV={SENSOR_01=2, GATEWAY=2}
    │              │
    └── Close ─────┘ (estado causal preservado durante sessão)
```

**Vantagens TCP**:
- **Conexões persistentes** reduzem overhead
- **Ordem garantida** de mensagens
- **Controle de fluxo** automático
- **Session state** para otimização de VV

### Exemplo TCP com Version Vector

```
🔌 [TCP] Nova conexão de /127.0.0.1:54032
📥 [TCP] Mensagem recebida: SENSOR_01 VV={SENSOR_01=1}
🔍 [TCP] Version Vector merged: {SENSOR_01=1, TCP_HANDLER=1}
📤 [TCP] ACK enviado: VV={SENSOR_01=1, TCP_HANDLER=1, DR_1=15}
```

## gRPC - Version Vector via Protocol Buffers

### Type-Safe Version Vector

O **protocolo gRPC** oferece **type safety** completa para Version Vector através de **Protocol Buffers**:

```protobuf
// iot_service.proto
message VersionVector {
  map<string, int64> vector = 1;  // sensor_id -> counter
}

message IoTMessage {
  string sensor_id = 1;
  SensorType sensor_type = 2;
  double sensor_value = 3;
  int64 timestamp = 4;
  VersionVector version_vector = 5;  // Type-safe VV
}

service IoTGatewayService {
  rpc SendSensorData(SensorDataRequest) returns (SensorDataResponse);
  rpc StreamSensorData(stream SensorDataRequest) returns (stream SensorDataResponse);
}
```

### Conversão Bi-direcional Type-Safe

```java
public class VersionVectorGRPCIntegration {
    
    // Java Map → Protocol Buffers
    public static IoTProtos.VersionVector convertToProto(Map<String, Long> javaVV) {
        IoTProtos.VersionVector.Builder builder = IoTProtos.VersionVector.newBuilder();
        
        javaVV.forEach((sensorId, version) -> {
            builder.putVector(sensorId, version);
        });
        
        return builder.build();
    }
    
    // Protocol Buffers → Java Map
    public static Map<String, Long> convertFromProto(IoTProtos.VersionVector protoVV) {
        Map<String, Long> javaVV = new ConcurrentHashMap<>();
        
        protoVV.getVectorMap().forEach((sensorId, version) -> {
            javaVV.put(sensorId, version);
        });
        
        return javaVV;
    }
}
```

### Streaming Bidirecional com Version Vector

```java
// gRPC Streaming com Version Vector contínuo
@Override
public StreamObserver<SensorDataRequest> streamSensorData(
        StreamObserver<SensorDataResponse> responseObserver) {
    
    return new StreamObserver<SensorDataRequest>() {
        @Override
        public void onNext(SensorDataRequest request) {
            // Extrair Version Vector do Protocol Buffer
            Map<String, Long> clientVV = VersionVectorGRPCIntegration
                .convertFromProto(request.getVersionVector());
            
            // Processar mensagem
            IoTMessage message = convertToIoTMessage(request, clientVV);
            DataReceiver receiver = gateway.selectReceiver(message);
            receiver.processMessage(message);
            
            // Resposta com Version Vector atualizado
            SensorDataResponse response = SensorDataResponse.newBuilder()
                .setVersionVector(VersionVectorGRPCIntegration
                    .convertToProto(receiver.getVersionVector()))
                .setStatus("SUCCESS")
                .build();
                
            responseObserver.onNext(response);
        }
    };
}
```

**Vantagens gRPC**:
- **Type safety** em compile-time
- **Binary efficiency** superior ao JSON
- **Streaming bidirecional** para sincronização contínua
- **Cross-language** support automático

### Exemplo gRPC com Version Vector

```
🚀 [gRPC] Server iniciado na porta 9090
📡 [gRPC] Request recebido: SENSOR_01 VV={SENSOR_01=5}
🔢 [gRPC] Version Vector convertido: Proto→Java
🔍 [gRPC] Version Vector merged: {SENSOR_01=5, GRPC_HANDLER=1}
📤 [gRPC] Response enviado: VV={SENSOR_01=5, GRPC_HANDLER=1, DR_1=25}
```

## Integração no Gateway - Protocol-Agnostic Processing

### Abstração de Protocolo

O **IoTGateway** processa Version Vectors de forma **agnóstica** ao protocolo:

```java
public class IoTGateway {
    
    public void processMessage(IoTMessage message, String protocol) {
        // Version Vector merge independente do protocolo
        updateVersionVector(message);
        
        // Seleção de receiver via Strategy (protocol-agnostic)
        DataReceiver receiver = receiverStrategy.selectReceiver(message, activeReceivers);
        
        // Processamento unificado
        receiver.processMessage(message);
        
        // Observadores notificados (protocol-agnostic)
        notifyObservers("MESSAGE_PROCESSED", message);
        
        logger.debug("✅ [{}] Mensagem processada: {} VV={}", 
                    protocol, message.getSensorId(), message.getVersionVector());
    }
}
```

### Métricas por Protocolo

```java
// Coleta de métricas específicas por protocolo
private final Map<String, AtomicLong> protocolStats = new ConcurrentHashMap<>();

public void updateProtocolStats(String protocol, IoTMessage message) {
    protocolStats.computeIfAbsent(protocol + "_messages", k -> new AtomicLong(0))
                 .incrementAndGet();
                 
    protocolStats.computeIfAbsent(protocol + "_vv_size", k -> new AtomicLong(0))
                 .set(message.getVersionVector().size());
}

// Relatório consolidado
📊 PROTOCOL STATS: UDP_messages=1247, HTTP_messages=856, TCP_messages=634, gRPC_messages=423
📊 VERSION VECTOR: UDP_vv_size=5, HTTP_vv_size=5, TCP_vv_size=5, gRPC_vv_size=5
```

## Sincronização Cross-Protocol

### Merge Unificado de Version Vectors

Independente do protocolo de origem, **todos** os Version Vectors são **merged** usando a **mesma lógica**:

```java
private void synchronizeAcrossProtocols() {
    // Coletar Version Vectors de todos os protocolos
    Map<String, Long> unifiedVV = new ConcurrentHashMap<>();
    
    for (DataReceiver receiver : dataReceivers) {
        Map<String, Long> receiverVV = receiver.getVersionVector();
        
        // Merge agnóstico ao protocolo
        receiverVV.forEach((nodeId, version) -> {
            unifiedVV.merge(nodeId, version, Long::max);
        });
    }
    
    logger.info("🔄 CROSS-PROTOCOL SYNC: Unified VV={}", unifiedVV);
}
```

### Demonstração Prática Cross-Protocol

```
Estado Inicial:
UDP Receiver:  VV={TEMP_01=15, UDP_R=25}
HTTP Receiver: VV={TEMP_01=12, HTTP_R=18}  
TCP Receiver:  VV={TEMP_01=14, TCP_R=22}
gRPC Receiver: VV={TEMP_01=16, GRPC_R=30}

Após Sincronização:
Todos Receivers: VV={TEMP_01=16, UDP_R=25, HTTP_R=18, TCP_R=22, GRPC_R=30}

Resultado: Consistência causal preservada independente do protocolo
```

## Observabilidade Multiprotocolo

### Logs Consolidados

```
🔍 [UDP] VERSION VECTOR ATUALIZADO: SENSOR_01 → {SENSOR_01=6, UDP_HANDLER=13}
🔍 [HTTP] VERSION VECTOR ATUALIZADO: SENSOR_02 → {SENSOR_02=4, HTTP_HANDLER=8}
🔍 [TCP] VERSION VECTOR ATUALIZADO: SENSOR_03 → {SENSOR_03=9, TCP_HANDLER=15}
🔍 [gRPC] VERSION VECTOR ATUALIZADO: SENSOR_04 → {SENSOR_04=12, GRPC_HANDLER=7}

📊 CONSOLIDATED VV: {SENSOR_01=6, SENSOR_02=4, SENSOR_03=9, SENSOR_04=12, 
                     UDP_HANDLER=13, HTTP_HANDLER=8, TCP_HANDLER=15, GRPC_HANDLER=7}
```

A integração multiprotocolo demonstra como **abstrações bem projetadas** permitem que **conceitos fundamentais** como Version Vector funcionem **uniformemente** através de **tecnologias heterogêneas**, mantendo **consistência semântica** independente das **características técnicas** de cada protocolo.