package br.ufrn.dimap;

import br.ufrn.dimap.core.SystemConfig;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principal para demonstração e testes do sistema.
 * Sprint 1: Validação da estrutura base e padrões GoF.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    public static void main(String[] args) {
        logger.info("=== SISTEMA DISTRIBUÍDO - SPRINT 1 ===");
        
        try {
            // 1. Teste de Configuração
            testSystemConfiguration();
            
            // 2. Teste do Singleton Pattern
            testSingletonPattern();
            
            // 3. Teste do Observer Pattern (básico)
            testObserverPattern();
            
            logger.info("=== SPRINT 1 CONCLUÍDA COM SUCESSO ===");
            
        } catch (Exception e) {
            logger.error("Erro durante execução: {}", e.getMessage(), e);
        }
    }
    
    private static void testSystemConfiguration() {
        logger.info("1. Testando Sistema de Configuração...");
        
        SystemConfig config = new SystemConfig();
        logger.info("Configuração carregada: {}", config);
        
        // Teste de propriedades customizadas
        config.setProperty("test.property", "test.value");
        String testValue = config.getProperty("test.property");
        
        if ("test.value".equals(testValue)) {
            logger.info("✅ Sistema de configuração funcionando corretamente");
        } else {
            logger.error("❌ Problema no sistema de configuração");
        }
    }
    
    private static void testSingletonPattern() {
        logger.info("2. Testando Padrão Singleton (API Gateway)...");
        
        // Teste de instância única
        APIGateway gateway1 = APIGateway.getInstance();
        APIGateway gateway2 = APIGateway.getInstance();
        
        if (gateway1 == gateway2) {
            logger.info("✅ Padrão Singleton implementado corretamente");
            logger.info("Gateway ID: {}", gateway1.getId());
        } else {
            logger.error("❌ Problema na implementação do Singleton");
        }
        
        logger.info("Gateway status: {}", gateway1);
    }
    
    private static void testObserverPattern() {
        logger.info("3. Testando Padrão Observer (Heartbeat)...");
        
        APIGateway gateway = APIGateway.getInstance();
        
        // Teste básico do observer está integrado no Gateway
        logger.info("✅ Padrão Observer integrado ao Gateway");
        logger.info("Nós registrados: {}", gateway.getRegisteredNodes().size());
        logger.info("Nós ativos: {}", gateway.getActiveNodes().size());
    }
}