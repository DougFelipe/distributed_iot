package br.ufrn.dimap.patterns.observer;

import br.ufrn.dimap.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para o HeartbeatSubject - Observer Pattern
 */
public class HeartbeatSubjectTest {

    private HeartbeatSubject heartbeatSubject;
    private TestHeartbeatObserver testObserver;
    
    @BeforeEach
    void setUp() {
        heartbeatSubject = new HeartbeatSubject(2); // 2 segundos timeout para testes rápidos
        testObserver = new TestHeartbeatObserver();
        heartbeatSubject.addObserver(testObserver);
    }
    
    @Test
    void testAddObserver() {
        TestHeartbeatObserver anotherObserver = new TestHeartbeatObserver();
        heartbeatSubject.addObserver(anotherObserver);
        
        Node node = new Node("test-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        // Ambos observers devem ser notificados
        assertTrue(testObserver.nodeRegisteredCalled);
        assertTrue(anotherObserver.nodeRegisteredCalled);
    }
    
    @Test
    void testRemoveObserver() {
        heartbeatSubject.removeObserver(testObserver);
        
        Node node = new Node("test-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        // Observer removido não deve ser notificado
        assertFalse(testObserver.nodeRegisteredCalled);
    }
    
    @Test
    void testRegisterNode() {
        Node node = new Node("test-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        assertTrue(testObserver.nodeRegisteredCalled);
        assertEquals(node, testObserver.lastRegisteredNode);
        
        List<Node> allNodes = heartbeatSubject.getAllNodes();
        assertEquals(1, allNodes.size());
        assertEquals(node, allNodes.get(0));
    }
    
    @Test
    void testUpdateHeartbeat() {
        Node node = new Node("test-node-1", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        // Reset flags do observer
        testObserver.reset();
        
        heartbeatSubject.updateHeartbeat(node.getId());
        
        assertTrue(testObserver.heartbeatReceivedCalled);
        assertEquals(node, testObserver.lastHeartbeatNode);
    }
    
    @Test
    void testUpdateHeartbeatNonExistentNode() {
        heartbeatSubject.updateHeartbeat("non-existent-node");
        
        // Não deve causar erro nem notificar observer
        assertFalse(testObserver.heartbeatReceivedCalled);
    }
    
    @Test
    void testGetActiveNodes() {
        Node node1 = new Node("active-node", "COMPONENT_A", "127.0.0.1", 8080);
        Node node2 = new Node("inactive-node", "COMPONENT_B", "127.0.0.1", 8081);
        
        heartbeatSubject.registerNode(node1);
        heartbeatSubject.registerNode(node2);
        
        // Simular que node2 está inativo
        node2.setActive(false);
        
        List<Node> activeNodes = heartbeatSubject.getActiveNodes();
        assertEquals(1, activeNodes.size());
        assertEquals(node1, activeNodes.get(0));
    }
    
    @Test
    @Timeout(5)
    void testHeartbeatTimeout() throws InterruptedException {
        Node node = new Node("timeout-node", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        // Reset observer
        testObserver.reset();
        
        // Aguardar timeout (2 segundos + margem)
        CountDownLatch latch = new CountDownLatch(1);
        testObserver.setFailureLatch(latch);
        
        boolean failureDetected = latch.await(4, TimeUnit.SECONDS);
        assertTrue(failureDetected, "Timeout de heartbeat não foi detectado");
        assertTrue(testObserver.nodeFailureCalled);
        assertEquals(node, testObserver.lastFailedNode);
        assertFalse(node.isActive());
    }
    
    @Test
    @Timeout(5)
    void testHeartbeatKeepAlive() throws InterruptedException {
        Node node = new Node("keepalive-node", "COMPONENT_A", "127.0.0.1", 8080);
        heartbeatSubject.registerNode(node);
        
        // Reset observer
        testObserver.reset();
        
        // Enviar heartbeats periodicamente
        for (int i = 0; i < 3; i++) {
            Thread.sleep(1500); // 1.5s - antes do timeout de 2s
            heartbeatSubject.updateHeartbeat(node.getId());
        }
        
        // Aguardar um pouco mais para garantir que não houve timeout
        Thread.sleep(1000);
        
        // Nó deve continuar ativo
        assertTrue(node.isActive());
        assertFalse(testObserver.nodeFailureCalled);
    }
    
    @Test
    void testMultipleNodesHeartbeat() {
        Node node1 = new Node("node-1", "COMPONENT_A", "127.0.0.1", 8080);
        Node node2 = new Node("node-2", "COMPONENT_B", "127.0.0.1", 8081);
        
        heartbeatSubject.registerNode(node1);
        heartbeatSubject.registerNode(node2);
        
        assertEquals(2, heartbeatSubject.getAllNodes().size());
        assertEquals(2, heartbeatSubject.getActiveNodes().size());
        
        // Atualizar heartbeat apenas do node1
        heartbeatSubject.updateHeartbeat(node1.getId());
        heartbeatSubject.updateHeartbeat(node2.getId());
        
        assertTrue(testObserver.heartbeatReceivedCalled);
    }
    
    @Test
    void testShutdown() {
        assertDoesNotThrow(() -> heartbeatSubject.shutdown());
    }
    
    /**
     * Observer de teste para capturar notificações
     */
    private static class TestHeartbeatObserver implements HeartbeatObserver {
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