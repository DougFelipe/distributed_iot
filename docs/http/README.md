# 🌐 **PROTOCOLO HTTP - SISTEMA IoT DISTRIBUÍDO**

**Comunicação HTTP/1.1 para Testes, Integração e Desenvolvimento**

---

## 📋 **ÍNDICE**

1. [Por que HTTP no Sistema IoT?](#1-por-que-http)
2. [Arquitetura e Design](#2-arquitetura-e-design)
3. [Como Funciona na Prática](#3-como-funciona)
4. [Integração com JMeter](#4-integração-jmeter)
5. [Sensores Virtuais HTTP](#5-sensores-virtuais)
6. [Data Receivers e Persistência](#6-data-receivers)
7. [Version Vector via HTTP](#7-version-vector)
8. [Performance e Limitações](#8-performance)

---

## 1. **POR QUE HTTP?**

### 🎯 **O Problema que Resolvemos**

Imagine que você tem um sistema IoT funcionando perfeitamente com UDP, mas precisa testá-lo com JMeter para avaliar performance sob carga. O problema é que o JMeter foi projetado para trabalhar com HTTP, não com UDP.

**A solução:** Criar uma camada HTTP que funciona como uma "ponte" para o sistema UDP existente. Assim, o JMeter pode enviar requisições HTTP normais, que são convertidas internamente para o formato IoT e processadas pelo mesmo motor que processa mensagens UDP.

### 🔄 **Benefícios Práticos**

**Para Desenvolvimento:**
- Debug mais fácil com navegadores e Postman
- Logs legíveis em formato JSON
- Ferramentas de rede padrão funcionam

**Para Testes:**
- JMeter pode simular milhares de sensores
- Medição precisa de latência e throughput
- Relatórios de performance automáticos

**Para Integração:**
- APIs REST para sistemas externos
- Health checks para monitoramento
- CORS habilitado para aplicações web

### 🤔 **HTTP vs UDP: Quando Usar Cada Um?**

**Use HTTP quando:**
- Estiver testando com JMeter ou ferramentas similares
- Precisar debugar problemas (JSON é legível)
- Integrar com sistemas web existentes
- Desenvolver e prototipar rapidamente
- Atravessar firewalls corporativos

**Use UDP quando:**
- Performance for crítica (sistemas de produção)
- Trabalhar com dispositivos embarcados
- Precisar de baixíssima latência
- Enviar milhares de mensagens por segundo
- Implementar multicast/broadcast

**A Beleza do Strategy Pattern:**
O sistema permite trocar entre HTTP e UDP **sem alterar uma linha** da lógica de negócio. O Gateway IoT, os Data Receivers, e todo o sistema de Version Vector funcionam exatamente igual nos dois protocolos.

---

## 2. **ARQUITETURA E DESIGN**

### 🏗️ **A Grande Ideia: Adapter Pattern**

O sistema HTTP funciona como um **adaptador** entre o mundo HTTP (JMeter, browsers, APIs) and o mundo IoT interno. É como ter um tradutor que fala HTTP com o cliente e IoT com o sistema interno.

**Fluxo Conceitual:**
1. **Cliente HTTP** envia POST /sensor/data com JSON
2. **HTTP Adapter** recebe e converte para IoTMessage  
3. **Gateway IoT** processa como se fosse UDP
4. **Data Receivers** armazenam normalmente
5. **HTTP Adapter** converte resposta para JSON
6. **Cliente HTTP** recebe status de sucesso

### 🎭 **Strategy Pattern em Ação**

O sistema implementa o Strategy Pattern, permitindo trocar protocolos dinamicamente:

- **Interface CommunicationStrategy:** Define o contrato comum
- **HTTPCommunicationStrategy:** Implementação HTTP
- **UDPCommunicationStrategy:** Implementação UDP  
- **IoTGateway:** Usa qualquer strategy transparentemente

**Benefício:** Adicionar um novo protocolo (TCP, gRPC, WebSocket) significa apenas criar uma nova Strategy, sem tocar no core do sistema.

### 🧵 **Threading e Concorrência**

**O Desafio:** Como processar centenas de requisições HTTP simultâneas sem travar?

**A Solução:** Thread Pool com arquitetura producer-consumer:
- **Main Thread:** Aceita conexões (ServerSocket.accept())
- **Thread Pool:** Processa cada conexão em thread separada
- **Worker Threads:** HTTPClientHandler processa uma requisição por vez

**Configuração Padrão:**
- 50 threads no pool (ajustável)
- 300 conexões no backlog
- Timeout automático para conexões penduradas

---

## 3. **COMO FUNCIONA NA PRÁTICA**

### � **O Ciclo de Vida de uma Requisição**

Vamos acompanhar o que acontece quando o JMeter envia uma requisição HTTP:

**1. Chegada da Requisição:**
O JMeter envia um POST para `http://localhost:8081/sensor/data` com JSON contendo dados do sensor. O servidor HTTP está escutando na porta 8081 e imediatamente aceita a conexão.

**2. Delegação para Thread Pool:**
O servidor não processa a requisição na thread principal (isso travaria outras conexões). Em vez disso, cria um HTTPClientHandler e envia para o thread pool processar.

**3. Parsing HTTP → IoT:**
O HTTPClientHandler faz o trabalho pesado:
- Lê headers HTTP line-by-line
- Extrai o corpo JSON da requisição  
- Converte JSON para objeto IoTMessage
- Valida se todos os campos obrigatórios estão presentes

**4. Integração com Gateway:**
Aqui está a mágica! O HTTPClientHandler entrega o IoTMessage para o mesmo Gateway que processa mensagens UDP. O Gateway não sabe (nem precisa saber) se a mensagem veio via HTTP ou UDP.

**5. Resposta HTTP:**
Após o processamento, o sistema constrói uma resposta JSON com status de sucesso, ID da mensagem processada, e o Version Vector atualizado. Tudo formatado seguindo padrões HTTP/1.1.

### ⚙️ **Configurações Avançadas**

```java
/**
 * BUILDER PATTERN para configurações HTTP
 */
public class HTTPCommunicationStrategy implements CommunicationStrategy {
    
    /**
     * Configurar tamanho do thread pool
     */
    public void setThreadPoolSize(int size) {
        if (running.get()) {
            throw new IllegalStateException("Não é possível alterar thread pool com servidor rodando");
        }
        
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
        
        this.threadPool = Executors.newFixedThreadPool(size);
        System.out.println("🌐 Thread pool HTTP configurado para " + size + " threads");
    }
    
    /**
     * VIRTUAL THREADS support (Java 21+)
     */
    public void enableVirtualThreads() {
        if (running.get()) {
            throw new IllegalStateException("Não é possível alterar thread pool com servidor rodando");
        }
        
        try {
            // Tentar criar ExecutorService com virtual threads
            this.threadPool = Executors.newVirtualThreadPerTaskExecutor();
            System.out.println("🌐 Virtual threads habilitadas para HTTP Strategy");
        } catch (Exception e) {
            System.out.println("⚠️ Virtual threads não disponíveis, usando thread pool tradicional");
            this.threadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        }
    }
    
    /**
     * Gateway Integration
     */
    public void setGateway(IoTGateway gateway) {
        this.gateway = gateway;
    }
}
```

---

## 3. **STRATEGY PATTERN**

### 🎯 **Interface CommunicationStrategy**

```java
/**
 * Strategy Pattern - Interface para protocolos de comunicação
 */
public interface CommunicationStrategy {
    
    /**
     * Inicia o servidor/listener para este protocolo
     */
    void startServer(int port) throws Exception;
    
    /**
     * Para o servidor/listener
     */
    void stopServer();
    
    /**
     * Envia mensagem IoT (cliente)
     */
    boolean sendMessage(IoTMessage message, String host, int port);
    
    /**
     * Processa mensagem IoT recebida
     */
    void processMessage(IoTMessage message, String senderHost, int senderPort);
    
    /**
     * Nome do protocolo
     */
    String getProtocolName();
    
    /**
     * Status do servidor
     */
    boolean isRunning();
}
```

### 🔄 **HTTP Strategy Implementation**

```java
/**
 * Implementação HTTP do Strategy Pattern
 */
public class HTTPCommunicationStrategy implements CommunicationStrategy {
    
    @Override
    public void startServer(int port) throws Exception {
        if (running.get()) {
            throw new IllegalStateException("HTTP Server já está rodando na porta " + this.port);
        }
        
        this.port = port;
        
        try {
            serverSocket = new ServerSocket(port, DEFAULT_BACKLOG);
            running.set(true);
            
            System.out.println("🌐 HTTP Strategy Server iniciado na porta " + port);
            System.out.println("🌐 Aguardando conexões HTTP para IoT Gateway...");
            
            // ASYNC ACCEPT LOOP - Non-blocking
            threadPool.submit(() -> {
                while (running.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        
                        if (!running.get()) {
                            clientSocket.close();
                            break;
                        }
                        
                        System.out.println("🌐 Nova conexão HTTP de " + 
                                         clientSocket.getRemoteSocketAddress());
                        
                        // DELEGATION PATTERN - Handler processing
                        HTTPClientHandler handler = new HTTPClientHandler(clientSocket, gateway);
                        threadPool.execute(handler);
                        
                    } catch (IOException e) {
                        if (running.get()) {
                            System.err.println("❌ Erro ao aceitar conexão HTTP: " + e.getMessage());
                        }
                    }
                }
            });
            
        } catch (IOException e) {
            running.set(false);
            System.err.println("❌ Erro ao iniciar HTTP Strategy Server: " + e.getMessage());
            throw new Exception("Falha ao iniciar servidor HTTP na porta " + port, e);
        }
    }
    
    @Override
    public String getProtocolName() {
        return "HTTP/1.1";
    }
    
    @Override
    public boolean sendMessage(IoTMessage message, String targetAddress, int targetPort) {
        // HTTP é principalmente server-side para este sistema IoT
        // Cliente HTTP seria implementado se necessário para comunicação externa
        System.out.println("⚠️ sendMessage não implementado para HTTP Strategy (server-only)");
        return false;
    }
}
```

---

**📝 Documentação HTTP criada por:** UFRN-DIMAP  
**📅 Data:** 30 de Setembro de 2025  
**🔖 Versão:** 1.0 - Parte 1  
**🎯 Foco:** Visão Geral, Arquitetura e Strategy Pattern