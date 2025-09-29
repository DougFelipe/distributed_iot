package br.ufrn.dimap.components;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Receiver - Instância B Stateful
 * 
 * Componente stateful responsável por:
 * - Receber dados roteados pelo Gateway (Proxy Pattern)
 * - Persistir dados em memória (ConcurrentHashMap)
 * - Manter Version Vector local distribuído
 * - Resolver conflitos usando Last Write Wins
 * - Logs detalhados para fácil compreensão
 * 
 * Arquitetura Minimalista:
 * - Armazenamento em memória
 * - Servidor UDP dedicado
 * - Thread-safe com concurrent collections
 * - Logs estruturados com timestamps
 * 
 * @author UFRN-DIMAP
 * @version 1.0 - Instância B Stateful
 */
public class DataReceiver {
    private static final Logger logger = LoggerFactory.getLogger(DataReceiver.class);
    
    private final String receiverId;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket serverSocket;
    private ExecutorService executorService;
    
    // ESTADO STATEFUL - Persistência em Memória
    private final ConcurrentHashMap<String, SensorDataEntry> sensorDatabase;
    private final ConcurrentHashMap<String, Long> versionVector;
    private final AtomicLong totalMessages;
    private final AtomicLong conflictsResolved;
    
    // Formatador para logs legíveis
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    /**
     * Entry para dados do sensor com timestamp para Last Write Wins
     */
    public static class SensorDataEntry implements Serializable {
        private final String sensorId;
        private final double value;
        private final String sensorType;
        private final LocalDateTime timestamp;
        private final long versionVectorClock;
        
        public SensorDataEntry(String sensorId, double value, String sensorType, 
                              LocalDateTime timestamp, long versionVectorClock) {
            this.sensorId = sensorId;
            this.value = value;
            this.sensorType = sensorType;
            this.timestamp = timestamp;
            this.versionVectorClock = versionVectorClock;
        }
        
        // Getters
        public String getSensorId() { return sensorId; }
        public double getValue() { return value; }
        public String getSensorType() { return sensorType; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public long getVersionVectorClock() { return versionVectorClock; }
        
        @Override
        public String toString() {
            return String.format("%.2f %s [%s] VV:%d", 
                               value, sensorType, timestamp.format(TIMESTAMP_FORMAT), versionVectorClock);
        }
    }
    
    public DataReceiver(String receiverId, int port) {
        this.receiverId = receiverId;
        this.port = port;
        this.sensorDatabase = new ConcurrentHashMap<>();
        this.versionVector = new ConcurrentHashMap<>();
        this.totalMessages = new AtomicLong(0);
        this.conflictsResolved = new AtomicLong(0);
        this.executorService = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "DataReceiver-" + receiverId + "-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        
        logger.info("🏗️ Data Receiver criado: {} na porta {}", receiverId, port);
    }
    
    /**
     * Inicia o Data Receiver
     */
    public void start() throws SocketException {
        if (running.get()) {
            logger.warn("⚠️ Data Receiver {} já está executando", receiverId);
            return;
        }
        
        serverSocket = new DatagramSocket(port);
        running.set(true);
        
        logger.info("🚀 Data Receiver {} iniciado na porta {}", receiverId, port);
        logger.info("📊 Estado inicial: Database={}, VV={}", sensorDatabase.size(), versionVector.size());
        
        // Thread principal para receber mensagens
        executorService.submit(() -> {
            byte[] buffer = new byte[65536];
            
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(packet);
                    
                    // Processar mensagem em thread separada
                    executorService.submit(() -> handleIncomingMessage(packet));
                    
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continuar
                } catch (Exception e) {
                    if (running.get()) {
                        logger.error("❌ Erro ao receber mensagem: {}", e.getMessage());
                    }
                }
            }
        });
        
        // Configurar timeout para recepção
        serverSocket.setSoTimeout(1000);
    }
    
    /**
     * Processa mensagem recebida do Gateway
     */
    private void handleIncomingMessage(DatagramPacket packet) {
        try {
            // Deserializar mensagem
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);
            IoTMessage message = (IoTMessage) ois.readObject();
            
            String senderHost = packet.getAddress().getHostAddress();
            int senderPort = packet.getPort();
            
            logger.debug("📬 Mensagem recebida do Gateway {}:{} - Tipo: {} - Sensor: {} - Valor: {}", 
                        senderHost, senderPort, message.getType(), message.getSensorId(), message.getSensorValue());
            
            // Processar baseado no tipo
            switch (message.getType()) {
                case SENSOR_DATA:
                    processSensorData(message);
                    break;
                case SENSOR_REGISTER:
                    processSensorRegistration(message);
                    break;
                case HEARTBEAT:
                    processHeartbeat(message);
                    break;
                default:
                    logger.debug("🔍 Tipo de mensagem ignorado: {}", message.getType());
            }
            
            // Enviar ACK de volta para o Gateway
            sendAck(message, packet.getAddress(), senderPort);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao processar mensagem: {}", e.getMessage());
        }
    }
    
    /**
     * Processa dados do sensor com Last Write Wins
     */
    private void processSensorData(IoTMessage message) {
        totalMessages.incrementAndGet();
        String sensorId = message.getSensorId();
        
        // Atualizar Version Vector
        updateVersionVector(message);
        
        // Criar nova entrada de dados
        SensorDataEntry newEntry = new SensorDataEntry(
            sensorId,
            message.getSensorValue(),
            message.getSensorType(),
            message.getTimestamp(),
            versionVector.getOrDefault(sensorId, 0L)
        );
        
        // Last Write Wins - Resolver conflitos por timestamp
        SensorDataEntry existingEntry = sensorDatabase.get(sensorId);
        boolean isConflict = false;
        
        if (existingEntry != null) {
            // Comparar timestamps para Last Write Wins
            if (newEntry.getTimestamp().isAfter(existingEntry.getTimestamp())) {
                // Nova entrada é mais recente
                sensorDatabase.put(sensorId, newEntry);
                logger.info("✅ [{}] Dados atualizados: {} = {} (Last Write Wins - Mais recente)", 
                           receiverId, sensorId, newEntry);
            } else if (newEntry.getTimestamp().isBefore(existingEntry.getTimestamp())) {
                // Entrada existente é mais recente - manter
                isConflict = true;
                conflictsResolved.incrementAndGet();
                logger.warn("⚠️ [{}] CONFLITO RESOLVIDO: {} mantido valor {} (Last Write Wins - Existente mais recente)", 
                           receiverId, sensorId, existingEntry);
            } else {
                // Timestamps iguais - usar Version Vector como desempate
                if (newEntry.getVersionVectorClock() > existingEntry.getVersionVectorClock()) {
                    sensorDatabase.put(sensorId, newEntry);
                    logger.info("✅ [{}] Dados atualizados: {} = {} (Desempate por VV)", 
                               receiverId, sensorId, newEntry);
                } else {
                    isConflict = true;
                    conflictsResolved.incrementAndGet();
                    logger.warn("⚠️ [{}] CONFLITO RESOLVIDO: {} mantido por VV {}", 
                               receiverId, sensorId, existingEntry);
                }
            }
        } else {
            // Primeira entrada para este sensor
            sensorDatabase.put(sensorId, newEntry);
            logger.info("✅ [{}] Novo sensor registrado: {} = {}", 
                       receiverId, sensorId, newEntry);
        }
        
        // Log estatísticas periodicamente
        if (totalMessages.get() % 10 == 0) {
            logger.info("📊 [{}] Stats: Mensagens={}, Sensores={}, Conflitos={}, VV={}", 
                       receiverId, totalMessages.get(), sensorDatabase.size(), 
                       conflictsResolved.get(), versionVector);
        }
    }
    
    /**
     * Processa registro de sensor
     */
    private void processSensorRegistration(IoTMessage message) {
        updateVersionVector(message);
        logger.info("📝 [{}] Sensor registrado: {} tipo: {}", 
                   receiverId, message.getSensorId(), message.getSensorType());
    }
    
    /**
     * Processa heartbeat
     */
    private void processHeartbeat(IoTMessage message) {
        updateVersionVector(message);
        logger.debug("💓 [{}] Heartbeat recebido: {}", receiverId, message.getSensorId());
    }
    
    /**
     * Atualiza Version Vector local
     */
    private void updateVersionVector(IoTMessage message) {
        if (message.getVersionVector() != null) {
            // Merge dos version vectors
            message.getVersionVector().forEach((senderId, version) -> {
                versionVector.merge(senderId, version.longValue(), Long::max);
            });
        }
    }
    
    /**
     * Envia ACK de confirmação
     */
    private void sendAck(IoTMessage originalMessage, InetAddress gatewayAddress, int gatewayPort) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            IoTMessage ackMessage = new IoTMessage(
                receiverId,
                IoTMessage.MessageType.ACK,
                "ACK_FROM_" + receiverId + "_FOR_" + originalMessage.getMessageId(),
                1.0,
                "ACK",
new ConcurrentHashMap<String, Integer>() {{
                    versionVector.forEach((k, v) -> put(k, v.intValue()));
                }}
            );
            
            // Serializar e enviar
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ackMessage);
            
            byte[] data = baos.toByteArray();
            DatagramPacket ackPacket = new DatagramPacket(data, data.length, gatewayAddress, gatewayPort);
            clientSocket.send(ackPacket);
            
            logger.debug("✅ [{}] ACK enviado para Gateway {}:{}", receiverId, gatewayAddress.getHostAddress(), gatewayPort);
            
        } catch (Exception e) {
            logger.warn("⚠️ [{}] Erro ao enviar ACK: {}", receiverId, e.getMessage());
        }
    }
    
    /**
     * Para o Data Receiver
     */
    public void stop() {
        logger.info("🛑 Parando Data Receiver {}...", receiverId);
        running.set(false);
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("✅ [{}] Data Receiver parado - Stats finais: Mensagens={}, Sensores={}, Conflitos={}", 
                   receiverId, totalMessages.get(), sensorDatabase.size(), conflictsResolved.get());
    }
    
    // Getters para monitoramento
    public String getReceiverId() { return receiverId; }
    public int getPort() { return port; }
    public boolean isRunning() { return running.get(); }
    public long getTotalMessages() { return totalMessages.get(); }
    public long getConflictsResolved() { return conflictsResolved.get(); }
    public int getSensorCount() { return sensorDatabase.size(); }
    public ConcurrentHashMap<String, Long> getVersionVector() { return new ConcurrentHashMap<>(versionVector); }
    
    /**
     * Retorna dados persistidos para monitoramento
     */
    public String getDatabaseStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Database Status:\n", receiverId));
        
        if (sensorDatabase.isEmpty()) {
            sb.append("  Nenhum dado persistido ainda\n");
        } else {
            sensorDatabase.forEach((sensorId, entry) -> {
                sb.append(String.format("  %s: %s\n", sensorId, entry));
            });
        }
        
        sb.append(String.format("  Version Vector: %s\n", versionVector));
        sb.append(String.format("  Stats: Msgs=%d, Conflitos=%d", totalMessages.get(), conflictsResolved.get()));
        
        return sb.toString();
    }
}