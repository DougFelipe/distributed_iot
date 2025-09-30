# 🌐 Plano JMX - Sistema IoT HTTP/TCP - Guia de Execução

## 📋 Resumo do Plano de Teste

### 🎯 **Arquivo Criado**: `Plano_Teste_HTTP_TCP_IoT.jmx`

O plano JMX foi configurado para testar completamente os protocolos **HTTP** e **TCP** do seu sistema IoT distribuído.

---

## 🏗️ Estrutura do Plano de Teste

### **1. 🌐 HTTP IoT Sensors Group**
- **Threads**: 5 sensores simultâneos
- **Ramp-up**: 10 segundos
- **Loops**: 10 iterações por sensor
- **Porta**: 8081 (HTTP Strategy)

#### **Samplers HTTP:**
1. **📊 HTTP Health Check** - `GET /health`
2. **🤖 HTTP Sensor Registration** - `POST /iot/sensor/register`
3. **📡 HTTP Sensor Data** - `POST /iot/sensor/data`

### **2. 🔌 TCP IoT Sensors Group**
- **Threads**: 3 sensores simultâneos
- **Ramp-up**: 5 segundos
- **Loops**: 10 iterações por sensor
- **Porta**: 8082 (TCP Strategy)
- **Delay**: 5 segundos (inicia após HTTP)

#### **Samplers TCP:**
1. **🤖 TCP Sensor Registration** - Formato: `SENSOR_REGISTER|sensor_id|type|location|timestamp|data`
2. **📡 TCP Sensor Data** - Formato: `SENSOR_DATA|sensor_id|type|location|timestamp|value`
3. **💓 TCP Heartbeat** - Formato: `HEARTBEAT|sensor_id|status|timestamp`

---

## 🎛️ Listeners Configurados para Visualização

### **📊 Listeners Incluídos:**

| Listener | Funcionalidade | Arquivo de Saída |
|----------|----------------|------------------|
| **📋 View Results Tree** | Detalhes de cada requisição | `http_tcp_results_tree.jtl` |
| **📊 Summary Report** | Estatísticas consolidadas | `http_tcp_summary.jtl` |
| **📈 Response Time Graph** | Gráfico de tempos de resposta | `http_tcp_response_times.jtl` |
| **🔄 Active Threads Over Time** | Threads ativas ao longo do tempo | `http_tcp_active_threads.jtl` |
| **⚡ Throughput Over Time** | Taxa de transferência temporal | `http_tcp_throughput.jtl` |
| **❌ View Results in Table** | Tabela de erros (apenas falhas) | `http_tcp_errors.jtl` |

---

## 🚀 Como Executar

### **1. Preparação do Sistema**

Primeiro, inicie o sistema com o protocolo desejado:

#### **Para testar HTTP:**
```bash
# Terminal 1 - Iniciar sistema HTTP
java -jar target/sistema-distribuido-1.0.0.jar HTTP
```

#### **Para testar TCP:**
```bash
# Terminal 1 - Iniciar sistema TCP  
java -jar target/sistema-distribuido-1.0.0.jar TCP
```

#### **Para testar ambos (recomendado):**
```powershell
# Terminal 1 - HTTP
java -jar target/sistema-distribuido-1.0.0.jar HTTP

# Terminal 2 - TCP (em paralelo)
java -jar target/sistema-distribuido-1.0.0.jar TCP
```

#### **⚠️ IMPORTANTE - Resolução de Problemas:**

Se aparecer erro **"Address already in use"**:
```powershell
# 1. Verificar processos usando as portas
netstat -ano | findstr ":8081"
netstat -ano | findstr ":8082"

# 2. Terminar processos Java antigos (se necessário)
taskkill /F /IM java.exe

# 3. Tentar novamente
java -jar target/sistema-distribuido-1.0.0.jar HTTP
```

### **2. Execução no JMeter GUI**

1. **Abrir JMeter GUI:**
   ```bash
   jmeter
   ```

2. **Carregar o plano:**
   - File → Open → Selecionar `jmeter/Plano_Teste_HTTP_TCP_IoT.jmx`

3. **Verificar configurações:**
   - **Variables**: HTTP_HOST=localhost, HTTP_PORT=8081, TCP_HOST=localhost, TCP_PORT=8082
   - **Thread Groups**: HTTP (5 threads) e TCP (3 threads) configurados

4. **Executar teste:**
   - Clique no botão ▶️ **Start** (Ctrl+R)

---

## 📊 Cenários de Teste

### **🌐 Cenário HTTP**

#### **Fluxo de Execução:**
1. **Health Check** → Verifica se servidor HTTP está respondendo
2. **Sensor Registration** → Registra sensor via POST JSON
3. **Sensor Data** → Envia dados IoT via POST JSON
4. **Random Wait** → Pausa de 1-3 segundos entre requisições

#### **Dados de Teste HTTP:**
```json
// Registration
{
  "messageId": "UUID_GENERATED",
  "sensorId": "HTTP_TCP_SENSOR_1_1",
  "messageType": "SENSOR_REGISTER",
  "sensorType": "0-4 (random)",
  "location": "HTTP_TEST_LOCATION_1",
  "timestamp": 1696118400,
  "versionVector": { "HTTP_TCP_SENSOR_1_1": 1 },
  "gatewayId": "HTTP_GATEWAY"
}

// Data
{
  "messageId": "UUID_GENERATED",
  "sensorId": "HTTP_TCP_SENSOR_1_1", 
  "messageType": "SENSOR_DATA",
  "sensorType": "0-4 (random)",
  "value": "15-35 (random)",
  "unit": "°C",
  "location": "HTTP_TEST_LOCATION_1",
  "timestamp": 1696118400,
  "versionVector": { "HTTP_TCP_SENSOR_1_1": 2 },
  "gatewayId": "HTTP_GATEWAY"
}
```

### **🔌 Cenário TCP**

#### **Fluxo de Execução:**
1. **TCP Registration** → Conecta e envia comando de registro
2. **TCP Data** → Envia dados do sensor via socket
3. **TCP Heartbeat** → Envia heartbeat de keep-alive
4. **Random Wait** → Pausa de 0.5-2 segundos entre conexões

#### **Dados de Teste TCP:**
```
// Registration
SENSOR_REGISTER|TCP_SENSOR_1_1|2|TCP_TEST_LOCATION_1|1696118400|REGISTER_DATA

// Data  
SENSOR_DATA|TCP_SENSOR_1_1|2|TCP_TEST_LOCATION_1|1696118400|23.5

// Heartbeat
HEARTBEAT|TCP_SENSOR_1_1|ACTIVE|1696118400
```

---

## 🎯 Validações e Assertions

### **HTTP Assertions:**
- **Health Check**: Response Code = 200
- **Registration**: Response Code = 200 ou 201
- **Data**: Response Code = 200

### **TCP Assertions:**
- **Registration**: Response contém "OK", "SUCCESS" ou "REGISTERED"
- **Data**: Response contém "OK", "SUCCESS" ou "PROCESSED"
- **Heartbeat**: Sem validação específica (opcional)

---

## 📈 Métricas Esperadas

### **🎯 Resultados de Sucesso:**

| Protocolo | Operação | Response Time | Throughput | Success Rate |
|-----------|----------|---------------|------------|--------------|
| **HTTP** | Health Check | < 50ms | > 100/sec | 100% |
| **HTTP** | Registration | < 200ms | > 50/sec | 100% |
| **HTTP** | Data Send | < 150ms | > 80/sec | 100% |
| **TCP** | Registration | < 100ms | > 30/sec | 100% |
| **TCP** | Data Send | < 80ms | > 50/sec | 100% |
| **TCP** | Heartbeat | < 30ms | > 100/sec | 100% |

### **⚠️ Cenários de Falha (Sistema Desligado):**
- **Connection Refused**: Quando sistema não está rodando
- **Timeout**: Quando sistema está sobrecarregado
- **Response Code 500**: Erros internos do servidor

---

## 🔧 Personalização do Plano

### **Ajustar Carga:**
```xml
<!-- Aumentar número de threads HTTP -->
<stringProp name="ThreadGroup.num_threads">10</stringProp>

<!-- Aumentar número de loops -->
<stringProp name="LoopController.loops">20</stringProp>

<!-- Ajustar ramp-up time -->
<stringProp name="ThreadGroup.ramp_time">30</stringProp>
```

### **Modificar Portas:**
```xml
<!-- Variáveis no TestPlan -->
<stringProp name="Argument.value">8080</stringProp> <!-- HTTP_PORT -->
<stringProp name="Argument.value">8083</stringProp> <!-- TCP_PORT -->
```

### **Adicionar Novos Listeners:**
1. Botão direito no Test Plan
2. Add → Listener → Escolher tipo
3. Configurar filename para salvar resultados

---

## 🎮 Comandos de Execução Rápida

### **Teste Completo (HTTP + TCP):**
```powershell
# Terminal 1 - Sistema HTTP (porta 8081)
java -jar target/sistema-distribuido-1.0.0.jar HTTP

# Terminal 2 - Sistema TCP (porta 8082) 
java -jar target/sistema-distribuido-1.0.0.jar TCP

# Terminal 3 - JMeter GUI para visualização
jmeter -t jmeter/HTTP_TCP_Test_Simple.jmx
```

### **Modo Não-GUI (CI/CD):**
```powershell
# JMeter em modo batch (sem GUI)
jmeter -n -t jmeter/HTTP_TCP_Test_Simple.jmx -l results/test_results.jtl -e -o results/html_report
```

### **✅ COMANDOS CORRETOS TESTADOS:**
```powershell
# HTTP na porta 8081
java -jar target/sistema-distribuido-1.0.0.jar HTTP

# TCP na porta 8082  
java -jar target/sistema-distribuido-1.0.0.jar TCP

# JMeter GUI com arquivo corrigido
jmeter -t jmeter/HTTP_TCP_Test_Simple.jmx
```

---

## 🎉 Resultados Esperados

### **✅ Sistema Funcionando Corretamente:**
- **HTTP Health Check**: 100% success, < 50ms
- **HTTP Registration**: 100% success, response JSON válido
- **HTTP Data**: 100% success, dados processados
- **TCP Registration**: 100% success, response "OK/SUCCESS"
- **TCP Data**: 100% success, dados recebidos
- **TCP Heartbeat**: 100% success, conexão mantida

### **📊 Gráficos no JMeter GUI:**
- **Response Time Graph**: Curvas estáveis < 200ms
- **Active Threads**: Crescimento gradual conforme ramp-up
- **Throughput**: Taxa constante sem quedas
- **Summary Report**: 0% error rate

### **❌ Indicadores de Problema:**
- **Error Rate > 0%**: Sistema não respondendo ou com falhas
- **Response Time > 1s**: Possível sobrecarga
- **Throughput baixo**: Gargalo de performance
- **Connection timeouts**: Sistema não acessível

---

**🎯 O plano JMX está pronto para uso! Execute no JMeter GUI para visualizar gráficos em tempo real e validar a performance dos protocolos HTTP e TCP do seu sistema IoT distribuído.**