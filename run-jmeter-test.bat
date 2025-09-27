@echo off
echo ===============================================
echo SISTEMA DISTRIBUIDO - TESTE DE PERFORMANCE HTTP
echo ===============================================
echo.
echo Verificando se o servidor hibrido esta executando...
echo Certifique-se de que o HybridServerApplication esteja rodando em:
echo - UDP Server: porta 9090
echo - HTTP Server: porta 9091
echo.
pause

echo.
echo Iniciando teste JMeter...

jmeter -n -t "jmeter-tests\sistema-distribuido-http-test.jmx" ^
       -l "jmeter-results\test-results-%date:~6,4%-%date:~3,2%-%date:~0,2%-%time:~0,2%-%time:~3,2%-%time:~6,2%.jtl" ^
       -e -o "jmeter-results\html-report-%date:~6,4%-%date:~3,2%-%date:~0,2%-%time:~0,2%-%time:~3,2%-%time:~6,2%" ^
       -j "jmeter-results\jmeter.log"

echo.
echo ===============================================
echo TESTE CONCLUIDO!
echo ===============================================
echo.
echo Resultados salvos em:
echo - Logs: jmeter-results\jmeter.log
echo - Dados: jmeter-results\test-results-*.jtl
echo - Relatorio HTML: jmeter-results\html-report-*\index.html
echo.
pause