# Script de Limpeza - Arquivos Deprecated

## Arquivos que podem ser REMOVIDOS com segurança:

### 1. Aplicações Obsoletas:
- `src\main\java\br\ufrn\dimap\applications\NativeIoTServerApplication.java`
- `src\main\java\br\ufrn\dimap\applications\IoTClientDemo.java`

### 2. Servidor UDP Antigo:
- `src\main\java\br\ufrn\dimap\communication\native_udp\NativeUDPIoTServer.java`

## Motivos da Remoção:

### ❌ **NativeIoTServerApplication.java**
- **Substituído por**: `IoTDistributedSystem.java` 
- **Problema**: Não implementa padrões GoF (Singleton, Strategy, Observer, Proxy)
- **Main class atual**: `br.ufrn.dimap.applications.IoTDistributedSystem` (definido no pom.xml)

### ❌ **IoTClientDemo.java**  
- **Substituído por**: `NativeUDPIoTClient` integrado ao `IoTDistributedSystem`
- **Problema**: Demo manual interativo não usado nos testes automatizados
- **Não utilizado por**: JMeter, testes unitários, ou sistema principal

### ❌ **NativeUDPIoTServer.java**
- **Substituído por**: `UDPCommunicationStrategy` + `IoTGateway`
- **Problema**: Implementação monolítica sem padrões GoF
- **Arquitetura nova**: Strategy Pattern + Singleton Pattern + Observer Pattern

## Sistema Atual Funcional:

✅ **Main Class**: `IoTDistributedSystem.java`
✅ **Padrões GoF**: Todos implementados e funcionais  
✅ **Comunicação UDP**: Via `UDPCommunicationStrategy`
✅ **Gateway**: `IoTGateway` (Singleton + Proxy)
✅ **Monitoramento**: `HeartbeatMonitor` (Observer)
✅ **Clientes**: `NativeUDPIoTClient` integrados
✅ **Testes JMeter**: Funcionando com 0% de erro

## Comandos para Remoção:

```powershell
# Remover arquivos deprecated
Remove-Item "src\main\java\br\ufrn\dimap\applications\NativeIoTServerApplication.java"
Remove-Item "src\main\java\br\ufrn\dimap\applications\IoTClientDemo.java"  
Remove-Item "src\main\java\br\ufrn\dimap\communication\native_udp\NativeUDPIoTServer.java"
```

## application.properties Atualizado:

✅ **Atualizado**: Configurações alinhadas com sistema atual
✅ **Configurações IoT**: Sensores, gateway, UDP, monitoramento
✅ **Logging**: Níveis adequados para depuração
✅ **JMeter**: Configurações para testes automatizados

**O sistema funcionará perfeitamente após a remoção destes arquivos obsoletos!**