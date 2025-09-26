package br.ufrn.dimap.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa um nó/componente no sistema distribuído.
 * Contém informações de identificação e localização do componente.
 */
public class Node {
    private final String id;
    private final String type;
    private final String host;
    private final int port;
    private final LocalDateTime registeredAt;
    private LocalDateTime lastHeartbeat;
    private boolean active;

    @JsonCreator
    public Node(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("host") String host,
            @JsonProperty("port") int port,
            @JsonProperty("registeredAt") LocalDateTime registeredAt) {
        this.id = id;
        this.type = type;
        this.host = host;
        this.port = port;
        this.registeredAt = registeredAt != null ? registeredAt : LocalDateTime.now();
        this.lastHeartbeat = this.registeredAt;
        this.active = true;
    }

    public Node(String id, String type, String host, int port) {
        this(id, type, host, port, LocalDateTime.now());
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", address='" + getAddress() + '\'' +
                ", active=" + active +
                ", lastHeartbeat=" + lastHeartbeat +
                '}';
    }
}