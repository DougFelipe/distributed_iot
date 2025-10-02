# 🎯 GUIA FINAL DE APRESENTAÇÃO - SISTEMA IOT DISTRIBUÍDO

## 🎪 DEMONSTRAÇÃO COMPLETA PARA APRESENTAÇÃO 🎪

### 📊 STATUS DO SISTEMA
✅ **ARQUITETURA CORRETA IMPLEMENTADA:**
- Instâncias A: Sensores IoT (Stateless) - TEMPERATURA e UMIDADE
- Instâncias B: Data Receivers (Stateful) - 2+ receptores com persistência
- Gateway: Proxy roteando mensagens para Data Receivers
- Tolerância a Falhas: Recuperação automática e monitoramento

✅ **PADRÕES GOF IMPLEMENTADOS:**
- Singleton: Gateway como proxy único
- Strategy: Seleção Round Robin de Data Receivers  
- Observer: Monitoramento de heartbeat
- Proxy: Gateway roteia para Data Receivers

✅ **RECURSOS DE TOLERÂNCIA A FALHAS:**
- Health Check automático a cada 5s
- Recuperação automática de instâncias falhas
- Criação automática de backup receivers
- Balanceamento de carga Round Robin

---

## 🚀 ROTEIRO DE DEMONSTRAÇÃO

### **PASSO 1: Iniciar o Sistema**
```powershell
# Navegar para o projeto
cd d:\distribuida

# Compilar o sistema
mvn compile

# Executar o sistema
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem"
```

**✅ RESULTADO ESPERADO:**
- Gateway iniciado na porta 9090
- DataReceiver1 iniciado na porta 9091
- DataReceiver2 iniciado na porta 9092
- FaultToleranceManager ativo
- Logs mostrando arquitetura completa

---

### **PASSO 2: Executar Testes JMeter**

#### 🧪 **Cenário 1: Operação Normal (Zero Erros)**
```powershell
# Executar teste JMeter
jmeter -n -t jmeter\Sistema_IoT_Final_Funcionando.jmx -l jmeter\results\test_normal.jtl
```

**✅ RESULTADO ESPERADO:**
- 0% de erro (todos os receivers online)
- Tempo de resposta baixo (< 100ms)
- Throughput alto
- Logs no sistema mostrando mensagens processadas

#### ⚠️ **Cenário 2: Simulação de Falha (Aumento de Erros)**
```powershell
# Durante o teste JMeter, simular falha:
# Parar um DataReceiver (Ctrl+C em uma instância)
# OU usar o script de simulação:
jmeter\scripts\simular_falha.ps1
```

**⚠️ RESULTADO ESPERADO:**
- Aumento na taxa de erro (receivers indisponíveis)
- Tempo de resposta aumenta
- Logs mostrando falhas detectadas
- FaultToleranceManager tentando recuperação

#### 🔄 **Cenário 3: Recuperação Automática (Diminuição de Erros)**
```powershell
# O sistema automaticamente:
# - Detecta a falha
# - Cria novo backup receiver na porta 9093
# - Rebalanceia a carga
# - Taxa de erro diminui
```

**🔄 RESULTADO ESPERADO:**
- Taxa de erro diminui progressivamente
- Novo receiver aparece nos logs
- Sistema volta à operação normal
- Demonstração de tolerância a falhas

---

### **PASSO 3: Análise dos Resultados**

#### 📈 **Métricas de Sucesso:**
1. **Taxa de Erro:**
   - Normal: 0%
   - Durante Falha: 20-50%
   - Após Recuperação: < 5%

2. **Tempo de Resposta:**
   - Normal: < 100ms
   - Durante Falha: > 200ms
   - Após Recuperação: < 150ms

3. **Throughput:**
   - Normal: > 50 req/s
   - Durante Falha: < 30 req/s
   - Após Recuperação: > 40 req/s

#### 📊 **Relatórios JMeter:**
- `jmeter/results/summary_final.jtl` - Resumo geral
- `jmeter/results/aggregate_final.jtl` - Métricas detalhadas
- `jmeter/results/response_times_final.jtl` - Tempos de resposta
- `jmeter/results/detailed_final.jtl` - Detalhes completos

---

## 🎯 PONTOS CHAVE PARA APRESENTAÇÃO

### **1. Arquitetura Distribuída Correta:**
```
Sensores IoT (A) → Gateway (Proxy) → Data Receivers (B)
     ↓                  ↓                    ↓
  Stateless         Roteamento          Stateful
```

### **2. Demonstração Visual:**
- **Console do Sistema:** Logs em tempo real
- **JMeter GUI:** Gráficos de performance
- **Tolerância a Falhas:** Recuperação automática

### **3. Narrativa de Apresentação:**
1. "Sistema iniciado com arquitetura correta"
2. "Teste normal mostra zero erros"
3. "Simulação de falha aumenta erros" 
4. "Sistema detecta e se recupera automaticamente"
5. "Taxa de erro diminui, sistema estabiliza"

---

## 🛠️ RESOLUÇÃO DE PROBLEMAS

### **Problema: JMeter não conecta**
```powershell
# Verificar se o sistema está rodando
netstat -an | findstr :9090

# Verificar logs do sistema
# Procurar por "Gateway iniciado na porta 9090"
```

### **Problema: Taxa de erro muito alta**
- Verificar se todos os Data Receivers estão online
- Checar logs de FaultToleranceManager
- Aguardar recuperação automática (10-15 segundos)

### **Problema: Sistema não inicia**
```powershell
# Limpar e recompilar
mvn clean compile

# Verificar se as portas estão livres
netstat -an | findstr ":909"
```

---

## 🎉 CONCLUSÃO DA APRESENTAÇÃO

**OBJETIVOS ALCANÇADOS:**
✅ Arquitetura distribuída correta (A → Proxy → B)
✅ Padrões GoF implementados e funcionais
✅ Tolerância a falhas com recuperação automática
✅ Testes JMeter demonstrando todos os cenários
✅ Sistema robusto e escalável

**PRÓXIMOS PASSOS (FUTURO):**
- Implementação de protocolos HTTP e gRPC
- Persistência em banco de dados
- Interface web para monitoramento
- Deploy em containers Docker

---

## 📞 COMANDOS RÁPIDOS PARA DEMONSTRAÇÃO

```powershell
# 1. Iniciar Sistema
cd d:\distribuida && mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem"

# 2. Teste JMeter (em outro terminal)
jmeter -n -t jmeter\Sistema_IoT_Final_Funcionando.jmx -l jmeter\results\demo.jtl

# 3. Ver Resultados
jmeter -g jmeter\results\demo.jtl -o jmeter\results\report-html/

# 4. Simular Falha (durante o teste)
# Parar uma instância com Ctrl+C e mostrar recuperação
```

**🎯 SISTEMA PRONTO PARA APRESENTAÇÃO PROFISSIONAL! 🎯** 🏆 GUIA FINAL DE APRESENTAÇÃO - SISTEMA IoT DISTRIBUÍDO

## 🎯 **SPRINT 2 - ARQUITETURA CORRETA + PADRÕES GoF + TOLERÂNCIA A FALHAS**

### 📊 **ARQUITETURA IMPLEMENTADA**

```
🔄 ARQUITETURA CORRETA DISTRIBUÍDA:

[Sensores IoT]     [Gateway]      [Data Receivers]
(Instâncias A) → (Proxy/Router) → (Instâncias B)
  Stateless         Stateless        Stateful
     |                  |               |
  📡 UDP            🔄 Round Robin   💾 Persistência
  📊 Dados          🛡️ Health Check  ⚡ Last Write Wins
  💨 Ephemeral      🔀 Load Balance  📈 Version Vector
```

### ✅ **PADRÕES GoF VALIDADOS**

1. **🏗️ Singleton Pattern**
   - **Onde**: `IoTGateway.java`
   - **Por quê**: Uma única instância de gateway para todo o sistema
   - **Benefício**: Controle centralizado de roteamento

2. **🎯 Strategy Pattern**
   - **Onde**: `ReceiverStrategy.java` + `RoundRobinReceiverStrategy.java`
   - **Por quê**: Diferentes algoritmos de seleção de Data Receivers
   - **Benefício**: Flexibilidade para trocar algoritmos de balanceamento

3. **👁️ Observer Pattern**
   - **Onde**: `HeartbeatMonitor.java`
   - **Por quê**: Monitoramento de status dos sensores
   - **Benefício**: Detecção automática de sensores inativos

4. **🔗 Proxy Pattern**
   - **Onde**: `IoTGateway.java` (refatorado de receptor final para proxy)
   - **Por quê**: Gateway atua como intermediário entre sensores e receivers
   - **Benefício**: Desacoplamento e roteamento inteligente

### 🛡️ **TOLERÂNCIA A FALHAS IMPLEMENTADA**

- **Health Check Automático**: Verifica Data Receivers a cada 5 segundos
- **Recuperação Automática**: Cria novos Data Receivers em caso de falha
- **Backup Receivers**: Portas 9093-9095 disponíveis para recuperação
- **Rebalanceamento**: Round Robin adapta-se automaticamente

### 🧪 **DEMONSTRAÇÃO JMeter**

#### **CENÁRIO 1: Operação Normal (Zero Erros)**
- ✅ 200 threads simultâneas
- ✅ 10 mensagens por thread = 2000 mensagens
- ✅ **Resultado esperado: 0% de erro**

#### **CENÁRIO 2: Simulação de Falha**
- 🔴 Desligar um Data Receiver manualmente
- 🔴 **Resultado esperado: Aumento na taxa de erro**
- 🟡 Sistema detecta falha e cria backup automaticamente
- 🟡 **Resultado esperado: Diminuição gradual da taxa de erro**

#### **CENÁRIO 3: Recuperação Completa**
- 🟢 Religar Data Receiver original
- 🟢 **Resultado esperado: Volta para 0% de erro**

---

## 🚀 **EXECUTANDO A DEMONSTRAÇÃO**

### **OPÇÃO 1: Automática (Recomendada)**
```powershell
# No diretório do projeto:
.\demonstracao_tolerancia_falhas.ps1
```

### **OPÇÃO 2: Manual (Para Controle Total)**

#### **1. Compilar o Sistema**
```powershell
mvn clean compile
```

#### **2. Iniciar Sistema Distribuído**
```powershell
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem"
```

#### **3. Executar JMeter (Operação Normal)**
```powershell
jmeter -n -t jmeter/Sistema_IoT_Apresentacao_Final.jmx -l jmeter/results/normal.jtl
```

#### **4. Simular Falha (Durante Execução JMeter)**
- Parar um Data Receiver (Ctrl+C)
- Observar aumento de erros
- Ver recuperação automática nos logs

#### **5. Executar JMeter (Com Falha)**
```powershell
jmeter -n -t jmeter/Sistema_IoT_Apresentacao_Final.jmx -l jmeter/results/com_falha.jtl
```

---

## 📊 **RESULTADOS ESPERADOS**

### **Taxa de Erro por Cenário:**
- 🟢 **Operação Normal**: 0-1% erros
- 🔴 **Com Falha**: 10-30% erros (temporário)
- 🟡 **Recuperação**: 5-15% erros (melhorando)
- 🟢 **Pós-Recuperação**: 0-1% erros

### **Logs do Sistema:**
```
✅ Sistema IoT Distribuído iniciado com sucesso!
📊 Arquitetura Final Implementada:
   🔸 Instâncias A: Sensores IoT (Stateless) - TEMPERATURA e UMIDADE
   🔸 Instâncias B: Data Receivers (Stateful) - 2+ receptores com persistência
   🔸 Gateway: Proxy roteando mensagens para Data Receivers
   🔸 Tolerância a Falhas: Recuperação automática e monitoramento
🛡️ Recursos de Tolerância a Falhas:
   🔸 Health Check automático a cada 5s
   🔸 Recuperação automática de instâncias falhas
   🔸 Criação automática de backup receivers
   🔸 Balanceamento de carga Round Robin
🧪 PRONTO PARA TESTES JMETER:
   🔸 Zero erros em operação normal
   🔸 Aumento de erros ao desligar instâncias
   🔸 Diminuição de erros na recuperação
```

---

## 🔧 **ESTRUTURA DO SISTEMA**

### **Portas Utilizadas:**
- **9090**: Gateway (Proxy) - Porta principal para JMeter
- **9091**: Data Receiver 1 (Stateful)
- **9092**: Data Receiver 2 (Stateful)
- **9093-9095**: Backup Receivers (Tolerância a Falhas)

### **Arquivos Principais:**
- `IoTDistributedSystem.java`: Aplicação principal
- `IoTGateway.java`: Singleton + Proxy Pattern
- `DataReceiver.java`: Instâncias B com persistência
- `FaultToleranceManager.java`: Sistema de recuperação
- `ReceiverStrategy.java`: Strategy Pattern
- `HeartbeatMonitor.java`: Observer Pattern

### **Logs e Resultados:**
- `logs/sistema-distribuido.log`: Logs detalhados do sistema
- `jmeter/results/`: Resultados dos testes JMeter
- `target/classes/`: Classes compiladas

---

## 🎯 **PONTOS-CHAVE PARA APRESENTAÇÃO**

### **1. Arquitetura Correta** ✅
- Instâncias A (Stateless) ✅
- Instâncias B (Stateful) ✅  
- Proxy Pattern no Gateway ✅

### **2. Padrões GoF Implementados** ✅
- Singleton (Gateway único) ✅
- Strategy (Seleção de receivers) ✅
- Observer (Monitoramento) ✅
- Proxy (Roteamento) ✅

### **3. Persistência e Conflitos** ✅
- In-memory ConcurrentHashMap ✅
- Last Write Wins ✅
- Version Vector ✅

### **4. Tolerância a Falhas** ✅
- Health Check automático ✅
- Recuperação automática ✅
- Backup receivers ✅

### **5. Validação JMeter** ✅
- Zero erros em operação normal ✅
- Aumento de erros com falhas ✅
- Recuperação demonstrável ✅

---

## 🏁 **CONCLUSÃO**

✅ **Sistema completo implementado com arquitetura distribuída correta**  
✅ **Todos os Padrões GoF solicitados validados e funcionando**  
✅ **Tolerância a falhas com recuperação automática demonstrável**  
✅ **Testes JMeter configurados para validação completa**  

🎯 **O sistema está pronto para apresentação e demonstra todos os conceitos de programação distribuída e padrões de projeto solicitados.**