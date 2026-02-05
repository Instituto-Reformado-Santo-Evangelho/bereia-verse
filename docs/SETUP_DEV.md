# Configuração de Ambiente de Desenvolvimento

## 📋 Pré-requisitos

- Java JDK 17
- Windows SDK (para compilar MSIX)
- PowerShell

## 🔐 Configuração de Credenciais

### Opção 1: Configuração Manual

1. Copie o arquivo de exemplo:
```powershell
cp .env.example .env
```

2. Edite `.env` e preencha com suas credenciais do GitHub Actions:
   - `GH_PAT_BEREIA` - Personal Access Token do GitHub
   - `GOOGLE_CLIENT_SECRETS` - Conteúdo do client_secrets.json (uma linha)
   - `MSIX_PFX_BASE64` - Certificado em Base64
   - `MSIX_PFX_PASSWORD` - Senha do certificado

### Opção 2: Configuração Automática via Script

```powershell
.\setup-env.ps1 `
    -GitHubPat "seu_token_aqui" `
    -GoogleClientSecrets '{"installed":{...}}' `
    -MsixPfxBase64 "base64_do_certificado" `
    -MsixPfxPassword "irse2026"
```

## 🏃 Executar Aplicação

```powershell
.\run.ps1
```

Este script:
- Carrega variáveis do `.env`
- Cria `client_secrets.json` se necessário
- Restaura certificado de Base64
- Executa a aplicação

## 📦 Compilar MSIX

```powershell
.\build-msix.ps1
```

Saída: `composeApp\build\outputs\msix\BereiaVerse.msix`

## 🔑 Criar Novo Certificado

```powershell
cd installers\windows
.\create-cert.ps1
```

Depois, adicione ao `.env`:
```powershell
# Converter PFX para Base64
$pfxBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("installers\windows\BereiaVerse_SelfSigned.pfx"))
Write-Output $pfxBase64
```

## 📂 Estrutura de Arquivos

```
bereia-verse/
├── .env                          # Credenciais locais (não commitado)
├── .env.example                  # Template de configuração
├── client_secrets.json           # Criado automaticamente do .env
├── run.ps1                       # Executar app
├── build-msix.ps1               # Compilar MSIX
├── setup-env.ps1                # Configurar .env automaticamente
└── installers/
    └── windows/
        ├── create-cert.ps1      # Criar certificado
        └── BereiaVerse_SelfSigned.pfx  # Certificado local
```

## 🔄 Sincronizar com GitHub Actions

Para baixar secrets do GitHub Actions e configurar localmente:

1. Acesse: https://github.com/seu-usuario/bereia-verse/settings/secrets/actions
2. Copie os valores dos secrets
3. Execute `.\setup-env.ps1` com os valores

## ⚙️ Variáveis de Ambiente

| Variável | Descrição | Obrigatório |
|----------|-----------|-------------|
| `GH_PAT_BEREIA` | GitHub Personal Access Token | Não |
| `GOOGLE_CLIENT_SECRETS` | JSON das credenciais do Google Drive | Sim (para sync) |
| `GOOGLE_CLIENT_SECRETS_PATH` | Caminho alternativo para client_secrets.json | Não |
| `MSIX_PFX_BASE64` | Certificado Windows em Base64 | Não |
| `MSIX_PFX_PASSWORD` | Senha do certificado | Sim (para MSIX) |
| `MSIX_PFX_PATH` | Caminho do arquivo PFX | Sim (para MSIX) |

## 🐛 Troubleshooting

### "client_secrets.json não encontrado"

1. Verifique se `.env` existe e tem `GOOGLE_CLIENT_SECRETS`
2. Execute `.\run.ps1` ao invés de `.\gradlew.bat` diretamente
3. Ou crie manualmente: `echo $env:GOOGLE_CLIENT_SECRETS > client_secrets.json`

### "JAVA_HOME is not set"

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
```

### "makeappx.exe not found"

Instale o Windows SDK:
```powershell
winget install Microsoft.WindowsSDK.10.0.22621
```
