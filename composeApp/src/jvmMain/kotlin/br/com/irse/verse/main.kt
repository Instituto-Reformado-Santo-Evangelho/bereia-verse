package br.com.irse.verse

import br.com.irse.verse.ui.theme.VerseColors
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import br.com.irse.verse.core.*
import br.com.irse.verse.di.appModule
import br.com.irse.verse.platform.JvmSnapshotHandler
import br.com.irse.verse.platform.JvmGoogleDriveProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.get
import org.koin.dsl.module
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

fun getActiveMonitorBounds(): Rectangle? {
    return try {
        val pointerInfo = MouseInfo.getPointerInfo()
        val mousePoint = pointerInfo.location
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        ge.screenDevices.firstOrNull { it.defaultConfiguration.bounds.contains(mousePoint) }?.defaultConfiguration?.bounds
        ?: ge.defaultScreenDevice.defaultConfiguration.bounds
    } catch (e: Exception) { e.printStackTrace(); null }
}

fun main(args: Array<String>) {
    // Configuração de Log de Erro (Essencial para diagnósticos em Linux/Mac/Win)
    try {
        // 1. Detecção de Crash Anterior (Safe Mode) & Verificação de Suporte
        try {
            var settings = SettingsManager.getSettingsSync()
            
            // Verificação de Suporte a Transparência
            val transparencySupported = try {
                val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                ge.defaultScreenDevice.isWindowTranslucencySupported(java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT)
            } catch (e: Exception) { 
                // Fallback conservador se falhar a detecção
                false 
            }

            // Atualiza configurações se o suporte mudou ou não estava gravado
            if (settings.isTransparencySupported != transparencySupported) {
                settings = settings.copy(isTransparencySupported = transparencySupported)
                SettingsManager.saveSettingsSync(settings)
            }

            // Desativa transparência se não suportado, forçando a configuração correta
            if (!transparencySupported && settings.isTransparent) {
                settings = settings.copy(isTransparent = false) // Mantém isTransparencySupported=false do passo anterior
                SettingsManager.saveSettingsSync(settings)
            }

            val crashDetected = SettingsManager.lockFile.exists()
            
            if (crashDetected && settings.isTransparent) {
                // Se crasheou e estava com transparência, desativa para a próxima tentativa
                SettingsManager.saveSettingsSync(settings.copy(isTransparent = false))
                System.setProperty("verse.transparencyWarning", "true")
            }
            // Cria o lock para esta sessão
            SettingsManager.lockFile.createNewFile()
        } catch (e: Exception) {
            // Ignora erro ao acessar arquivo de lock/config para não impedir inicialização
            e.printStackTrace()
        }

        // Detecta argumento manual de desativação de transparência
        if (args.contains("--no-transparent")) {
            System.setProperty("verse.noTransparent", "true")
        }

        runApplication()
    } catch (e: Throwable) {
        // Se houve erro no Kotlin, remove o lock para não disparar safe mode falso (opcional)
        try { SettingsManager.lockFile.delete() } catch (_: Exception) {}
        
        val os = System.getProperty("os.name").lowercase()
        val logDir = if (os.contains("win")) {
            File(System.getenv("APPDATA"), "BereiaVerse")
        } else {
            File(System.getProperty("user.home"), ".local/share/bereia-verse")
        }
        if (!logDir.exists()) logDir.mkdirs()
        
        val crashFile = File(logDir, "crash_log.txt")
        FileOutputStream(crashFile).use { out ->
            val message = "Crash at ${java.time.LocalDateTime.now()}\nOS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}\n\n${e.stackTraceToString()}"
            out.write(message.toByteArray())
        }
        e.printStackTrace() // Ainda imprime no console se disponível
    }
}

fun runApplication() {
    val osName = System.getProperty("os.name").lowercase()
    val isWindows = osName.contains("win")
    val isLinux = osName.contains("linux")
    val isMac = osName.contains("mac")

    // Configurações Específicas por SO
    if (isLinux) {
        System.setProperty("skiko.linux.autodetect.gpu", "true")
        // Alguns ambientes Linux/Wayland falham com aceleração de hardware em janelas transparentes
        // Se houver problemas, o usuário pode tentar lançar com -Dskiko.renderApi=SOFTWARE
    }
    
    // Detecção robusta do Wine (Env Vars + Registry)
    var isWineDetected = isWindows && (System.getenv("WINEPREFIX") != null || System.getenv("WINELOADERNOEXEC") != null)
    
    if (isWindows && !isWineDetected) {
        try {
            // Tenta detectar chave do Wine no registro se as env vars falharem
            // Usa timeout para evitar hang
            val process = ProcessBuilder("reg", "query", "HKLM\\Software\\Wine").start()
            val finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            if (finished) {
                isWineDetected = process.exitValue() == 0
            } else {
                process.destroy()
            }
        } catch (e: Exception) {
            // Ignora erro se 'reg' não existir ou falhar
        }
    }

    if (isWineDetected) {
        System.setProperty("verse.isWine", "true")
        // Força renderização via software para evitar crashes (X_CopyArea)
        System.setProperty("sun.java2d.xrender", "false")
        System.setProperty("sun.java2d.d3d", "false")
        System.setProperty("skiko.renderApi", "SOFTWARE")
    }

    application {
    val defaultWidth = 400.dp
    val defaultHeight = 350.dp
    val miniSize = 64.dp
    val screenPadding = 20
    
    // Carrega configuração salva
    val savedSettings = remember { SettingsManager.getSettingsSync() }
    
    val icon = painterResource(Res.drawable.logo)
    val isLinux = remember { System.getProperty("os.name").lowercase().contains("linux") }
    val isWindows = remember { System.getProperty("os.name").lowercase().contains("win") }
    val isWine = remember { System.getProperty("verse.isWine") == "true" }
    val forceNoTransparent = remember { System.getProperty("verse.noTransparent") == "true" }
    
    // Windows: transparência agora respeita a configuração do usuário
    // MAS valida suporte antes de aplicar
    val defaultTransparency = savedSettings.isTransparent
    val shouldBeTransparent = !isWine && !forceNoTransparent && defaultTransparency
    
    // Se transparência estava ativada mas não há suporte, desativa automaticamente
    LaunchedEffect(Unit) {
        if (defaultTransparency && (isWine || forceNoTransparent)) {
            // Corrige configuração salva incorretamente
            val correctedSettings = savedSettings.copy(isTransparent = false)
            SettingsManager.saveSettingsSync(correctedSettings)
        }
    }

    // SEMPRE inicia com dimensões padrão (não carrega dimensões salvas)
    // Mas CARREGA a posição salva se existir
    val initialPosition = if (savedSettings.windowX != null && savedSettings.windowY != null) {
        WindowPosition(savedSettings.windowX.dp, savedSettings.windowY.dp)
    } else {
        WindowPosition(Alignment.TopEnd)
    }
    
    val state = rememberWindowState(
        width = defaultWidth, 
        height = defaultHeight,
        position = initialPosition,
        isMinimized = false // SEMPRE inicia maximizada/normal
    )
    
    var isVisible by remember { mutableStateOf(true) }
    var isMiniMode by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hasSetInitialPosition by remember { mutableStateOf(false) }
    var currentScreenBounds by remember { mutableStateOf<Rectangle?>(null) }
    
    // Memoriza a última posição não-mini para restaurar
    var lastNormalPosition by remember { mutableStateOf(initialPosition) }
    var lastNormalSize by remember { mutableStateOf(DpSize(defaultWidth, defaultHeight)) }

    // Gerenciamento de altura - largura é FIXA exceto se usuário redimensionar manualmente
    var targetHeight by remember { mutableStateOf(defaultHeight) }
    
    var currentWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    
    // Dependencies via Koin
    val viewModel = remember { mutableStateOf<VerseViewModel?>(null) }
    
    // Coletar a preferência do ViewModel
    val isAnimatedWindow by if (viewModel.value != null) {
        viewModel.value!!.animatedWindow.collectAsState()
    } else {
        remember { mutableStateOf(true) }
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "WindowHeightAnimation"
    )

    val finalHeight = if (isAnimatedWindow) animatedHeight else targetHeight
    
    // Salva a posição sempre que mudar (modo normal)
    LaunchedEffect(state.position, state.size, isMiniMode) {
        // Só salva se estiver pronto, NÃO estiver em mini mode E o tamanho for maior que o mini widget
        if (isReady && !isMiniMode && state.size.width > miniSize) {
            // Memoriza posição e tamanho para restaurar depois
            lastNormalPosition = state.position
            lastNormalSize = state.size
            
            // Salva no arquivo de configuração
            delay(500) // Debounce
            val currentSettings = SettingsManager.getSettingsSync()
            SettingsManager.saveSettingsSync(
                currentSettings.copy(
                    windowX = state.position.x.value.toInt(),
                    windowY = state.position.y.value.toInt()
                )
            )
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Efeito de redimensionamento: Mantém largura estável, ajusta apenas altura
    LaunchedEffect(finalHeight, isMiniMode) {
        if (!isMiniMode && isReady && currentWindow != null) {
            val win = currentWindow!!
            // Usa os bounds do monitor em pixels
            val bounds = currentScreenBounds ?: getActiveMonitorBounds()
            
            if (bounds != null) {
                // Converte a altura alvo (DP) para Pixels Inteiros usando a densidade correta
                val targetHeightPx = with(density) { finalHeight.roundToPx() }
                
                // Obtém a largura nativa atual em Pixels (EVITA O CONFLITO COM O COMPOSE STATE)
                val currentWidthPx = win.width
                
                val currentYPx = win.y
                var newYPx = currentYPx
                val screenPaddingPx = with(density) { screenPadding.dp.roundToPx() }
                
                // Lógica de ancoragem no fundo (em Pixels)
                if (currentYPx + targetHeightPx > bounds.y + bounds.height - screenPaddingPx) {
                    newYPx = bounds.y + bounds.height - screenPaddingPx - targetHeightPx
                }
                
                // Aplica a atualização via AWT apenas se a altura mudou (tolerância de 1px)
                // Isso permite que o usuário redimensione a largura livremente, pois repassamos o currentWidthPx
                if (abs(win.height - targetHeightPx) > 1) {
                    win.setBounds(win.x, newYPx, currentWidthPx, targetHeightPx)
                }
            }
        }
    }
    
    fun applyAnchorPosition(mini: Boolean, height: Dp? = null) {
        val bounds = currentScreenBounds ?: getActiveMonitorBounds() ?: return
        
        if (mini) {
            // Salva posição atual antes de ir para mini mode
            lastNormalPosition = state.position
            lastNormalSize = state.size
            
            val newX = bounds.x + bounds.width - screenPadding - miniSize.value.toInt()
            val newY = bounds.y + (bounds.height / 2) - (miniSize.value.toInt() / 2)
            state.position = WindowPosition(newX.dp, newY.dp)
            state.size = DpSize(miniSize, miniSize)
        } else {
            // Restaura a última posição conhecida ao sair do mini mode
            if (lastNormalPosition != initialPosition || hasSetInitialPosition) {
                state.position = lastNormalPosition
                state.size = DpSize(lastNormalSize.width, height ?: lastNormalSize.height)
            } else {
                // Primeira vez - usa posição padrão
                val newX = bounds.x + bounds.width - screenPadding - defaultWidth.value.toInt()
                val newY = bounds.y + screenPadding
                state.position = WindowPosition(newX.dp, newY.dp)
                state.size = DpSize(defaultWidth, height ?: defaultHeight)
                hasSetInitialPosition = true
            }
        }
    }

    // Initialization
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val mappingBytes = Res.readBytes("files/bible_mapping.json")
                val mapping = Json.decodeFromString<Map<String, BookMetaData>>(mappingBytes.decodeToString())
                val dbBytes = Res.readBytes("files/bible.sqlite")
                val tempDbFile = File.createTempFile("bible_verse_db", ".sqlite").apply { deleteOnExit() }
                FileOutputStream(tempDbFile).use { it.write(dbBytes) }
                
                withContext(Dispatchers.Main) {
                    startKoin {
                        modules(
                            appModule,
                            module {
                                single { mapping }
                                single { BibleDatabase(tempDbFile.absolutePath) }
                                single { SettingsManager.dataDir }
                                single<SnapshotHandler> { JvmSnapshotHandler() }
                                single<CloudSyncProvider> { JvmGoogleDriveProvider() }
                            }
                        )
                    }
                    
                    val vm: VerseViewModel = get(VerseViewModel::class.java)
                    viewModel.value = vm
                    
                    val syncManager: SyncManager = get(SyncManager::class.java)
                    syncManager.startAutoSync()
                    
                    isReady = true
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(isReady) {
        if (isReady && viewModel.value != null) {
            withContext(Dispatchers.IO) {
                var lastText = ""
                ClipboardMonitor.textFlow().collect { text ->
                    if (text != lastText) {
                        viewModel.value!!.processQuery(text, isExternal = true)
                        lastText = text
                    }
                }
            }
        }
    }

    // Limpeza do arquivo Lock após 5 segundos de estabilidade
    LaunchedEffect(isReady) {
        if (isReady) {
            // Se o aviso estiver ativo, mostra via feedback do ViewModel
            if (System.getProperty("verse.transparencyWarning") == "true") {
                viewModel.value?.showToast("Aviso: Bordas arredondadas desativadas por segurança.")
            }
            
            delay(5000)
            try { SettingsManager.lockFile.delete() } catch (_: Exception) {}
        }
    }

    if (viewModel.value != null) {
        val detectedVerses by viewModel.value!!.detectedVerses.collectAsState()
        val isInternalUpdate by viewModel.value!!.isInternalUpdate.collectAsState()
        
        LaunchedEffect(detectedVerses) {
            if (detectedVerses.isNotEmpty()) {
                if (isLinux && isVisible && !isInternalUpdate) {
                    isVisible = false
                    delay(150) 
                }
                
                val newBounds = getActiveMonitorBounds()
                
                if (currentScreenBounds != null && newBounds != currentScreenBounds) {
                    currentScreenBounds = newBounds
                    applyAnchorPosition(mini = false)
                } else if (currentScreenBounds == null) {
                    currentScreenBounds = newBounds
                    hasSetInitialPosition = true
                }
                
                isVisible = true
                isMiniMode = false
                
                // Correção Windows: Forçar restauração se minimizado e aguardar estabilidade
                if (isWindows && !isLinux) {
                    state.isMinimized = false
                    delay(200) // Delay maior para Windows processar a composição transparente
                }

                currentWindow?.let { win ->
                    win.toFront()
                    win.requestFocus()
                }
            }
        }
    }

    LaunchedEffect(isMiniMode) { if (isReady) applyAnchorPosition(mini = isMiniMode) }

    DisposableEffect(Unit) { onDispose { stopKoin() } }

    val actualIsTraySupported = isTraySupported && !isLinux && !isWine
    if (actualIsTraySupported) {
        val toggleAction = {
            if (isVisible) {
                isVisible = false
            } else {
                scope.launch {
                    isVisible = true
                    state.isMinimized = false
                    if (isWindows) delay(250)
                    currentWindow?.toFront()
                    currentWindow?.requestFocus()
                }
                Unit
            }
        }

        Tray(icon = icon, tooltip = "IRSE | Bereia Verse", onAction = toggleAction, menu = {
            Item("Exibir/Ocultar", onClick = toggleAction)
            Separator()
            Item("Sair", onClick = { exitApplication() })
        })
    }

    Window(
        onCloseRequest = { 
            if (actualIsTraySupported) {
                isVisible = false
            } else {
                exitApplication() 
            }
        },
        title = "IRSE | Bereia Verse",
        state = state,
        icon = icon,
        visible = isVisible,
        undecorated = true, 
        transparent = shouldBeTransparent, 
        alwaysOnTop = true, 
        resizable = true // Permite redimensionamento
    ) {
        SideEffect { 
            currentWindow = window
            // No Windows, define como janela utilitária para não aparecer na barra de tarefas
            if (isWindows && window is java.awt.Window) {
                try {
                    val frame = window as? java.awt.Frame
                    // Em janelas undecorated, isso ajuda a ocultar da barra de tarefas
                    window.type = java.awt.Window.Type.UTILITY
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        
        AnimatedContent(
            targetState = isMiniMode,
            transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
        ) { mini ->
            if (mini) {
                MiniWidget(onClick = { 
                    isMiniMode = false
                    // Restaura posição e tamanho ao sair do mini mode
                    applyAnchorPosition(mini = false)
                }, isTransparent = shouldBeTransparent)
            } else {
                if (viewModel.value != null) {
                    App(
                        viewModel = viewModel.value!!,
                        onClose = { 
                            if (isWindows) {
                                state.isMinimized = true
                            } else {
                                if (actualIsTraySupported) isVisible = false else isMiniMode = true 
                            }
                        },
                        onHeightRequest = { height ->
                            if (!isMiniMode) {
                                targetHeight = height
                            }
                        },
                        isTransparent = shouldBeTransparent,
                        headerModifier = Modifier.pointerInput(Unit) {
                            var startWindowX = 0
                            var startWindowY = 0
                            var startMouseX = 0
                            var startMouseY = 0
                            
                            detectDragGestures(
                                onDragStart = {
                                    val awtWindow = window
                                    startWindowX = awtWindow.x
                                    startWindowY = awtWindow.y
                                    
                                    val mousePos = MouseInfo.getPointerInfo().location
                                    startMouseX = mousePos.x
                                    startMouseY = mousePos.y
                                },
                                onDragEnd = {
                                    val awtWindow = window
                                    val density = this.density
                                    state.position = WindowPosition(
                                        (awtWindow.x / density).dp,
                                        (awtWindow.y / density).dp
                                    )
                                }
                            ) { change, _ ->
                                change.consume()
                                
                                val currentMousePos = MouseInfo.getPointerInfo().location
                                val deltaX = currentMousePos.x - startMouseX
                                val deltaY = currentMousePos.y - startMouseY
                                
                                window.setLocation(startWindowX + deltaX, startWindowY + deltaY)
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MaterialTheme { CircularProgressIndicator(color = VerseColors.PrimaryAmber) }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun MiniWidget(onClick: () -> Unit, isTransparent: Boolean = true) {
    MaterialTheme {
        // Se sem transparência (Wine ou force), usa fundo sólido ou ajusta layout
        val surfaceColor = if (!isTransparent) Color(0xFF202020) else Color.Transparent
        
        Surface(color = surfaceColor, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(VerseColors.PrimaryAmber.copy(alpha = 0.8f)).clickable { onClick() }) {
                    Image(
                        painter = painterResource(Res.drawable.logo), 
                        contentDescription = null, 
                        modifier = Modifier.size(40.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}