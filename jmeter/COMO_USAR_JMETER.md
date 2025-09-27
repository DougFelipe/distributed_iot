# 🧪 JMeter - Teste dos Padrões GoF UDP
## Sistema IoT Distribuído - Sprint 2

### 📋 **ARQUIVO CORRIGIDO PRONTO PARA USO**

O arquivo **`IoT_GoF_Patterns_UDP_Test_Simple.jmx`** foi corrigido e está funcionando perfeitamente! 

---

## 🚀 **Como Usar no JMeter GUI**

### **1. Abrir JMeter**
```bash
# Navegar para o diretório
cd d:\distribuida\jmeter

# Abrir JMeter GUI
"D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat"
```

### **2. Carregar o Arquivo de Teste**
- No JMeter GUI: **File → Open**
- Selecionar: `IoT_GoF_Patterns_UDP_Test_Simple.jmx`
- O arquivo será carregado sem erros!

---

## 🔧 **Configuração dos Testes**

### **Variáveis Globais (já configuradas)**
- **SERVER_HOST**: `localhost`
- **GATEWAY_PORT**: `9090`

### **Thread Groups (4 Padrões GoF)**

#### **1. Strategy Pattern - UDP Communication**
- **Threads**: 2 usuários
- **Ramp-up**: 2 segundos
- **Loops**: 3 iterações
- **Teste**: Registro de sensores via UDP

#### **2. Singleton Pattern - Gateway Instance**
- **Threads**: 3 usuários
- **Ramp-up**: 1 segundo
- **Loops**: 4 iterações
- **Teste**: Validação da instância única do Gateway

#### **3. Observer Pattern - Heartbeat Monitor**
- **Threads**: 2 usuários
- **Ramp-up**: 1 segundo
- **Loops**: 3 iterações
- **Teste**: Monitoramento de heartbeat

#### **4. Proxy Pattern - Message Routing**
- **Threads**: 2 usuários
- **Ramp-up**: 1 segundo
- **Loops**: 3 iterações
- **Teste**: Roteamento de mensagens via Gateway

---

## ⚡ **Como Executar os Testes**

### **PASSO 1: Iniciar o Sistema IoT**
```bash
# No terminal, na pasta do projeto
cd d:\distribuida
mvn compile exec:java "-Dexec.mainClass=br.ufrn.dimap.applications.IoTDistributedSystem"
```

### **PASSO 2: Executar no JMeter GUI**
1. **Carregar o arquivo**: `IoT_GoF_Patterns_UDP_Test_Simple.jmx`
2. **Verificar configurações**: Threads, loops, delays
3. **Executar**: Clique no botão **▶️ Start** (triângulo verde)
4. **Monitorar**: Use os listeners (View Results Tree, Summary Report, Aggregate Report)

---

## 📊 **Listeners Incluídos**

### **1. View Results Tree**
- Mostra cada requisição individual
- Útil para debug e análise detalhada
- Vê requests/responses em tempo real

### **2. Summary Report**
- Resumo estatístico por sampler
- Mostra throughput, erro rate, tempos médios
- Ideal para análise rápida

### **3. Aggregate Report**
- Estatísticas agregadas detalhadas
- Percentis, desvio padrão, min/max
- Análise completa de performance

---

## ✅ **Critérios de Sucesso**

### **Strategy Pattern**
- ✅ Comunicação UDP estabelecida
- ✅ Mensagens enviadas com sucesso
- ✅ Latência < 100ms

### **Singleton Pattern**
- ✅ Gateway responde consistentemente
- ✅ Mesma instância para todas as threads
- ✅ Status requests processados

### **Observer Pattern**
- ✅ Heartbeats enviados regularmente
- ✅ Monitor responde aos sinais
- ✅ Padrão de observação ativo

### **Proxy Pattern**
- ✅ Mensagens roteadas via Gateway
- ✅ Diferentes tipos de sensor suportados
- ✅ Roteamento transparente

---

## 🔍 **Validação dos Resultados**

### **Success Rate Esperado**
- **> 95%** para todos os testes
- Timeouts são esperados (sistema UDP)
- Erros de conexão indicam sistema offline

### **Latência Esperada**
- **< 50ms** para operações locais
- **< 100ms** para operações complexas
- Variação normal devido ao UDP

### **Throughput Esperado**
- **3-5 requests/segundo** por thread group
- Total: **~20 operations/segundo**
- Dependente da capacidade do sistema

---

## 🚨 **Troubleshooting**

### **Sistema IoT não está rodando**
```
Erro: Connection refused / Timeout
Solução: Iniciar o sistema IoT primeiro
```

### **Porta 9090 ocupada**
```
Erro: Address already in use
Solução: Verificar processos na porta 9090
```

### **JMeter não abre arquivo**
```
Erro: XML parsing error
Solução: Usar IoT_GoF_Patterns_UDP_Test_Simple.jmx
```

---

## 📈 **Executar Teste Completo**

### **Sequência Recomendada**
1. **Iniciar sistema IoT**
2. **Aguardar 10 segundos** (inicialização completa)
3. **Carregar arquivo JMeter**
4. **Executar todos os Thread Groups**
5. **Analisar resultados nos Listeners**
6. **Verificar logs do sistema IoT**

### **Duração Total Esperada**
- **~25-30 segundos** de execução
- **30 requests** totais (todos os padrões)
- **4 padrões GoF** validados simultaneamente

---

## 🎯 **Arquivo Pronto**

**✅ ARQUIVO FUNCIONANDO**: `IoT_GoF_Patterns_UDP_Test_Simple.jmx`

- ✅ Sem erros de parsing
- ✅ JSR223 Samplers com código Java nativo
- ✅ UDP sockets implementados corretamente
- ✅ Todos os 4 padrões GoF cobertos
- ✅ Listeners configurados para análise
- ✅ Timers para controle de carga
- ✅ Validações implementadas

**🚀 Pronto para usar na GUI do JMeter!**