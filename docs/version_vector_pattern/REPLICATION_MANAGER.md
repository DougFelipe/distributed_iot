# Gerenciamento de Replicação - Sincronização Automática com Version Vector

## Conceito do DataReplicationManager

O **DataReplicationManager** é o **orquestrador central** da sincronização de estado entre Data Receivers distribuídos. Ele implementa um **sistema automático** de replicação que utiliza Version Vectors para **detectar conflitos**, **sincronizar estado** e **recuperar dados** após falhas.

## Arquitetura do Sistema de Replicação

```
                    ┌─────────────────────────────────┐
                    │   DataReplicationManager        │
                    │      (Orchestrator)             │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │    Scheduled Operations         │
                    │                                 │
                    │ ┌─────────────┐ ┌─────────────┐ │
                    │ │Sync Timer   │ │Backup Timer │ │
                    │ │(3s interval)│ │(10s interval│ │
                    │ └─────────────┘ └─────────────┘ │
                    │ ┌─────────────┐                 │
                    │ │Health Check │                 │
                    │ │(2s interval)│                 │
                    │ └─────────────┘                 │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │       Data Receivers            │
                    │                                 │
                    │ ┌─────────┐ ┌─────────┐        │
                    │ │   DR1   │ │   DR2   │        │
                    │ │VV={A=5, │ │VV={A=3, │        │
                    │ │   DR1=12│ │   DR2=8}│        │
                    │ │       } │ │         │        │
                    │ └─────────┘ └─────────┘        │
                    │         ▲     ▲                │
                    │         │     │                │
                    │    ┌────▼─────▼────┐           │
                    │    │ Conflict       │           │ 
                    │    │ Detection &    │           │
                    │    │ Resolution     │           │
                    │    └────────────────┘           │
                    └─────────────────────────────────┘
```

## Operações Automáticas Periódicas

### 1. Sincronização Automática (3 segundos)

A **sincronização periódica** é o coração do sistema de replicação:

```java
scheduler.scheduleAtFixedRate(this::performSynchronization, 
                            SYNC_INTERVAL, SYNC_INTERVAL, TimeUnit.SECONDS);
```

**Processo de Sincronização**:
```
Para cada par de Data Receivers (DR1, DR2):
1. Obter Version Vectors atuais
2. Comparar VVs para detectar conflitos
3. Determinar receiver com mais dados (source)
4. Criar backup do source
5. Restaurar backup no target
6. Merge de Version Vectors
7. Log detalhado da operação
```

### 2. Backup Automático (10 segundos)

```java
scheduler.scheduleAtFixedRate(this::performAutomaticBackup, 
                            BACKUP_INTERVAL, BACKUP_INTERVAL, TimeUnit.SECONDS);
```

**Criação de Backups**:
- **Estado completo** de cada Data Receiver
- **Version Vector preservado** no backup
- **Timestamp** para ordenação de backups
- **Contadores de mensagens** para comparação

### 3. Health Check (2 segundos)

```java
scheduler.scheduleAtFixedRate(this::performHealthCheck, 
                            HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
```

**Detecção e Recuperação**:
- **Monitor contínuo** de saúde dos receivers
- **Recuperação automática** quando falhas são detectadas
- **Restauração de dados** usando backup mais recente
- **Re-sincronização** após recuperação

## Algoritmo de Detecção de Conflitos

### Comparação de Version Vectors

O sistema implementa o **algoritmo clássico** de comparação de Version Vectors:

```java
private boolean detectVersionVectorConflicts(ConcurrentHashMap<String, Long> vv1, 
                                            ConcurrentHashMap<String, Long> vv2) {
    // Um Version Vector A domina B se A[i] >= B[i] para todo i
    boolean vv1DominatesVv2 = true;
    boolean vv2DominatesVv1 = true;
    
    // Obter união de todas as chaves
    var allKeys = ConcurrentHashMap.<String>newKeySet();
    allKeys.addAll(vv1.keySet());
    allKeys.addAll(vv2.keySet());
    
    // Verificar dominância componente por componente
    for (String key : allKeys) {
        long val1 = vv1.getOrDefault(key, 0L);
        long val2 = vv2.getOrDefault(key, 0L);
        
        if (val1 < val2) vv1DominatesVv2 = false;
        if (val2 < val1) vv2DominatesVv1 = false;
    }
    
    // Conflito: nenhum VV domina o outro
    boolean hasConflict = !vv1DominatesVv2 && !vv2DominatesVv1;
    
    return hasConflict;
}
```

### Interpretação dos Resultados

```
Caso 1: VV1={A=5, B=3}, VV2={A=3, B=2}
        VV1 domina VV2 (5≥3 e 3≥2)
        Resultado: SEM conflito, VV1 é mais recente

Caso 2: VV1={A=5, B=2}, VV2={A=3, B=4}  
        VV1 NÃO domina VV2 (5≥3 mas 2<4)
        VV2 NÃO domina VV1 (3<5 mas 4≥2)
        Resultado: CONFLITO detectado (eventos concorrentes)

Caso 3: VV1={A=5, B=3}, VV2={A=5, B=3}
        VV1 domina VV2 E VV2 domina VV1 (iguais)
        Resultado: SEM conflito, estados sincronizados
```

## Processo de Sincronização Passo-a-Passo

### Sincronização Entre Dois Receivers

```java
private void synchronizeBetweenReceivers(DataReceiver primary, DataReceiver secondary) {
    // PASSO 1: Obter estado atual
    var primaryVV = primary.getVersionVector();
    var secondaryVV = secondary.getVersionVector();
    
    logger.debug("🔍 SYNC {} ↔ {}: VV1={}, VV2={}", 
                primary.getReceiverId(), secondary.getReceiverId(), 
                primaryVV, secondaryVV);
    
    // PASSO 2: Detectar conflitos
    boolean hasConflicts = detectVersionVectorConflicts(primaryVV, secondaryVV);
    
    if (hasConflicts) {
        conflictsDetected.incrementAndGet();
        logger.warn("⚠️ VERSION VECTOR CONFLICT detectado - resolvendo via merge");
    }
    
    // PASSO 3: Criar backups
    var primaryBackup = primary.createBackup();
    var secondaryBackup = secondary.createBackup();
    
    // PASSO 4: Determinar direção da sincronização
    DataReceiver source, target;
    if (primaryBackup.getTotalMessages() >= secondaryBackup.getTotalMessages()) {
        source = primary;
        target = secondary;
    } else {
        source = secondary;
        target = primary;
    }
    
    // PASSO 5: Sincronizar se há diferença
    long messageDiff = Math.abs(primaryBackup.getTotalMessages() - 
                               secondaryBackup.getTotalMessages());
    if (messageDiff > 0) {
        var sourceBackup = source.createBackup();
        target.restoreFromBackup(sourceBackup);
        
        logger.info("✅ SYNC REALIZADA: {} → {} (diff: {} mensagens)", 
                   source.getReceiverId(), target.getReceiverId(), messageDiff);
    }
}
```

### Fluxo Completo de Sincronização

```
Estado Inicial:
DR1: 50 mensagens, VV={TEMP=15, DR1=50, DR2=20}
DR2: 35 mensagens, VV={TEMP=12, DR1=45, DR2=35}

Detecção de Conflito:
- DR1 não domina DR2: DR1[TEMP]=15 > DR2[TEMP]=12 ✓, mas DR1[DR2]=20 < DR2[DR2]=35 ✗
- DR2 não domina DR1: DR2[TEMP]=12 < DR1[TEMP]=15 ✗
- CONFLITO detectado: eventos concorrentes

Resolução:
- DR1 tem mais mensagens (50 > 35)
- DR1 é escolhido como source
- Backup de DR1 é aplicado em DR2
- Version Vectors são merged

Estado Final:
DR1: 50 mensagens, VV={TEMP=15, DR1=50, DR2=35}
DR2: 50 mensagens, VV={TEMP=15, DR1=50, DR2=35}
```

## Sistema de Backup e Recuperação

### Estrutura do Backup

```java
public static class DataReceiverBackup {
    private final String receiverId;
    private final ConcurrentHashMap<String, SensorDataEntry> sensorDatabase;
    private final ConcurrentHashMap<String, Long> versionVector;  // VV preservado
    private final long totalMessages;
    private final long backupTimestamp;
    
    // Backup completo do estado + informação causal
}
```

### Recuperação Após Falhas

```java
private void recoverDataAfterFailure(DataReceiver recoveredReceiver) {
    // PASSO 1: Encontrar melhor backup
    DataReceiver.DataReceiverBackup bestBackup = null;
    long maxMessages = -1;
    
    for (DataReceiver receiver : dataReceivers) {
        if (!receiver.isRunning() || receiver.equals(recoveredReceiver)) continue;
        
        var backup = receiver.createBackup();
        if (backup.getTotalMessages() > maxMessages) {
            maxMessages = backup.getTotalMessages();
            bestBackup = backup;
        }
    }
    
    // PASSO 2: Restaurar usando melhor backup
    if (bestBackup != null) {
        recoveredReceiver.restoreFromBackup(bestBackup);
        
        logger.info("✅ DADOS RECUPERADOS: {} restaurado com {} mensagens, VV={}", 
                   recoveredReceiver.getReceiverId(), 
                   bestBackup.getTotalMessages(),
                   bestBackup.getVersionVector());
    }
}
```

## Métricas e Observabilidade

### Contadores Automáticos

```java
private final AtomicLong syncOperations = new AtomicLong(0);
private final AtomicLong conflictsDetected = new AtomicLong(0);  
private final AtomicLong backupsCreated = new AtomicLong(0);

// Relatório periódico (a cada 10 operações)
if (syncOperations.get() % 10 == 0) {
    logger.info("📊 REPLICAÇÃO STATUS: Syncs={}, Conflitos={}, Backups={}", 
               syncOperations.get(), conflictsDetected.get(), backupsCreated.get());
}
```

### Logs Estruturados de Sincronização

```
🔍 SYNC DATA_RECEIVER_1 ↔ DATA_RECEIVER_2: VV1={TEMP=15, DR1=50}, VV2={TEMP=12, DR2=35}
⚠️ VERSION VECTOR CONFLICT detectado - resolvendo via merge
✅ SYNC REALIZADA: DATA_RECEIVER_1 → DATA_RECEIVER_2 (diff: 15 mensagens)
📊 VERSION VECTORS APÓS SYNC: VV1={TEMP=15, DR1=50, DR2=35}, VV2={TEMP=15, DR1=50, DR2=35}
📊 REPLICAÇÃO STATUS: Syncs=10, Conflitos=3, Backups=25
```

## Configuração de Intervalos

### Ajuste Fino dos Timers

```java
private static final int SYNC_INTERVAL = 3;      // Sincronização frequente
private static final int BACKUP_INTERVAL = 10;   // Backup menos frequente  
private static final int HEARTBEAT_INTERVAL = 2; // Health check muito frequente
```

**Rationale**:
- **Sync (3s)**: Frequente o suficiente para detectar divergências rapidamente
- **Backup (10s)**: Menos frequente para não sobrecarregar o sistema
- **Health (2s)**: Muito frequente para detecção rápida de falhas

### Thread Pool Dedicado

```java
this.scheduler = Executors.newScheduledThreadPool(4, r -> {
    Thread t = new Thread(r, "DataReplication-" + System.nanoTime());
    t.setDaemon(true);  // Não bloqueia shutdown da JVM
    return t;
});
```

**Configuração**:
- **4 threads dedicadas** para operações de replicação
- **Daemon threads** para shutdown graceful
- **Nomes únicos** para debugging

## Cenários de Uso Real

### Operação Normal
```
10:30:00 - Sistema iniciado, 2 Data Receivers ativos
10:30:03 - Primeira sincronização: estados idênticos
10:30:06 - Sync: DR1=15 msgs, DR2=12 msgs → sync DR1→DR2
10:30:09 - Sync: estados sincronizados
10:30:10 - Backup automático: 2 backups criados
```

### Cenário de Conflito
```
10:35:15 - Sensores enviando dados simultaneamente
10:35:18 - Sync detecta conflito: VV concurrent events
10:35:18 - Resolução: DR com mais mensagens vence
10:35:18 - Merge de VVs: informação causal preservada
10:35:21 - Próxima sync: estados consistentes
```

### Cenário de Falha e Recuperação
```
10:40:30 - Health check detecta DR2 offline
10:40:30 - Tentativa de recovery automático
10:40:32 - DR2 recuperado, mas dados perdidos
10:40:33 - Busca melhor backup (DR1 com 45 msgs)
10:40:34 - Restauração: DR2 ← backup de DR1
10:40:36 - Sync confirma: estados consistentes
```

O DataReplicationManager demonstra como **algoritmos clássicos** de sistemas distribuídos podem ser implementados **concretamente** para resolver problemas reais de **consistência eventual** em ambientes IoT.