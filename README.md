# Verse Reader (ACF) - IRSE

Um leitor de versículos bíblicos (Almeida Corrigida Fiel) moderno e flutuante, construído com Kotlin Multiplatform (Compose Desktop + Android). Detecta automaticamente referências bíblicas na área de transferência (ex: `Jo 3:16`) e exibe o texto instantaneamente.

## Funcionalidades
*   **Detecção Automática:** Monitora o clipboard e exibe versículos automaticamente.
*   **Modo Widget:** Minimiza para um pequeno ícone flutuante.
*   **Multi-janela (Android):** Suporte a janelas flutuantes (Overlay) no Android.
*   **Offline:** Banco de dados SQLite embutido (ACF).

---

## Instalação e Configuração

### Windows
*   **Recomendado:** Utilize o instalador `VerseReader_Setup_X.X.X.exe`.
*   Este instalador configura tudo automaticamente, cria atalhos e **já inclui o Java 21**.

### Linux / Mac
*   **Requisito:** Java Runtime Environment (JRE) 21 ou superior instalado.
*   **Universal:** Execute o arquivo `.jar` distribuído:
    ```bash
    java -jar VerseReaderIRSE-linux-x64-X.X.X.jar
    ```

### Arch Linux (Manualmente via PKGBUILD)
O projeto inclui um script de empacotamento para Arch Linux que compila e instala o binário corretamente.

1.  Navegue até a pasta de instaladores:
    ```bash
    cd installers/linux/arch
    ```
2.  Gere e instale o pacote:
    ```bash
    makepkg -si
    ```

### Configuração para Hyprland (Linux Wayland)
Para que o popup funcione corretamente (flutue, siga o foco e não sofra tiling), adicione as seguintes regras ao seu `hyprland.conf`:

```ini
# Verse Reader Rules
windowrulev2 = float,class:^(VerseReaderIRSE)$
windowrulev2 = noanim,class:^(VerseReaderIRSE)$
windowrulev2 = nofocus,class:^(VerseReaderIRSE)$
windowrulev2 = pin,class:^(VerseReaderIRSE)$
```
*Nota: A regra `nofocus` é opcional, mas ajuda a não roubar o foco enquanto você digita, a menos que você clique na janela.*

### Configuração para GNOME/KDE (X11/Wayland)
Geralmente funciona "out-of-the-box". Se a janela aparecer atrás de outras ou com bordas estranhas, verifique se sua DE suporta "Always on Top" ou configure nas regras de janela do sistema.

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
Os arquivos gerados estarão em `composeApp/build/compose/binaries/main/`.

**Gerar Instalador Windows (.exe) via Linux:**
Este projeto suporta cross-compilation para Windows usando Launch4j e NSIS.
1.  Instale o NSIS: `sudo pacman -S nsis` (Arch) ou `sudo apt install nsis` (Debian/Ubuntu).
2.  Execute a task:
    ```bash
    ./gradlew :composeApp:packageWindows
    ```
    O instalador será gerado na raiz do projeto.

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

**A janela não aparece no Hyprland?**
Certifique-se de ter aplicado as regras de `windowrulev2` acima. O Hyprland tende a forçar tiling em todas as janelas por padrão.

**Erro "Unresolved reference" no Build?**
Tente limpar o cache do Gradle:
```bash
./gradlew clean
```

**Texto não detectado?**
O app suporta o formato `Livro Cap:Verso` (ex: `Gênesis 1:1`, `Gn 1.1`, `Jo 3:16`). Certifique-se de que a abreviação é válida.