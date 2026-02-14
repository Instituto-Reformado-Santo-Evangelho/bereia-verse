# Script para compilar MSIX do Bereia Verse com variáveis de ambiente do .env

param(
    [switch]$SkipEnv
)

$envFile = Join-Path $PSScriptRoot ".env"

if (-not $SkipEnv) {
    if (-not (Test-Path $envFile)) {
        Write-Host "Arquivo .env nao encontrado!" -ForegroundColor Red
        Write-Host "Copie .env.example para .env e configure suas credenciais" -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Carregando variaveis de ambiente de .env..." -ForegroundColor Cyan

    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  OK $name" -ForegroundColor Green
        }
    }
}

if (-not $env:JAVA_HOME) {
    $jdkPath = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
    if (Test-Path $jdkPath) {
        $env:JAVA_HOME = $jdkPath
        Write-Host "  OK JAVA_HOME configurado" -ForegroundColor Green
    }
}

$pfxPath = [Environment]::GetEnvironmentVariable("MSIX_PFX_PATH", "Process")
$pfxPassword = [Environment]::GetEnvironmentVariable("MSIX_PFX_PASSWORD", "Process")
$pfxBase64 = [Environment]::GetEnvironmentVariable("MSIX_PFX_BASE64", "Process")

if ($pfxBase64 -and $pfxPath) {
    $fullPfxPath = Join-Path $PSScriptRoot $pfxPath
    if (-not (Test-Path $fullPfxPath)) {
        Write-Host "Restaurando certificado de Base64..." -ForegroundColor Cyan
        try {
            $pfxBytes = [Convert]::FromBase64String($pfxBase64)
            New-Item -ItemType Directory -Force -Path (Split-Path $fullPfxPath) | Out-Null
            [IO.File]::WriteAllBytes($fullPfxPath, $pfxBytes)
            Write-Host "  OK Certificado restaurado" -ForegroundColor Green
        } catch {
            Write-Host "  Aviso: Erro ao restaurar certificado: $_" -ForegroundColor Yellow
        }
    }
}

$fullPfxPath = Join-Path $PSScriptRoot $pfxPath
if (-not (Test-Path $fullPfxPath)) {
    Write-Host "Certificado nao encontrado: $fullPfxPath" -ForegroundColor Red
    Write-Host "Execute: .\installers\windows\create-cert.ps1" -ForegroundColor Yellow
    exit 1
}

# Cria client_secrets.json ANTES da compilação para incluir no JAR
$clientSecretsPath = Join-Path $PSScriptRoot "composeApp\src\commonMain\composeResources\files\client_secrets.json"
$googleClientSecrets = [Environment]::GetEnvironmentVariable("GOOGLE_CLIENT_SECRETS", "Process")
$createdClientSecrets = $false

if ($googleClientSecrets) {
    try {
        $resourcesDir = Split-Path $clientSecretsPath
        New-Item -ItemType Directory -Force -Path $resourcesDir | Out-Null
        [IO.File]::WriteAllText($clientSecretsPath, $googleClientSecrets, [System.Text.Encoding]::UTF8)
        Write-Host "  OK client_secrets.json criado para build" -ForegroundColor Green
        $createdClientSecrets = $true
    } catch {
        Write-Host "  Aviso: Erro ao criar client_secrets.json: $_" -ForegroundColor Yellow
    }
}

Write-Host "`nCompilando MSIX..." -ForegroundColor Cyan
Write-Host "Certificado: $fullPfxPath" -ForegroundColor Gray

& .\gradlew.bat :composeApp:createMsix "-Pmsix.pfx.path=$fullPfxPath" "-Pmsix.pfx.password=$pfxPassword"

$buildSuccess = $LASTEXITCODE -eq 0

# Remove client_secrets.json temporário apenas no final
if ($createdClientSecrets -and (Test-Path $clientSecretsPath)) {
    Remove-Item $clientSecretsPath -Force
    Write-Host "  OK client_secrets.json temporario removido" -ForegroundColor Gray
}

if ($buildSuccess) {
    Write-Host "`nMSIX compilado com sucesso!" -ForegroundColor Green
    Write-Host "Localizacao: composeApp\build\outputs\msix\BereiaVerse.msix" -ForegroundColor Cyan
} else {
    Write-Host "`nErro na compilacao" -ForegroundColor Red
}
