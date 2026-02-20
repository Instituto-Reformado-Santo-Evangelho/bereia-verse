# Documento: Plano de Publicação e Higienização do Repositório

Este documento descreve as decisões tomadas e o plano de ação para tornar o repositório `bereia-verse` público de forma segura, com o objetivo final de publicar o aplicativo no Flathub.

## 1. Desafio de Segurança Identificado

Foi identificado que o arquivo `client_secrets.json`, contendo chaves sensíveis da API do Google (`client_id` e `client_secret`), foi commitado no histórico do Git. Tornar o repositório público neste estado exporia essas chaves, representando um risco de segurança significativo.

## 2. Análise de Risco: Rotação de Chaves

A solução imediata para uma chave exposta é "rotacioná-la" (gerar uma nova e invalidar a antiga) no Google Cloud Console. No entanto, foi determinado que esta ação quebraria a funcionalidade de login para todos os novos usuários do aplicativo que já está em produção (publicado na Microsoft Store), uma vez que a chave antiga está compilada dentro da versão atual do aplicativo.

## 3. Plano de Ação Decidido

Com base na análise de risco, o seguinte plano foi estabelecido para garantir uma transição segura, sem impactar os usuários atuais:

**Etapa A: Preparação (Repositório Privado)**

1.  **Manter o Repositório Privado:** O repositório permanecerá privado até que uma nova versão segura do aplicativo seja lançada, protegendo o segredo que está no histórico.
2.  **Higienização do Código-Fonte:** O código será preparado para se tornar público.
    *   Os arquivos `client_secrets.json` serão removidos do código-fonte.
    *   O nome `client_secrets.json` e `.env` serão garantidos no arquivo `.gitignore`.
    *   O conteúdo do `client_secrets.json` será movido para uma variável secreta no GitHub Actions, chamada `GOOGLE_CLIENT_SECRETS_JSON`.
    *   Os workflows de build (GitHub Actions) serão modificados para receber este segredo e criar o arquivo `client_secrets.json` temporariamente durante a compilação, apagando-o ao final do processo.

**Etapa B: Lançamento e Transição**

3.  **Lançamento da Nova Versão:** Uma nova versão do aplicativo (ex: v1.4.0) será compilada usando o novo sistema de build seguro e publicada nas lojas (Microsoft Store, etc.).
4.  **ROTACIONAR A CHAVE (Pós-Lançamento):** **Somente após** a nova versão estar no ar e os usuários começarem a migrar para ela, a chave `client_secret` será rotacionada no Google Cloud Console.
5.  **Atualizar Segredo no GitHub:** O novo `client_secret` gerado será imediatamente atualizado na variável `GOOGLE_CLIENT_SECRETS_JSON` no GitHub.
6.  **TORNAR O REPOSITÓRIO PÚBLICO:** Com o segredo antigo invalidado e o novo protegido, o repositório poderá ser tornado público com segurança.

**Etapa C: Publicação no Flathub**

7.  **Submissão ao Flathub:** Com o repositório público e o manifesto Flatpak validado, o aplicativo será submetido ao Flathub para publicação.
