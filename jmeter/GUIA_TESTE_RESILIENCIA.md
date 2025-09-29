# 🛡️ COMPARAÇÃO - ARQUIVOS JMETER PARA TESTE DE RESILIÊNCIA

## 📋 **PROBLEMA IDENTIFICADO**

O arquivo original `Sistema_IoT_Final_Funcionando.jmx` retornava **200 OK** mesmo com o servidor desligado porque:
- `waitresponse = false` → Não esperava resposta do servidor
- Sem Response Assertions → Não validava conteúdo da resposta
- Timeout alto (5000ms) → Mascarava problemas de conectividade

## 🔧 **CORREÇÕES APLICADAS NO ARQUIVO CORRIGIDO**

### **📁 Arquivo Corrigido: `Sistema_IoT_Resiliente_Corrigido.jmx`**

### **1. Configuração UDP Corrigida:**
```xml
<!-- ANTES (PROBLEMÁTICO) -->
<boolProp name="waitresponse">false</boolProp>  ❌ Não espera resposta
<stringProp name="timeout">5000</stringProp>     ❌ Timeout muito alto

<!-- DEPOIS (CORRIGIDO) -->
<boolProp name="waitresponse">true</boolProp>    ✅ Força esperar resposta
<stringProp name="timeout">3000</stringProp>     ✅ Timeout mais realista
```

### **2. Response Assertions Adicionadas:**
```xml
<ResponseAssertion>
  <stringProp name="1968934071">SUCCESS</stringProp>  ✅ Deve conter "SUCCESS"
  <stringProp name="Assertion.custom_message">ERRO: Sistema não respondeu com SUCCESS - pode estar offline!</stringProp>
</ResponseAssertion>
```

## 🧪 **COMPORTAMENTO ESPERADO AGORA**

### **✅ Com Servidor ONLINE:**
- **Taxa de Erro**: 0-5%
- **Resposta**: "SUCCESS" nas mensagens UDP
- **Assertions**: Passam (encontram "SUCCESS")

### **❌ Com Servidor OFFLINE:**
- **Taxa de Erro**: 100%
- **Resposta**: Timeout ou vazia
- **Assertions**: Falham (não encontram "SUCCESS")

## 🎯 **COMO USAR PARA DEMONSTRAR RESILIÊNCIA**

### **1. CENÁRIO NORMAL (Baixos Erros):**
```powershell
# 1. Iniciar sistema IoT
mvn exec:java

# 2. Executar teste JMeter
D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat -n -t jmeter\Sistema_IoT_Resiliente_Corrigido.jmx -l jmeter\results\cenario_normal.jtl
```

### **2. CENÁRIO FALHA (100% Erros):**
```powershell
# 1. DESLIGAR sistema IoT (Ctrl+C)

# 2. Executar teste JMeter (deve falhar)
D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat -n -t jmeter\Sistema_IoT_Resiliente_Corrigido.jmx -l jmeter\results\cenario_falha.jtl
```

### **3. CENÁRIO RECUPERAÇÃO (Erros Diminuem):**
```powershell
# 1. RELIGAR sistema IoT durante execução do JMeter
mvn exec:java

# 2. Observar diminuição gradual de erros
```

## 📊 **ANÁLISE DOS RESULTADOS**

### **Arquivo de Logs:**
- `cenario_normal.jtl` → Deve mostrar Success=true, responseCode=200
- `cenario_falha.jtl` → Deve mostrar Success=false, timeouts
- `cenario_recuperacao.jtl` → Deve mostrar transição false→true

### **Comando para Análise:**
```powershell
# Ver taxa de erro em tempo real
Get-Content jmeter\results\cenario_falha.jtl | Select-String "false" | Measure-Object
```

## 🏆 **DEMONSTRAÇÃO PERFEITA DE TOLERÂNCIA A FALHAS**

1. **Sistema Online** → JMeter mostra 0% erros ✅
2. **Sistema Offline** → JMeter mostra 100% erros ❌ 
3. **Sistema Recupera** → JMeter mostra erros diminuindo 🔄
4. **Fault Tolerance Manager** → Cria backups automaticamente 🛡️

---

## 🔗 **ARQUIVOS DISPONÍVEIS**

- `Sistema_IoT_Final_Funcionando.jmx` → Original (waitResponse=false)
- `Sistema_IoT_Resiliente_Corrigido.jmx` → **USAR ESTE** (waitResponse=true + Assertions)

**🎯 Use sempre o arquivo CORRIGIDO para demonstrar resiliência real!**