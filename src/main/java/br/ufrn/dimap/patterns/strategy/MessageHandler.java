package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.core.Message;

/**
 * Interface para handlers de mensagens recebidas.
 * Usado para processar mensagens que chegam via qualquer protocolo.
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * Processa uma mensagem recebida
     * @param message mensagem recebida
     * @param senderHost host que enviou a mensagem
     * @param senderPort porta que enviou a mensagem
     */
    void handleMessage(Message message, String senderHost, int senderPort);
}