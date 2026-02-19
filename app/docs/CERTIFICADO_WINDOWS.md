# Guia de Certificados para Windows (MSIX)

## Problema

Aplicativos MSIX no Windows requerem assinatura digital. Se o certificado não for confiável, o Windows bloqueará a instalação com erro de "certificado inválido".

## Soluções

### 🔧 Opção 1: Certificado Auto-Assinado (Desenvolvimento/Testes Locais)

**Vantagens:**
- Gratuito
- Funciona imediatamente após instalação
- Ideal para desenvolvimento e distribuição interna

**Desvantagens:**
- Cada usuário precisa instalar o certificado manualmente
- Não funciona para Microsoft Store
- Windows SmartScreen mostrará avisos

#### Passos:

**1. Criar o Certificado (Execute como Administrador):**
```powershell
cd installers\windows
.\create-selfsigned-cert.ps1
```

Siga as instruções e anote a senha que você definir.

**2. Atualizar o Manifesto MSIX:**

Após criar o certificado, o script mostrará o Subject. Atualize o arquivo `composeApp/packaging/msix/AppxManifest.xml.template`:

```xml
Publisher="CN=Organização IRSE"
```

E também atualize `composeApp/build.gradle.kts`:

```kotlin
msix {
    manifest {
        publisher.set("CN=Organização IRSE")
        // ... outras configurações
    }
}
```

**3. Build do MSIX com Assinatura:**

```powershell
.\gradlew :composeApp:createMsix `
  -Pmsix.pfx.path="installers\windows\BereiaVerse_SelfSigned.pfx" `
  -Pmsix.pfx.password="SUA_SENHA_AQUI"
```

**4. Para distribuir para outros usuários:**

Cada usuário precisará instalar o certificado primeiro:

```powershell
# Execute como Administrador no computador do usuário
cd installers\windows
.\install-cert-from-msix.ps1 -MsixPath "caminho\para\BereiaVerse.msix"
```

Ou manualmente:
1. Clique com botão direito no arquivo `.msix`
2. Propriedades → Assinaturas Digitais
3. Selecione a assinatura → Detalhes → Exibir Certificado
4. Instalar Certificado → Máquina Local → Colocar todos os certificados no repositório a seguir
5. Procurar → Autoridades de Certificação Confiáveis

---

### 🏢 Opção 2: Certificado Comercial (Produção/Microsoft Store)

**Vantagens:**
- Usuários não precisam instalar certificado
- Sem avisos do Windows SmartScreen
- Requerido para Microsoft Store
- Aparência profissional

**Desvantagens:**
- Custo anual (€60-€400/ano dependendo do fornecedor)
- Processo de validação da empresa

#### Fornecedores Recomendados:

1. **DigiCert** (mais confiável)
   - Code Signing Certificate: ~$474/ano
   - EV Code Signing: ~$474/ano
   - https://www.digicert.com/

2. **Sectigo** (melhor custo-benefício)
   - Code Signing: ~€60/ano
   - https://sectigo.com/

3. **GlobalSign**
   - Code Signing: ~€300/ano
   - https://www.globalsign.com/

#### Passos:

1. **Comprar o Certificado:**
   - Escolha "Code Signing Certificate"
   - Complete a validação da empresa
   - Baixe o certificado em formato PFX

2. **Atualizar Manifesto:**

Em `composeApp/packaging/msix/AppxManifest.xml.template`:

```xml
Publisher="CN=Nome da Sua Empresa, O=Organização, L=Cidade, S=Estado, C=BR"
```

**Importante:** Use exatamente o Subject do certificado adquirido.

3. **Build com o Certificado Comercial:**

```powershell
.\gradlew :composeApp:createMsix `
  -Pmsix.pfx.path="caminho\para\seu_certificado.pfx" `
  -Pmsix.pfx.password="senha_do_certificado"
```

4. **Para CI/CD (GitHub Actions):**

Já está configurado! Apenas adicione os secrets:

```bash
# No GitHub: Settings → Secrets → Actions

# 1. Converter PFX para Base64
[Convert]::ToBase64String([IO.File]::ReadAllBytes("seu_certificado.pfx")) | Set-Clipboard

# 2. Adicionar secrets:
MSIX_PFX_BASE64 = <cole o conteúdo>
MSIX_PFX_PASSWORD = <sua senha>
```

---

### 📦 Opção 3: Usar Instalador NSIS (Sem MSIX)

Se você não quer lidar com certificados agora, pode usar o instalador NSIS tradicional que já está configurado.

**Vantagens:**
- Não requer certificado
- Instalação tradicional do Windows
- Funciona imediatamente

**Desvantagens:**
- Avisos do Windows SmartScreen
- Não pode ser publicado na Microsoft Store
- Menos moderno que MSIX

O arquivo `installers/windows/installer.nsi` já está configurado. Você precisaria apenas criar um script de build.

---

## Recomendação

**Para desenvolvimento e testes:** Use Opção 1 (certificado auto-assinado)

**Para distribuição pública/profissional:** Use Opção 2 (certificado comercial)

**Para distribuição rápida sem complicações:** Use Opção 3 (NSIS)

---

## Troubleshooting

### "O pacote não pode ser instalado porque não tem uma assinatura válida"

Execute o script de instalação do certificado:
```powershell
.\installers\windows\install-cert-from-msix.ps1 -MsixPath "seu_arquivo.msix"
```

### "Esta aplicação foi bloqueada para sua proteção"

- Isso é o Windows SmartScreen
- Com certificado comercial isso não acontece
- Com auto-assinado, clique em "Mais informações" → "Executar assim mesmo"

### Build falha com erro de assinatura

Verifique:
1. O arquivo PFX existe no caminho especificado
2. A senha está correta
3. O Publisher no manifesto corresponde ao certificado

### Certificado expirado

Execute novamente o script `create-selfsigned-cert.ps1` para criar um novo certificado com validade de 3 anos.
