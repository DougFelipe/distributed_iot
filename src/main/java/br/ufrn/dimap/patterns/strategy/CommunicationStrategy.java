package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.core.Message;

/**
 * Interface Strategy para diferentes protocolos de comunicação.
 * Permite trocar o protocolo de comunicação em tempo de execução.
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
     * Envia uma mensagem usando este protocolo
     * @param message mensagem a ser enviada
     * @param targetHost host de destino
     * @param targetPort porta de destino
     * @return true se enviado com sucesso
     */
    boolean sendMessage(Message message, String targetHost, int targetPort);
    
    /**
     * Registra um handler para processar mensagens recebidas
     * @param handler handler para processar mensagens
     */
    void setMessageHandler(MessageHandler handler);
    
    /**
     * Retorna o nome do protocolo
     * @return nome do protocolo (UDP, HTTP, GRPC)
     */
    String getProtocolName();
    
    /**
     * Verifica se o servidor está rodando
     * @return true se o servidor está ativo
     */
    boolean isServerRunning();
}