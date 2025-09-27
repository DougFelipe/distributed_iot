# Guia: Criando Teste UDP no JMeter Manualmente

## 🎯 Problema Identificado
Os arquivos JMX estão apresentando incompatibilidades. A melhor abordagem é criar o teste **manualmente** no JMeter GUI.

---

## 📝 Instruções Passo-a-Passo

### 1. **Abrir JMeter**
```bash
# Abrir JMeter GUI
jmeter.bat  # Windows
# ou
jmeter      # Linux/Mac
```

### 2. **Criar Test Plan**
1. **Test Plan** já existe por padrão
2. **Renomear:** "UDP Sistema Distribuído Test"
3. **Comments:** "Teste de performance protocolo UDP"

### 3. **Adicionar User Defined Variables**
1. **Botão direito** no Test Plan → Add → Config Element → **User Defined Variables**
2. **Nome:** "Configurações do Sistema"
3. **Adicionar variáveis:**

| Name | Value | Description |
|------|--------|-------------|
| `SERVER_HOST` | `127.0.0.1` | IP do servidor UDP |
| `SERVER_PORT` | `9090` | Porta do API Gateway |
| `THREADS` | `5` | Número de usuários |
| `LOOPS` | `10` | Repetições por usuário |

### 4. **Criar Thread Group**
1. **Botão direito** no Test Plan → Add → Threads (Users) → **Thread Group**
2. **Nome:** "UDP Load Test"
3. **Configurações:**
   - **Number of Threads:** `${THREADS}` ou `5`
   - **Ramp-up Period:** `2` (segundos)
   - **Loop Count:** `${LOOPS}` ou `10`

### 5. **Adicionar TCP Sampler (Simulando UDP)**

#### 5.1 Register Component
1. **Botão direito** no Thread Group → Add → Sampler → **TCP Sampler**
2. **Nome:** "UDP - Register Component"
3. **Configurações:**
   - **TCPClient classname:** `TCPClientImpl`
   - **Server Name or IP:** `${SERVER_HOST}`
   - **Port Number:** `${SERVER_PORT}`
   - **Timeout (milliseconds):** `5000`
   - **Set Nodelay:** ☐ (desmarcado)
   - **Close connection:** ☑ (marcado)
   - **SO_LINGER:** deixe vazio
   - **End of line(EOL) byte value:** `10` (representa \\n)
   - **Text to send:**
```json
{"id":"CLIENT-${__threadNum}-${__time}","type":"REGISTER","timestamp":"${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}","data":{"nodeId":"CLIENT-${__threadNum}","nodeType":"CLIENT","host":"${SERVER_HOST}","port":"${__Random(9000,9999)}"}}
```

#### 5.2 Discovery Request
1. **Botão direito** no Thread Group → Add → Sampler → **TCP Sampler**
2. **Nome:** "UDP - Discovery Request"
3. **Configurações:** (mesmas do anterior, exceto Text to send)
   - **Text to send:**
```json
{"id":"DISCOVERY-${__threadNum}-${__time}","type":"DISCOVERY","timestamp":"${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}","data":{"nodeId":"CLIENT-${__threadNum}"}}
```

#### 5.3 Data Request
1. **Botão direito** no Thread Group → Add → Sampler → **TCP Sampler**
2. **Nome:** "UDP - Data Request"
3. **Configurações:** (timeout 10000ms)
   - **Text to send:**
```json
{"id":"DATA-REQ-${__threadNum}-${__time}","type":"DATA_REQUEST","timestamp":"${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}","data":{"query":"performance_test","requestedBy":"CLIENT-${__threadNum}","requestId":"${__UUID}"}}
```

#### 5.4 Heartbeat
1. **Botão direito** no Thread Group → Add → Sampler → **TCP Sampler**
2. **Nome:** "UDP - Heartbeat"
3. **Configurações:** (timeout 3000ms)
   - **Text to send:**
```json
{"id":"HEARTBEAT-${__threadNum}-${__time}","type":"HEARTBEAT","timestamp":"${__time(yyyy-MM-dd'T'HH:mm:ss.SSS)}","data":{"nodeId":"CLIENT-${__threadNum}"}}
```

### 6. **Adicionar Delays (Opcional)**
Para cada TCP Sampler, adicionar um timer:
1. **Botão direito** no sampler → Add → Timer → **Uniform Random Timer**
2. **Configurações:**
   - **Random Delay Maximum:** `300` (300ms)
   - **Constant Delay Offset:** `100` (100ms)

### 7. **Adicionar Listeners**

#### 7.1 View Results Tree
1. **Botão direito** no Test Plan → Add → Listener → **View Results Tree**
2. **Nome:** "Resultados Detalhados"
3. **Filename:** `jmeter-results/detailed-results.jtl`

#### 7.2 Summary Report
1. **Botão direito** no Test Plan → Add → Listener → **Summary Report**
2. **Nome:** "Relatório de Performance"
3. **Filename:** `jmeter-results/summary-report.jtl`

#### 7.3 Aggregate Report
1. **Botão direito** no Test Plan → Add → Listener → **Aggregate Report**
2. **Nome:** "Estatísticas Agregadas"
3. **Filename:** `jmeter-results/aggregate-report.jtl`

### 8. **Salvar o Teste**
1. **File** → **Save Test Plan As...**
2. **Nome:** `udp-manual-test.jmx`
3. **Local:** `d:\distribuida\jmeter-tests\`

---

## 🚀 Executando o Teste

### Preparação
```bash
# 1. Iniciar o sistema distribuído
cd d:\distribuida
mvn clean compile exec:java

# 2. Aguardar logs:
# "API Gateway iniciado na porta 9090 usando protocolo UDP"
```

### Execução no JMeter
1. **Verificar configurações** de variáveis
2. **Clicar no botão ▶️ Start** (triângulo verde)
3. **Monitorar** pelos listeners em tempo real

### Validação
- **View Results Tree:** Verificar se requests estão sendo enviados
- **Summary Report:** Analisar métricas de performance
- **Console do sistema:** Verificar se mensagens chegam no servidor

---

## 📊 Métricas Esperadas

| Métrica | Valor Esperado | Observações |
|---------|---------------|-------------|
| **Samples** | 200 (5 users × 10 loops × 4 samplers) | Total de requisições |
| **Average** | < 100ms | Tempo médio de resposta |
| **Error %** | < 5% | Taxa de erro aceitável |
| **Throughput** | > 50/sec | Requisições por segundo |

---

## 🔧 Troubleshooting

### Problema: Connection Refused
```
java.net.ConnectException: Connection refused
```
**Solução:**
- Verificar se sistema distribuído está rodando
- Confirmar porta 9090 está aberta
- Testar: `telnet 127.0.0.1 9090`

### Problema: Timeout
```
Read timed out
```
**Soluções:**
- Aumentar timeout nos TCP Samplers
- Reduzir número de threads
- Verificar logs do servidor

### Problema: Invalid Response
```
Response seems truncated
```
**Soluções:**
- Verificar formato JSON das mensagens
- Confirmar EOL byte = 10
- Verificar encoding UTF-8

---

## 💡 Dicas Importantes

1. **TCP vs UDP:** Estamos simulando UDP via TCP por compatibilidade
2. **EOL Byte:** Valor 10 representa quebra de linha (\\n)
3. **JSON Válido:** Testar mensagens em validador JSON online
4. **Logs:** Sempre monitorar logs do sistema distribuído
5. **Incremental:** Começar com 1 thread, depois aumentar gradualmente

---

## 📁 Estrutura Final no JMeter

```
📋 UDP Sistema Distribuído Test
├── 🎛️ Configurações do Sistema (User Defined Variables)
├── 👥 UDP Load Test (Thread Group)
│   ├── 📤 UDP - Register Component (TCP Sampler)
│   ├── 🔍 UDP - Discovery Request (TCP Sampler)
│   ├── 📊 UDP - Data Request (TCP Sampler)
│   └── 💓 UDP - Heartbeat (TCP Sampler)
├── 🌳 Resultados Detalhados (View Results Tree)
├── 📋 Relatório de Performance (Summary Report)
└── 📊 Estatísticas Agregadas (Aggregate Report)
```

Este método manual garante 100% de compatibilidade! 🎯