# 🧪 TESTES JMETER - PADRÕES GoF UDP
## Sistema IoT Distribuído com Version Vector - Sprint 2

Este diretório contém os testes JMeter para validar os **4 Padrões GoF** implementados no sistema IoT distribuído.

---

## 📋 ARQUIVOS INCLUÍDOS

### 🎯 Arquivo Principal de Teste
- **`IoT_GoF_Patterns_UDP_Test.jmx`** - Plano de teste JMeter completo

### 🚀 Scripts de Execução
- **`run_gof_tests.bat`** - Script Windows Batch
- **`run_gof_tests.ps1`** - Script PowerShell (recomendado)

### 📊 Diretório de Resultados
- **`results/`** - Resultados dos testes (criado automaticamente)

---

## 🔧 PRÉ-REQUISITOS

### 1. Apache JMeter Instalado
```bash
# JMeter configurado em: D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\
# Scripts já configurados com o caminho correto
```

### 2. Sistema IoT Rodando
```bash
# No diretório raiz do projeto (COMANDO CORRIGIDO)
mvn compile exec:java "-Dexec.mainClass=br.ufrn.dimap.applications.IoTDistributedSystem"

# Ou simplesmente (após correção do pom.xml)
mvn compile exec:java
```

---

## 🧪 PADRÕES GoF TESTADOS

### 1. 🔸 Strategy Pattern
- **Teste:** Registro de sensores via UDP
- **Validação:** Comunicação UDP Strategy funcional
- **Threads:** 5 sensores simultâneos

### 2. 🔸 Singleton Pattern  
- **Teste:** Consulta de status do Gateway
- **Validação:** Gateway único IOT-GATEWAY-* respondendo
- **Threads:** 10 clientes simultâneos

### 3. 🔸 Observer Pattern
- **Teste:** Envio de heartbeats regulares
- **Validação:** HeartbeatMonitor processando eventos
- **Threads:** 3 sensores com heartbeat a cada 5s

### 4. 🔸 Proxy Pattern
- **Teste:** Envio de dados de sensores
- **Validação:** Gateway roteando mensagens processadas
- **Threads:** 5 sensores enviando dados a cada 2s

---

## ⚡ EXECUÇÃO RÁPIDA

### Opção 1: PowerShell (Recomendado)
```powershell
cd jmeter
.\run_gof_tests.ps1
```

### Opção 2: Batch Script
```cmd
cd jmeter
run_gof_tests.bat
```

### Opção 3: JMeter Direto
```bash
"D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat" -n -t IoT_GoF_Patterns_UDP_Test.jmx -l results/test_results.jtl
```

---

## 📊 CENÁRIOS DE TESTE DETALHADOS

### 🎯 Thread Group 1: Sensor Registration Test
- **Objetivo:** Testar Strategy Pattern
- **Configuração:** 5 threads, 1 loop, ramp-up 5s
- **Mensagem:** `SENSOR_REGISTER|SENSOR_X|TEMPERATURE|Lab-X|timestamp|25.5`
- **Validação:** Resposta contém "UDP"

### 🎯 Thread Group 2: Sensor Data Streaming Test  
- **Objetivo:** Testar Proxy Pattern
- **Configuração:** 5 threads, 10 loops, ramp-up 10s
- **Mensagem:** `SENSOR_DATA|SENSOR_X|valor|TEMPERATURE|timestamp|VV:SENSOR_X=counter`
- **Validação:** Resposta contém "processada"
- **Timer:** 2s entre mensagens

### 🎯 Thread Group 3: Heartbeat Observer Test
- **Objetivo:** Testar Observer Pattern  
- **Configuração:** 3 threads, 5 loops, ramp-up 5s
- **Mensagem:** `HEARTBEAT|SENSOR_X|ALIVE|timestamp|VV:SENSOR_X=counter`
- **Validação:** Resposta contém "Heartbeat"
- **Timer:** 5s entre heartbeats

### 🎯 Thread Group 4: Singleton Gateway Test
- **Objetivo:** Testar Singleton Pattern
- **Configuração:** 10 threads, 3 loops, ramp-up 2s  
- **Mensagem:** `DISCOVERY|CLIENT_X|GATEWAY_STATUS|timestamp`
- **Validação:** Resposta contém "IOT-GATEWAY"

---

## 📈 RELATÓRIOS GERADOS

### 🔍 Tipos de Relatório
1. **View Results Tree** - Detalhes de cada requisição
2. **Summary Report** - Resumo estatístico
3. **Graph Results** - Gráfico de performance
4. **Aggregate Report** - Métricas detalhadas
5. **HTML Dashboard** - Relatório visual completo

### 📂 Localização dos Resultados
```
results/
├── iot_gof_test_results.jtl     # Dados brutos
├── iot_gof_summary.jtl          # Resumo estatístico  
├── iot_gof_graph.jtl            # Dados para gráficos
├── iot_gof_aggregate.jtl        # Métricas agregadas
└── html-report/                 # Dashboard HTML
    └── index.html               # Relatório principal
```

---

## 🎯 MÉTRICAS DE SUCESSO

### ✅ Critérios de Aprovação
- **Taxa de Sucesso:** > 95%
- **Tempo de Resposta:** < 100ms (média)  
- **Throughput:** > 1 req/s por thread
- **Errors:** < 5% do total

### 📊 KPIs Esperados
- **Strategy Pattern:** 100% das mensagens UDP aceitas
- **Singleton Pattern:** ID do Gateway consistente em todas as respostas
- **Observer Pattern:** Heartbeats registrados sem falhas
- **Proxy Pattern:** Mensagens roteadas e processadas corretamente

---

## 🔧 TROUBLESHOOTING

### ❌ Problemas Comuns

#### 1. "Connection refused"
```
Solução: Verificar se o sistema IoT está rodando na porta 9090
Comando: netstat -an | findstr 9090
```

#### 2. "JMeter not found"
```
Solução: Instalar JMeter e adicionar ao PATH
Download: https://jmeter.apache.org/download_jmeter.cgi
```

#### 3. "Timeout errors"
```
Solução: Aumentar timeout no arquivo JMX
Padrão: 3000ms -> Aumentar para 5000ms
```

#### 4. "Pattern validation failed"
```
Solução: Verificar logs do sistema IoT
Arquivo: logs/sistema-distribuido.log  
```

### 🔍 Debug Mode
Para executar com mais detalhes:
```bash
jmeter -n -t IoT_GoF_Patterns_UDP_Test.jmx -l results/debug.jtl -j jmeter.log
```

---

## 📝 CUSTOMIZAÇÃO

### 🎛️ Variáveis Configuráveis
No arquivo JMX, seção "User Defined Variables":
- **SERVER_HOST:** localhost (padrão)
- **GATEWAY_PORT:** 9090 (padrão)  
- **TEST_DURATION:** 60 (segundos)

### 🔄 Modificações Avançadas
Para ajustar cenários específicos, editar no JMeter GUI:
1. Abrir `IoT_GoF_Patterns_UDP_Test.jmx`  
2. Modificar Thread Groups conforme necessário
3. Ajustar timers e assertions
4. Salvar e executar novamente

---

## 🏆 VALIDAÇÃO DA SPRINT 2

Este conjunto de testes **valida completamente** a implementação dos **4 Padrões GoF** no sistema IoT distribuído, garantindo:

- ✅ **Strategy Pattern** funcional para comunicação UDP
- ✅ **Singleton Pattern** mantendo gateway único
- ✅ **Observer Pattern** monitorando eventos em tempo real  
- ✅ **Proxy Pattern** roteando mensagens transparentemente

**🎓 UFRN - DIMAP - Sprint 2 - Testes Automatizados** 🚀