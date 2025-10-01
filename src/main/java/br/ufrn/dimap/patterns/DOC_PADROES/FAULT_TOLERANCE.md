# Tolerância a Falhas - Recuperação Automática e Resiliência

## Conceito de Fault Tolerance

O **FaultToleranceManager** representa a evolução natural dos padrões GoF aplicados a sistemas distribuídos modernos. Embora não seja um padrão GoF clássico, ele **integra e orquestra** os padrões existentes para criar um **sistema auto-resiliente** que detecta, isola e recupera automaticamente de falhas.

## Arquitetura de Recuperação Automática

```
                    ┌─────────────────────────────────┐
                    │    FaultToleranceManager        │
                    │      (Orchestrator)             │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │       Health Monitoring          │
                    │                                 │
                    │ ┌─────────┐ ┌─────────┐        │
                    │ │ Health  │ │ Load    │        │
                    │ │ Check   │ │ Monitor │        │
                    │ │ (5s)    │ │ (10s)   │        │
                    │ └─────────┘ └─────────┘        │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │      Failure Detection           │
                    │                                 │
                    │ Socket ──► Network ──► Timeout  │
                    │ Exception   Error      Alert     │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │     Automatic Recovery           │
                    │                                 │
                    │ 1. Stop Failed Instance         │
                    │ 2. Start Backup Instance        │
                    │ 3. Update Load Balancing        │
                    │ 4. Notify Observers             │
                    │ 5. Verify Recovery Success      │
                    └─────────────────────────────────┘
```

## Integração com Padrões GoF

### Colaboração com Singleton (IoTGateway)
```java
// FaultToleranceManager trabalha COM o Gateway, não contra ele
IoTGateway gateway = IoTGateway.getInstance();
FaultToleranceManager ftm = new FaultToleranceManager(gateway);

// Gateway delega responsabilidade de fault tolerance
gateway.setFaultToleranceManager(ftm);
ftm.start(); // Inicia monitoramento automático
```

**Benefício**: O Singleton mantém **coordenação central** enquanto o FaultToleranceManager fornece **capacidades especializadas** de recuperação.

### Colaboração com Observer (HeartbeatMonitor)
```java
// Observer detecta → FaultTolerance age
HeartbeatMonitor monitor = new HeartbeatMonitor(60);
monitor.onTimeout(sensorId -> {
    // Observer notifica FaultToleranceManager
    ftm.handleSensorTimeout(sensorId);
});
```

**Sinergia**: Observer **detecta problemas**, FaultTolerance **resolve problemas**.

### Colaboração com Strategy (Load Balancing)
```java
// Quando receiver falha, Strategy se adapta automaticamente
ftm.onReceiverFailure(failedReceiver -> {
    // Remove receiver falho da strategy
    gateway.getReceiverStrategy().removeReceiver(failedReceiver);
    
    // Inicia novo receiver backup
    DataReceiver backup = ftm.startBackupReceiver();
    
    // Adiciona backup à strategy
    gateway.getReceiverStrategy().addReceiver(backup);
});
```

**Adaptação**: Strategy **se ajusta dinamicamente** às mudanças na topologia do sistema.

## Tipos de Falhas Detectadas

### 1. Falhas de Network/Socket
```java
// Detecção via exception handling
try {
    receiver.processMessage(message);
} catch (SocketException e) {
    logger.error("❌ Socket failure detectada: {}", receiver.getId());
    ftm.handleReceiverFailure(receiver, e);
}
```

**Ação Automática**: 
- Marca receiver como inativo
- Inicia backup receiver na mesma porta
- Redireciona tráfego automaticamente

### 2. Falhas de Timeout/Responsividade
```java
// Health check periódico
if (!receiver.respondsToPing(HEALTH_CHECK_TIMEOUT)) {
    logger.warn("⚠️ Receiver {} não responsivo", receiver.getId());
    ftm.scheduleRecovery(receiver);
}
```

**Ação Automática**:
- Tenta recovery graceful primeiro
- Se falhar, força restart do receiver
- Monitora recovery success

### 3. Falhas de Sobrecarga
```java
// Monitoramento de carga
if (receiver.getCurrentLoad() > MAX_LOAD_THRESHOLD) {
    logger.warn("🚨 Overload detectado: {}", receiver.getId());
    ftm.initiateLoadBalance();
}
```

**Ação Automática**:
- Inicia receivers adicionais
- Redistributa carga existente
- Ajusta strategy de balanceamento

## Configuração de Backup Receivers

### Pool de Receivers de Backup
```java
// Pool pré-configurado para recovery rápido
backupConfigs.offer(new DataReceiverConfig("BACKUP_1", 9093));
backupConfigs.offer(new DataReceiverConfig("BACKUP_2", 9094));  
backupConfigs.offer(new DataReceiverConfig("BACKUP_3", 9095));
```

**Estratégia**: Receivers backup são **pré-configurados** mas **não iniciados** até serem necessários, economizando recursos.

### Configuração Inteligente de Portas
```java
public class DataReceiverConfig {
    public final String receiverId;     // Identificador único
    public final int port;              // Porta específica
    public final int maxRecoveryAttempts; // Limite de tentativas
    public int currentAttempts = 0;     // Estado atual
}
```

**Vantagem**: Cada backup tem **configuração específica** adaptada ao seu contexto de uso.

## Algoritmos de Recovery

### Recovery Graceful (Primeira Tentativa)
```java
private boolean attemptGracefulRecovery(DataReceiver failed) {
    try {
        // 1. Para receiver gentilmente
        failed.gracefulShutdown();
        
        // 2. Aguarda cleanup completo
        Thread.sleep(RECOVERY_DELAY * 1000);
        
        // 3. Reinicia na mesma configuração
        failed.restart();
        
        // 4. Verifica se recovery funcionou
        return failed.isHealthy();
        
    } catch (Exception e) {
        logger.warn("⚠️ Graceful recovery falhou: {}", e.getMessage());
        return false;
    }
}
```

### Recovery Forçado (Segunda Tentativa)
```java
private boolean attemptForcedRecovery(DataReceiver failed) {
    try {
        // 1. Para receiver forçadamente
        failed.forceStop();
        
        // 2. Limpa recursos manualmente
        cleanupResources(failed);
        
        // 3. Cria nova instância
        DataReceiver newInstance = createNewReceiver(failed.getConfig());
        
        // 4. Substitui na arquitetura
        gateway.replaceReceiver(failed, newInstance);
        
        return newInstance.isHealthy();
        
    } catch (Exception e) {
        logger.error("❌ Forced recovery falhou: {}", e.getMessage());
        return false;
    }
}
```

### Backup Activation (Terceira Tentativa)
```java
private boolean activateBackupReceiver() {
    DataReceiverConfig backupConfig = availableBackups.poll();
    
    if (backupConfig == null) {
        logger.error("❌ CRÍTICO: Nenhum backup disponível!");
        return false;
    }
    
    try {
        // 1. Cria receiver backup  
        DataReceiver backup = new DataReceiver(backupConfig);
        
        // 2. Inicia em nova porta
        backup.start();
        
        // 3. Adiciona ao gateway
        gateway.addReceiver(backup);
        
        // 4. Atualiza load balancing
        gateway.getReceiverStrategy().addReceiver(backup);
        
        logger.info("✅ Backup receiver ativado: {}", backup.getId());
        return true;
        
    } catch (Exception e) {
        logger.error("❌ Backup activation falhou: {}", e.getMessage());
        backupConfig.currentAttempts++;
        return false;
    }
}
```

## Monitoramento de Recuperação

### Métricas de Fault Tolerance
```java
// Métricas coletadas automaticamente
private final AtomicLong totalFailures = new AtomicLong(0);
private final AtomicLong successfulRecoveries = new AtomicLong(0);
private final AtomicLong backupActivations = new AtomicLong(0);

// Cálculo de success rate
public double calculateRecoverySuccessRate() {
    long total = totalFailures.get();
    long successful = successfulRecoveries.get();
    
    return total > 0 ? (double) successful / total * 100.0 : 0.0;
}
```

### Logs de Recovery Detalhados
```
🛡️ Fault Tolerance Manager criado com 3 backup configs
⚠️ Receiver DATA_RECEIVER_01 não responsivo há 15s  
🔄 Iniciando graceful recovery para DATA_RECEIVER_01
✅ Graceful recovery SUCCESS: DATA_RECEIVER_01 responsivo
📊 Recovery Stats: 15 falhas, 14 sucessos (93.3% success rate)
```

## Escalation Procedures

### Níveis de Severidade
```java
public enum FailureSeverity {
    LOW,        // 1 receiver falhou, backups disponíveis
    MEDIUM,     // Multiple receivers falharam
    HIGH,       // >50% dos receivers indisponíveis  
    CRITICAL    // Sistema sem receivers funcionais
}
```

### Ações por Nível
```
LOW (1 falha):
├─► Graceful recovery
├─► Log informativo  
└─► Continue operação normal

MEDIUM (2-3 falhas):
├─► Forced recovery + backup activation
├─► Alert para ops team
└─► Increased monitoring

HIGH (>50% falhas):
├─► Emergency backup activation
├─► Critical alert + paging
├─► Auto-scale receivers
└─► Degraded mode operation

CRITICAL (total failure):
├─► Emergency shutdown
├─► All hands alert
├─► Manual intervention required
└─► Disaster recovery procedures
```

## Cenários de Uso Real

### Falha Individual de Receiver
```
11:23:45 - Receiver DATA_RECEIVER_01 falha (SocketException)
11:23:45 - FaultTolerance detecta falha
11:23:46 - Graceful recovery iniciado
11:23:51 - Recovery SUCCESS, receiver operational
11:23:51 - Sistema volta ao normal (zero downtime)
```

### Falha Múltipla (Sobrecarga)
```
14:15:30 - High load detectado (3000 msgs/sec)  
14:15:35 - Receivers 01 e 02 ficam overloaded
14:15:36 - FaultTolerance ativa backup receivers
14:15:37 - Load rebalanceado entre 5 receivers  
14:15:38 - System stable (600 msgs/sec por receiver)
```

### Falha de Rede (Datacenter)
```
09:45:12 - Network outage detectado
09:45:15 - Todos receivers primários inacessíveis
09:45:16 - Emergency backup activation
09:45:18 - Backup datacenter operational  
09:45:20 - Traffic rerouted automaticamente
09:45:25 - Service restored (degraded performance)
```

## Integração com JMeter Testing

### Simulação de Falhas
O FaultToleranceManager permite **injeção controlada de falhas** para testes:

```java
// Para demonstrações JMeter
ftm.simulateFailure("DATA_RECEIVER_01", 30); // Falha por 30s
ftm.simulateOverload(75); // Simula 75% CPU usage
ftm.simulateNetworkPartition(15); // Network issues por 15s
```

### Métricas Observáveis
```
Normal operation: 0% error rate, 200ms avg latency
Failure injection: 15% error rate spike
Recovery phase: Error rate drops to 2%  
Full recovery: Back to 0% error rate
```

O sistema de Fault Tolerance demonstra como **padrões arquiteturais modernos** podem **estender e amplificar** os benefícios dos padrões GoF clássicos, criando sistemas verdadeiramente **resilientes e auto-adaptativos** para ambientes distribuídos complexos.