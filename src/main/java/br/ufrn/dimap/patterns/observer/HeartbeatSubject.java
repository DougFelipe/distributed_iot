package br.ufrn.dimap.patterns.observer;

import br.ufrn.dimap.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Subject do padrão Observer para gerenciar heartbeats.
 * Monitora nós e notifica observers sobre mudanças de status.
 */
public class HeartbeatSubject {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatSubject.class);
    
    private final List<HeartbeatObserver> observers = new ArrayList<>();
    private final ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final int heartbeatTimeoutSeconds;
    
    public HeartbeatSubject(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        startHeartbeatMonitoring();
    }
    
    /**
     * Adiciona um observer para receber notificações
     */
    public void addObserver(HeartbeatObserver observer) {
        observers.add(observer);
        logger.debug("Observer adicionado: {}", observer.getClass().getSimpleName());
    }
    
    /**
     * Remove um observer
     */
    public void removeObserver(HeartbeatObserver observer) {
        observers.remove(observer);
        logger.debug("Observer removido: {}", observer.getClass().getSimpleName());
    }
    
    /**
     * Registra um novo nó no sistema
     */
    public void registerNode(Node node) {
        nodes.put(node.getId(), node);
        notifyNodeRegistered(node);
        logger.info("Nó registrado: {}", node);
    }
    
    /**
     * Atualiza o heartbeat de um nó
     */
    public void updateHeartbeat(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            node.updateHeartbeat();
            notifyHeartbeatReceived(node);
            logger.debug("Heartbeat atualizado para nó: {}", nodeId);
        } else {
            logger.warn("Tentativa de atualizar heartbeat para nó não registrado: {}", nodeId);
        }
    }
    
    /**
     * Retorna todos os nós registrados
     */
    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    /**
     * Retorna apenas nós ativos
     */
    public List<Node> getActiveNodes() {
        return nodes.values().stream()
                .filter(Node::isActive)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Inicia o monitoramento de heartbeat em background
     */
    private void startHeartbeatMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 
            heartbeatTimeoutSeconds, heartbeatTimeoutSeconds, TimeUnit.SECONDS);
        logger.info("Monitoramento de heartbeat iniciado (timeout: {}s)", heartbeatTimeoutSeconds);
    }
    
    /**
     * Verifica se algum nó perdeu o heartbeat
     */
    private void checkHeartbeats() {
        LocalDateTime now = LocalDateTime.now();
        
        for (Node node : nodes.values()) {
            if (node.isActive()) {
                long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(
                    node.getLastHeartbeat(), now);
                
                if (secondsSinceLastHeartbeat > heartbeatTimeoutSeconds) {
                    node.setActive(false);
                    notifyNodeFailure(node);
                    logger.warn("Nó detectado como falho: {} (último heartbeat: {}s atrás)", 
                        node.getId(), secondsSinceLastHeartbeat);
                }
            }
        }
    }
    
    private void notifyHeartbeatReceived(Node node) {
        for (HeartbeatObserver observer : observers) {
            try {
                observer.onHeartbeatReceived(node);
            } catch (Exception e) {
                logger.error("Erro ao notificar observer sobre heartbeat: {}", e.getMessage());
            }
        }
    }
    
    private void notifyNodeFailure(Node node) {
        for (HeartbeatObserver observer : observers) {
            try {
                observer.onNodeFailure(node);
            } catch (Exception e) {
                logger.error("Erro ao notificar observer sobre falha: {}", e.getMessage());
            }
        }
    }
    
    private void notifyNodeRegistered(Node node) {
        for (HeartbeatObserver observer : observers) {
            try {
                observer.onNodeRegistered(node);
            } catch (Exception e) {
                logger.error("Erro ao notificar observer sobre registro: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Para o monitoramento e libera recursos
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("Monitoramento de heartbeat finalizado");
    }
}