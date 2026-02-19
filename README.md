<div align="center">
  
# Bereia Versículos

![Logo](app/composeApp/src/desktopMain/resources/bereiaverse.png)

</div>

**Um ecossistema de ferramentas para leitura e anotação bíblica, projetado para ser integrado e multiplataforma.**

O Bereia Versículos é um projeto de código aberto do [Instituto Reformado Santo Evangelho (IRSE)](https://irse.com.br) que visa facilitar o acesso e o estudo das Escrituras Sagradas. Ele transforma automaticamente referências de versículos em texto legível, onde quer que você esteja: no seu desktop, navegando na web ou em seu site WordPress.

---

## ✨ Projetos no Repositório

Este monorepo contém vários pacotes que trabalham juntos para criar o ecossistema Bereia Versículos.

| Projeto | Diretório | Tecnologia | Propósito |
| :--- | :--- | :--- | :--- |
| 🖥️ **App Desktop** | `/app` | Kotlin Multiplatform | Aplicativo nativo para Windows, macOS e Linux com recursos de anotações, histórico e sincronização na nuvem. |
| 🌐 **Site Principal** | `/web` | Nuxt.js / Vue.js | A página inicial do projeto, com documentação e links para download dos aplicativos. |
| 🧩 **Extensão Chrome** | `/ext-chrome` | JavaScript | Detecta e exibe versículos bíblicos em qualquer página da web que você visitar. |
| 🔌 **Plugin WordPress** | `/wp-plugin` | PHP / JavaScript | Leva a funcionalidade de detecção de versículos para o seu site ou blog WordPress. |
| ⚙️ **Servidor Backend**| `/server` | Cloudflare Workers | Fornece a API utilizada pelas outras aplicações para buscar os textos bíblicos. |

---

## 🚀 Como Começar (Desenvolvimento)

Para contribuir com o desenvolvimento, você precisará de algumas ferramentas instaladas:

- **Git:** Para controle de versão.
- **Node.js & npm:** Para os projetos web, server e extensões.
- **JDK 21 (ou superior):** Para o aplicativo desktop em Kotlin.
- **GitHub CLI (`gh`):** Necessário para usar o script de release.

**Passos básicos:**

1.  Clone o repositório:
    ```bash
    git clone https://github.com/Instituto-Reformado-Santo-Evangelho/bereia-verse.git
    cd bereia-verse
    ```

2.  Para instruções detalhadas de como compilar e rodar cada projeto, consulte o nosso **[Guia de Desenvolvimento](./docs/desenvolvimento.md)**.

---

## 릴 Releases e Instalação

Os pacotes de instalação para todos os aplicativos podem ser encontrados na [**página de Releases**](https://github.com/Instituto-Reformado-Santo-Evangelho/bereia-verse/releases) do projeto.

Para guias de instalação detalhados, acesse a pasta `docs`:
- **[Instalação do App Desktop](./docs/app-desktop.md)**
- **[Instalação da Extensão para Chrome](./docs/extensao-chrome.md)**
- **[Instalação do Plugin para WordPress](./docs/plugin-wordpress.md)**

---

## 📄 Licença

O código-fonte deste projeto é licenciado sob a **Creative Commons Atribuição-NãoComercial-SemDerivações 4.0 Internacional (CC BY-NC-ND 4.0)**.

Isso significa que você é livre para copiar e redistribuir o material em qualquer meio ou formato, desde que:
- Dê o crédito apropriado ao **Instituto Reformado Santo Evangelho (IRSE)**.
- Não utilize o material para fins comerciais.
- Não distribua material modificado.

Veja o arquivo [`LICENSE`](./LICENSE) para o texto completo da licença.

**Nota sobre Conteúdo:** Esta licença se aplica ao código-fonte do projeto. O texto da Bíblia (Almeida Corrigida Fiel - ACF) possui seus próprios direitos autorais, que devem ser respeitados conforme os termos de seus detentores.
