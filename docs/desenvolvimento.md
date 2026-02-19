# Guia de Desenvolvimento - Bereia Versículos

Este guia oferece instruções detalhadas para configurar seu ambiente de desenvolvimento e trabalhar nos diversos subprojetos do ecossistema Bereia Versículos.

---

## 🛠️ Pré-requisitos

Certifique-se de ter as seguintes ferramentas instaladas em seu sistema:

*   **Git:** Ferramenta de controle de versão.
    *   [Download e Instalação do Git](https://git-scm.com/downloads)
*   **Node.js (LTS recomendado) e npm (ou Yarn):** Para os projetos `web`, `server`, `ext-chrome` e `wp-plugin` (para algumas tarefas de build, se aplicável).
    *   [Download e Instalação do Node.js](https://nodejs.org/en/download/)
*   **JDK 21 (Java Development Kit):** Para o projeto `app` (Kotlin Multiplatform).
    *   [Download e Instalação do JDK](https://adoptium.net/) (Recomendado Temurin OpenJDK 21)
*   **GitHub CLI (`gh`):** Para interagir com o GitHub e usar o script `release.sh`.
    *   [Download e Instalação do GitHub CLI](https://cli.github.com/)
    *   Após a instalação, autentique-se: `gh auth login`
*   **IDE (Opcional, mas recomendado):**
    *   **IntelliJ IDEA Ultimate Edition:** Para o desenvolvimento em Kotlin Multiplatform (`app`).
    *   **VS Code:** Para os projetos `web`, `server`, `ext-chrome` e `wp-plugin`.

---

## 🚀 Configuração do Ambiente

1.  **Clone o Repositório:**
    ```bash
    git clone https://github.com/Instituto-Reformado-Santo-Evangelho/bereia-verse.git
    cd bereia-verse
    ```

2.  **Configuração Inicial (Projetos Node.js):**
    Para os projetos `web`, `server` e `ext-chrome`:
    ```bash
    npm install --prefix web
    npm install --prefix server
    npm install --prefix ext-chrome
    ```
    (Note: `wp-plugin` geralmente não tem dependências Node/npm, mas pode ter scripts de build no futuro).

---

## ⚙️ Rodando os Subprojetos Localmente

### Aplicativo Desktop (`/app`)

*   **Tecnologia:** Kotlin Multiplatform (Compose for Desktop)
*   **Para Rodar:**
    ```bash
    cd app
    ./gradlew run
    ```
    (No Windows, use `gradlew.bat run`)

### Site Principal (`/web`)

*   **Tecnologia:** Nuxt.js / Vue.js
*   **Para Rodar:**
    ```bash
    cd web
    npm run dev
    ```
    O site estará acessível em `http://localhost:3000` (ou outra porta indicada).

### Servidor Backend (`/server`)

*   **Tecnologia:** Cloudflare Workers (TypeScript)
*   **Para Rodar (com Wrangler CLI):**
    ```bash
    cd server
    npm run dev
    ```
    O servidor estará acessível localmente para testes.

### Extensão Chrome (`/ext-chrome`)

*   **Tecnologia:** JavaScript, HTML, CSS
*   **Para Testar:** Consulte o guia de instalação em [docs/extensao-chrome.md](./extensao-chrome.md) e siga os passos para "Carregar sem compactação" no modo de desenvolvedor do Chrome, apontando para o diretório `ext-chrome`.

### Plugin WordPress (`/wp-plugin`)

*   **Tecnologia:** PHP, JavaScript, CSS
*   **Para Testar:** Instale em um ambiente de desenvolvimento WordPress (ex: LocalWP, Docker, MAMP/XAMPP). Consulte o guia de instalação em [docs/plugin-wordpress.md](./plugin-wordpress.md).

---

## 📦 Builds e Releases

### Script de Release Automatizado

Utilize o script `release.sh` na raiz do projeto para automatizar a criação de tags, o push e o disparo dos workflows de build no GitHub Actions.

*   **Tornar Executável (se ainda não for):**
    ```bash
    chmod +x release.sh
    ```
*   **Uso Básico:**
    ```bash
    ./release.sh <alvo> <versão>
    ```
    Exemplos:
    *   `./release.sh all v1.0.0` (Para construir e lançar todos os pacotes)
    *   `./release.sh deb v1.0.0` (Para construir e lançar apenas o pacote Debian)
    *   `./release.sh ext v1.0.0` (Para construir e lançar apenas a extensão e o plugin)

*   **Detalhes e Opções:**
    ```bash
    ./release.sh --help
    ```

### Builds Manuais (Via Gradle/npm)

*   **Aplicativo Desktop (`/app`):**
    ```bash
    cd app
    ./gradlew packageDeb       # Gera pacote .deb para Linux
    ./gradlew packageDmg       # Gera pacote .dmg para macOS
    ./gradlew createMsix       # Gera pacote .msix para Windows
    ```
*   **Extensão Chrome (`/ext-chrome`):**
    ```bash
    cd ext-chrome
    zip -r ../ext-chrome.zip . # Cria o zip para distribuição
    ```
*   **Plugin WordPress (`/wp-plugin`):**
    ```bash
    cd wp-plugin
    zip -r ../wp-plugin.zip . # Cria o zip para distribuição
    ```
*   **Site Principal (`/web`):**
    ```bash
    cd web
    npm run generate           # Gera a versão estática do site
    ```
*   **Servidor Backend (`/server`):**
    ```bash
    cd server
    npm run build              # Gera o worker para deploy no Cloudflare
    ```

---

**Lembre-se de sempre manter seu branch atualizado e seu diretório de trabalho limpo antes de realizar operações de release.**
