package br.ufrn.dimap.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * Mensagem nativa UDP com Version Vector para sensores IoT
 * Baseada nos exemplos UDP nativos com extensões para sistemas distribuídos
 */
public class IoTMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String messageId;
    private final String sensorId;
    private final MessageType type;
    private final String content;
    private final LocalDateTime timestamp;
    private final ConcurrentHashMap<String, Integer> versionVector;
    private final double sensorValue;
    private final String sensorType;
    
    public enum MessageType {
        SENSOR_REGISTER(1),
        SENSOR_DATA(2), 
        HEARTBEAT(3),
        DISCOVERY(4),
        ACK(5),
        SYNC(6);
        
        private final int code;
        
        MessageType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static MessageType fromCode(int code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Tipo de mensagem inválido: " + code);
        }
    }
    
    public IoTMessage(String sensorId, MessageType type, String content, 
                     double sensorValue, String sensorType, 
                     ConcurrentHashMap<String, Integer> versionVector) {
        this.messageId = generateMessageId();
        this.sensorId = sensorId;
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.sensorValue = sensorValue;
        this.sensorType = sensorType;
        this.versionVector = new ConcurrentHashMap<>(versionVector);
    }
    
    // Construtor para mensagens simples
    public IoTMessage(String sensorId, MessageType type, String content) {
        this(sensorId, type, content, 0.0, "GENERIC", new ConcurrentHashMap<>());
    }
    
    private static String generateMessageId() {
        return "IOT-MSG-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 10000);
    }
    
    // Getters
    public String getMessageId() { return messageId; }
    public String getSensorId() { return sensorId; }
    public String getSenderId() { return sensorId; } // Alias para compatibilidade
    public String getClientId() { return sensorId; } // Alias para compatibilidade
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public ConcurrentHashMap<String, Integer> getVersionVector() { 
        return new ConcurrentHashMap<>(versionVector); 
    }
    public double getSensorValue() { return sensorValue; }
    public String getSensorType() { return sensorType; }
    
    // Métodos para Version Vector
    public void incrementVersionVector(String nodeId) {
        versionVector.compute(nodeId, (k, v) -> (v == null) ? 1 : v + 1);
    }
    
    public void mergeVersionVector(ConcurrentHashMap<String, Integer> otherVector) {
        for (Map.Entry<String, Integer> entry : otherVector.entrySet()) {
            versionVector.merge(entry.getKey(), entry.getValue(), Integer::max);
        }
    }
    
    public boolean isConcurrentWith(IoTMessage other) {
        return !this.happensBefore(other) && !other.happensBefore(this);
    }
    
    public boolean happensBefore(IoTMessage other) {
        boolean hasSmaller = false;
        for (Map.Entry<String, Integer> entry : this.versionVector.entrySet()) {
            String nodeId = entry.getKey();
            int thisValue = entry.getValue();
            int otherValue = other.versionVector.getOrDefault(nodeId, 0);
            
            if (thisValue > otherValue) {
                return false;
            }
            if (thisValue < otherValue) {
                hasSmaller = true;
            }
        }
        
        for (Map.Entry<String, Integer> entry : other.versionVector.entrySet()) {
            String nodeId = entry.getKey();
            if (!this.versionVector.containsKey(nodeId) && entry.getValue() > 0) {
                hasSmaller = true;
            }
        }
        
        return hasSmaller;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IoTMessage that = (IoTMessage) o;
        return Objects.equals(messageId, that.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
    
    @Override
    public String toString() {
        return String.format("IoTMessage{id='%s', sensor='%s', type=%s, value=%.2f, vv=%s}", 
                           messageId, sensorId, type, sensorValue, versionVector);
    }
}