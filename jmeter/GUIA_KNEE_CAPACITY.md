# 📈 Guia para Identificação de Knee Capacity e Usable Capacity

## 🎯 Objetivo dos Testes

Este documento orienta como usar o arquivo `UDP_KNEE.jmx` para descobrir:

- **KNEE CAPACITY**: Ponto onde o throughput atinge o máximo antes de começar a degradar
- **USABLE CAPACITY**: Capacidade máxima utilizável com degradação aceitável (geralmente 80-90% do knee)

## 🔬 Metodologia de Teste

### Fases de Teste Implementadas

O arquivo `UDP_KNEE.jmx` implementa 6 fases progressivas de carga:

```
🔵 FASE 1: Baseline (10 threads)    - Estabelecer linha de base
🟢 FASE 2: Low Load (25 threads)    - Carga baixa estável
🟡 FASE 3: Medium Load (50 threads) - Carga média controlada
🟠 FASE 4: High Load (100 threads)  - Carga alta monitorada
🔴 FASE 5: Stress Test (200 threads)- Teste de stress
🔥 FASE 6: Peak Test (500 threads)  - Teste de pico máximo
```

### Configurações por Fase

| Fase | Threads | Ramp-up | Duração | Timer | Objetivo |
|------|---------|---------|---------|-------|----------|
| 1    | 10      | 5s      | 60s     | 100ms | Baseline |
| 2    | 25      | 10s     | 60s     | 50ms  | Low Load |
| 3    | 50      | 15s     | 60s     | 25ms  | Medium Load |
| 4    | 100     | 20s     | 60s     | 10ms  | High Load |
| 5    | 200     | 30s     | 60s     | 5ms   | Stress Test |
| 6    | 500     | 45s     | 60s     | 1ms   | Peak Test |

## 🚀 Executando os Testes

### 1. Preparação do Ambiente

```bash
# Navegar para o diretório do projeto
cd d:\distribuida

# Iniciar o sistema IoT primeiro
java -cp "target/classes;target/dependency/*" br.ufrn.dimap.applications.IoTSystemDynamicMenu
```

### 2. Execução dos Testes JMeter

#### Modo GUI (Para análise visual):
```bash
# Executar JMeter em modo GUI
jmeter -t jmeter/UDP_KNEE.jmx
```

#### Modo Linha de Comando (Para performance):
```bash
# Executar em modo não-GUI (recomendado para testes de carga)
jmeter -n -t jmeter/UDP_KNEE.jmx -l jmeter/results/udp_knee_results.jtl -e -o jmeter/reports/udp_knee_report

# Com logging detalhado
jmeter -n -t jmeter/UDP_KNEE.jmx -l jmeter/results/udp_knee_results.jtl -j jmeter/logs/udp_knee.log -e -o jmeter/reports/udp_knee_report
```

### 3. Parâmetros Configuráveis

Você pode ajustar os parâmetros no JMeter:

```xml
SERVER_HOST=localhost    # Endereço do servidor
SERVER_PORT=9090        # Porta do servidor UDP
TIMEOUT_MS=10000        # Timeout em milissegundos
```

## 📊 Análise dos Resultados

### Métricas Chave para Identificar Knee Capacity

#### 1. **Throughput (TPS - Transactions Per Second)**
- **Indicador**: Quando o TPS para de crescer ou começa a diminuir
- **Localização**: Gráfico "Transactions per Second"
- **Knee Point**: Pico do throughput antes da queda

#### 2. **Tempo de Resposta**
- **Indicador**: Aumento exponencial do tempo de resposta
- **Localização**: Gráfico "Response Times Over Time"
- **Degradação**: Quando passa de crescimento linear para exponencial

#### 3. **Taxa de Erro**
- **Indicador**: Aumento significativo de erros/timeouts
- **Localização**: "Aggregate Report" - coluna Error %
- **Limiar**: Quando errors > 1% e crescendo

#### 4. **Análise de Threads Ativas**
- **Indicador**: Correlação entre threads ativas e performance
- **Localização**: "Active Threads Over Time"
- **Saturação**: Quando mais threads não melhoram throughput

### Interpretação dos Resultados

#### Exemplo de Análise:

```
📊 RESULTADOS TÍPICOS:

FASE 1 (10 threads):   TPS: 95    | Resp: 15ms  | Error: 0%   ✅ Baseline
FASE 2 (25 threads):   TPS: 220   | Resp: 18ms  | Error: 0%   ✅ Scaling
FASE 3 (50 threads):   TPS: 380   | Resp: 25ms  | Error: 0.1% ✅ Good
FASE 4 (100 threads):  TPS: 520   | Resp: 45ms  | Error: 0.5% ⚠️ Watch
FASE 5 (200 threads):  TPS: 480   | Resp: 120ms | Error: 3%   🔴 Degrading
FASE 6 (500 threads):  TPS: 320   | Resp: 400ms | Error: 15%  💥 Overload

🎯 CONCLUSÃO:
- KNEE CAPACITY: ~520 TPS (Fase 4)
- USABLE CAPACITY: ~450 TPS (85% do knee)
- SAFE OPERATING: ~100 threads concurrent
```

## 🔧 Modificações Incrementais dos Testes

### Como Refinar a Análise

#### 1. **Ajustar Faixas de Thread**

Se o knee está entre 50-100 threads, adicione fases intermediárias:

```xml
<!-- Adicionar fase 3.5 -->
<ThreadGroup testname="🟡 FASE 3.5: Medium-High Load (75 threads)">
    <stringProp name="ThreadGroup.num_threads">75</stringProp>
    <stringProp name="ThreadGroup.ramp_time">18</stringProp>
    <stringProp name="ThreadGroup.delay">185</stringProp>
</ThreadGroup>
```

#### 2. **Modificar Duração dos Testes**

Para análise mais precisa, aumente a duração:

```xml
<!-- Para steady-state mais longo -->
<stringProp name="ThreadGroup.duration">120</stringProp> <!-- 2 minutos -->
```

#### 3. **Ajustar Timers**

Para encontrar throughput máximo, reduza os timers:

```xml
<!-- Timer mais agressivo -->
<stringProp name="ConstantTimer.delay">1</stringProp> <!-- 1ms -->
```

#### 4. **Habilitar Debugging**

Para análise detalhada de falhas:

```xml
<!-- Habilitar View Results Tree -->
<ResultCollector testname="🔍 View Results Tree (Debug Only)" enabled="true">
```

### Script de Auto-Análise

Crie um script PowerShell para análise automática:

```powershell
# analyze_knee.ps1
param(
    [string]$ResultFile = "jmeter/results/udp_knee_results.jtl"
)

Write-Host "📊 ANÁLISE DE KNEE CAPACITY" -ForegroundColor Green

# Analisar arquivo de resultados
$results = Import-Csv $ResultFile -Delimiter "`t"

# Agrupar por thread groups (fases)
$phases = $results | Group-Object label | Sort-Object Name

foreach ($phase in $phases) {
    $avgResponse = ($phase.Group | Measure-Object elapsed -Average).Average
    $maxResponse = ($phase.Group | Measure-Object elapsed -Maximum).Maximum
    $errorRate = ($phase.Group | Where-Object success -eq "false" | Measure-Object).Count / $phase.Count * 100
    $throughput = $phase.Count / 60 # requests por segundo (assumindo 60s de teste)
    
    Write-Host "🔍 $($phase.Name):" -ForegroundColor Yellow
    Write-Host "   Throughput: $([math]::Round($throughput, 2)) TPS"
    Write-Host "   Avg Response: $([math]::Round($avgResponse, 2)) ms"
    Write-Host "   Max Response: $([math]::Round($maxResponse, 2)) ms"
    Write-Host "   Error Rate: $([math]::Round($errorRate, 2))%"
    Write-Host ""
}
```

## 📈 Identificação dos Pontos Críticos

### Algoritmo para Encontrar Knee Capacity

1. **Plotar Throughput vs Carga**
   - Eixo X: Número de threads
   - Eixo Y: TPS (Transactions Per Second)
   - Procurar pelo pico antes da queda

2. **Aplicar Regra do 90%**
   - Knee = Ponto de throughput máximo
   - Usable = 90% do throughput máximo

3. **Verificar Critérios de Qualidade**
   - Tempo de resposta < 100ms (para IoT)
   - Taxa de erro < 1%
   - CPU do servidor < 80%

### Fórmulas de Cálculo

```
Knee Capacity = MAX(TPS) onde Error_Rate < 5%

Usable Capacity = Knee_Capacity * 0.9

Safe Operating Point = Ponto onde:
- TPS >= 80% do Knee_Capacity
- Response_Time <= 2x Baseline_Response_Time  
- Error_Rate <= 1%
```

## 🎯 Critérios de Aceitação

### Para Sistema IoT Distribuído

| Métrica | Baseline | Aceitável | Crítico |
|---------|----------|-----------|---------|
| **Throughput** | 50 TPS | 200+ TPS | 500+ TPS |
| **Resposta** | 10ms | 50ms | 100ms |
| **Erro** | 0% | 1% | 5% |
| **Disponibilidade** | 99.9% | 99.5% | 99% |

### Interpretação dos Resultados

- **🟢 VERDE**: Sistema operando dentro dos parâmetros ideais
- **🟡 AMARELO**: Sistema próximo aos limites, monitoramento necessário
- **🔴 VERMELHO**: Sistema degradado, ação corretiva necessária
- **💥 CRÍTICO**: Sistema em falha, intervenção imediata

## 📋 Relatório Final

Após executar os testes, documente:

### Template de Relatório

```markdown
# RELATÓRIO DE ANÁLISE DE CAPACIDADE

## Resumo Executivo
- **Data do Teste**: [DATA]
- **Duração Total**: [TEMPO]
- **Configuração**: Sistema IoT UDP - [SPECS]

## Resultados Principais
- **Knee Capacity**: [X] TPS em [Y] threads
- **Usable Capacity**: [X] TPS em [Y] threads  
- **Safe Operating**: [X] TPS em [Y] threads

## Métricas por Fase
[TABELA COM RESULTADOS]

## Gráficos de Análise
- Throughput vs Threads
- Response Time vs Threads
- Error Rate vs Threads

## Recomendações
1. Operar com máximo [X] threads concorrentes
2. Monitorar quando throughput > [Y] TPS
3. Implementar circuit breaker em [Z] TPS

## Limitações Identificadas
- Gargalo principal: [CPU/MEMORY/NETWORK/IO]
- Ponto de saturação: [DESCRIÇÃO]
- Fator limitante: [ANÁLISE]
```

Este guia permite identificar sistematicamente os limites de capacidade do sistema IoT distribuído, fornecendo dados concretos para dimensionamento e operação em produção.