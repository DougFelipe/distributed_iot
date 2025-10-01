package br.ufrn.dimap.communication.http;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.singleton.IoTGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Strategy de comunicação HTTP para IoT
 * Baseado no exemplo WebServer.java do professor com adaptações
 */
public class HTTPCommunicationStrategy implements CommunicationStrategy {
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private IoTGateway gateway;
    private int port;
    
    // Configurações
    private static final int DEFAULT_THREAD_POOL_SIZE = 50;
    private static final int DEFAULT_BACKLOG = 300;
    
    public HTTPCommunicationStrategy() {
        this.threadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
    }
    
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
            
            // Executar loop de aceitação em thread separada para não bloquear
            threadPool.submit(() -> {
                while (running.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        
                        if (!running.get()) {
                            clientSocket.close();
                            break;
                        }
                        
                        System.out.println("🌐 Nova conexão HTTP de " + clientSocket.getRemoteSocketAddress());
                        
                        // Processar em thread separada
                        HTTPClientHandler handler = new HTTPClientHandler(clientSocket, gateway);
                        threadPool.execute(handler);
                        
                    } catch (IOException e) {
                        if (running.get()) {
                            System.err.println("❌ Erro ao aceitar conexão HTTP: " + e.getMessage());
                        }
                        // Se não está rodando, é shutdown normal
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
    public void stopServer() {
        if (!running.get()) {
            return;
        }
        
        System.out.println("🌐 Parando HTTP Strategy Server...");
        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("❌ Erro ao fechar ServerSocket HTTP: " + e.getMessage());
        }
        
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("✅ HTTP Strategy Server parado com sucesso");
    }
    
    @Override
    public boolean sendMessage(IoTMessage message, String targetAddress, int targetPort) {
        // HTTP é principalmente server-side para este sistema IoT
        // Cliente HTTP seria implementado se necessário para comunicação externa
        System.out.println("⚠️ sendMessage não implementado para HTTP Strategy (server-only)");
        return false;
    }
    
    @Override
    public void processMessage(IoTMessage message, String senderHost, int senderPort) {
        // Processamento é feito no HTTPClientHandler
        System.out.println("� [HTTP] Processando mensagem: " + message.getMessageId() + 
                         " de " + senderHost + ":" + senderPort);
    }
    
    // Métodos específicos HTTP
    
    /**
     * Define o gateway IoT que processará as mensagens
     */
    public void setGateway(IoTGateway gateway) {
        this.gateway = gateway;
    }
    
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
     * Usar virtual threads (Java 21+) se disponível
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
    
    // Implementação de métodos da interface
    
    @Override
    public String getProtocolName() {
        return "HTTP/1.1";
    }
    
    // Métodos específicos HTTP adicionais
    
    public boolean supportsKeepAlive() {
        return false; // Implementação simples sem keep-alive
    }
    
    public int getMaxConcurrentConnections() {
        return DEFAULT_THREAD_POOL_SIZE;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public int getPort() {
        return port;
    }
}