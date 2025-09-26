package br.ufrn.dimap.patterns.singleton;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.patterns.observer.HeartbeatObserver;
import br.ufrn.dimap.patterns.observer.HeartbeatSubject;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação do padrão Singleton para o API Gateway.
 * Ponto único de entrada e controle do sistema distribuído.
 */
public class APIGateway implements HeartbeatObserver, MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(APIGateway.class);
    
    // Instância única (Singleton)
    private static volatile APIGateway instance;
    
    private final String id;
    private final ConcurrentHashMap<String, Node> registeredNodes;
    private final HeartbeatSubject heartbeatSubject;
    private CommunicationStrategy communicationStrategy;
    private boolean running;
    
    // Construtor privado para garantir Singleton
    private APIGateway() {
        this(30); // 30 segundos timeout padrão
    }
    
    // Construtor com timeout personalizado (para testes)
    protected APIGateway(int heartbeatTimeoutSeconds) {
        this.id = "API-GATEWAY-" + System.currentTimeMillis();
        this.registeredNodes = new ConcurrentHashMap<>();
        this.heartbeatSubject = new HeartbeatSubject(heartbeatTimeoutSeconds);
        this.running = false;
        
        // Registra-se como observer do heartbeat
        this.heartbeatSubject.addObserver(this);
        
        logger.info("API Gateway criado com ID: {} (timeout: {}s)", id, heartbeatTimeoutSeconds);
    }
    
    /**
     * Retorna a instância única do API Gateway (Singleton)
     */
    public static APIGateway getInstance() {
        if (instance == null) {
            synchronized (APIGateway.class) {
                if (instance == null) {
                    instance = new APIGateway();
                }
            }
        }
        return instance;
    }
    
    /**
     * Cria uma nova instância para testes (quebra temporariamente o singleton)
     * APENAS para uso em testes
     */
    public static APIGateway createTestInstance(int heartbeatTimeoutSeconds) {
        return new APIGateway(heartbeatTimeoutSeconds);
    }
    
    /**
     * Configura a estratégia de comunicação
     */
    public void setCommunicationStrategy(CommunicationStrategy strategy) {
        this.communicationStrategy = strategy;
        logger.info("Estratégia de comunicação configurada: {}", strategy.getProtocolName());
    }
    
    /**
     * Inicia o API Gateway
     */
    public void start(int port) throws Exception {
        if (communicationStrategy == null) {
            throw new IllegalStateException("Estratégia de comunicação não configurada");
        }
        
        // Configura este gateway como handler das mensagens
        communicationStrategy.setMessageHandler(this);
        communicationStrategy.startServer(port);
        running = true;
        
        logger.info("API Gateway iniciado na porta {} usando protocolo {}", 
            port, communicationStrategy.getProtocolName());
    }
    
    /**
     * Para o API Gateway
     */
    public void stop() {
        if (communicationStrategy != null) {
            communicationStrategy.stopServer();
        }
        heartbeatSubject.shutdown();
        running = false;
        
        logger.info("API Gateway parado");
    }
    
    /**
     * Registra um novo componente no sistema
     */
    public void registerComponent(Node node) {
        registeredNodes.put(node.getId(), node);
        heartbeatSubject.registerNode(node);
        
        logger.info("Componente registrado: {} [{}:{}]", 
            node.getId(), node.getHost(), node.getPort());
    }
    
    /**
     * Retorna todos os nós registrados
     */
    public List<Node> getRegisteredNodes() {
        return new ArrayList<>(registeredNodes.values());
    }
    
    /**
     * Retorna apenas nós ativos
     */
    public List<Node> getActiveNodes() {
        return heartbeatSubject.getActiveNodes();
    }
    
    /**
     * Atualiza heartbeat de um componente
     */
    public void updateComponentHeartbeat(String nodeId) {
        heartbeatSubject.updateHeartbeat(nodeId);
    }
    
    /**
     * Alias para updateComponentHeartbeat (compatibilidade)
     */
    public void updateHeartbeat(String nodeId) {
        updateComponentHeartbeat(nodeId);
    }
    
    /**
     * Retorna o HeartbeatSubject para acesso direto (necessário para testes)
     */
    public HeartbeatSubject getHeartbeatSubject() {
        return heartbeatSubject;
    }
    
    /**
     * Envia mensagem para um nó específico
     */
    public boolean sendMessageToNode(String nodeId, Message message) {
        Node node = registeredNodes.get(nodeId);
        if (node != null && node.isActive()) {
            return communicationStrategy.sendMessage(message, node.getHost(), node.getPort());
        }
        logger.warn("Tentativa de enviar mensagem para nó não encontrado ou inativo: {}", nodeId);
        return false;
    }
    
    /**
     * Roteia mensagem para um nó ativo disponível (balanceamento simples)
     */
    public boolean routeMessage(Message message) {
        List<Node> activeNodes = getActiveNodes();
        if (activeNodes.isEmpty()) {
            logger.warn("Nenhum nó ativo disponível para roteamento");
            return false;
        }
        
        // Balanceamento simples: round-robin baseado no hash da mensagem
        int index = Math.abs(message.getId().hashCode() % activeNodes.size());
        Node targetNode = activeNodes.get(index);
        
        boolean sent = communicationStrategy.sendMessage(message, targetNode.getHost(), targetNode.getPort());
        if (sent) {
            logger.debug("Mensagem roteada para nó: {} [{}:{}]", 
                targetNode.getId(), targetNode.getHost(), targetNode.getPort());
        }
        return sent;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public String getProtocolName() {
        return communicationStrategy != null ? communicationStrategy.getProtocolName() : "NONE";
    }
    
    // Implementação do HeartbeatObserver
    
    @Override
    public void onHeartbeatReceived(Node node) {
        logger.debug("Heartbeat recebido do nó: {}", node.getId());
    }
    
    @Override
    public void onNodeFailure(Node node) {
        logger.warn("Nó detectado como falho: {} - removendo da rotação", node.getId());
        // Aqui poderia implementar lógica de re-roteamento
    }
    
    @Override
    public void onNodeRegistered(Node node) {
        logger.info("Novo nó disponível para roteamento: {}", node.getId());
    }
    
    // Implementação do MessageHandler
    
    @Override
    public void handleMessage(Message message, String senderHost, int senderPort) {
        logger.info("Mensagem recebida no Gateway de {}:{} - Type: {}", 
            senderHost, senderPort, message.getType());
        
        switch (message.getType()) {
            case "REGISTER":
                handleNodeRegistration(message, senderHost, senderPort);
                break;
            case "HEARTBEAT":
                handleHeartbeat(message, senderHost, senderPort);
                break;
            case "DISCOVERY":
                handleDiscovery(message, senderHost, senderPort);
                break;
            case "REQUEST":
                handleRequest(message, senderHost, senderPort);
                break;
            default:
                logger.warn("Tipo de mensagem não reconhecido: {}", message.getType());
        }
    }
    
    private void handleNodeRegistration(Message message, String senderHost, int senderPort) {
        try {
            // Espera formato: "nodeId:nodeType:port"
            String[] parts = message.getContent().split(":");
            if (parts.length >= 3) {
                String nodeId = parts[0];
                String nodeType = parts[1];
                int nodePort = Integer.parseInt(parts[2]);
                
                Node node = new Node(nodeId, nodeType, senderHost, nodePort);
                registerComponent(node);
                
                // Responde com confirmação
                Message response = new Message("REGISTER_ACK", "OK", getId());
                communicationStrategy.sendMessage(response, senderHost, senderPort);
                
                logger.info("Nó registrado via UDP: {} [{}:{}]", nodeId, senderHost, nodePort);
            }
        } catch (Exception e) {
            logger.error("Erro ao processar registro de nó: {}", e.getMessage());
        }
    }
    
    private void handleHeartbeat(Message message, String senderHost, int senderPort) {
        String nodeId = message.getSenderId();
        updateComponentHeartbeat(nodeId);
        logger.debug("Heartbeat processado para nó: {}", nodeId);
    }
    
    private void handleDiscovery(Message message, String senderHost, int senderPort) {
        // Retorna lista de nós ativos
        List<Node> activeNodes = getActiveNodes();
        StringBuilder nodeList = new StringBuilder();
        for (Node node : activeNodes) {
            nodeList.append(node.getId()).append(":").append(node.getType())
                   .append(":").append(node.getAddress()).append(";");
        }
        
        Message response = new Message("DISCOVERY_RESPONSE", nodeList.toString(), getId());
        communicationStrategy.sendMessage(response, senderHost, senderPort);
        
        logger.debug("Resposta de descoberta enviada para {}:{}", senderHost, senderPort);
    }
    
    private void handleRequest(Message message, String senderHost, int senderPort) {
        // Roteia requisição para nó disponível
        boolean routed = routeMessage(message);
        if (!routed) {
            Message errorResponse = new Message("ERROR", "Nenhum nó disponível", getId());
            communicationStrategy.sendMessage(errorResponse, senderHost, senderPort);
        }
    }
    
    @Override
    public String toString() {
        return "APIGateway{" +
                "id='" + id + '\'' +
                ", protocol='" + getProtocolName() + '\'' +
                ", running=" + running +
                ", registeredNodes=" + registeredNodes.size() +
                ", activeNodes=" + getActiveNodes().size() +
                '}';
    }
}