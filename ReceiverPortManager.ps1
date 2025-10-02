<# 
    ReceiverPortManager.ps1
    Menu interativo para monitorar e pausar/retomar processos do Sistema IoT Distribuído.
    
    Portas monitoradas:
      - Gateway UDP: 9090
      - Data Receiver 1: 9091  
      - Data Receiver 2: 9092

    Funciona com:
      - PsSuspend.exe (preferido, Sysinternals) 
      - OU (fallback) Suspend-Process / Resume-Process (PowerShell 7+)

    Execute como Administrador.
#>

[CmdletBinding()]
param()

# ---------------------- Utilidades ----------------------

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p  = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-PsSuspendPath {
    # Primeiro tenta encontrar no PATH do sistema
    $cmd = Get-Command pssuspend -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    
    # Procura no diretório PSTools do projeto
    $psToolsPath = Join-Path -Path (Split-Path -Parent $PSCommandPath) -ChildPath "PSTools\pssuspend.exe"
    if (Test-Path $psToolsPath) { return $psToolsPath }
    
    # Procura versão 64-bit no PSTools
    $psTools64Path = Join-Path -Path (Split-Path -Parent $PSCommandPath) -ChildPath "PSTools\pssuspend64.exe"
    if (Test-Path $psTools64Path) { return $psTools64Path }
    
    # Procura no diretório do script (nome original)
    $local = Join-Path -Path (Split-Path -Parent $PSCommandPath) -ChildPath "PsSuspend.exe"
    if (Test-Path $local) { return $local }
    
    return $null
}

function Test-PsSuspendWorking {
    param([string]$PsSuspendPath)
    
    if (-not $PsSuspendPath -or -not (Test-Path $PsSuspendPath)) {
        return $false
    }
    
    try {
        # Testa executando com -? para ver se responde (com timeout)
        $null = Start-Process -FilePath $PsSuspendPath -ArgumentList "-accepteula", "-?" -Wait -WindowStyle Hidden -PassThru -ErrorAction Stop
        return $true
    } catch {
        Write-Host "PsSuspend não está funcionando corretamente: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

function Stop-ProcessByPID {
    param([int[]]$PIDList)
    
    foreach ($processId in $PIDList | Select-Object -Unique) {
        try {
            Write-Host "Terminando processo PID $processId..." -ForegroundColor Red
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "Processo PID $processId terminado." -ForegroundColor Green
        } catch {
            Write-Host "Erro ao terminar processo PID $processId : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

function Get-PortBindings {
    param(
        [Parameter(Mandatory=$true)][int]$Port
    )
    $tcp = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
           Select-Object LocalAddress,LocalPort,State,OwningProcess,CreationTime
    $udp = Get-NetUDPEndpoint -LocalPort $Port -ErrorAction SilentlyContinue |
           Select-Object LocalAddress,LocalPort,@{n='State';e={'UDP'}},OwningProcess
    # enriquecer com nome do processo
    $all = @()
    $connections = @()
    if ($tcp) { $connections += $tcp }
    if ($udp) { $connections += $udp }
    
    foreach ($c in $connections) {
        if ($null -ne $c.OwningProcess -and $c.OwningProcess -ne 0) {
            try {
                $p = Get-Process -Id $c.OwningProcess -ErrorAction Stop
                $proc = $p.ProcessName
            } catch { $proc = "<desconhecido>" }
        } else { $proc = "<n/d>" }
        $protocol = if ($c.State -eq 'UDP') { 'UDP' } else { 'TCP' }
        $obj = [PSCustomObject]@{
            Protocol      = $protocol
            LocalAddress  = $c.LocalAddress
            LocalPort     = $c.LocalPort
            State         = $c.State
            PID           = $c.OwningProcess
            Process       = $proc
        }
        $all += $obj
    }
    return $all | Sort-Object Protocol, PID -Unique
}

function Show-PortStatus {
    param([int]$Port)
    $data = Get-PortBindings -Port $Port
    if (-not $data -or $data.Count -eq 0) {
        Write-Host ("Porta {0}: nenhum processo ouvindo." -f $Port) -ForegroundColor Yellow
    } else {
        Write-Host ("Porta {0}:" -f $Port) -ForegroundColor Cyan
        $data | Format-Table Protocol,LocalAddress,LocalPort,State,PID,Process -AutoSize
    }
}

function Show-CurrentBindings {
    Write-Host "---- Status atual ----" -ForegroundColor Cyan
    Write-Host "Gateway UDP (porta 9090):" -ForegroundColor Magenta
    Show-PortStatus -Port 9090
    Write-Host ""
    Write-Host "Data Receivers UDP:" -ForegroundColor Magenta
    Show-PortStatus -Port 9091
    Write-Host ""
    Show-PortStatus -Port 9092
    Write-Host "----------------------" -ForegroundColor Cyan
}

# ---------------------- Suspender / Retomar ----------------------

function Suspend-ByPID {
    param(
        [Parameter(Mandatory=$true)][int[]]$PIDList,
        [Parameter(Mandatory=$false)][string]$PsSuspendPath
    )

    if ($PIDList.Count -eq 0) {
        Write-Host "Nenhum PID encontrado para suspender." -ForegroundColor Yellow
        return
    }

    if ($PsSuspendPath) {
        foreach ($processId in $PIDList | Select-Object -Unique) {
            Write-Host "Suspending PID $processId via PsSuspend..." -ForegroundColor Magenta
            try {
                # Usar -AcceptEula para evitar prompt de EULA
                $result = Start-Process -FilePath $PsSuspendPath -ArgumentList "-accepteula", "$processId" -Wait -WindowStyle Hidden -PassThru
                if ($result.ExitCode -eq 0) {
                    Write-Host "PID $processId suspenso com sucesso." -ForegroundColor Green
                } else {
                    Write-Host "Erro ao suspender PID $processId (Exit Code: $($result.ExitCode))" -ForegroundColor Red
                }
            } catch {
                Write-Host "Erro ao executar PsSuspend para PID $processId : $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    } else {
        # fallback para PowerShell 7+
        foreach ($processId in $PIDList | Select-Object -Unique) {
            Write-Host "Suspending PID $processId via Suspend-Process..." -ForegroundColor Magenta
            Suspend-Process -Id $processId -ErrorAction Stop
        }
    }
}

function Resume-ByPID {
    param(
        [Parameter(Mandatory=$true)][int[]]$PIDList,
        [Parameter(Mandatory=$false)][string]$PsSuspendPath
    )

    if ($PIDList.Count -eq 0) {
        Write-Host "Nenhum PID encontrado para retomar." -ForegroundColor Yellow
        return
    }

    if ($PsSuspendPath) {
        foreach ($processId in $PIDList | Select-Object -Unique) {
            Write-Host "Resuming PID $processId via PsSuspend..." -ForegroundColor Green
            try {
                # Usar -AcceptEula para evitar prompt de EULA e -r para resume
                $result = Start-Process -FilePath $PsSuspendPath -ArgumentList "-accepteula", "-r", "$processId" -Wait -WindowStyle Hidden -PassThru
                if ($result.ExitCode -eq 0) {
                    Write-Host "PID $processId retomado com sucesso." -ForegroundColor Green
                } else {
                    Write-Host "Erro ao retomar PID $processId (Exit Code: $($result.ExitCode))" -ForegroundColor Red
                }
            } catch {
                Write-Host "Erro ao executar PsSuspend para PID $processId : $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    } else {
        # fallback para PowerShell 7+
        foreach ($processId in $PIDList | Select-Object -Unique) {
            Write-Host "Resuming PID $processId via Resume-Process..." -ForegroundColor Green
            Resume-Process -Id $processId -ErrorAction Stop
        }
    }
}

function Get-PIDsByPort {
    param([int]$Port)

    $bindings = Get-PortBindings -Port $Port
    $pids = @()
    foreach ($b in $bindings) {
        if ($b.PID -and $b.PID -ne 0) { $pids += $b.PID }
    }
    return $pids | Sort-Object -Unique
}

function Suspend-PortProcesses {
    param([int]$Port)

    if (-not (Test-IsAdmin)) {
        Write-Host "Execute este script como Administrador para suspender processos." -ForegroundColor Red
        return
    }

    $pids = Get-PIDsByPort -Port $Port
    if (-not $pids -or $pids.Count -eq 0) {
        Write-Host "Nenhum processo ouvindo na porta $Port." -ForegroundColor Yellow
        return
    }

    Write-Host "Processos encontrados na porta $Port : $($pids -join ', ')" -ForegroundColor Cyan
    Write-Host "Escolha a ação:" -ForegroundColor Yellow
    Write-Host "[1] Suspender com PsSuspend (recomendado)" -ForegroundColor Green
    Write-Host "[2] Suspender com PowerShell (PowerShell 7+)" -ForegroundColor Green
    Write-Host "[3] Terminar processos (CUIDADO: mata os processos)" -ForegroundColor Red
    Write-Host "[0] Cancelar" -ForegroundColor Gray
    
    $choice = Read-Host "Opção"
    
    switch ($choice) {
        '1' {
            $pss = Get-PsSuspendPath
            if ($pss) {
                Write-Host "Suspendendo processos com PsSuspend..." -ForegroundColor Magenta
                Suspend-ByPID -PIDList $pids -PsSuspendPath $pss
            } else {
                Write-Host "PsSuspend não encontrado!" -ForegroundColor Red
            }
        }
        '2' {
            if (Get-Command Suspend-Process -ErrorAction SilentlyContinue) {
                Write-Host "Suspendendo processos com PowerShell..." -ForegroundColor Magenta
                Suspend-ByPID -PIDList $pids -PsSuspendPath $null
            } else {
                Write-Host "Suspend-Process não disponível (necessário PowerShell 7+)" -ForegroundColor Red
            }
        }
        '3' {
            Write-Host "ATENÇÃO: Isso irá TERMINAR os processos!" -ForegroundColor Red
            $confirm = Read-Host "Digite 'CONFIRMAR' para prosseguir"
            if ($confirm -eq "CONFIRMAR") {
                Stop-ProcessByPID -PIDList $pids
            } else {
                Write-Host "Operação cancelada." -ForegroundColor Yellow
            }
        }
        '0' {
            Write-Host "Operação cancelada." -ForegroundColor Yellow
            return
        }
        Default {
            Write-Host "Opção inválida." -ForegroundColor Red
            return
        }
    }
}

function Resume-PortProcesses {
    param([int]$Port)

    if (-not (Test-IsAdmin)) {
        Write-Host "Execute este script como Administrador para retomar processos." -ForegroundColor Red
        return
    }

    $pids = Get-PIDsByPort -Port $Port
    if (-not $pids -or $pids.Count -eq 0) {
        Write-Host "Nenhum processo ouvindo na porta $Port (nada a retomar)." -ForegroundColor Yellow
        return
    }

    Write-Host "Processos encontrados na porta $Port : $($pids -join ', ')" -ForegroundColor Cyan
    Write-Host "Escolha a ação:" -ForegroundColor Yellow
    Write-Host "[1] Retomar com PsSuspend (recomendado)" -ForegroundColor Green
    Write-Host "[2] Retomar com PowerShell (PowerShell 7+)" -ForegroundColor Green
    Write-Host "[0] Cancelar" -ForegroundColor Gray
    
    $choice = Read-Host "Opção"
    
    switch ($choice) {
        '1' {
            $pss = Get-PsSuspendPath
            if ($pss) {
                Write-Host "Retomando processos com PsSuspend..." -ForegroundColor Cyan
                Resume-ByPID -PIDList $pids -PsSuspendPath $pss
            } else {
                Write-Host "PsSuspend não encontrado!" -ForegroundColor Red
            }
        }
        '2' {
            if (Get-Command Resume-Process -ErrorAction SilentlyContinue) {
                Write-Host "Retomando processos com PowerShell..." -ForegroundColor Cyan
                Resume-ByPID -PIDList $pids -PsSuspendPath $null
            } else {
                Write-Host "Resume-Process não disponível (necessário PowerShell 7+)" -ForegroundColor Red
            }
        }
        '0' {
            Write-Host "Operação cancelada." -ForegroundColor Yellow
            return
        }
        Default {
            Write-Host "Opção inválida." -ForegroundColor Red
            return
        }
    }
}

# ---------------------- Monitoramento ----------------------

function Show-MonitorOnce {
    Clear-Host
    Show-CurrentBindings
}

function Start-ContinuousMonitor {
    param([int]$IntervalSeconds = 2)

    Write-Host "Monitorando continuamente (CTRL+C para sair, ou pressione qualquer tecla)..." -ForegroundColor Cyan
    while ($true) {
        if ([Console]::KeyAvailable) {
            $null = [Console]::ReadKey($true)
            break
        }
        Clear-Host
        Show-CurrentBindings
        Start-Sleep -Seconds $IntervalSeconds
    }
}

# ---------------------- Menu ----------------------

function Show-Menu {
@"
============================================================
   IoT Distributed System Port Manager - Menu
============================================================
   Gateway UDP: 9090 | Data Receivers UDP: 9091, 9092
============================================================
[1] Mostrar status das portas (uma vez)
[2] Monitorar continuamente (até tecla)
[3] Pausar Gateway (porta 9090)
[4] Retomar Gateway (porta 9090)
[5] Pausar Data Receiver 1 (porta 9091)
[6] Retomar Data Receiver 1 (porta 9091)
[7] Pausar Data Receiver 2 (porta 9092)
[8] Retomar Data Receiver 2 (porta 9092)
[9] Pausar porta (informar)
[10] Retomar porta (informar)
[11] Mostrar status detalhado agora
[0] Sair
============================================================
"@
}

# ---------------------- Loop principal ----------------------

while ($true) {
    Show-Menu
    $choice = Read-Host "Escolha uma opção"

    switch ($choice) {
        '1' { Show-MonitorOnce }
        '2' { 
            $i = Read-Host "Intervalo (segundos) [padrão=2]"
            if (-not [int]::TryParse($i, [ref]$null)) { $i = 2 }
            Start-ContinuousMonitor -IntervalSeconds ([int]$i)
        }
        '3' { Suspend-PortProcesses -Port 9090 }
        '4' { Resume-PortProcesses -Port 9090 }
        '5' { Suspend-PortProcesses -Port 9091 }
        '6' { Resume-PortProcesses -Port 9091 }
        '7' { Suspend-PortProcesses -Port 9092 }
        '8' { Resume-PortProcesses -Port 9092 }
        '9' { 
            $p = Read-Host "Informe a porta a pausar"
            if ([int]::TryParse($p, [ref]$null)) { Suspend-PortProcesses -Port ([int]$p) } else { Write-Host "Porta inválida." -ForegroundColor Red }
        }
        '10' { 
            $p = Read-Host "Informe a porta a retomar"
            if ([int]::TryParse($p, [ref]$null)) { Resume-PortProcesses -Port ([int]$p) } else { Write-Host "Porta inválida." -ForegroundColor Red }
        }
        '11' { Show-CurrentBindings }
        '0' { break }
        Default { Write-Host "Opção inválida." -ForegroundColor Red }
    }

    Write-Host ""
    $null = Read-Host "Pressione ENTER para voltar ao menu"
    Clear-Host
}

Write-Host "Encerrado." -ForegroundColor Cyan
