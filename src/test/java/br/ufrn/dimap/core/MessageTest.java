package br.ufrn.dimap.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Testes unitários para a classe Message
 */
class MessageTest {
    
    private Message message;
    
    @BeforeEach
    void setUp() {
        message = new Message("TEST", "Hello World", "sender-123");
    }
    
    @Test
    void testMessageCreation() {
        assertNotNull(message.getId());
        assertEquals("TEST", message.getType());
        assertEquals("Hello World", message.getContent());
        assertEquals("sender-123", message.getSenderId());
        assertNotNull(message.getTimestamp());
    }
    
    @Test
    void testMessageWithCustomTimestamp() {
        LocalDateTime customTime = LocalDateTime.now().minusHours(1);
        Message customMessage = new Message("MSG-1", "CUSTOM", "content", "sender", customTime);
        
        assertEquals(customTime, customMessage.getTimestamp());
    }
    
    @Test
    void testMessageEquality() {
        Message message1 = new Message("ID-1", "TYPE", "content", "sender", LocalDateTime.now());
        Message message2 = new Message("ID-1", "OTHER", "other", "other", LocalDateTime.now());
        
        // Mensagens são iguais se têm o mesmo ID
        assertEquals(message1, message2);
    }
    
    @Test
    void testToString() {
        String str = message.toString();
        assertTrue(str.contains("Message{"));
        assertTrue(str.contains("type='TEST'"));
        assertTrue(str.contains("senderId='sender-123'"));
    }
}