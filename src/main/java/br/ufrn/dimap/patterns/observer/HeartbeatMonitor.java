package br.ufrn.dimap.patterns.observer;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observer Pattern - Monitor de Heartbeat para sensores IoT
 * 
 * Implementação concreta do Observer que monitora:
 * - Heartbeat dos sensores
 * - Detecção de falhas
 * - Estatísticas de comunicação
 * - Alertas de timeout
 * 
 * Características:
 * - Thread-safe
 * - Timeout configurável
 * - Métricas em tempo real
 * - Logs estruturados
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public class HeartbeatMonitor implements IoTObserver {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatMonitor.class);
    
    private final ConcurrentHashMap<String, LocalDateTime> lastHeartbeat;
    private final ConcurrentHashMap<String, AtomicLong> messageCount;
    private final AtomicLong totalEvents;
    private final long timeoutSeconds;
    
    public HeartbeatMonitor(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.messageCount = new ConcurrentHashMap<>();
        this.totalEvents = new AtomicLong(0);
        
        logger.info("💓 HeartbeatMonitor criado (timeout: {}s)", timeoutSeconds);
    }
    
    @Override
    public void onIoTEvent(String eventType, Object eventData) {
        totalEvents.incrementAndGet();
        
        switch (eventType) {
            case "SENSOR_REGISTERED":
                handleSensorRegistered((IoTSensor) eventData);
                break;
                
            case "SENSOR_UNREGISTERED":
                handleSensorUnregistered((IoTSensor) eventData);
                break;
                
            case "MESSAGE_RECEIVED":
                handleMessageReceived((IoTMessage) eventData);
                break;
                
            default:
                logger.debug("👁️ Evento IoT observado: {} - {}", eventType, eventData);
        }
    }
    
    /**
     * Processa registro de novo sensor
     */
    private void handleSensorRegistered(IoTSensor sensor) {
        String sensorId = sensor.getSensorId();
        lastHeartbeat.put(sensorId, LocalDateTime.now());
        messageCount.put(sensorId, new AtomicLong(0));
        
        logger.info("💓 Monitoramento iniciado para sensor: {} ({})", 
                   sensorId, sensor.getType());
    }
    
    /**
     * Processa desregistro de sensor
     */
    private void handleSensorUnregistered(IoTSensor sensor) {
        String sensorId = sensor.getSensorId();
        lastHeartbeat.remove(sensorId);
        messageCount.remove(sensorId);
        
        logger.info("💔 Monitoramento removido para sensor: {}", sensorId);
    }
    
    /**
     * Processa mensagem recebida (atualiza heartbeat)
     */
    private void handleMessageReceived(IoTMessage message) {
        // Usar clientId como identificador se senderId não estiver disponível
        String senderId = extractSenderId(message);
        
        if (senderId != null) {
            lastHeartbeat.put(senderId, LocalDateTime.now());
            messageCount.computeIfAbsent(senderId, k -> new AtomicLong(0)).incrementAndGet();
            
            long totalMsgs = messageCount.get(senderId).get();
            logger.debug("💓 Heartbeat atualizado: {} (total: {}) - Tipo Msg: {} [Código: {}] - Valor: {} {}", 
                        senderId, totalMsgs, message.getType(), message.getType().getCode(),
                        message.getSensorValue(), message.getSensorType());
        }
    }
    
    /**
     * Extrai ID do remetente da mensagem
     */
    private String extractSenderId(IoTMessage message) {
        // Tentar diferentes campos para identificar o remetente
        if (message.getClientId() != null) {
            return message.getClientId();
        }
        
        // Fallback: usar timestamp + tipo como identificador único
        return "UNKNOWN-" + message.getTimestamp();
    }
    
    /**
     * Verifica sensores que estão com timeout
     */
    public void checkTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        
        lastHeartbeat.forEach((sensorId, lastSeen) -> {
            long secondsSinceLastSeen = java.time.Duration.between(lastSeen, now).getSeconds();
            
            if (secondsSinceLastSeen > timeoutSeconds) {
                logger.warn("⚠️ Sensor timeout detectado: {} ({}s sem heartbeat)", 
                           sensorId, secondsSinceLastSeen);
            }
        });
    }
    
    /**
     * Retorna estatísticas de monitoramento
     */
    public String getMonitoringStats() {
        return String.format(
            "HeartbeatMonitor Stats:\n" +
            "  Sensores monitorados: %d\n" +
            "  Total de eventos: %d\n" +
            "  Timeout configurado: %ds\n" +
            "  Mensagens por sensor: %s",
            lastHeartbeat.size(),
            totalEvents.get(),
            timeoutSeconds,
            getMessageCountSummary()
        );
    }
    
    /**
     * Resumo de contagem de mensagens por sensor
     */
    private String getMessageCountSummary() {
        StringBuilder sb = new StringBuilder("{");
        messageCount.forEach((sensorId, count) -> {
            sb.append(sensorId).append("=").append(count.get()).append(", ");
        });
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2); // Remove última vírgula
        }
        sb.append("}");
        return sb.toString();
    }
    
    // Getters para testes e integração
    public int getMonitoredSensorsCount() { return lastHeartbeat.size(); }
    public long getTotalEvents() { return totalEvents.get(); }
    public long getTimeoutSeconds() { return timeoutSeconds; }
}