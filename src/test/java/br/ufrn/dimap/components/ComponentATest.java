package br.ufrn.dimap.components;

import br.ufrn.dimap.communication.udp.UDPCommunicationStrategy;
import br.ufrn.dimap.core.SystemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para ComponentA
 */
class ComponentATest {
    
    private ComponentA componentA;
    private SystemConfig config;
    
    @BeforeEach
    void setUp() {
        config = new SystemConfig();
        componentA = new ComponentA(config);
    }
    
    @AfterEach
    void tearDown() {
        if (componentA.isRunning()) {
            componentA.stop();
        }
    }
    
    @Test
    void testComponentCreation() {
        assertNotNull(componentA.getComponentId());
        assertTrue(componentA.getComponentId().startsWith("COMPONENT_A"));
        assertEquals("COMPONENT_A", componentA.getComponentType());
        assertFalse(componentA.isRunning());
    }
    
    @Test
    void testComponentStartStop() throws Exception {
        componentA.setCommunicationStrategy(new UDPCommunicationStrategy());
        
        assertFalse(componentA.isRunning());
        
        componentA.start(8091);
        assertTrue(componentA.isRunning());
        assertEquals("UDP", componentA.getProtocolName());
        
        componentA.stop();
        assertFalse(componentA.isRunning());
    }
    
    @Test
    void testProcessRequest() {
        String result = componentA.processRequest("test request");
        assertNotNull(result);
        assertTrue(result.contains("TEST REQUEST"));
        assertTrue(result.contains("Request #1"));
    }
    
    @Test
    void testGetStats() {
        String stats = componentA.getStats();
        assertNotNull(stats);
        assertTrue(stats.contains("ComponentA Stats"));
        assertTrue(stats.contains("Requests: 0"));
        assertTrue(stats.contains("STOPPED"));
    }
    
    @Test
    void testGetAllData() {
        var data = componentA.getAllData();
        assertNotNull(data);
        assertTrue(data.containsKey("status"));
        assertTrue(data.containsKey("type"));
        assertEquals("active", data.get("status"));
        assertEquals("processing_unit", data.get("type"));
    }
}