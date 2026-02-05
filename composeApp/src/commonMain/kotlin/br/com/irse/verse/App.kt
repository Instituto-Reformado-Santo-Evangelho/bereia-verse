package br.com.irse.verse

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.AppTab
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.components.*
import br.com.irse.verse.ui.onHover
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors
import br.com.irse.verse.ui.views.*
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Camera
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo

// App tabs imported from core

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(
    viewModel: VerseViewModel,
    onClose: () -> Unit = {},
    onHeightRequest: (Dp) -> Unit = {},
    isTransparent: Boolean = true,
    headerModifier: Modifier = Modifier
)
{
    val detectedVerses by viewModel.detectedVerses.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val history by viewModel.history.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val showSnapshotAction by viewModel.showSnapshotAction.collectAsState()

    val clipboard = LocalClipboardManager.current
    var currentTab by remember { mutableStateOf(AppTab.VERSES) }

    // Navigation Request Listener (e.g. from Smart Links)
    LaunchedEffect(Unit) {
        viewModel.tabRequest.collect { tab ->
            currentTab = tab
        }
    }

    // Reset para a aba de versículos quando novos versículos são detectados externamente
    LaunchedEffect(detectedVerses) {
        if (detectedVerses.isNotEmpty()) {
            currentTab = AppTab.VERSES
        }
    }
    
    // Abre a aba de notas quando o editor é ativado (ex: via pesquisa)
    LaunchedEffect(isNoteEditorOpen) {
        if (isNoteEditorOpen) {
            currentTab = AppTab.NOTES
        }
    }
    
    val uniqueBooks = remember(detectedVerses) { detectedVerses.map { it.first.book }.distinct() }
    val titleDisplay = when (currentTab) {
        AppTab.HISTORY -> stringResource(Res.string.tab_history)
        AppTab.SEARCH -> stringResource(Res.string.tab_search)
        AppTab.NOTES -> stringResource(Res.string.tab_notes)
        AppTab.ABOUT -> stringResource(Res.string.tab_about)
        AppTab.SETTINGS -> stringResource(Res.string.tab_settings)
        AppTab.VERSES -> if (uniqueBooks.size == 1) uniqueBooks.first() else if (uniqueBooks.isEmpty()) stringResource(Res.string.tab_verses) else "${uniqueBooks.size} ${stringResource(Res.string.books_detected)}"
    }
    
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) VerseColors.DarkSurface else VerseColors.LightSurface
    val textColor = if (isDark) VerseColors.DarkText else VerseColors.LightText
    val footerColor = if (isDark) VerseColors.DarkFooter else VerseColors.LightFooter
    val borderColor = if (isDark) VerseColors.DarkBorder else VerseColors.LightBorder
    
    // Se sem transparência, usa cantos retos para evitar fundo preto ou orelhas
    val windowShape = if (!isTransparent) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(12.dp)

    val globalFontFamily = when (fontFamily) {
        "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
        "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
        "cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }

    val targetHeight = remember(detectedVerses, currentTab, isNoteEditorOpen) {
        if (isNoteEditorOpen) 600.dp
        else when(currentTab) {
            AppTab.VERSES -> if (detectedVerses.isEmpty()) 350.dp else (150.dp + (130.dp * detectedVerses.size)).coerceIn(400.dp, 600.dp)
            else -> 600.dp 
        }
    }

    LaunchedEffect(targetHeight) {
        onHeightRequest(targetHeight)
    }

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme(primary = VerseColors.PrimaryAmber, surface = surfaceColor, onSurface = textColor)
        else lightColorScheme(primary = VerseColors.PrimaryAmber, surface = surfaceColor, onSurface = textColor)
    ) {
        val toastState by viewModel.toastState.collectAsState()
        
        LaunchedEffect(detectedVerses, currentTab) {
            // Pode resetar estados se necessário
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val isCtrl = event.isCtrlPressed
                        when (event.key) {
                            Key.Escape -> { onClose(); true }
                            Key.M -> if (isCtrl) { onClose(); true } else false
                            Key.F -> if (isCtrl) { currentTab = AppTab.SEARCH; true } else false
                            Key.H -> if (isCtrl) { currentTab = AppTab.HISTORY; true } else false
                            Key.N -> if (isCtrl) { currentTab = AppTab.NOTES; true } else false
                            Key.V -> if (isCtrl) { currentTab = AppTab.VERSES; true } else false
                            Key.S -> if (isCtrl) { currentTab = AppTab.SETTINGS; true } else false
                            Key.I -> if (isCtrl) { currentTab = AppTab.ABOUT; true } else false
                            Key.DirectionLeft -> if (isCtrl && canGoBack) { viewModel.navigateBack(); true } else false
                            Key.DirectionRight -> if (isCtrl && canGoForward) { viewModel.navigateForward(); true } else false
                            else -> false
                        }
                    } else false
                }, 
            shape = windowShape, 
            color = surfaceColor, 
            shadowElevation = 8.dp, 
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    val headerShape = if (!isTransparent) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(headerShape)
                            .background(VerseColors.PrimaryAmber)
                            .then(headerModifier) // Aplica o modificador de arrasto aqui
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.weight(1f)
                                .pointerHoverIconHand()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { currentTab = AppTab.ABOUT }
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.logo),  
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = stringResource(Res.string.app_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VerseColors.HeaderContentColor, fontSize = 16.sp)
                                Text(text = titleDisplay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = VerseColors.HeaderContentColor.copy(alpha = 0.8f), fontSize = 13.sp)
                            }
                        }
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(VerseColors.HeaderContentColor.copy(alpha = 0.1f))
                                .pointerHoverIconHand()
                                .clickable { if (currentTab != AppTab.VERSES) currentTab = AppTab.VERSES else onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = if (currentTab != AppTab.VERSES) Icons.Default.ArrowCircleLeft else Icons.Default.Close
                            Icon(icon, contentDescription = null, tint = VerseColors.HeaderContentColor, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = VerseColors.PrimaryAmber.copy(alpha = 0.5f))

                    // Navigation Bar (Internal) - Only visible when in Verses Tab
                    if (currentTab == AppTab.VERSES && detectedVerses.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.navigateBack() }, enabled = canGoBack, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = FeatherIcons.ArrowLeft, contentDescription = stringResource(Res.string.back), tint = if (canGoBack) textColor else textColor.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.navigateForward() }, enabled = canGoForward, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = FeatherIcons.ArrowRight, contentDescription = stringResource(Res.string.forward), tint = if (canGoForward) textColor else textColor.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = borderColor.copy(alpha = 0.3f))
                        }
                    }

                    // Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentTab) {
                            AppTab.HISTORY -> HistoryView(history = history, onSelect = { viewModel.processQuery(it); currentTab = AppTab.VERSES }, textColor = textColor, fontSize = fontSize, fontFamily = globalFontFamily)
                            AppTab.SEARCH -> SearchView(viewModel = viewModel, onVerseSelect = { viewModel.selectVerse(it); currentTab = AppTab.VERSES }, textColor = textColor, fontSize = fontSize, fontFamily = globalFontFamily)
                            AppTab.NOTES -> NotesView(viewModel, textColor)
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
                                                Text(text = stringResource(Res.string.copy_hint_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)), color = textColor)
                                                Text(text = stringResource(Res.string.copy_hint_subtitle), style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                } else {
                                    VersesView(
                                        viewModel = viewModel,
                                        detectedVerses = detectedVerses, 
                                        uniqueBooks = uniqueBooks, 
                                        textColor = textColor, 
                                        fontSize = fontSize, 
                                        fontFamily = globalFontFamily, 
                                        lineHeight = lineHeight, 
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
                            
                            // 1. Configurações
                            var isSettingsHovered by remember { mutableStateOf(false) }
                            Surface(color = if (currentTab == AppTab.SETTINGS || isSettingsHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isSettingsHovered = true }, onExit = { isSettingsHovered = false }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentTab = AppTab.SETTINGS }) {
                                Box(modifier = Modifier.padding(8.dp)) { SettingsIcon(color = if (currentTab == AppTab.SETTINGS || isSettingsHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f)) }
                            }

                            // 2. Sobre
                            var isAboutHovered by remember { mutableStateOf(false) }
                            Surface(color = if (currentTab == AppTab.ABOUT || isAboutHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isAboutHovered = true }, onExit = { isAboutHovered = false }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentTab = AppTab.ABOUT }) {
                                Box(modifier = Modifier.padding(8.dp)) { BibleIcon(color = if (currentTab == AppTab.ABOUT || isAboutHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f)) }
                            }

                            // 3. Histórico
                            var isHistoryHovered by remember { mutableStateOf(false) }
                            Surface(color = if (currentTab == AppTab.HISTORY || isHistoryHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isHistoryHovered = true }, onExit = { isHistoryHovered = false }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentTab = AppTab.HISTORY; viewModel.refreshHistory() }) {
                                Box(modifier = Modifier.padding(8.dp)) { HistoryIcon(color = if (currentTab == AppTab.HISTORY || isHistoryHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f)) }
                            }

                            // 4. Notas
                            var isNotesHovered by remember { mutableStateOf(false) }
                            Surface(color = if (currentTab == AppTab.NOTES || isNotesHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isNotesHovered = true }, onExit = { isNotesHovered = false }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentTab = AppTab.NOTES }) {
                                Box(modifier = Modifier.padding(8.dp)) { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = if (currentTab == AppTab.NOTES || isNotesHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                            }

                            // 5. Pesquisa
                            var isSearchHovered by remember { mutableStateOf(false) }
                            Surface(color = if (currentTab == AppTab.SEARCH || isSearchHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isSearchHovered = true }, onExit = { isSearchHovered = false }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { currentTab = AppTab.SEARCH }) {
                                Box(modifier = Modifier.padding(8.dp)) { SearchIcon(color = if (currentTab == AppTab.SEARCH || isSearchHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f)) }
                            }
                        }
                        
                        val showCamera = ((currentTab == AppTab.VERSES && detectedVerses.isNotEmpty()) || isNoteEditorOpen) && showSnapshotAction
                        val showCopy = currentTab == AppTab.VERSES && detectedVerses.isNotEmpty() && !isNoteEditorOpen

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showCamera) {
                                var isCameraHovered by remember { mutableStateOf(false) }
                                Surface(
                                    color = if (isCameraHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, 
                                    shape = RoundedCornerShape(8.dp), 
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isCameraHovered = true }, onExit = { isCameraHovered = false })
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                                            if (isNoteEditorOpen) viewModel.captureNoteSnapshot() else viewModel.captureSnapshot()
                                        }
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) { Icon(imageVector = FeatherIcons.Camera, contentDescription = stringResource(Res.string.save_image), tint = if (isCameraHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                                }
                            }
                            
                            if (showCopy) {
                                Spacer(modifier = Modifier.width(4.dp))
                                var isCopyHovered by remember { mutableStateOf(false) }
                                
                                val copiedLabel = stringResource(Res.string.copied)
                                Surface(
                                    color = if (isCopyHovered) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent, 
                                    shape = RoundedCornerShape(8.dp), 
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).pointerHoverIconHand().onHover(onEnter = { isCopyHovered = true }, onExit = { isCopyHovered = false })
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                                            val text = viewModel.formatVersesForClipboard(detectedVerses)
                                            clipboard.setText(AnnotatedString(text))
                                            viewModel.showToast("$copiedLabel ${viewModel.getConsolidatedReference(detectedVerses)}")
                                        }
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) { 
                                        if (toastState != null && toastState!!.message.startsWith(copiedLabel)) CheckIcon(color = VerseColors.SuccessGreen) 
                                        else CopyIcon(color = if (isCopyHovered) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.6f)) 
                                    }
                                }
                            }
                        }
                    }
                }

                // Toast Feedback Floating Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = toastState != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
                ) {
                    val backgroundColor = when (toastState?.type) {
                        VerseViewModel.ToastType.ERROR -> VerseColors.ErrorRed
                        VerseViewModel.ToastType.INFO -> VerseColors.PrimaryAmber
                        else -> VerseColors.SuccessGreen
                    }
                    
                    Surface(
                        color = backgroundColor.copy(alpha = 0.9f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = toastState?.message ?: "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Error Overlay
                errorState?.let { uiError ->
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { viewModel.clearError() }, contentAlignment = Alignment.Center) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp, modifier = Modifier.padding(32.dp).clickable(enabled = false) { }) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = VerseColors.ErrorRed, modifier = Modifier.size(32.dp))
                                Text(text = stringResource(Res.string.error_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                
                                val errorMessage = stringResource(uiError.resource, *uiError.args.toTypedArray())
                                Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(color = VerseColors.PrimaryAmber, shape = RoundedCornerShape(8.dp), modifier = Modifier.pointerHoverIconHand().clickable { viewModel.clearError() }) {
                                    Text(text = stringResource(Res.string.understood), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = VerseColors.HeaderContentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
