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
     * Processa mensagem TCP e converte para IoTMessage - VERSÃO SIMPLIFICADA
     * Aceita qualquer formato simples e gera resposta de sucesso
     */
    public IoTMessage processIncomingMessage(String rawMessage, String clientAddress) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            System.err.println("❌ [TCP] Mensagem vazia recebida de " + clientAddress);
            return null;
        }
        
        System.out.println("📥 [TCP] Mensagem recebida de " + clientAddress + ": " + rawMessage);
        
        try {
            // PARSE SIMPLIFICADO - aceita qualquer formato
            String[] parts = rawMessage.trim().split("\\|");
            
            String messageTypeStr = parts.length > 0 ? parts[0].trim() : "SENSOR_DATA";
            String sensorId = parts.length > 1 ? parts[1].trim() : "TCP_SENSOR_" + System.currentTimeMillis();
            String sensorType = parts.length > 2 ? parts[2].trim() : "TEMPERATURE";
            String valueStr = parts.length > 3 ? parts[3].trim() : "25.0";
            
            // Determinar tipo de mensagem - aceita qualquer tipo
            MessageType messageType = MessageType.SENSOR_DATA; // Default
            if (messageTypeStr.contains("REGISTER")) {
                messageType = MessageType.SENSOR_REGISTER;
            } else if (messageTypeStr.contains("HEARTBEAT")) {
                messageType = MessageType.HEARTBEAT;
            }
            
            // Parse do valor
            double sensorValue = 25.0; // Default
            try {
                sensorValue = Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ [TCP] Valor inválido, usando 25.0: " + valueStr);
            }
            
            // Version Vector simplificado
            ConcurrentHashMap<String, Integer> versionVector = new ConcurrentHashMap<>();
            versionVector.put(sensorId, (int)(System.currentTimeMillis() % 1000));
            
            // Criar IoTMessage
            IoTMessage message = new IoTMessage(sensorId, messageType, clientAddress, 
                                              sensorValue, sensorType, versionVector);
            
            System.out.println("✅ [TCP] Mensagem processada: " + message.getMessageId() + 
                             " - Sensor: " + sensorId + " - Tipo: " + messageType + 
                             " - VV: " + versionVector + " - Origem: " + clientAddress);
            
            return message;
            
        } catch (Exception e) {
            System.err.println("❌ [TCP] Erro ao processar mensagem: " + rawMessage + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parseia Version Vector de formato simples 
     * Formato: sensor1:5,sensor2:3,sensor3:1
     */
    private ConcurrentHashMap<String, Integer> parseVersionVector(String vvStr) {
        ConcurrentHashMap<String, Integer> vv = new ConcurrentHashMap<>();
        
        if (vvStr == null || vvStr.trim().isEmpty()) {
            return vv;
        }
        
        try {
            String[] pairs = vvStr.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    vv.put(key, Integer.parseInt(value));
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [TCP] Erro ao parsear Version Vector '" + vvStr + "': " + e.getMessage());
            // Retorna VV vazio em caso de erro
        }
        
        return vv;
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
     * Serializa Version Vector para formato TCP
     * Formato: sensor1:5,sensor2:3,sensor3:1
     */
    public String serializeVersionVector(ConcurrentHashMap<String, Integer> versionVector) {
        if (versionVector == null || versionVector.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ConcurrentHashMap.Entry<String, Integer> entry : versionVector.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * Gera mensagem TCP com Version Vector a partir de IoTMessage
     * Formato: "SENSOR_DATA|sensor_id|type|location|timestamp|value|versionVector"
     */
    public String generateTCPMessage(IoTMessage message) {
        if (message == null) {
            return null;
        }
        
        StringBuilder msgBuilder = new StringBuilder();
        
        // Tipo da mensagem
        switch (message.getType()) {
            case SENSOR_REGISTER:
                msgBuilder.append(MSG_SENSOR_REGISTER);
                break;
            case SENSOR_DATA:
                msgBuilder.append(MSG_SENSOR_DATA);
                break;
            case HEARTBEAT:
                msgBuilder.append(MSG_HEARTBEAT);
                break;
            default:
                msgBuilder.append(MSG_SENSOR_DATA);
        }
        
        msgBuilder.append(FIELD_SEPARATOR).append(message.getSensorId());
        msgBuilder.append(FIELD_SEPARATOR).append(message.getSensorType());
        msgBuilder.append(FIELD_SEPARATOR).append(message.getContent());
        msgBuilder.append(FIELD_SEPARATOR).append(message.getTimestamp());
        msgBuilder.append(FIELD_SEPARATOR).append(message.getSensorValue());
        
        // Adicionar Version Vector
        String vvStr = serializeVersionVector(message.getVersionVector());
        msgBuilder.append(FIELD_SEPARATOR).append(vvStr);
        
        return msgBuilder.toString();
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