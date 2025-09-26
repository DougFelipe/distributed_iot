package br.ufrn.dimap.patterns.observer;

import br.ufrn.dimap.core.Node;

/**
 * Interface Observer for heartbeat monitoring.
 * Implementa o padrão Observer para monitoramento de componentes.
 */
public interface HeartbeatObserver {
    
    /**
     * Notifica quando um heartbeat é recebido de um nó
     * @param node nó que enviou o heartbeat
     */
    void onHeartbeatReceived(Node node);
    
    /**
     * Notifica quando um nó falha (timeout no heartbeat)
     * @param node nó que falhou
     */
    void onNodeFailure(Node node);
    
    /**
     * Notifica quando um novo nó se registra
     * @param node novo nó registrado
     */
    void onNodeRegistered(Node node);
}