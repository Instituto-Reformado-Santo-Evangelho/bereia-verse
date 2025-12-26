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
import kotlinx.coroutines.delay
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
    var detectedVerses by remember { mutableStateOf<List<Pair<VerseRequest, String?>>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    val parser = remember { mutableStateOf<BibleParser?>(null) }
    val database = remember { mutableStateOf<BibleDatabase?>(null) }
    var currentWindow by remember { mutableStateOf<java.awt.Window?>(null) }
    var currentScreenBounds by remember { mutableStateOf<Rectangle?>(null) }
    val icon = painterResource(Res.drawable.logo)
    val isLinux = remember { System.getProperty("os.name").lowercase().contains("linux") }
    val scope = rememberCoroutineScope()

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
        val bounds = currentScreenBounds ?: return
        val anchorX = bounds.x + bounds.width - screenPadding
        val width = if (mini) miniSize.value.toInt() else fullWidth.value.toInt()
        val newX = anchorX - width
        val newY = if (mini) bounds.y + (bounds.height / 2) - (miniSize.value.toInt() / 2) else bounds.y + screenPadding
        
        state.position = WindowPosition(newX.dp, newY.dp)
        
        val targetHeight = if (mini) miniSize else (height ?: state.size.height).coerceAtLeast(350.dp)
        state.size = DpSize(if (mini) miniSize else fullWidth, targetHeight)
    }

    // Função para processar uma query (usada na detecção e no histórico)
    suspend fun processQuery(text: String, addToHistory: Boolean = true) {
        if (parser.value == null || database.value == null) return
        isProcessing = true
        try {
            // Run synchronous parser in IO context to avoid blocking UI
            val requests = withContext(Dispatchers.IO) {
                parser.value!!.processSelection(text)
            }
            
            if (requests.isNotEmpty()) {
                val results = withContext(Dispatchers.IO) {
                    requests.map { req -> database.value!!.getText(req.id) to req }.map { it.second to it.first }
                }
                
                withContext(Dispatchers.Main) { 
                    detectedVerses = results 
                    if (addToHistory) {
                        HistoryManager.saveEntry(text)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() } 
        finally { isProcessing = false }
    }

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
                isReady = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(isReady) {
        if (isReady) {
            withContext(Dispatchers.IO) {
                var lastText = ""
                ClipboardMonitor.textFlow().collect { text ->
                    if (text != lastText) {
                        lastText = text
                        processQuery(text, addToHistory = true)
                    }
                }
            }
        }
    }

    LaunchedEffect(detectedVerses) {
        if (detectedVerses.isNotEmpty()) {
            currentScreenBounds = getActiveMonitorBounds()
            if (isLinux && isVisible) {
                isVisible = false
                delay(150) 
            }
            applyAnchorPosition(mini = false) 
            if (state.isMinimized) state.isMinimized = false
            isVisible = true
            isMiniMode = false
            currentWindow?.isVisible = true
            currentWindow?.toFront()
            currentWindow?.requestFocus()
        }
    }

    LaunchedEffect(isMiniMode) {
        if (currentScreenBounds == null) currentScreenBounds = getActiveMonitorBounds()
        applyAnchorPosition(mini = isMiniMode)
    }

    if (isTraySupported) {
        Tray(icon = icon, tooltip = "Bereia Verse", onAction = { isVisible = !isVisible }, menu = {
            Item("Exibir/Ocultar", onClick = { isVisible = !isVisible })
            Separator()
            Item("Sair", onClick = { exitApplication() })
        })
    }

    Window(
        onCloseRequest = { exitApplication() },
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
                App(
                    detectedVerses = detectedVerses,
                    isProcessing = isProcessing,
                    onClose = { if (isTraySupported) isVisible = false else isMiniMode = true },
                    onHeightRequest = { height ->
                        if (!isMiniMode && Math.abs(state.size.height.value - height.value) > 5) {
                            applyAnchorPosition(mini = false, height = height)
                        }
                    },
                    onSearch = { query ->
                        scope.launch(Dispatchers.IO) { processQuery(query, addToHistory = true) }
                    },
                    onVerseSelect = { req ->
                        scope.launch(Dispatchers.IO) {
                            val content = database.value?.getText(req.id)
                            withContext(Dispatchers.Main) {
                                detectedVerses = listOf(req to content)
                            }
                        }
                    },
                    bibleDatabase = database.value,
                    bibleRepository = parser.value?.repository
                )
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