package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.core.IoTMessage;

/**
 * Strategy Pattern - Interface para diferentes protocolos de comunicação IoT
 * 
 * Permite trocar o protocolo de comunicação em tempo de execução:
 * - UDP nativo (produção)
 * - HTTP (JMeter) 
 * - gRPC (alta performance)
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public interface CommunicationStrategy {
    
    /**
     * Inicia o servidor/listener para este protocolo
     * @param port porta para escutar
     * @throws Exception se houver erro na inicialização
     */
    void startServer(int port) throws Exception;
    
    /**
     * Para o servidor/listener
     */
    void stopServer();
    
    /**
     * Envia uma mensagem IoT usando este protocolo
     * @param message mensagem IoT a ser enviada
     * @param host endereço de destino
     * @param port porta de destino
     * @return true se enviado com sucesso
     */
    boolean sendMessage(IoTMessage message, String host, int port);
    
    /**
     * Processa uma mensagem IoT recebida
     * @param message mensagem recebida
     * @param senderHost host do remetente
     * @param senderPort porta do remetente
     */
    void processMessage(IoTMessage message, String senderHost, int senderPort);
    
    /**
     * Retorna o nome do protocolo
     * @return nome do protocolo (UDP, HTTP, gRPC)
     */
    String getProtocolName();
    
    /**
     * Verifica se o servidor está ativo
     * @return true se ativo
     */
    boolean isRunning();
}