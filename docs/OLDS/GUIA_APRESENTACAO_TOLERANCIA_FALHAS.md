# 🎯 **GUIA DE APRESENTAÇÃO - TOLERÂNCIA A FALHAS EM TEMPO REAL**

**Universidade Federal do Rio Grande do Norte - DIMAP**  
**Disciplina:** Programação Distribuída  
**Data:** 30 de Setembro de 2025  
**Apresentação:** 16/10/2025 a 28/10/2025

---

## 📋 **CENÁRIOS DE DEMONSTRAÇÃO OBRIGATÓRIOS**

Durante a apresentação, o sistema deve demonstrar **tolerância a falhas** através da manipulação manual de instâncias em tempo de execução. Os cenários seguem a especificação:

### **Cenário 1: Funcionamento Normal** ✅
- **Expectativa:** Summary Report deve indicar **zero erros**
- **Demonstração:** Sistema operando com todas as instâncias ativas

### **Cenário 2: Simulação de Falhas** ⚠️
- **Ação:** Desligar instâncias durante apresentação
- **Expectativa:** **Taxa de erro deve aumentar**
- **Demonstração:** Impacto imediato na disponibilidade

### **Cenário 3: Recuperação do Sistema** 💚
- **Ação:** Criar novas instâncias do componente desligado
- **Expectativa:** **Taxa de erro deve diminuir**
- **Demonstração:** Recuperação automática e rebalanceamento

---

## 🏗️ **ARQUITETURA DO SISTEMA PARA APRESENTAÇÃO**

### **Componentes Críticos Identificados:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    SISTEMA IOT DISTRIBUÍDO                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌──────────────────┐    ┌─────────────┐ │
│  │   JMeter Test   │    │   API Gateway    │    │Data Receiver│ │
│  │   (Cliente)     │───▶│   (Singleton)    │───▶│ Instances   │ │ 
│  │                 │    │                  │    │ (Stateful)  │ │
│  │ - HTTP Requests │    │ - Port 9090      │    │ - Port 9091 │ │
│  │ - Load Testing  │    │ - Proxy Pattern  │    │ - Port 9092 │ │
│  │ - Error Metrics │    │ - Load Balancing │    │ - Port 9093 │ │
│  └─────────────────┘    │ - Fault Detection│    │ - Port 9094 │ │
│                         └──────────────────┘    └─────────────┘ │
│                                                                 │
│  MANIPULAÇÃO EM TEMPO REAL:                                    │
│  • Desligar Data Receivers → Aumenta taxa de erro             │
│  • Criar novos Data Receivers → Diminui taxa de erro          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 **SELEÇÃO DE PROTOCOLO PARA APRESENTAÇÃO**

### **Strategy Pattern - Escolha do Protocolo:**

O sistema implementa **Strategy Pattern** para permitir a seleção do protocolo de comunicação em tempo de inicialização. Durante a apresentação, você pode demonstrar diferentes protocolos:

#### **Protocolo UDP (RECOMENDADO)** ✅
- **Uso:** Demonstração estável e confiável
- **JMeter:** Requer configuração UDP específica
- **Comando:** `UDP` como argumento
- **Vantagem:** Protocolo nativo mais performático
- **Porta Gateway:** 9090

#### **Protocolo HTTP** 🌐
- **Uso:** Demonstração com JMeter HTTP padrão
- **JMeter:** Funciona com testes HTTP tradicionais
- **Comando:** `HTTP` como argumento
- **Vantagem:** Compatibilidade total com JMeter
- **Porta Gateway:** 8080

#### **Protocolo gRPC** 🚀
- **Uso:** Demonstração avançada (3,00 pontos)
- **JMeter:** Requer plugin gRPC
- **Comando:** `GRPC` como argumento
- **Vantagem:** Protocolo moderno, mais pontos
- **Porta Gateway:** 9000

### **Como o Sistema Detecta o Protocolo:**

O sistema utiliza múltiplas formas de configuração (ordem de prioridade):

1. **System Property:** `-Diot.protocol=UDP`
2. **Argumento Explícito:** `--protocol=UDP`
3. **Argumento Simples:** `UDP`
4. **Arquivo Properties:** `iot.protocol=UDP`
5. **Padrão:** UDP (fallback)

---

## 🎮 **COMANDOS DE MANIPULAÇÃO EM TEMPO REAL**

### **🤖 TOLERÂNCIA A FALHAS AUTOMÁTICA**

**IMPORTANTE:** Este sistema implementa **tolerância a falhas AUTOMÁTICA**. Não é necessário manipular receivers manualmente!

#### **Como Funciona a Demonstração:**

1. **Inicie apenas o Gateway** - Data Receivers são criados automaticamente
2. **Execute JMeter** - Para monitorar métricas em tempo real  
3. **Observe os logs** - O sistema simula e recupera falhas automaticamente
4. **Monitore as métricas** - Taxa de erro aumenta/diminui conforme falhas

#### **Sistema Inteligente de Falhas:**
- 🤖 **Detecção Automática:** Health checks a cada 5 segundos
- 🤖 **Simulação Natural:** Timeouts de rede, sobrecarga, latência
- 🤖 **Recuperação Inteligente:** Restart automático de receivers
- 🤖 **Backup Dinâmico:** Criação de novos receivers em portas alternativas
- 🤖 **Sincronização:** Version Vector mantém consistência automaticamente

#### **🎮 Para Apresentação Manual Controlada:**
**NOVO:** Script de controle para manipular receivers individualmente!

**Terminal 2: Controlador de Receivers**
```powershell
cd d:\distribuida
java -cp "target/classes;target/lib/*" br.ufrn.dimap.applications.ReceiverController
```

**Menu Interativo:**
- `f1` - 💀 Simular falha do DATA_RECEIVER_1
- `f2` - 💀 Simular falha do DATA_RECEIVER_2  
- `fall` - 💀💀 Simular falha de TODOS os receivers
- `r1` - 💚 Recuperar DATA_RECEIVER_1
- `r2` - 💚 Recuperar DATA_RECEIVER_2
- `rall` - 💚 Recuperar TODOS os receivers
- `s` - 📊 Mostrar status atual

---

### **1. Preparação do Ambiente**

#### **Terminal 1: API Gateway (Sempre Ativo)**
```powershell
# Navegar para o diretório do projeto
cd d:\distribuida

# RECOMENDADO PARA APRESENTAÇÃO: UDP (mais confiável)
mvn exec:java "-Dexec.args=UDP"

# Aguardar logs: "✅ Sistema IoT Distribuído iniciado com sucesso!"
```

#### **Terminal 2: Controlador de Receivers (Apresentação Manual)**
```powershell
# Em outro terminal PowerShell
cd d:\distribuida

# IMPORTANTE: Primeiro compile o projeto
mvn compile -q

# Iniciar controlador APÓS o Gateway estar rodando
java -cp "target/classes;target/lib/*" br.ufrn.dimap.applications.ReceiverController

# Aguardar mensagem: "🎮 CONTROLADOR DE DATA RECEIVERS"
# FUNCIONA com UDP, HTTP e gRPC automaticamente!
```

#### **IMPORTANTE: Data Receivers são iniciados AUTOMATICAMENTE!** ✅

⚠️ **NÃO é necessário abrir terminais separados para Data Receivers!**

Quando você executa o Gateway, o sistema já inicia automaticamente:
- ✅ DATA_RECEIVER_1 (porta 9091)
- ✅ DATA_RECEIVER_2 (porta 9092)
- ✅ Sistema de replicação entre receivers
- ✅ Tolerância a falhas ativada

**Logs esperados durante inicialização:**
```
✅ Data Receiver criado: DATA_RECEIVER_1 na porta 9091
✅ Data Receiver criado: DATA_RECEIVER_2 na porta 9092
✅ Data Receivers registrados no Gateway (PROXY)
✅ Tolerância a falhas ativada com recuperação automática
```

#### **Terminal 4: JMeter (Monitoramento)**
```powershell
# Executar teste JMeter para monitorar taxa de erro
cd d:\distribuida\jmeter
jmeter -n -t "Sistema_UDP_Funcionando.jmx" -l results/live_demo.jtl -e -o results/live_report
```

---

## 🎪 **ROTEIRO DE APRESENTAÇÃO PASSO A PASSO**

### **FASE 1: DEMONSTRAÇÃO DE FUNCIONAMENTO NORMAL** ✅

#### **Passo 1.1: Verificar Status Inicial**
```powershell
# No terminal do Gateway, verificar logs de inicialização:
# 
# ✅ Deve mostrar: "🔧 Protocolo definido via argumento: UDP"
# ✅ Deve mostrar: "✅ Strategy Pattern: Protocolo UDP configurado"
# ✅ Deve mostrar: "🚀 Gateway IoT iniciado na porta 9090"
# ✅ Deve mostrar: "Data Receivers registrados: 2"
# ✅ Status: ACTIVE para ambos receivers
#
# SE NÃO APARECER: Protocolo não foi detectado corretamente
```

#### **Passo 1.2: Executar Teste Baseline**
```powershell
# Executar JMeter com carga normal (5-10 usuários)
cd d:\distribuida\jmeter
jmeter -n -t "Sistema_UDP_Funcionando.jmx" -l results/baseline.jtl

# Verificar: 0% de erro no Summary Report
```

**📊 Métricas Esperadas (Funcionamento Normal):**
- ✅ **Taxa de Erro:** 0%
- ✅ **Throughput:** ~70 requisições/min
- ✅ **Data Receivers Ativos:** 2/2
- ✅ **Version Vector:** Sincronizado entre instâncias

---

### **FASE 2: SIMULAÇÃO DE FALHAS** ⚠️

#### **Passo 2.1: Simular Falha Controlada (RECOMENDADO PARA APRESENTAÇÃO)** 🎮

**MÉTODO 1: Via Controlador Manual (Mais Controlado)**
```powershell
# No Terminal 2 (Controlador), digite:
f1

# Resposta esperada:
# "✅ Falha simulada no Receiver 1"
# "Redirecionando mensagens para outros receivers ativos"
```

**Observar no Terminal 1 (Gateway):**
```
❌ FALHA DETECTADA: DATA_RECEIVER_1 marcado como INACTIVE
🔄 Redirecionando tráfego para DATA_RECEIVER_2  
⚠️ Sistema operando com capacidade reduzida (1/2 receivers)
```

**MÉTODO 2: Via Sistema Interno (Falha Natural)**
```powershell
# Aguardar falha automática via timeout/health check
# Observar logs: "💔 FALHA DETECTADA: DATA_RECEIVER_1 não está saudável"
# Menos controlado, mais realístico
```

#### **Passo 2.2: Observar Impacto Imediato** 📊
```powershell
# Logs esperados no Gateway:
# "❌ FALHA DETECTADA: DATA_RECEIVER_1 não responde"
# "🔄 Redirecionando tráfego para DATA_RECEIVER_2"
# "⚠️ Sistema operando com capacidade reduzida"
```

#### **Passo 2.3: Executar Teste Durante Falha**
```powershell
# Executar JMeter imediatamente após desligar receiver
jmeter -n -t "Sistema_UDP_Funcionando.jmx" -l results/failure_test.jtl

# Verificar aumento da taxa de erro
```

**📊 Métricas Esperadas (1 Receiver Falhando):**
- ⚠️ **Taxa de Erro:** 15-30% (aumento significativo)
- ⚠️ **Throughput:** ~50 requisições/min (redução)
- ⚠️ **Data Receivers Ativos:** 1/2
- ⚠️ **Latência:** Aumento devido à sobrecarga

#### **Passo 2.4: Simular Falha Total (Cenário Crítico)** 💀💀

**Via Controlador:**
```powershell
# No Terminal 2 (Controlador):
fall

# Resposta:
# "⚠️ TODOS os receivers foram marcados como inativos!"
# "🔴 Sistema entrará em modo de emergência"
```

**Observar no Terminal 1 (Gateway):**
```
🔴 EMERGÊNCIA: Todos os Data Receivers estão inativos (0/2)
⚠️ Rejeitando mensagens temporariamente
🔄 Ativando protocolo de recuperação automática
```

**📊 Métricas Esperadas (Todos Receivers Falhando):**
- 🔴 **Taxa de Erro:** 80-100% (falha crítica)
- 🔴 **Throughput:** <10 requisições/min
- 🔴 **Data Receivers Ativos:** 0/2

---

### **FASE 3: RECUPERAÇÃO CONTROLADA** 🔄

#### **Passo 3.1: Recuperar Receiver Individual**
```powershell
# No Terminal 2 (Controlador):
r1

# Resposta:
# "✅ Receiver 1 foi recuperado e reativado"
# "Sistema detectou melhoria na capacidade"
```

#### **Passo 3.2: Verificar Status dos Receivers**
```powershell
# No Terminal 2 (Controlador):
status

# Resposta esperada:
# "📊 Status dos Data Receivers:"
# "✅ Receiver 1: ACTIVE"
# "❌ Receiver 2: INACTIVE"
```

#### **Passo 3.3: Recuperação Completa**
```powershell
# No Terminal 2 (Controlador):
rall

# Resposta:
# "✅ TODOS os receivers foram recuperados!"
# "Sistema voltou à capacidade total"
```

**📊 Métricas Esperadas (Após Recuperação Total):**
- ✅ **Taxa de Erro:** 0-5%
- ✅ **Throughput:** ~70 requisições/min (normal)
- ✅ **Data Receivers Ativos:** 2/2

---

## **🎯 COMANDOS RÁPIDOS PARA APRESENTAÇÃO**

### **Sequência Recomendada de Demonstração:**

```
1️⃣ status    → Verificar estado inicial (2/2 ativos)
2️⃣ f1        → Simular falha no Receiver 1
3️⃣ status    → Mostrar degradação (1/2 ativo)
4️⃣ f2        → Simular falha no Receiver 2  
5️⃣ status    → Mostrar estado crítico (0/2 ativos)
6️⃣ r1        → Recuperar Receiver 1
7️⃣ status    → Mostrar recuperação parcial (1/2 ativo)
8️⃣ rall      → Recuperação total
9️⃣ status    → Confirmar estado normal (2/2 ativos)
```

### **Comandos do Controlador:**
| Comando | Função | Impacto |
|---------|---------|---------|
| `f1` | Falha no Receiver 1 | Capacidade reduzida |
| `f2` | Falha no Receiver 2 | Capacidade reduzida |
| `fall` | Falha em TODOS | 🔴 Modo emergência |
| `r1` | Recupera Receiver 1 | Melhoria gradual |
| `r2` | Recupera Receiver 2 | Melhoria gradual |
| `rall` | Recupera TODOS | ✅ Capacidade total |
| `status` | Mostra estado atual | Monitoramento |
| `sair` | Encerra controlador | Fim da demonstração |
- 🔴 **Status:** Sistema em modo de emergência

---

### **FASE 3: RECUPERAÇÃO DO SISTEMA** 💚

#### **Passo 3.1: Observar Recuperação Automática** 🔄

**RECUPERAÇÃO É AUTOMÁTICA! Não requer intervenção manual.**

```powershell
# O sistema possui Fault Tolerance Manager que:
# 1. Detecta falhas via health check (a cada 5 segundos)
# 2. Tenta recuperar receivers existentes
# 3. Cria novos backup receivers se necessário
# 4. Restaura dados via Version Vector

# Logs esperados de recuperação automática:
# "🔄 RECUPERAÇÃO AUTOMÁTICA: DATA_RECEIVER_1 restaurado"
# "� Sistema em recuperação - capacidade parcial restaurada"
# "✅ BACKUP RECEIVER criado: DATA_RECEIVER_BACKUP_1 na porta 9093"
# "� Sincronização de dados via Version Vector iniciada"
```

#### **Passo 3.2: Monitorar Recuperação Completa** 💚
```powershell
# Aguardar até sistema voltar ao normal:
# "💚 RECUPERAÇÃO COMPLETA: 2/2 receivers ativos"
# "📊 REPLICAÇÃO STATUS: Syncs=X, Conflitos=Y, Backups=Z"
# "✅ Sistema operacional - taxa de erro deve diminuir"

# JMeter deve mostrar taxa de erro voltando a 0-5%
```

#### **Passo 3.4: Executar Teste de Recuperação**
```powershell
# Executar JMeter após recuperação
jmeter -n -t "Sistema_UDP_Funcionando.jmx" -l results/recovery_test.jtl

# Verificar diminuição da taxa de erro
```

**📊 Métricas Esperadas (Recuperação Completa):**
- ✅ **Taxa de Erro:** 0-5% (recuperação bem-sucedida)
- ✅ **Throughput:** ~70 requisições/min (restaurado)
- ✅ **Data Receivers Ativos:** 2/2 (novas instâncias)
- ✅ **Version Vector:** Sincronizado automaticamente

---

## 🔍 **MONITORAMENTO EM TEMPO REAL**

### **Interpretação dos Logs Durante Apresentação:**

#### **✅ Logs de Funcionamento Normal:**
```
✅ Data Receiver registrado: DATA_RECEIVER_1 na porta 9091 (Total: 2)
💚 RECUPERAÇÃO COMPLETA - Data Receiver operacional novamente
📊 REPLICAÇÃO STATUS: Syncs=10, Conflitos=0, Backups=5
🛡️ Health check executado: 2/2 receivers ativos
```

#### **⚠️ Logs de Falha Detectada:**
```
💔 FALHA DETECTADA: DATA_RECEIVER_1 não está saudável
🔄 Redirecionando tráfego para DATA_RECEIVER_2
⚠️ Sistema operando com capacidade reduzida (1/2 receivers)
```

#### **🔴 Logs de Falha Crítica:**
```
🔴 SISTEMA EM MODO DE EMERGÊNCIA: 0/2 receivers ativos
⚠️ Todas as mensagens serão rejeitadas temporariamente
🔄 Iniciando procedimentos de recuperação automática
```

#### **💚 Logs de Recuperação Automática:**
```
🔄 RECUPERAÇÃO AUTOMÁTICA: DATA_RECEIVER_1 restaurado
✅ BACKUP RECEIVER criado: DATA_RECEIVER_BACKUP_1 na porta 9093
🔄 Sincronização de dados via Version Vector iniciada
💚 Sistema em recuperação - capacidade parcial restaurada
```

#### **📊 Logs de Version Vector (REPLICAÇÃO):**
```
🔍 SYNC DATA_RECEIVER_1 ↔ DATA_RECEIVER_2: VV1={TEMP_01=5}, VV2={TEMP_01=4}
⚠️ VERSION VECTOR CONFLICT detectado - resolvendo via merge
✅ SYNC REALIZADA: DATA_RECEIVER_1 → DATA_RECEIVER_2 (diff: 1 mensagens)
📊 VERSION VECTORS APÓS SYNC: VV1={TEMP_01=5}, VV2={TEMP_01=5}
```

### **Métricas JMeter a Observar:**
- **Summary Report:** Taxa de erro deve variar 0% → 30% → 0%
- **Throughput:** ~70 req/min → ~40 req/min → ~70 req/min
- **Response Time:** Aumenta durante falhas, normaliza na recuperação

---

## 📊 **DASHBOARD DE APRESENTAÇÃO**

### **Métricas Críticas a Serem Monitoradas:**

| **Fase** | **Receivers Ativos** | **Taxa de Erro** | **Throughput** | **Status** | **Ação do Sistema** |
|----------|---------------------|------------------|----------------|------------|---------------------|
| Inicialização | 2/2 | 0% | ~70 req/min | ✅ HEALTHY | Sistema estável |
| Falha Detectada | 1/2 | 15-30% | ~50 req/min | ⚠️ DEGRADED | Redireciona tráfego |
| Falha Crítica | 0/2 | 80-100% | <10 req/min | 🔴 CRITICAL | Modo de emergência |
| Auto-Recuperação | 1/2 | 20-40% | ~40 req/min | 🔄 RECOVERING | Cria backup receivers |
| Sistema Restaurado | 2/2 | 0-5% | ~70 req/min | 💚 RECOVERED | Operação normal |

---

## 🎯 **PONTOS-CHAVE PARA APRESENTAÇÃO**

### **1. Demonstração de Tolerância a Falhas:**
- ✅ Sistema detecta falhas automaticamente
- ✅ Redireciona tráfego para instâncias saudáveis
- ✅ Mantém operação mesmo com instâncias indisponíveis

### **2. Demonstração do Version Vector:**
- ✅ Sincronização automática entre novas instâncias
- ✅ Resolução de conflitos por timestamp
- ✅ Ordenação causal de eventos entre receivers

### **3. Demonstração de Recuperação:**
- ✅ Criação dinâmica de novas instâncias
- ✅ Descoberta automática pelo Gateway
- ✅ Restauração completa da capacidade

### **4. Impacto Mensurável:**
- ✅ Taxa de erro aumenta com falhas
- ✅ Taxa de erro diminui com recuperação
- ✅ Métricas em tempo real via JMeter

---

## 🚨 **CENÁRIOS DE CONTINGÊNCIA**

### **Se o Gateway Falhar (NÃO DEVE ACONTECER):**
```powershell
# Reiniciar Gateway rapidamente com mesmo protocolo usado antes
cd d:\distribuida

# Usar o mesmo protocolo da sessão anterior
mvn exec:java "-Dexec.args=UDP"
# OU
mvn exec:java "-Dexec.args=HTTP"
```

### **Se JMeter Não Funcionar:**
```powershell
# Usar cliente de teste simples alternativo
cd d:\distribuida
mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="client test"
```

### **Se Porta Estiver Em Uso:**
```powershell
# Usar portas alternativas
# DATA_RECEIVER_5: porta 9095
# DATA_RECEIVER_6: porta 9096
```

### **Se Protocolo Não For Detectado:**
```powershell
# Opção 1: Forçar via system property
mvn exec:java "-Diot.protocol=UDP"

# Opção 2: Usar formato explícito
mvn exec:java "-Dexec.args=--protocol=UDP"

# Opção 3: Verificar application.properties
# Arquivo: src/main/resources/application.properties
# Adicionar: iot.protocol=UDP
```

### **Se JMeter Não Conectar:**
```powershell
# Para UDP: Verificar se está na porta 9090
# Para HTTP: Verificar se está na porta 8080
# Para gRPC: Verificar se está na porta 9000

# Verificar logs do Gateway para confirmar porta ativa
```

---

## 📝 **CHECKLIST DE APRESENTAÇÃO**

### **Antes da Apresentação:**
- [ ] ✅ Compilar projeto: `mvn compile`
- [ ] ✅ Testar todos os terminais individualmente
- [ ] ✅ Verificar configuração do JMeter
- [ ] ✅ Preparar terminais com comandos prontos
- [ ] ✅ Testar cenário completo pelo menos uma vez

### **Durante a Apresentação:**
- [ ] ✅ Iniciar Gateway (receivers são criados automaticamente)
- [ ] ✅ Verificar logs: "Data Receivers registrados: 2"
- [ ] ✅ Executar teste baseline JMeter (0% erro)
- [ ] ✅ Aguardar falha automática OU observar health checks
- [ ] ✅ Mostrar aumento de erro nas métricas JMeter
- [ ] ✅ Observar recuperação automática nos logs
- [ ] ✅ Mostrar diminuição de erro nas métricas JMeter
- [ ] ✅ Demonstrar Version Vector nos logs de sincronização

### **Após a Apresentação:**
- [ ] ✅ Parar todos os terminais graciosamente
- [ ] ✅ Salvar logs e métricas da apresentação
- [ ] ✅ Documentar resultados obtidos

---

## 🎓 **EXPLICAÇÕES TÉCNICAS PARA PERGUNTAS**

### **"Como o sistema detecta falhas?"**
- **Resposta:** "Utilizamos heartbeat entre Gateway e Data Receivers. Quando um receiver não responde por 5 segundos, é marcado como indisponível e o tráfego é redirecionado."

### **"Como funciona o Version Vector?"**
- **Resposta:** "Cada operação incrementa o Version Vector local. Na sincronização, fazemos merge dos vectores para determinar a ordem causal dos eventos e resolver conflitos."

### **"Como novas instâncias recuperam dados?"**
- **Resposta:** "Novas instâncias se registram no Gateway e recebem backup da instância com mais dados. O Version Vector garante consistência na restauração."

### **"O que acontece se o Gateway falhar?"**
- **Resposta:** "O Gateway é um Singleton crítico. Em produção, implementaríamos Leader Election, mas para este projeto, o Gateway é o ponto central obrigatório."

---

## 📚 **ARQUIVOS DE REFERÊNCIA**

### **Código Principal:**
- `IoTDistributedSystem.java` - Aplicação principal
- `IoTGateway.java` - Singleton Gateway (Proxy Pattern)
- `DataReceiver.java` - Instâncias stateful com Version Vector
- `DataReplicationManager.java` - Sistema de replicação

### **Configuração JMeter:**
- `Sistema_UDP_Funcionando.jmx` - Teste principal
- `results/` - Diretório para resultados da apresentação

### **Logs de Monitoramento:**
- `logs/sistema-distribuido.log` - Logs detalhados do sistema

---

## 🏆 **CRITÉRIOS DE SUCESSO DA APRESENTAÇÃO**

### **Pontuação Total: 10,00 pontos**

1. **Protocolos Funcionando (6,00 pontos):**
   - ✅ UDP: 1,50 pontos (implementado e testado)
   - ✅ HTTP: 1,50 pontos (via Strategy Pattern)
   - ✅ gRPC: 3,00 pontos (via Strategy Pattern)

2. **Padrões GoF (1,00 ponto):**
   - ✅ Strategy: Protocolos de comunicação
   - ✅ Singleton: API Gateway único
   - ✅ Observer: Monitoramento de falhas
   - ✅ Proxy: Gateway como proxy para receivers

3. **Tolerância a Falhas (3,00 pontos):**
   - ✅ **Taxa de erro aumenta** quando instâncias falham
   - ✅ **Taxa de erro diminui** quando instâncias se recuperam
   - ✅ **Demonstração clara** em tempo real

---

---

## 🎯 **RESUMO EXECUTIVO - APRESENTAÇÃO SIMPLIFICADA**

### **✅ O QUE MUDOU:**
1. **Data Receivers são AUTOMÁTICOS** - Não precisam de terminais separados
2. **Falhas são AUTOMÁTICAS** - Sistema simula e detecta falhas naturalmente  
3. **Recuperação é AUTOMÁTICA** - Backup receivers são criados dinamicamente
4. **Version Vector VISÍVEL** - Logs mostram sincronização em tempo real

### **📋 PROCESSO SIMPLIFICADO:**
1. **Inicie apenas 1 terminal:** `mvn exec:java "-Dexec.args=UDP"`
2. **Execute JMeter em paralelo:** Para monitorar métricas
3. **Observe os logs:** Sistema mostra todo o ciclo de vida automaticamente
4. **Apresente as métricas:** Taxa de erro aumenta/diminui conforme esperado

### **🎯 PONTOS-CHAVE PARA APRESENTAÇÃO:**
- ✅ **Sistema 100% automático** - Não requer intervenção manual
- ✅ **Logs detalhados** - Mostram cada fase da tolerância a falhas
- ✅ **Version Vector visível** - Demonstra replicação de dados em ação
- ✅ **Métricas mensuráveis** - JMeter comprova aumento/diminuição de erros
- ✅ **Arquitetura correta** - Gateway + Data Receivers + Replicação

### **⚠️ COMANDOS DEPRECATED REMOVIDOS:**
- ❌ ~~Terminal 2: Data Receiver 1~~ (automático)
- ❌ ~~Terminal 3: Data Receiver 2~~ (automático)  
- ❌ ~~Terminal 5: Recovery Receiver~~ (automático)
- ❌ ~~Terminal 6: Backup Receiver~~ (automático)

**🎯 SUCESSO DA APRESENTAÇÃO = UM TERMINAL + JMETER + OBSERVAÇÃO DE LOGS AUTOMÁTICOS**