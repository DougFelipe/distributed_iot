package br.ufrn.dimap.components;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.core.SystemConfig;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.strategy.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe base para componentes distribuídos do sistema.
 * Implementa funcionalidades comuns como registro, heartbeat e comunicação.
 */
public abstract class BaseComponent implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(BaseComponent.class);
    
    protected final String componentId;
    protected final String componentType;
    protected final SystemConfig config;
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    protected CommunicationStrategy communicationStrategy;
    protected Node selfNode;
    protected final AtomicBoolean running = new AtomicBoolean(false);
    
    // Configuração do Gateway
    protected String gatewayHost = "localhost";
    protected int gatewayPort = 9090;
    
    public BaseComponent(String componentType, SystemConfig config) {
        this.componentType = componentType;
        this.config = config;
        this.componentId = componentType + "-" + System.currentTimeMillis();
        
        logger.info("Componente criado: {} [{}]", componentId, componentType);
    }
    
    /**
     * Configura a estratégia de comunicação
     */
    public void setCommunicationStrategy(CommunicationStrategy strategy) {
        this.communicationStrategy = strategy;
        this.communicationStrategy.setMessageHandler(this);
        logger.info("Estratégia de comunicação configurada: {}", strategy.getProtocolName());
    }
    
    /**
     * Inicia o componente
     */
    public void start(int port) throws Exception {
        if (communicationStrategy == null) {
            throw new IllegalStateException("Estratégia de comunicação não configurada");
        }
        
        // Inicia servidor
        communicationStrategy.startServer(port);
        selfNode = new Node(componentId, componentType, config.getHost(), port);
        running.set(true);
        
        // Registra no Gateway
        registerWithGateway();
        
        // Inicia heartbeat
        startHeartbeat();
        
        logger.info("Componente {} iniciado na porta {} usando protocolo {}", 
            componentId, port, communicationStrategy.getProtocolName());
    }
    
    /**
     * Para o componente
     */
    public void stop() {
        running.set(false);
        
        if (communicationStrategy != null) {
            communicationStrategy.stopServer();
        }
        
        scheduler.shutdown();
        logger.info("Componente {} parado", componentId);
    }
    
    /**
     * Registra este componente no API Gateway
     */
    private void registerWithGateway() {
        String registrationContent = String.format("%s:%s:%d", 
            componentId, componentType, selfNode.getPort());
        
        Message registerMessage = new Message("REGISTER", registrationContent, componentId);
        
        boolean sent = communicationStrategy.sendMessage(registerMessage, gatewayHost, gatewayPort);
        if (sent) {
            logger.info("Registro enviado ao Gateway {}:{}", gatewayHost, gatewayPort);
        } else {
            logger.error("Falha ao enviar registro ao Gateway");
        }
    }
    
    /**
     * Inicia envio periódico de heartbeat
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (running.get()) {
                sendHeartbeat();
            }
        }, 10, 15, TimeUnit.SECONDS); // Heartbeat a cada 15 segundos
        
        logger.info("Heartbeat iniciado para componente {}", componentId);
    }
    
    /**
     * Envia heartbeat para o Gateway
     */
    private void sendHeartbeat() {
        Message heartbeat = new Message("HEARTBEAT", "alive", componentId);
        boolean sent = communicationStrategy.sendMessage(heartbeat, gatewayHost, gatewayPort);
        
        if (sent) {
            logger.debug("Heartbeat enviado ao Gateway");
        } else {
            logger.warn("Falha ao enviar heartbeat ao Gateway");
        }
    }
    
    /**
     * Solicita descoberta de outros componentes
     */
    public void discoverComponents() {
        Message discoveryMessage = new Message("DISCOVERY", "request", componentId);
        boolean sent = communicationStrategy.sendMessage(discoveryMessage, gatewayHost, gatewayPort);
        
        if (sent) {
            logger.info("Solicitação de descoberta enviada ao Gateway");
        } else {
            logger.error("Falha ao solicitar descoberta ao Gateway");
        }
    }
    
    /**
     * Envia mensagem para outro componente via Gateway
     */
    public boolean sendMessageViaGateway(Message message) {
        return communicationStrategy.sendMessage(message, gatewayHost, gatewayPort);
    }
    
    // Implementação padrão do MessageHandler
    @Override
    public void handleMessage(Message message, String senderHost, int senderPort) {
        logger.info("Mensagem recebida no {} de {}:{} - Type: {}", 
            componentId, senderHost, senderPort, message.getType());
        
        switch (message.getType()) {
            case "REGISTER_ACK":
                handleRegistrationAck(message, senderHost, senderPort);
                break;
            case "DISCOVERY_RESPONSE":
                handleDiscoveryResponse(message, senderHost, senderPort);
                break;
            case "REQUEST":
                handleRequest(message, senderHost, senderPort);
                break;
            default:
                // Delega para implementação específica do componente
                handleSpecificMessage(message, senderHost, senderPort);
        }
    }
    
    private void handleRegistrationAck(Message message, String senderHost, int senderPort) {
        logger.info("Registro confirmado pelo Gateway: {}", message.getContent());
    }
    
    private void handleDiscoveryResponse(Message message, String senderHost, int senderPort) {
        logger.info("Resposta de descoberta recebida: {}", message.getContent());
        // Processa lista de componentes descobertos
        String[] components = message.getContent().split(";");
        for (String component : components) {
            if (!component.isEmpty()) {
                logger.info("Componente descoberto: {}", component);
            }
        }
    }
    
    private void handleRequest(Message message, String senderHost, int senderPort) {
        // Processa requisição recebida
        String response = processRequest(message.getContent());
        
        Message responseMessage = new Message("RESPONSE", response, componentId);
        communicationStrategy.sendMessage(responseMessage, senderHost, senderPort);
        
        logger.info("Requisição processada e resposta enviada para {}:{}", senderHost, senderPort);
    }
    
    /**
     * Método abstrato para processamento específico de mensagens
     */
    protected abstract void handleSpecificMessage(Message message, String senderHost, int senderPort);
    
    /**
     * Método abstrato para processamento de requisições
     */
    protected abstract String processRequest(String requestContent);
    
    // Getters
    public String getComponentId() {
        return componentId;
    }
    
    public String getComponentType() {
        return componentType;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public String getProtocolName() {
        return communicationStrategy != null ? communicationStrategy.getProtocolName() : "NONE";
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', type='%s', protocol='%s', running=%s}", 
            getClass().getSimpleName(), componentId, componentType, getProtocolName(), running.get());
    }
}