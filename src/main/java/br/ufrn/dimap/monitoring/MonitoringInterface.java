package br.ufrn.dimap.monitoring;

import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.patterns.observer.HeartbeatObserver;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interface de monitoramento avançada para o sistema distribuído.
 * Implementa funcionalidades da Sprint 3: Sistema de Monitoramento e Heartbeat.
 */
public class MonitoringInterface implements HeartbeatObserver {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringInterface.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final APIGateway gateway;
    private final Map<String, NodeMetrics> nodeMetrics;
    private final AtomicInteger totalHeartbeats;
    private final AtomicInteger totalFailures;
    private final AtomicInteger totalRegistrations;
    
    public MonitoringInterface(APIGateway gateway) {
        this.gateway = gateway;
        this.nodeMetrics = new ConcurrentHashMap<>();
        this.totalHeartbeats = new AtomicInteger(0);
        this.totalFailures = new AtomicInteger(0);
        this.totalRegistrations = new AtomicInteger(0);
        
        // Registrar-se como observer
        gateway.getHeartbeatSubject().addObserver(this);
        
        logger.info("Interface de monitoramento iniciada");
    }
    
    @Override
    public void onHeartbeatReceived(Node node) {
        NodeMetrics metrics = nodeMetrics.computeIfAbsent(node.getId(), k -> new NodeMetrics(node));
        metrics.recordHeartbeat();
        totalHeartbeats.incrementAndGet();
        
        logger.debug("Heartbeat registrado: {} (total: {})", node.getId(), metrics.getHeartbeatCount());
    }
    
    @Override
    public void onNodeFailure(Node node) {
        NodeMetrics metrics = nodeMetrics.get(node.getId());
        if (metrics != null) {
            metrics.recordFailure();
        }
        totalFailures.incrementAndGet();
        
        logger.warn("FALHA DETECTADA: Nó {} está inativo (Falhas totais: {})", 
            node.getId(), totalFailures.get());
    }
    
    @Override
    public void onNodeRegistered(Node node) {
        NodeMetrics metrics = new NodeMetrics(node);
        nodeMetrics.put(node.getId(), metrics);
        totalRegistrations.incrementAndGet();
        
        logger.info("NOVO NÓ REGISTRADO: {} [{}:{}] (Total registros: {})", 
            node.getId(), node.getHost(), node.getPort(), totalRegistrations.get());
    }
    
    /**
     * Retorna estatísticas completas do sistema
     */
    public SystemStats getSystemStats() {
        List<Node> activeNodes = gateway.getActiveNodes();
        List<Node> allNodes = gateway.getHeartbeatSubject().getAllNodes();
        
        return new SystemStats(
            allNodes.size(),
            activeNodes.size(),
            allNodes.size() - activeNodes.size(),
            totalHeartbeats.get(),
            totalFailures.get(),
            totalRegistrations.get(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Retorna métricas de um nó específico
     */
    public NodeMetrics getNodeMetrics(String nodeId) {
        return nodeMetrics.get(nodeId);
    }
    
    /**
     * Retorna tabela de componentes ativos formatada
     */
    public String getActiveComponentsTable() {
        List<Node> activeNodes = gateway.getActiveNodes();
        if (activeNodes.isEmpty()) {
            return "Nenhum componente ativo encontrado.";
        }
        
        StringBuilder table = new StringBuilder();
        table.append("\n=== COMPONENTES ATIVOS ===\n");
        table.append(String.format("%-20s %-15s %-8s %-15s %-12s%n", 
            "ID", "TIPO", "PORTA", "HOST", "HEARTBEATS"));
        table.append("-".repeat(75)).append("\n");
        
        for (Node node : activeNodes) {
            NodeMetrics metrics = nodeMetrics.get(node.getId());
            int heartbeatCount = metrics != null ? metrics.getHeartbeatCount() : 0;
            
            table.append(String.format("%-20s %-15s %-8d %-15s %-12d%n",
                truncate(node.getId(), 20),
                node.getType(),
                node.getPort(),
                node.getHost(),
                heartbeatCount
            ));
        }
        
        return table.toString();
    }
    
    /**
     * Retorna relatório de status do sistema
     */
    public String getSystemStatusReport() {
        SystemStats stats = getSystemStats();
        
        StringBuilder report = new StringBuilder();
        report.append("\n=== RELATÓRIO DE STATUS DO SISTEMA ===\n");
        report.append("Timestamp: ").append(stats.getTimestamp().format(TIMESTAMP_FORMAT)).append("\n");
        report.append("Protocolo ativo: ").append(gateway.getProtocolName()).append("\n\n");
        
        report.append("ESTATÍSTICAS GERAIS:\n");
        report.append("  • Total de nós: ").append(stats.getTotalNodes()).append("\n");
        report.append("  • Nós ativos: ").append(stats.getActiveNodes()).append("\n");
        report.append("  • Nós inativos: ").append(stats.getInactiveNodes()).append("\n");
        report.append("  • Total heartbeats: ").append(stats.getTotalHeartbeats()).append("\n");
        report.append("  • Total falhas: ").append(stats.getTotalFailures()).append("\n");
        report.append("  • Total registros: ").append(stats.getTotalRegistrations()).append("\n\n");
        
        // Status de saúde
        double healthRatio = stats.getTotalNodes() > 0 ? 
            (double) stats.getActiveNodes() / stats.getTotalNodes() : 0;
        
        report.append("STATUS DE SAÚDE: ");
        if (healthRatio >= 0.8) {
            report.append("SAUDÁVEL ✓");
        } else if (healthRatio >= 0.5) {
            report.append("DEGRADADO ⚠");
        } else {
            report.append("CRÍTICO ✗");
        }
        report.append(" (").append(String.format("%.1f%%", healthRatio * 100)).append(")\n");
        
        return report.toString();
    }
    
    /**
     * Força verificação de componentes ativos (útil para testes)
     */
    public void forceHealthCheck() {
        List<Node> allNodes = gateway.getHeartbeatSubject().getAllNodes();
        logger.info("Executando verificação forçada de saúde para {} nós", allNodes.size());
        
        for (Node node : allNodes) {
            logger.debug("Nó {}: {} (último heartbeat: {})", 
                node.getId(), 
                node.isActive() ? "ATIVO" : "INATIVO",
                node.getLastHeartbeat().format(TIMESTAMP_FORMAT)
            );
        }
    }
    
    private String truncate(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
    
    /**
     * Classe para métricas de nó individual
     */
    public static class NodeMetrics {
        private final Node node;
        private final LocalDateTime registeredAt;
        private final AtomicInteger heartbeatCount;
        private final AtomicInteger failureCount;
        private LocalDateTime lastHeartbeat;
        
        public NodeMetrics(Node node) {
            this.node = node;
            this.registeredAt = LocalDateTime.now();
            this.heartbeatCount = new AtomicInteger(0);
            this.failureCount = new AtomicInteger(0);
            this.lastHeartbeat = this.registeredAt;
        }
        
        public void recordHeartbeat() {
            heartbeatCount.incrementAndGet();
            lastHeartbeat = LocalDateTime.now();
        }
        
        public void recordFailure() {
            failureCount.incrementAndGet();
        }
        
        // Getters
        public Node getNode() { return node; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public int getHeartbeatCount() { return heartbeatCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
        public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    }
    
    /**
     * Classe para estatísticas do sistema
     */
    public static class SystemStats {
        private final int totalNodes;
        private final int activeNodes;
        private final int inactiveNodes;
        private final int totalHeartbeats;
        private final int totalFailures;
        private final int totalRegistrations;
        private final LocalDateTime timestamp;
        
        public SystemStats(int totalNodes, int activeNodes, int inactiveNodes, 
                          int totalHeartbeats, int totalFailures, int totalRegistrations,
                          LocalDateTime timestamp) {
            this.totalNodes = totalNodes;
            this.activeNodes = activeNodes;
            this.inactiveNodes = inactiveNodes;
            this.totalHeartbeats = totalHeartbeats;
            this.totalFailures = totalFailures;
            this.totalRegistrations = totalRegistrations;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getTotalNodes() { return totalNodes; }
        public int getActiveNodes() { return activeNodes; }
        public int getInactiveNodes() { return inactiveNodes; }
        public int getTotalHeartbeats() { return totalHeartbeats; }
        public int getTotalFailures() { return totalFailures; }
        public int getTotalRegistrations() { return totalRegistrations; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}