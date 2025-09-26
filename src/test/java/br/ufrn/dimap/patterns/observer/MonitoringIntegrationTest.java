package br.ufrn.dimap.patterns.observer;

import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para o sistema de monitoramento completo
 */
public class MonitoringIntegrationTest {

    private APIGateway gateway;
    private TestMonitoringObserver monitoringObserver;
    
    @BeforeEach
    void setUp() {
        gateway = APIGateway.createTestInstance(30); // 30 segundos timeout padrão
        monitoringObserver = new TestMonitoringObserver();
        
        // Adicionar observer adicional para capturar eventos
        gateway.getHeartbeatSubject().addObserver(monitoringObserver);
    }
    
    @AfterEach
    void tearDown() {
        if (gateway != null) {
            gateway.stop();
        }
    }
    
    @Test
    void testCompleteNodeLifecycle() {
        // 1. Registrar nó
        Node node = new Node("lifecycle-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        assertTrue(monitoringObserver.nodeRegisteredCalled);
        assertEquals(node, monitoringObserver.lastRegisteredNode);
        
        // 2. Verificar se está na lista de nós ativos
        List<Node> activeNodes = gateway.getActiveNodes();
        assertEquals(1, activeNodes.size());
        assertTrue(activeNodes.contains(node));
        
        // 3. Atualizar heartbeat
        monitoringObserver.reset();
        gateway.updateHeartbeat(node.getId());
        
        assertTrue(monitoringObserver.heartbeatReceivedCalled);
        assertEquals(node, monitoringObserver.lastHeartbeatNode);
        
        // 4. Verificar que ainda está ativo
        assertTrue(node.isActive());
    }
    
    @Test
    @Timeout(8)
    void testNodeFailureDetection() throws InterruptedException {
        // Usar timeout menor para teste mais rápido
        gateway.stop();
        gateway = APIGateway.createTestInstance(2); // 2 segundos timeout
        gateway.getHeartbeatSubject().addObserver(monitoringObserver);
        
        Node node = new Node("failure-node", "COMPONENT_A", "127.0.0.1", 8080);
        gateway.registerComponent(node);
        
        // Reset observer
        monitoringObserver.reset();
        
        // Aguardar detecção de falha
        CountDownLatch failureLatch = new CountDownLatch(1);
        monitoringObserver.setFailureLatch(failureLatch);
        
        boolean failureDetected = failureLatch.await(5, TimeUnit.SECONDS);
        assertTrue(failureDetected, "Falha de nó não foi detectada dentro do tempo esperado");
        
        assertTrue(monitoringObserver.nodeFailureCalled);
        assertEquals(node, monitoringObserver.lastFailedNode);
        assertFalse(node.isActive());
        
        // Nó falho não deve aparecer na lista de ativos
        List<Node> activeNodes = gateway.getActiveNodes();
        assertFalse(activeNodes.contains(node));
    }
    
    @Test
    @Timeout(8)
    void testMultipleNodesMonitoring() throws InterruptedException {
        // Usar timeout menor para teste
        gateway.stop();
        gateway = APIGateway.createTestInstance(3); // 3 segundos timeout
        gateway.getHeartbeatSubject().addObserver(monitoringObserver);
        
        Node node1 = new Node("multi-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        Node node2 = new Node("multi-node-2", "COMPONENT_B", "127.0.0.1", 8081);
        Node node3 = new Node("multi-node-3", "COMPONENT_A", "127.0.0.1", 8082);
        
        gateway.registerComponent(node1);
        gateway.registerComponent(node2);
        gateway.registerComponent(node3);
        
        assertEquals(3, gateway.getActiveNodes().size());
        
        // Manter node1 e node2 vivos, deixar node3 morrer
        for (int i = 0; i < 3; i++) {
            Thread.sleep(2000); // 2 segundos
            gateway.updateHeartbeat(node1.getId());
            gateway.updateHeartbeat(node2.getId());
            // Não atualizar node3
        }
        
        // Aguardar detecção de falha do node3
        Thread.sleep(2000);
        
        // Verificar status
        List<Node> activeNodes = gateway.getActiveNodes();
        assertEquals(2, activeNodes.size());
        assertTrue(activeNodes.contains(node1));
        assertTrue(activeNodes.contains(node2));
        assertFalse(activeNodes.contains(node3));
        
        assertTrue(node1.isActive());
        assertTrue(node2.isActive());
        assertFalse(node3.isActive());
    }
    
    @Test
    void testHeartbeatSubjectAccess() {
        HeartbeatSubject heartbeatSubject = gateway.getHeartbeatSubject();
        assertNotNull(heartbeatSubject);
        
        // Deve ser a mesma instância
        assertSame(heartbeatSubject, gateway.getHeartbeatSubject());
    }
    
    @Test
    void testObserverNotificationResilience() {
        // Adicionar observer que lança exceção
        HeartbeatObserver faultyObserver = new HeartbeatObserver() {
            @Override
            public void onHeartbeatReceived(Node node) {
                throw new RuntimeException("Teste de falha do observer");
            }
            
            @Override
            public void onNodeFailure(Node node) {
                throw new RuntimeException("Teste de falha do observer");
            }
            
            @Override
            public void onNodeRegistered(Node node) {
                throw new RuntimeException("Teste de falha do observer");
            }
        };
        
        gateway.getHeartbeatSubject().addObserver(faultyObserver);
        
        Node node = new Node("resilience-node", "COMPONENT_A", "127.0.0.1", 8080);
        
        // Sistema deve continuar funcionando mesmo com observer com falha
        assertDoesNotThrow(() -> {
            gateway.registerComponent(node);
            gateway.updateHeartbeat(node.getId());
        });
        
        // Observer bom deve continuar recebendo notificações
        assertTrue(monitoringObserver.nodeRegisteredCalled);
        assertTrue(monitoringObserver.heartbeatReceivedCalled);
    }
    
    /**
     * Observer de teste para monitoramento de integração
     */
    private static class TestMonitoringObserver implements HeartbeatObserver {
        boolean heartbeatReceivedCalled = false;
        boolean nodeFailureCalled = false;
        boolean nodeRegisteredCalled = false;
        
        Node lastHeartbeatNode = null;
        Node lastFailedNode = null;
        Node lastRegisteredNode = null;
        
        CountDownLatch failureLatch = null;
        
        @Override
        public void onHeartbeatReceived(Node node) {
            heartbeatReceivedCalled = true;
            lastHeartbeatNode = node;
        }
        
        @Override
        public void onNodeFailure(Node node) {
            nodeFailureCalled = true;
            lastFailedNode = node;
            if (failureLatch != null) {
                failureLatch.countDown();
            }
        }
        
        @Override
        public void onNodeRegistered(Node node) {
            nodeRegisteredCalled = true;
            lastRegisteredNode = node;
        }
        
        void reset() {
            heartbeatReceivedCalled = false;
            nodeFailureCalled = false;
            nodeRegisteredCalled = false;
            lastHeartbeatNode = null;
            lastFailedNode = null;
            lastRegisteredNode = null;
        }
        
        void setFailureLatch(CountDownLatch latch) {
            this.failureLatch = latch;
        }
    }
}