# Data Receivers - Version Vector em Componentes Stateful

## Conceito de Componente Stateful

Os **Data Receivers** são os componentes **stateful** do sistema IoT, responsáveis por **armazenar e sincronizar estado** entre diferentes instâncias distribuídas. Cada Data Receiver mantém:

1. **Base de dados local** de sensores e suas leituras
2. **Version Vector próprio** para ordenação causal
3. **Mecanismos de sincronização** com outros receivers
4. **Resolução de conflitos** usando Version Vector + Last Write Wins

## Arquitetura Stateful dos Data Receivers

```
            ┌─────────────────────────────────────────┐
            │            DataReceiver                 │
            ├─────────────────────────────────────────┤
            │                                         │
    ┌───────┤ • receiverId (único)                    │
    │       │ • sensorDatabase (stateful)             │
    │       │ • versionVector (ConcurrentHashMap)     │
    │       │ • totalMessages (AtomicLong)            │
    │       │ • conflictsResolved (AtomicLong)        │
    │       │                                         │
    │       └─────────────────────────────────────────┘
    │                           │
    │                           ▼
    │       ┌─────────────────────────────────────────┐
    │       │         State Management                │
    │       │                                         │
    │       │ ┌─────────────┐  ┌─────────────────┐   │
    │       │ │ Sensor Data │  │ Version Vector  │   │
    │       │ │ Database    │  │ Maintenance     │   │
    │       │ │             │  │                 │   │
    │       │ │ TEMP_01 =   │  │ TEMP_01: 5      │   │
    │       │ │ 23.5°C      │  │ DR_1: 12        │   │
    │       │ │ @10:30:15   │  │ DR_2: 8         │   │
    │       │ │ VV_Clock=5  │  │ GATEWAY: 3      │   │
    │       │ └─────────────┘  └─────────────────┘   │
    │       └─────────────────────────────────────────┘
    │
    └─► Sincronização periódica com outros Data Receivers
```

## Version Vector nos Data Receivers

### Inicialização do Version Vector

Cada Data Receiver inicia com **Version Vector vazio** e o **constrói gradualmente**:

```java
// Inicialização no construtor
private final ConcurrentHashMap<String, Long> versionVector = new ConcurrentHashMap<>();

// Estado inicial: {}
// Após primeiro sensor: {TEMP_SENSOR_01=1}  
// Após processamento: {TEMP_SENSOR_01=1, DATA_RECEIVER_1=1}
// Após sincronização: {TEMP_SENSOR_01=1, DR_1=1, DR_2=3, GATEWAY=2}
```

### Atualização Automática do Version Vector

A cada mensagem processada, o Version Vector é **automaticamente atualizado**:

```java
private void updateVersionVector(IoTMessage message) {
    if (message.getVersionVector() != null) {
        // Merge com version vector da mensagem recebida
        message.getVersionVector().forEach((senderId, version) -> {
            versionVector.merge(senderId, version.longValue(), Long::max);
        });
        
        // Incrementa contador próprio (evento local)
        versionVector.merge(receiverId, 1L, Long::sum);
        
        logger.debug("🔍 [{}] VERSION VECTOR ATUALIZADO: {} → {}", 
                    receiverId, message.getSensorId(), versionVector);
    }
}
```

**Processo Detalhado**:
1. **Recebe mensagem** com Version Vector do remetente
2. **Merge inteligente**: Para cada entrada, mantém o valor máximo
3. **Incrementa próprio contador**: Registra processamento como evento local
4. **Log detalhado**: Documenta evolução do Version Vector

## Armazenamento de Estado com Version Vector

### Estrutura SensorDataEntry

Cada entrada de sensor armazena **dados + informação causal**:

```java
public class SensorDataEntry {
    private final String sensorId;
    private final double value;
    private final String type;
    private final LocalDateTime timestamp;      // Para Last Write Wins
    private final long versionVectorClock;      // Para desempate causal
    
    // Estado típico:
    // sensorId: "TEMP_SENSOR_01"
    // value: 23.5
    // type: "TEMPERATURE"  
    // timestamp: 2025-10-01T10:30:15
    // versionVectorClock: 5 (valor do VV para este sensor)
}
```

### Base de Dados Local Thread-Safe

```java
private final ConcurrentHashMap<String, SensorDataEntry> sensorDatabase = new ConcurrentHashMap<>();

// Exemplo de estado:
// {
//   "TEMP_SENSOR_01" -> SensorDataEntry{value=23.5, timestamp=10:30:15, vvClock=5},
//   "HUMIDITY_02" -> SensorDataEntry{value=65.2, timestamp=10:30:14, vvClock=3},
//   "PRESSURE_03" -> SensorDataEntry{value=1013.2, timestamp=10:30:16, vvClock=7}
// }
```

## Resolução de Conflitos com Version Vector

### Algoritmo Híbrido: Last Write Wins + Version Vector

O sistema usa **duas camadas de resolução**:

```java
private void processSensorData(IoTMessage message) {
    // Criar nova entrada com informação causal
    SensorDataEntry newEntry = new SensorDataEntry(
        sensorId,
        message.getSensorValue(),
        message.getSensorType(),
        message.getTimestamp(),
        versionVector.getOrDefault(sensorId, 0L)  // VV clock atual
    );
    
    SensorDataEntry existingEntry = sensorDatabase.get(sensorId);
    
    if (existingEntry != null) {
        // PRIMEIRO CRITÉRIO: Last Write Wins por timestamp
        if (newEntry.getTimestamp().isAfter(existingEntry.getTimestamp())) {
            sensorDatabase.put(sensorId, newEntry);
            logger.info("✅ Atualizado por timestamp: {} = {}", sensorId, newEntry);
            
        } else if (newEntry.getTimestamp().equals(existingEntry.getTimestamp())) {
            // SEGUNDO CRITÉRIO: Version Vector como desempate
            if (newEntry.getVersionVectorClock() > existingEntry.getVersionVectorClock()) {
                sensorDatabase.put(sensorId, newEntry);
                logger.info("✅ Atualizado por Version Vector: {} = {}", sensorId, newEntry);
            } else {
                conflictsResolved.incrementAndGet();
                logger.warn("⚠️ CONFLITO RESOLVIDO: {} mantido por VV", sensorId);
            }
        }
    } else {
        // Primeira entrada - sempre aceita
        sensorDatabase.put(sensorId, newEntry);
        logger.info("✅ Novo sensor registrado: {} = {}", sensorId, newEntry);
    }
}
```

### Cenários de Resolução de Conflitos

#### Cenário 1: Timestamps Diferentes
```
Entrada Existente: temp=25°C, time=10:30:15, vv_clock=3
Nova Entrada:      temp=23°C, time=10:30:17, vv_clock=5

Resolução: Nova entrada ACEITA (timestamp mais recente)
Resultado: temp=23°C armazenado
```

#### Cenário 2: Timestamps Iguais, Version Vector Diferente
```
Entrada Existente: temp=25°C, time=10:30:15, vv_clock=3  
Nova Entrada:      temp=23°C, time=10:30:15, vv_clock=5

Resolução: Nova entrada ACEITA (VV clock maior = mais causal)
Resultado: temp=23°C armazenado
```

#### Cenário 3: Timestamps Iguais, Version Vector Igual
```
Entrada Existente: temp=25°C, time=10:30:15, vv_clock=5
Nova Entrada:      temp=23°C, time=10:30:15, vv_clock=5

Resolução: Entrada existente MANTIDA (primeiro a chegar prevalece)
Resultado: temp=25°C mantido, conflito registrado
```

## Sincronização Entre Data Receivers

### Backup e Restore com Version Vector

```java
public ReceiverBackup createBackup() {
    return new ReceiverBackup(
        receiverId,
        new ConcurrentHashMap<>(sensorDatabase),
        new ConcurrentHashMap<>(versionVector),  // VV incluído no backup
        totalMessages.get(),
        System.currentTimeMillis()
    );
}

public void restoreFromBackup(ReceiverBackup backup) {
    // Restaurar dados
    this.sensorDatabase.clear();
    this.sensorDatabase.putAll(backup.getSensorDatabase());
    
    // Merge Version Vectors (NÃO sobrescrever!)
    backup.getVersionVector().forEach((nodeId, clock) -> {
        this.versionVector.merge(nodeId, clock, Long::max);
    });
    
    logger.info("✅ [{}] Backup restaurado: {} sensores, VV={}", 
               receiverId, sensorDatabase.size(), versionVector);
}
```

**Ponto Crítico**: Durante restore, os Version Vectors são **merged**, não substituídos, preservando **informação causal local**.

### Processo de Sincronização

```
DataReceiver_1                    DataReceiver_2
VV={TEMP=5, DR_1=12}             VV={TEMP=3, DR_2=8}
     │                                │
     ▼                                ▼
1. Create Backup ◄─────────────────► Create Backup
     │                                │
     ▼                                ▼
2. Compare Totals ◄────────────────► Compare Totals
   (12 messages)                     (8 messages)
     │                                │
     ▼                                ▼
3. DR_1 is source ─────restore────► DR_2 is target
     │                                │
     ▼                                ▼
4. DR_2 merges VV: {TEMP=5, DR_1=12, DR_2=8}
     │                                │
     ▼                                ▼
   Final State:                   Final State:
   VV={TEMP=5, DR_1=12}          VV={TEMP=5, DR_1=12, DR_2=8}
```

## Observabilidade do Version Vector

### Logs Estruturados de Estado

```java
// A cada 10 mensagens processadas
logger.info("📊 [{}] Stats: Mensagens={}, Sensores={}, Conflitos={}, VV={}", 
           receiverId, totalMessages.get(), sensorDatabase.size(), 
           conflictsResolved.get(), versionVector);
```

**Exemplo de Log Real**:
```
📊 [DATA_RECEIVER_1] Stats: Mensagens=50, Sensores=3, Conflitos=2, VV={TEMP_01=15, DR_1=50, DR_2=23, GATEWAY=12}
🔍 [DATA_RECEIVER_1] VERSION VECTOR ATUALIZADO: TEMP_SENSOR_01 → {TEMP_01=16, DR_1=51, DR_2=23, GATEWAY=12}
✅ [DATA_RECEIVER_1] Dados atualizados: TEMP_01 = 24.8°C (Last Write Wins - Mais recente)
```

### Métricas de Consistência

```java
// Coletadas automaticamente
private final AtomicLong totalMessages = new AtomicLong(0);
private final AtomicLong conflictsResolved = new AtomicLong(0);

// Taxa de conflitos
public double getConflictRate() {
    long total = totalMessages.get();
    long conflicts = conflictsResolved.get();
    return total > 0 ? (double) conflicts / total * 100.0 : 0.0;
}
```

## Health Check e Estado do Version Vector

```java
public boolean isHealthy() {
    return running.get() && 
           socket != null && 
           !socket.isClosed() &&
           versionVector.size() > 0;  // VV deve ter pelo menos uma entrada
}

// Diagnóstico completo
public String getDetailedStatus() {
    return String.format(
        "DataReceiver[%s]: running=%s, messages=%d, sensors=%d, conflicts=%d, vv_size=%d, vv=%s",
        receiverId, running.get(), totalMessages.get(), sensorDatabase.size(),
        conflictsResolved.get(), versionVector.size(), versionVector
    );
}
```

## Integração com Protocolos de Comunicação

### UDP: Version Vector em Datagramas
```java
// Recepção UDP com VV
byte[] buffer = new byte[2048];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
socket.receive(packet);

IoTMessage message = deserializeMessage(packet.getData());
updateVersionVector(message);  // Atualiza VV automaticamente
processSensorData(message);    // Processa com resolução de conflitos
```

### HTTP: Version Vector em Headers
```java
// Version Vector enviado como header JSON
response.setHeader("X-Version-Vector", 
                  objectMapper.writeValueAsString(versionVector));
```

### gRPC: Version Vector em Protocol Buffers
```protobuf
message IoTMessage {
    string sensor_id = 1;
    double sensor_value = 2;
    VersionVector version_vector = 3;
}

message VersionVector {
    map<string, int64> vector = 1;  // node_id -> clock
}
```

Os Data Receivers demonstram como **componentes stateful** podem manter **consistência causal** em sistemas distribuídos através do uso inteligente de **Version Vectors** combinados com **estratégias de resolução de conflitos**.