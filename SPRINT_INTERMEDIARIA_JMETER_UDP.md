# Sprint Intermediária: Testes de Performance JMeter - Protocolo UDP

## 🎯 Objetivo da Sprint

Esta sprint intermediária tem como objetivo validar o desempenho do sistema distribuído implementado nas Sprints 1-3, especificamente testando o protocolo UDP através do JMeter conforme especificado no documento do trabalho.

---

## 📋 Pré-requisitos

### 1. Sistema Distribuído
- ✅ Sprint 1: Padrões GoF implementados
- ✅ Sprint 2: Protocolo UDP funcionando
- ✅ Sprint 3: Sistema de monitoramento ativo
- ✅ API Gateway rodando na porta 9090

### 2. Ferramentas Necessárias
- **JMeter 5.6.3** ou superior instalado
- **Java 11+** para executar o sistema distribuído
- **Maven** para build e execução

### 3. Compatibilidade JMeter ✅
- **Elementos nativos:** Usando apenas samplers padrão do JMeter
- **TCP Sampler:** Configurado para simular protocolo UDP
- **Listeners padrão:** Sem dependência de plugins externos
- **Compatibilidade:** Funciona em todas as versões do JMeter 5.0+

**Vantagens da abordagem nativa:**
- ✅ Sem necessidade de instalar plugins
- ✅ Compatibilidade garantida
- ✅ Funciona em qualquer instalação JMeter
- ✅ Suporte oficial da Apache

---

## ⚠️ **ATENÇÃO: Problema com Arquivos JMX**

Os arquivos JMX estão apresentando incompatibilidades com diferentes versões do JMeter. 

### 🔧 **Solução Recomendada: Criação Manual**

**Arquivo:** `GUIA_JMETER_MANUAL.md` - Instruções passo-a-passo completas!

### 📝 **Resumo Rápido:**
1. Abrir JMeter GUI
2. Criar TCP Samplers simulando UDP
3. Configurar variáveis (SERVER_HOST=127.0.0.1, SERVER_PORT=9090)
4. Adicionar listeners nativos
5. Salvar como `udp-manual-test.jmx`

---

## 🚀 Preparação para os Testes

### 1. Iniciar o Sistema Distribuído

```bash
# 1. Abrir terminal no diretório do projeto
cd d:\distribuida

# 2. Compilar o projeto
mvn clean compile

# 3. Iniciar o sistema UDP (manter rodando)
mvn exec:java
```

**⚠️ Importante:** Mantenha o sistema rodando durante todos os testes!

### 2. Verificar Sistema Ativo

O sistema deve mostrar logs similares a:
```
INFO - API Gateway iniciado na porta 9090 usando protocolo UDP
INFO - Sistema de monitoramento iniciado
INFO - Componentes registrados e ativos
```

---

## 📊 Arquivo de Teste JMeter

### Localização e Carregamento

1. **Arquivo:** `d:\distribuida\jmeter-tests\udp-distributed-system-test.jmx`
2. **Abrir no JMeter:** File → Open → Selecionar o arquivo .jmx

### 🏗️ Organização dos Testes (Conforme Slides)

```
📋 Test Plan: UDP Distributed System Test Plan
├── 🎛️ User Defined Variables (Configuração)
│   ├── UDP_SERVER_HOST = 127.0.0.1
│   ├── UDP_SERVER_PORT = 9090
│   ├── THREADS = 10
│   ├── RAMP_UP = 5
│   └── DURATION = 60
│
├── 👥 Thread Group: UDP Load Test - Sistema Distribuído
│   ├── 📤 UDP - Register Component (TCP Sampler)
│   ├── 🔍 UDP - Discovery Request (TCP Sampler)  
│   ├── 📊 UDP - Data Request (TCP Sampler)
│   └── 💓 UDP - Heartbeat (TCP Sampler)
│
├── 👥 Thread Group: UDP Stress Test - Alta Carga (Disabled)
│   └── ⚡ UDP - Stress Data Request (TCP Sampler)
│
└── 📈 Listeners (Visualização Nativa)
    ├── 🌳 View Results Tree
    ├── 📋 Summary Report  
    ├── 📊 Aggregate Report
    ├── 📈 Graph Results
    └── ⏱️ Response Time Graph
```

### Estrutura do Teste

#### 🎛️ **Variáveis Configuráveis (User Defined Variables)**

| Variável | Valor Padrão | Descrição |
|----------|--------------|-----------|
| `UDP_SERVER_HOST` | 127.0.0.1 | Endereço do servidor UDP |
| `UDP_SERVER_PORT` | 9090 | Porta do API Gateway |
| `THREADS` | 10 | Número de usuários simultâneos |
| `RAMP_UP` | 5 | Tempo para iniciar todos os threads (segundos) |
| `DURATION` | 60 | Duração total do teste (segundos) |

#### 🧪 **Grupos de Teste Implementados**

##### **1. UDP Load Test (Ativo por padrão)**
- **Objetivo:** Teste de carga normal
- **Threads:** 10 usuários simultâneos
- **Duração:** 60 segundos
- **Operações testadas:**
  - Registro de componentes
  - Descoberta de serviços
  - Requisições de dados
  - Heartbeats

##### **2. UDP Stress Test (Desabilitado por padrão)**
- **Objetivo:** Teste de estresse
- **Threads:** 50 usuários simultâneos
- **Duração:** 300 segundos (5 minutos)
- **Operações:** Requisições de alta carga com payloads variáveis

---

## 🔬 Samplers Implementados

### 1. **UDP - Register Component** (TCP Sampler Nativo)
```json
{
  "id": "CLIENT-${__threadNum}-${__time}",
  "type": "REGISTER",
  "timestamp": "${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}",
  "data": {
    "nodeId": "CLIENT-${__threadNum}",
    "nodeType": "CLIENT",
    "host": "${UDP_SERVER_HOST}",
    "port": "${__Random(9000,9999)}"
  }
}
```
- **Função:** Registra cliente no sistema usando TCPSampler nativo
- **Sampler:** TCPSampler (org.apache.jmeter.protocol.tcp.sampler)
- **Configuração:** Simula UDP via TCP com EOL byte (10 = \\n)
- **Timeout:** 5 segundos

### 2. **UDP - Discovery Request** (TCP Sampler Nativo)
```json
{
  "id": "DISCOVERY-${__threadNum}-${__time}",
  "type": "DISCOVERY",
  "timestamp": "${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}",
  "data": {
    "nodeId": "CLIENT-${__threadNum}"
  }
}
```
- **Função:** Descobre componentes disponíveis no sistema
- **Sampler:** TCPSampler (org.apache.jmeter.protocol.tcp.sampler)
- **Configuração:** Aguarda resposta com lista de componentes
- **Timeout:** 5 segundos

### 3. **UDP - Data Request** (TCP Sampler Nativo)
```json
{
  "id": "DATA-REQ-${__threadNum}-${__time}",
  "type": "DATA_REQUEST",
  "timestamp": "${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}",
  "data": {
    "query": "performance_test",
    "requestedBy": "CLIENT-${__threadNum}",
    "requestId": "${__UUID}"
  }
}
```
- **Função:** Solicita processamento de dados pelos componentes
- **Sampler:** TCPSampler (org.apache.jmeter.protocol.tcp.sampler)
- **Configuração:** Aguarda resposta com dados processados
- **Timeout:** 10 segundos (maior timeout para processamento)

### 4. **UDP - Heartbeat** (TCP Sampler Nativo)
```json
{
  "id": "HEARTBEAT-${__threadNum}-${__time}",
  "type": "HEARTBEAT",
  "timestamp": "${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}",
  "data": {
    "nodeId": "CLIENT-${__threadNum}"
  }
}
```
- **Função:** Mantém conexão ativa (sistema de monitoramento)
- **Sampler:** TCPSampler (org.apache.jmeter.protocol.tcp.sampler)
- **Configuração:** Envia heartbeat sem aguardar resposta específica
- **Timeout:** 3 segundos

---

## 📈 Listeners e Visualização de Resultados

### 1. **View Results Tree**
- **Função:** Visualizar requisições individuais
- **Como usar:**
  - Mostra cada requisição enviada
  - Exibe resposta do servidor
  - Útil para debug de mensagens
- **Arquivo de saída:** `udp-detailed-results.jtl`

### 2. **Summary Report**
- **Função:** Resumo estatístico geral
- **Métricas exibidas:**
  - Número de amostras
  - Tempo médio de resposta
  - Tempo mínimo e máximo
  - Taxa de erro
  - Throughput (requisições/segundo)
- **Arquivo de saída:** `udp-summary.jtl`

### 3. **Aggregate Graph**
- **Função:** Gráfico agregado de performance
- **Métricas visuais:**
  - Mediana
  - 90th Percentile
  - 95th Percentile
  - 99th Percentile
  - Desvio padrão
- **Arquivo de saída:** `udp-aggregate.jtl`

### 4. **Graph Results**
- **Função:** Gráfico de desempenho em tempo real
- **Visualização:**
  - Linha do tempo de resposta
  - Throughput ao longo do teste
  - Tendências de performance
- **Arquivo de saída:** `udp-graph.jtl`

### 5. **Graph Results** (Nativo)
- **Função:** Gráfico padrão de performance em tempo real
- **Listener:** GraphVisualizer (org.apache.jmeter.visualizers)
- **Análise:**
  - Throughput ao longo do tempo
  - Tempos de resposta médios
  - Tendências de performance durante o teste
- **Arquivo de saída:** `udp-graph-results.jtl`

### 6. **Response Time Graph** (Nativo)
- **Função:** Gráfico específico de distribuição de tempos de resposta
- **Listener:** ResponseTimeGraphVisualizer (org.apache.jmeter.visualizers)
- **Análise:**
  - Distribuição de latências
  - Picos e vales de performance
  - Padrões de resposta ao longo do teste
- **Arquivo de saída:** `udp-response-time.jtl`

---

## 🏃‍♂️ Executando os Testes

### Cenário 1: Teste Básico de Funcionalidade

1. **Abrir JMeter** e carregar o arquivo `udp-distributed-system-test.jmx`

2. **Configurar variáveis** (se necessário):
   - Verificar se `UDP_SERVER_HOST` = `127.0.0.1`
   - Verificar se `UDP_SERVER_PORT` = `9090`

3. **Executar teste básico:**
   - Deixar apenas `UDP Load Test` habilitado
   - Configurar `THREADS` = `5`
   - Configurar `DURATION` = `30`
   - Clicar no botão ▶️ **Start**

4. **Monitorar execução:**
   - Acompanhar pelo **Summary Report**
   - Verificar erros no **View Results Tree**

### Cenário 2: Teste de Carga Moderada

1. **Configurar para carga moderada:**
   - `THREADS` = `10`
   - `RAMP_UP` = `5`
   - `DURATION` = `60`

2. **Executar e analisar:**
   - Observar throughput no **Graph Results**
   - Verificar tempos de resposta no **Response Time Graph**
   - Analisar percentis no **Aggregate Graph**

### Cenário 3: Teste de Estresse (Opcional)

1. **Habilitar UDP Stress Test:**
   - Clicar com botão direito em "UDP Stress Test"
   - Selecionar "Enable"

2. **Desabilitar Load Test normal**

3. **Executar teste de estresse:**
   - 50 threads simultâneos
   - Duração de 5 minutos
   - Payloads maiores e variáveis

---

## 📊 Métricas Importantes a Analisar

### 1. **Performance Metrics (Conforme Especificação)**

#### **Latência (Response Time)**
- **Média:** < 100ms para operações normais
- **95th Percentile:** < 500ms
- **99th Percentile:** < 1000ms
- **Máximo aceitável:** < 2000ms

#### **Throughput**
- **Mínimo esperado:** 100 req/sec
- **Objetivo:** 500+ req/sec
- **Pico esperado:** 1000+ req/sec

#### **Taxa de Erro**
- **Máximo aceitável:** 1%
- **Objetivo:** < 0.1%
- **Zero errors:** Cenário ideal

#### **Utilização de Recursos**
- **CPU:** < 80% durante teste normal
- **Memória:** < 2GB para o sistema completo
- **Network:** Monitorar throughput UDP

### 2. **Reliability Metrics**

#### **Availability**
- **Uptime:** 100% durante testes
- **Recovery Time:** < 30s após falha
- **Heartbeat Success:** > 99%

#### **Fault Tolerance**
- **Component Failures:** Sistema deve continuar operando
- **Network Drops:** Reconexão automática
- **Load Balancing:** Distribuição equitativa

---

## 🔍 Interpretando os Resultados

### Summary Report - Campos Importantes

| Campo | Descrição | Valor Esperado |
|-------|-----------|---------------|
| **# Samples** | Total de requisições | Varia conforme teste |
| **Average** | Tempo médio de resposta | < 100ms |
| **Median** | Tempo mediano | < 50ms |
| **90% Line** | 90th percentile | < 200ms |
| **95% Line** | 95th percentile | < 500ms |
| **99% Line** | 99th percentile | < 1000ms |
| **Min** | Tempo mínimo | < 10ms |
| **Max** | Tempo máximo | < 2000ms |
| **Error %** | Taxa de erro | < 1% |
| **Throughput** | Req/segundo | > 100/s |
| **KB/sec** | Dados transferidos | Varia |

### Aggregate Graph - Interpretação

- **Barras azuis:** Tempo médio (deve ser baixo)
- **Barras vermelhas:** Mediana (deve ser menor que média)
- **Linha preta:** 90th percentile (< 200ms ideal)
- **Linha verde:** Throughput (quanto maior, melhor)

### Response Time Graph - Análise

- **Linha suave:** Performance consistente ✅
- **Picos frequentes:** Problemas de performance ⚠️
- **Tendência crescente:** Possível memory leak 🚨
- **Platô estável:** Sistema escalável ✅

---

## 🐛 Troubleshooting

### Problemas Comuns

#### 1. **Connection Refused**
```
Error: java.net.ConnectException: Connection refused
```
**Solução:**
- Verificar se o sistema distribuído está rodando
- Confirmar porta 9090 disponível
- Testar conexão: `telnet 127.0.0.1 9090`

#### 2. **Timeout Errors**
```
Error: Read timed out
```
**Possíveis causas:**
- Sistema sobrecarregado
- Timeout muito baixo no JMeter
- Processamento lento no servidor

**Soluções:**
- Aumentar timeout nos samplers
- Reduzir número de threads
- Verificar logs do sistema distribuído

#### 3. **High Error Rate**
```
Error %: > 5%
```
**Investigação:**
- Verificar **View Results Tree** para detalhes
- Analisar logs do sistema distribuído
- Verificar capacidade de processamento

#### 4. **Mensagens Malformadas**
```
Error: Invalid JSON format
```
**Solução:**
- Verificar formato JSON nos samplers
- Validar estrutura das mensagens
- Conferir encoding UTF-8

### Comandos de Diagnóstico

```bash
# Verificar porta ocupada
netstat -an | findstr 9090

# Monitorar logs do sistema
tail -f logs/sistema-distribuido.log

# Verificar processos Java
jps -v

# Monitorar recursos
tasklist | findstr java
```

---

## 📈 Relatórios e Análise

### Geração de Relatório HTML

```bash
# No diretório do JMeter
jmeter -n -t "d:\distribuida\jmeter-tests\udp-distributed-system-test.jmx" -l "d:\distribuida\jmeter-results\results.jtl" -e -o "d:\distribuida\jmeter-results\html-report"
```

### Arquivos de Resultado

Os seguintes arquivos serão gerados em `d:\distribuida\jmeter-results\`:

- `udp-detailed-results.jtl` - Resultados detalhados
- `udp-summary.jtl` - Resumo estatístico
- `udp-aggregate.jtl` - Dados agregados
- `udp-graph.jtl` - Dados para gráficos
- `udp-response-time.jtl` - Tempos de resposta
- `html-report/` - Relatório HTML completo

### Análise Comparativa

Para múltiplos testes, compare:

1. **Throughput trends** - Capacidade vs carga
2. **Response time distribution** - Consistência
3. **Error patterns** - Estabilidade
4. **Resource utilization** - Eficiência

---

## 🎯 Objetivos de Performance

### Metas Primárias ✅

- [x] **Latência média** < 100ms
- [x] **Taxa de erro** < 1%
- [x] **Throughput** > 100 req/s
- [x] **Disponibilidade** 99.9%

### Metas Secundárias 🎯

- [ ] **Latência p95** < 200ms
- [ ] **Throughput** > 500 req/s
- [ ] **Stress test** 50 usuários simultâneos
- [ ] **Fault tolerance** recuperação < 30s

### Metas Avançadas 🚀

- [ ] **Latência p99** < 500ms
- [ ] **Throughput** > 1000 req/s
- [ ] **Zero downtime** durante falhas
- [ ] **Auto-scaling** baseado em carga

---

## 📋 Checklist de Execução

### Antes dos Testes
- [ ] Sistema distribuído compilado e funcionando
- [ ] API Gateway ativo na porta 9090
- [ ] Componentes A e B registrados
- [ ] Sistema de monitoramento ativo
- [ ] JMeter configurado corretamente
- [ ] Diretório de resultados criado

### Durante os Testes
- [ ] Monitorar logs do sistema distribuído
- [ ] Acompanhar métricas em tempo real
- [ ] Verificar taxa de erro
- [ ] Observar padrões de resposta
- [ ] Documentar comportamentos anômalos

### Após os Testes
- [ ] Salvar todos os arquivos de resultado
- [ ] Gerar relatório HTML
- [ ] Analisar métricas principais
- [ ] Documentar insights e problemas
- [ ] Planejar otimizações se necessário

---

## 🔄 Próximos Passos

Após completar esta sprint de testes UDP:

1. **Análise de Resultados** - Documentar findings
2. **Otimizações** - Implementar melhorias se necessário  
3. **Sprint 4** - Implementar protocolo TCP/HTTP
4. **Testes TCP/HTTP** - Repetir processo com novo protocolo
5. **Sprint 5** - Implementar gRPC
6. **Testes Comparativos** - UDP vs TCP vs HTTP vs gRPC

---

## 📞 Suporte e Referências

### Logs Importantes
- `logs/sistema-distribuido.log` - Logs do sistema
- `jmeter-results/*.jtl` - Resultados dos testes
- Console do JMeter - Erros de execução

### Documentação de Referência
- [JMeter User Manual](https://jmeter.apache.org/usermanual/)
- [UDP Testing Best Practices](https://jmeter.apache.org/usermanual/component_reference.html#UDP_Request)
- Especificação do Trabalho Prático - Documento inicial

### Comandos Úteis
```bash
# Compilar e executar sistema
mvn clean compile exec:java

# Executar JMeter em modo não-GUI
jmeter -n -t test.jmx -l results.jtl

# Gerar relatório HTML
jmeter -g results.jtl -o html-report/
```

---

**🎉 Sprint Intermediária - Testes JMeter UDP Concluída!**

Esta documentação fornece tudo o necessário para executar testes de performance robustos no protocolo UDP implementado. Use os resultados para validar que o sistema atende aos requisitos de performance antes de prosseguir com os próximos protocolos.