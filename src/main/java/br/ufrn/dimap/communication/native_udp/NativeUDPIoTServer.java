package br.ufrn.dimap.communication.native_udp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servidor UDP nativo para sensores IoT
 * Implementação nativa baseada nos exemplos UDP
 */
public class NativeUDPIoTServer {
    private static final Logger logger = LoggerFactory.getLogger(NativeUDPIoTServer.class);
    
    private final int port;
    private final String serverId;
    private final ConcurrentHashMap<String, IoTSensor> registeredSensors;
    private final ConcurrentHashMap<String, InetSocketAddress> sensorAddresses;
    private final ConcurrentHashMap<String, LocalDateTime> lastHeartbeats;
    private final ConcurrentHashMap<String, Integer> versionVector;
    
    private DatagramSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    private ExecutorService messageProcessor;
    private ScheduledExecutorService maintenanceScheduler;
    
    public NativeUDPIoTServer(int port) {
        this.port = port;
        this.serverId = "IOT-SERVER-" + System.currentTimeMillis();
        this.registeredSensors = new ConcurrentHashMap<>();
        this.sensorAddresses = new ConcurrentHashMap<>();
        this.lastHeartbeats = new ConcurrentHashMap<>();
        this.versionVector = new ConcurrentHashMap<>();
        
        // Pool de threads nativo para processamento de mensagens
        this.messageProcessor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
        this.maintenanceScheduler = Executors.newScheduledThreadPool(2);
        
        logger.info("🏭 Servidor UDP IoT nativo criado: {} porta: {}", serverId, port);
    }
    
    public void start() throws IOException {
        if (running.get()) {
            logger.warn("⚠️ Servidor já está em execução");
            return;
        }
        
        serverSocket = new DatagramSocket(port);
        running.set(true);
        
        logger.info("🚀 Servidor UDP IoT iniciado na porta {}", port);
        logger.info("📡 Aguardando mensagens de sensores IoT...");
        
        // Iniciar tarefas de manutenção
        startMaintenanceTasks();
        
        // Loop principal do servidor
        while (running.get()) {
            try {
                byte[] receiveBuffer = new byte[4096];
                DatagramPacket receivePacket = new DatagramPacket(
                    receiveBuffer, receiveBuffer.length
                );
                
                serverSocket.receive(receivePacket);
                totalMessages.incrementAndGet();
                
                // Processar mensagem em thread separada
                messageProcessor.submit(() -> 
                    processMessage(receivePacket)
                );
                
            } catch (SocketTimeoutException e) {
                // Timeout normal, continuar
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("❌ Erro ao receber mensagem UDP: {}", e.getMessage());
                    errorCount.incrementAndGet();
                }
            }
        }
    }
    
    private void processMessage(DatagramPacket packet) {
        try {
            // Deserializar mensagem IoT
            IoTMessage message = deserializeMessage(packet.getData());
            InetSocketAddress senderAddress = new InetSocketAddress(
                packet.getAddress(), packet.getPort()
            );
            
            logger.debug("📨 Mensagem recebida: {} de {}", message.getType(), senderAddress);
            
            // Atualizar version vector
            message.getVersionVector().forEach((nodeId, clock) -> 
                versionVector.merge(nodeId, clock, Integer::max)
            );
            
            // Processar baseado no tipo
            switch (message.getType()) {
                case SENSOR_REGISTER:
                    processSensorRegister(message, senderAddress);
                    break;
                case SENSOR_DATA:
                    processSensorData(message, senderAddress);
                    break;
                case HEARTBEAT:
                    processHeartbeat(message, senderAddress);
                    break;
                case DISCOVERY:
                    processDiscovery(message, senderAddress);
                    break;
                case SYNC:
                    processSync(message, senderAddress);
                    break;
                default:
                    logger.warn("⚠️ Tipo de mensagem desconhecido: {}", message.getType());
            }
            
            // Enviar ACK se necessário
            if (message.getType() != IoTMessage.MessageType.ACK) {
                sendAck(message, senderAddress);
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro ao processar mensagem: {}", e.getMessage(), e);
            errorCount.incrementAndGet();
        }
    }
    
    private void processSensorRegister(IoTMessage message, InetSocketAddress senderAddress) {
        String sensorId = message.getSensorId();
        
        // Criar sensor baseado na mensagem
        IoTSensor.SensorType sensorType;
        try {
            sensorType = IoTSensor.SensorType.valueOf(message.getSensorType());
        } catch (IllegalArgumentException e) {
            sensorType = IoTSensor.SensorType.TEMPERATURE; // Default
        }
        
        IoTSensor sensor = new IoTSensor(
            sensorId, 
            extractNodeId(message.getContent()),
            sensorType,
            extractLocation(message.getContent())
        );
        
        sensor.mergeVersionVector(message.getVersionVector());
        sensor.setStatus(IoTSensor.SensorStatus.ACTIVE);
        
        registeredSensors.put(sensorId, sensor);
        sensorAddresses.put(sensorId, senderAddress);
        lastHeartbeats.put(sensorId, LocalDateTime.now());
        
        logger.info("✅ Sensor registrado: {} tipo: {} endereço: {}", 
                   sensorId, sensorType, senderAddress);
    }
    
    private void processSensorData(IoTMessage message, InetSocketAddress senderAddress) {
        String sensorId = message.getSensorId();
        IoTSensor sensor = registeredSensors.get(sensorId);
        
        if (sensor != null) {
            sensor.updateValue(message.getSensorValue());
            sensor.mergeVersionVector(message.getVersionVector());
            sensor.updateHeartbeat();
            
            logger.debug("📊 Dados do sensor {}: {:.2f} {}", 
                        sensorId, message.getSensorValue(), sensor.getType().getUnit());
        } else {
            logger.warn("⚠️ Dados recebidos de sensor não registrado: {}", sensorId);
        }
    }
    
    private void processHeartbeat(IoTMessage message, InetSocketAddress senderAddress) {
        String sensorId = message.getSensorId();
        lastHeartbeats.put(sensorId, LocalDateTime.now());
        
        IoTSensor sensor = registeredSensors.get(sensorId);
        if (sensor != null) {
            sensor.updateHeartbeat();
            sensor.mergeVersionVector(message.getVersionVector());
        }
        
        logger.debug("💓 Heartbeat de {}: {}", sensorId, senderAddress);
    }
    
    private void processDiscovery(IoTMessage message, InetSocketAddress senderAddress) {
        logger.debug("🔍 Solicitação de descoberta de: {}", senderAddress);
        
        // Enviar lista de sensores registrados
        StringBuilder response = new StringBuilder("SENSORS:");
        registeredSensors.forEach((id, sensor) -> {
            if (sensor.isHealthy()) {
                response.append(id).append(",");
            }
        });
        
        IoTMessage discoveryResponse = new IoTMessage(
            serverId,
            IoTMessage.MessageType.ACK,
            response.toString(),
            registeredSensors.size(),
            "SERVER",
            new ConcurrentHashMap<>(versionVector)
        );
        
        sendMessage(discoveryResponse, senderAddress);
    }
    
    private void processSync(IoTMessage message, InetSocketAddress senderAddress) {
        logger.debug("🔄 Sincronização de version vector de: {}", senderAddress);
        
        // Merge version vectors
        message.getVersionVector().forEach((nodeId, clock) -> 
            versionVector.merge(nodeId, clock, Integer::max)
        );
        
        // Responder com version vector atual
        IoTMessage syncResponse = new IoTMessage(
            serverId,
            IoTMessage.MessageType.SYNC,
            "VERSION_VECTOR_SYNC",
            0.0,
            "SERVER",
            new ConcurrentHashMap<>(versionVector)
        );
        
        sendMessage(syncResponse, senderAddress);
    }
    
    private void sendAck(IoTMessage originalMessage, InetSocketAddress address) {
        IoTMessage ack = new IoTMessage(
            serverId,
            IoTMessage.MessageType.ACK,
            "ACK_" + originalMessage.getMessageId(),
            0.0,
            "SERVER",
            new ConcurrentHashMap<>(versionVector)
        );
        
        sendMessage(ack, address);
    }
    
    private void sendMessage(IoTMessage message, InetSocketAddress address) {
        try {
            byte[] data = serializeMessage(message);
            DatagramPacket packet = new DatagramPacket(
                data, data.length, address
            );
            serverSocket.send(packet);
            
        } catch (IOException e) {
            logger.error("❌ Erro ao enviar mensagem para {}: {}", address, e.getMessage());
        }
    }
    
    private IoTMessage deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (IoTMessage) ois.readObject();
    }
    
    private byte[] serializeMessage(IoTMessage message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        return bos.toByteArray();
    }
    
    private void startMaintenanceTasks() {
        // Verificação de saúde dos sensores a cada 30 segundos
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            checkSensorHealth();
        }, 30, 30, TimeUnit.SECONDS);
        
        // Logs de estatísticas a cada 60 segundos
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            logStatistics();
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private void checkSensorHealth() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        int inactiveSensors = 0;
        
        for (var entry : lastHeartbeats.entrySet()) {
            String sensorId = entry.getKey();
            LocalDateTime lastHeartbeat = entry.getValue();
            
            if (lastHeartbeat.isBefore(threshold)) {
                IoTSensor sensor = registeredSensors.get(sensorId);
                if (sensor != null && sensor.getStatus() == IoTSensor.SensorStatus.ACTIVE) {
                    sensor.setStatus(IoTSensor.SensorStatus.INACTIVE);
                    inactiveSensors++;
                    logger.warn("⚠️ Sensor {} inativo há mais de 2 minutos", sensorId);
                }
            }
        }
        
        if (inactiveSensors > 0) {
            logger.info("📊 Verificação de saúde: {} sensores inativos", inactiveSensors);
        }
    }
    
    private void logStatistics() {
        int totalSensors = registeredSensors.size();
        long activeSensors = registeredSensors.values().stream()
            .mapToLong(sensor -> sensor.isHealthy() ? 1 : 0)
            .sum();
        
        logger.info("📈 Estatísticas do servidor:");
        logger.info("   🔸 Sensores registrados: {}", totalSensors);
        logger.info("   🔸 Sensores ativos: {}", activeSensors);
        logger.info("   🔸 Total de mensagens: {}", totalMessages.get());
        logger.info("   🔸 Erros: {}", errorCount.get());
        logger.info("   🔸 Version Vector: {}", versionVector);
    }
    
    private String extractNodeId(String content) {
        String[] parts = content.split("_");
        return parts.length > 3 ? parts[3] : "UNKNOWN";
    }
    
    private String extractLocation(String content) {
        String[] parts = content.split("_");
        return parts.length > 2 ? parts[2] : "UNKNOWN";
    }
    
    public void stop() {
        logger.info("🛑 Parando servidor UDP IoT...");
        running.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        messageProcessor.shutdown();
        maintenanceScheduler.shutdown();
        
        try {
            if (!messageProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageProcessor.shutdownNow();
            }
            if (!maintenanceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageProcessor.shutdownNow();
            maintenanceScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("✅ Servidor UDP IoT parado com sucesso");
    }
    
    // Getters para estatísticas
    public int getRegisteredSensorsCount() { return registeredSensors.size(); }
    public long getTotalMessages() { return totalMessages.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public boolean isRunning() { return running.get(); }
    public ConcurrentHashMap<String, IoTSensor> getRegisteredSensors() { 
        return new ConcurrentHashMap<>(registeredSensors); 
    }
}