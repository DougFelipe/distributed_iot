package br.ufrn.dimap.communication.tcp;

import br.ufrn.dimap.core.IoTMessage;
import br.ufrn.dimap.core.IoTMessage.MessageType;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import static br.ufrn.dimap.communication.tcp.TCPProtocolConstants.*;

/**
 * Processador de mensagens TCP IoT
 * Baseado no exemplo ProcessPayload.java do professor com adaptações
 */
public class TCPMessageProcessor {
    
    /**
     * Processa mensagem TCP e converte para IoTMessage
     * Formato: "SENSOR_DATA|sensor_id|type|location|timestamp|value"
     */
    public IoTMessage processIncomingMessage(String rawMessage, String clientAddress) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            System.err.println("❌ [TCP] Mensagem vazia recebida de " + clientAddress);
            return null;
        }
        
        try {
            // Simular delay de processamento (como no exemplo do professor)
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        try {
            StringTokenizer tokenizer = new StringTokenizer(rawMessage.trim(), FIELD_SEPARATOR);
            
            if (tokenizer.countTokens() < 3) {
                System.err.println("❌ [TCP] Formato inválido: " + rawMessage);
                return null;
            }
            
            // Parse dos campos
            String messageTypeStr = tokenizer.nextToken().trim();
            String sensorId = tokenizer.nextToken().trim();
            String sensorType = tokenizer.nextToken().trim();
            
            // Campos opcionais
            String location = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : clientAddress;
            if (tokenizer.hasMoreTokens()) tokenizer.nextToken(); // skip timestamp
            String valueStr = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : "0.0";
            
            // Determinar tipo de mensagem
            MessageType messageType;
            if (MSG_SENSOR_REGISTER.equals(messageTypeStr)) {
                messageType = MessageType.SENSOR_REGISTER;
            } else if (MSG_SENSOR_DATA.equals(messageTypeStr)) {
                messageType = MessageType.SENSOR_DATA;
            } else if (MSG_HEARTBEAT.equals(messageTypeStr)) {
                messageType = MessageType.HEARTBEAT;
            } else {
                System.err.println("❌ [TCP] Tipo de mensagem desconhecido: " + messageTypeStr);
                return null;
            }
            
            // Parse do valor
            double sensorValue = 0.0;
            try {
                sensorValue = Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ [TCP] Valor inválido, usando 0.0: " + valueStr);
            }
            
            // Criar IoTMessage
            IoTMessage message = new IoTMessage(sensorId, messageType, location, 
                                              sensorValue, sensorType, 
                                              new ConcurrentHashMap<>());
            
            System.out.println("✅ [TCP] Mensagem processada: " + message.getMessageId() + 
                             " - Sensor: " + sensorId + " - Tipo: " + messageType + 
                             " - Origem: " + clientAddress);
            
            return message;
            
        } catch (Exception e) {
            System.err.println("❌ [TCP] Erro ao processar mensagem: " + rawMessage + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gera resposta TCP para mensagem processada
     */
    public String generateResponse(IoTMessage processedMessage, boolean success) {
        if (processedMessage == null) {
            return formatErrorResponse("UNKNOWN", "UNKNOWN", "INVALID_MESSAGE");
        }
        
        if (success) {
            return formatSuccessResponse(processedMessage.getMessageId(), processedMessage.getSensorId());
        } else {
            return formatErrorResponse(processedMessage.getMessageId(), processedMessage.getSensorId(), "PROCESSING_FAILED");
        }
    }
    
    /**
     * Valida formato de mensagem TCP
     */
    public boolean isValidMessageFormat(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        StringTokenizer tokenizer = new StringTokenizer(message.trim(), FIELD_SEPARATOR);
        
        // Pelo menos 3 campos obrigatórios: tipo|sensor_id|sensor_type
        if (tokenizer.countTokens() < 3) {
            return false;
        }
        
        String messageType = tokenizer.nextToken().trim();
        return MSG_SENSOR_REGISTER.equals(messageType) || 
               MSG_SENSOR_DATA.equals(messageType) || 
               MSG_HEARTBEAT.equals(messageType);
    }
    
    /**
     * Extrai ID do sensor da mensagem
     */
    public String extractSensorId(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        try {
            StringTokenizer tokenizer = new StringTokenizer(message.trim(), FIELD_SEPARATOR);
            if (tokenizer.countTokens() >= 2) {
                tokenizer.nextToken(); // skip message type
                return tokenizer.nextToken().trim();
            }
        } catch (Exception e) {
            System.err.println("❌ [TCP] Erro ao extrair sensor ID: " + e.getMessage());
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Gera uma resposta de erro formatada para o protocolo TCP.
     * 
     * @param errorCode código do erro
     * @return resposta de erro formatada
     */
    public String generateErrorResponse(String errorCode) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        switch (errorCode) {
            case "INVALID_MESSAGE_FORMAT":
                return "ERROR|INVALID_FORMAT|" + timestamp + "|Message format is invalid";
            
            case "PROCESSING_ERROR":
                return "ERROR|PROCESSING_FAILED|" + timestamp + "|Failed to process message";
            
            case "UNKNOWN_SENSOR_TYPE":
                return "ERROR|UNKNOWN_TYPE|" + timestamp + "|Sensor type not recognized";
            
            case "MISSING_REQUIRED_FIELDS":
                return "ERROR|MISSING_FIELDS|" + timestamp + "|Required fields are missing";
            
            default:
                return "ERROR|GENERAL_ERROR|" + timestamp + "|" + errorCode;
        }
    }
}