# ================================================================
# CONFIGURAÇÃO UDP SAMPLERS - JMeter Sistema IoT
# ================================================================

## 🎯 ARQUIVO CORRIGIDO: `Sistema_UDP_Funcionando.jmx`

### ✅ CAMPOS CONFIGURADOS AUTOMATICAMENTE:

#### 📡 **Registro de Sensor:**
- **Hostname/IP**: `localhost`
- **UDP Port**: `9090`
- **Request Data**: `SENSOR_REGISTER|TEMP_SENSOR_${__threadNum}|TEMPERATURE|Lab-${__threadNum}|${__time()}|0.0`
- **Wait for Response**: `false` (desmarcado)
- **Close UDP Socket**: `false` (desmarcado)
- **Response Timeout**: `3000`
- **Data Encode/Decode class**: (vazio)
- **Bind Local Address**: (vazio)
- **Bind Local Port**: (vazio)

#### 📊 **Dados do Sensor:**
- **Hostname/IP**: `localhost`
- **UDP Port**: `9090`
- **Request Data**: `SENSOR_DATA|TEMP_SENSOR_${__threadNum}|TEMPERATURE|${__time()}|${__Random(15,35)}.${__Random(0,99)}`
- **Wait for Response**: `false` (desmarcado)
- **Close UDP Socket**: `false` (desmarcado)
- **Response Timeout**: `3000`
- **Data Encode/Decode class**: (vazio)
- **Bind Local Address**: (vazio)
- **Bind Local Port**: (vazio)

#### 💓 **Heartbeat:**
- **Hostname/IP**: `localhost`
- **UDP Port**: `9090`
- **Request Data**: `HEARTBEAT|TEMP_SENSOR_${__threadNum}|TEMPERATURE|${__time()}|ALIVE`
- **Wait for Response**: `false` (desmarcado)
- **Close UDP Socket**: `false` (desmarcado)
- **Response Timeout**: `3000`
- **Data Encode/Decode class**: (vazio)
- **Bind Local Address**: (vazio)
- **Bind Local Port**: (vazio)

## 🚀 COMO TESTAR:

1. **✅ Sistema rodando**: Porta 9090 ativa
2. **✅ Arquivo corrigido**: `Sistema_UDP_Funcionando.jmx`
3. **Abrir JMeter**: `File → Open → Sistema_UDP_Funcionando.jmx`
4. **Executar**: `Run → Start` (Ctrl+R)
5. **Verificar**:
   - View Results Tree (sem erros)
   - Summary Report (0% erro)
   - Logs salvos em `results/udp_*.jtl`

## 📊 ESTRUTURA DO TESTE:

- **3 threads** (sensores)
- **10 loops** cada = **30 mensagens UDP**
- **3 tipos** de mensagem:
  1. **Registro** (uma vez por sensor)
  2. **Dados** (a cada 2s)
  3. **Heartbeat** (a cada 3º loop)

## 🔍 MONITORAR LOGS:

### No sistema Java:
```powershell
Get-Content -Path "logs/sistema-distribuido.log" -Wait -Tail 10
```

### No JMeter:
- `results/udp_summary.jtl`
- `results/udp_details.jtl`

## ⚡ STATUS:
- ✅ **Plugin UDP**: Instalado e funcionando
- ✅ **Configuração**: Campos preenchidos automaticamente
- ✅ **Sistema**: Rodando na porta 9090
- ✅ **Sintaxe**: Corrigida para o formato correto

**PRONTO PARA TESTAR!** 🎉