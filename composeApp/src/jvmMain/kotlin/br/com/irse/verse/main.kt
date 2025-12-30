package br.com.irse.verse

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

fun main() {
    // Configuração de compatibilidade ANTES de iniciar a aplicação Compose
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    
    // Detecção robusta do Wine (Env Vars + Registry)
    var isWineDetected = isWindows && (System.getenv("WINEPREFIX") != null || System.getenv("WINELOADERNOEXEC") != null)
    
    if (isWindows && !isWineDetected) {
        try {
            // Tenta detectar chave do Wine no registro se as env vars falharem
            val process = ProcessBuilder("reg", "query", "HKLM\\Software\\Wine").start()
            isWineDetected = process.waitFor() == 0
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
    val fullWidth = 400.dp
    val miniSize = 64.dp
    val screenPadding = 20
    
    val state = rememberWindowState(
        width = fullWidth, 
        height = 400.dp,
        position = WindowPosition(Alignment.TopEnd)
    )
    
    var isVisible by remember { mutableStateOf(true) }
    var isMiniMode by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var hasSetInitialPosition by remember { mutableStateOf(false) }
    var currentScreenBounds by remember { mutableStateOf<Rectangle?>(null) }

    // Gerenciamento de altura
    var targetHeight by remember { mutableStateOf(400.dp) }
    
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

    // Efeito de redimensionamento: Ajusta apenas Y se sair da tela, mantém X intacto
    LaunchedEffect(finalHeight) {
        if (!isMiniMode && isReady) {
            val bounds = currentScreenBounds ?: getActiveMonitorBounds() ?: return@LaunchedEffect
            val currentX = state.position.x
            val currentY = state.position.y.value
            
            var adjustedY = currentY
            if (currentY + finalHeight.value > bounds.y + bounds.height - screenPadding) {
                adjustedY = bounds.y + bounds.height - screenPadding - finalHeight.value
            }
            
            state.position = WindowPosition(currentX, adjustedY.dp)
            state.size = DpSize(fullWidth, finalHeight)
        }
    }
    
    var currentWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    val icon = painterResource(Res.drawable.logo)
    val isLinux = remember { System.getProperty("os.name").lowercase().contains("linux") }
    val isWindows = remember { System.getProperty("os.name").lowercase().contains("win") }
    // Usa a propriedade definida no início do main()
    val isWine = remember { System.getProperty("verse.isWine") == "true" }
    
    val shouldBeTransparent = !isWine

    fun applyAnchorPosition(mini: Boolean, height: Dp? = null) {
        val bounds = currentScreenBounds ?: getActiveMonitorBounds() ?: return
        val targetWidth = if (mini) miniSize else fullWidth
        val h = if (mini) miniSize else (height ?: targetHeight).coerceAtLeast(400.dp)
        
        if (mini) {
            val newX = bounds.x + bounds.width - screenPadding - miniSize.value.toInt()
            val newY = bounds.y + (bounds.height / 2) - (miniSize.value.toInt() / 2)
            state.position = WindowPosition(newX.dp, newY.dp)
        } else {
            val newX = bounds.x + bounds.width - screenPadding - targetWidth.value.toInt()
            val newY = bounds.y + screenPadding
            state.position = WindowPosition(newX.dp, newY.dp)
            hasSetInitialPosition = true
        }
        
        state.size = DpSize(targetWidth, h)
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
        Tray(icon = icon, tooltip = "IRSE | Bereia Verse", onAction = { isVisible = !isVisible }, menu = {
            Item("Exibir/Ocultar", onClick = { isVisible = !isVisible })
            Separator()
            Item("Sair", onClick = { exitApplication() })
        })
    }

    Window(
        onCloseRequest = { exitApplication() },
        title = "IRSE | Bereia Verse",
        state = state,
        icon = icon,
        visible = isVisible,
        undecorated = true, 
        transparent = shouldBeTransparent, 
        alwaysOnTop = true, 
        resizable = false
    ) {
        SideEffect { currentWindow = window }
        if (shouldBeTransparent) {
            LaunchedEffect(Unit) { window.setBackground(java.awt.Color(0, 0, 0, 0)) }
        }
        
        AnimatedContent(
            targetState = isMiniMode,
            transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
        ) { mini ->
            if (mini) {
                MiniWidget(onClick = { isMiniMode = false }, isWine = isWine)
            } else {
                if (viewModel.value != null) {
                    App(
                        viewModel = viewModel.value!!,
                        onClose = { if (actualIsTraySupported) isVisible = false else if (isWine) isVisible = false else isMiniMode = true },
                        onHeightRequest = { height ->
                            if (!isMiniMode) {
                                targetHeight = height
                            }
                        },
                        isWine = isWine
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MaterialTheme { CircularProgressIndicator(color = Color(0xFFFFC107)) }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun MiniWidget(onClick: () -> Unit, isWine: Boolean = false) {
    MaterialTheme {
        // Se Wine (sem transparência), usa fundo sólido ou ajusta layout
        val surfaceColor = if (isWine) Color(0xFF202020) else Color.Transparent
        
        Surface(color = surfaceColor, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFC107).copy(alpha = 0.8f)).clickable { onClick() }) {
                    Image(painter = painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(32.dp).align(Alignment.Center))
                }
            }
        }
    }
}