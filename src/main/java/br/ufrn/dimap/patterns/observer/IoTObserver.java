package br.ufrn.dimap.patterns.observer;

/**
 * Observer Pattern - Interface para observadores de eventos IoT
 * 
 * Permite que diferentes componentes sejam notificados sobre:
 * - Registro/desregistro de sensores
 * - Recebimento de mensagens
 * - Mudanças de status
 * - Falhas detectadas
 * 
 * Implementações típicas:
 * - HeartbeatMonitor (monitora saúde dos sensores)
 * - StatisticsCollector (coleta métricas)
 * - AlertManager (gerencia alertas)
 * - LogAggregator (agrega logs)
 * 
 * @author UFRN-DIMAP
 * @version 1.0
 */
public interface IoTObserver {
    
    /**
     * Notificação de evento IoT
     * 
     * @param eventType tipo do evento (SENSOR_REGISTERED, MESSAGE_RECEIVED, etc.)
     * @param eventData dados do evento (sensor, mensagem, etc.)
     */
    void onIoTEvent(String eventType, Object eventData);
    
    /**
     * Retorna o nome do observer para identificação
     */
    default String getObserverName() {
        return this.getClass().getSimpleName();
    }
}