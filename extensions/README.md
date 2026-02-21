# Bereia Versículos | IRSE - Módulo de Extensões

Este diretório contém o monorepo para as extensões do projeto **Bereia Versículos**, incluindo a extensão para Chrome e o plugin para WordPress.

## Estrutura de Pastas

*   `core/`: Lógica compartilhada (Parser, UI do Popup, Scanner de Texto).
    *   `adapters/`: Código de integração específico para cada plataforma.
*   `ext-bereia-verse/`: Código-fonte da extensão para Google Chrome.
*   `wp-bereia-verse/`: Código-fonte do plugin para WordPress.
*   `data/`: Mapeamentos bíblicos, banco de dados SQLite e scripts SQL.
*   `scripts/`: Scripts de build, automação de banco de dados e testes.
*   `dist/`: (Gerado após build) Pacotes .zip prontos para distribuição.

## Fluxo de Desenvolvimento e Build

O projeto utiliza um pipeline que injeta o código do `core/` nos destinos finais para evitar duplicação de lógica.

### 1. Requisitos
*   Node.js (para o build core)
*   Python 3 (para geração dos pacotes .zip)

### 2. Comandos Principais

No diretório `extensions/`, você pode executar:

```bash
# Executa o build completo (Core -> Extensões -> Zips -> Web)
npm run build

# Build apenas da lógica compartilhada (Core)
npm run build:core

# Gerar pacotes .zip da extensão Chrome
npm run build:extension

# Gerar pacotes .zip do plugin WordPress
npm run build:wp
```

### 3. Como funciona o Build
1.  **build:core**: Concatena os arquivos de `core/` com o adaptador correspondente e os salva em `ext-bereia-verse/content.js` e `wp-bereia-verse/assets/js/script.js`. Também sincroniza estilos CSS e o arquivo de mapeamento bíblico.
2.  **build:extension / build:wp**:
    *   Lê a versão atual no `manifest.json` ou header do PHP.
    *   Cria um arquivo `.zip` de produção na pasta `dist/`.
    *   Cria um arquivo `.zip` de teste com timestamp na versão.
    *   **Deploy Automático**: Copia o `.zip` de produção para `web/downloads/` e atualiza automaticamente os metadados (versão e URL) nos arquivos de conteúdo do site Nuxt (`web/content/apps/`).

## Gestão de Dados (Bíblia ACF)

Existem scripts em `scripts/` para gerenciar o banco de dados de versículos:

*   `db:build`: Converte o arquivo bruto `.ont` para um banco SQLite otimizado.
*   `db:dump`: Gera um dump SQL do banco SQLite.
*   `db:upload`: Divide o SQL em chunks e faz o upload para o Cloudflare D1 (usado pela API).

## Ativos
*   `logo.png`: Logo mestre do IRSE usado em todas as extensões.
*   `arrow-up-right.svg`: Ícone de seta usado para marcação de links.

---
Desenvolvido por **Instituto Reformado Santo Evangelho (IRSE)**.
