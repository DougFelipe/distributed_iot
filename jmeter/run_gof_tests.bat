@echo off
echo ===============================================================================
echo                    JMETER - TESTE DOS PADROES GoF UDP
echo                     Sistema IoT Distribuido - Sprint 2
echo ===============================================================================
echo.

REM Configurar caminho do JMeter
set JMETER_PATH=D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat

REM Verificar se JMeter existe no caminho especificado
if not exist "%JMETER_PATH%" (
    echo ❌ JMeter nao encontrado em: %JMETER_PATH%
    echo.
    echo 📋 Verifique se o JMeter esta instalado em:
    echo    D:\Downloads\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\
    echo.
    echo    Ou ajuste a variavel JMETER_PATH no script
    echo.
    pause
    exit /b 1
)

echo 🚀 Iniciando testes JMeter para Padroes GoF...
echo.

REM Limpar resultados anteriores
if exist "results\*.jtl" del /q "results\*.jtl"
if exist "results\*.log" del /q "results\*.log"

echo 📋 Configuracao do teste:
echo    • Host: localhost
echo    • Porta: 9090 (Gateway UDP)
echo    • Padroes: Singleton, Strategy, Observer, Proxy
echo    • Sensores: 5 threads simulando sensores IoT
echo    • Duracao: ~60 segundos
echo.

echo ⚠️  IMPORTANTE: Certifique-se de que o sistema IoT esteja rodando!
echo    Execute antes: mvn compile exec:java
echo.

set /p continue="Continuar com os testes? (s/n): "
if /i not "%continue%"=="s" (
    echo Teste cancelado.
    exit /b 0
)

echo.
echo 🧪 Executando testes dos Padroes GoF...

REM Executar JMeter em modo non-GUI
"%JMETER_PATH%" -n -t "IoT_GoF_Patterns_UDP_Test.jmx" ^
       -l "results/iot_gof_test_results.jtl" ^
       -e -o "results/html-report" ^
       -Jjmeter.save.saveservice.output_format=xml ^
       -Jjmeter.save.saveservice.response_data=true ^
       -Jjmeter.save.saveservice.samplerData=true ^
       -Jjmeter.save.saveservice.requestHeaders=true ^
       -Jjmeter.save.saveservice.responseHeaders=true

if %errorlevel% equ 0 (
    echo.
    echo ✅ Testes concluidos com sucesso!
    echo.
    echo 📊 Resultados disponiveis em:
    echo    • results/iot_gof_test_results.jtl - Dados brutos
    echo    • results/html-report/index.html - Relatorio HTML
    echo.
    echo 🔍 Verificacoes realizadas:
    echo    ✓ Strategy Pattern - Comunicacao UDP
    echo    ✓ Singleton Pattern - Gateway unico
    echo    ✓ Observer Pattern - Monitoramento heartbeat
    echo    ✓ Proxy Pattern - Roteamento de mensagens
    echo.
    
    REM Verificar se arquivo de resultados foi criado
    if exist "results\iot_gof_test_results.jtl" (
        echo 📈 Resumo rapido dos resultados:
        powershell -Command "Get-Content 'results\iot_gof_test_results.jtl' | Where-Object { $_ -match 'success=\"true\"' } | Measure-Object | Select-Object -ExpandProperty Count" > temp_success.txt
        set /p success_count=<temp_success.txt
        powershell -Command "Get-Content 'results\iot_gof_test_results.jtl' | Where-Object { $_ -match 'success=\"false\"' } | Measure-Object | Select-Object -ExpandProperty Count" > temp_errors.txt
        set /p error_count=<temp_errors.txt
        del temp_success.txt temp_errors.txt
        
        echo    • Testes bem-sucedidos: %success_count%
        echo    • Testes com erro: %error_count%
    )
    
    echo.
    set /p open_report="Abrir relatorio HTML? (s/n): "
    if /i "%open_report%"=="s" (
        start "" "results\html-report\index.html"
    )
) else (
    echo.
    echo ❌ Erro na execucao dos testes!
    echo    Verifique se o sistema IoT esta rodando na porta 9090
    echo    Verifique os logs do JMeter para mais detalhes
)

echo.
pause