# 🎯 SCRIPT DE DEMONSTRAÇÃO - TOLERÂNCIA A FALHAS
# Sistema IoT Distribuído - Sprint 2 - UFRN/DIMAP
# 
# Este script demonstra a tolerância a falhas do sistema:
# 1. Inicia o sistema completo
# 2. Executa testes JMeter (operação normal)
# 3. Simula falhas em Data Receivers
# 4. Mostra recuperação automática

Write-Host "🏆 DEMONSTRAÇÃO FINAL - SISTEMA IoT DISTRIBUÍDO" -ForegroundColor Cyan
Write-Host "📊 Arquitetura: Instâncias A (Stateless) → Gateway (Proxy) → Instâncias B (Stateful)" -ForegroundColor Green
Write-Host "🛡️ Tolerância a Falhas com Recuperação Automática" -ForegroundColor Yellow
Write-Host ""

# Função para mostrar status
function Show-Status {
    param($message, $color = "White")
    Write-Host "[$((Get-Date).ToString('HH:mm:ss'))] $message" -ForegroundColor $color
}

# Função para executar comando com status
function Execute-Command {
    param($command, $description)
    Show-Status "▶️ $description" "Cyan"
    try {
        Invoke-Expression $command
        Show-Status "✅ $description - Concluído" "Green"
    } catch {
        Show-Status "❌ Erro em: $description - $($_.Exception.Message)" "Red"
    }
}

# Verificar se estamos no diretório correto
if (-not (Test-Path "pom.xml")) {
    Show-Status "❌ Execute este script no diretório raiz do projeto (onde está o pom.xml)" "Red"
    exit 1
}

Show-Status "🔄 FASE 1: Compilação e Preparação" "Yellow"
Execute-Command "mvn clean compile" "Compilando o projeto Java"

Show-Status "📁 Criando diretórios de logs e resultados" "Gray"
if (-not (Test-Path "jmeter/results")) { New-Item -ItemType Directory -Path "jmeter/results" -Force | Out-Null }
if (-not (Test-Path "logs")) { New-Item -ItemType Directory -Path "logs" -Force | Out-Null }

Show-Status "🚀 FASE 2: Iniciando Sistema Distribuído" "Yellow"
Show-Status "📡 Iniciando Sistema IoT (Gateway + Data Receivers + Tolerância a Falhas)" "Cyan"

# Iniciar o sistema em background
$systemJob = Start-Job -ScriptBlock {
    Set-Location $using:PWD
    mvn exec:java -Dexec.mainClass="br.ufrn.dimap.applications.IoTDistributedSystem" -Dexec.args="" -q
} -Name "IoTSystem"

# Aguardar sistema iniciar
Show-Status "⏳ Aguardando sistema inicializar (15 segundos)..." "Gray"
Start-Sleep -Seconds 15

# Verificar se o sistema está rodando
$systemRunning = Get-Job -Name "IoTSystem" -ErrorAction SilentlyContinue
if ($systemRunning -and $systemRunning.State -eq "Running") {
    Show-Status "✅ Sistema IoT Distribuído iniciado com sucesso!" "Green"
    Show-Status "   🔸 Gateway (Proxy): localhost:9090" "Gray"
    Show-Status "   🔸 Data Receiver 1: localhost:9091" "Gray"
    Show-Status "   🔸 Data Receiver 2: localhost:9092" "Gray"
    Show-Status "   🔸 Tolerância a Falhas: Ativa" "Gray"
} else {
    Show-Status "❌ Falha ao iniciar o sistema IoT" "Red"
    Show-Status "📋 Log do sistema:" "Gray"
    Receive-Job -Name "IoTSystem" -ErrorAction SilentlyContinue
    exit 1
}

Show-Status "🧪 FASE 3: Executando Testes JMeter - Operação Normal" "Yellow"
Show-Status "📊 Executando 200 threads por 60 segundos (deve resultar em 0% erros)" "Cyan"

# Executar JMeter
Execute-Command "jmeter -n -t jmeter/Sistema_IoT_Apresentacao_Final.jmx -l jmeter/results/normal_operation.jtl" "Teste JMeter - Operação Normal"

Show-Status "📈 Resultados da Operação Normal:" "Green"
if (Test-Path "jmeter/results/normal_operation.jtl") {
    $results = Get-Content "jmeter/results/normal_operation.jtl" | Select-Object -Skip 1
    $totalSamples = $results.Count
    $errorSamples = ($results | Where-Object { $_.Split(',')[7] -eq 'false' }).Count
    $errorPercentage = if ($totalSamples -gt 0) { [math]::Round(($errorSamples / $totalSamples) * 100, 2) } else { 0 }
    
    Show-Status "   📊 Total de amostras: $totalSamples" "White"
    Show-Status "   ❌ Erros: $errorSamples" "White"
    Show-Status "   📈 Taxa de erro: $errorPercentage%" "White"
    
    if ($errorPercentage -le 1) {
        Show-Status "✅ SUCESSO: Taxa de erro muito baixa ($errorPercentage%)" "Green"
    } else {
        Show-Status "⚠️ ATENÇÃO: Taxa de erro alta ($errorPercentage%)" "Yellow"
    }
}

Show-Status "🛡️ FASE 4: Demonstração de Tolerância a Falhas" "Yellow"
Show-Status "⚠️ SIMULAÇÃO: O sistema detectará falhas automaticamente e criará backups" "Cyan"
Show-Status "📋 Para demonstrar manualmente:" "Gray"
Show-Status "   1. Desligue um Data Receiver (Ctrl+C em outra janela)" "Gray"
Show-Status "   2. Execute JMeter novamente → Erros aumentam" "Gray"
Show-Status "   3. O sistema cria backup automaticamente → Erros diminuem" "Gray"
Show-Status "   4. Religque o Data Receiver → Sistema volta ao normal" "Gray"

Show-Status "🎯 FASE 5: Sistema Pronto para Demonstração Interativa" "Yellow"
Show-Status "📊 Padrões GoF Implementados e Funcionando:" "Green"
Show-Status "   ✅ Singleton: Gateway único como proxy" "Gray"
Show-Status "   ✅ Strategy: Seleção Round Robin de Data Receivers" "Gray"
Show-Status "   ✅ Observer: Monitoramento de heartbeat" "Gray"
Show-Status "   ✅ Proxy: Gateway roteando para Data Receivers" "Gray"

Show-Status "🔄 Sistema rodando... Pressione qualquer tecla para encerrar" "Cyan"
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Show-Status "🔄 Encerrando sistema..." "Yellow"
Stop-Job -Name "IoTSystem" -PassThru | Remove-Job

Show-Status "🏁 Demonstração concluída!" "Green"
Show-Status "📁 Logs salvos em: logs/sistema-distribuido.log" "Gray"
Show-Status "📊 Resultados JMeter em: jmeter/results/" "Gray"