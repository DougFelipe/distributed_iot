# ================================================================
# SCRIPT DE ANÁLISE DE LOGS - JMeter + Sistema IoT
# ================================================================

Write-Host "🔍 ANÁLISE DE LOGS - Sistema IoT Distribuído" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# Verificar se o sistema está rodando
Write-Host "`n📡 1. VERIFICANDO SISTEMA..." -ForegroundColor Yellow
$portCheck = netstat -an | findstr 9090
if ($portCheck) {
    Write-Host "✅ Sistema ativo na porta 9090" -ForegroundColor Green
    Write-Host $portCheck
} else {
    Write-Host "❌ Sistema NÃO está rodando na porta 9090" -ForegroundColor Red
}

# Verificar logs do sistema
Write-Host "`n📋 2. ÚLTIMOS LOGS DO SISTEMA..." -ForegroundColor Yellow
if (Test-Path "logs/sistema-distribuido.log") {
    Write-Host "✅ Arquivo de log encontrado" -ForegroundColor Green
    Write-Host "📊 Últimas 10 linhas:" -ForegroundColor Cyan
    Get-Content -Path "logs/sistema-distribuido.log" -Tail 10
} else {
    Write-Host "❌ Arquivo de log NÃO encontrado" -ForegroundColor Red
}

# Verificar resultados do JMeter
Write-Host "`n📊 3. RESULTADOS DO JMETER..." -ForegroundColor Yellow
$resultsDir = "jmeter/results"
if (Test-Path $resultsDir) {
    Write-Host "✅ Pasta de resultados encontrada" -ForegroundColor Green
    $files = Get-ChildItem $resultsDir -Filter "*.jtl"
    if ($files.Count -gt 0) {
        Write-Host "📁 Arquivos de resultado:" -ForegroundColor Cyan
        foreach ($file in $files) {
            $size = [math]::Round($file.Length / 1KB, 2)
            Write-Host "   - $($file.Name) ($size KB) - $($file.LastWriteTime)" -ForegroundColor White
        }
        
        # Analisar o summary report se existir
        $summaryFile = "$resultsDir/summary_report.jtl"
        if (Test-Path $summaryFile) {
            Write-Host "`n📈 Resumo dos testes (últimas 5 linhas):" -ForegroundColor Cyan
            Get-Content $summaryFile -Tail 5
        }
    } else {
        Write-Host "⚠️ Nenhum arquivo de resultado encontrado" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Pasta de resultados NÃO encontrada" -ForegroundColor Red
}

# Verificar logs do JMeter
Write-Host "`n🔧 4. LOGS DO JMETER..." -ForegroundColor Yellow
$jmeterLog = "jmeter.log"
if (Test-Path $jmeterLog) {
    Write-Host "✅ Log do JMeter encontrado" -ForegroundColor Green
    Write-Host "🐛 Últimas mensagens de erro/info:" -ForegroundColor Cyan
    Get-Content $jmeterLog -Tail 20 | Select-String -Pattern "(ERROR|WARN|INFO)" | Select-Object -Last 5
} else {
    Write-Host "⚠️ Log do JMeter não encontrado no diretório atual" -ForegroundColor Yellow
}

Write-Host "`n✅ ANÁLISE CONCLUÍDA!" -ForegroundColor Green
Write-Host "💡 Para monitorar em tempo real:" -ForegroundColor Cyan
Write-Host "   Get-Content -Path 'logs/sistema-distribuido.log' -Wait -Tail 10" -ForegroundColor White