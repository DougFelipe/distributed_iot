package br.ufrn.dimap.communication.udp;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.patterns.strategy.MessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para UDPCommunicationStrategy
 */
class UDPCommunicationStrategyTest {
    
    private UDPCommunicationStrategy strategy;
    private static final int TEST_PORT = 9999;
    
    @BeforeEach
    void setUp() {
        strategy = new UDPCommunicationStrategy();
    }
    
    @AfterEach
    void tearDown() {
        if (strategy.isServerRunning()) {
            strategy.stopServer();
        }
    }
    
    @Test
    void testProtocolName() {
        assertEquals("UDP", strategy.getProtocolName());
    }
    
    @Test
    void testServerStartStop() throws Exception {
        assertFalse(strategy.isServerRunning());
        
        strategy.startServer(TEST_PORT);
        assertTrue(strategy.isServerRunning());
        
        strategy.stopServer();
        assertFalse(strategy.isServerRunning());
    }
    
    @Test
    void testMessageSendReceive() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message> receivedMessage = new AtomicReference<>();
        AtomicReference<String> senderHost = new AtomicReference<>();
        
        // Configura handler para capturar mensagem recebida
        strategy.setMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(Message message, String host, int port) {
                receivedMessage.set(message);
                senderHost.set(host);
                latch.countDown();
            }
        });
        
        // Inicia servidor
        strategy.startServer(TEST_PORT);
        Thread.sleep(100); // Aguarda servidor inicializar
        
        // Envia mensagem de teste
        Message testMessage = new Message("TEST", "Hello UDP", "test-sender");
        boolean sent = strategy.sendMessage(testMessage, "localhost", TEST_PORT);
        
        assertTrue(sent);
        
        // Aguarda recebimento
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Valida mensagem recebida
        assertNotNull(receivedMessage.get());
        assertEquals("TEST", receivedMessage.get().getType());
        assertEquals("Hello UDP", receivedMessage.get().getContent());
        assertEquals("test-sender", receivedMessage.get().getSenderId());
        assertEquals("127.0.0.1", senderHost.get());
    }
    
    @Test
    void testMultipleMessagesHandling() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        
        strategy.setMessageHandler((message, host, port) -> latch.countDown());
        strategy.startServer(TEST_PORT);
        Thread.sleep(100);
        
        // Envia múltiplas mensagens
        for (int i = 0; i < 3; i++) {
            Message msg = new Message("TEST", "Message " + i, "sender-" + i);
            strategy.sendMessage(msg, "localhost", TEST_PORT);
        }
        
        // Todas as mensagens devem ser recebidas
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    @Test
    void testSendToInvalidHost() {
        Message message = new Message("TEST", "content", "sender");
        boolean sent = strategy.sendMessage(message, "invalid-host-xyz", 9999);
        assertFalse(sent);
    }
}