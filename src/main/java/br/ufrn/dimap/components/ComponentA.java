package br.ufrn.dimap.components;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.core.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação do Componente A do sistema distribuído.
 * Responsável por processamento de dados e comunicação com outros componentes.
 */
public class ComponentA extends BaseComponent {
    private static final Logger logger = LoggerFactory.getLogger(ComponentA.class);
    
    private final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    
    public ComponentA(SystemConfig config) {
        super("COMPONENT_A", config);
        initializeData();
    }
    
    /**
     * Inicializa alguns dados de exemplo
     */
    private void initializeData() {
        dataStore.put("status", "active");
        dataStore.put("startup_time", LocalDateTime.now().toString());
        dataStore.put("type", "processing_unit");
        logger.info("Dados iniciais carregados no Componente A");
    }
    
    @Override
    protected void handleSpecificMessage(Message message, String senderHost, int senderPort) {
        switch (message.getType()) {
            case "DATA_REQUEST":
                handleDataRequest(message, senderHost, senderPort);
                break;
            case "PROCESS_DATA":
                handleProcessData(message, senderHost, senderPort);
                break;
            case "PING":
                handlePing(message, senderHost, senderPort);
                break;
            default:
                logger.warn("Tipo de mensagem não suportado pelo Componente A: {}", message.getType());
        }
    }
    
    @Override
    protected String processRequest(String requestContent) {
        int requestId = requestCounter.incrementAndGet();
        logger.info("Processando requisição #{}: {}", requestId, requestContent);
        
        // Simula processamento
        try {
            Thread.sleep(100); // Simula tempo de processamento
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String result = String.format("Processado pelo %s - Request #%d: %s", 
            componentId, requestId, requestContent.toUpperCase());
        
        logger.info("Requisição #{} processada com sucesso", requestId);
        return result;
    }
    
    private void handleDataRequest(Message message, String senderHost, int senderPort) {
        String key = message.getContent();
        String value = dataStore.getOrDefault(key, "NOT_FOUND");
        
        Message response = new Message("DATA_RESPONSE", 
            String.format("%s=%s", key, value), componentId);
        communicationStrategy.sendMessage(response, senderHost, senderPort);
        
        logger.info("Dados enviados para {}:{} - {}={}", senderHost, senderPort, key, value);
    }
    
    private void handleProcessData(Message message, String senderHost, int senderPort) {
        String[] parts = message.getContent().split("=", 2);
        if (parts.length == 2) {
            String key = parts[0];
            String value = parts[1];
            dataStore.put(key, value);
            
            Message response = new Message("PROCESS_ACK", "Data stored successfully", componentId);
            communicationStrategy.sendMessage(response, senderHost, senderPort);
            
            logger.info("Dados armazenados: {}={}", key, value);
        }
    }
    
    private void handlePing(Message message, String senderHost, int senderPort) {
        Message pong = new Message("PONG", 
            String.format("ComponentA-%s-OK", componentId), componentId);
        communicationStrategy.sendMessage(pong, senderHost, senderPort);
        
        logger.debug("PING respondido para {}:{}", senderHost, senderPort);
    }
    
    /**
     * Envia dados para outro componente
     */
    public void sendDataToComponent(String key, String value) {
        String content = String.format("%s=%s", key, value);
        Message message = new Message("PROCESS_DATA", content, componentId);
        sendMessageViaGateway(message);
        
        logger.info("Dados enviados via Gateway: {}={}", key, value);
    }
    
    /**
     * Solicita dados de outro componente
     */
    public void requestDataFromComponent(String key) {
        Message message = new Message("DATA_REQUEST", key, componentId);
        sendMessageViaGateway(message);
        
        logger.info("Solicitação de dados enviada via Gateway: {}", key);
    }
    
    /**
     * Retorna estatísticas do componente
     */
    public String getStats() {
        return String.format("ComponentA Stats - Requests: %d, Data Items: %d, Status: %s", 
            requestCounter.get(), dataStore.size(), 
            running.get() ? "RUNNING" : "STOPPED");
    }
    
    /**
     * Retorna todos os dados armazenados
     */
    public ConcurrentHashMap<String, String> getAllData() {
        return new ConcurrentHashMap<>(dataStore);
    }
}