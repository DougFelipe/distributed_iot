# 🚀 GUIA COMPLETO - MULTI-PROTOCOLO HTTP + TCP

## ✅ **PROBLEMA COMPLETAMENTE RESOLVIDO**

O erro "Address already in use: bind" foi **100% corrigido** com o **IoTMultiProtocolLauncher** que usa **portas isoladas para cada protocolo**.

## 📋 **COMANDOS PARA TESTES SIMULTÂNEOS**

### 🔥 **MÉTODO 1: Terminais Separados (RECOMENDADO)**

**Terminal 1 - HTTP (Porta 8081):**
```powershell
mvn exec:java@multi-protocol '-Dexec.args=HTTP'
```

**Terminal 2 - TCP (Porta 8082):**
```powershell
mvn exec:java@multi-protocol '-Dexec.args=TCP'
```

**Terminal 3 - UDP (Porta 9090):**
```powershell
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="UDP"
```

**Terminal 4 - GRPC (Porta 9999):**
```powershell
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="GRPC"
```

---

## 🔧 **CONFIGURAÇÃO DE PORTAS POR PROTOCOLO**

| Protocolo | Gateway Port | Data Receivers Ports | JMeter Target |
|-----------|-------------|-------------------|---------------|
| **HTTP**  | 8081        | 9001, 9002       | localhost:8081 |
| **TCP**   | 8082        | 9003, 9004       | localhost:8082 |
| **UDP**   | 9090        | 9091, 9092       | localhost:9090 |
| **GRPC**  | 9999        | 9005, 9006       | localhost:9999 |

---

## 🧪 **SEQUÊNCIA DE TESTES JMETER**

### **1. Preparar Ambientes**
```powershell
# Terminal 1 - HTTP
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="HTTP"

# Terminal 2 - TCP
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="TCP"
```

### **2. Executar Testes JMeter**

**Teste HTTP:**
```powershell
# Configure JMeter para porta 8081
# Execute: HTTP_TCP_Test_FINAL_CORRIGIDO.jmx com HTTP endpoints
```

**Teste TCP:**
```powershell
# Configure JMeter para porta 8082
# Execute: HTTP_TCP_Test_FINAL_CORRIGIDO.jmx com TCP sockets
```

---

## 📊 **LOGS ESPERADOS**

### **HTTP System (Terminal 1):**
```
===============================================================================
      SISTEMA IoT DISTRIBUÍDO - PROTOCOLO HTTP 
                   UFRN - DIMAP - Multi-Protocol
===============================================================================
🚀 Iniciando Sistema IoT Distribuído com Protocolo HTTP...
🔧 Gateway Port: 8081
🔧 Receiver Ports: [9001, 9002]
🔧 Protocolo definido: HTTP
✅ Estratégia HTTP configurada na porta 8081
🌐 Endpoints HTTP disponíveis:
   POST /sensor/data - Envio de dados de sensores
   GET  /sensor/status - Status do sistema
   GET  /health - Health check
✅ Gateway IoT iniciado na porta 8081
🏗️ Criando Data Receivers (Instâncias B Stateful) para HTTP...
✅ Data Receiver DATA_RECEIVER_HTTP_1 iniciado na porta 9001
✅ Data Receiver DATA_RECEIVER_HTTP_2 iniciado na porta 9002
🛡️ Iniciando Fault Tolerance Manager...
✅ Observer Pattern: HeartbeatMonitor adicionado
✅ Sistema IoT Distribuído HTTP iniciado com sucesso!
🧪 PRONTO PARA TESTES JMETER HTTP na porta 8081
🔄 Sistema HTTP executando. Use Ctrl+C para parar.
```

### **TCP System (Terminal 2):**
```
===============================================================================
      SISTEMA IoT DISTRIBUÍDO - PROTOCOLO TCP 
                   UFRN - DIMAP - Multi-Protocol
===============================================================================
🚀 Iniciando Sistema IoT Distribuído com Protocolo TCP...
🔧 Gateway Port: 8082
🔧 Receiver Ports: [9003, 9004]
🔧 Protocolo definido: TCP
✅ Estratégia TCP configurada na porta 8082
🔌 Servidor TCP aguardando conexões persistentes
📝 Formato de mensagem: SENSOR_DATA|sensor_id|type|location|timestamp|value
✅ Gateway IoT iniciado na porta 8082
🏗️ Criando Data Receivers (Instâncias B Stateful) para TCP...
✅ Data Receiver DATA_RECEIVER_TCP_1 iniciado na porta 9003
✅ Data Receiver DATA_RECEIVER_TCP_2 iniciado na porta 9004
🛡️ Iniciando Fault Tolerance Manager...
✅ Observer Pattern: HeartbeatMonitor adicionado
✅ Sistema IoT Distribuído TCP iniciado com sucesso!
🧪 PRONTO PARA TESTES JMETER TCP na porta 8082
🔄 Sistema TCP executando. Use Ctrl+C para parar.
```

---

## ⚡ **VANTAGENS DA NOVA ARQUITETURA**

### ✅ **1. Isolamento Total**
- Cada protocolo em **portas completamente diferentes**
- **Zero conflitos** de binding
- **Receivers independentes** por protocolo

### ✅ **2. Testes Simultâneos**
- **HTTP e TCP rodando ao mesmo tempo**
- **JMeter simultâneo** em protocolos diferentes
- **Comparação de performance** entre protocolos

### ✅ **3. Tolerância a Falhas Individual**
- **Fault tolerance independente** por protocolo
- **Recovery específico** para cada sistema
- **Monitoramento isolado**

---

## 🎯 **PRÓXIMOS PASSOS**

1. **Executar HTTP:**
   ```powershell
   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="HTTP"
   ```

2. **Executar TCP (Terminal separado):**
   ```powershell
   mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTMultiProtocolLauncher" -Dexec.args="TCP"
   ```

3. **Configurar JMeter:**
   - HTTP → `localhost:8081`
   - TCP → `localhost:8082`

4. **Executar testes simultâneos** e comparar resultados!

---

## 🐛 **TROUBLESHOOTING**

### **Porta ainda ocupada?**
```powershell
# Verificar portas em uso
netstat -ano | findstr ":8081"
netstat -ano | findstr ":8082"

# Matar processo se necessário
taskkill /PID <PID> /F
```

### **Erro de compilação?**
```powershell
# Limpar e recompilar
mvn clean compile
```

---

✅ **PROBLEMA RESOLVIDO: Agora HTTP e TCP podem rodar simultaneamente em terminais separados com portas isoladas!** 🎉