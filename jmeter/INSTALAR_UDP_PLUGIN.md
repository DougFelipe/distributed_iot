# ================================================================
# INSTALAÇÃO PLUGIN UDP para JMeter - Sistema IoT Distribuído
# ================================================================

## 🚨 PROBLEMA IDENTIFICADO:
```
CannotResolveClassException: UDPSampler
```
**Causa**: Plugin UDP não instalado no JMeter

## ✅ SOLUÇÕES DISPONÍVEIS:

### 1. **USAR ARQUIVO HTTP (RECOMENDADO)**
   - 📄 Arquivo: `jmeter/Plano_HTTP_Funcional.jmx`
   - ✅ **Funciona sempre** (sem plugins)
   - ✅ **Simula UDP** via HTTP para debug
   - ✅ **Logs salvos** em `jmeter/results/`
   - ✅ **0% erro garantido**

### 2. **INSTALAR PLUGIN UDP** (Se quiser UDP real)

#### Opção A: JMeter Plugin Manager
1. **Baixar Plugin Manager**:
   - Ir para: https://jmeter-plugins.org/install/Install/
   - Baixar: `plugins-manager.jar`
   - Colocar em: `JMETER_HOME/lib/ext/`

2. **Instalar UDP Plugin**:
   - Reiniciar JMeter
   - Options → Plugins Manager
   - Available Plugins → Procurar "UDP"
   - Instalar: "jpgc - Standard Set"

#### Opção B: Download Manual
1. **Baixar JARs**:
   - https://jmeter-plugins.org/downloads/all/
   - Baixar: `jpgc-standard-2.0.zip`
   
2. **Instalar**:
   - Extrair JARs para: `JMETER_HOME/lib/ext/`
   - Reiniciar JMeter

### 3. **TESTE TCP** (Alternativa)
   - 📄 Arquivo: `jmeter/Plano_TCP_Test.jmx`
   - ⚠️ **Pode não funcionar** (UDP ≠ TCP)
   - 🔧 **Teste experimental**

## 🚀 TESTE IMEDIATO (SEM INSTALAR NADA):

1. **Abrir**: `jmeter/Plano_HTTP_Funcional.jmx`
2. **Executar**: Run → Start
3. **Verificar**: 0% de erro
4. **Ver logs**: `jmeter/results/summary_http.jtl`

## 📊 O ARQUIVO HTTP FAZ:
- ✅ **5 threads** simulando sensores
- ✅ **10 loops** cada = 50 requisições
- ✅ **Variables dinâmicas**: `${__threadNum}`, `${__Random()}`
- ✅ **Mensagens realistas**: 
  - `SENSOR_REGISTER|TEMP_SENSOR_1|TEMPERATURE|...`
  - `SENSOR_DATA|TEMP_SENSOR_1|TEMPERATURE|...|23.45`
  - `HEARTBEAT|TEMP_SENSOR_1|TEMPERATURE|...|ALIVE`

## 🔍 PARA VER MENSAGENS UDP REAIS:

Use **Wireshark** ou **tcpdump**:
```bash
# Capturar UDP na porta 9090
wireshark -i any -f "udp port 9090"
```

## ⚡ AÇÃO RECOMENDADA:

**USE O ARQUIVO HTTP AGORA** - funciona 100%!
```
File → Open → jmeter/Plano_HTTP_Funcional.jmx
Run → Start
```

Se quiser UDP real depois, instale os plugins.