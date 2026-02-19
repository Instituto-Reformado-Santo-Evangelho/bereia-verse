package br.com.irse.verse

import br.com.irse.verse.ui.theme.VerseColors
import androidx.compose.animation.AnimatedContent
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
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.get
import org.koin.dsl.module
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo
import verse.composeapp.generated.resources.sys_icon
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import java.net.ServerSocket
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val SINGLE_INSTANCE_PORT = 54321

class JvmDeepLinkHandler : br.com.irse.verse.core.DeepLinkHandler {
    private val _deepLinkFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val deepLinkFlow = _deepLinkFlow.asSharedFlow()

    override fun handleDeepLink(uri: String) {
        _deepLinkFlow.tryEmit(uri)
    }
}

fun getActiveMonitorBounds(): Rectangle? {
    return try {
        val pointerInfo = MouseInfo.getPointerInfo()
        val mousePoint = pointerInfo.location
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val device = ge.screenDevices.firstOrNull { it.defaultConfiguration.bounds.contains(mousePoint) }
        ?: ge.defaultScreenDevice
        
        val config = device.defaultConfiguration
        val bounds = config.bounds
        val insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(config)
        
        Rectangle(
            bounds.x + insets.left,
            bounds.y + insets.top,
            bounds.width - insets.left - insets.right,
            bounds.height - insets.top - insets.bottom
        )
    } catch (e: Exception) { e.printStackTrace(); null }
}

private var instanceServerSocket: ServerSocket? = null

fun handleSingleInstance(args: Array<String>, deepLinkHandler: br.com.irse.verse.core.DeepLinkHandler): Boolean {
    try {
        val serverSocket = ServerSocket(SINGLE_INSTANCE_PORT)
        instanceServerSocket = serverSocket
        
        Thread {
            while (!serverSocket.isClosed) {
                try {
                    val clientSocket = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val receivedArgs = reader.readLine()?.split("|")?.toTypedArray() ?: emptyArray()
                    
                    receivedArgs.find { it.startsWith("bereia-verse://") }?.let { uri ->
                        deepLinkHandler.handleDeepLink(uri)
                    }
                    
                    clientSocket.close()
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) e.printStackTrace()
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
        return true
    } catch (e: Exception) {
        try {
            val socket = Socket("localhost", SINGLE_INSTANCE_PORT)
            val writer = PrintWriter(socket.getOutputStream(), true)
            writer.println(args.joinToString("|"))
            socket.close()
        } catch (e2: Exception) {}
        return false
    }
}

fun main(args: Array<String>) {
    val deepLinkHandler = JvmDeepLinkHandler()

    if (!handleSingleInstance(args, deepLinkHandler)) {
        return
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        try { instanceServerSocket?.close() } catch (_: Exception) {}
    })

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
                false 
            }

            // Lógica de "Power User Override":
            // Se o usuário já ativou a transparência nas configurações, respeitamos essa escolha incondicionalmente.
            // Isso evita que falhas na detecção do AWT (comuns em Linux/Hyprland e alguns drivers Windows)
            // desativem o recurso automaticamente. Se for a primeira vez (padrão), respeitamos a detecção do sistema.
            val finalSupportFlag = if (settings.isTransparent) true else transparencySupported

            // Atualiza configurações se o flag de suporte mudou
            if (settings.isTransparencySupported != finalSupportFlag) {
                settings = settings.copy(isTransparencySupported = finalSupportFlag)
                SettingsManager.saveSettingsSync(settings)
            }

            // Apenas desativa se REALMENTE não houver suporte E o usuário não tiver forçado (lógica acima já cobre)
            // Mantemos este bloco para casos extremos onde o override não se aplica
            if (!finalSupportFlag && settings.isTransparent) {
                settings = settings.copy(isTransparent = false)
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

        runApplication(args, deepLinkHandler)
    } catch (e: Throwable) {
        // Se houve erro no Kotlin, remove o lock para não disparar safe mode falso (opcional)
        try { SettingsManager.lockFile.delete() } catch (_: Exception) {}
        
        val logDir = SettingsManager.dataDir
        val crashFile = File(logDir, "crash_log.txt")
        FileOutputStream(crashFile).use { out ->
            val message = "Crash at ${java.time.LocalDateTime.now()}\nOS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}\n\n${e.stackTraceToString()}"
            out.write(message.toByteArray())
        }
        e.printStackTrace() // Ainda imprime no console se disponível
    }
}

fun runApplication(args: Array<String>, deepLinkHandler: br.com.irse.verse.core.DeepLinkHandler) {
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

    if (isWindows && !isWineDetected) {
        // 1. Tenta liberar o Loopback de Rede (Exigido para OAuth em MSIX)
        try {
            // Nome da Família do Pacote extraído do seu log de instalação
            val packageFamilyName = "OrganizaoIRSE.BereiaVersculos_c6gk0bqxkzmpt"
            ProcessBuilder("cmd", "/c", "CheckNetIsolation.exe LoopbackExempt -a -n=$packageFamilyName").start()
        } catch (e: Exception) {
            // Ignora se falhar (ex: falta de admin), pois o app ainda deve tentar abrir
        }

        val crashDetected = SettingsManager.lockFile.exists()
        if (crashDetected) {
            // Se houve crash anterior, usa renderização por software para garantir que o app abra
            System.setProperty("skiko.renderApi", "SOFTWARE")
        } else {
            // Estabiliza renderização em Windows 11/10 com aceleração de hardware
            System.setProperty("skiko.renderApi", "DIRECT3D")
            System.setProperty("sun.java2d.d3d", "true")
        }
    }

    application {
    val defaultWidth = 400.dp
    val defaultHeight = 350.dp
    val minWindowSize = DpSize(280.dp, 300.dp)
    val miniSize = 64.dp
    val screenPadding = 4
    
    // Carrega configuração salva
    val savedSettings = remember { SettingsManager.getSettingsSync() }
    
    val icon = painterResource(Res.drawable.sys_icon)
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
    
    var isVisible by remember { mutableStateOf(false) }
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

    // Animação da altura
    val animatedHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetHeight,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "windowHeightAnimation"
    )

    // Efeito de redimensionamento: Mantém largura estável, ajusta apenas altura suavemente
    LaunchedEffect(animatedHeight) {
        if (!isMiniMode && isReady && currentWindow != null) {
            val win = currentWindow!!
            val targetHeightPx = animatedHeight.value.toInt()

            // Só ajusta se houver diferença relevante para evitar jitter
            if (abs(win.height - targetHeightPx) > 1) {
                // Pega os limites do monitor atual para garantir que não saia da tela
                val bounds = getActiveMonitorBounds() ?: currentScreenBounds
                
                if (bounds != null) {
                    val screenTop = bounds.y + screenPadding
                    val screenBottom = bounds.y + bounds.height - screenPadding
                    
                    // Garante que a altura não seja maior que o próprio monitor
                    val maxHeight = bounds.height - (screenPadding * 2)
                    val finalHeightPx = targetHeightPx.coerceAtMost(maxHeight)

                    // Se a nova altura fizer a janela passar do fundo da tela, move ela para cima
                    if (win.y + finalHeightPx > screenBottom) {
                        val newY = screenBottom - finalHeightPx
                        val adjustedY = newY.coerceAtLeast(screenTop)
                        
                        // Atualiza posição no estado do Compose (Dp) para manter sincronia
                        state.position = WindowPosition(state.position.x, adjustedY.dp)
                        win.setLocation(win.x, adjustedY)
                    }
                    
                    win.setSize(win.width, finalHeightPx)
                } else {
                    win.setSize(win.width, targetHeightPx)
                }
            }
        }
    }
    
    fun applyAnchorPosition(mini: Boolean, height: Dp? = null) {
        val bounds = getActiveMonitorBounds() ?: currentScreenBounds ?: return
        
        if (mini) {
            // Salva posição atual antes de ir para mini mode
            lastNormalPosition = state.position
            lastNormalSize = state.size
            
            // Pixels do monitor já são lógicos no Windows/Compose
            val boundsX = bounds.x
            val boundsWidth = bounds.width
            val boundsY = bounds.y
            val boundsHeight = bounds.height
            
            val newX = boundsX + boundsWidth - screenPadding - miniSize.value
            val newY = boundsY + (boundsHeight / 2) - (miniSize.value / 2)
            state.position = WindowPosition(newX.dp, newY.dp)
            state.size = DpSize(miniSize, miniSize)
        } else {
            // Restaura a última posição conhecida ao sair do mini mode
            if (lastNormalPosition != initialPosition || hasSetInitialPosition) {
                state.position = lastNormalPosition
                state.size = DpSize(lastNormalSize.width, height ?: lastNormalSize.height)
            } else {
                // Primeira vez - usa posição padrão (canto superior direito do monitor ativo)
                val boundsX = bounds.x
                val boundsWidth = bounds.width
                val boundsY = bounds.y
                
                val newX = boundsX + boundsWidth - screenPadding - defaultWidth.value
                val newY = boundsY + screenPadding
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
                                single<DeepLinkHandler> { deepLinkHandler }
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
            isVisible = true
            
            // Traz o app para frente quando um deep link é recebido (mesmo com app já aberto)
            scope.launch {
                deepLinkHandler.deepLinkFlow.collect { _ ->
                    isVisible = true
                    state.isMinimized = false
                    currentWindow?.toFront()
                }
            }

            // Verifica se o app foi aberto INICIALMENTE via protocolo
            args.find { it.startsWith("bereia-verse://") }?.let { uri ->
                deepLinkHandler.handleDeepLink(uri)
            }

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

    LaunchedEffect(state.isMinimized) {
        if (!state.isMinimized) { // Se a janela não estiver mais minimizada (SO a restaurou)
            isVisible = true      // Garante que nosso estado de visibilidade também reflita isso
        }
    }

    if (viewModel.value != null) {
        val detectedVerses by viewModel.value!!.detectedVerses.collectAsState()
        val isInternalUpdate by viewModel.value!!.isInternalUpdate.collectAsState()
        
        LaunchedEffect(detectedVerses) {
            if (detectedVerses.isNotEmpty()) {
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

    LaunchedEffect(isMiniMode, currentWindow) {
        currentWindow?.let { win ->
            val minSize = if (isMiniMode) DpSize(miniSize, miniSize) else minWindowSize
            win.minimumSize = Dimension(minSize.width.value.toInt(), minSize.height.value.toInt())
        }

        if (isReady) {
            applyAnchorPosition(mini = isMiniMode)
        }
    }

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

        Tray(icon = icon, tooltip = "Bereia Versículos | IRSE", onAction = toggleAction, menu = {
            Item("Exibir/Ocultar", onClick = toggleAction)
            Item("Alternar Transparência", onClick = {
                scope.launch {
                    val currentTransparency = viewModel.value?.isTransparent?.value ?: false
                    viewModel.value?.updateIsTransparent(!currentTransparency)
                    delay(500) // Garante que a configuração seja salva antes de sair
                    System.exit(0) // Fecha o app para aplicar a mudança
                }
            })
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
        title = "Bereia Versículos | IRSE",
        state = state,
        icon = icon,
        visible = isVisible,
        undecorated = true, 
        transparent = shouldBeTransparent, 
        alwaysOnTop = isLinux || isWindows || isMac, 
        resizable = true // Permite redimensionamento
    ) {
        // O SideEffect que definia o tipo da janela foi removido para corrigir um crash
        // na inicialização (IllegalComponentStateException). A consequência é que
        // a janela pode aparecer na barra de tarefas em alguns cenários.
        SideEffect {
            currentWindow = window
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
                            var startMousePos = java.awt.Point()
                            var startWindowPos = java.awt.Point()
                            
                            detectDragGestures(
                                onDragStart = {
                                    // Captura a posição inicial absoluta (em pixels) para evitar loop de feedback
                                    startMousePos = MouseInfo.getPointerInfo().location
                                    startWindowPos = java.awt.Point(window.x, window.y)
                                    window.toFront()
                                }
                            ) { change, _ ->
                                change.consume()
                                
                                // Calcula o deslocamento real baseado na tela inteira
                                val currentMousePos = MouseInfo.getPointerInfo().location
                                val deltaX = currentMousePos.x - startMousePos.x
                                val deltaY = currentMousePos.y - startMousePos.y
                                
                                // Nova posição sugerida em pixels
                                var newXPx = startWindowPos.x + deltaX
                                var newYPx = startWindowPos.y + deltaY

                                // Confinamento aos limites do monitor
                                val bounds = getActiveMonitorBounds() ?: currentScreenBounds
                                if (bounds != null) {
                                    val paddingPx = screenPadding
                                    newXPx = newXPx.coerceIn(bounds.x + paddingPx, bounds.x + bounds.width - window.width - paddingPx)
                                    newYPx = newYPx.coerceIn(bounds.y + paddingPx, bounds.y + bounds.height - window.height - paddingPx)
                                }
                                
                                // Aplica a nova posição (1 logical pixel = 1 Dp no Windows/Compose)
                                state.position = WindowPosition(newXPx.dp, newYPx.dp)
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
                        painter = painterResource(Res.drawable.sys_icon), 
                        contentDescription = null, 
                        modifier = Modifier.size(35.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}
