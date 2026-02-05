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
import compose.icons.feathericons.ChevronRight
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
    onRemoveContext: (Int) -> Boolean = { false }
) {
    val listState = rememberLazyListState()
    val notes by viewModel.notes.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val editingVerseRequest by viewModel.editingVerseRequest.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    
    var previousLastId by remember { mutableStateOf<Int?>(null) }
    var lastScrollEvent by remember { mutableStateOf(0L) }

    LaunchedEffect(detectedVerses) {
        val currentLastId = detectedVerses.lastOrNull()?.first?.id
        // Auto-scroll apenas se foi expansão incremental (size +1)
        // Se mudou muito (Navegação/Troca de livro), não rola p/ baixo, o LazyColumn já trata o reset p/ top
        if (previousLastId != null && currentLastId != previousLastId && 
            detectedVerses.size == (listState.layoutInfo.totalItemsCount - 2)) { // -2 accounts for top/bottom controls if present? No, layoutInfo is old state.
            // Better heuristic: if previous was not null and lastID > prevID and size increased slightly
             if (currentLastId!! > previousLastId!!) {
                 delay(100) 
                 listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
             }
        }
        previousLastId = currentLastId
    }

    Box(modifier = Modifier.fillMaxSize().pointerInput(uniqueBooks) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll && uniqueBooks.size <= 1) {
                    val delta = event.changes.first().scrollDelta
                    val now = System.currentTimeMillis()
                    if (now - lastScrollEvent > 200) { // Debounce 200ms
                         val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                         val isAtBottom = !listState.canScrollForward
                         
                         if (isAtTop && isAtBottom) { // Conteúdo cabe na tela (Pequeno)
                             if (delta.y < 0) { // Scroll Up (Pull Down)
                                 // Try collapse bottom (1) -> If fails (protected), expand top (-1)
                                 if (!onRemoveContext(1)) onLoadContext(-1)
                                 lastScrollEvent = now
                             } else if (delta.y > 0) { // Scroll Down (Push Up)
                                 // Try collapse top (-1) -> If fails (protected), expand bottom (1)
                                 if (!onRemoveContext(-1)) onLoadContext(1)
                                 lastScrollEvent = now
                             }
                         } else { // Lista longa (Scroll normal até bater na borda)
                             if (isAtTop && delta.y < 0) {
                                 if (!onRemoveContext(1)) onLoadContext(-1)
                                 lastScrollEvent = now
                             } else if (isAtBottom && delta.y > 0) {
                                 if (!onRemoveContext(-1)) onLoadContext(1)
                                 lastScrollEvent = now
                             }
                         }
                    }
                }
            }
        }
    }) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp), 
            modifier = Modifier.fillMaxSize()
        ) {
            var lastBook = ""
            detectedVerses.forEach { (req, content) ->
                if (uniqueBooks.size > 1) {
                    if (req.book != lastBook) {
                        item(key = "header_${req.book}_${req.id}") {
                            Column(modifier = Modifier.pointerHoverIconHand().clickable { viewModel.focusOnBook(req.book) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = req.book.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = VerseColors.PrimaryAmber)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(FeatherIcons.ChevronRight, contentDescription = null, tint = VerseColors.PrimaryAmber, modifier = Modifier.size(16.dp).offset(y = 1.dp))
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = VerseColors.PrimaryAmber.copy(alpha = 0.3f))
                            }
                        }
                        lastBook = req.book
                    }
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