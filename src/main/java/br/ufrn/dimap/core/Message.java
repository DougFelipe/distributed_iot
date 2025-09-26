package br.ufrn.dimap.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe que representa uma mensagem no sistema distribuído.
 * Contém informações básicas para comunicação entre componentes.
 */
public class Message {
    private final String id;
    private final String type;
    private final String content;
    private final String senderId;
    private final LocalDateTime timestamp;

    @JsonCreator
    public Message(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("content") String content,
            @JsonProperty("senderId") String senderId,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.senderId = senderId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Message(String type, String content, String senderId) {
        this(generateId(), type, content, senderId, LocalDateTime.now());
    }

    private static String generateId() {
        return "MSG-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getSenderId() {
        return senderId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", senderId='" + senderId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}