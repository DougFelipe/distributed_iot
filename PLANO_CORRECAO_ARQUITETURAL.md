# 🚧 Plano de Correção Arquitetural - Sprint Atual

## 🎯 **Objetivo Prioritário**
Corrigir a arquitetura para implementar **Data Receivers** como Instâncias B (stateful) e modificar o Gateway para atuar como proxy real, garantindo que os testes UDP funcionem corretamente como base para outros protocolos.

---

## 📋 **FASE 1: Implementação dos Data Receivers**

### **1.1 Criar Data Receiver Component**
```java
// Arquivo: src/main/java/br/ufrn/dimap/components/DataReceiver.java
```

**Responsabilidades:**
- ✅ Receber dados roteados pelo Gateway via UDP
- ✅ Persistir dados dos sensores em estrutura local
- ✅ Manter Version Vector local distribuído
- ✅ Sincronizar estado com outros Data Receivers
- ✅ Responder com ACK para confirmação

### **1.2 Implementar Strategy para Receptores**
```java
// Arquivo: src/main/java/br/ufrn/dimap/patterns/strategy/ReceiverStrategy.java
// Arquivo: src/main/java/br/ufrn/dimap/patterns/strategy/UDPReceiverStrategy.java
```

**Funcionalidades:**
- Load balancing entre receptores
- Seleção de receptor baseada em critérios
- Failover automático

### **1.3 Criar Serviço de Persistência**
```java
// Arquivo: src/main/java/br/ufrn/dimap/persistence/IoTDataPersistence.java
```

**Características:**
- Armazenamento em memória (ConcurrentHashMap)
- Estruturas otimizadas para consulta
- Backup/restore de dados

---

## 📋 **FASE 2: Modificação do Gateway para Proxy Real**

### **2.1 Refatorar IoTGateway**
**Modificações em:** `src/main/java/br/ufrn/dimap/patterns/singleton/IoTGateway.java`

**Alterações Necessárias:**
- ❌ Remover processamento direto de dados dos sensores
- ✅ Implementar roteamento para Data Receivers
- ✅ Adicionar registro de Data Receivers
- ✅ Implementar load balancing
- ✅ Monitoramento de saúde dos receptores

### **2.2 Atualizar UDPCommunicationStrategy**
**Modificações em:** `src/main/java/br/ufrn/dimap/patterns/strategy/UDPCommunicationStrategy.java`

**Alterações Necessárias:**
- ✅ Configurar callback para roteamento (não processamento)
- ✅ Suporte a múltiplos destinos (receptores)
- ✅ Tratamento de falhas de receptores

---

## 📋 **FASE 3: Configuração do Sistema Distribuído**

### **3.1 Atualizar Aplicação Principal**
**Modificações em:** `src/main/java/br/ufrn/dimap/applications/IoTDistributedSystem.java`

**Alterações Necessárias:**
- ✅ Inicializar 2+ Data Receivers
- ✅ Configurar portas distintas para cada receptor
- ✅ Registrar receptores no Gateway
- ✅ Configurar Strategy Pattern para seleção

### **3.2 Configuração de Portas**
```
Gateway:        9090 (porta principal)
Data Receiver 1: 9091 (receptor primário)
Data Receiver 2: 9092 (receptor secundário)
```

---

## 📋 **FASE 4: Atualização dos Testes JMeter**

### **4.1 Manter Compatibilidade**
- ✅ JMeter continua enviando para Gateway (porta 9090)
- ✅ Gateway roteia para Data Receivers internamente
- ✅ Transparência total para JMeter

### **4.2 Validar Padrões GoF**
1. **Strategy Pattern:** Seleção de receptor
2. **Singleton Pattern:** Gateway único
3. **Observer Pattern:** Monitoramento de receptores  
4. **Proxy Pattern:** Roteamento transparente

---

## 🔧 **IMPLEMENTAÇÃO PASSO A PASSO**

### **Passo 1: Data Receiver (30 min)**
1. Criar classe `DataReceiver` com servidor UDP interno
2. Implementar persistência em memória
3. Adicionar Version Vector local
4. Testar recepção UDP básica

### **Passo 2: Receiver Strategy (20 min)**  
1. Criar interface `ReceiverStrategy`
2. Implementar `RoundRobinReceiverStrategy`
3. Integrar com Gateway
4. Testar seleção de receptores

### **Passo 3: Modificar Gateway (25 min)**
1. Refatorar `processIncomingMessage()` para `routeToReceiver()`
2. Adicionar lista de receptores registrados
3. Implementar load balancing
4. Configurar callback de roteamento

### **Passo 4: Integração Sistema (15 min)**
1. Inicializar múltiplos Data Receivers
2. Registrar receptores no Gateway
3. Configurar Strategy Pattern
4. Testar fluxo completo

### **Passo 5: Validação JMeter (10 min)**
1. Executar testes UDP existentes
2. Verificar logs de roteamento
3. Validar persistência nos receptores
4. Confirmar padrões GoF

---

## ✅ **Critérios de Sucesso**

### **Funcional:**
- ✅ JMeter envia mensagens para Gateway (9090)
- ✅ Gateway roteia para Data Receivers (9091, 9092)
- ✅ Receptores persistem dados e respondem com ACK
- ✅ Version Vector distribuído entre receptores
- ✅ Tolerância a falhas básica implementada

### **Arquitetural:**
- ✅ 3+ componentes distribuídos funcionais
- ✅ Instâncias A (sensores) stateless
- ✅ Instâncias B (receptores) stateful  
- ✅ Gateway como proxy real
- ✅ 4 padrões GoF validados

### **Testes:**
- ✅ Summary Report JMeter sem erros
- ✅ Logs estruturados com roteamento
- ✅ Dados persistidos nos receptores
- ✅ Monitoramento funcional

---

## 🕐 **Timeline Estimado**
**Total: ~1h30min** para implementação completa da correção arquitetural.

Esta correção garantirá que o sistema esteja alinhado com a especificação acadêmica antes de implementar HTTP e gRPC como próximos protocolos.