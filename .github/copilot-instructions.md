# Instruções do GitHub Copilot - Bereia Verse

## Contexto do Projeto
Aplicação desktop multiplataforma (Kotlin Multiplatform + Compose Desktop) para leitura bíblica automática com sincronização Google Drive.

## Diretrizes de Comunicação com o Agente

### O QUE FAZER
- ✅ Responder diretamente no chat
- ✅ Ser objetivo e conciso
- ✅ Focar estritamente no que foi pedido
- ✅ Implementar mudanças diretamente no código quando solicitado
- ✅ Explicar decisões técnicas no chat quando relevante

### O QUE NÃO FAZER
- ❌ Criar documentos `.md` não solicitados
- ❌ Criar scripts auxiliares sem pedido explícito
- ❌ Adicionar comentários explicativos excessivos no código
- ❌ "Sujar" a raiz do projeto com arquivos desnecessários
- ❌ Criar documentação proativamente

**Regra de Ouro:** Se não foi explicitamente pedido, não crie. Informe no chat, não em arquivos.

## Padrões de Código

### Kotlin
- Usar Kotlin idiomático e conciso
- Preferir `val` sobre `var`
- Usar scope functions (`let`, `apply`, `run`) quando apropriado
- Documentar funções públicas com KDoc
- Usar coroutines para operações assíncronas (`suspend fun`, `withContext`)

### Compose Desktop
- Componentes devem ser `@Composable` functions
- State hoisting: elevar estado para o nível mais alto necessário
- Usar `remember` e `mutableStateOf` para estado local
- ViewModel para lógica de negócio
- Evitar side effects fora de `LaunchedEffect` ou `DisposableEffect`

### Estrutura
```
composeApp/src/
  commonMain/     - Código compartilhado
  jvmMain/        - Código específico Desktop/JVM
```

### Nomeação
- Classes: `PascalCase` (ex: `BibleReaderViewModel`)
- Functions: `camelCase` (ex: `loadChapter`)
- Composables: `PascalCase` (ex: `ChapterCard`)
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.with.dots`

## Práticas Específicas

### Configuração
- Credenciais via variáveis de ambiente (`.env`)
- Nunca hardcode secrets no código
- Usar `System.getenv("VAR_NAME")` para acessar

### Recursos
- Assets em `composeApp/src/commonMain/composeResources/`
- Ícones: usar Material Icons Extended
- Localização: português brasileiro prioritário

### Database
- SQLite para armazenamento local
- Transações para operações batch
- Prepared statements sempre

### Google Drive Sync
- Autenticação OAuth 2.0
- Tokens em `~/.bereia-verse/tokens/`
- Credenciais de `GOOGLE_CLIENT_SECRETS` env var ou `client_secrets.json`

### Build/Packaging
- Gradle Kotlin DSL
- MSIX para Windows (assinado)
- DEB para Linux
- DMG para macOS

## Evitar
- ❌ Lógica de UI em ViewModels
- ❌ Operações bloqueantes na UI thread
- ❌ Secrets hardcoded
- ❌ Nomes genéricos (ex: `data`, `item`, `temp`)
- ❌ Comentários óbvios
- ❌ God objects/classes gigantes

## Preferir
- ✅ Funções pequenas e focadas
- ✅ Imutabilidade quando possível
- ✅ Extension functions para utilitários
- ✅ Sealed classes para estados
- ✅ Data classes para modelos
- ✅ Dependency injection (Koin)

## Comentários
- Escrever em português brasileiro
- Explicar "porquê", não "o quê"
- Documentar workarounds e hacks com `// HACK:` ou `// TODO:`
- Usar KDoc para APIs públicas

## Exemplo de Código Ideal

```kotlin
/**
 * Carrega um capítulo da Bíblia do banco de dados local.
 * 
 * @param book Nome do livro (ex: "Gênesis")
 * @param chapter Número do capítulo
 * @return Lista de versículos ou null se não encontrado
 */
suspend fun loadChapter(book: String, chapter: Int): List<Verse>? = 
    withContext(Dispatchers.IO) {
        database.query(
            "SELECT * FROM verses WHERE book = ? AND chapter = ?",
            arrayOf(book, chapter)
        ).map { row ->
            Verse(
                book = row.getString("book"),
                chapter = row.getInt("chapter"),
                verse = row.getInt("verse"),
                text = row.getString("text")
            )
        }.ifEmpty { null }
    }

@Composable
fun ChapterCard(
    chapter: ChapterData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${chapter.verseCount} versículos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

## Segurança
- Validar inputs do usuário
- Sanitizar queries SQL
- Não logar informações sensíveis
- Verificar permissões antes de operações de arquivo

## Performance
- Lazy loading para listas grandes
- Virtualização com `LazyColumn`
- Cancelamento de coroutines quando componente é destruído
- Cache quando apropriado
