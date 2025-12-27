package br.com.irse.verse

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.*
import writers.composeapp.generated.resources.Res
import writers.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import java.awt.MouseInfo
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import br.com.irse.verse.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

fun main() = application {
    val fullWidth = 400.dp
    val miniSize = 64.dp
    val screenPadding = 20
    
    val state = rememberWindowState(
        width = fullWidth, 
        height = 350.dp,
        position = WindowPosition(androidx.compose.ui.Alignment.TopEnd)
    )
    
    var isVisible by remember { mutableStateOf(true) }
    var isMiniMode by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    
    // Dependencies
    val parser = remember { mutableStateOf<BibleParser?>(null) }
    val database = remember { mutableStateOf<BibleDatabase?>(null) }
    val viewModel = remember { mutableStateOf<VerseViewModel?>(null) }
    
    var currentWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    var currentScreenBounds by remember { mutableStateOf<Rectangle?>(null) }
    val icon = painterResource(Res.drawable.logo)
    val isLinux = remember { System.getProperty("os.name").lowercase().contains("linux") }
    val isWine = remember { System.getProperty("os.name").lowercase().contains("windows") && 
                            (System.getenv("WINEPREFIX") != null || System.getenv("WINELOADERNOEXEC") != null) }

    fun getActiveMonitorBounds(): Rectangle? {
        return try {
            val pointerInfo = MouseInfo.getPointerInfo()
            val mousePoint = pointerInfo.location
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            ge.screenDevices.firstOrNull { it.defaultConfiguration.bounds.contains(mousePoint) }?.defaultConfiguration?.bounds
            ?: ge.defaultScreenDevice.defaultConfiguration.bounds
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun applyAnchorPosition(mini: Boolean, height: Dp? = null) {
        val bounds = currentScreenBounds ?: getActiveMonitorBounds() ?: return
        currentScreenBounds = bounds
        val anchorX = bounds.x + bounds.width - screenPadding
        val width = if (mini) miniSize.value.toInt() else fullWidth.value.toInt()
        val newX = anchorX - width
        val newY = if (mini) bounds.y + (bounds.height / 2) - (miniSize.value.toInt() / 2) else bounds.y + screenPadding
        
        state.position = WindowPosition(newX.dp, newY.dp)
        
        val targetHeight = if (mini) miniSize else (height ?: state.size.height).coerceAtLeast(350.dp)
        state.size = DpSize(if (mini) miniSize else fullWidth, targetHeight)
    }

    // Initialization
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val mappingBytes = Res.readBytes("files/bible_mapping.json")
                val mapping = Json.decodeFromString<Map<String, BookMetaData>>(mappingBytes.decodeToString())
                val repo = BibleRepository(mapping)
                parser.value = BibleParser(repo)
                val dbBytes = Res.readBytes("files/bible.sqlite")
                val tempDbFile = File.createTempFile("bible_verse_db", ".sqlite").apply { deleteOnExit() }
                FileOutputStream(tempDbFile).use { it.write(dbBytes) }
                database.value = BibleDatabase(tempDbFile.absolutePath)
                
                withContext(Dispatchers.Main) {
                    viewModel.value = VerseViewModel(parser.value!!, database.value!!)
                    isReady = true
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Clipboard Monitor
    LaunchedEffect(isReady) {
        if (isReady && viewModel.value != null) {
            withContext(Dispatchers.IO) {
                var lastText = ""
                ClipboardMonitor.textFlow().collect { text ->
                    if (text != lastText) {
                        lastText = text
                        viewModel.value!!.processQuery(text)
                    }
                }
            }
        }
    }

    // Window Management based on ViewModel State
    if (viewModel.value != null) {
        val detectedVerses by viewModel.value!!.detectedVerses.collectAsState()
        
        LaunchedEffect(detectedVerses) {
            if (detectedVerses.isNotEmpty()) {
                // Para Hyprland/Wayland, sempre forçamos o ciclo Hide->Show quando o conteúdo muda.
                // Isso garante que a janela 'caminhe' para o workspace onde o mouse está.
                if (isLinux && isVisible) {
                    isVisible = false
                    kotlinx.coroutines.delay(150) 
                }

                currentScreenBounds = getActiveMonitorBounds()
                applyAnchorPosition(mini = false) 
                
                if (state.isMinimized) state.isMinimized = false
                isVisible = true
                isMiniMode = false
                
                currentWindow?.let { win ->
                    win.isVisible = true
                    win.toFront()
                    win.requestFocus()
                }
            }
        }
    }

    LaunchedEffect(isMiniMode) {
        if (isReady) applyAnchorPosition(mini = isMiniMode)
    }

    val actualIsTraySupported = isTraySupported && !isLinux && !isWine

    if (actualIsTraySupported) {
        Tray(icon = icon, tooltip = "Bereia Verse", onAction = { isVisible = !isVisible }, menu = {
            Item("Exibir/Ocultar", onClick = { isVisible = !isVisible })
            Separator()
            Item("Sair", onClick = { exitApplication() })
        })
    }

    Window(
        onCloseRequest = { 
            exitApplication() // Win + C ou fechar via gerenciador de janelas mata o processo
        },
        title = "Bereia Verse",
        state = state,
        icon = icon,
        visible = isVisible,
        undecorated = true, transparent = true, alwaysOnTop = true, resizable = false
    ) {
        SideEffect { currentWindow = window }
        
        AnimatedContent(
            targetState = isMiniMode,
            transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
        ) { mini ->
            if (mini) {
                MiniWidget(onClick = { isMiniMode = false })
            } else {
                if (viewModel.value != null) {
                    App(
                        viewModel = viewModel.value!!,
                        onClose = { if (actualIsTraySupported) isVisible = false else if (isWine) isVisible = false else isMiniMode = true },
                        onHeightRequest = { height ->
                            if (!isMiniMode && Math.abs(state.size.height.value - height.value) > 10) {
                                applyAnchorPosition(mini = false, height = height)
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MaterialTheme {
                            CircularProgressIndicator(color = Color(0xFFFFC107))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniWidget(onClick: () -> Unit) {
    MaterialTheme {
        Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFC107).copy(alpha = 0.8f)).clickable { onClick() }) {
                    Image(painter = painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(32.dp).align(Alignment.Center))
                }
            }
        }
    }
}