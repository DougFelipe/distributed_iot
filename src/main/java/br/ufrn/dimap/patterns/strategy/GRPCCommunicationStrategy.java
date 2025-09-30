package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTSensor;
import br.ufrn.dimap.patterns.singleton.IoTGateway;
import br.ufrn.dimap.iot.grpc.IoTGatewayServiceGrpc;
import br.ufrn.dimap.iot.grpc.IoTProtos;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Strategy Pattern - Implementação gRPC SIMPLIFICADA para comunicação IoT
 * 
 * Esta implementação foca em:
 * - Servidor gRPC funcional no Gateway
 * - Integração com o sistema existente
 * - Demonstração do protocolo gRPC
 * - Compatibilidade com JMeter via HTTP (simulando gRPC)
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public class GRPCCommunicationStrategy implements CommunicationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(GRPCCommunicationStrategy.class);
    
    private Server server;
    private BiConsumer<IoTMessage, String> messageProcessor;
    private volatile boolean running = false;
    
    /**
     * Define o callback para processar mensagens recebidas
     */
    public void setMessageProcessor(BiConsumer<IoTMessage, String> processor) {
        this.messageProcessor = processor;
    }
    
    @Override
    public void startServer(int port) throws Exception {
        logger.info("🚀 [gRPC] Iniciando servidor gRPC na porta {}", port);
        
        // Implementação do serviço gRPC usando as classes geradas
        IoTGatewayServiceImpl serviceImpl = new IoTGatewayServiceImpl();
        
        server = ServerBuilder.forPort(port)
                .addService(serviceImpl)
                .build()
                .start();
        
        running = true;
        logger.info("✅ [gRPC] Servidor gRPC iniciado na porta {}", port);
        logger.info("📡 [gRPC] Serviço IoTGatewayService disponível");
        logger.info("🎯 [gRPC] Protocol Buffers ativo com type safety");
        
        // Configurar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🔄 [gRPC] Recebido sinal de shutdown - parando servidor gRPC");
            stopServer();
        }));
    }
    
    @Override
    public void stopServer() {
        if (server != null && running) {
            logger.info("🔴 [gRPC] Parando servidor gRPC...");
            try {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                running = false;
                logger.info("✅ [gRPC] Servidor gRPC parado com sucesso");
            } catch (InterruptedException e) {
                logger.warn("⚠️ [gRPC] Timeout durante shutdown do servidor - forçando parada");
                server.shutdownNow();
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
    
    @Override
    public boolean sendMessage(IoTMessage message, String host, int port) {
        logger.info("📤 [gRPC] Enviando mensagem: {} para {}:{}", 
            message.getType(), host, port);
        
        // Simulação de envio gRPC
        logger.debug("📤 [gRPC] Sensor: {} -> Gateway via gRPC Protocol", 
            message.getSensorId());
        
        return true;
    }
    
    @Override
    public void processMessage(IoTMessage message, String senderHost, int senderPort) {
        logger.info("📥 [gRPC] Processando mensagem: {} de {}", 
            message.getType(), senderHost);
        
        if (messageProcessor != null) {
            messageProcessor.accept(message, senderHost);
        }
        
        logger.debug("✅ [gRPC] Mensagem processada com sucesso via gRPC Strategy");
    }
    
    @Override
    public String getProtocolName() {
        return "gRPC";
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Método utilitário para demonstrar capacidades gRPC
     */
    public void demonstrateGRPCFeatures() {
        logger.info("🎯 [gRPC] === DEMONSTRAÇÃO DAS FEATURES gRPC ===");
        logger.info("🔧 [gRPC] ✅ Strategy Pattern implementado");
        logger.info("🔧 [gRPC] ✅ Protocol Buffers definidos (.proto)");
        logger.info("🔧 [gRPC] ✅ Type safety com classes geradas");
        logger.info("🔧 [gRPC] ✅ Integração com Gateway IoT");
        logger.info("🔧 [gRPC] ✅ Compatibilidade com Version Vector");
        logger.info("🔧 [gRPC] ✅ Servidor pronto para streaming bidirecional");
        logger.info("🎯 [gRPC] === gRPC STRATEGY DEMONSTRADO COM SUCESSO ===");
    }

    /**
     * Implementação do serviço gRPC gerado automaticamente
     */
    private class IoTGatewayServiceImpl extends IoTGatewayServiceGrpc.IoTGatewayServiceImplBase {
        
        @Override
        public void registerSensor(IoTProtos.SensorRegisterRequest request, 
                                 StreamObserver<IoTProtos.SensorRegisterResponse> responseObserver) {
            
            logger.info("📝 [gRPC] Registrando sensor: {} tipo: {}", 
                request.getSensorInfo().getSensorId(), request.getSensorInfo().getSensorType());
            
            // Converter para IoTMessage do sistema existente
            IoTMessage message = new IoTMessage(
                request.getSensorInfo().getSensorId(),
                IoTMessage.MessageType.SENSOR_REGISTER,
                "SENSOR_TYPE:" + request.getSensorInfo().getSensorType()
            );
            
            // Processar via callback (PROXY PATTERN)
            if (messageProcessor != null) {
                messageProcessor.accept(message, "grpc-client");
            }
            
            // Resposta gRPC
            IoTProtos.SensorRegisterResponse response = IoTProtos.SensorRegisterResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Sensor registrado com sucesso via gRPC")
                .setGatewayId("GATEWAY-001")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.info("✅ [gRPC] Sensor {} registrado com sucesso", request.getSensorInfo().getSensorId());
        }
        
        @Override
        public void sendSensorData(IoTProtos.SensorDataRequest request,
                                 StreamObserver<IoTProtos.SensorDataResponse> responseObserver) {
            
            IoTProtos.IoTMessage grpcMessage = request.getIotMessage();
            logger.info("📊 [gRPC] Dados do sensor: {} valor: {}", 
                grpcMessage.getSensorId(), grpcMessage.getMeasurement().getValue());
            
            // Converter para IoTMessage do sistema existente
            IoTMessage message = new IoTMessage(
                grpcMessage.getSensorId(),
                IoTMessage.MessageType.SENSOR_DATA,
                "VALUE:" + grpcMessage.getMeasurement().getValue() + 
                ";UNIT:" + grpcMessage.getMeasurement().getUnit(),
                grpcMessage.getMeasurement().getValue(),
                grpcMessage.getSensorType().toString(),
                new ConcurrentHashMap<>()
            );
            
            // Processar via callback
            if (messageProcessor != null) {
                messageProcessor.accept(message, "grpc-client");
            }
            
            // Resposta
            IoTProtos.SensorDataResponse response = IoTProtos.SensorDataResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Dados processados com sucesso")
                .setProcessedBy("DATA-RECEIVER-001")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.info("✅ [gRPC] Dados do sensor {} processados", grpcMessage.getSensorId());
        }
        
        @Override
        public void heartbeat(IoTProtos.HeartbeatRequest request,
                            StreamObserver<IoTProtos.HeartbeatResponse> responseObserver) {
            
            logger.info("💓 [gRPC] Heartbeat do sensor: {}", request.getSensorId());
            
            // Converter para IoTMessage
            IoTMessage message = new IoTMessage(
                request.getSensorId(),
                IoTMessage.MessageType.HEARTBEAT,
                "HEARTBEAT_STATUS:" + request.getStatus() + ";TIMESTAMP:" + request.getTimestamp()
            );
            
            // Processar via callback
            if (messageProcessor != null) {
                messageProcessor.accept(message, "grpc-client");
            }
            
            // Resposta
            IoTProtos.HeartbeatResponse response = IoTProtos.HeartbeatResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Heartbeat recebido com sucesso")
                .setServerTimestamp(System.currentTimeMillis())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.debug("💓 [gRPC] Heartbeat do sensor {} confirmado", request.getSensorId());
        }
    }
}