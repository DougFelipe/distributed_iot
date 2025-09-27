package br.ufrn.dimap.communication.native_udp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

/**
 * Cliente UDP nativo para simular sensores IoT
 * Baseado nos exemplos UDP com funcionalidades IoT avançadas
 */
public class NativeUDPIoTClient {
    private static final Logger logger = LoggerFactory.getLogger(NativeUDPIoTClient.class);
    
    private final String clientId;
    private final IoTSensor sensor;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private DatagramSocket clientSocket;
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    
    public NativeUDPIoTClient(String sensorId, IoTSensor.SensorType sensorType, 
                             String location, String serverHost, int serverPort) throws UnknownHostException {
        this.clientId = "CLIENT-" + sensorId;
        this.sensor = new IoTSensor(sensorId, clientId, sensorType, location);
        this.serverAddress = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;
        this.scheduler = Executors.newScheduledThreadPool(3);
        
        logger.info("🔧 Cliente IoT criado: {} sensor: {} tipo: {}", 
                   clientId, sensorId, sensorType);
    }
    
    public void start() throws SocketException {
        if (running.get()) {
            logger.warn("⚠️ Cliente já está em execução");
            return;
        }
        
        clientSocket = new DatagramSocket();
        running.set(true);
        
        logger.info("🚀 Cliente IoT {} iniciado", clientId);
        
        // Registrar sensor no servidor
        registerSensor();
        
        // Programar tarefas periódicas
        schedulePeriodicTasks();
        
        logger.info("✅ Cliente IoT {} operacional", clientId);
    }
    
    private void registerSensor() {
        try {
            IoTMessage registerMessage = sensor.createRegisterMessage();
            sendMessage(registerMessage);
            logger.info("📝 Sensor {} registrado no servidor", sensor.getSensorId());
            
        } catch (Exception e) {
            logger.error("❌ Erro ao registrar sensor: {}", e.getMessage());
        }
    }
    
    private void schedulePeriodicTasks() {
        // Enviar dados do sensor a cada 5-15 segundos (aleatório)
        scheduler.scheduleAtFixedRate(() -> {
            if (running.get()) {
                sendSensorData();
            }
        }, 2, 5 + random.nextInt(10), TimeUnit.SECONDS);
        
        // Enviar heartbeat a cada 30 segundos
        scheduler.scheduleAtFixedRate(() -> {
            if (running.get()) {
                sendHeartbeat();
            }
        }, 10, 30, TimeUnit.SECONDS);
        
        // Simular falhas ocasionais (5% de chance a cada minuto)
        scheduler.scheduleAtFixedRate(() -> {
            if (running.get() && random.nextDouble() < 0.05) {
                simulateFailure();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private void sendSensorData() {
        try {
            // Simular leitura do sensor
            double value = sensor.readValue();
            IoTMessage dataMessage = sensor.createSensorDataMessage();
            
            sendMessage(dataMessage);
            
            logger.debug("📊 Dados enviados: {} = {:.2f} {} - Msg ID: {} - Tipo: {} [Código: {}] - Timestamp: {}", 
                        sensor.getSensorId(), value, sensor.getType().getUnit(),
                        dataMessage.getMessageId(), dataMessage.getType(), 
                        dataMessage.getType().getCode(), dataMessage.getTimestamp());
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar dados do sensor: {}", e.getMessage());
        }
    }
    
    private void sendHeartbeat() {
        try {
            IoTMessage heartbeatMessage = sensor.createHeartbeatMessage();
            sendMessage(heartbeatMessage);
            
            logger.debug("💓 Heartbeat enviado: {} - Msg ID: {} - Tipo: {} [Código: {}] - Timestamp: {}", 
                        sensor.getSensorId(), heartbeatMessage.getMessageId(), 
                        heartbeatMessage.getType(), heartbeatMessage.getType().getCode(),
                        heartbeatMessage.getTimestamp());
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar heartbeat: {}", e.getMessage());
        }
    }
    
    private void simulateFailure() {
        int failureType = random.nextInt(3);
        
        switch (failureType) {
            case 0:
                logger.warn("⚠️ Simulando sensor com erro temporário");
                sensor.setStatus(IoTSensor.SensorStatus.ERROR);
                
                // Recuperar após 30 segundos
                scheduler.schedule(() -> {
                    sensor.setStatus(IoTSensor.SensorStatus.ACTIVE);
                    logger.info("✅ Sensor {} recuperado do erro", sensor.getSensorId());
                }, 30, TimeUnit.SECONDS);
                break;
                
            case 1:
                logger.warn("⚠️ Simulando sensor em manutenção");
                sensor.setStatus(IoTSensor.SensorStatus.MAINTENANCE);
                
                // Voltar ao normal após 60 segundos
                scheduler.schedule(() -> {
                    sensor.setStatus(IoTSensor.SensorStatus.ACTIVE);
                    logger.info("✅ Sensor {} voltou da manutenção", sensor.getSensorId());
                }, 60, TimeUnit.SECONDS);
                break;
                
            case 2:
                logger.warn("⚠️ Simulando perda de conectividade temporária");
                // Parar de enviar mensagens por 45 segundos
                running.set(false);
                
                scheduler.schedule(() -> {
                    running.set(true);
                    logger.info("✅ Conectividade restaurada para {}", sensor.getSensorId());
                }, 45, TimeUnit.SECONDS);
                break;
        }
    }
    
    private void sendMessage(IoTMessage message) throws IOException {
        byte[] data = serializeMessage(message);
        DatagramPacket packet = new DatagramPacket(
            data, data.length, serverAddress, serverPort
        );
        
        clientSocket.send(packet);
        
        // Aguardar ACK (opcional, com timeout)
        receiveAck();
    }
    
    private void receiveAck() {
        try {
            clientSocket.setSoTimeout(2000); // 2 segundos timeout
            
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(
                receiveBuffer, receiveBuffer.length
            );
            
            clientSocket.receive(receivePacket);
            
            IoTMessage ackMessage = deserializeMessage(receivePacket.getData());
            if (ackMessage.getType() == IoTMessage.MessageType.ACK) {
                logger.debug("✅ ACK recebido: {}", ackMessage.getContent());
                
                // Merge version vector do servidor
                sensor.mergeVersionVector(ackMessage.getVersionVector());
            }
            
        } catch (SocketTimeoutException e) {
            logger.debug("⏱️ Timeout ao aguardar ACK");
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao receber ACK: {}", e.getMessage());
        } finally {
            try {
                clientSocket.setSoTimeout(0); // Remover timeout
            } catch (SocketException e) {
                // Ignorar
            }
        }
    }
    
    public void sendDiscoveryRequest() {
        try {
            IoTMessage discoveryMessage = new IoTMessage(
                sensor.getSensorId(),
                IoTMessage.MessageType.DISCOVERY,
                "DISCOVERY_REQUEST",
                0.0,
                "CLIENT",
                sensor.getVersionVector()
            );
            
            sendMessage(discoveryMessage);
            logger.info("🔍 Solicitação de descoberta enviada");
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar descoberta: {}", e.getMessage());
        }
    }
    
    public void sendSyncRequest() {
        try {
            IoTMessage syncMessage = new IoTMessage(
                sensor.getSensorId(),
                IoTMessage.MessageType.SYNC,
                "VERSION_VECTOR_SYNC_REQUEST",
                0.0,
                "CLIENT",
                sensor.getVersionVector()
            );
            
            sendMessage(syncMessage);
            logger.info("🔄 Solicitação de sincronização enviada");
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar sincronização: {}", e.getMessage());
        }
    }
    
    private byte[] serializeMessage(IoTMessage message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        return bos.toByteArray();
    }
    
    private IoTMessage deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (IoTMessage) ois.readObject();
    }
    
    public void stop() {
        logger.info("🛑 Parando cliente IoT {}...", clientId);
        running.set(false);
        
        scheduler.shutdown();
        
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("✅ Cliente IoT {} parado com sucesso", clientId);
    }
    
    // Getters para monitoramento
    public String getClientId() { return clientId; }
    public IoTSensor getSensor() { return sensor; }
    public boolean isRunning() { return running.get(); }
    
    // Método estático para criar múltiplos clientes de teste
    public static void createTestClients(String serverHost, int serverPort, int count) {
        IoTSensor.SensorType[] types = IoTSensor.SensorType.values();
        String[] locations = {"SALA_A", "SALA_B", "LABORATORIO", "CORREDOR", "EXTERIOR"};
        
        for (int i = 0; i < count; i++) {
            try {
                String sensorId = String.format("SENSOR_%03d", i + 1);
                IoTSensor.SensorType type = types[i % types.length];
                String location = locations[i % locations.length];
                
                NativeUDPIoTClient client = new NativeUDPIoTClient(
                    sensorId, type, location, serverHost, serverPort
                );
                
                // Iniciar cliente em thread separada
                Thread clientThread = new Thread(() -> {
                    try {
                        client.start();
                        
                        // Manter cliente ativo
                        while (client.isRunning()) {
                            Thread.sleep(1000);
                        }
                        
                    } catch (Exception e) {
                        logger.error("❌ Erro no cliente {}: {}", sensorId, e.getMessage());
                    }
                });
                
                clientThread.setDaemon(true);
                clientThread.start();
                
                // Delay entre criação de clientes
                Thread.sleep(1000);
                
            } catch (Exception e) {
                logger.error("❌ Erro ao criar cliente {}: {}", i, e.getMessage());
            }
        }
        
        logger.info("🏭 {} clientes IoT de teste criados", count);
    }
}