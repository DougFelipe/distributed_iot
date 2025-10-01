# 🔧 **PROTOCOLO SELECTION CHEAT SHEET - APRESENTAÇÃO**

## 🎯 **RESUMO EXECUTIVO**

Sua pergunta foi **FUNDAMENTAL** e expõe um ponto crítico da arquitetura! O Sistema IoT implementa **Strategy Pattern** para permitir seleção de protocolo na inicialização. Aqui está como funciona:

---

## 📋 **COMANDOS CORRETOS PARA APRESENTAÇÃO**

### **✅ COMANDO RECOMENDADO (UDP - Mais Estável):**
```powershell
cd d:\distribuida
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="UDP"
```

### **🌐 COMANDO ALTERNATIVO (HTTP - JMeter Fácil):**
```powershell
cd d:\distribuida
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="HTTP"
```

### **🚀 COMANDO AVANÇADO (gRPC - 3,00 Pontos):**
```powershell
cd d:\distribuida
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="GRPC"
```

---

## 🔍 **COMO O SISTEMA DETECTA O PROTOCOLO**

### **1. Parsing de Argumentos (Implementado):**
```java
private static String getProtocolFromArgs(String[] args) {
    // Verifica argumentos: UDP, HTTP, TCP, GRPC
    // Retorna protocolo encontrado ou "UDP" como padrão
}
```

### **2. Strategy Pattern (Implementado):**
```java
public static void configureCommunicationStrategy(IoTGateway gateway, String protocol) {
    switch (protocol) {
        case "UDP": configureUDPStrategy(gateway); break;
        case "HTTP": configureHTTPStrategy(gateway); break;
        case "GRPC": configureGRPCStrategy(gateway); break;
        default: configureUDPStrategy(gateway); // Fallback
    }
}
```

### **3. Portas Automáticas por Protocolo:**
```java
public static int getProtocolPort(String protocol) {
    switch (protocol) {
        case "UDP": return 9090;
        case "HTTP": return 8080;
        case "GRPC": return 9000;
        default: return 9090;
    }
}
```

---

## 🎪 **DEMONSTRAÇÃO DURANTE APRESENTAÇÃO**

### **CENÁRIO 1: UDP (Recomendado para Estabilidade)**
```powershell
# Terminal 1 - Gateway UDP (Data Receivers inclusos automaticamente)
mvn exec:java "-Dexec.args=UDP"

# Logs esperados:
# ✅ "🔧 Protocolo definido via argumento: UDP"
# ✅ "✅ Strategy Pattern: Protocolo UDP configurado"  
# ✅ "🚀 Gateway IoT iniciado na porta 9090"
```

### **CENÁRIO 2: HTTP (Fácil Integração JMeter)**
```powershell
# Terminal 1 - Gateway HTTP
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="HTTP"

# Logs esperados:
# ✅ "🔧 Protocolo definido via argumento: HTTP"
# ✅ "✅ Strategy Pattern: Protocolo HTTP configurado"
# ✅ "🚀 Gateway IoT iniciado na porta 8080"
```

### **CENÁRIO 3: gRPC (Máxima Pontuação)**
```powershell
# Terminal 1 - Gateway gRPC
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="GRPC"

# Logs esperados:
# ✅ "🔧 Protocolo definido via argumento: GRPC"
# ✅ "✅ Strategy Pattern: Protocolo GRPC configurado"
# ✅ "🚀 Gateway IoT iniciado na porta 9000"
```

---

## 🚨 **TROUBLESHOOTING COMMON ISSUES**

### **Problema: "Protocol not detected"**
```powershell
# Solução 1: System Property
mvn exec:java -Diot.protocol=UDP -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem"

# Solução 2: Formato explícito
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="--protocol=UDP"
```

### **Problema: "Port already in use"**
```powershell
# Verificar processo usando a porta
netstat -ano | findstr ":9090"
netstat -ano | findstr ":8080"
netstat -ano | findstr ":9000"

# Matar processo se necessário
taskkill /PID <PID> /F
```

### **Problema: JMeter não conecta**
- **UDP:** JMeter precisa de plugin UDP, porta 9090
- **HTTP:** JMeter HTTP padrão, porta 8080
- **gRPC:** JMeter precisa de plugin gRPC, porta 9000

---

## 💡 **ESTRATÉGIA DE APRESENTAÇÃO**

### **Opção 1: Demonstração Segura (UDP)**
- ✅ **Protocolo:** UDP (mais estável)
- ✅ **Pontuação:** 1,50 pontos (UDP)
- ✅ **Risco:** Baixo
- ✅ **JMeter:** Requer configuração UDP

### **Opção 2: Demonstração Versátil (HTTP)**
- ✅ **Protocolo:** HTTP (mais compatível)  
- ✅ **Pontuação:** 1,50 pontos (HTTP)
- ✅ **Risco:** Médio
- ✅ **JMeter:** Funciona out-of-the-box

### **Opção 3: Demonstração Ambiciosa (gRPC)**
- ✅ **Protocolo:** gRPC (mais pontos)
- ✅ **Pontuação:** 3,00 pontos (gRPC)
- ✅ **Risco:** Alto
- ✅ **JMeter:** Requer configuração avançada

---

## 📊 **MATRIZ DE DECISÃO**

| **Protocolo** | **Pontos** | **Estabilidade** | **JMeter** | **Complexidade** | **Recomendação** |
|---------------|------------|------------------|------------|------------------|------------------|
| UDP | 1,50 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ✅ **SEGURO** |
| HTTP | 1,50 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ **VERSÁTIL** |
| gRPC | 3,00 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⚠️ **ARRISCADO** |

---

## 🎯 **RESPOSTA À SUA PERGUNTA**

> **"Como saberá qual protocolo chamar?"**

**R:** O sistema implementa **Strategy Pattern** com **detecção automática de protocolo** via:

1. **Argumento de linha de comando:** `UDP`, `HTTP`, `GRPC`
2. **Configuração de porta automática:** Cada protocolo tem sua porta
3. **Fallback inteligente:** Se não especificado, usa UDP como padrão
4. **Logs claros:** Mostra qual protocolo foi detectado e configurado

**O comando correto não é `"gateway 9090"`, mas sim `"UDP"` ou `"HTTP"` ou `"GRPC"`!**

---

## 🏆 **RECOMENDAÇÃO FINAL**

**Para apresentação de TOLERÂNCIA A FALHAS:**

```powershell
# COMANDO RECOMENDADO:
mvn exec:java "-Dexec.args=UDP"
```

**Por quê UDP?**
- ✅ Mais estável para demo ao vivo
- ✅ Protocolo nativo implementado
- ✅ Menos dependências externas
- ✅ Logs mais claros
- ✅ Recuperação de falhas mais previsível

**Tolerância a falhas funciona IGUAL em todos os protocolos!** 📊