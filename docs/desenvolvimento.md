# Guia de Desenvolvimento - Bereia Versículos

Este guia oferece instruções detalhadas para configurar seu ambiente de desenvolvimento e trabalhar nos diversos subprojetos do ecossistema Bereia Versículos.

---

## 🛠️ Pré-requisitos

Certifique-se de ter as seguintes ferramentas instaladas em seu sistema:

*   **Git:** Ferramenta de controle de versão.
    *   [Download e Instalação do Git](https://git-scm.com/downloads)
*   **Node.js (LTS recomendado) e npm (ou Yarn):** Para os projetos `web`, `workers`, `ext-bereia-verse` e `wp-bereia-verse` (para algumas tarefas de build, se aplicável).
    *   [Download e Instalação do Node.js](https://nodejs.org/en/download/)
*   **JDK 21 (Java Development Kit):** Para o projeto `app` (Kotlin Multiplatform).
    *   [Download e Instalação do JDK](https://adoptium.net/) (Recomendado Temurin OpenJDK 21)
*   **IDE (Opcional, mas recomendado):**
    *   **IntelliJ IDEA Ultimate Edition:** Para o desenvolvimento em Kotlin Multiplatform (`app`).
    *   **VS Code:** Para os projetos `web`, `workers` e `extensions`.

---

## 🚀 Configuração do Ambiente

1.  **Clone o Repositório:**
    ```bash
    git clone https://github.com/Instituto-Reformado-Santo-Evangelho/bereia-verse.git
    cd bereia-verse
    ```

2.  **Configuração Inicial (Projetos Node.js):**
    Para os projetos `web`, `workers`:
    ```bash
    npm install --prefix web
    npm install --prefix workers
    ```
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

### Servidor Backend (`/workers`)

*   **Tecnologia:** Cloudflare Workers (TypeScript)
*   **Para Rodar (com Wrangler CLI):**
    ```bash
    cd workers
    npm run dev
    ```
    O servidor estará acessível localmente para testes.

### Extensão Chrome (`/ext-bereia-verse`)

*   **Tecnologia:** JavaScript, HTML, CSS
*   **Para Testar:** Consulte o guia de instalação em [docs/extensao-chrome.md](./extensao-chrome.md) e siga os passos para "Carregar sem compactação" no modo de desenvolvedor do Chrome, apontando para o diretório `ext-bereia-verse`.

### Plugin WordPress (`/wp-bereia-verse`)

*   **Tecnologia:** PHP, JavaScript, CSS
*   **Para Testar:** Instale em um ambiente de desenvolvimento WordPress (ex: LocalWP, Docker, MAMP/XAMPP). Consulte o guia de instalação em [docs/plugin-wordpress.md](./plugin-wordpress.md).

---

### Builds Manuais (Via Gradle/npm)

*   **Aplicativo Desktop (`/app`):**
    ```bash
    cd app
    ./gradlew packageDeb       # Gera pacote .deb para Linux
    ./gradlew packageDmg       # Gera pacote .dmg para macOS
    ./gradlew createMsix       # Gera pacote .msix para Windows
    ```
*   **Extensão Chrome e Plugin Wordpress (`extensions/`):**
    ```bash
    cd extensions/ && npm run build
    ```
    
*   **Site Principal (`/web`):**
    ```bash
    cd web
    npm run generate           # Gera a versão estática do site
    ```
*   **Servidor Backend (`/workers`):**
    ```bash
    cd workers
    npm run build              # Gera o worker para deploy no Cloudflare
    ```

---

**Lembre-se de sempre manter seu branch atualizado e seu diretório de trabalho limpo antes de realizar operações de release.**
