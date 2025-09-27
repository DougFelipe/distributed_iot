package br.ufrn.dimap.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Representa um sensor IoT no sistema distribuído
 * Implementação nativa com suporte a Version Vector
 */
public class IoTSensor implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String sensorId;
    private final String nodeId;
    private final SensorType type;
    private final String location;
    private final ConcurrentHashMap<String, Integer> versionVector;
    private final AtomicInteger localClock;
    
    private volatile double currentValue;
    private volatile SensorStatus status;
    private volatile LocalDateTime lastUpdate;
    private volatile LocalDateTime lastHeartbeat;
    
    public enum SensorType {
        TEMPERATURE("°C", -50.0, 100.0),
        HUMIDITY("%", 0.0, 100.0),
        PRESSURE("hPa", 800.0, 1200.0),
        LIGHT("lux", 0.0, 100000.0),
        MOTION("bool", 0.0, 1.0),
        AIR_QUALITY("ppm", 0.0, 500.0),
        SOIL_MOISTURE("%", 0.0, 100.0),
        BATTERY("V", 0.0, 5.0);
        
        private final String unit;
        private final double minValue;
        private final double maxValue;
        
        SensorType(String unit, double minValue, double maxValue) {
            this.unit = unit;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        public String getUnit() { return unit; }
        public double getMinValue() { return minValue; }
        public double getMaxValue() { return maxValue; }
    }
    
    public enum SensorStatus {
        ACTIVE, INACTIVE, ERROR, MAINTENANCE, UNKNOWN
    }
    
    public IoTSensor(String sensorId, String nodeId, SensorType type, String location) {
        this.sensorId = sensorId;
        this.nodeId = nodeId;
        this.type = type;
        this.location = location;
        this.versionVector = new ConcurrentHashMap<>();
        this.localClock = new AtomicInteger(0);
        this.currentValue = 0.0;
        this.status = SensorStatus.INACTIVE;
        this.lastUpdate = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
        
        // Inicializar version vector com este nó
        this.versionVector.put(nodeId, 0);
    }
    
    // Simular leitura do sensor baseada no tipo
    public double readValue() {
        double simulatedValue;
        
        switch (type) {
            case TEMPERATURE:
                simulatedValue = 20.0 + (Math.random() * 15.0) - 7.5; // 12.5°C a 27.5°C
                break;
            case HUMIDITY:
                simulatedValue = 40.0 + (Math.random() * 40.0); // 40% a 80%
                break;
            case PRESSURE:
                simulatedValue = 1013.25 + (Math.random() * 50.0) - 25.0; // ~1013 hPa
                break;
            case LIGHT:
                simulatedValue = Math.random() * 1000.0; // 0 a 1000 lux
                break;
            case MOTION:
                simulatedValue = Math.random() > 0.9 ? 1.0 : 0.0; // 10% chance de movimento
                break;
            case AIR_QUALITY:
                simulatedValue = Math.random() * 100.0; // 0 a 100 ppm
                break;
            case SOIL_MOISTURE:
                simulatedValue = 30.0 + (Math.random() * 40.0); // 30% a 70%
                break;
            case BATTERY:
                simulatedValue = 3.0 + (Math.random() * 1.5); // 3.0V a 4.5V
                break;
            default:
                simulatedValue = Math.random() * 100.0;
        }
        
        this.currentValue = simulatedValue;
        this.lastUpdate = LocalDateTime.now();
        this.incrementVersionVector();
        
        return simulatedValue;
    }
    
    public void updateValue(double value) {
        this.currentValue = value;
        this.lastUpdate = LocalDateTime.now();
        this.incrementVersionVector();
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.status = SensorStatus.ACTIVE;
    }
    
    public void incrementVersionVector() {
        int newValue = localClock.incrementAndGet();
        versionVector.put(nodeId, newValue);
    }
    
    public void mergeVersionVector(ConcurrentHashMap<String, Integer> otherVector) {
        for (var entry : otherVector.entrySet()) {
            versionVector.merge(entry.getKey(), entry.getValue(), Integer::max);
        }
    }
    
    public IoTMessage createSensorDataMessage() {
        return new IoTMessage(
            sensorId,
            IoTMessage.MessageType.SENSOR_DATA,
            String.format("%.2f %s", currentValue, type.getUnit()),
            currentValue,
            type.name(),
            new ConcurrentHashMap<>(versionVector)
        );
    }
    
    public IoTMessage createHeartbeatMessage() {
        updateHeartbeat();
        return new IoTMessage(
            sensorId,
            IoTMessage.MessageType.HEARTBEAT,
            "HEARTBEAT_" + status.name(),
            currentValue,
            type.name(),
            new ConcurrentHashMap<>(versionVector)
        );
    }
    
    public IoTMessage createRegisterMessage() {
        return new IoTMessage(
            sensorId,
            IoTMessage.MessageType.SENSOR_REGISTER,
            String.format("REGISTER_%s_%s_%s", type.name(), location, nodeId),
            0.0,
            type.name(),
            new ConcurrentHashMap<>(versionVector)
        );
    }
    
    public boolean isHealthy() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return lastHeartbeat.isAfter(threshold) && status == SensorStatus.ACTIVE;
    }
    
    // Getters
    public String getSensorId() { return sensorId; }
    public String getNodeId() { return nodeId; }
    public SensorType getType() { return type; }
    public String getLocation() { return location; }
    public double getCurrentValue() { return currentValue; }
    public SensorStatus getStatus() { return status; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public ConcurrentHashMap<String, Integer> getVersionVector() { 
        return new ConcurrentHashMap<>(versionVector); 
    }
    public int getLocalClock() { return localClock.get(); }
    
    public void setStatus(SensorStatus status) { 
        this.status = status; 
        this.lastUpdate = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("IoTSensor{id='%s', type=%s, value=%.2f%s, status=%s, vv=%s}", 
                           sensorId, type, currentValue, type.getUnit(), status, versionVector);
    }
}