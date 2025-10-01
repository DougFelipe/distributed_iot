# 🔧 **COMPONENTES PRINCIPAIS TCP**

**Análise Detalhada dos Componentes Core do Sistema TCP IoT**

---

## 4. **COMPONENTES PRINCIPAIS**

### 🎯 **TCPCommunicationStrategy - O Coração do Sistema**

O `TCPCommunicationStrategy` é o componente central que implementa a interface `CommunicationStrategy` para comunicação TCP. Ele gerencia todo o ciclo de vida do servidor TCP e coordena as conexões de múltiplos sensores.

**Responsabilidades Principais:**

**Gerenciamento do Servidor:** Inicializa e mantém o `ServerSocket` na porta 8082, configurando timeouts e parâmetros de conexão apropriados para o ambiente IoT.

**Pool de Threads:** Mantém um pool de 50 threads que são reutilizadas para processar conexões de diferentes sensores, otimizando o uso de recursos do sistema.

**Monitoramento de Conexões:** Rastreia todas as conexões ativas usando um `ConcurrentHashMap`, permitindo estatísticas em tempo real e desconexão seletiva de clientes.

**Shutdown Gracioso:** Implementa um processo de encerramento que permite que conexões em andamento terminem de forma limpa antes de fechar o servidor.

**Configurações Importantes:**

- **Porta Padrão:** 8082 (diferente do HTTP 8081 para evitar conflitos)
- **Timeout de Aceitação:** 5 segundos (permite verificação periódica do status de running)
- **Tamanho do Pool:** 50 threads (balanceio entre performance e recursos)
- **Timeout de Shutdown:** 30 segundos (tempo máximo para encerramento gracioso)

### 🔌 **TCPClientHandler - Gerenciador de Conexões**

Cada sensor que se conecta ao gateway TCP recebe seu próprio `TCPClientHandler`, que é executado em uma thread dedicada. Este componente é responsável por toda a comunicação com um sensor específico.

**Lifecycle de um Handler:**

**Inicialização:** Quando uma nova conexão é aceita, um handler é criado com referências ao socket do cliente e ao gateway IoT. O timeout do socket é configurado para 30 segundos.

**Loop de Processamento:** O handler entra em um loop onde lê mensagens do sensor usando `BufferedReader`. Cada mensagem é processada imediatamente e uma resposta é enviada via `PrintWriter`.

**Compatibilidade JMeter:** Para funcionar corretamente com JMeter TCP Sampler, o handler fecha a conexão após processar uma mensagem IoT, simulando um padrão request-response.

**Limpeza:** Quando a conexão encerra (por timeout, comando DISCONNECT, ou erro), todos os recursos são liberados automaticamente através do try-with-resources.

**Características Especiais:**

**Timeout Inteligente:** Conexões inativas são automaticamente encerradas após 30 segundos, liberando recursos sem afetar sensores ativos.

**Tratamento de Comandos:** Reconhece comandos especiais como "DISCONNECT" e "EXIT" para encerramento controlado.

**Logging Detalhado:** Registra todas as atividades da conexão para facilitar debugging e monitoramento.

**Tratamento de Erros Robusto:** Captura e trata diferentes tipos de exceções (timeout, I/O, processamento) sem afetar outras conexões.

### 📝 **TCPMessageProcessor - Processamento de Mensagens**

O `TCPMessageProcessor` é responsável por converter mensagens TCP brutas em objetos `IoTMessage` que o sistema interno pode processar, e vice-versa.

**Funcionalidades Principais:**

**Parsing Flexível:** Aceita mensagens em formato delimitado por pipe (`|`) compatível com o sistema UDP existente, mas também funciona com formatos mais simples.

**Geração de IoTMessage:** Converte mensagens TCP em objetos IoTMessage completos, incluindo timestamp, version vector, e metadados necessários.

**Version Vector Integration:** Processa e serializa version vectors para manter consistência distribuída entre sensores.

**Geração de Respostas:** Cria respostas formatadas que informam ao sensor sobre o status do processamento.

**Formato de Mensagem Suportado:**

```
SENSOR_DATA|sensor_id|sensor_type|location|timestamp|value|version_vector
```

**Exemplo Prático:**
```
SENSOR_DATA|TEMP_001|TEMPERATURE|Lab-A|1640995200000|25.6|TEMP_001:5
```

**Tolerância a Erros:** Se uma mensagem não segue exatamente o formato esperado, o processador tenta extrair informações básicas (ID do sensor, tipo, valor) e cria um objeto IoTMessage válido com valores padrão para campos ausentes.

**Respostas Geradas:**

**Sucesso:** `SUCCESS|IOT-MSG-timestamp|sensor_id|PROCESSED`
**Erro:** `ERROR|IOT-MSG-timestamp|sensor_id|error_description`

### 🔧 **TCPProtocolConstants - Configurações Centralizadas**

Este componente centraliza todas as constantes e configurações do protocolo TCP, garantindo consistência em todo o sistema.

**Categorias de Constantes:**

**Protocolo:** Separadores de campo (`|`), terminadores de linha, tipos de mensagem suportados.

**Respostas:** Formatos padrão para respostas de sucesso e erro, garantindo que todos os sensores recebam respostas consistentes.

**Configurações de Rede:** Timeouts, tamanhos de buffer, configurações de thread pool.

**Compatibilidade:** Mantém compatibilidade com o sistema UDP existente através de constantes compartilhadas.

---

## 5. **FLUXO DE COMUNICAÇÃO**

### 🔄 **Fluxo Completo de Comunicação TCP**

```ascii
┌─────────────────────────────────────────────────────────────────┐
│                    FLUXO DE COMUNICAÇÃO TCP                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  FASE 1: ESTABELECIMENTO DE CONEXÃO                            │
│                                                                 │
│  ┌─────────────┐         TCP SYN         ┌─────────────────┐   │
│  │   Sensor    │ ─────────────────────── ▶│  TCP Gateway    │   │
│  │   IoT       │         TCP SYN-ACK     │  (Port 8082)    │   │
│  │             │ ◀─────────────────────── │                 │   │
│  │ TEMP_001    │         TCP ACK         │ • ServerSocket  │   │
│  │             │ ─────────────────────── ▶│ • Accept()      │   │
│  └─────────────┘                         │ • New Thread    │   │
│                                           └─────────────────┘   │
│                                                                 │
│  FASE 2: CRIAÇÃO DO HANDLER                                    │
│                                                                 │
│                           ┌─────────────────────────────────┐   │
│                           │    TCPCommunicationStrategy     │   │
│                           │                                 │   │
│                           │  handleClientConnection()       │   │
│                           │         │                       │   │
│                           │         ▼                       │   │
│                           │  new TCPClientHandler()         │   │
│                           │         │                       │   │
│                           │         ▼                       │   │
│                           │  threadPool.submit()            │   │
│                           └─────────────────────────────────┘   │
│                                     │                           │
│                                     ▼                           │
│  FASE 3: PROCESSAMENTO DE MENSAGENS                            │
│                                                                 │
│  ┌─────────────┐    Message Data    ┌─────────────────────┐     │
│  │   Sensor    │ ─────────────────▶ │  TCPClientHandler   │     │
│  │             │                    │                     │     │
│  │ Sends:      │                    │  • BufferedReader   │     │
│  │ "SENSOR_DATA│                    │  • processMessage() │     │
│  │ |TEMP_001|   │                    │  • TCPMessageProc   │     │
│  │ TEMPERATURE │                    │  • Convert to       │     │
│  │ |Lab-A|     │                    │    IoTMessage       │     │
│  │ 1640995200000                    │                     │     │
│  │ |25.6"       │                    └─────────────────────┘     │
│  └─────────────┘                              │                 │
│                                               ▼                 │
│                              ┌─────────────────────────────┐     │
│                              │        IoTGateway           │     │
│                              │                             │     │
│                              │  routeToDataReceiver()      │     │
│                              │  • Version Vector Update    │     │
│                              │  • Data Storage             │     │
│                              │  • Replication              │     │
│                              └─────────────────────────────┘     │
│                                               │                 │
│                                               ▼                 │
│  FASE 4: RESPOSTA                                              │
│                                                                 │
│  ┌─────────────┐    Response Data   ┌─────────────────────┐     │
│  │   Sensor    │ ◀───────────────── │  TCPClientHandler   │     │
│  │             │                    │                     │     │
│  │ Receives:   │                    │  • Generate Response│     │
│  │ "SUCCESS|   │                    │  • PrintWriter     │     │
│  │ IOT-MSG-    │                    │  • Flush Output     │     │
│  │ 1640995200123                    │                     │     │
│  │ |TEMP_001|  │                    │                     │     │
│  │ PROCESSED"  │                    └─────────────────────┘     │
│  └─────────────┘                                               │
│                                                                 │
│  FASE 5: ENCERRAMENTO (Compatibilidade JMeter)                 │
│                                                                 │
│                              ┌─────────────────────────────┐     │
│                              │    Connection Cleanup       │     │
│                              │                             │     │
│                              │  • Close Socket             │     │
│                              │  • Remove from Active List │     │
│                              │  • Release Thread           │     │
│                              │  • Log Disconnection        │     │
│                              └─────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 🔄 **Processamento Concorrente**

Uma das grandes vantagens do TCP no sistema IoT é sua capacidade de processar múltiplas conexões simultaneamente:

**Múltiplos Sensores:** Diferentes sensores (TEMP_001, HUMID_002, PRESSURE_003) podem se conectar simultaneamente, cada um obtendo sua própria thread de processamento.

**Isolamento de Falhas:** Se um sensor envia dados corrompidos ou se desconecta inesperadamente, isso não afeta os outros sensores conectados.

**Balanceamento de Carga:** O thread pool automaticamente distribui a carga de processamento entre as threads disponíveis.

**Monitoramento Individual:** Cada conexão pode ser monitorada independentemente, facilitando debugging e otimização.

### 📊 **Estados de Conexão**

Durante seu ciclo de vida, uma conexão TCP passa por diferentes estados:

**CONNECTING:** Estabelecimento inicial da conexão TCP (handshake)
**ACTIVE:** Conexão estabelecida, aguardando ou processando mensagens
**PROCESSING:** Mensagem sendo processada pelo gateway
**RESPONDING:** Resposta sendo enviada de volta ao sensor
**TIMEOUT:** Conexão inativa por mais de 30 segundos
**DISCONNECTING:** Encerramento controlado (comando DISCONNECT)
**CLOSED:** Conexão encerrada, recursos liberados

Este modelo de estados permite monitoramento detalhado e troubleshooting eficiente.

---

**📝 Documentação TCP criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0 - Parte 2  
**🎯 Foco:** Componentes Principais e Fluxo de Comunicação