# Script para extrair e instalar o certificado de um MSIX assinado
# Execute como Administrador
# Use este script se você já tem um MSIX assinado mas o certificado não está instalado

param(
    [Parameter(Mandatory=$true)]
    [string]$MsixPath
)

if (-not (Test-Path $MsixPath)) {
    Write-Error "Arquivo MSIX não encontrado: $MsixPath"
    exit 1
}

Write-Host "Extraindo certificado do MSIX: $MsixPath" -ForegroundColor Cyan

# Extrai a assinatura do MSIX
$tempDir = Join-Path $env:TEMP "msix_cert_extract"
if (Test-Path $tempDir) {
    Remove-Item $tempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

try {
    # Usa Get-AuthenticodeSignature para obter o certificado
    $signature = Get-AuthenticodeSignature -FilePath $MsixPath
    
    if ($signature.Status -eq "NotSigned") {
        Write-Error "O MSIX não está assinado!"
        exit 1
    }
    
    $cert = $signature.SignerCertificate
    
    Write-Host "`nInformações do Certificado:" -ForegroundColor Cyan
    Write-Host "Subject: $($cert.Subject)"
    Write-Host "Issuer: $($cert.Issuer)"
    Write-Host "Thumbprint: $($cert.Thumbprint)"
    Write-Host "Válido até: $($cert.NotAfter)"
    
    # Instala o certificado na raiz confiável
    Write-Host "`nInstalando certificado na raiz confiável..." -ForegroundColor Yellow
    $store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "LocalMachine")
    $store.Open("ReadWrite")
    $store.Add($cert)
    $store.Close()
    
    Write-Host "`nCertificado instalado com sucesso!" -ForegroundColor Green
    Write-Host "Agora você pode instalar o MSIX normalmente." -ForegroundColor Green
    
} catch {
    Write-Error "Erro ao processar o certificado: $_"
    exit 1
} finally {
    if (Test-Path $tempDir) {
        Remove-Item $tempDir -Recurse -Force
    }
}
