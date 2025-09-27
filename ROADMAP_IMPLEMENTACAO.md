# Roadmap de Implementação - Sistema Distribuído Tolerante a Falhas

## Visão Geral do Projeto

**Instituição:** Universidade Federal do Rio Grande do Norte - DIMAP  
**Disciplina:** Programação Distribuída  
**Desenvolvimento:** Individual

**Objetivo:** Desenvolver um sistema distribuído IoT tolerante a falhas com sensores simulados, implementando Version Vector para ordenação causal e múltiplos protocolos de comunicação.

**Tema Escolhido:** Sistema IoT Distribuído com Sensores e Version Vector
- **Padrão Distribuído:** Version Vector para ordenação causal de eventos
- **Domínio:** Internet of Things (IoT) com sensores simulados
- **Tolerância a Falhas:** Detecção e recuperação automática de sensores

**Requisitos Específicos Implementados:**
- ✅ Sistema distribuído com sensores IoT e coordenador central
- ✅ Version Vector implementado para ordenação causal
- ✅ Mínimo 3 componentes distribuídos (API Gateway + Sensor Manager + Múltiplos Sensores)
- ✅ API Gateway como coordenador central obrigatório
- ✅ Múltiplas instâncias de sensores (5 tipos diferentes)
- ✅ Comunicação nativa UDP funcional

## Arquitetura do Sistema

### Configuração Mínima (conforme especificação):
- **Mínimo 3 componentes distribuídos**
- **API Gateway obrigatório** como um dos componentes
- **Mínimo 2 instâncias** para componentes stateless
- **Replicação obrigatória** para componentes stateful

### Componentes Principais Reimplementados:
1. **API Gateway IoT** (Singleton - Obrigatório)
   - Ponto único de entrada para requisições do JMeter
   - Coordenador central do sistema IoT distribuído
   - Registro e descoberta dinâmica de sensores IoT
   - Proxy para roteamento de requisições aos sensores
   - Monitoramento via Heartbeat de todos os sensores
   - Manutenção do Version Vector global do sistema

2. **IoT Sensor Manager** (Observer Subject)
   - Gerenciamento centralizado de sensores distribuídos
   - Implementação do padrão Observer para monitoramento
   - Notificação de mudanças de status dos sensores
   - Coordenação da coleta de dados distribuída
   - Replicação de estado crítico dos sensores

3. **IoT Sensors Distribuídos** (Múltiplas Instâncias)
   - **Sensor de Temperatura** (Instâncias 1-N)
   - **Sensor de Umidade** (Instâncias 1-N)  
   - **Sensor de Pressão** (Instâncias 1-N)
   - **Sensor de Luminosidade** (Instâncias 1-N)
   - **Sensor de Movimento** (Instâncias 1-N)
   - Cada sensor mantém Version Vector individual
   - Comunicação distribuída com coordenador central

### Protocolos de Comunicação Suportados:
- **UDP Nativo:** ✅ Implementado e funcional (serialização Java)
- **TCP com HTTP:** 🔄 A implementar via Strategy Pattern
- **gRPC:** 🔄 A implementar via Strategy Pattern
- **Strategy Pattern:** Seleção de protocolo em tempo de execução
- **Compatibilidade:** JMeter via HTTP, Produção via UDP nativo

### Padrões GoF Obrigatórios (ADAPTADOS PARA IoT):
- **Strategy:** 🔄 Escolha de protocolo de comunicação IoT (UDP/HTTP/gRPC)
- **Observer:** 🔄 Monitoramento de sensores via heartbeat e notificações de eventos
- **Singleton:** ✅ API Gateway IoT como coordenador único do sistema
- **Proxy:** 🔄 Gateway como proxy para acesso aos sensores distribuídos

### Padrões de Sistemas Distribuídos Implementados:
- ✅ **Version Vector:** Ordenação causal de eventos entre sensores
- ✅ **Heartbeat:** Detecção de falhas de sensores
- ✅ **Leader Election:** API Gateway como líder do sistema IoT
- ✅ **Service Discovery:** Registro dinâmico de sensores

### Padrões de Sistemas Distribuídos:
- Implementação baseada em "Patterns of Distributed Systems" (Addison-Wesley, 2024)
- **Tolerância a Falhas:** Sistema deve ser resiliente a falhas de rede/componentes

### Fluxo de Execução IoT Distribuído:
1. **API Gateway IoT** inicializa como Singleton (coordenador único)
2. **Sensores IoT** se registram dinamicamente no Gateway (Service Discovery)
3. **JMeter** envia requisições para o **API Gateway** via HTTP
4. **Gateway** atua como **Proxy**, roteando requisições aos sensores via Strategy Pattern
5. **Sensores** enviam dados periodicamente com **Version Vector** atualizado
6. **Observer Pattern:** Gateway monitora heartbeat e notifica mudanças de status
7. **Version Vector Global** mantém ordenação causal de todos os eventos IoT
8. **Tolerância a Falhas:** Detecção automática e recuperação de sensores falhos

### Critérios de Avaliação:
- **Implementação dos protocolos:** UDP (1,50), TCP com HTTP (1,50), gRPC (3,00) - Total: 6,00 pontos
- **Implementação dos padrões GoF:** 1,00 ponto  
- **Execução com Tolerância a Falhas:** 3,00 pontos
- **Testes JMeter:** Configuração com usuários simultâneos > 5 e < Knee Capacity

### Cenários de Teste Obrigatórios:
1. **Funcionamento Normal:** Summary Report deve indicar zero erros
2. **Simulação de Falhas:** Desligar instâncias durante apresentação, taxa de erro deve aumentar
3. **Recuperação:** Criar novas instâncias, taxa de erro deve diminuir
4. **Apresentação:** Explicar qualquer parte do código durante apresentação

**Data de Apresentação:** 16/10/2025 a 28/10/2025

---

## Sprint 1: ✅ COMPLETO - Base IoT UDP Nativa Funcional
**Status:** ✅ **CONCLUÍDO**  
**Resultado:** Sistema IoT UDP nativo totalmente funcional com Version Vector

### Implementações Concluídas:
1. **✅ Configuração do Projeto IoT**
   - ✅ Estrutura Maven configurada para IoT
   - ✅ Dependências para UDP nativo e serialização Java
   - ✅ Sistema de logging profissional (SLF4J)
   - ✅ Build system funcional

2. **✅ Sistema IoT Nativo Implementado**
   - ✅ **IoTMessage:** Mensagens com Version Vector
   - ✅ **IoTSensor:** 5 tipos de sensores simulados
   - ✅ **NativeUDPIoTServer:** Servidor UDP nativo
   - ✅ **NativeUDPIoTClient:** Cliente UDP para sensores
   - ✅ **NativeIoTServerApplication:** Aplicação principal

3. **✅ Version Vector e Comunicação**
   - ✅ Version Vector completo para ordenação causal
   - ✅ Serialização nativa Java via UDP
   - ✅ Comunicação assíncrona entre sensores
   - ✅ Thread-safety com ConcurrentHashMap

4. **✅ Validação e Testes**
   - ✅ Sistema executando com 0% de erros
   - ✅ 70 mensagens processadas em 1 minuto
   - ✅ 5 sensores IoT funcionais
   - ✅ Logs profissionais estruturados

### Status Atual:
- ✅ **Base sólida funcionando perfeitamente**
- 🔄 **Próximo:** Implementar padrões GoF obrigatórios

---

## Sprint 2: ✅ COMPLETO - Padrões GoF para IoT (1,00 ponto)
**Status:** ✅ **CONCLUÍDO**  
**Resultado:** Todos os 4 padrões GoF implementados no sistema IoT reativo

### Implementações Concluídas:
1. **✅ Singleton Pattern - API Gateway**
   - ✅ `IoTGateway` implementado como Singleton
   - ✅ Ponto único de acesso ao sistema IoT
   - ✅ Instância única do coordenador garantida
   - ✅ Interface unificada para gerenciamento

2. **✅ Strategy Pattern - Protocolos de Comunicação**
   - ✅ Interface `CommunicationStrategy` criada
   - ✅ `UDPCommunicationStrategy` implementada e funcional
   - ✅ Arquitetura preparada para HTTP e gRPC
   - ✅ Seleção dinâmica de protocolo implementada

3. **✅ Observer Pattern - Monitoramento IoT**
   - ✅ Interface `IoTObserver` implementada
   - ✅ `HeartbeatMonitor` funcionando como Observer
   - ✅ Gateway observa mudanças de status dos sensores
   - ✅ Notificações automáticas de eventos IoT ativas

4. **✅ Proxy Pattern - Gateway como Proxy**
   - ✅ Gateway atua como proxy para sensores
   - ✅ Roteamento inteligente de requisições implementado
   - ✅ Processamento centralizado de mensagens
   - ✅ Controle de acesso via Gateway único

### Melhorias Implementadas:
- ✅ **Sistema Reativo:** Inicia vazio, sensores criados via JMeter
- ✅ **Nomenclatura Descritiva:** TEMP_SENSOR_01, HUMIDITY_SENSOR_01, etc.
- ✅ **Logs Detalhados:** Códigos numéricos, valores, timestamps
- ✅ **Arquitetura Limpa:** Removidos arquivos deprecated

### Status Atual:
- ✅ **Sistema funcional:** 0% erro, pronto para JMeter
- ✅ **Padrões GoF:** Todos implementados e validados
- ✅ **Arquitetura distribuída:** Gateway + Sensores dinâmicos
- ✅ **Próximo:** Implementar HTTP Strategy Pattern

---

## Sprint 3: 🔄 HTTP Strategy Pattern (1,50 pontos)
**Duração:** 1 semana  
**Objetivo:** Implementar protocolo HTTP via Strategy Pattern mantendo funcionalidade IoT

### Tarefas:
1. **🔄 HTTPCommunicationStrategy**
   - Implementar HTTPCommunicationStrategy seguindo interface Strategy
   - Servidor HTTP para receber requisições JMeter
   - Adaptação das mensagens IoT para formato HTTP/JSON
   - Manter compatibilidade com sistema UDP existente

2. **🔄 API Gateway HTTP**
   - Gateway como Singleton recebe requisições HTTP do JMeter
   - Proxy HTTP para acessar sensores IoT
   - Conversão HTTP ↔ UDP transparente
   - Endpoints REST para operações IoT

3. **🔄 Integração HTTP + UDP**
   - JMeter → HTTP → Gateway → UDP → Sensores
   - Resposta: Sensores → UDP → Gateway → HTTP → JMeter
   - Strategy Pattern permite escolha UDP ou HTTP no startup
   - Mesma lógica IoT, protocolos diferentes

4. **🔄 Testes de Compatibilidade**
   - Validar que funcionalidade IoT permanece inalterada
   - Testes JMeter via HTTP
   - Comparação UDP vs HTTP performance
   - Métricas de tempo de resposta

### Entregáveis:
- 🔄 Sistema dual UDP (produção) + HTTP (JMeter)
- 🔄 Strategy Pattern funcional para protocolos
- 🔄 Compatibilidade total com JMeter
- ✅ Funcionalidade IoT preservada

---

## Sprint 4: 🔄 gRPC Strategy Pattern (3,00 pontos)
**Duração:** 2 semanas  
**Objetivo:** Implementar protocolo gRPC completando Strategy Pattern

### Tarefas:
1. **🔄 gRPCCommunicationStrategy**
   - Definir arquivos .proto para mensagens IoT
   - Implementar gRPCCommunicationStrategy
   - Gerar classes Java para IoTMessage e IoTSensor
   - Integração completa com Strategy Pattern

2. **🔄 API Gateway gRPC**
   - Servidor gRPC no Gateway para requisições
   - Streaming bidirecional para dados IoT
   - Adaptação Version Vector para Protobuf
   - Proxy gRPC para sensores distribuídos

3. **🔄 Sensores IoT com gRPC**
   - Cliente gRPC para comunicação com Gateway
   - Streaming de dados de sensores em tempo real
   - Version Vector em formato Protobuf
   - Heartbeat via gRPC streaming

4. **🔄 Strategy Pattern Completo**
   - 3 estratégias: UDP, HTTP, gRPC
   - Seleção via parâmetro: --protocol=udp|http|grpc
   - Mesma funcionalidade IoT em todos os protocolos
   - Testes de intercambiabilidade completos

### Entregáveis:
- 🔄 Strategy Pattern completo com 3 protocolos
- 🔄 Sistema IoT funcional em UDP/HTTP/gRPC
- 🔄 Arquivos .proto para comunicação
- ✅ Funcionalidade Version Vector preservada

---

## Sprint 5: Implementação gRPC (3,00 pontos)
**Duração:** 2 semanas  
**Objetivo:** Implementar protocolo gRPC completando Strategy Pattern

### Tarefas:
1. **Strategy Pattern para gRPC**
   - Definir arquivos .proto para comunicação
   - Implementar gRPCCommunicationStrategy
   - Gerar classes Java a partir dos .proto files
   - Integração completa com Strategy Pattern existente

2. **API Gateway com gRPC**
   - Implementar servidor gRPC no Gateway
   - Adaptação do sistema de descoberta para gRPC
   - Roteamento gRPC para componentes distribuídos
   - Streaming bidirecional para heartbeat

3. **Componentes A e B com gRPC**
   - Implementar serviços gRPC nos componentes
   - Cliente gRPC para comunicação com Gateway
   - Sistema de registro via gRPC
   - Aproveitamento de features avançadas do gRPC

4. **Validação do Strategy Pattern Completo**
   - Testes com todos os protocolos (UDP, HTTP, gRPC)
   - Seleção de protocolo via parâmetro de startup
   - Validação de que lógica permanece inalterada
   - Documentação das diferenças de implementação

### Entregáveis:
- Sistema de replicação funcional
- Algoritmos de consistência implementados
- Resolução de conflitos automática
- Testes de consistência completos

---

## Sprint 6: Implementação de Tolerância a Falhas (3,00 pontos)
**Duração:** 2 semanas  
**Objetivo:** Implementar tolerância completa a falhas conforme especificação

### Tarefas:
1. **Replicação de Dados (para componentes stateful)**
   - Identificar componentes que precisam de replicação
   - Implementar sincronização de estado entre instâncias
   - Sistema de backup automático de dados críticos
   - Recuperação de dados após falhas

2. **Recuperação Automática do Sistema**
   - Detecção automática de componentes falhos
   - Redistribuição de carga para componentes ativos
   - Re-roteamento automático pelo API Gateway
   - Recuperação graceful de componentes

3. **Cenários de Teste de Falhas**
   - Simulação de falha individual de componentes
   - Teste de recuperação com nova instância
   - Validação de que taxa de erro diminui após recuperação
   - Teste de múltiplas falhas simultâneas

4. **Monitoramento e Métricas**
   - Métricas de disponibilidade do sistema
   - Tempo de detecção e recuperação de falhas
   - Taxa de sucesso durante falhas
   - Logs detalhados para análise post-mortem

### Entregáveis:
- Simulador de sensores funcional
- Cliente de teste implementado
- Resultados de testes básicos
- Relatório de validação funcional

---

## Sprint 7: Configuração JMeter e Testes de Performance (Obrigatório)
**Duração:** 1 semana  
**Objetivo:** Configurar JMeter conforme especificações para apresentação

### Tarefas:
1. **Configuração JMeter Específica**
   - Configurar número de usuários simultâneos > 5 e < Knee Capacity
   - Criar samplers para todos os protocolos (UDP, TCP/HTTP, gRPC)
   - Configurar Summary Report para apresentação
   - Preparar cenários para demonstração ao vivo

2. **Cenários de Teste Obrigatórios**
   - **Cenário 1:** Funcionamento normal - Summary Report com zero erros
   - **Cenário 2:** Simulação de falhas - desligar instâncias durante teste
   - **Cenário 3:** Recuperação - criar novas instâncias e validar recuperação
   - Todos os testes prontos e configurados antecipadamente

3. **Determinação de Capacidades**
   - Identificar Knee Capacity do sistema
   - Determinar Usable Capacity para operação normal
   - Métricas de throughput, tempo de resposta e taxa de erro
   - Documentar limites de cada protocolo

4. **Preparação para Apresentação**
   - Testes configurados e validados previamente
   - Scripts automatizados para demonstração
   - Documentação das métricas obtidas
   - Preparação para explicar qualquer parte do código

### Entregáveis:
- Configuração JMeter completa
- Resultados de testes de performance
- Relatório de capacidade do sistema
- Otimizações implementadas

---

## Sprint 8: Validação Final e Refinamentos
**Duração:** 1 semana  
**Objetivo:** Validação final de todos os requisitos e refinamentos

### Tarefas:
1. **Validação dos Padrões GoF (1,00 ponto)**
   - Verificar implementação correta do Strategy Pattern
   - Validar Observer Pattern no sistema de Heartbeat
   - Confirmar Singleton no API Gateway
   - Verificar Proxy Pattern no roteamento

2. **Testes Integrados de Todos os Protocolos**
   - Validar funcionamento correto de UDP (1,50 pontos)
   - Validar funcionamento correto de TCP/HTTP (1,50 pontos)
   - Validar funcionamento correto de gRPC (3,00 pontos)
   - Teste de mudança de protocolo via startup parameter

3. **Validação Final de Tolerância a Falhas (3,00 pontos)**
   - Teste completo de recuperação automática
   - Validação de replicação para componentes stateful
   - Teste de múltiplas falhas e recuperações
   - Medição de tempo de recuperação

4. **Preparação para Apresentação Final**
   - Documentação completa do sistema
   - Roteiro de demonstração preparado
   - Validação de que todos os pontos (10,00) estão implementados
   - Preparação para perguntas sobre qualquer parte do código

### Entregáveis:
- Arquitetura modular refatorada
- Interfaces protocol-agnostic
- Testes de arquitetura
- Documentação técnica completa

---

## Sprint 9: Preparação Final para Apresentação
**Duração:** 1 semana  
**Objetivo:** Preparação final para apresentação (16/10/2025 a 28/10/2025)

### Tarefas:
1. **Documentação Final do Sistema**
   - Manual completo de instalação e execução
   - Documentação dos padrões implementados
   - Explicação da arquitetura distribuída
   - Guia de configuração para cada protocolo

2. **Scripts de Demonstração**
   - Scripts automatizados para startup de todos os componentes
   - Configuração JMeter pronta para apresentação
   - Cenários de demonstração testados e validados
   - Backup de configurações funcionais

3. **Preparação para Avaliação Oral**
   - Estudo detalhado de todo o código implementado
   - Preparação para explicar qualquer parte do sistema
   - Simulação de cenários de falha e recuperação
   - Domínio completo dos padrões GoF utilizados

4. **Validação Final de Pontuação**
   - ✅ Protocolos: UDP (1,50) + TCP/HTTP (1,50) + gRPC (3,00) = 6,00
   - ✅ Padrões GoF implementados = 1,00  
   - ✅ Tolerância a Falhas funcional = 3,00
   - ✅ **Total: 10,00 pontos**

### Entregáveis:
- Documentação completa do projeto
- Sistema totalmente funcional
- Material de apresentação
- Relatório final de resultados

---

## Cronograma Atualizado - Sistema IoT Distribuído
- **Sprint 1:** ✅ **COMPLETO** - Base IoT UDP Nativa (Sistema funcionando)
- **Sprint 2:** 🔄 **PRÓXIMO** - Padrões GoF IoT - 1,00 ponto (1 semana)  
- **Sprint 3:** 🔄 HTTP Strategy Pattern - 1,50 pontos (1 semana)
- **Sprint 4:** 🔄 gRPC Strategy Pattern - 3,00 pontos (2 semanas)
- **Sprint 5:** 🔄 Tolerância a Falhas IoT - 3,00 pontos (2 semanas)
- **Sprint 6:** 🔄 Testes JMeter Configurados (1 semana)
- **Sprint 7:** 🔄 Validação Final e Refinamentos (1 semana)
- **Sprint 8:** 🔄 Preparação para Apresentação (1 semana)

**Situação Atual:** 26/09/2025  
**Apresentação:** 16/10/2025 a 28/10/2025  
**Tempo Restante:** ~3 semanas

### Status de Pontuação:
- ✅ **UDP Nativo:** Base sólida implementada
- 🔄 **Padrões GoF:** 1,00 ponto (próximo)
- 🔄 **HTTP:** 1,50 pontos 
- 🔄 **gRPC:** 3,00 pontos
- 🔄 **Tolerância a Falhas:** 3,00 pontos
- **Meta:** 10,00 pontos totais

## Tecnologias e Ferramentas
- **Linguagem:** Java 11+
- **Build:** Maven ou Gradle
- **Testes:** JUnit 5, Mockito
- **Performance:** Apache JMeter
- **Serialização:** JSON (Jackson) ou Protocol Buffers
- **Logging:** SLF4J + Logback
- **Monitoramento:** Métricas customizadas

## Riscos e Mitigações
1. **Complexidade de sincronização:** Implementação incremental com testes extensivos
2. **Performance UDP:** Otimização baseada em profiling e métricas
3. **Tolerância a falhas:** Cenários de teste abrangentes
4. **Consistência eventual:** Validação rigorosa de algoritmos de consenso

## Critérios de Sucesso - Sistema IoT Distribuído

### Pontuação Total: 10,00 pontos
- **Implementação dos Protocolos (6,00 pontos):**
  - UDP: 1,50 pontos - ✅ **IMPLEMENTADO** (nativo funcional)
  - TCP com HTTP: 1,50 pontos - 🔄 **Via Strategy Pattern**
  - gRPC: 3,00 pontos - 🔄 **Via Strategy Pattern**

- **Implementação dos Padrões GoF (1,00 ponto):**
  - Strategy: 🔄 Protocolos de comunicação IoT
  - Observer: 🔄 Monitoramento de sensores 
  - Singleton: 🔄 API Gateway IoT único
  - Proxy: 🔄 Gateway como proxy dos sensores

- **Execução com Tolerância a Falhas (3,00 pontos):**
  - Sistema resiliente a falhas de sensores IoT: 🔄
  - Recuperação automática de sensores: 🔄
  - Version Vector para consistência: ✅ **IMPLEMENTADO**

### Requisitos Técnicos - Status Atual:
- ✅ **3+ componentes distribuídos** (Gateway + Sensor Manager + 5 Sensores)
- ✅ **API Gateway IoT** implementado como coordenador central
- ✅ **Múltiplas instâncias** (5 tipos de sensores IoT)
- ✅ **Version Vector** para replicação de estado distribuído
- 🔄 **Strategy Pattern** para múltiplos protocolos
- ✅ **Comunicação UDP nativa** funcional

### Arquitetura IoT Distribuída Atual:
- ✅ **Coordenador Central:** NativeUDPIoTServer (port 9090)
- ✅ **Sensores Distribuídos:** 5 tipos (TEMP, HUMIDITY, PRESSURE, LIGHT, MOTION)
- ✅ **Version Vector:** Ordenação causal entre eventos de sensores
- ✅ **Tolerância a Falhas:** Heartbeat e detecção de sensores inativos
- ✅ **Thread Safety:** ConcurrentHashMap para estado distribuído
- ✅ **Logging Profissional:** SLF4J com métricas detalhadas

### Validação Funcional:
- ✅ **Sistema executando** com 0% de erros
- ✅ **70+ mensagens processadas** em ambiente de produção
- ✅ **5 sensores ativos** comunicando simultaneamente
- ✅ **Version Vector funcional** com contadores independentes
- ✅ **Shutdown gracioso** e gestão de recursos