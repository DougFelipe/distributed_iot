package br.ufrn.dimap.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Classe para gerenciar configurações do sistema.
 * Permite configuração flexível através de arquivos properties.
 */
public class SystemConfig {
    private static final String CONFIG_FILE = "application.properties";
    private final Properties properties;
    
    public SystemConfig() {
        this.properties = new Properties();
        loadDefaultConfig();
        loadConfigFromFile();
    }
    
    private void loadDefaultConfig() {
        // Configurações padrão
        properties.setProperty("system.default.port", "8080");
        properties.setProperty("system.heartbeat.timeout", "30");
        properties.setProperty("system.component.type", "COMPONENT_A");
        properties.setProperty("system.protocol", "UDP");
        properties.setProperty("system.host", "localhost");
    }
    
    private void loadConfigFromFile() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar configurações: " + e.getMessage());
        }
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    // Métodos de conveniência para configurações específicas
    
    public int getDefaultPort() {
        return getIntProperty("system.default.port", 8080);
    }
    
    public int getHeartbeatTimeout() {
        return getIntProperty("system.heartbeat.timeout", 30);
    }
    
    public String getComponentType() {
        return getProperty("system.component.type", "COMPONENT_A");
    }
    
    public String getProtocol() {
        return getProperty("system.protocol", "UDP");
    }
    
    public String getHost() {
        return getProperty("system.host", "localhost");
    }
    
    @Override
    public String toString() {
        return "SystemConfig{" +
                "port=" + getDefaultPort() +
                ", protocol='" + getProtocol() + '\'' +
                ", componentType='" + getComponentType() + '\'' +
                ", heartbeatTimeout=" + getHeartbeatTimeout() +
                '}';
    }
}