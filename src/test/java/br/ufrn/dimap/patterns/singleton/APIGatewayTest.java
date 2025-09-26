package br.ufrn.dimap.patterns.singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o padrão Singleton do API Gateway
 */
class APIGatewayTest {
    
    @BeforeEach
    void setUp() {
        // Note: Não podemos "resetar" um singleton real, mas podemos testar seu comportamento
    }
    
    @Test
    void testSingletonInstance() {
        APIGateway gateway1 = APIGateway.getInstance();
        APIGateway gateway2 = APIGateway.getInstance();
        
        // Deve ser a mesma instância
        assertSame(gateway1, gateway2);
        assertNotNull(gateway1.getId());
        assertNotNull(gateway2.getId());
        assertEquals(gateway1.getId(), gateway2.getId());
    }
    
    @Test
    void testInitialState() {
        APIGateway gateway = APIGateway.getInstance();
        
        assertFalse(gateway.isRunning());
        assertEquals(0, gateway.getRegisteredNodes().size());
        assertEquals(0, gateway.getActiveNodes().size());
        assertNotNull(gateway.getId());
        assertTrue(gateway.getId().startsWith("API-GATEWAY-"));
    }
    
    @Test
    void testToString() {
        APIGateway gateway = APIGateway.getInstance();
        String str = gateway.toString();
        
        assertTrue(str.contains("APIGateway{"));
        assertTrue(str.contains("running=false"));
        assertTrue(str.contains("registeredNodes=0"));
    }
}