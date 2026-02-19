# 🔐 Guia Rápido: Preencher .env com Secrets do GitHub Actions

## Passo 1: Converter Certificado Local para Base64

Execute no PowerShell:

```powershell
cd c:\Users\helen\Documents\github\bereia-verse
$pfxBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("installers\windows\BereiaVerse_SelfSigned.pfx"))
$pfxBase64 | Set-Clipboard
Write-Host "Base64 copiado! Cole no .env em MSIX_PFX_BASE64"
```

## Passo 2: Obter GOOGLE_CLIENT_SECRETS do GitHub

### Opção A: Via GitHub CLI (gh)
```powershell
gh secret list
gh secret view GOOGLE_CLIENT_SECRETS
```

### Opção B: Via Interface Web
1. Acesse: https://github.com/seu-usuario/bereia-verse/settings/secrets/actions
2. Clique em `GOOGLE_CLIENT_SECRETS`
3. Não é possível visualizar, então você precisa ter salvo localmente ou recriar

### Opção C: Usar suas credenciais do Google Cloud
1. Acesse: https://console.cloud.google.com/apis/credentials
2. Baixe o arquivo JSON das credenciais OAuth 2.0
3. Abra o arquivo e copie TODO o conteúdo em UMA LINHA:
```powershell
$json = Get-Content "caminho\para\client_secrets.json" -Raw
$oneLine = $json -replace '\r?\n', '' -replace '\s+', ' '
$oneLine | Set-Clipboard
Write-Host "JSON em uma linha copiado! Cole no .env em GOOGLE_CLIENT_SECRETS"
```

## Passo 3: Obter GH_PAT_BEREIA

### Se você já tem o token:
- Cole diretamente no `.env`

### Se precisa criar novo:
1. Acesse: https://github.com/settings/tokens
2. Clique em "Generate new token (classic)"
3. Selecione scopes: `repo`, `workflow`
4. Copie o token gerado e cole no `.env`

## Passo 4: Verificar .env

Seu arquivo `.env` deve estar assim:

```env
GH_PAT_BEREIA=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

GOOGLE_CLIENT_SECRETS={"installed":{"client_id":"...","project_id":"...","auth_uri":"...","token_uri":"...","auth_provider_x509_cert_url":"...","client_secret":"...","redirect_uris":["..."]}}

MSIX_PFX_BASE64=MIIJ...base64...==

MSIX_PFX_PASSWORD=irse2026

MSIX_PFX_PATH=installers/windows/BereiaVerse_SelfSigned.pfx
GOOGLE_CLIENT_SECRETS_PATH=client_secrets.json
```

## Passo 5: Testar Configuração

```powershell
# Executar app
.\run.ps1

# Compilar MSIX
.\build-msix.ps1
```

## 📝 Notas Importantes

- **NUNCA** commite o arquivo `.env` (já está no .gitignore)
- O certificado Base64 deve estar em UMA LINHA
- O JSON do Google também deve estar em UMA LINHA
- Se não tiver acesso aos secrets, você pode recriar:
  - Certificado: `.\installers\windows\create-cert.ps1`
  - Google: Baixe novo JSON do Google Cloud Console
  - GitHub Token: Crie novo em github.com/settings/tokens

## 🆘 Ajuda

Se não conseguir acessar os secrets do GitHub Actions:

1. **Certificado**: Use o que acabamos de criar localmente
2. **Google Secrets**: Baixe novo do Google Cloud Console  
3. **GitHub Token**: Crie um novo (o antigo continuará funcionando no CI/CD)
