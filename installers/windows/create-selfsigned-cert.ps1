# Script para criar certificado auto-assinado para MSIX
# Execute como Administrador

# Configuração
$certName = "CN=Organização IRSE"
$pfxPassword = Read-Host "Digite a senha para o certificado PFX" -AsSecureString
$certOutputPath = Join-Path $PSScriptRoot "BereiaVerse_SelfSigned.pfx"

Write-Host "Criando certificado auto-assinado..." -ForegroundColor Cyan

# Cria o certificado
$cert = New-SelfSignedCertificate `
    -Type Custom `
    -Subject $certName `
    -KeyUsage DigitalSignature `
    -FriendlyName "Bereia Verse Self-Signed Certificate" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.3", "2.5.29.19={text}") `
    -NotAfter (Get-Date).AddYears(3)

# Exporta para arquivo PFX
Write-Host "Exportando certificado para $certOutputPath..." -ForegroundColor Cyan
Export-PfxCertificate -Cert $cert -FilePath $certOutputPath -Password $pfxPassword

# Instala o certificado na raiz confiável
Write-Host "Instalando certificado na raiz confiável..." -ForegroundColor Yellow
$store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "LocalMachine")
$store.Open("ReadWrite")
$store.Add($cert)
$store.Close()

Write-Host "`nCertificado criado com sucesso!" -ForegroundColor Green
Write-Host "Arquivo PFX: $certOutputPath" -ForegroundColor Green
Write-Host "`nThumbprint: $($cert.Thumbprint)" -ForegroundColor Cyan
Write-Host "Subject: $($cert.Subject)" -ForegroundColor Cyan

Write-Host "`n=== PRÓXIMOS PASSOS ===" -ForegroundColor Yellow
Write-Host "1. Atualize o AppxManifest.xml.template com o Publisher correto"
Write-Host "   Publisher='$certName'"
Write-Host "`n2. Para build local, use:"
Write-Host "   .\gradlew :composeApp:createMsix -Pmsix.pfx.path='installers\windows\BereiaVerse_SelfSigned.pfx' -Pmsix.pfx.password='SUA_SENHA'"
Write-Host "`n3. Para CI/CD, converta o PFX para Base64:"
Write-Host "   Comando exemplo para copiar Base64 do certificado para clipboard"
