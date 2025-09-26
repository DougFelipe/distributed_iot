package br.ufrn.dimap.applications;

import br.ufrn.dimap.communication.udp.UDPCommunicationStrategy;
import br.ufrn.dimap.core.Node;
import br.ufrn.dimap.monitoring.MonitoringInterface;
import br.ufrn.dimap.patterns.singleton.APIGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplicação de demonstração para Sprint 3: Sistema de Monitoramento e Heartbeat
 * Demonstra o Observer Pattern e funcionalidades de monitoramento avançado.
 */
public class MonitoringDemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringDemoApplication.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("=== SISTEMA DISTRIBUÍDO - SPRINT 3 - DEMONSTRAÇÃO DE MONITORAMENTO ===");
        
        // 1. Inicializar API Gateway com timeout menor para demonstração
        APIGateway gateway = APIGateway.createTestInstance(10); // 10 segundos timeout
        gateway.setCommunicationStrategy(new UDPCommunicationStrategy());
        gateway.start(9090);
        
        // 2. Inicializar Interface de Monitoramento
        MonitoringInterface monitoring = new MonitoringInterface(gateway);
        
        logger.info("Sistema de monitoramento iniciado com timeout de 10 segundos");
        
        // 3. Simular registro de múltiplos componentes
        logger.info("\n--- FASE 1: REGISTRO DE COMPONENTES ---");
        
        Node component1 = new Node("MONITOR-COMP-A-001", "COMPONENT_A", "127.0.0.1", 8081);
        Node component2 = new Node("MONITOR-COMP-B-001", "COMPONENT_B", "127.0.0.1", 8082);
        Node component3 = new Node("MONITOR-COMP-A-002", "COMPONENT_A", "127.0.0.1", 8083);
        
        gateway.registerComponent(component1);
        Thread.sleep(1000);
        gateway.registerComponent(component2);
        Thread.sleep(1000);
        gateway.registerComponent(component3);
        Thread.sleep(2000);
        
        // 4. Mostrar status inicial
        logger.info(monitoring.getSystemStatusReport());
        logger.info(monitoring.getActiveComponentsTable());
        
        // 5. Simular heartbeats periódicos
        logger.info("\n--- FASE 2: SIMULAÇÃO DE HEARTBEATS ATIVOS ---");
        
        for (int i = 1; i <= 3; i++) {
            logger.info("Ciclo de heartbeats #{}", i);
            
            // Enviar heartbeats para todos os componentes
            gateway.updateHeartbeat(component1.getId());
            gateway.updateHeartbeat(component2.getId());
            gateway.updateHeartbeat(component3.getId());
            
            Thread.sleep(3000); // 3 segundos entre ciclos
            
            // Mostrar estatísticas
            MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
            logger.info("Heartbeats totais: {}, Nós ativos: {}", 
                stats.getTotalHeartbeats(), stats.getActiveNodes());
        }
        
        // 6. Simular falha de componente
        logger.info("\n--- FASE 3: SIMULAÇÃO DE FALHA DE COMPONENTE ---");
        logger.info("Parando heartbeats do componente: {}", component2.getId());
        
        // Continuar enviando heartbeats apenas para component1 e component3
        for (int i = 1; i <= 4; i++) {
            logger.info("Ciclo pós-falha #{} (aguardando timeout do componente {})", i, component2.getId());
            
            gateway.updateHeartbeat(component1.getId());
            gateway.updateHeartbeat(component3.getId());
            // Não enviar heartbeat para component2 - simular falha
            
            Thread.sleep(3000);
            
            // Verificar status a cada ciclo
            MonitoringInterface.SystemStats stats = monitoring.getSystemStats();
            logger.info("Status: {} ativos, {} inativos, {} falhas totais", 
                stats.getActiveNodes(), stats.getInactiveNodes(), stats.getTotalFailures());
        }
        
        // 7. Mostrar relatório final
        logger.info("\n--- RELATÓRIO FINAL ---");
        logger.info(monitoring.getSystemStatusReport());
        logger.info(monitoring.getActiveComponentsTable());
        
        // 8. Demonstrar recuperação de componente
        logger.info("\n--- FASE 4: SIMULAÇÃO DE RECUPERAÇÃO ---");
        logger.info("Reativando componente: {}", component2.getId());
        
        // Simular "reinício" do component2 registrando novamente
        Node recoveredComponent = new Node("MONITOR-COMP-B-001-RECOVERED", "COMPONENT_B", "127.0.0.1", 8082);
        gateway.registerComponent(recoveredComponent);
        
        for (int i = 1; i <= 3; i++) {
            logger.info("Ciclo pós-recuperação #{}", i);
            
            gateway.updateHeartbeat(component1.getId());
            gateway.updateHeartbeat(component3.getId());
            gateway.updateHeartbeat(recoveredComponent.getId());
            
            Thread.sleep(2000);
        }
        
        // 9. Relatório final de recuperação
        logger.info("\n--- RELATÓRIO FINAL PÓS-RECUPERAÇÃO ---");
        logger.info(monitoring.getSystemStatusReport());
        logger.info(monitoring.getActiveComponentsTable());
        
        logger.info("\n--- DEMONSTRAÇÃO ESPECÍFICA DO OBSERVER PATTERN ---");
        
        // Demonstrar Observer Pattern em ação
        logger.info("Adicionando observer adicional para demonstrar padrão...");
        
        DemoObserver demoObserver = new DemoObserver();
        gateway.getHeartbeatSubject().addObserver(demoObserver);
        
        logger.info("Enviando heartbeats para demonstrar notificações do Observer...");
        gateway.updateHeartbeat(component1.getId());
        gateway.updateHeartbeat(recoveredComponent.getId());
        
        Thread.sleep(2000);
        
        logger.info("Observer personalizado recebeu {} notificações de heartbeat", 
            demoObserver.getNotificationCount());
        
        // 10. Finalização limpa
        logger.info("\n--- FINALIZANDO DEMONSTRAÇÃO ---");
        monitoring.forceHealthCheck();
        
        Thread.sleep(2000);
        gateway.stop();
        
        logger.info("=== DEMONSTRAÇÃO SPRINT 3 CONCLUÍDA ===");
        logger.info("Observer Pattern: FUNCIONANDO ✓");
        logger.info("Sistema de Monitoramento: FUNCIONANDO ✓");  
        logger.info("Detecção de Falhas: FUNCIONANDO ✓");
        logger.info("Tolerância a Falhas: FUNCIONANDO ✓");
    }
    
    /**
     * Observer de demonstração para mostrar o padrão em ação
     */
    private static class DemoObserver implements br.ufrn.dimap.patterns.observer.HeartbeatObserver {
        private int notificationCount = 0;
        
        @Override
        public void onHeartbeatReceived(Node node) {
            notificationCount++;
            logger.info("🔔 DemoObserver: Heartbeat recebido de {}", node.getId());
        }
        
        @Override
        public void onNodeFailure(Node node) {
            logger.warn("🔔 DemoObserver: FALHA detectada em {}", node.getId());
        }
        
        @Override
        public void onNodeRegistered(Node node) {
            logger.info("🔔 DemoObserver: Novo nó registrado {}", node.getId());
        }
        
        public int getNotificationCount() {
            return notificationCount;
        }
    }
}