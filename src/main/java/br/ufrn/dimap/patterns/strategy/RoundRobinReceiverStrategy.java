package br.ufrn.dimap.patterns.strategy;

import br.ufrn.dimap.components.DataReceiver;
import br.ufrn.dimap.core.IoTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round Robin Strategy - Distribuição balanceada entre receptores
 * 
 * Implementação minimalista de load balancing:
 * - Rotação circular entre receptores disponíveis
 * - Simplicidade para fácil compreensão
 * - Logs detalhados para demonstração
 * 
 * @author UFRN-DIMAP
 * @version 1.0 - Round Robin para Data Receivers
 */
public class RoundRobinReceiverStrategy implements ReceiverStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RoundRobinReceiverStrategy.class);
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    @Override
    public DataReceiver selectReceiver(IoTMessage message, List<DataReceiver> availableReceivers) {
        if (availableReceivers == null || availableReceivers.isEmpty()) {
            logger.warn("⚠️ Nenhum Data Receiver disponível para mensagem {}", message.getMessageId());
            return null;
        }
        
        // Filtrar apenas receptores ativos
        List<DataReceiver> activeReceivers = availableReceivers.stream()
                .filter(DataReceiver::isRunning)
                .toList();
        
        if (activeReceivers.isEmpty()) {
            logger.warn("⚠️ Nenhum Data Receiver ativo para mensagem {}", message.getMessageId());
            return null;  
        }
        
        // Round Robin simples
        int index = currentIndex.getAndIncrement() % activeReceivers.size();
        DataReceiver selected = activeReceivers.get(index);
        
        logger.debug("🎯 [ROUND_ROBIN] Selecionado {} para mensagem {} do sensor {} (índice {}/{})", 
                    selected.getReceiverId(), message.getMessageId(), message.getSensorId(), 
                    index, activeReceivers.size());
        
        return selected;
    }
    
    @Override
    public void handleReceiverFailure(DataReceiver failedReceiver, List<DataReceiver> availableReceivers) {
        logger.warn("⚠️ [ROUND_ROBIN] Falha detectada no receiver {}", failedReceiver.getReceiverId());
        
        // Contar receptores ativos restantes
        long activeCount = availableReceivers.stream()
                .filter(DataReceiver::isRunning)
                .count();
        
        if (activeCount > 0) {
            logger.info("✅ [ROUND_ROBIN] {} receptores ainda disponíveis para failover", activeCount);
            // Reset do índice para redistribuir carga
            currentIndex.set(0);
        } else {
            logger.error("❌ [ROUND_ROBIN] ALERTA: Nenhum Data Receiver disponível!");
        }
    }
    
    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }
}