# 🔧 Guia do ReceiverPortManager.ps1

## 📋 Visão Geral

O **ReceiverPortManager.ps1** é um script PowerShell para monitorar e controlar (pausar/retomar) processos que estão ouvindo nas portas **9091** e **9092** dos Data Receivers UDP do sistema IoT distribuído.

### ✨ Funcionalidades

- 🔍 **Monitoramento** em tempo real das portas dos receivers
- ⏸️ **Pausar** processos específicos (suspender threads)
- ▶️ **Retomar** processos suspensos
- 📊 **Status detalhado** de processos por porta
- 🎛️ **Interface de menu** interativa

---

## ⚡ Pré-requisitos

### 🔐 Permissões de Administrador
> **⚠️ IMPORTANTE:** Para pausar/retomar processos, o script **DEVE** ser executado como Administrador.

### 🛠️ Ferramentas Necessárias

#### Opção 1: PsSuspend.exe (Recomendado)
- **Download:** [Sysinternals PsSuspend](https://docs.microsoft.com/en-us/sysinternals/downloads/pssuspend)
- **Local:** Coloque o `PsSuspend.exe` na mesma pasta do script
- **Vantagem:** Funciona em qualquer versão do PowerShell

#### Opção 2: PowerShell 7+
- **Download:** [PowerShell 7+](https://github.com/PowerShell/PowerShell/releases)
- **Vantagem:** Comandos nativos `Suspend-Process` e `Resume-Process`

---

## 🚀 Como Executar como Administrador

### Método 1: PowerShell como Admin (Recomendado)

1. **Pressione** `Win + X` ou clique com botão direito no menu Iniciar
2. **Selecione** "Windows PowerShell (Administrador)" ou "Terminal (Administrador)"
3. **Confirme** o UAC (Controle de Conta de Usuário)
4. **Navegue** até a pasta do projeto:
   ```powershell
   cd D:\distribuida
   ```
5. **Execute** o script:
   ```powershell
   .\ReceiverPortManager.ps1
   ```

### Método 2: Via Explorador de Arquivos

1. **Navegue** até a pasta `D:\distribuida`
2. **Clique com botão direito** no arquivo `ReceiverPortManager.ps1`
3. **Selecione** "Executar com PowerShell"
4. **Se solicitado**, confirme execução como administrador

### Método 3: Atalho com Privilégios Elevados

1. **Crie um atalho** na área de trabalho com o seguinte destino:
   ```
   powershell.exe -ExecutionPolicy Bypass -File "D:\distribuida\ReceiverPortManager.ps1"
   ```
2. **Clique com botão direito** no atalho → **Propriedades**
3. **Aba "Atalho"** → **Avançado** → ✅ **"Executar como administrador"**
4. **Aplicar** e **OK**

---

## 📖 Como Usar o Script

### 🎯 Menu Principal
```
============================================================
   Receiver Port Manager (9091 / 9092) - Menu
============================================================
[1] Mostrar status das portas (uma vez)
[2] Monitorar continuamente (até tecla)
[3] Pausar porta 9091
[4] Retomar porta 9091
[5] Pausar porta 9092
[6] Retomar porta 9092
[7] Pausar porta (informar)
[8] Retomar porta (informar)
[9] Mostrar status detalhado agora
[0] Sair
============================================================
```

### 🔧 Operações Disponíveis

#### 📊 **Opção 1: Status das Portas**
- **Função:** Mostra uma vez o status atual das portas 9091 e 9092
- **Uso:** Verificar se há processos ouvindo nas portas
- **Exemplo de saída:**
  ```
  Porta 9091:
  Protocol LocalAddress LocalPort State PID  Process
  -------- ------------ --------- ----- ---- -------
  UDP      0.0.0.0      9091      UDP   1234 java
  
  Porta 9092:
  Protocol LocalAddress LocalPort State PID  Process
  -------- ------------ --------- ----- ---- -------
  UDP      0.0.0.0      9092      UDP   5678 java
  ```

#### 🔄 **Opção 2: Monitoramento Contínuo**
- **Função:** Atualiza automaticamente o status das portas
- **Uso:** Monitorar em tempo real
- **Controles:** 
  - Pressione **qualquer tecla** para parar
  - **CTRL+C** para forçar saída
- **Configuração:** Define intervalo em segundos (padrão: 2s)

#### ⏸️ **Opções 3-6: Pausar/Retomar Portas Específicas**
- **Pausar 9091/9092:** Suspende todos os processos da porta especificada
- **Retomar 9091/9092:** Reativa todos os processos suspensos da porta
- **⚠️ Requer:** Execução como administrador

#### 🎛️ **Opções 7-8: Portas Personalizadas**
- **Função:** Pausar/retomar qualquer porta informada pelo usuário
- **Uso:** Digite o número da porta quando solicitado
- **Exemplo:** 
  ```
  Informe a porta a pausar: 9090
  ```

#### 📋 **Opção 9: Status Detalhado**
- **Função:** Igual à opção 1, mas no contexto do menu
- **Uso:** Verificação rápida sem sair do menu

---

## 🔍 Cenários de Uso

### 🧪 **Teste de Falha de Receiver**
```powershell
# 1. Iniciar o sistema IoT
mvn exec:java@multi-protocol -Dexec.args=UDP

# 2. Em outro terminal como Admin, executar o script
.\ReceiverPortManager.ps1

# 3. Pausar um receiver para simular falha
Escolha: 3  # Pausar porta 9091

# 4. Executar testes JMeter
# 5. Retomar o receiver
Escolha: 4  # Retomar porta 9091
```

### 📊 **Monitoramento Durante Testes de Carga**
```powershell
# 1. Monitorar continuamente
Escolha: 2
Intervalo: 1  # Atualizar a cada segundo

# 2. Observar PIDs e uso durante os testes JMeter
# 3. Pressionar qualquer tecla para parar quando necessário
```

### 🔄 **Simulação de Recuperação Automática**
```powershell
# Cenário: Testar comportamento do Gateway quando receivers ficam indisponíveis
# 1. Pausar DATA_RECEIVER_UDP_1 (porta 9091)
# 2. Verificar redirecionamento para DATA_RECEIVER_UDP_2 (porta 9092)
# 3. Retomar DATA_RECEIVER_UDP_1
# 4. Verificar balanceamento restored
```

---

## ⚠️ Troubleshooting

### ❌ **Erro: "Execute este script como Administrador"**
**Solução:** Siga os métodos de execução como administrador descritos acima.

### ❌ **Erro: "PsSuspend.exe não encontrado" ou Loop Infinito**
**🔄 NOVO:** O script agora oferece múltiplas opções quando você tenta pausar um processo:

**Opções disponíveis:**
1. **PsSuspend (Recomendado):** Suspende temporariamente o processo
2. **PowerShell Nativo:** Usa comandos nativos (PowerShell 7+)
3. **Terminar Processo:** Última opção (mata o processo - requer confirmação)

**Soluções para problemas com PsSuspend:**
1. **Download PsSuspend:** [Link Sysinternals](https://docs.microsoft.com/en-us/sysinternals/downloads/pssuspend)
2. **Extrair na pasta:** `D:\distribuida\PSTools\` 
3. **OU usar alternativa:** Escolha opção 2 ou 3 no menu do script

### ❌ **Erro: "Nenhum processo ouvindo na porta"**
**Causas possíveis:**
- Sistema IoT não está rodando
- Portas diferentes (verificar logs)
- Processo falhou ao inicializar

**Verificação:**
```powershell
# Verificar se o sistema está rodando
netstat -an | findstr "909"
```

### ❌ **Script não executa/erro de sintaxe**
**Soluções:**
1. **Verificar ExecutionPolicy:**
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```
2. **Executar diretamente:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\ReceiverPortManager.ps1
   ```

---

## 🔧 Configuração Avançada

### 📝 **Personalizar Portas Monitoradas**
Para alterar as portas padrão, edite as linhas no script:

```powershell
# Localizar e alterar:
Show-PortStatus -Port 9091  # Alterar 9091 para sua porta
Show-PortStatus -Port 9092  # Alterar 9092 para sua porta
```

### ⏱️ **Ajustar Intervalo de Monitoramento**
No menu, opção 2, você pode definir:
- **1 segundo:** Monitoramento intensivo
- **2 segundos:** Padrão (recomendado)
- **5+ segundos:** Monitoramento leve

---

## 📚 Integração com Testes

### 🧪 **Workflow de Teste Completo**

1. **Preparação:**
   ```powershell
   # Terminal 1: Iniciar sistema
   mvn exec:java@multi-protocol -Dexec.args=UDP
   
   # Terminal 2: Monitorar (como Admin)
   .\ReceiverPortManager.ps1
   ```

2. **Execução:**
   - Iniciar testes JMeter
   - Pausar receivers conforme cenário de teste
   - Monitorar comportamento do gateway
   - Retomar receivers
   - Validar recuperação

3. **Análise:**
   - Logs do sistema: `logs/sistema-distribuido.log`
   - Resultados JMeter: `jmeter/results/`
   - Status dos processos via script

---

## 🎯 Próximos Passos

Com o **ReceiverPortManager.ps1** você pode:

1. ✅ **Simular falhas** de Data Receivers
2. ✅ **Testar recuperação** automática do sistema
3. ✅ **Validar balanceamento** do Gateway
4. ✅ **Monitorar performance** durante testes de carga
5. 🔄 **Implementar automação** de cenários de teste

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Verificar logs em `logs/sistema-distribuido.log`
2. Executar diagnósticos com opção 9 do menu
3. Verificar se todos os pré-requisitos estão atendidos
4. Confirmar execução como administrador

**Versão do Script:** v1.0  
**Compatibilidade:** Windows PowerShell 5.1+ / PowerShell 7+  
**Última Atualização:** Outubro 2025