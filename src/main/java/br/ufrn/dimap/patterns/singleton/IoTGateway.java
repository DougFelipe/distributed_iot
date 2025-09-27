package br.ufrn.dimap.patterns.singleton;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import br.ufrn.dimap.patterns.strategy.CommunicationStrategy;
import br.ufrn.dimap.patterns.observer.IoTObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Singleton Pattern - API Gateway IoT como ponto único de entrada
 * 
 * Características:
 * - Instância única do coordenador central
 * - Thread-safe (double-checked locking)
 * - Integração com Strategy Pattern para protocolos
 * - Observer Pattern para monitoramento
 * - Proxy Pattern para roteamento
 * 
 * Responsabilidades:
 * - Coordenação central do sistema IoT
 * - Registro e descoberta de sensores
 * - Roteamento de mensagens
 * - Monitoramento de saúde do sistema
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public class IoTGateway {
    private static final Logger logger = LoggerFactory.getLogger(IoTGateway.class);
    
    // Singleton instance com double-checked locking
    private static volatile IoTGateway instance;
    private static final Object lock = new Object();
    
    // Strategy Pattern - Protocolo de comunicação atual
    private CommunicationStrategy communicationStrategy;
    
    // Estado do gateway
    private final String gatewayId;
    private final ConcurrentHashMap<String, IoTSensor> registeredSensors;
    private final ConcurrentHashMap<String, LocalDateTime> lastHeartbeat;
    private final AtomicLong totalMessages;
    private final List<IoTObserver> observers;
    private volatile boolean active;
    
    // Version Vector global do sistema
    private final ConcurrentHashMap<String, Long> globalVersionVector;
    
    /**
     * Construtor privado para Singleton
     */
    private IoTGateway() {
        this.gatewayId = "IOT-GATEWAY-" + System.currentTimeMillis();
        this.registeredSensors = new ConcurrentHashMap<>();
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.totalMessages = new AtomicLong(0);
        this.observers = new ArrayList<>();
        this.globalVersionVector = new ConcurrentHashMap<>();
        this.active = false;
        
        logger.info("🏭 IoT Gateway Singleton criado: {}", gatewayId);
    }
    
    /**
     * Obtém a instância única do Gateway (Thread-safe)
     */
    public static IoTGateway getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new IoTGateway();
                }
            }
        }
        return instance;
    }
    
    /**
     * Configura a estratégia de comunicação (Strategy Pattern)
     */
    public void setCommunicationStrategy(CommunicationStrategy strategy) {
        this.communicationStrategy = strategy;
        logger.info("🔧 Estratégia de comunicação configurada: {}", strategy.getProtocolName());
    }
    
    /**
     * Inicia o Gateway IoT
     */
    public synchronized void start(int port) throws Exception {
        if (active) {
            logger.warn("⚠️ Gateway IoT já está ativo");
            return;
        }
        
        if (communicationStrategy == null) {
            throw new IllegalStateException("Estratégia de comunicação não configurada");
        }
        
        // Iniciar estratégia de comunicação
        communicationStrategy.startServer(port);
        active = true;
        
        logger.info("🚀 IoT Gateway Singleton iniciado na porta {} usando {}", 
                   port, communicationStrategy.getProtocolName());
        logger.info("📡 Gateway ID: {}", gatewayId);
    }
    
    /**
     * Para o Gateway IoT
     */
    public synchronized void stop() {
        if (!active) {
            return;
        }
        
        if (communicationStrategy != null) {
            communicationStrategy.stopServer();
        }
        
        active = false;
        logger.info("🛑 IoT Gateway Singleton parado: {}", gatewayId);
    }
    
    /**
     * Registra um sensor IoT no gateway
     */
    public boolean registerSensor(IoTSensor sensor, String host, int port) {
        if (!active) {
            logger.warn("⚠️ Tentativa de registro com Gateway inativo");
            return false;
        }
        
        String sensorId = sensor.getSensorId();
        registeredSensors.put(sensorId, sensor);
        lastHeartbeat.put(sensorId, LocalDateTime.now());
        
        // Inicializar Version Vector para o sensor
        globalVersionVector.put(sensorId, 0L);
        
        logger.info("✅ Sensor registrado: {} tipo: {} endereço: {}:{}", 
                   sensorId, sensor.getType(), host, port);
        
        // Notificar observers
        notifyObservers("SENSOR_REGISTERED", sensor);
        
        return true;
    }
    
    /**
     * Remove sensor do registry
     */
    public void unregisterSensor(String sensorId) {
        IoTSensor removed = registeredSensors.remove(sensorId);
        lastHeartbeat.remove(sensorId);
        globalVersionVector.remove(sensorId);
        
        if (removed != null) {
            logger.info("🗑️ Sensor removido: {}", sensorId);
            notifyObservers("SENSOR_UNREGISTERED", removed);
        }
    }
    
    /**
     * Proxy Pattern - Roteia mensagem para sensor específico
     */
    public boolean routeMessageToSensor(String sensorId, IoTMessage message, String host, int port) {
        if (!active || communicationStrategy == null) {
            return false;
        }
        
        IoTSensor sensor = registeredSensors.get(sensorId);
        if (sensor == null) {
            logger.warn("⚠️ Tentativa de roteamento para sensor inexistente: {}", sensorId);
            return false;
        }
        
        // Atualizar Version Vector
        updateVersionVector(message);
        
        // Log do roteamento (Proxy Pattern)
        logger.debug("🔄 Proxy: Roteando mensagem para sensor {} ({}:{})", sensorId, host, port);
        
        return communicationStrategy.sendMessage(message, host, port);
    }
    
    /**
     * Processa mensagem recebida de sensor
     */
    public void processIncomingMessage(IoTMessage message, String senderHost, int senderPort) {
        totalMessages.incrementAndGet();
        
        // Atualizar heartbeat
        if (message.getSenderId() != null) {
            lastHeartbeat.put(message.getSenderId(), LocalDateTime.now());
        }
        
        // Atualizar Version Vector global
        updateVersionVector(message);
        
        logger.debug("📨 Mensagem processada de {}:{} - Tipo: {} [Código: {}] - ID: {} - Sensor: {} - Valor: {} {} - Total Msgs: {}", 
                    senderHost, senderPort, message.getType(), message.getType().getCode(),
                    message.getMessageId(), message.getSensorId(), message.getSensorValue(), 
                    message.getSensorType(), totalMessages.get());
        
        // Notificar observers
        notifyObservers("MESSAGE_RECEIVED", message);
    }
    
    /**
     * Atualiza Version Vector global
     */
    private void updateVersionVector(IoTMessage message) {
        if (message.getVersionVector() != null) {
            // Merge dos version vectors (converter Integer para Long)
            message.getVersionVector().forEach((senderId, version) -> {
                globalVersionVector.merge(senderId, version.longValue(), Long::max);
            });
        }
    }
    
    /**
     * Observer Pattern - Adiciona observer
     */
    public void addObserver(IoTObserver observer) {
        synchronized (observers) {
            observers.add(observer);
            logger.debug("👁️ Observer adicionado: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Observer Pattern - Remove observer
     */
    public void removeObserver(IoTObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }
    
    /**
     * Notifica todos os observers
     */
    private void notifyObservers(String eventType, Object eventData) {
        synchronized (observers) {
            for (IoTObserver observer : observers) {
                try {
                    observer.onIoTEvent(eventType, eventData);
                } catch (Exception e) {
                    logger.error("❌ Erro ao notificar observer: {}", e.getMessage());
                }
            }
        }
    }
    
    // Getters para estatísticas
    public String getGatewayId() { return gatewayId; }
    public int getRegisteredSensorsCount() { return registeredSensors.size(); }
    public long getTotalMessages() { return totalMessages.get(); }
    public boolean isActive() { return active; }
    public ConcurrentHashMap<String, Long> getGlobalVersionVector() { return new ConcurrentHashMap<>(globalVersionVector); }
    
    /**
     * Estatísticas detalhadas do Gateway
     */
    public String getDetailedStats() {
        return String.format(
            "IoT Gateway Singleton Stats:\n" +
            "  Gateway ID: %s\n" +
            "  Active: %s\n" +
            "  Protocol: %s\n" +
            "  Registered Sensors: %d\n" +
            "  Total Messages: %d\n" +
            "  Observers: %d\n" +
            "  Version Vector: %s",
            gatewayId, active, 
            communicationStrategy != null ? communicationStrategy.getProtocolName() : "NONE",
            registeredSensors.size(), totalMessages.get(), observers.size(),
            globalVersionVector.toString()
        );
    }
}