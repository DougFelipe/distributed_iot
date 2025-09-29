package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.components.DataReceiver;
import br.ufrn.dimap.core.IoTMessage;

import java.util.List;

/**
 * Strategy Pattern - Interface para seleção de Data Receivers
 * 
 * Define estratégias para:
 * - Seleção de receptor para mensagens
 * - Load balancing entre receptores
 * - Failover automático
 * 
 * @author UFRN-DIMAP  
 * @version 1.0 - Strategy Pattern para Receivers
 */
public interface ReceiverStrategy {
    
    /**
     * Seleciona um Data Receiver para processar a mensagem
     * @param message Mensagem a ser processada
     * @param availableReceivers Lista de receptores disponíveis
     * @return Data Receiver selecionado ou null se nenhum disponível
     */
    DataReceiver selectReceiver(IoTMessage message, List<DataReceiver> availableReceivers);
    
    /**
     * Trata falha de um receptor
     * @param failedReceiver Receptor que falhou
     * @param availableReceivers Lista de receptores disponíveis
     */
    void handleReceiverFailure(DataReceiver failedReceiver, List<DataReceiver> availableReceivers);
    
    /**
     * Nome da estratégia para logs
     */
    String getStrategyName();
}