# Guia de Uso - Estratégias HTTP e TCP

## Visão Geral

O sistema IoT foi expandido para suportar três protocolos de comunicação:

1. **UDP** (original) - Para compatibilidade com testes existentes
2. **HTTP** (novo) - Para compatibilidade total com JMeter e APIs REST
3. **TCP** (novo) - Para conexões persistentes e alta confiabilidade

## Como Usar

### 1. Executando com UDP (padrão)
```bash
java -jar sistema-iot.jar
java -jar sistema-iot.jar UDP
java -Diot.protocol=UDP -jar sistema-iot.jar
```

### 2. Executando com HTTP
```bash
java -jar sistema-iot.jar HTTP
java -jar sistema-iot.jar --protocol=HTTP
java -Diot.protocol=HTTP -jar sistema-iot.jar
```

### 3. Executando com TCP
```bash
java -jar sistema-iot.jar TCP
java -jar sistema-iot.jar --protocol=TCP
java -Diot.protocol=TCP -jar sistema-iot.jar
```

### 4. Configurando Portas Personalizadas
```bash
# HTTP na porta 8080
java -Diot.http.port=8080 -Diot.protocol=HTTP -jar sistema-iot.jar

# TCP na porta 9000
java -Diot.tcp.port=9000 -Diot.protocol=TCP -jar sistema-iot.jar

# UDP na porta 9095
java -Diot.udp.port=9095 -Diot.protocol=UDP -jar sistema-iot.jar
```

## Protocolos Implementados

### HTTP Strategy

**Porta padrão:** 8081

**Endpoints disponíveis:**
- `POST /sensor/data` - Envio de dados de sensores
- `GET /sensor/status` - Status do sistema IoT
- `GET /health` - Health check do sistema

**Formato de envio (POST /sensor/data):**
```json
{
  "sensorId": "TEMP_001",
  "type": "TEMPERATURE",
  "location": "Lab-A",
  "value": 25.6,
  "timestamp": 1640995200000
}
```

**Formato de envio (Query Parameters):**
```http
POST /sensor/data?sensorId=TEMP_001&type=TEMPERATURE&location=Lab-A&value=25.6&timestamp=1640995200000
```

**Resposta de sucesso:**
```json
{
  "status": "SUCCESS",
  "message": "Data received successfully",
  "timestamp": 1640995200123,
  "sensorId": "TEMP_001"
}
```

### TCP Strategy

**Porta padrão:** 8082

**Protocolo:** Conexões persistentes com formato de mensagem compatível com UDP

**Formato de mensagem:**
```
SENSOR_DATA|sensor_id|type|location|timestamp|value
```

**Exemplo:**
```
SENSOR_DATA|TEMP_001|TEMPERATURE|Lab-A|1640995200000|25.6
```

**Resposta de sucesso:**
```
OK|RECEIVED|1640995200123|Data processed successfully
```

**Comandos especiais:**
- `DISCONNECT` ou `EXIT` - Desconectar do servidor

### UDP Strategy (Original)

**Porta padrão:** 9090

**Formato de mensagem:** Igual ao TCP para compatibilidade
- Binário: Serialização Java nativa
- Texto: `SENSOR_DATA|sensor_id|type|location|timestamp|value`

## Configurações no application.properties

```properties
# Protocolo padrão
iot.protocol=UDP

# Configurações HTTP
iot.http.port=8081
iot.http.host=localhost
iot.http.thread.pool.size=50
iot.http.timeout.ms=30000

# Configurações TCP
iot.tcp.port=8082
iot.tcp.host=localhost
iot.tcp.thread.pool.size=50
iot.tcp.connection.timeout.ms=30000

# Configurações UDP
iot.udp.port=9090
iot.udp.host=localhost
iot.udp.buffer.size=65536
iot.udp.timeout.ms=1000
```

## Testes com JMeter

### Para HTTP
1. Usar HTTP Request sampler
2. URL: `http://localhost:8081/sensor/data`
3. Method: POST
4. Content-Type: application/json ou application/x-www-form-urlencoded

### Para TCP
1. Usar TCP Sampler plugin
2. Server: localhost
3. Port: 8082
4. Mensagem: `SENSOR_DATA|TEMP_001|TEMPERATURE|Lab-A|${__time()}|${__Random(20,30)}`

### Para UDP
1. Usar UDP Request plugin (pode ter timeouts)
2. Server: localhost
3. Port: 9090
4. Mensagem: Formato binário ou texto

## Vantagens de Cada Protocolo

### HTTP
- ✅ Compatibilidade total com JMeter
- ✅ Padrão REST/JSON
- ✅ Debugging fácil
- ✅ Firewall-friendly
- ❌ Overhead maior

### TCP
- ✅ Conexões persistentes  
- ✅ Confiabilidade alta
- ✅ Formato compatível com UDP
- ✅ Menos overhead que HTTP
- ❌ Requer plugin JMeter para teste

### UDP
- ✅ Baixo overhead
- ✅ Protocolo original do sistema
- ✅ Alta performance
- ❌ JMeter plugin buggy (timeouts)
- ❌ Sem garantia de entrega

## Logs e Monitoramento

Todos os protocolos geram logs detalhados mostrando:
- Protocolo ativo
- Porta em uso  
- Conexões aceitas/recusadas
- Mensagens processadas
- Estatísticas de performance

## Exemplos de Uso Completo

### Sistema HTTP completo:
```bash
# Terminal 1 - Iniciar sistema
java -Diot.protocol=HTTP -Diot.http.port=8080 -jar sistema-iot.jar

# Terminal 2 - Teste manual
curl -X POST http://localhost:8080/sensor/data \
  -H "Content-Type: application/json" \
  -d '{"sensorId":"TEMP_001","type":"TEMPERATURE","location":"Lab-A","value":25.6}'
```

### Sistema TCP completo:
```bash
# Terminal 1 - Iniciar sistema  
java -Diot.protocol=TCP -Diot.tcp.port=9000 -jar sistema-iot.jar

# Terminal 2 - Teste manual
telnet localhost 9000
SENSOR_DATA|TEMP_001|TEMPERATURE|Lab-A|1640995200000|25.6
DISCONNECT
```

Esta implementação permite trocar o protocolo em runtime mantendo toda a arquitetura de padrões GoF intacta!