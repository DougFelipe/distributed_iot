# Padrão Singleton - Coordenador Central IoT

## Conceito e Implementação

O **IoTGateway** representa o coração do sistema distribuído, implementando o padrão Singleton de forma **thread-safe** e **robusta**. Este componente não é apenas um singleton tradicional, mas um **orquestrador inteligente** que coordena todos os aspectos da comunicação IoT.

## Arquitetura do Singleton

```
                    ┌─────────────────────────────────┐
                    │         IoTGateway              │
                    │        (SINGLETON)              │
                    ├─────────────────────────────────┤
                    │                                 │
    ┌───────────────┤ - instance (volatile)           │
    │               │ - lock (Object)                 │
    │               │ - observers (List)              │
    │               │ - strategies (Map)              │
    │               │ - receivers (List)              │
    │               │ - running (AtomicBoolean)       │
    │               │                                 │
    │               └─────────────────────────────────┘
    │                           │
    │                           ▼
    │               ┌─────────────────────────────────┐
    │               │     Double-Checked Locking      │
    │               │                                 │
    │               │ 1. Check instance == null      │
    │               │ 2. Synchronized block           │
    │               │ 3. Second check                 │
    │               │ 4. Create instance              │
    │               │                                 │
    │               └─────────────────────────────────┘
    │
    └─► Garante UMA única instância em ambiente multi-threaded
```

## Por que Singleton no Sistema IoT?

### Problema do Estado Distribuído
Em um sistema IoT com **múltiplos sensores**, **vários protocolos** e **diversos receivers**, ter múltiplas instâncias de coordenadores causaria:

- **Conflitos de porta**: Múltiplos listeners na mesma porta
- **Estado inconsistente**: Diferentes visões do sistema
- **Duplicação de recursos**: Memory leaks e desperdício
- **Competição por recursos**: Race conditions entre instâncias

### Solução Singleton Inteligente
O IoTGateway resolve estes problemas sendo:

1. **Ponto único de coordenação**: Todos os componentes se registram através dele
2. **Gerenciador de ciclo de vida**: Controla startup/shutdown de forma ordenada
3. **Centralizador de estado**: Mantém visão única e consistente do sistema
4. **Coordenador de recursos**: Evita conflitos de portas e threads

## Implementação Thread-Safe

### Double-Checked Locking Pattern
```java
// Primeira verificação (sem sincronização - rápida)
if (instance == null) {
    // Sincronização apenas quando necessária
    synchronized (lock) {
        // Segunda verificação (dentro do bloco sincronizado)
        if (instance == null) {
            instance = new IoTGateway();
        }
    }
}
```

### Por que Double-Checked?
- **Performance**: Evita sincronização desnecessária após criação
- **Thread-safety**: Garante apenas uma instância mesmo com múltiplas threads
- **Lazy initialization**: Instância criada apenas quando necessária
- **Memory efficiency**: Sem overhead de sincronização após inicialização

## Responsabilidades do Gateway

### 1. Gerenciamento de Observers
O Gateway mantém uma **lista thread-safe de observers** que são notificados de todos os eventos importantes:

```
Event ──► Gateway ──► Notify All Observers ──┐
                                             │
    ┌────────────────────────────────────────┘
    │
    ├─► HeartbeatMonitor (atualiza métricas)
    ├─► FaultToleranceManager (detecta falhas)
    ├─► LoggingObserver (registra eventos)
    └─► CustomObserver (funcionalidades específicas)
```

**Vantagem**: Qualquer componente pode **observar eventos do sistema** sem acoplar-se diretamente ao Gateway.

### 2. Coordenação de Strategies
O Gateway **não decide sozinho** qual protocolo ou receiver usar. Em vez disso, **delega estas decisões** para as strategies appropriadas:

```
Mensagem chegando ──► Gateway ──┐
                                │
                                ▼
                    ┌─ Consulta CommunicationStrategy
                    │       │
                    │       ▼
                    │  Seleciona: UDP/HTTP/TCP/gRPC
                    │
                    ├─ Consulta ReceiverStrategy  
                    │       │
                    │       ▼
                    │  Seleciona: RoundRobin/Random/Priority
                    │
                    └─► Executa processamento
```

### 3. Gerenciamento de Receivers
O Gateway **registra e monitora** todos os Data Receivers do sistema:

- **Registry dinâmico**: Receivers podem se registrar/desregistrar em runtime
- **Health monitoring**: Verifica se receivers estão responsivos
- **Load balancing**: Distribui carga entre receivers disponíveis
- **Failover automático**: Remove receivers com falha e redistribui carga

## Ciclo de Vida Controlado

### Startup Sequence
```
1. getInstance() ──► Cria instância única
                         │
                         ▼
2. Inicializa estruturas ──► observers = new ArrayList<>()
   thread-safe               receivers = new ArrayList<>()
                         │   strategies = new ConcurrentHashMap<>()
                         ▼
3. Configura strategies ──► Registra UDP, HTTP, TCP, gRPC
                         │
                         ▼
4. Inicia monitoramento ──► FaultToleranceManager.start()
                         │   HeartbeatMonitor ativo
                         ▼
5. Sistema ready ──────► running.set(true)
```

### Shutdown Sequence
```
1. stop() chamado ────► running.set(false)
                         │
                         ▼
2. Para receivers ────► receiver.stop() para cada um
                         │
                         ▼
3. Para observers ────► observer.cleanup() se suportado
                         │
                         ▼
4. Limpa recursos ────► ExecutorService.shutdown()
                         │
                         ▼
5. Estado limpo ──────► Pronto para restart se necessário
```

## Integração com Outros Padrões

### Com Strategy Pattern
O Singleton **não compete** com Strategy, mas sim **coordena** seu uso:

```java
// Gateway não implementa lógica de seleção
public void processMessage(IoTMessage message) {
    // Delega para Strategy
    CommunicationStrategy strategy = selectCommunicationStrategy(message);
    ReceiverStrategy receiverStrategy = getReceiverStrategy();
    
    // Coordena a execução
    DataReceiver receiver = receiverStrategy.selectReceiver(message, receivers);
    strategy.sendMessage(message, receiver);
    
    // Notifica observers
    notifyObservers("MESSAGE_PROCESSED", message);
}
```

### Com Observer Pattern
O Singleton é o **Subject central** do sistema, notificando observers de todos os eventos relevantes:

- **Sensor registration/unregistration**
- **Message processing events**
- **Receiver status changes**
- **System state changes**

### Com Fault Tolerance
O Singleton **coordena a tolerância a falhas** mas não a implementa diretamente:

```java
// Gateway detecta falha
if (!receiver.isHealthy()) {
    // Notifica observers
    notifyObservers("RECEIVER_FAILED", receiver);
    
    // Remove da lista ativa
    removeReceiver(receiver);
    
    // FaultToleranceManager (observer) automaticamente
    // inicia recovery procedures
}
```

## Vantagens da Implementação

### Centralização Inteligente
- **Coordenação sem controle excessivo**: Gateway orquestra mas não microgerencia
- **Delegação apropriada**: Strategies fazem decisões específicas
- **Observabilidade**: Todos os eventos passam pelo ponto central

### Thread Safety Eficiente
- **Double-checked locking**: Performance otimizada
- **Estruturas concorrentes**: ConcurrentHashMap para dados compartilhados
- **AtomicBoolean**: Estado thread-safe sem sincronização pesada

### Flexibilidade Arquitetural
- **Easy testing**: Singleton pode ser resetado em testes
- **Runtime configuration**: Strategies podem ser alteradas dinamicamente
- **Modular growth**: Novos observers e strategies podem ser adicionados

## Cenários de Uso Real

### Startup do Sistema
```
Sistema iniciando ──► IoTGateway.getInstance()
                            │
                            ▼
                    Configurações carregadas
                            │
                            ▼
                    Strategies registradas
                            │
                            ▼
                    Observers attachados
                            │
                            ▼
                    Data Receivers iniciados
                            │
                            ▼
                    Sistema pronto para receber dados
```

### Processamento de Mensagem
```
UDP Message ──► Gateway.processMessage()
                        │
                        ├─► Strategy seleciona protocolo
                        ├─► Strategy seleciona receiver
                        ├─► Observer notificado
                        └─► Message processada
```

### Falha de Receiver
```
Receiver falha ──► Gateway detecta via health check
                          │
                          ├─► Remove receiver da lista
                          ├─► Notifica observers
                          └─► FaultTolerance inicia recovery
```

O padrão Singleton no IoTGateway demonstra como um **coordenador central inteligente** pode **orquestrar sistemas complexos** sem criar **acoplamento excessivo**, mantendo **flexibilidade** e **robustez** em ambientes distribuídos de alta concorrência.