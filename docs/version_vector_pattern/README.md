# Version Vector Pattern - Consistência Causal em Sistemas IoT Distribuídos

## Conceito Fundamental

O **Version Vector** é um mecanismo de ordenação causal que permite a sistemas distribuídos **detectar relações de dependência** entre eventos ocorridos em diferentes nós da rede. No contexto deste sistema IoT, o Version Vector resolve o problema fundamental de **manter consistência de dados** entre múltiplos Data Receivers que processam mensagens de sensores de forma concorrente.

## Problema da Consistência Distribuída

### Cenário Típico Sem Version Vector
```
Sensor ─┬─► DataReceiver_1 (dados: temp=25°C, time=10:30:15)
        └─► DataReceiver_2 (dados: temp=23°C, time=10:30:16)

Problema: Qual é o valor correto da temperatura?
- Por timestamp: 23°C (mais recente)
- Por ordem de chegada: indeterminado
- Por conteúdo: ambíguo
```

### Solução com Version Vector
```
Sensor envía: 
├─► Msg1: temp=25°C, VV={SENSOR_01=1}
└─► Msg2: temp=23°C, VV={SENSOR_01=2}

DataReceiver pode determinar:
- Msg2 aconteceu DEPOIS de Msg1 (VV[SENSOR_01]: 2 > 1)
- temp=23°C é o valor mais atual causalmente
- Ordem causal é independente de timestamps físicos
```

## Arquitetura do Version Vector

```
                    ┌─────────────────────────────────┐
                    │        IoT Message              │
                    │                                 │
                    │ ┌─────────────────────────────┐ │
                    │ │     Version Vector          │ │
                    │ │                             │ │
                    │ │ SENSOR_01: 5               │ │
                    │ │ DATA_RECEIVER_1: 12        │ │
                    │ │ DATA_RECEIVER_2: 8         │ │
                    │ │ GATEWAY: 3                 │ │
                    │ └─────────────────────────────┘ │
                    └─────────────────────────────────┘
                                    │
                    ┌───────────────▼───────────────┐
                    │    Causal Ordering Logic       │
                    │                               │
                    │ happensBefore(Message other)  │
                    │ isConcurrentWith(Message)     │
                    │ mergeVersionVector(VV other)  │
                    └───────────────────────────────┘
```

## Implementação no Sistema IoT

### Estrutura Básica do Version Vector

O Version Vector é implementado como um **ConcurrentHashMap<String, Integer>** onde:
- **Chave (String)**: Identificador único do nó (sensor, receiver, gateway)
- **Valor (Integer)**: Contador lógico de eventos para aquele nó
- **ConcurrentHashMap**: Garante thread-safety em ambiente distribuído

### Evolução do Version Vector

```java
// Inicialização
Map<String, Integer> vv = new ConcurrentHashMap<>();
// Estado inicial: {}

// Sensor envia primeira mensagem
vv.put("TEMP_SENSOR_01", 1);
// Estado: {TEMP_SENSOR_01=1}

// DataReceiver processa mensagem
vv.put("DATA_RECEIVER_1", 1);
// Estado: {TEMP_SENSOR_01=1, DATA_RECEIVER_1=1}

// Sincronização com outro receiver
vv.merge("DATA_RECEIVER_2", 5, Integer::max);
// Estado: {TEMP_SENSOR_01=1, DATA_RECEIVER_1=1, DATA_RECEIVER_2=5}
```

## Operações Fundamentais

### 1. Incremento Local
Quando um nó **gera um evento local** (processa mensagem, cria dados):
```java
public void incrementVersionVector(String nodeId) {
    versionVector.compute(nodeId, (k, v) -> (v == null) ? 1 : v + 1);
}
```

**Resultado**: O contador do nó local é incrementado, indicando que **um novo evento causal** aconteceu neste nó.

### 2. Merge de Version Vectors
Quando um nó **recebe informação de outro nó**:
```java
public void mergeVersionVector(ConcurrentHashMap<String, Integer> otherVector) {
    for (Map.Entry<String, Integer> entry : otherVector.entrySet()) {
        versionVector.merge(entry.getKey(), entry.getValue(), Integer::max);
    }
}
```

**Resultado**: Para cada nó, mantém o **maior contador** conhecido, incorporando conhecimento causal do remetente.

### 3. Detecção de Ordem Causal
Determina se uma mensagem **aconteceu antes** de outra:
```java
public boolean happensBefore(IoTMessage other) {
    boolean hasSmaller = false;
    
    // Verificar todos os componentes do version vector
    for (Map.Entry<String, Integer> entry : this.versionVector.entrySet()) {
        String nodeId = entry.getKey();
        int thisValue = entry.getValue();
        int otherValue = other.versionVector.getOrDefault(nodeId, 0);
        
        if (thisValue > otherValue) {
            return false;  // Esta mensagem não pode ter acontecido antes
        }
        if (thisValue < otherValue) {
            hasSmaller = true;  // Pelo menos um componente é menor
        }
    }
    
    return hasSmaller;  // Happened-before se pelo menos um componente é menor
}
```

**Lógica**: Mensagem A aconteceu antes de B se **A[i] ≤ B[i]** para todo nó i, e **A[j] < B[j]** para pelo menos um nó j.

### 4. Detecção de Concorrência
Identifica mensagens **concorrentes** (sem relação causal):
```java
public boolean isConcurrentWith(IoTMessage other) {
    return !this.happensBefore(other) && !other.happensBefore(this);
}
```

**Resultado**: Duas mensagens são concorrentes se **nenhuma aconteceu antes da outra**.

## Fluxo Completo de Processamento

### Cenário: Sensor Enviando Dados

```
1. Sensor cria mensagem ──► VV = {SENSOR_01=1}
                           │
                           ▼
2. Gateway recebe ────────► VV = {SENSOR_01=1, GATEWAY=1} 
                           │  (incrementa próprio contador)
                           ▼
3. Distribui para Receivers:
   ├─► DataReceiver_1 ───► VV = {SENSOR_01=1, GATEWAY=1, DR1=1}
   └─► DataReceiver_2 ───► VV = {SENSOR_01=1, GATEWAY=1, DR2=1}
                           │
                           ▼
4. Sincronização periódica:
   DR1 ↔ DR2 merge ─────► VV = {SENSOR_01=1, GATEWAY=1, DR1=1, DR2=1}
```

### Detecção de Conflitos Durante Sincronização

```java
// DataReplicationManager verifica conflitos
boolean vv1DominatesVv2 = true;
boolean vv2DominatesVv1 = true;

for (String nodeId : allNodes) {
    long val1 = vv1.getOrDefault(nodeId, 0L);
    long val2 = vv2.getOrDefault(nodeId, 0L);
    
    if (val1 < val2) vv1DominatesVv2 = false;
    if (val2 < val1) vv2DominatesVv1 = false;
}

boolean hasConflict = !vv1DominatesVv2 && !vv2DominatesVv1;
```

**Interpretação**:
- **VV1 domina VV2**: VV1 conhece todos os eventos de VV2 e mais alguns
- **Conflito detectado**: Nenhum domina o outro, indicando eventos concorrentes

## Integração com Data Receivers

### Atualização Automática do Version Vector

Cada **DataReceiver** mantém seu próprio Version Vector e o atualiza **automaticamente**:

```java
private void updateVersionVector(IoTMessage message) {
    if (message.getVersionVector() != null) {
        // Merge com version vector da mensagem recebida
        message.getVersionVector().forEach((senderId, version) -> {
            versionVector.merge(senderId, version.longValue(), Long::max);
        });
        
        // Incrementa contador próprio (evento local de processamento)
        versionVector.merge(receiverId, 1L, Long::sum);
        
        logger.debug("🔍 VERSION VECTOR ATUALIZADO: {} → {}", 
                    message.getSensorId(), versionVector);
    }
}
```

### Resolução de Conflitos com Version Vector

Quando **múltiplos valores** existem para o mesmo sensor:
```java
private void processSensorData(IoTMessage message) {
    String sensorId = message.getSensorId();
    SensorData existingEntry = sensorDatabase.get(sensorId);
    SensorData newEntry = new SensorData(message);
    
    if (existingEntry == null) {
        // Primeira vez vendo este sensor
        sensorDatabase.put(sensorId, newEntry);
        return;
    }
    
    // Resolver conflito usando Version Vector + Timestamp
    if (newEntry.getTimestamp().isAfter(existingEntry.getTimestamp())) {
        // Last Write Wins por timestamp
        sensorDatabase.put(sensorId, newEntry);
        logger.info("✅ Atualizado por timestamp: {} = {}", sensorId, newEntry);
        
    } else if (newEntry.getTimestamp().equals(existingEntry.getTimestamp())) {
        // Desempate por Version Vector Clock
        if (newEntry.getVersionVectorClock() > existingEntry.getVersionVectorClock()) {
            sensorDatabase.put(sensorId, newEntry);
            logger.info("✅ Atualizado por Version Vector: {} = {}", sensorId, newEntry);
        }
    }
}
```

## Benefícios do Version Vector

### 1. Detecção Precisa de Causalidade
- **Identifica ordem real** de eventos independente de clock físico
- **Detecta concorrência** verdadeira entre eventos
- **Garante consistência causal** mesmo com latência de rede variável

### 2. Resolução Inteligente de Conflitos
- **Combina com Last Write Wins** para desempate final
- **Preserva informação causal** durante sincronização
- **Evita perda de dados** por ordenação incorreta

### 3. Tolerância a Falhas
- **Funciona com clocks desincronizados** entre nós
- **Resistente a partições de rede** temporárias
- **Recupera estado consistente** após reconexão

### 4. Escalabilidade
- **Overhead linear** com número de nós ativos
- **Poda automática** de nós inativos
- **Merge eficiente** durante sincronização

## Observabilidade e Debugging

### Logs Estruturados do Version Vector

O sistema produz **logs detalhados** para rastreamento:

```
🔍 VERSION VECTOR ATUALIZADO: TEMP_SENSOR_01 → {TEMP_SENSOR_01=5, DATA_RECEIVER_1=12}
🔍 SYNC DR1 ↔ DR2: VV1={TEMP_SENSOR_01=5, DR1=12}, VV2={TEMP_SENSOR_01=3, DR2=8}
⚠️ VERSION VECTOR CONFLICT detectado - VV1={A=1, B=2}, VV2={A=2, B=1}
📊 VERSION VECTORS APÓS SYNC: VV1={A=2, B=2, DR1=13}, VV2={A=2, B=2, DR2=9}
```

### Métricas de Consistência

```java
// Métricas coletadas automaticamente
private final AtomicLong conflictsDetected = new AtomicLong(0);
private final AtomicLong syncOperations = new AtomicLong(0);

// Taxa de conflitos
public double getConflictRate() {
    long syncs = syncOperations.get();
    long conflicts = conflictsDetected.get();
    return syncs > 0 ? (double) conflicts / syncs * 100.0 : 0.0;
}
```

O Version Vector implementado demonstra como **teoria de sistemas distribuídos** pode ser aplicada **concretamente** para resolver problemas reais de consistência em ambientes IoT de alta concorrência.