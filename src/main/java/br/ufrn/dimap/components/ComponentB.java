package br.ufrn.dimap.components;

import br.ufrn.dimap.core.Message;
import br.ufrn.dimap.core.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação do Componente B do sistema distribuído.
 * Responsável por análise de dados e geração de relatórios.
 */
public class ComponentB extends BaseComponent {
    private static final Logger logger = LoggerFactory.getLogger(ComponentB.class);
    
    private final ConcurrentHashMap<String, Integer> analyticsData = new ConcurrentHashMap<>();
    private final AtomicInteger analysisCounter = new AtomicInteger(0);
    
    public ComponentB(SystemConfig config) {
        super("COMPONENT_B", config);
        initializeAnalytics();
    }
    
    /**
     * Inicializa dados de análise
     */
    private void initializeAnalytics() {
        analyticsData.put("requests_processed", 0);
        analyticsData.put("data_analyzed", 0);
        analyticsData.put("reports_generated", 0);
        logger.info("Sistema de análise inicializado no Componente B");
    }
    
    @Override
    protected void handleSpecificMessage(Message message, String senderHost, int senderPort) {
        switch (message.getType()) {
            case "ANALYZE_DATA":
                handleAnalyzeData(message, senderHost, senderPort);
                break;
            case "GENERATE_REPORT":
                handleGenerateReport(message, senderHost, senderPort);
                break;
            case "GET_ANALYTICS":
                handleGetAnalytics(message, senderHost, senderPort);
                break;
            case "PING":
                handlePing(message, senderHost, senderPort);
                break;
            default:
                logger.warn("Tipo de mensagem não suportado pelo Componente B: {}", message.getType());
        }
    }
    
    @Override
    protected String processRequest(String requestContent) {
        int requestId = analysisCounter.incrementAndGet();
        logger.info("Analisando requisição #{}: {}", requestId, requestContent);
        
        // Simula análise de dados
        try {
            Thread.sleep(150); // Simula tempo de análise (um pouco mais que ComponentA)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Atualiza estatísticas
        analyticsData.merge("requests_processed", 1, Integer::sum);
        analyticsData.merge("data_analyzed", requestContent.length(), Integer::sum);
        
        String result = String.format("Analisado pelo %s - Analysis #%d: [%d chars] -> ANALYZED", 
            componentId, requestId, requestContent.length());
        
        logger.info("Análise #{} concluída", requestId);
        return result;
    }
    
    private void handleAnalyzeData(Message message, String senderHost, int senderPort) {
        String data = message.getContent();
        
        // Simula análise complexa
        int dataComplexity = data.length() * 2;
        String analysisResult = String.format("Analysis: complexity=%d, pattern=detected, confidence=0.95", 
            dataComplexity);
        
        analyticsData.merge("data_analyzed", 1, Integer::sum);
        
        Message response = new Message("ANALYSIS_RESULT", analysisResult, componentId);
        communicationStrategy.sendMessage(response, senderHost, senderPort);
        
        logger.info("Análise de dados enviada para {}:{}", senderHost, senderPort);
    }
    
    private void handleGenerateReport(Message message, String senderHost, int senderPort) {
        String reportType = message.getContent();
        
        String report = generateReport(reportType);
        analyticsData.merge("reports_generated", 1, Integer::sum);
        
        Message response = new Message("REPORT", report, componentId);
        communicationStrategy.sendMessage(response, senderHost, senderPort);
        
        logger.info("Relatório '{}' gerado e enviado para {}:{}", reportType, senderHost, senderPort);
    }
    
    private void handleGetAnalytics(Message message, String senderHost, int senderPort) {
        StringBuilder analytics = new StringBuilder();
        for (var entry : analyticsData.entrySet()) {
            analytics.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        
        Message response = new Message("ANALYTICS_DATA", analytics.toString(), componentId);
        communicationStrategy.sendMessage(response, senderHost, senderPort);
        
        logger.info("Dados de análise enviados para {}:{}", senderHost, senderPort);
    }
    
    private void handlePing(Message message, String senderHost, int senderPort) {
        Message pong = new Message("PONG", 
            String.format("ComponentB-%s-OK", componentId), componentId);
        communicationStrategy.sendMessage(pong, senderHost, senderPort);
        
        logger.debug("PING respondido para {}:{}", senderHost, senderPort);
    }
    
    /**
     * Gera relatório baseado no tipo solicitado
     */
    private String generateReport(String reportType) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (reportType.toLowerCase()) {
            case "status":
                return String.format("STATUS_REPORT|timestamp=%s|component=%s|requests=%d|analyses=%d", 
                    now, componentId, 
                    analyticsData.get("requests_processed"), 
                    analyticsData.get("data_analyzed"));
                    
            case "performance":
                return String.format("PERFORMANCE_REPORT|timestamp=%s|avg_processing_time=150ms|success_rate=99.5%%", 
                    now);
                    
            case "summary":
                return String.format("SUMMARY_REPORT|timestamp=%s|total_reports=%d|system_health=GOOD", 
                    now, analyticsData.get("reports_generated"));
                    
            default:
                return String.format("UNKNOWN_REPORT|timestamp=%s|error=unsupported_type", now);
        }
    }
    
    /**
     * Solicita análise de dados via Gateway
     */
    public void requestDataAnalysis(String data) {
        Message message = new Message("ANALYZE_DATA", data, componentId);
        sendMessageViaGateway(message);
        
        logger.info("Solicitação de análise enviada via Gateway: {} chars", data.length());
    }
    
    /**
     * Solicita geração de relatório via Gateway
     */
    public void requestReport(String reportType) {
        Message message = new Message("GENERATE_REPORT", reportType, componentId);
        sendMessageViaGateway(message);
        
        logger.info("Solicitação de relatório '{}' enviada via Gateway", reportType);
    }
    
    /**
     * Retorna estatísticas do componente
     */
    public String getStats() {
        return String.format("ComponentB Stats - Analyses: %d, Reports: %d, Data: %d, Status: %s", 
            analysisCounter.get(), analyticsData.get("reports_generated"),
            analyticsData.get("data_analyzed"), 
            running.get() ? "RUNNING" : "STOPPED");
    }
    
    /**
     * Retorna dados de análise
     */
    public ConcurrentHashMap<String, Integer> getAnalyticsData() {
        return new ConcurrentHashMap<>(analyticsData);
    }
}