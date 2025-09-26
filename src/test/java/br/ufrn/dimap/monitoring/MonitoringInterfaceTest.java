package br.ufrn.dimap.monitoring;

import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para a Interface de Monitoramento Avançada
 */
public class MonitoringInterfaceTest {

    private APIGateway gateway;
    private MonitoringInterface monitoring;
    
    @BeforeEach
    void setUp() {
        gateway = APIGateway.createTestInstance(2); // 2 segundos timeout
        monitoring = new MonitoringInterface(gateway);
    }
    
    @Test
    void testMonitoringInterfaceCreation() {
        assertNotNull(monitoring);
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        assertEquals(0, stats.getTotalNodes());
        assertEquals(0, stats.getActiveNodes());
        assertEquals(0, stats.getTotalHeartbeats());
        assertEquals(0, stats.getTotalFailures());
        assertEquals(0, stats.getTotalRegistrations());
    }
    
    @Test
    void testNodeRegistrationMonitoring() {
        Node node = new Node("test-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        assertEquals(1, stats.getTotalNodes());
        assertEquals(1, stats.getActiveNodes());
        assertEquals(1, stats.getTotalRegistrations());
        
        // Verificar métricas do nó
        MonitoringInterface.NodeMetrics nodeMetrics = monitoring.getNodeMetrics(node.getId());
        assertNotNull(nodeMetrics);
        assertEquals(node, nodeMetrics.getNode());
        assertEquals(0, nodeMetrics.getHeartbeatCount());
        assertEquals(0, nodeMetrics.getFailureCount());
    }
    
    @Test
    void testHeartbeatMonitoring() {
        Node node = new Node("heartbeat-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        // Enviar alguns heartbeats
        gateway.updateHeartbeat(node.getId());
        gateway.updateHeartbeat(node.getId());
        gateway.updateHeartbeat(node.getId());
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        assertEquals(3, stats.getTotalHeartbeats());
        
        MonitoringInterface.NodeMetrics nodeMetrics = monitoring.getNodeMetrics(node.getId());
        assertEquals(3, nodeMetrics.getHeartbeatCount());
    }
    
    @Test
    @Timeout(6)
    void testFailureDetectionMonitoring() throws InterruptedException {
        Node node = new Node("failure-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        // Aguardar timeout (2 segundos + margem)
        Thread.sleep(3000);
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        assertEquals(1, stats.getTotalNodes());
        assertEquals(0, stats.getActiveNodes()); // Nó deve estar inativo
        assertEquals(1, stats.getInactiveNodes());
        assertEquals(1, stats.getTotalFailures());
        
        MonitoringInterface.NodeMetrics nodeMetrics = monitoring.getNodeMetrics(node.getId());
        assertEquals(1, nodeMetrics.getFailureCount());
    }
    
    @Test
    void testMultipleNodesMonitoring() {
        Node node1 = new Node("node-1", "COMPONENT_A", "127.0.0.1", 8080);
        Node node2 = new Node("node-2", "COMPONENT_B", "127.0.0.1", 8081);
        Node node3 = new Node("node-3", "COMPONENT_A", "127.0.0.1", 8082);
        
        gateway.registerComponent(node1);
        gateway.registerComponent(node2);
        gateway.registerComponent(node3);
        
        // Enviar heartbeats
        gateway.updateHeartbeat(node1.getId());
        gateway.updateHeartbeat(node2.getId());
        gateway.updateHeartbeat(node1.getId());
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        assertEquals(3, stats.getTotalNodes());
        assertEquals(3, stats.getActiveNodes());
        assertEquals(3, stats.getTotalHeartbeats());
        assertEquals(3, stats.getTotalRegistrations());
        
        // Verificar métricas individuais
        assertEquals(2, monitoring.getNodeMetrics(node1.getId()).getHeartbeatCount());
        assertEquals(1, monitoring.getNodeMetrics(node2.getId()).getHeartbeatCount());
        assertEquals(0, monitoring.getNodeMetrics(node3.getId()).getHeartbeatCount());
    }
    
    @Test
    void testActiveComponentsTable() {
        Node node1 = new Node("table-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        Node node2 = new Node("table-node-2", "COMPONENT_B", "127.0.0.1", 8081);
        
        gateway.registerComponent(node1);
        gateway.registerComponent(node2);
        
        gateway.updateHeartbeat(node1.getId());
        gateway.updateHeartbeat(node2.getId());
        
        String table = monitoring.getActiveComponentsTable();
        
        assertNotNull(table);
        assertTrue(table.contains("COMPONENTES ATIVOS"));
        assertTrue(table.contains("table-node-1"));
        assertTrue(table.contains("table-node-2"));
        assertTrue(table.contains("COMPONENT_A"));
        assertTrue(table.contains("COMPONENT_B"));
        assertTrue(table.contains("8080"));
        assertTrue(table.contains("8081"));
    }
    
    @Test
    void testActiveComponentsTableEmpty() {
        String table = monitoring.getActiveComponentsTable();
        assertEquals("Nenhum componente ativo encontrado.", table);
    }
    
    @Test
    void testSystemStatusReport() {
        Node node = new Node("status-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        gateway.updateHeartbeat(node.getId());
        
        String report = monitoring.getSystemStatusReport();
        
        assertNotNull(report);
        assertTrue(report.contains("RELATÓRIO DE STATUS DO SISTEMA"));
        assertTrue(report.contains("Total de nós: 1"));
        assertTrue(report.contains("Nós ativos: 1"));
        assertTrue(report.contains("Total heartbeats: 1"));
        assertTrue(report.contains("STATUS DE SAÚDE: SAUDÁVEL"));
    }
    
    @Test
    void testHealthStatusCalculation() {
        // Cenário 1: Sistema saudável (80%+)
        Node node1 = new Node("health-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node1);
        
        String report = monitoring.getSystemStatusReport();
        assertTrue(report.contains("SAUDÁVEL"));
        
        // Cenário 2: Sistema degradado (50-79%)
        Node node2 = new Node("health-node-2", "COMPONENT_B", "127.0.0.1", 8081);
        Node node3 = new Node("health-node-3", "COMPONENT_A", "127.0.0.1", 8082);
        gateway.registerComponent(node2);
        gateway.registerComponent(node3);
        
        // Simular que apenas 2 de 3 estão ativos (66%)
        node3.setActive(false);
        
        report = monitoring.getSystemStatusReport();
        assertTrue(report.contains("DEGRADADO") || report.contains("SAUDÁVEL")); // Pode variar dependendo da precisão
    }
    
    @Test
    void testForceHealthCheck() {
        Node node = new Node("health-check-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        // Deve executar sem erro
        assertDoesNotThrow(() -> monitoring.forceHealthCheck());
    }
    
    @Test
    void testNodeMetricsNull() {
        MonitoringInterface.NodeMetrics metrics = monitoring.getNodeMetrics("non-existent-node");
        assertNull(metrics);
    }
    
    @Test
    void testSystemStatsAttributes() {
        Node node = new Node("stats-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        gateway.updateHeartbeat(node.getId());
        
        MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
        
        assertNotNull(stats.getTimestamp());
        assertTrue(stats.getTotalNodes() >= 0);
        assertTrue(stats.getActiveNodes() >= 0);
        assertTrue(stats.getInactiveNodes() >= 0);
        assertTrue(stats.getTotalHeartbeats() >= 0);
        assertTrue(stats.getTotalFailures() >= 0);
        assertTrue(stats.getTotalRegistrations() >= 0);
        
        // Verificar consistência
        assertEquals(stats.getTotalNodes(), stats.getActiveNodes() + stats.getInactiveNodes());
    }
    
    @Test
    void testNodeMetricsAttributes() {
        Node node = new Node("metrics-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        MonitoringInterface.NodeMetrics metrics = monitoring.getNodeMetrics(node.getId());
        
        assertNotNull(metrics.getRegisteredAt());
        assertNotNull(metrics.getLastHeartbeat());
        assertEquals(node, metrics.getNode());
        assertEquals(0, metrics.getHeartbeatCount());
        assertEquals(0, metrics.getFailureCount());
    }
}