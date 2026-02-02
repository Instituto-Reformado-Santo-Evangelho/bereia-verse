package br.com.irse.verse.ui.views

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.*
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Minus
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*

@Composable
fun VersesView(
    viewModel: VerseViewModel,
    detectedVerses: List<Pair<VerseRequest, String?>>, 
    uniqueBooks: List<String>, 
    textColor: Color, 
    fontSize: Int = 16, 
    fontFamily: FontFamily = FontFamily.SansSerif,
    lineHeight: Float = 1.4f,
    onLoadContext: (Int) -> Unit = {},
    onRemoveContext: (Int) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val notes by viewModel.notes.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val editingVerseRequest by viewModel.editingVerseRequest.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    
    var previousLastId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(detectedVerses) {
        val currentLastId = detectedVerses.lastOrNull()?.first?.id
        // Auto-scroll suave apenas se o fim mudou (expansão inferior)
        if (previousLastId != null && currentLastId != previousLastId) {
            delay(100) // Pequeno delay para o Compose processar os novos itens no layout
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
        previousLastId = currentLastId
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp), 
            modifier = Modifier.fillMaxSize()
        ) {
            // Zona Superior (Anterior)
            item(key = "top_control") {
                ContextControlZone(
                    isTop = true, 
                    canRemove = detectedVerses.size > 1, 
                    onExpand = { onLoadContext(-1) }, 
                    onRemove = { onRemoveContext(-1) }, 
                    iconColor = VerseColors.PrimaryAmber
                )
            }

            var lastBook = ""
            detectedVerses.forEach { (req, content) ->
                if (uniqueBooks.size > 1 && req.book != lastBook) {
                    item(key = "header_${req.book}_${req.id}") {
                        Column {
                            Text(text = req.book.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = VerseColors.PrimaryAmber)
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = VerseColors.PrimaryAmber.copy(alpha = 0.3f))
                        }
                    }
                    lastBook = req.book
                }
                item(key = "verse_${req.id}") {
                    val note = notes.find { it.verseId == req.id }
                    ContinuousVerseItem(
                        request = req, content = content, textColor = textColor, fontSize = fontSize, fontFamily = fontFamily, lineHeight = lineHeight,
                        hasNote = note != null,
                        onNoteClick = {
                            viewModel.openNoteEditor(request = req, note = note)
                        }
                    )
                }
            }

            // Zona Inferior (Próximo)
            item(key = "bottom_control") {
                 ContextControlZone(
                    isTop = false, 
                    canRemove = detectedVerses.size > 1, 
                    onExpand = { onLoadContext(1) }, 
                    onRemove = { onRemoveContext(1) }, 
                    iconColor = VerseColors.PrimaryAmber
                )
            }
        }

        // Editor integrado persistente
        AnimatedVisibility(
            visible = isNoteEditorOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            editingVerseRequest?.let { req ->
                InlineNoteEditor(
                    initialText = editingNote?.content ?: "",
                    reference = "${req.book} ${req.chapter}:${req.verse}",
                    textColor = textColor,
                    fontFamily = fontFamily,
                    onSave = { content ->
                        viewModel.saveNote(content)
                        viewModel.closeNoteEditor()
                    },
                    onDismiss = { viewModel.closeNoteEditor() }
                )
            }
        }
    }
}

@Composable
fun ContextControlZone(
    isTop: Boolean,
    canRemove: Boolean,
    onExpand: () -> Unit,
    onRemove: () -> Unit,
    iconColor: Color
) {
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Zona de detecção restrita ao centro
        Box(
            modifier = Modifier
                .width(160.dp) // Apenas a área central detecta o mouse
                .fillMaxHeight()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> isHovered = true
                                PointerEventType.Exit -> isHovered = false
                            }
                        }
                    }
                }
                .pointerHoverIconHand(),
            contentAlignment = Alignment.Center
        ) {
            // Controles Ativos (Hover)
            AnimatedVisibility(
                visible = isHovered,
                enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
                exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Expandir
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isTop) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                            contentDescription = stringResource(Res.string.expand),
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Botão Recolher (se aplicável)
                    if (canRemove) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Minus,
                                contentDescription = stringResource(Res.string.collapse),
                                tint = iconColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousVerseItem(request: VerseRequest, content: String?, textColor: Color, fontSize: Int = 16, fontFamily: FontFamily = FontFamily.SansSerif, lineHeight: Float = 1.4f, hasNote: Boolean = false, onNoteClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = VerseColors.PrimaryAmber, fontWeight = FontWeight.Bold, fontSize = (fontSize - 2).sp)) { append("${request.chapter}:${request.verse}  ") }
                if (content != null) { append(remember(content) { HtmlTextFormatter.format(content) }) }
                else { withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Red)) { append(stringResource(Res.string.text_not_available)) } }
            }
            Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, fontFamily = fontFamily), lineHeight = (fontSize * lineHeight).sp, color = textColor)
        }
        IconButton(onClick = onNoteClick, modifier = Modifier.size(32.dp).pointerHoverIconHand()) {
            Icon(imageVector = if (hasNote) Icons.Default.EditNote else Icons.Default.AddComment, contentDescription = null, tint = if (hasNote) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
        }
    }
}