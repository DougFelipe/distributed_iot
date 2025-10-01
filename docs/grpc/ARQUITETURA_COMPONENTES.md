# 🏗️ **ARQUITETURA gRPC - COMPONENTES E COMUNICAÇÃO**

**Sistema IoT Distribuído - Análise Arquitetural Detalhada**

---

## 📋 **ÍNDICE ARQUITETURAL**

1. [Visão Arquitetural Geral](#1-visão-arquitetural)
2. [Componentes e Responsabilidades](#2-componentes)
3. [Sensores IoT com gRPC](#3-sensores-iot)
4. [Data Receivers e Processamento](#4-data-receivers)
5. [Padrões de Comunicação](#5-padrões-comunicação)
6. [Version Vector Integration](#6-version-vector)
7. [Fault Tolerance com gRPC](#7-fault-tolerance)

---

## 1. **VISÃO ARQUITETURAL**

### 🎯 **Arquitetura Hexagonal com gRPC**

```ascii
                    ┌─────────────────────────────────────────────┐
                    │             HEXAGONAL ARCHITECTURE         │
                    │                    WITH gRPC               │
                    └─────────────────────┬───────────────────────┘
                                          │
                    ┌─────────────────────▼───────────────────────┐
                    │                  ADAPTERS                   │
                    │                                             │
                    │  ┌─────────────┐    ┌─────────────────┐    │
                    │  │ gRPC        │    │ JMeter Testing  │    │
                    │  │ Clients     │    │ Adapter         │    │
                    │  │             │    │                 │    │
                    │  │ • Proto     │    │ • HTTP Bridge   │    │
                    │  │ • Streaming │    │ • Load Testing  │    │
                    │  │ • Type Safe │    │ • Metrics       │    │
                    │  └─────────────┘    └─────────────────┘    │
                    └─────────────────────┬───────────────────────┘
                                          │
                    ┌─────────────────────▼───────────────────────┐
                    │                 CORE DOMAIN                 │
                    │                                             │
                    │  ┌─────────────────────────────────────┐    │
                    │  │           IoTGateway                │    │
                    │  │         (Singleton + Proxy)         │    │
                    │  │                                     │    │
                    │  │  • Strategy Pattern (gRPC)         │    │
                    │  │  • Load Balancing                   │    │
                    │  │  • Service Discovery                │    │
                    │  │  • Message Routing                  │    │
                    │  └─────────────────────────────────────┘    │
                    │                                             │
                    │  ┌─────────────────────────────────────┐    │
                    │  │          Data Receivers             │    │
                    │  │         (Stateful Services)         │    │
                    │  │                                     │    │
                    │  │  • Version Vector                   │    │
                    │  │  • Data Replication                 │    │
                    │  │  • Conflict Resolution              │    │
                    │  │  • Fault Recovery                   │    │
                    │  └─────────────────────────────────────┘    │
                    └─────────────────────────────────────────────┘
```

### 🔄 **Fluxo de Dados Multinível**

```
NÍVEL 1 - PROTOCOL LAYER:
┌─────────────────────────────────────────────────────────────┐
│ gRPC Client → HTTP/2 → Protocol Buffers → Binary Stream    │
└─────────────────────────┬───────────────────────────────────┘
                          │
NÍVEL 2 - STRATEGY LAYER: │
┌─────────────────────────▼───────────────────────────────────┐
│ GRPCCommunicationStrategy → Message Conversion → Callback  │
└─────────────────────────┬───────────────────────────────────┘
                          │
NÍVEL 3 - GATEWAY LAYER:  │
┌─────────────────────────▼───────────────────────────────────┐
│ IoTGateway (Proxy) → Load Balancing → Route Selection      │
└─────────────────────────┬───────────────────────────────────┘
                          │
NÍVEL 4 - PROCESSING:     │
┌─────────────────────────▼───────────────────────────────────┐
│ DataReceiver → Version Vector → Storage → Replication      │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. **COMPONENTES**

### 🎯 **GRPCCommunicationStrategy - Componente Central**

```java
/**
 * Arquitetura do GRPCCommunicationStrategy
 */
public class GRPCCommunicationStrategy implements CommunicationStrategy {
    
    // === CORE COMPONENTS ===
    private Server server;                           // gRPC Server Engine
    private BiConsumer<IoTMessage, String> messageProcessor; // Callback para Gateway
    private volatile boolean running = false;       // State Management
    
    // === SERVICE IMPLEMENTATION ===
    private IoTGatewayServiceImpl serviceImpl;      // gRPC Service Logic
    
    // === ARCHITECTURAL METHODS ===
    
    /**
     * BOOTSTRAP PATTERN - Inicialização do servidor
     */
    @Override
    public void startServer(int port) throws Exception {
        // 1. Service Discovery Setup
        serviceImpl = new IoTGatewayServiceImpl();
        
        // 2. Server Builder Pattern
        server = ServerBuilder.forPort(port)
                .addService(serviceImpl)          // Registrar serviço
                .build()
                .start();
        
        // 3. Lifecycle Management
        running = true;
        setupShutdownHooks();
    }
    
    /**
     * CALLBACK PATTERN - Integração com Gateway
     */
    public void setMessageProcessor(BiConsumer<IoTMessage, String> processor) {
        this.messageProcessor = processor;
        logger.info("🔗 [gRPC] Callback configurado para Gateway integration");
    }
    
    /**
     * ADAPTER PATTERN - Conversão Proto ↔ Java
     */
    private IoTMessage convertToIoTMessage(IoTProtos.SensorRegisterRequest request) {
        return new IoTMessage(
            request.getSensorInfo().getSensorId(),
            IoTMessage.MessageType.SENSOR_REGISTER,
            buildDataPayload(request)
        );
    }
}
```

### 🏭 **IoTGatewayServiceImpl - Service Factory**

```java
/**
 * FACTORY PATTERN para implementação dos serviços gRPC
 */
private class IoTGatewayServiceImpl extends IoTGatewayServiceGrpc.IoTGatewayServiceImplBase {
    
    // === SERVICE METHODS ===
    
    /**
     * TEMPLATE METHOD PATTERN para registro de sensores
     */
    @Override
    public void registerSensor(IoTProtos.SensorRegisterRequest request, 
                             StreamObserver<IoTProtos.SensorRegisterResponse> responseObserver) {
        
        // Template Steps:
        IoTMessage message = validateAndConvert(request);      // 1. Validation
        processViaGateway(message, "grpc-client");            // 2. Processing  
        IoTProtos.SensorRegisterResponse response = buildSuccessResponse(); // 3. Response
        sendResponse(responseObserver, response);             // 4. Transmission
    }
    
    /**
     * OBSERVER PATTERN para dados de sensor
     */
    @Override
    public void sendSensorData(IoTProtos.SensorDataRequest request,
                             StreamObserver<IoTProtos.SensorDataResponse> responseObserver) {
        
        // Observer notification to Gateway
        notifyGatewayObservers(request.getIotMessage());
        
        // Response with processing status
        responseObserver.onNext(buildDataResponse(request));
        responseObserver.onCompleted();
    }
    
    /**
     * HEARTBEAT PATTERN para monitoramento
     */
    @Override
    public void heartbeat(IoTProtos.HeartbeatRequest request,
                        StreamObserver<IoTProtos.HeartbeatResponse> responseObserver) {
        
        // Health check logic
        boolean isHealthy = performHealthCheck(request.getSensorId());
        
        IoTProtos.HeartbeatResponse response = IoTProtos.HeartbeatResponse.newBuilder()
            .setSuccess(isHealthy)
            .setMessage(isHealthy ? "Healthy" : "Degraded")
            .setServerTimestamp(System.currentTimeMillis())
            .build();
            
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

---

## 3. **SENSORES IoT**

### 📡 **Modelo de Sensor com gRPC**

```java
/**
 * Representação conceitual de um Sensor IoT com gRPC
 */
public class IoTSensorGRPC {
    
    private String sensorId;
    private SensorType type;
    private String location;
    private ManagedChannel channel;      // gRPC Connection
    private IoTGatewayServiceGrpc.IoTGatewayServiceBlockingStub stub;
    
    /**
     * CONNECTION FACTORY PATTERN
     */
    public void connectToGateway(String gatewayHost, int gatewayPort) {
        // 1. Channel Management
        channel = ManagedChannelBuilder.forAddress(gatewayHost, gatewayPort)
                .usePlaintext()                    // Para desenvolvimento
                .keepAliveTime(30, TimeUnit.SECONDS)  // Keep-alive
                .build();
        
        // 2. Stub Creation
        stub = IoTGatewayServiceGrpc.newBlockingStub(channel);
        
        logger.info("📡 [Sensor] Conectado ao Gateway via gRPC: {}:{}", 
            gatewayHost, gatewayPort);
    }
    
    /**
     * BUILDER PATTERN para registro
     */
    public void registerWithGateway() {
        IoTProtos.SensorInfo sensorInfo = IoTProtos.SensorInfo.newBuilder()
            .setSensorId(this.sensorId)
            .setSensorType(convertToProtoType(this.type))
            .setLocation(this.location)
            .setStatus(IoTProtos.SensorStatus.ACTIVE)
            .setLastSeen(System.currentTimeMillis())
            .build();
        
        IoTProtos.SensorRegisterRequest request = 
            IoTProtos.SensorRegisterRequest.newBuilder()
                .setSensorInfo(sensorInfo)
                .setVersionVector(buildVersionVector())
                .build();
        
        // Synchronous call
        IoTProtos.SensorRegisterResponse response = stub.registerSensor(request);
        
        if (response.getSuccess()) {
            logger.info("✅ [Sensor] Registrado com sucesso: {}", response.getMessage());
        } else {
            logger.error("❌ [Sensor] Falha no registro: {}", response.getMessage());
        }
    }
    
    /**
     * PERIODIC PATTERN para envio de dados
     */
    public void startDataTransmission(int intervalSeconds) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. Generate sensor data
                SensorMeasurement measurement = generateMeasurement();
                
                // 2. Build gRPC message
                IoTProtos.IoTMessage iotMessage = IoTProtos.IoTMessage.newBuilder()
                    .setMessageId(UUID.randomUUID().toString())
                    .setSensorId(this.sensorId)
                    .setSensorType(convertToProtoType(this.type))
                    .setMessageType("SENSOR_DATA")
                    .setMeasurement(convertToProtoMeasurement(measurement))
                    .setVersionVector(updateAndGetVersionVector())
                    .setTimestamp(System.currentTimeMillis())
                    .build();
                
                IoTProtos.SensorDataRequest request = 
                    IoTProtos.SensorDataRequest.newBuilder()
                        .setIotMessage(iotMessage)
                        .build();
                
                // 3. Send via gRPC
                IoTProtos.SensorDataResponse response = stub.sendSensorData(request);
                
                logger.info("📊 [Sensor] Dados enviados: {} processado por {}", 
                    measurement.getValue(), response.getProcessedBy());
                
            } catch (Exception e) {
                logger.error("❌ [Sensor] Erro ao enviar dados: {}", e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * HEARTBEAT PATTERN para monitoramento
     */
    public void startHeartbeat(int intervalSeconds) {
        ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
        
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            IoTProtos.HeartbeatRequest request = IoTProtos.HeartbeatRequest.newBuilder()
                .setSensorId(this.sensorId)
                .setStatus(IoTProtos.SensorStatus.ACTIVE)
                .setTimestamp(System.currentTimeMillis())
                .build();
            
            try {
                IoTProtos.HeartbeatResponse response = stub.heartbeat(request);
                logger.debug("💓 [Sensor] Heartbeat: {}", response.getMessage());
            } catch (Exception e) {
                logger.warn("⚠️ [Sensor] Heartbeat failed: {}", e.getMessage());
            }
        }, 10, intervalSeconds, TimeUnit.SECONDS);
    }
}
```

### 🔄 **Sensor Lifecycle Management**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                    SENSOR LIFECYCLE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. INITIALIZATION                                              │
│     ┌─────────────────┐                                         │
│     │ Create Channel  │                                         │  
│     │ Build Stub      │                                         │
│     │ Setup Config    │                                         │
│     └─────────────────┘                                         │
│              │                                                  │
│              ▼                                                  │
│  2. REGISTRATION                                                │
│     ┌─────────────────┐    gRPC     ┌─────────────────┐        │
│     │ SensorInfo      │─────────────▶│ Gateway         │        │
│     │ VersionVector   │              │ Registration    │        │
│     │ Capabilities    │◄─────────────│ Response        │        │
│     └─────────────────┘              └─────────────────┘        │
│              │                                                  │
│              ▼                                                  │
│  3. OPERATIONAL                                                 │
│     ┌─────────────────┐                                         │
│     │ Data Stream     │──┐                                      │
│     │ Heartbeat       │  │ Parallel                             │
│     │ Error Handling  │  │ Operations                           │
│     └─────────────────┘  │                                      │
│     ┌─────────────────┐  │                                      │
│     │ Health Monitor  │◄─┘                                      │
│     │ Reconnection    │                                         │
│     │ Fault Recovery  │                                         │
│     └─────────────────┘                                         │
│              │                                                  │
│              ▼                                                  │
│  4. SHUTDOWN                                                    │
│     ┌─────────────────┐                                         │
│     │ Graceful Stop   │                                         │
│     │ Channel Close   │                                         │
│     │ Resource Clean  │                                         │
│     └─────────────────┘                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. **DATA RECEIVERS**

### 🗄️ **Data Receiver Architecture**

```java
/**
 * Data Receiver com integração gRPC
 */
public class DataReceiver {
    
    // === STATE MANAGEMENT ===
    private Map<String, IoTMessage> database = new ConcurrentHashMap<>();
    private Map<String, Long> versionVector = new ConcurrentHashMap<>();
    private String receiverId;
    private int port;
    
    // === gRPC INTEGRATION ===
    private boolean isProcessingGRPCMessage = false;
    
    /**
     * STRATEGY PATTERN - Processamento por protocolo
     */
    public boolean processMessage(IoTMessage message, String protocol) {
        logger.info("📨 [{}] Processando mensagem {} via {}", 
            receiverId, message.getSensorId(), protocol);
            
        switch (protocol) {
            case "UDP":
                return processUDPMessage(message);
            case "HTTP":
                return processHTTPMessage(message);
            case "GRPC":
                return processGRPCMessage(message);    // 🆕 New case
            case "TCP":
                return processTCPMessage(message);
            default:
                logger.warn("⚠️ [{}] Protocolo desconhecido: {}", receiverId, protocol);
                return false;
        }
    }
    
    /**
     * gRPC-specific processing with enhanced features
     */
    private boolean processGRPCMessage(IoTMessage message) {
        isProcessingGRPCMessage = true;
        
        try {
            // 1. VALIDATION - Type safety já garantida pelo Proto
            if (!validateGRPCMessage(message)) {
                logger.warn("⚠️ [{}] Mensagem gRPC inválida: {}", receiverId, message.getSensorId());
                return false;
            }
            
            // 2. VERSION VECTOR - Enhanced for gRPC
            boolean isNewVersion = updateVersionVectorForGRPC(message);
            if (!isNewVersion) {
                logger.debug("🔄 [{}] Mensagem duplicada (gRPC): {}", receiverId, message.getSensorId());
                return true; // Not an error, just duplicate
            }
            
            // 3. BUSINESS LOGIC - Same across all protocols
            database.put(message.getSensorId(), message);
            
            // 4. REPLICATION - Notify other receivers
            replicateToOtherReceivers(message, "GRPC");
            
            // 5. METRICS - gRPC specific metrics
            updateGRPCMetrics(message);
            
            logger.info("✅ [{}] Mensagem gRPC processada: {} (VV: {})", 
                receiverId, message.getSensorId(), versionVector.get(message.getSensorId()));
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ [{}] Erro ao processar mensagem gRPC: {}", receiverId, e.getMessage());
            return false;
        } finally {
            isProcessingGRPCMessage = false;
        }
    }
    
    /**
     * Enhanced Version Vector for gRPC with type safety
     */
    private boolean updateVersionVectorForGRPC(IoTMessage message) {
        String sensorId = message.getSensorId();
        
        // Get current version
        Long currentVersion = versionVector.getOrDefault(sensorId, 0L);
        
        // Extract version from gRPC message (Protocol Buffers guarantee type safety)
        Long messageVersion = extractVersionFromMessage(message);
        
        if (messageVersion <= currentVersion) {
            return false; // Old or duplicate message
        }
        
        // Update with new version
        versionVector.put(sensorId, messageVersion);
        
        logger.debug("🔢 [{}] Version Vector atualizado: {}={} (via gRPC)", 
            receiverId, sensorId, messageVersion);
        
        return true;
    }
    
    /**
     * gRPC-specific validation (leveraging Protocol Buffers type safety)
     */
    private boolean validateGRPCMessage(IoTMessage message) {
        // Basic validations - most type errors are caught at compile-time with Proto
        if (message.getSensorId() == null || message.getSensorId().isEmpty()) {
            return false;
        }
        
        if (message.getType() == null) {
            return false;
        }
        
        // gRPC messages come with guaranteed type safety from Protocol Buffers
        return true;
    }
    
    /**
     * Metrics específicas para gRPC
     */
    private void updateGRPCMetrics(IoTMessage message) {
        // Update gRPC-specific counters
        incrementCounter("grpc_messages_processed");
        recordLatency("grpc_processing_time", System.currentTimeMillis() - message.getTimestamp());
        
        // Protocol Buffers size efficiency
        recordGRPCMessageSize(message);
    }
}
```

### 🔄 **Data Receiver Interaction Patterns**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                DATA RECEIVER INTERACTIONS                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  SCENARIO 1: gRPC Message Processing                           │
│                                                                 │
│  ┌─────────────┐    gRPC     ┌─────────────────┐               │
│  │   Gateway   │─────────────▶│ DataReceiver1   │               │
│  │             │              │                 │               │
│  │ - Route Msg │              │ - Validate      │               │
│  │ - Load Bal  │              │ - Version Vec   │               │
│  │ - Failover  │              │ - Store         │               │
│  └─────────────┘              │ - Replicate     │               │
│                                └─────────────────┘               │
│                                         │                       │
│                                         │ Replication           │
│                                         ▼                       │
│                                ┌─────────────────┐               │
│                                │ DataReceiver2   │               │
│                                │                 │               │
│                                │ - Sync VV       │               │
│                                │ - Conflict Res  │               │
│                                │ - Backup        │               │
│                                └─────────────────┘               │
│                                                                 │
│  SCENARIO 2: Fault Tolerance                                   │
│                                                                 │
│  ┌─────────────┐    gRPC     ┌─────────────────┐               │
│  │   Gateway   │─────X──────▶│ DataReceiver1   │ (FAILED)      │
│  │             │              │     DOWN        │               │
│  │ - Detect    │              └─────────────────┘               │
│  │ - Failover  │                                                │
│  │ - Reroute   │    gRPC     ┌─────────────────┐               │
│  │             │─────────────▶│ DataReceiver2   │ (ACTIVE)      │
│  └─────────────┘              │ - Process       │               │
│                                │ - Queue for R1  │               │
│                                │ - Auto Recovery │               │
│                                └─────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. **PADRÕES DE COMUNICAÇÃO**

### 🔄 **Request-Response Pattern**

```java
/**
 * Padrão Unary Request-Response (mais comum)
 */
public class UnaryGRPCPattern {
    
    // CLIENT SIDE
    public void sendSensorData(SensorMeasurement measurement) {
        IoTProtos.SensorDataRequest request = buildRequest(measurement);
        
        // Synchronous unary call
        IoTProtos.SensorDataResponse response = stub.sendSensorData(request);
        
        handleResponse(response);
    }
    
    // SERVER SIDE  
    @Override
    public void sendSensorData(IoTProtos.SensorDataRequest request,
                             StreamObserver<IoTProtos.SensorDataResponse> responseObserver) {
        
        // Process request
        IoTMessage message = convertFromProto(request.getIotMessage());
        boolean success = processMessage(message);
        
        // Build response
        IoTProtos.SensorDataResponse response = IoTProtos.SensorDataResponse.newBuilder()
            .setSuccess(success)
            .setMessage(success ? "Processado com sucesso" : "Erro no processamento")
            .setProcessedBy(getReceiverId())
            .build();
        
        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### 📡 **Streaming Patterns**

```java
/**
 * Padrão Client Streaming (múltiplas mensagens → uma resposta)
 */
public class ClientStreamingPattern {
    
    // SERVER SIDE
    @Override
    public StreamObserver<IoTProtos.SensorDataRequest> streamSensorData(
            StreamObserver<IoTProtos.SensorDataResponse> responseObserver) {
        
        return new StreamObserver<IoTProtos.SensorDataRequest>() {
            private List<IoTMessage> batch = new ArrayList<>();
            
            @Override
            public void onNext(IoTProtos.SensorDataRequest request) {
                // Collect messages in batch
                IoTMessage message = convertFromProto(request.getIotMessage());
                batch.add(message);
                
                logger.debug("📥 [Streaming] Recebida mensagem {} (batch size: {})", 
                    message.getSensorId(), batch.size());
            }
            
            @Override
            public void onCompleted() {
                // Process entire batch
                int processed = processBatch(batch);
                
                // Send single response
                IoTProtos.SensorDataResponse response = IoTProtos.SensorDataResponse.newBuilder()
                    .setSuccess(processed == batch.size())
                    .setMessage(String.format("Processadas %d/%d mensagens", processed, batch.size()))
                    .setProcessedBy(getReceiverId())
                    .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
                logger.info("✅ [Streaming] Batch processado: {}/{} mensagens", 
                    processed, batch.size());
            }
            
            @Override
            public void onError(Throwable t) {
                logger.error("❌ [Streaming] Erro no client streaming: {}", t.getMessage());
            }
        };
    }
}

/**
 * Padrão Bidirectional Streaming (tempo real)
 */
public class BidirectionalStreamingPattern {
    
    // SERVER SIDE
    @Override
    public StreamObserver<IoTProtos.SensorDataRequest> realtimeDataExchange(
            StreamObserver<IoTProtos.SensorDataResponse> responseObserver) {
        
        return new StreamObserver<IoTProtos.SensorDataRequest>() {
            
            @Override
            public void onNext(IoTProtos.SensorDataRequest request) {
                // Process message immediately
                IoTMessage message = convertFromProto(request.getIotMessage());
                boolean success = processMessage(message);
                
                // Send immediate response
                IoTProtos.SensorDataResponse response = IoTProtos.SensorDataResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage("Processamento em tempo real")
                    .setProcessedBy(getReceiverId())
                    .setUpdatedVersionVector(buildCurrentVersionVector())
                    .build();
                
                responseObserver.onNext(response);
                
                // Optional: Send additional notifications
                if (shouldNotifyOthers(message)) {
                    sendNotificationToOtherSensors(message, responseObserver);
                }
            }
            
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                logger.info("🔄 [Streaming] Sessão bidirectional encerrada");
            }
            
            @Override
            public void onError(Throwable t) {
                logger.error("❌ [Streaming] Erro no bidirectional streaming: {}", t.getMessage());
            }
        };
    }
}
```

---

## 6. **VERSION VECTOR INTEGRATION**

### 🔢 **Version Vector com Protocol Buffers**

```java
/**
 * Integração do Version Vector com gRPC/Protocol Buffers
 */
public class VersionVectorGRPCIntegration {
    
    /**
     * Conversão Java Version Vector → Protocol Buffers
     */
    public static IoTProtos.VersionVector convertToProto(Map<String, Long> javaVV) {
        IoTProtos.VersionVector.Builder builder = IoTProtos.VersionVector.newBuilder();
        
        javaVV.forEach((sensorId, version) -> {
            builder.putVector(sensorId, version);
        });
        
        return builder.build();
    }
    
    /**
     * Conversão Protocol Buffers → Java Version Vector
     */
    public static Map<String, Long> convertFromProto(IoTProtos.VersionVector protoVV) {
        Map<String, Long> javaVV = new ConcurrentHashMap<>();
        
        protoVV.getVectorMap().forEach((sensorId, version) -> {
            javaVV.put(sensorId, version);
        });
        
        return javaVV;
    }
    
    /**
     * Merge de Version Vectors com resolução de conflitos
     */
    public static Map<String, Long> mergeVersionVectors(
            Map<String, Long> local, 
            IoTProtos.VersionVector remote) {
        
        Map<String, Long> result = new ConcurrentHashMap<>(local);
        Map<String, Long> remoteMap = convertFromProto(remote);
        
        remoteMap.forEach((sensorId, remoteVersion) -> {
            Long localVersion = result.getOrDefault(sensorId, 0L);
            
            // Vector clock merge rule: max(local, remote)
            result.put(sensorId, Math.max(localVersion, remoteVersion));
        });
        
        logger.debug("🔢 Version Vector merge: {} + {} = {}", 
            local.size(), remoteMap.size(), result.size());
        
        return result;
    }
    
    /**
     * Detecção de conflitos com Version Vector
     */
    public static ConflictResolution detectConflict(
            IoTMessage localMessage, 
            IoTMessage remoteMessage) {
        
        Map<String, Long> localVV = localMessage.getVersionVector();
        Map<String, Long> remoteVV = remoteMessage.getVersionVector();
        
        String sensorId = localMessage.getSensorId();
        Long localVersion = localVV.getOrDefault(sensorId, 0L);
        Long remoteVersion = remoteVV.getOrDefault(sensorId, 0L);
        
        if (localVersion > remoteVersion) {
            return ConflictResolution.LOCAL_WINS;
        } else if (remoteVersion > localVersion) {
            return ConflictResolution.REMOTE_WINS;
        } else {
            // Same version - check timestamp as tiebreaker
            if (localMessage.getTimestamp() > remoteMessage.getTimestamp()) {
                return ConflictResolution.LOCAL_WINS;
            } else {
                return ConflictResolution.REMOTE_WINS;
            }
        }
    }
    
    public enum ConflictResolution {
        LOCAL_WINS,
        REMOTE_WINS,
        MERGE_REQUIRED
    }
}
```

### 🔄 **Version Vector Flow Diagram**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│              VERSION VECTOR WITH gRPC FLOW                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. MESSAGE CREATION                                            │
│     ┌─────────────────┐                                         │
│     │ Sensor          │                                         │
│     │ VV: {TEMP_01=5} │                                         │
│     └─────────────────┘                                         │
│              │ Increment & Send                                 │
│              ▼                                                  │
│  2. gRPC TRANSMISSION                                           │
│     ┌─────────────────┐    Protocol     ┌─────────────────┐    │
│     │ ProtoMessage    │    Buffers      │ Gateway         │    │
│     │ VV: {TEMP_01=6} │───────────────▶│ Receives        │    │
│     │ Type: SENSOR_DATA│                │ & Validates     │    │
│     └─────────────────┘                 └─────────────────┘    │
│                                                  │              │
│                                                  ▼              │
│  3. DATA RECEIVER PROCESSING                                    │
│     ┌─────────────────┐                ┌─────────────────┐     │
│     │ DataReceiver1   │                │ DataReceiver2   │     │
│     │ Local: {TEMP_01=4}               │ Local: {TEMP_01=5}    │
│     │ Incoming: {TEMP_01=6}            │ Sync: {TEMP_01=6}     │
│     │ Result: ACCEPT   │◄──────────────│ Result: ACCEPT  │     │
│     └─────────────────┘    Replication └─────────────────┘     │
│              │                                  │              │
│              ▼                                  ▼              │
│  4. VERSION VECTOR UPDATE                                       │
│     ┌─────────────────┐                ┌─────────────────┐     │
│     │ Updated VV:     │                │ Updated VV:     │     │
│     │ {TEMP_01=6,     │                │ {TEMP_01=6,     │     │
│     │  HUMID_01=3}    │                │  HUMID_01=3}    │     │
│     └─────────────────┘                └─────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. **FAULT TOLERANCE**

### 🛡️ **Fault Tolerance Mechanisms**

```java
/**
 * Tolerância a Falhas específica para gRPC
 */
public class GRPCFaultTolerance {
    
    /**
     * CIRCUIT BREAKER PATTERN para gRPC
     */
    public class GRPCCircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private State state = State.CLOSED;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private final int failureThreshold = 5;
        private final long timeout = 60000; // 1 minute
        
        public <T> T execute(Supplier<T> grpcCall) throws Exception {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime < timeout) {
                    throw new RuntimeException("Circuit breaker is OPEN");
                } else {
                    state = State.HALF_OPEN;
                    logger.info("🔄 [Circuit] Tentando HALF_OPEN para gRPC");
                }
            }
            
            try {
                T result = grpcCall.get();
                
                // Success - reset circuit breaker
                if (state == State.HALF_OPEN) {
                    state = State.CLOSED;
                    failureCount = 0;
                    logger.info("✅ [Circuit] gRPC circuit breaker CLOSED");
                }
                
                return result;
                
            } catch (Exception e) {
                failureCount++;
                lastFailureTime = System.currentTimeMillis();
                
                if (failureCount >= failureThreshold) {
                    state = State.OPEN;
                    logger.warn("🔴 [Circuit] gRPC circuit breaker OPEN após {} falhas", failureCount);
                }
                
                throw e;
            }
        }
    }
    
    /**
     * RETRY PATTERN com Exponential Backoff
     */
    public class GRPCRetryPolicy {
        private final int maxRetries = 3;
        private final long baseDelay = 1000; // 1 second
        
        public <T> T executeWithRetry(Supplier<T> grpcCall) throws Exception {
            Exception lastException = null;
            
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    return grpcCall.get();
                    
                } catch (StatusRuntimeException e) {
                    lastException = e;
                    
                    if (attempt == maxRetries) {
                        logger.error("❌ [Retry] gRPC falhou após {} tentativas: {}", 
                            maxRetries + 1, e.getStatus());
                        break;
                    }
                    
                    // Check if error is retryable
                    if (!isRetryableError(e.getStatus())) {
                        logger.warn("⚠️ [Retry] Erro não-retryável: {}", e.getStatus());
                        throw e;
                    }
                    
                    // Exponential backoff
                    long delay = baseDelay * (1L << attempt);
                    logger.warn("🔄 [Retry] Tentativa {} falhou, tentando novamente em {}ms", 
                        attempt + 1, delay);
                    
                    Thread.sleep(delay);
                }
            }
            
            throw lastException;
        }
        
        private boolean isRetryableError(Status status) {
            return status.getCode() == Status.Code.UNAVAILABLE ||
                   status.getCode() == Status.Code.DEADLINE_EXCEEDED ||
                   status.getCode() == Status.Code.RESOURCE_EXHAUSTED;
        }
    }
    
    /**
     * FAILOVER PATTERN para múltiplos gateways
     */
    public class GRPCFailover {
        private List<String> gatewayAddresses;
        private int currentGatewayIndex = 0;
        private ManagedChannel currentChannel;
        private IoTGatewayServiceGrpc.IoTGatewayServiceBlockingStub currentStub;
        
        public void initializeWithFailover(List<String> addresses) {
            this.gatewayAddresses = new ArrayList<>(addresses);
            connectToGateway(0);
        }
        
        private void connectToGateway(int index) {
            if (currentChannel != null) {
                currentChannel.shutdown();
            }
            
            String address = gatewayAddresses.get(index);
            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            currentChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
                
            currentStub = IoTGatewayServiceGrpc.newBlockingStub(currentChannel);
            currentGatewayIndex = index;
            
            logger.info("🔄 [Failover] Conectado ao gateway: {}", address);
        }
        
        public <T> T executeWithFailover(Function<IoTGatewayServiceGrpc.IoTGatewayServiceBlockingStub, T> grpcCall) 
                throws Exception {
            
            Exception lastException = null;
            
            // Try all gateways
            for (int attempt = 0; attempt < gatewayAddresses.size(); attempt++) {
                try {
                    return grpcCall.apply(currentStub);
                    
                } catch (StatusRuntimeException e) {
                    lastException = e;
                    logger.warn("⚠️ [Failover] Gateway {} falhou: {}", 
                        gatewayAddresses.get(currentGatewayIndex), e.getStatus());
                    
                    // Try next gateway
                    int nextIndex = (currentGatewayIndex + 1) % gatewayAddresses.size();
                    connectToGateway(nextIndex);
                }
            }
            
            logger.error("❌ [Failover] Todos os gateways falharam");
            throw lastException;
        }
    }
}
```

### 🔄 **Fault Recovery Flow**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                    gRPC FAULT RECOVERY FLOW                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  NORMAL OPERATION                                               │
│  ┌─────────────┐    gRPC     ┌─────────────────────────────┐   │
│  │   Sensor    │─────────────▶│        Gateway              │   │
│  │             │              │       (Primary)             │   │
│  │ - Send Data │              │ - Process                   │   │
│  │ - Heartbeat │              │ - Route                     │   │
│  └─────────────┘              │ - Respond                   │   │
│                                └─────────────────────────────┘   │
│                                                                 │
│  FAILURE DETECTION                                              │
│  ┌─────────────┐    Timeout   ┌─────────────────────────────┐   │
│  │   Sensor    │─────X───────▷│        Gateway              │   │
│  │             │              │      (Failed/Slow)          │   │
│  │ - Detect    │              │                             │   │
│  │ - Circuit   │              │                             │   │
│  │   Breaker   │              │                             │   │
│  └─────────────┘              └─────────────────────────────┘   │
│         │                                                       │
│         │ Failover                                              │
│         ▼                                                       │
│  FAILOVER TO BACKUP                                             │
│  ┌─────────────┐    gRPC     ┌─────────────────────────────┐   │
│  │   Sensor    │─────────────▶│        Gateway              │   │
│  │             │              │       (Backup)              │   │
│  │ - Reconnect │              │ - Take Over                 │   │
│  │ - Resume    │              │ - Process                   │   │
│  │ - Monitor   │              │ - Sync State                │   │
│  └─────────────┘              └─────────────────────────────┘   │
│         │                                                       │
│         │ Health Check                                          │
│         ▼                                                       │
│  RECOVERY ATTEMPT                                               │
│  ┌─────────────┐   Health     ┌─────────────────────────────┐   │
│  │   Sensor    │   Check      │        Gateway              │   │
│  │             │─────────────▶│       (Primary)             │   │
│  │ - Test Conn │  Circuit     │ - Recovered?                │   │
│  │ - Validate  │  Half-Open   │ - Health OK?                │   │
│  └─────────────┘              └─────────────────────────────┘   │
│                                         │                       │
│                                         │ If OK                 │
│                                         ▼                       │
│  RETURN TO NORMAL                                               │
│  ┌─────────────┐    gRPC     ┌─────────────────────────────┐   │
│  │   Sensor    │◄────────────│        Gateway              │   │
│  │             │              │       (Primary)             │   │
│  │ - Full Ops  │              │ - Normal Service            │   │
│  │ - Reset     │              │ - Circuit Closed            │   │
│  │   Circuit   │              │                             │   │
│  └─────────────┘              └─────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

**📝 Documentação de Arquitetura criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0  
**🎯 Foco:** Componentes, Sensores, Data Receivers e Comunicação gRPC