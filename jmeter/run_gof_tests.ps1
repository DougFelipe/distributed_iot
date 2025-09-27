# JMETER - TESTE DOS PADRÕES GoF UDP
# Sistema IoT Distribuído - Sprint 2

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "                    JMETER - TESTE DOS PADRÕES GoF UDP" -ForegroundColor Yellow
Write-Host "                     Sistema IoT Distribuído - Sprint 2" -ForegroundColor Yellow
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""

# Configurar caminho do JMeter
$jmeterPath = "D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat"

# Verificar se JMeter existe no caminho especificado
if (-not (Test-Path $jmeterPath)) {
    Write-Host "❌ JMeter não encontrado em: $jmeterPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "📋 Verifique se o JMeter está instalado em:" -ForegroundColor Yellow
    Write-Host "   D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\"
    Write-Host ""
    Write-Host "   Ou ajuste a variável jmeterPath no script"
    Write-Host ""
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "🚀 Iniciando testes JMeter para Padrões GoF..." -ForegroundColor Green
Write-Host ""

# Limpar resultados anteriores
if (Test-Path "results\*.jtl") { Remove-Item "results\*.jtl" -Force }
if (Test-Path "results\*.log") { Remove-Item "results\*.log" -Force }
if (Test-Path "results\html-report") { Remove-Item "results\html-report" -Recurse -Force }

Write-Host "📋 Configuração do teste:" -ForegroundColor Cyan
Write-Host "   • Host: localhost"
Write-Host "   • Porta: 9090 (Gateway UDP)"
Write-Host "   • Padrões: Singleton, Strategy, Observer, Proxy"
Write-Host "   • Sensores: 5 threads simulando sensores IoT"
Write-Host "   • Duração: ~60 segundos"
Write-Host ""

Write-Host "⚠️  IMPORTANTE: Certifique-se de que o sistema IoT esteja rodando!" -ForegroundColor Yellow
Write-Host "   Execute antes: mvn compile exec:java" -ForegroundColor Yellow
Write-Host ""

$continue = Read-Host "Continuar com os testes? (s/n)"
if ($continue -ne "s" -and $continue -ne "S") {
    Write-Host "Teste cancelado." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "🧪 Executando testes dos Padrões GoF..." -ForegroundColor Green

# Executar JMeter em modo non-GUI
$jmeterArgs = @(
    "-n",
    "-t", "IoT_GoF_Patterns_UDP_Test.jmx",
    "-l", "results/iot_gof_test_results.jtl",
    "-e", "-o", "results/html-report",
    "-Jjmeter.save.saveservice.output_format=xml",
    "-Jjmeter.save.saveservice.response_data=true",
    "-Jjmeter.save.saveservice.samplerData=true",
    "-Jjmeter.save.saveservice.requestHeaders=true",
    "-Jjmeter.save.saveservice.responseHeaders=true"
)

$process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$jmeterPath`"", ($jmeterArgs -join " ") -Wait -NoNewWindow -PassThru

if ($process.ExitCode -eq 0) {
    Write-Host ""
    Write-Host "✅ Testes concluídos com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Resultados disponíveis em:" -ForegroundColor Cyan
    Write-Host "   • results/iot_gof_test_results.jtl - Dados brutos"
    Write-Host "   • results/html-report/index.html - Relatório HTML"
    Write-Host ""
    Write-Host "🔍 Verificações realizadas:" -ForegroundColor Yellow
    Write-Host "   ✓ Strategy Pattern - Comunicação UDP"
    Write-Host "   ✓ Singleton Pattern - Gateway único"
    Write-Host "   ✓ Observer Pattern - Monitoramento heartbeat"
    Write-Host "   ✓ Proxy Pattern - Roteamento de mensagens"
    Write-Host ""
    
    # Analisar resultados
    if (Test-Path "results\iot_gof_test_results.jtl") {
        $content = Get-Content "results\iot_gof_test_results.jtl"
        $successCount = ($content | Select-String 'success="true"').Count
        $errorCount = ($content | Select-String 'success="false"').Count
        
        Write-Host "📈 Resumo rápido dos resultados:" -ForegroundColor Cyan
        Write-Host "   • Testes bem-sucedidos: $successCount" -ForegroundColor Green
        Write-Host "   • Testes com erro: $errorCount" -ForegroundColor $(if($errorCount -gt 0) {"Red"} else {"Green"})
    }
    
    Write-Host ""
    $openReport = Read-Host "Abrir relatório HTML? (s/n)"
    if ($openReport -eq "s" -or $openReport -eq "S") {
        Start-Process "results\html-report\index.html"
    }
} else {
    Write-Host ""
    Write-Host "❌ Erro na execução dos testes!" -ForegroundColor Red
    Write-Host "   Verifique se o sistema IoT está rodando na porta 9090"
    Write-Host "   Verifique os logs do JMeter para mais detalhes"
}

Write-Host ""
Read-Host "Pressione Enter para sair"