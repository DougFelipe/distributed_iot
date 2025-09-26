# Roadmap de Implementação - Sistema Distribuído Tolerante a Falhas

## Visão Geral do Projeto

**Instituição:** Universidade Federal do Rio Grande do Norte - DIMAP  
**Disciplina:** Programação Distribuída  
**Desenvolvimento:** Individual

**Objetivo:** Desenvolver um sistema distribuído tolerante a falhas de rede/componentes, implementado seguindo padrões de Sistemas Distribuídos descritos no livro "Patterns of Distributed Systems" (Addison-Wesley Signature Series, 2024).

**Requisitos Específicos:**
- Implementar aplicação simples relacionada com Padrão de Sistema Distribuído escolhido
- Exemplos: Leader and Followers, Generation Clock, ou similar
- Sistema deve ter pelo menos 3 componentes distribuídos
- Um componente obrigatoriamente deve ser um API Gateway
- Mínimo 2 instâncias se forem stateless
- Replicação de dados obrigatória se um componente for stateful

## Arquitetura do Sistema

### Configuração Mínima (conforme especificação):
- **Mínimo 3 componentes distribuídos**
- **API Gateway obrigatório** como um dos componentes
- **Mínimo 2 instâncias** para componentes stateless
- **Replicação obrigatória** para componentes stateful

### Componentes Principais:
1. **API Gateway** (Obrigatório)
   - Ponto único de entrada para requisições do JMeter
   - Descoberta dinâmica e registro de componentes ativos  
   - Roteamento inteligente para diferentes componentes (A e B)
   - Monitoramento via Heartbeat de todos os componentes internos
   - Manutenção de tabela atualizada com componentes ativos

2. **Componente A** (Instâncias 1 e 2)
   - Processamento distribuído de requisições
   - Implementação de padrões de sistemas distribuídos
   - Comunicação interna via protocolos definidos

3. **Componente B** (Instâncias 1 e 2) 
   - Processamento distribuído complementar
   - Replicação de dados se stateful
   - Integração com API Gateway para descoberta

### Protocolos de Comunicação Suportados:
- **Transporte:** UDP e TCP
- **Aplicação:** HTTP e gRPC
- **Implementação:** Todos os padrões devem ser suportados em um único projeto, com definição de protocolo no startup

### Padrões GoF Obrigatórios:
- **Strategy:** Escolha de protocolo em tempo de execução
- **Observer:** Monitoramento via heartbeat  
- **Singleton:** API Gateway como ponto único de entrada
- **Proxy:** Encaminhamento de requisições pelo Gateway
- Sistema deve seguir padrões GoF descritos no livro "Design Patterns: Elements of Reusable Object-Oriented Software" (1995)

### Padrões de Sistemas Distribuídos:
- Implementação baseada em "Patterns of Distributed Systems" (Addison-Wesley, 2024)
- **Tolerância a Falhas:** Sistema deve ser resiliente a falhas de rede/componentes

### Fluxo de Execução da Arquitetura:
1. **JMeter** envia requisições para o **API Gateway**
2. **API Gateway** roteia requisições para componentes internos (A e B) usando samplers UDP, TCP, HTTP e gRPC
3. **Componentes A e B** devem descobrir dinamicamente outros componentes
4. **Componentes** iniciam enviando mensagem ao API Gateway com endereço IP/porta
5. **API Gateway** monitora disponibilidade usando padrão Heartbeat
6. **Comunicação interna** entre Componentes A e B também via API Gateway

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

## Sprint 1: Fundação e Estrutura Base do Projeto
**Duração:** 1 semana  
**Objetivo:** Estabelecer a estrutura base do projeto e implementar os padrões fundamentais

### Tarefas:
1. **Configuração do Projeto**
   - Criar estrutura de diretórios Maven/Gradle
   - Configurar dependências para todos os protocolos (UDP, TCP, HTTP, gRPC)
   - Estabelecer padrões de codificação seguindo GoF
   - Configurar sistema de build para desenvolvimento individual

2. **Implementação dos Padrões GoF Obrigatórios**
   - **Singleton:** API Gateway como instância única
   - **Strategy Interface:** Definir interface para seleção de protocolo em runtime
   - **Observer Pattern:** Interface para sistema Heartbeat
   - **Proxy Pattern:** Encaminhamento de requisições pelo Gateway

3. **Estrutura Base da Arquitetura**
   - Definir interfaces para Componente A e Componente B
   - Implementar sistema de descoberta dinâmica de componentes
   - Criar classes base para comunicação inter-componentes
   - Estrutura para registro de endereços IP/porta

4. **Sistema de Configuração Multi-protocolo**
   - Configuração para startup com protocolo específico
   - Sistema de logging estruturado para debugging
   - Configurações para diferentes ambientes de teste

### Entregáveis:
- Estrutura de projeto configurada
- Classes base implementadas
- Interfaces dos padrões GoF definidas
- Testes unitários básicos
- Documentação da arquitetura

---

## Sprint 2: Implementação da Comunicação UDP (1,50 pontos)
**Duração:** 2 semanas  
**Objetivo:** Implementar comunicação UDP completa conforme especificação

### Tarefas:
1. **Strategy Pattern para UDP**
   - Implementar UDPCommunicationStrategy seguindo padrão Strategy
   - Criar UDPSender e UDPReceiver para comunicação
   - Implementar serialização/deserialização de mensagens
   - Configuração para uso no startup do componente

2. **API Gateway UDP**
   - Recepção de requisições JMeter via UDP
   - Descoberta dinâmica: registro de Componentes A e B
   - Roteamento inteligente baseado em disponibilidade
   - Sistema de registro com endereço IP/porta dos componentes

3. **Componentes A e B com UDP**
   - Implementar comunicação UDP entre componentes
   - Sistema de descoberta: envio de mensagem inicial ao Gateway
   - Processamento de requisições recebidas do Gateway
   - Comunicação interna entre Componentes A e B via Gateway

4. **Protocolo de Mensagens UDP**
   - Mensagens de descoberta (REGISTER, DISCOVER)
   - Mensagens de dados (REQUEST, RESPONSE)
   - Mensagens de controle (HEARTBEAT, STATUS)
   - Formato padronizado para todas as mensagens

### Entregáveis:
- Comunicação UDP completa entre componentes
- API Gateway funcional para UDP
- Serviço de Coleta operacional
- Protocolo de mensagens definido e implementado
- Testes de integração UDP

---

## Sprint 3: Sistema de Monitoramento e Heartbeat (Observer Pattern)
**Duração:** 1 semana  
**Objetivo:** Implementar sistema de monitoramento conforme padrão Observer

### Tarefas:
1. **Observer Pattern para Heartbeat (Obrigatório)**
   - Implementar HeartbeatObserver no API Gateway
   - Criar HeartbeatSubject nos Componentes A e B
   - Sistema de detecção de falhas com timeout configurável
   - Notificação automática de mudanças de status

2. **Tabela de Componentes Ativos**
   - API Gateway mantém tabela atualizada de componentes
   - Registro/desregistro automático baseado em heartbeat
   - Status de saúde de cada componente (ativo/inativo)
   - Timestamp da última comunicação

3. **Tolerância a Falhas - Preparação**
   - Detecção de componentes inativos
   - Remoção automática de componentes falhos da rotação
   - Preparação para redistribuição de carga
   - Logs detalhados para debugging

4. **Testes de Monitoramento**
   - Simulação de falhas de componentes
   - Validação de detecção de heartbeat
   - Testes de recuperação automática
   - Métricas de tempo de detecção de falhas

### Entregáveis:
- Sistema de heartbeat funcional
- Descoberta dinâmica implementada
- Mecanismos de tolerância a falhas
- Interface de monitoramento básica

---

## Sprint 4: Implementação TCP com HTTP (1,50 pontos)
**Duração:** 1 semana  
**Objetivo:** Implementar protocolo TCP com HTTP usando Strategy Pattern

### Tarefas:
1. **Strategy Pattern para TCP/HTTP**
   - Implementar TCPHTTPCommunicationStrategy
   - Criar HTTPServer e HTTPClient para comunicação
   - Integração com Strategy existente (trocar protocolo no startup)
   - Manter mesma interface de comunicação

2. **API Gateway com HTTP**
   - Recepção de requisições JMeter via HTTP
   - Adaptação do sistema de descoberta para HTTP
   - Roteamento HTTP para Componentes A e B
   - Manutenção da mesma lógica de negócio

3. **Componentes A e B com HTTP**
   - Implementar endpoints HTTP nos componentes
   - Sistema de registro via HTTP ao Gateway
   - Processamento de requisições HTTP
   - Comunicação interna via HTTP através do Gateway

4. **Testes de Intercambiabilidade**
   - Validar que lógica de negócio permanece inalterada
   - Testes de mudança de protocolo no startup
   - Comparação de funcionalidades UDP vs HTTP
   - Métricas de performance para ambos protocolos

### Entregáveis:
- APIs de consulta funcionais
- Operações de agregação implementadas
- Sistema de cache otimizado
- Integração completa com Gateway

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

## Cronograma Resumido (Atualizado)
- **Sprint 1:** Fundação e Padrões GoF (1 semana)
- **Sprint 2:** Protocolo UDP - 1,50 pontos (2 semanas)  
- **Sprint 3:** Sistema Heartbeat/Observer (1 semana)
- **Sprint 4:** Protocolo TCP/HTTP - 1,50 pontos (1 semana)
- **Sprint 5:** Protocolo gRPC - 3,00 pontos (2 semanas)
- **Sprint 6:** Tolerância a Falhas - 3,00 pontos (2 semanas)
- **Sprint 7:** Testes JMeter Obrigatórios (1 semana)
- **Sprint 8:** Validação Final de Requisitos (1 semana)
- **Sprint 9:** Preparação para Apresentação (1 semana)

**Total:** 12 semanas  
**Apresentação:** 16/10/2025 a 28/10/2025

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

## Critérios de Sucesso (Conforme Avaliação)

### Pontuação Total: 10,00 pontos
- **Implementação dos Protocolos (6,00 pontos):**
  - UDP: 1,50 pontos ✅
  - TCP com HTTP: 1,50 pontos ✅  
  - gRPC: 3,00 pontos ✅

- **Implementação dos Padrões GoF (1,00 ponto):**
  - Strategy, Observer, Singleton, Proxy ✅

- **Execução com Tolerância a Falhas (3,00 pontos):**
  - Sistema resiliente a falhas de componentes ✅
  - Recuperação automática demonstrada ✅

### Requisitos Técnicos Obrigatórios:
- ✅ Mínimo 3 componentes distribuídos (API Gateway + Componentes A e B)
- ✅ API Gateway obrigatório implementado
- ✅ Mínimo 2 instâncias para componentes stateless
- ✅ Replicação implementada para componentes stateful
- ✅ Todos os protocolos suportados em um único projeto
- ✅ Seleção de protocolo via parâmetro de startup

### Critérios de Apresentação:
- ✅ JMeter configurado com usuários > 5 e < Knee Capacity
- ✅ Summary Report mostrando zero erros em funcionamento normal
- ✅ Demonstração de falhas e recuperação ao vivo
- ✅ Capacidade de explicar qualquer parte do código
- ✅ Sistema completamente funcional para demonstração