package br.com.irse.verse

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.components.BibleIcon
import br.com.irse.verse.ui.components.CheckIcon
import br.com.irse.verse.ui.components.CopyIcon
import br.com.irse.verse.ui.components.FireAnimation
import br.com.irse.verse.ui.components.HistoryIcon
import br.com.irse.verse.ui.components.SearchIcon
import br.com.irse.verse.ui.components.SettingsIcon
import br.com.irse.verse.ui.copyToClipboard
import br.com.irse.verse.ui.onHover
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.views.AboutView
import br.com.irse.verse.ui.views.HistoryView
import br.com.irse.verse.ui.views.SearchView
import br.com.irse.verse.ui.views.SettingsView
import br.com.irse.verse.ui.views.VersesView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo

// Cores de Identidade
val PrimaryAmber = Color(0xFFFFC107)
val HeaderContentColor = Color(0xFF333333)

// Theme Colors
val LightSurface = Color.White
val DarkSurface = Color(0xFF1E1E1E)
val DarkText = Color(0xFFE0E0E0)
val DarkFooter = Color(0xFF2D2D2D)
val DarkBorder = Color(0xFF444444)

enum class AppTab { VERSES, HISTORY, SEARCH, ABOUT, SETTINGS }

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(
    viewModel: VerseViewModel,
    onClose: () -> Unit = {},
    onHeightRequest: (Dp) -> Unit = {}
) {
    val detectedVerses by viewModel.detectedVerses.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val history by viewModel.history.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    
    // Navigation State
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(AppTab.VERSES) }
    
    val uniqueBooks = remember(detectedVerses) { detectedVerses.map { it.first.book }.distinct() }
    val titleDisplay = when (currentTab) {
        AppTab.HISTORY -> Strings.HISTORY_TAB
        AppTab.SEARCH -> Strings.SEARCH_TAB
        AppTab.ABOUT -> Strings.ABOUT_TAB
        AppTab.SETTINGS -> Strings.SETTINGS_TAB
        AppTab.VERSES -> if (uniqueBooks.size == 1) uniqueBooks.first() else if (uniqueBooks.isEmpty()) Strings.VERSES_TAB else "${uniqueBooks.size} ${Strings.BOOKS_DETECTED}"
    }
    
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) DarkSurface else LightSurface
    val textColor = if (isDark) DarkText else Color(0xFF333333)
    val footerColor = if (isDark) DarkFooter else Color(0xFFF5F5F5)
    val borderColor = if (isDark) DarkBorder else Color.LightGray

    val globalFontFamily = when (fontFamily) {
        "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }

    val targetHeight = remember(detectedVerses, currentTab) {
        when(currentTab) {
            AppTab.VERSES -> if (detectedVerses.isEmpty()) 350.dp else (150.dp + (130.dp * detectedVerses.size)).coerceIn(400.dp, 600.dp)
            else -> 600.dp 
        }
    }

    LaunchedEffect(targetHeight) {
        onHeightRequest(targetHeight)
    }

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme(primary = PrimaryAmber, surface = surfaceColor, onSurface = textColor)
        else lightColorScheme(primary = PrimaryAmber, surface = surfaceColor, onSurface = textColor)
    ) {
        var isCopied by remember { mutableStateOf(false) }
        
        LaunchedEffect(detectedVerses, currentTab) {
            isCopied = false
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val isCtrl = event.isCtrlPressed
                        when (event.key) {
                            Key.Escape -> { onClose(); true }
                            Key.M -> if (isCtrl) { onClose(); true } else false
                            Key.F -> if (isCtrl) { currentTab = AppTab.SEARCH; true } else false
                            Key.H -> if (isCtrl) { currentTab = AppTab.HISTORY; true } else false
                            Key.V -> if (isCtrl) { currentTab = AppTab.VERSES; true } else false
                            Key.S -> if (isCtrl) { currentTab = AppTab.SETTINGS; true } else false
                            Key.I -> if (isCtrl) { currentTab = AppTab.ABOUT; true } else false
                            Key.DirectionLeft -> if (isCtrl && canGoBack) { viewModel.navigateBack(); true } else false
                            Key.DirectionRight -> if (isCtrl && canGoForward) { viewModel.navigateForward(); true } else false
                            else -> false
                        }
                    } else false
                }, 
            shape = RoundedCornerShape(16.dp), 
            color = surfaceColor, 
            shadowElevation = 8.dp, 
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(PrimaryAmber).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.weight(1f)
                            .pointerHoverIconHand()
                            .clickable { currentTab = AppTab.ABOUT }
                    ) {
                        Image(painter = painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = Strings.APP_TITLE, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HeaderContentColor, fontSize = 16.sp)
                            Text(text = titleDisplay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = HeaderContentColor.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(HeaderContentColor.copy(alpha = 0.1f))
                            .pointerHoverIconHand()
                            .clickable { if (currentTab != AppTab.VERSES) currentTab = AppTab.VERSES else onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (currentTab != AppTab.VERSES) Icons.Default.ArrowCircleLeft else Icons.Default.Close
                        Icon(icon, contentDescription = null, tint = HeaderContentColor, modifier = Modifier.size(20.dp))
                    }
                }

                if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = PrimaryAmber.copy(alpha = 0.5f))

                // Navigation Bar (Internal) - Only visible when in Verses Tab
                if (currentTab == AppTab.VERSES && detectedVerses.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, // Botões nas extremidades
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.navigateBack() }, 
                                enabled = canGoBack,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.ArrowLeft, 
                                    contentDescription = "Voltar",
                                    tint = if (canGoBack) textColor else textColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateForward() }, 
                                enabled = canGoForward,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.ArrowRight, 
                                    contentDescription = "Avançar",
                                    tint = if (canGoForward) textColor else textColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.3f))
                    }
                }

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        AppTab.HISTORY -> HistoryView(
                            history = history,
                            onSelect = { 
                                viewModel.processQuery(it)
                                currentTab = AppTab.VERSES 
                            }, 
                            textColor = textColor,
                            fontSize = fontSize,
                            fontFamily = globalFontFamily
                        )
                        AppTab.SEARCH -> SearchView(
                            viewModel = viewModel,
                            onVerseSelect = { 
                                viewModel.selectVerse(it)
                                currentTab = AppTab.VERSES 
                            },
                            textColor = textColor,
                            fontSize = fontSize,
                            fontFamily = globalFontFamily
                        )
                        AppTab.ABOUT -> AboutView(textColor)
                        AppTab.SETTINGS -> SettingsView(viewModel, textColor)
                        AppTab.VERSES -> {
                            if (detectedVerses.isEmpty()) {
                                val showFireAnimation by viewModel.showFireAnimation.collectAsState()
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (showFireAnimation) {
                                        FireAnimation(modifier = Modifier.fillMaxSize().alpha(0.6f))
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = Strings.COPY_HINT_TITLE, 
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.3f),
                                                        blurRadius = 8f
                                                    )
                                                ), 
                                                color = textColor
                                            )
                                            Text(
                                                text = Strings.COPY_HINT_SUBTITLE, 
                                                style = MaterialTheme.typography.bodyLarge, 
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                VersesView(
                                    detectedVerses, 
                                    uniqueBooks, 
                                    textColor, 
                                    fontSize, 
                                    globalFontFamily, 
                                    lineHeight,
                                    onLoadContext = viewModel::loadContext,
                                    onRemoveContext = viewModel::removeContext
                                )
                            }
                        }
                    }
                }

                // Footer
                Row(modifier = Modifier.fillMaxWidth().background(footerColor).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
                        // Search Button
                        var isSearchHovered by remember { mutableStateOf(false) }
                        Surface(
                            color = if (currentTab == AppTab.SEARCH || isSearchHovered) PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .pointerHoverIconHand()
                                .onHover(onEnter = { isSearchHovered = true }, onExit = { isSearchHovered = false })
                                .clickable { currentTab = AppTab.SEARCH }
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                SearchIcon(color = if (currentTab == AppTab.SEARCH || isSearchHovered) PrimaryAmber else textColor.copy(alpha = 0.6f))
                            }
                        }

                        // History Button
                        var isHistoryHovered by remember { mutableStateOf(false) }
                        Surface(
                            color = if (currentTab == AppTab.HISTORY || isHistoryHovered) PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .pointerHoverIconHand()
                                .onHover(onEnter = { isHistoryHovered = true }, onExit = { isHistoryHovered = false })
                                .clickable { 
                                    currentTab = AppTab.HISTORY
                                    viewModel.refreshHistory()
                                }
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                HistoryIcon(color = if (currentTab == AppTab.HISTORY || isHistoryHovered) PrimaryAmber else textColor.copy(alpha = 0.6f))
                            }
                        }

                        // Settings Button
                        var isSettingsHovered by remember { mutableStateOf(false) }
                        Surface(
                            color = if (currentTab == AppTab.SETTINGS || isSettingsHovered) PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .pointerHoverIconHand()
                                .onHover(onEnter = { isSettingsHovered = true }, onExit = { isSettingsHovered = false })
                                .clickable { currentTab = AppTab.SETTINGS }
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                SettingsIcon(color = if (currentTab == AppTab.SETTINGS || isSettingsHovered) PrimaryAmber else textColor.copy(alpha = 0.6f))
                            }
                        }

                        // About Button (Bible Icon)
                        var isAboutHovered by remember { mutableStateOf(false) }
                        Surface(
                            color = if (currentTab == AppTab.ABOUT || isAboutHovered) PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .pointerHoverIconHand()
                                .onHover(onEnter = { isAboutHovered = true }, onExit = { isAboutHovered = false })
                                .clickable { currentTab = AppTab.ABOUT }
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                BibleIcon(color = if (currentTab == AppTab.ABOUT || isAboutHovered) PrimaryAmber else textColor.copy(alpha = 0.6f))
                            }
                        }
                    }
                    
                    if (currentTab == AppTab.VERSES && detectedVerses.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val fullText = detectedVerses.joinToString("\n\n") { (req, content) ->
                                    val cleanContent = content?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                                    "$cleanContent (${req.book} ${req.chapter}:${req.verse} - ACF)"
                                }
                                scope.launch {
                                    copyToClipboard(clipboard, fullText)
                                }
                                isCopied = true
                            }, 
                            modifier = Modifier.size(32.dp).padding(end = 4.dp).pointerHoverIconHand()
                        ) {
                             if (isCopied) CheckIcon(color = Color(0xFF2E7D32)) else CopyIcon(color = textColor.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}
