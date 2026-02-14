# IRSE | Bereia Verse

Um leitor de versículos bíblicos (Almeida Corrigida Fiel) moderno e flutuante, construído com Kotlin Multiplatform (Compose Desktop + Android). Detecta automaticamente referências bíblicas na área de transferência (ex: `Jo 3:16`) e exibe o texto instantaneamente.

## Funcionalidades
*   **Detecção Automática:** Monitora o clipboard e exibe versículos automaticamente.
*   **Modo Widget:** Minimiza para um pequeno ícone flutuante no Linux com Hyprland.
*   **Multi-janela (Android):** Suporte a janelas flutuantes (Overlay) no Android e Windows.
*   **Offline:** Banco de dados SQLite embutido (ACF).

---

## Distribuição e Instaladores

Todos os binários gerados são centralizados na pasta `/dist`.

### 🪟 Windows
*   **Instalador:** `VerseReader_Setup_X.X.X.exe` (gerado na raiz).
*   **Comando:** `./gradlew :composeApp:packageWindows` (Requer NSIS).
*   O instalador configura tudo automaticamente, cria atalhos e **já inclui o Java 21**.

### 🐧 Linux
*   **Debian/Ubuntu (.deb):** Localizado em `dist/linux/`.
    *   **Comando:** `./gradlew :composeApp:packageDeb`.
*   **Arch Linux:** Localizado em `dist/linux/`.
    *   **Como gerar:** `cd dist/linux && makepkg -si`.
*   **Universal:** Execute o arquivo `.jar` distribuído com `java -jar`.

### 🤖 Android
*   **APKs:** Localizados em `dist/android/`.
    *   **Comando:** `./gradlew :androidApp:assembleRelease` (Cópia automática para dist).

### 🍎 macOS
*   **Disco (.dmg):** Localizado em `dist/mac/`.
    *   **Comando:** `./gradlew :composeApp:packageDmg`.

---

## Configuração Específica (Linux)

### Hyprland (Wayland)
Para que o popup funcione corretamente (flutue, siga o foco e não sofra tiling), adicione as seguintes regras ao seu `hyprland.conf`:

```ini
# Verse Reader Rules
windowrulev2 = float,class:^(VerseReaderIRSE)$
windowrulev2 = noanim,class:^(VerseReaderIRSE)$
windowrulev2 = nofocus,class:^(VerseReaderIRSE)$
windowrulev2 = pin,class:^(VerseReaderIRSE)$
```

---

## Desenvolvimento (Build)

### Comandos Básicos (Gradle)

**Rodar Desktop (Desenvolvimento):**
```bash
./gradlew :composeApp:run
```

**Gerar Executável/Pacote (.deb, .msi, .dmg):**
```bash
./gradlew :composeApp:createDistributable
```

**Rodar Android:**
```bash
./gradlew :composeApp:installDebug
```

### Estrutura do Projeto
*   `composeApp`: Código principal UI (Compose Multiplatform).
    *   `commonMain`: Lógica compartilhada, Banco de Dados, Parser.
    *   `jvmMain`: Lógica específica de Desktop (Window, Clipboard AWT/Native).
    *   `androidMain`: Lógica específica Android (Service, Overlay, Clipboard Manager).
*   `iosApp`: Cliente iOS (SwiftUI bridge).

---

## Solução de Problemas
*   **Erro de Build:** `./gradlew clean`
*   **Versão do Java:** Certifique-se de estar usando o **JDK 21** ou superior.
*   **Texto não detectado?** O app suporta o formato `Livro Cap:Verso` (ex: `Gênesis 1:1`, `Gn 1.1`, `Jo 3:16`).
