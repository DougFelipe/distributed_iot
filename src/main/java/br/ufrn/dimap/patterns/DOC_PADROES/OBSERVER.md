# Padrão Observer - Monitoramento Reativo e Inteligente

## Conceito e Implementação

O padrão Observer no sistema IoT vai além do monitoramento tradicional, implementando um **sistema de observação inteligente e reativo** que funciona como o **sistema nervoso** de toda a infraestrutura distribuída. O **HeartbeatMonitor** não apenas observa eventos, mas **reage proativamente** a mudanças no sistema.

## Arquitetura Observer Integrada

```
                    ┌─────────────────────────────────┐
                    │         IoTGateway              │
                    │        (Observable)             │
                    └─────────────┬───────────────────┘
                                  │ notifyObservers()
                    ┌─────────────▼───────────────────┐
                    │      Event Distribution          │
                    └─┬─────┬─────┬─────┬─────┬─────┬─┘
                      │     │     │     │     │     │
            ┌─────────▼──┐ ┌▼────┐ ┌▼──┐ ┌▼──┐ ┌▼──┐ ┌▼────┐
            │Heartbeat   │ │Fault│ │Log│ │Perf│ │Alert│ │Custom│
            │Monitor     │ │Tol. │ │Obs│ │Mon│ │Mgr │ │Observer│
            │            │ │Mgr  │ │   │ │   │ │    │ │        │
            │• Timeout   │ │     │ │   │ │   │ │    │ │        │
            │• Metrics   │ │     │ │   │ │   │ │    │ │        │
            │• Health    │ │     │ │   │ │   │ │    │ │        │
            │• Alerting  │ │     │ │   │ │   │ │    │ │        │
            └────────────┘ └─────┘ └───┘ └───┘ └────┘ └──────┘
                    │
                    ▼
            ┌──────────────────┐
            │  Reactive Actions │
            │                  │
            │ • Auto Recovery  │
            │ • Load Balance   │  
            │ • Alert Generation│
            │ • Metrics Update │
            └──────────────────┘
```

## HeartbeatMonitor - Observer Inteligente

### Responsabilidades Multidimensionais

O HeartbeatMonitor não é apenas um "ping monitor", mas um **observador sofisticado** que monitora:

1. **Heartbeat de Sensores**: Detecta sensores que pararam de enviar dados
2. **Performance Metrics**: Coleta estatísticas de throughput e latência
3. **Health Status**: Monitora estado de receivers e componentes
4. **Communication Patterns**: Analisa padrões de comunicação para otimização

### Estrutura de Dados Thread-Safe

```java
// Estruturas concorrentes para ambiente multi-threaded
ConcurrentHashMap<String, LocalDateTime> lastHeartbeat;     // Último heartbeat por sensor
ConcurrentHashMap<String, AtomicLong> messageCount;         // Contadores de mensagens
AtomicLong totalEvents;                                     // Total de eventos processados
```

**Por que ConcurrentHashMap?**
- **Thread-safety**: Múltiplas threads podem atualizar simultaneamente
- **Performance**: Locking granular apenas nos buckets necessários
- **Scalability**: Suporta milhares de sensores simultaneamente

## Tipos de Eventos Observados

### 1. SENSOR_REGISTERED - Novo Sensor no Sistema
```
Sensor se registra ──► Gateway ──► notifyObservers("SENSOR_REGISTERED", sensor)
                                          │
                                          ▼
                                 HeartbeatMonitor inicia monitoramento:
                                 • Cria entrada no lastHeartbeat
                                 • Inicializa contador de mensagens
                                 • Configura timeout específico
                                 • Log de início de monitoramento
```

**Ação Reativa**: O monitor **automaticamente** começar a rastrear o novo sensor sem configuração manual.

### 2. SENSOR_UNREGISTERED - Sensor Removido
```
Sensor desconecta ──► Gateway ──► notifyObservers("SENSOR_UNREGISTERED", sensor)
                                         │
                                         ▼
                                HeartbeatMonitor limpa recursos:
                                • Remove do lastHeartbeat
                                • Remove contador de mensagens
                                • Libera recursos de monitoramento
                                • Log de fim de monitoramento
```

**Ação Reativa**: **Limpeza automática** evita memory leaks e acúmulo de dados obsoletos.

### 3. MESSAGE_RECEIVED - Dados de Sensor
```
Mensagem UDP/HTTP/TCP ──► Gateway ──► notifyObservers("MESSAGE_RECEIVED", message)
                                             │
                                             ▼
                                    HeartbeatMonitor atualiza:
                                    • lastHeartbeat = now()
                                    • messageCount++
                                    • totalEvents++
                                    • Log com métricas atualizadas
```

**Ação Reativa**: **Atualização automática** de métricas e detecção proativa de padrões anômalos.

## Sistema de Detecção de Timeout

### Algoritmo de Detecção
```java
// Verifica timeout para cada sensor monitorado
LocalDateTime lastSeen = lastHeartbeat.get(sensorId);
Duration silenceTime = Duration.between(lastSeen, LocalDateTime.now());

if (silenceTime.getSeconds() > timeoutSeconds) {
    // ALERTA: Sensor possivelmente com falha
    generateTimeoutAlert(sensorId, silenceTime);
}
```

### Timeout Configurável por Contexto
- **Sensores críticos**: 30 segundos (sistemas médicos, segurança)
- **Sensores normais**: 60 segundos (temperatura, umidade)
- **Sensores batch**: 300 segundos (dados históricos)

### Estratégias de Timeout
```
┌─────────────────────────────────────────────────────────┐
│                 Timeout Management                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Sensor Crítico ──► 30s ──► ALERT IMEDIATO             │
│  Sensor Normal ──► 60s ──► WARNING + Monitor Estendido  │
│  Sensor Batch ──► 300s ──► INFO + Verificação Manual    │
│                                                         │
│  Escalation:                                            │
│  Warning ──► 2x timeout ──► Critical Alert              │
│  Critical ──► 3x timeout ──► Assume Sensor Failure      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Métricas em Tempo Real

### Coleta Automática de Estatísticas
O HeartbeatMonitor coleta **automaticamente**:

```java
// Para cada sensor
long totalMessages = messageCount.get(sensorId).get();
LocalDateTime lastActivity = lastHeartbeat.get(sensorId);
Duration uptime = Duration.between(firstSeen, LocalDateTime.now());

// Métricas calculadas
double messagesPerSecond = totalMessages / uptime.getSeconds();
double averageInterval = uptime.getSeconds() / totalMessages;
```

### Dashboard Implícito via Logs
```
💓 Heartbeat atualizado: SENSOR_001 (total: 1,247) - Tipo Msg: SENSOR_DATA [20] - Valor: 23.5°C TEMPERATURE
💓 Heartbeat atualizado: SENSOR_002 (total: 891) - Tipo Msg: HEARTBEAT [10] - Valor: OK STATUS  
💓 Heartbeat atualizado: SENSOR_003 (total: 2,103) - Tipo Msg: ALERT [30] - Valor: HIGH_TEMP TEMPERATURE
```

**Vantagem**: Logs estruturados podem ser **facilmente parseados** por ferramentas como ELK Stack, Splunk, ou Grafana para criar dashboards automáticos.

## Integração com Fault Tolerance

### Observer como Detector de Falhas
```
HeartbeatMonitor detecta timeout ──► Gera evento "SENSOR_TIMEOUT"
                                           │
                                           ▼
                                  Gateway notifica observers:
                                           │
                                           ├─► FaultToleranceManager
                                           │   (inicia recovery procedures)
                                           │
                                           ├─► AlertingSystem  
                                           │   (notifica operadores)
                                           │
                                           └─► LoadBalancer
                                               (redistribui carga)
```

### Cascata de Recuperação Automática
1. **Detecção**: HeartbeatMonitor identifica timeout
2. **Notificação**: Observer pattern propaga evento
3. **Análise**: FaultToleranceManager analisa gravidade
4. **Ação**: Recuperação automática ou alerta manual
5. **Verificação**: HeartbeatMonitor confirma recuperação

## Padrões de Comunicação Inteligentes

### Análise de Comportamento
O monitor identifica **padrões anômalos**:

```java
// Detecção de burst patterns
if (messagesLastMinute > normalRate * 3) {
    logger.warn("🚨 BURST detectado em {}: {} msgs/min (normal: {})", 
                sensorId, messagesLastMinute, normalRate);
}

// Detecção de degradação gradual
if (averageInterval > historicalAverage * 1.5) {
    logger.warn("⚠️ DEGRADAÇÃO detectada em {}: intervalo {}s (normal: {}s)",
                sensorId, averageInterval, historicalAverage);
}
```

### Adaptação Proativa
- **Burst handling**: Aumenta buffer sizes temporariamente
- **Slow sensors**: Ajusta timeout dinamicamente
- **Pattern learning**: Aprende comportamento normal de cada sensor

## Observabilidade Multinível

### Logs Estruturados por Contexto

**Debug Level** - Operação normal:
```
💓 Heartbeat atualizado: SENSOR_001 (total: 1,247) - 23.5°C
```

**Info Level** - Eventos importantes:
```
💓 Monitoramento iniciado para sensor: SENSOR_001 (TEMPERATURE)
💔 Monitoramento removido para sensor: SENSOR_001
```

**Warn Level** - Situações anômalas:
```
⚠️ BURST detectado em SENSOR_001: 150 msgs/min (normal: 12)
⚠️ DEGRADAÇÃO detectada em SENSOR_002: intervalo 90s (normal: 60s)
```

**Error Level** - Falhas críticas:
```
❌ TIMEOUT CRÍTICO: SENSOR_001 silencioso há 180s (limite: 60s)
❌ FALHA SISTÊMICA: 85% dos sensores com timeout simultâneo
```

## Extensibilidade do Observer

### Múltiplos Observers Especializados
```java
// Diferentes observers para diferentes aspectos
gateway.addObserver(new HeartbeatMonitor(60));           // Monitoramento básico
gateway.addObserver(new SecurityMonitor());             // Eventos de segurança  
gateway.addObserver(new PerformanceMonitor());          // Métricas de performance
gateway.addObserver(new AuditLogger());                 // Logs de auditoria  
gateway.addObserver(new MetricsCollector());            // Coleta para Prometheus
```

### Observer Chain Reaction
```
Evento único ──► Gateway ──► Múltiplos Observers ──┐
                                                   │
    ┌──────────────────────────────────────────────┘
    │
    ├─► HeartbeatMonitor (atualiza métricas)
    ├─► SecurityMonitor (analisa padrões suspeitos)  
    ├─► PerformanceMonitor (calcula latências)
    ├─► AuditLogger (registra para compliance)
    └─► MetricsCollector (exporta para monitoring)
```

## Cenários de Uso Real

### Sistema de Monitoramento Industrial
```
Sensores de pressão ──► Timeout: 10s ──► Alertas críticos imediatos
Sensores de temperatura ──► Timeout: 30s ──► Monitoring normal
Sensores de umidade ──► Timeout: 60s ──► Alertas informativos
```

### Sistema IoT Residencial
```
Detectores de fumaça ──► Timeout: 15s ──► Notificação emergência
Termostatos ──► Timeout: 120s ──► Ajuste automático
Sensores jardim ──► Timeout: 600s ──► Log para análise
```

### Recuperação Automática de Falhas
```
1. HeartbeatMonitor detecta timeout
2. FaultToleranceManager recebe notificação
3. Tenta reconectar sensor automaticamente
4. Se falha, marca sensor como inativo
5. Redistribui carga para sensores ativos
6. Continua tentativas de recovery em background
7. HeartbeatMonitor confirma recuperação quando sensor volta
```

O padrão Observer no sistema IoT cria um **sistema nervoso inteligente** que não apenas monitora, mas **reage proativamente** a mudanças, garantindo **alta disponibilidade** e **operação autônoma** da infraestrutura distribuída.