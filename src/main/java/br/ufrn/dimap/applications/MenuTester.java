package br.ufrn.dimap.applications;

/**
 * Classe para testar o menu interativo do sistema IoT
 */
public class MenuTester {
    public static void main(String[] args) {
        System.out.println("🎯 Iniciando Sistema IoT com Menu Interativo...\n");
        
        try {
            IoTInteractiveMenu menu = new IoTInteractiveMenu();
            menu.start();
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar o sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}