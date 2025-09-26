package br.ufrn.dimap;

import br.ufrn.dimap.communication.udp.UDPCommunicationStrategy;
import br.ufrn.dimap.components.ComponentA;
import br.ufrn.dimap.components.ComponentB;
import br.ufrn.dimap.core.SystemConfig;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplicação de demonstração da Sprint 2 - Comunicação UDP.
 * Demonstra o funcionamento completo do sistema com protocolo UDP.
 */
public class UDPDemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(UDPDemoApplication.class);
    
    public static void main(String[] args) {
        logger.info("=== SISTEMA DISTRIBUÍDO - SPRINT 2 - DEMONSTRAÇÃO UDP ===");
        
        try {
            // 1. Configuração do sistema
            SystemConfig config = new SystemConfig();
            config.setProperty("system.protocol", "UDP");
            
            // 2. Inicialização do API Gateway
            logger.info("1. Iniciando API Gateway...");
            APIGateway gateway = APIGateway.getInstance();
            gateway.setCommunicationStrategy(new UDPCommunicationStrategy());
            gateway.start(9090);
            
            // Aguarda um pouco para o Gateway estabilizar
            Thread.sleep(1000);
            
            // 3. Inicialização dos Componentes
            logger.info("2. Iniciando Componentes A e B...");
            
            ComponentA componentA = new ComponentA(config);
            componentA.setCommunicationStrategy(new UDPCommunicationStrategy());
            componentA.start(8081);
            
            ComponentB componentB = new ComponentB(config);
            componentB.setCommunicationStrategy(new UDPCommunicationStrategy());
            componentB.start(8082);
            
            // Aguarda componentes se registrarem
            Thread.sleep(2000);
            
            // 4. Demonstração de descoberta
            logger.info("3. Testando descoberta de componentes...");
            componentA.discoverComponents();
            componentB.discoverComponents();
            
            Thread.sleep(1000);
            
            // 5. Demonstração de comunicação
            logger.info("4. Testando comunicação entre componentes...");
            
            // ComponentA envia dados
            componentA.sendDataToComponent("test_key", "test_value_from_A");
            componentA.requestDataFromComponent("status");
            
            // ComponentB solicita análises
            componentB.requestDataAnalysis("sample data for analysis");
            componentB.requestReport("status");
            
            Thread.sleep(2000);
            
            // 6. Status do sistema
            logger.info("5. Status do sistema:");
            logger.info("Gateway: {}", gateway);
            logger.info("Nós registrados: {}", gateway.getRegisteredNodes().size());
            logger.info("Nós ativos: {}", gateway.getActiveNodes().size());
            logger.info(componentA.getStats());
            logger.info(componentB.getStats());
            
            // 7. Aguarda um pouco para observar heartbeats
            logger.info("6. Aguardando heartbeats... (30 segundos)");
            Thread.sleep(30000);
            
            // 8. Finaliza componentes
            logger.info("7. Finalizando sistema...");
            componentA.stop();
            componentB.stop();
            gateway.stop();
            
            logger.info("=== DEMONSTRAÇÃO UDP CONCLUÍDA COM SUCESSO ===");
            
        } catch (Exception e) {
            logger.error("Erro durante demonstração: {}", e.getMessage(), e);
        }
    }
}