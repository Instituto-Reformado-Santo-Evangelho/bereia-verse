# Script para criar certificado auto-assinado para MSIX (sem privilegios de admin)

$certName = "CN=B41FD2FB-AD80-4515-8823-5F91386585CC"
$pfxPassword = Read-Host "Digite a senha para o certificado PFX" -AsSecureString
$certOutputPath = Join-Path $PSScriptRoot "BereiaVerse_SelfSigned.pfx"

Write-Host "Criando certificado auto-assinado..." -ForegroundColor Cyan

$cert = New-SelfSignedCertificate `
    -Type Custom `
    -Subject $certName `
    -KeyUsage DigitalSignature `
    -FriendlyName "Bereia Verse Self-Signed Certificate" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.3", "2.5.29.19={text}") `
    -NotAfter (Get-Date).AddYears(3)

Write-Host "Exportando certificado para $certOutputPath..." -ForegroundColor Cyan
Export-PfxCertificate -Cert $cert -FilePath $certOutputPath -Password $pfxPassword | Out-Null

Write-Host "Instalando certificado na raiz confiavel do USUARIO..." -ForegroundColor Yellow
$store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "CurrentUser")
$store.Open("ReadWrite")
$store.Add($cert)
$store.Close()

Write-Host "`nCertificado criado com sucesso!" -ForegroundColor Green
Write-Host "Arquivo PFX: $certOutputPath" -ForegroundColor Green
Write-Host "Thumbprint: $($cert.Thumbprint)" -ForegroundColor Cyan
Write-Host "Subject: $($cert.Subject)" -ForegroundColor Cyan
Write-Host "`nPara compilar o MSIX com este certificado, use:" -ForegroundColor Yellow
Write-Host "  cd c:\Users\helen\Documents\github\bereia-verse" -ForegroundColor White
Write-Host "  .\gradlew :composeApp:createMsix -Pmsix.pfx.path=`"installers\windows\BereiaVerse_SelfSigned.pfx`" -Pmsix.pfx.password=`"SUA_SENHA`"" -ForegroundColor White
