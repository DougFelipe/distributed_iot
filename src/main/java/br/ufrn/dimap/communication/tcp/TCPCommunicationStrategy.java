package br.ufrn.dimap.communication.tcp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.singleton.IoTGateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Estratégia de comunicação TCP baseada nos exemplos do professor.
 * Implementa servidor TCP multi-threaded para IoT.
 * 
 * Inspirado em:
 * - exemplo_tcp/TCPServer.java (servidor TCP básico)
 * - exemplo_tcp_virtual_threads/TCPServerVirtualThreads.java (threads virtuais)
 * - exemplo_tcp/ProcessPayload.java (processamento de mensagens)
 * 
 * Características:
 * - Servidor TCP multi-threaded
 * - Gerenciamento de conexões persistentes
 * - Thread pool configurável
 * - Timeout de conexão
 * - Compatibilidade com protocolo UDP (para JMeter)
 */
public class TCPCommunicationStrategy implements CommunicationStrategy {
    private static final Logger logger = Logger.getLogger(TCPCommunicationStrategy.class.getName());
    
    // Configurações do servidor TCP
    private static final int DEFAULT_PORT = 8082;
    private static final int ACCEPT_TIMEOUT = 5000; // 5 segundos
    private static final int THREAD_POOL_SIZE = 50;
    private static final int SHUTDOWN_TIMEOUT = 30; // segundos
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, TCPClientHandler> activeClients = new ConcurrentHashMap<>();
    
    private int port;
    private IoTGateway gateway;
    private Thread serverThread;
    
    public TCPCommunicationStrategy() {
        this(DEFAULT_PORT);
    }
    
    public TCPCommunicationStrategy(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        logger.info("Estratégia TCP criada para porta: " + port);
    }
    
    @Override
    public void startServer(int port) throws Exception {
        this.port = port > 0 ? port : this.port;
        this.gateway = IoTGateway.getInstance();
        
        logger.info("Inicializando estratégia TCP na porta: " + this.port);
        if (isRunning.get()) {
            logger.warning("Servidor TCP já está em execução");
            return;
        }
        
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
            isRunning.set(true);
            
            // Iniciar thread do servidor
            serverThread = new Thread(this::runServer, "TCP-Server-" + port);
            serverThread.setDaemon(false);
            serverThread.start();
            
            logger.info("🚀 Servidor TCP iniciado na porta " + port + " com pool de " + THREAD_POOL_SIZE + " threads");
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao iniciar servidor TCP na porta " + port, e);
            isRunning.set(false);
            throw new RuntimeException("Falha ao iniciar servidor TCP", e);
        }
    }
    
    /**
     * Loop principal do servidor TCP.
     * Baseado no padrão dos exemplos do professor.
     */
    private void runServer() {
        logger.info("Loop principal do servidor TCP iniciado");
        
        while (isRunning.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClientConnection(clientSocket);
                
            } catch (SocketTimeoutException e) {
                // Timeout normal - continua o loop para verificar isRunning
                continue;
                
            } catch (IOException e) {
                if (isRunning.get()) {
                    logger.log(Level.WARNING, "Erro ao aceitar conexão TCP: " + e.getMessage());
                }
            }
        }
        
        logger.info("Loop principal do servidor TCP finalizado");
    }
    
    /**
     * Gerencia uma nova conexão de cliente.
     * Segue o padrão thread-per-connection dos exemplos do professor.
     */
    private void handleClientConnection(Socket clientSocket) {
        String clientKey = clientSocket.getRemoteSocketAddress().toString();
        
        try {
            logger.info("Nova conexão TCP aceita de: " + clientKey);
            
            // Criar handler para o cliente
            TCPClientHandler clientHandler = new TCPClientHandler(clientSocket, gateway);
            
            // Registrar cliente ativo
            activeClients.put(clientKey, clientHandler);
            
            // Submeter para thread pool
            threadPool.submit(() -> {
                try {
                    clientHandler.run();
                } finally {
                    // Remover cliente quando desconectar
                    activeClients.remove(clientKey);
                    logger.info("Cliente TCP desconectado: " + clientKey);
                }
            });
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao configurar conexão do cliente TCP " + clientKey, e);
            
            // Fechar socket em caso de erro
            try {
                clientSocket.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Erro ao fechar socket do cliente: " + ex.getMessage());
            }
        }
    }
    
    @Override
    public void stopServer() {
        if (!isRunning.get()) {
            logger.info("Servidor TCP já está parado");
            return;
        }
        
        logger.info("🛑 Parando servidor TCP...");
        isRunning.set(false);
        
        // Parar clientes ativos
        stopActiveClients();
        
        // Fechar server socket
        closeServerSocket();
        
        // Parar thread pool
        shutdownThreadPool();
        
        // Aguardar thread do servidor
        if (serverThread != null) {
            try {
                serverThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Interrupção ao aguardar thread do servidor TCP");
            }
        }
        
        logger.info("✅ Servidor TCP parado com sucesso");
    }
    
    /**
     * Para todos os clientes ativos.
     */
    private void stopActiveClients() {
        logger.info("Parando " + activeClients.size() + " clientes TCP ativos...");
        
        for (TCPClientHandler client : activeClients.values()) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao parar cliente TCP: " + e.getMessage());
            }
        }
        
        activeClients.clear();
    }
    
    /**
     * Fecha o server socket.
     */
    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Erro ao fechar server socket TCP: " + e.getMessage());
            }
        }
    }
    
    /**
     * Para o thread pool de forma segura.
     */
    private void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            logger.info("Parando thread pool TCP...");
            
            threadPool.shutdown();
            
            try {
                if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                    logger.warning("Timeout aguardando thread pool TCP - forçando shutdown");
                    threadPool.shutdownNow();
                    
                    if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.severe("Thread pool TCP não conseguiu parar completamente");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                threadPool.shutdownNow();
                logger.warning("Interrupção ao aguardar thread pool TCP");
            }
        }
    }
    
    @Override
    public boolean sendMessage(IoTMessage message, String host, int port) {
        // TCP é baseado em conexões - clientes se conectam ao servidor
        // Esta implementação não suporta envio ativo de mensagens
        logger.warning("TCP strategy não suporta envio ativo de mensagens - protocolo baseado em conexão");
        return false;
    }
    
    @Override
    public void processMessage(IoTMessage message, String senderHost, int senderPort) {
        // Método obrigatório da interface - processamento é feito via TCPClientHandler
        logger.fine("Processando mensagem TCP de " + senderHost + ":" + senderPort + 
                   " - Sensor: " + message.getSensorId());
        
        // O processamento real é delegado para o IoTGateway via TCPClientHandler
        if (gateway != null) {
            logger.fine("Mensagem TCP processada com sucesso: " + message.getSensorId());
        }
    }
    
    @Override
    public String getProtocolName() {
        return "TCP";
    }
    
    public int getPort() {
        return port;
    }
    
    @Override
    public boolean isRunning() {
        return isRunning.get() && 
               serverSocket != null && 
               !serverSocket.isClosed() && 
               serverThread != null && 
               serverThread.isAlive();
    }
    
    /**
     * Obtém estatísticas do servidor TCP.
     */
    public String getServerStats() {
        return String.format(
            "TCP Server Stats - Port: %d, Running: %s, Active Clients: %d, Thread Pool Active: %d",
            port,
            isRunning.get(),
            activeClients.size(),
            threadPool instanceof java.util.concurrent.ThreadPoolExecutor ? 
                ((java.util.concurrent.ThreadPoolExecutor) threadPool).getActiveCount() : 0
        );
    }
    
    /**
     * Obtém informações sobre clientes conectados.
     */
    public Set<String> getConnectedClients() {
        return activeClients.keySet();
    }
    
    /**
     * Desconecta um cliente específico.
     */
    public boolean disconnectClient(String clientKey) {
        TCPClientHandler client = activeClients.get(clientKey);
        if (client != null) {
            client.stop();
            activeClients.remove(clientKey);
            logger.info("Cliente TCP desconectado manualmente: " + clientKey);
            return true;
        }
        return false;
    }
}