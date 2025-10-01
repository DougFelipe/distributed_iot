# Padrão Strategy - Adaptação Dinâmica de Protocolos e Balanceamento

## Implementação Dupla do Strategy

O sistema IoT implementa o padrão Strategy em **duas dimensões complementares**, cada uma resolvendo aspectos diferentes da comunicação distribuída:

1. **CommunicationStrategy**: Como comunicar (protocolo)
2. **ReceiverStrategy**: Onde entregar (balanceamento de carga)

Esta abordagem dupla permite **flexibilidade máxima** na adaptação a diferentes cenários de comunicação IoT.

## Arquitetura Strategy Dupla

```
                    ┌─────────────────────────────────┐
                    │        IoTGateway               │
                    │     (Strategy Client)           │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │      Message Processing         │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │     Dual Strategy Selection      │
                    └─────┬───────────────────┬───────┘
                          │                   │
            ┌─────────────▼──────┐  ┌─────────▼──────────┐
            │ CommunicationStrategy│  │  ReceiverStrategy   │
            │                    │  │                    │
            │ UDP ◄─┐            │  │ RoundRobin ◄─┐     │
            │ HTTP  │ Select     │  │ Random       │ Select │
            │ TCP   │ Protocol   │  │ Priority     │ Target │
            │ gRPC ◄┘            │  │ LoadBased   ◄┘     │
            └────────────────────┘  └────────────────────┘
                          │                   │
                          └─────────┬─────────┘
                                    ▼
                        ┌─────────────────────┐
                        │   Message Delivery   │
                        └─────────────────────┘
```

## CommunicationStrategy - Seleção de Protocolo

### Problema Resolvido
Sistemas IoT precisam **suportar múltiplos protocolos** simultaneamente. Diferentes cenários requerem diferentes abordagens:

- **UDP**: Baixa latência, aceitável perda de pacotes (sensores de temperatura)
- **HTTP**: Interoperabilidade web, RESTful APIs (dashboards)
- **TCP**: Comunicação confiável (dados críticos)
- **gRPC**: Alta performance, streaming (dados em tempo real)

### Implementação da Strategy

```
┌─────────────────────────────────────────────────────────┐
│                CommunicationStrategy                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │     UDP     │  │    HTTP     │  │     TCP     │     │
│  │             │  │             │  │             │     │
│  │ • Fast      │  │ • RESTful   │  │ • Reliable  │     │
│  │ • Lossy     │  │ • Cacheable │  │ • Ordered   │     │
│  │ • Broadcast │  │ • Stateless │  │ • Stateful  │     │
│  │             │  │             │  │             │     │
│  │ Port: 9091  │  │ Port: 8080  │  │ Port: 9092  │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                                                         │
│                  ┌─────────────┐                       │
│                  │    gRPC     │                       │
│                  │             │                       │
│                  │ • Streaming │                       │
│                  │ • Binary    │                       │
│                  │ • Efficient │                       │
│                  │             │                       │
│                  │ Port: 9090  │                       │
│                  └─────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

### Seleção Inteligente de Protocolo

O Gateway seleciona o protocolo baseado em **características da mensagem**:

```java
CommunicationStrategy strategy = switch (message.getType()) {
    case SENSOR_DATA -> udpStrategy;        // Rápido, tolerante a perdas
    case ALERT -> tcpStrategy;              // Confiável, crítico
    case HEARTBEAT -> udpStrategy;          // Frequente, não crítico
    case STATUS_REQUEST -> httpStrategy;    // RESTful, cacheable
    case STREAMING_DATA -> grpcStrategy;    // Alto volume, eficiente
};
```

### Vantagens por Protocolo

**UDP Strategy:**
- **Latência mínima**: Sem handshake ou confirmações
- **Broadcast natural**: Um sensor pode notificar múltiplos receivers
- **Tolerante a falhas**: Perda de pacotes não interrompe fluxo
- **Ideal para**: Dados de sensores frequentes e não-críticos

**HTTP Strategy:**
- **Interoperabilidade**: Qualquer cliente HTTP pode consumir
- **Cache inteligente**: Responses podem ser cacheadas
- **RESTful design**: Semântica clara de operações
- **Ideal para**: APIs públicas e dashboards

**TCP Strategy:**
- **Confiabilidade garantida**: Entrega ordenada e confirmada
- **Controle de fluxo**: Adaptação automática à capacidade do receiver
- **Conexões persistentes**: Reutilização eficiente de conexões
- **Ideal para**: Dados críticos e comandos de controle

**gRPC Strategy:**
- **Performance superior**: Serialização binária eficiente
- **Streaming bidirecional**: Comunicação full-duplex
- **Type safety**: Contratos bem definidos via Protocol Buffers
- **Ideal para**: Comunicação interna de alta performance

## ReceiverStrategy - Balanceamento de Carga

### Problema do Bottleneck
Com **múltiplos sensores** enviando dados simultaneamente, um único receiver rapidamente se torna bottleneck. O sistema precisa **distribuir carga** de forma inteligente entre receivers disponíveis.

### Estratégias de Balanceamento

```
                    ┌─────────────────────────────────┐
                    │      Incoming Messages          │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │    Receiver Strategy Selector   │
                    └─────┬───────┬───────┬───────────┘
                          │       │       │
              ┌───────────▼──┐  ┌─▼─┐  ┌─▼────────┐
              │ Round Robin  │  │Random│ │Priority │
              │              │  │      │ │         │
              │ R1 → R2 → R3 │  │ R?   │ │ P1>P2>P3│
              │ ↑         ↓  │  │      │ │         │
              │ └─────────┘  │  └─────┘ │ └─────────┘
              └──────────────┘          └──────────┘
                      │                     │
                      ▼                     ▼
              ┌───────────────┐    ┌───────────────┐
              │ DataReceiver1 │    │ DataReceiver2 │
              │ Port: 9093    │    │ Port: 9094    │
              └───────────────┘    └───────────────┘
```

### Round Robin Strategy - Distribuição Balanceada

**Filosofia**: Distribuição **circular e justa** entre todos os receivers ativos.

**Implementação**:
- **AtomicInteger** para índice thread-safe
- **Modulo operation** para rotação circular
- **Filtros dinâmicos** para receivers ativos apenas

**Vantagens**:
- **Simplicidade**: Lógica clara e previsível
- **Justiça**: Todos os receivers recebem carga igual
- **Thread-safe**: Funciona corretamente em ambiente concorrente

**Cenário Ideal**: 
- Receivers com **capacidade similar**
- Mensagens com **custo de processamento uniforme**
- **Carga constante** ao longo do tempo

### Random Strategy - Distribuição Estocástica

**Filosofia**: Seleção **aleatória** para evitar padrões previsíveis.

**Vantagens**:
- **Anti-hotspot**: Reduz pontos quentes por acaso
- **Load spreading**: Distribui carga naturalmente
- **Simple failover**: Falhas não afetam padrão de distribuição

**Cenário Ideal**:
- **Carga irregular** com picos imprevisíveis
- Receivers com **capacidades diferentes**
- Necessidade de **distribuição orgânica**

### Priority Strategy - Distribuição Hierárquica

**Filosofia**: Receivers têm **prioridades diferentes** baseadas em capacidade ou importância.

**Implementação**:
- **Receivers primários**: Recebem carga primeiro
- **Receivers secundários**: Ativados sob alta carga
- **Receivers backup**: Apenas para failover

**Cenário Ideal**:
- **Hardware heterogêneo** com capacidades diferentes
- **SLA requirements** com receivers críticos
- **Cost optimization** usando receivers premium apenas quando necessário

## Integração das Strategies

### Decisão Composta
O Gateway faz **duas decisões independentes** para cada mensagem:

```java
// Seleção de protocolo baseada no tipo de mensagem
CommunicationStrategy commStrategy = selectCommunicationStrategy(message);

// Seleção de receiver baseada na estratégia de balanceamento
ReceiverStrategy receiverStrategy = getCurrentReceiverStrategy();
DataReceiver target = receiverStrategy.selectReceiver(message, activeReceivers);

// Execução composta
commStrategy.sendMessage(message, target);
```

### Flexibilidade Combinada
Esta abordagem permite **16 combinações diferentes** (4 protocolos × 4 strategies):

- **UDP + Round Robin**: Distribuição balanceada de dados de sensores
- **HTTP + Priority**: APIs críticas em receivers premium
- **TCP + Random**: Dados confiáveis com distribuição orgânica
- **gRPC + Load Based**: Streaming eficiente com balanceamento inteligente

## Adaptação Dinâmica

### Runtime Strategy Changes
O sistema permite **mudança de strategies em runtime** sem interrupção:

```java
// Mudança de strategy baseada em métricas
if (systemLoad > HIGH_THRESHOLD) {
    gateway.setReceiverStrategy(new PriorityReceiverStrategy());
} else if (systemLoad < LOW_THRESHOLD) {
    gateway.setReceiverStrategy(new RoundRobinStrategy());
}
```

### Failover Strategy
Quando receivers falham, as strategies **se adaptam automaticamente**:

```java
@Override
public void handleReceiverFailure(DataReceiver failed, List<DataReceiver> available) {
    // Remove receiver falho da rotação
    // Rebalanceia carga automaticamente
    // Continua operação sem interrupção
}
```

## Monitoramento e Métricas

### Observabilidade Strategy
Cada strategy **registra métricas detalhadas**:

- **Distribuição de carga** por receiver
- **Latência média** por protocolo
- **Taxa de falhas** por strategy
- **Throughput** por combinação protocol+receiver

### Otimização Baseada em Dados
O sistema pode **otimizar strategies automaticamente** baseado em:

- **Padrões de carga históricos**
- **Performance metrics em tempo real**
- **Capacidade atual dos receivers**
- **SLA requirements**

## Cenários de Uso Real

### Sistema IoT Industrial
```
Sensores críticos ──► TCP + Priority ──► Receivers premium
Sensores normais ──► UDP + Round Robin ──► Receivers padrão  
Dashboard API ──► HTTP + Random ──► Cache servers
Streaming data ──► gRPC + Load Based ──► Processing cluster
```

### Sistema IoT Residencial
```
Termostatos ──► UDP + Round Robin ──► Home servers
Câmeras ──► TCP + Priority ──► Storage servers
Assistente ──► HTTP + Random ──► Cloud APIs
Sensores ──► UDP + Random ──► Local processing
```

O padrão Strategy duplo permite que o sistema IoT **se adapte dinamicamente** às necessidades de comunicação, oferecendo **flexibilidade máxima** sem sacrificar **performance** ou **simplicidade** de implementação.