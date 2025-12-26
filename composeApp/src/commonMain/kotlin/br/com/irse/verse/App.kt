package br.com.irse.verse

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import writers.composeapp.generated.resources.Res
import writers.composeapp.generated.resources.logo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Cores de Identidade
val PrimaryAmber = Color(0xFFFFC107)
val HeaderContentColor = Color(0xFF333333)

// Theme Colors
val LightSurface = Color.White
val DarkSurface = Color(0xFF1E1E1E)
val DarkText = Color(0xFFE0E0E0)
val DarkFooter = Color(0xFF2D2D2D)
val DarkBorder = Color(0xFF444444)

@Composable
fun CopyIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawRect(color = color.copy(alpha = 0.5f), topLeft = androidx.compose.ui.geometry.Offset(x = 3.dp.toPx(), y = 3.dp.toPx()), size = androidx.compose.ui.geometry.Size(width = 10.dp.toPx(), height = 10.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
        drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(x = 6.dp.toPx(), y = 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(width = 10.dp.toPx(), height = 10.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun HistoryIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = center, end = androidx.compose.ui.geometry.Offset(center.x, center.y - 5.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color = color, start = center, end = androidx.compose.ui.geometry.Offset(center.x + 3.dp.toPx(), center.y), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun SearchIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        drawCircle(color = color, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(7.dp.toPx(), 7.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx()), end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 16.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun CheckIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(4.dp.toPx(), 9.dp.toPx())
            lineTo(8.dp.toPx(), 13.dp.toPx())
            lineTo(15.dp.toPx(), 5.dp.toPx())
        }
        drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

enum class AppTab { VERSES, HISTORY, SEARCH }

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(
    viewModel: VerseViewModel,
    onClose: () -> Unit = {},
    onHeightRequest: (Dp) -> Unit = {}
) {
    val detectedVerses by viewModel.detectedVerses.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val history by viewModel.history.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    var currentTab by remember { mutableStateOf(AppTab.VERSES) }
    
    val uniqueBooks = remember(detectedVerses) { detectedVerses.map { it.first.book }.distinct() }
    val titleDisplay = when (currentTab) {
        AppTab.HISTORY -> Strings.HISTORY_TAB
        AppTab.SEARCH -> Strings.SEARCH_TAB
        AppTab.VERSES -> if (uniqueBooks.size == 1) uniqueBooks.first() else if (uniqueBooks.isEmpty()) Strings.VERSES_TAB else "${uniqueBooks.size} ${Strings.BOOKS_DETECTED}"
    }
    
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) DarkSurface else LightSurface
    val textColor = if (isDark) DarkText else Color(0xFF333333)
    val footerColor = if (isDark) DarkFooter else Color(0xFFF5F5F5)
    val borderColor = if (isDark) DarkBorder else Color.LightGray

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme(primary = PrimaryAmber, surface = surfaceColor, onSurface = textColor)
        else lightColorScheme(primary = PrimaryAmber, surface = surfaceColor, onSurface = textColor)
    ) {
        var isCopied by remember { mutableStateOf(false) }
        
        LaunchedEffect(detectedVerses, currentTab) {
            isCopied = false
            val baseHeight = 160.dp
            val targetHeight = when(currentTab) {
                AppTab.SEARCH -> 450.dp
                AppTab.HISTORY -> (150.dp + (80.dp * history.size)).coerceIn(350.dp, 600.dp)
                AppTab.VERSES -> (150.dp + (130.dp * detectedVerses.size)).coerceIn(350.dp, 600.dp)
            }
            onHeightRequest(targetHeight)
        }

        Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp), color = surfaceColor, shadowElevation = 8.dp, border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(PrimaryAmber).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(painter = painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = Strings.APP_TITLE, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HeaderContentColor, fontSize = 16.sp)
                            Text(text = titleDisplay, style = MaterialTheme.typography.bodySmall, color = HeaderContentColor.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(HeaderContentColor.copy(alpha = 0.1f))
                            .clickable { if (currentTab != AppTab.VERSES) currentTab = AppTab.VERSES else onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (currentTab != AppTab.VERSES) Icons.Default.ArrowBack else Icons.Default.Close
                        Icon(icon, contentDescription = null, tint = HeaderContentColor, modifier = Modifier.size(20.dp))
                    }
                }

                if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = PrimaryAmber.copy(alpha = 0.5f))

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        AppTab.HISTORY -> HistoryView(
                            history = history,
                            onSelect = { 
                                viewModel.processQuery(it)
                                currentTab = AppTab.VERSES 
                            }, 
                            textColor = textColor
                        )
                        AppTab.SEARCH -> SearchView(
                            viewModel = viewModel,
                            onVerseSelect = { 
                                viewModel.selectVerse(it)
                                currentTab = AppTab.VERSES 
                            },
                            textColor = textColor
                        )
                        AppTab.VERSES -> {
                            if (detectedVerses.isEmpty()) {
                                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(Strings.COPY_HINT_TITLE, style = MaterialTheme.typography.titleMedium, color = textColor.copy(alpha = 0.7f))
                                    Text(Strings.COPY_HINT_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.4f))
                                }
                            } else {
                                VersesView(detectedVerses, uniqueBooks, textColor)
                            }
                        }
                    }
                }

                // Footer
                Row(modifier = Modifier.fillMaxWidth().background(footerColor).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 8.dp)) {
                        IconButton(onClick = { currentTab = AppTab.SEARCH }, modifier = Modifier.size(32.dp)) {
                            SearchIcon(color = if (currentTab == AppTab.SEARCH) PrimaryAmber else textColor.copy(alpha = 0.6f))
                        }
                        IconButton(onClick = { 
                            currentTab = AppTab.HISTORY
                            viewModel.refreshHistory()
                        }, modifier = Modifier.size(32.dp)) {
                            HistoryIcon(color = if (currentTab == AppTab.HISTORY) PrimaryAmber else textColor.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "ACF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                    }
                    
                    if (currentTab == AppTab.VERSES && detectedVerses.isNotEmpty()) {
                        IconButton(onClick = {
                            val fullText = detectedVerses.joinToString("\n\n") { (req, content) ->
                                val cleanContent = content?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                                "${cleanContent} (${req.book} ${req.chapter}:${req.verse} - ACF)"
                            }
                            clipboardManager.setText(AnnotatedString(fullText))
                            isCopied = true
                        }, modifier = Modifier.size(32.dp).padding(end = 8.dp)) {
                             if (isCopied) CheckIcon(color = Color(0xFF2E7D32)) else CopyIcon(color = textColor.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchView(viewModel: VerseViewModel, onVerseSelect: (Int) -> Unit, textColor: Color) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    
    var selectedIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    // Auto-scroll LazyColumn para acompanhar a seleção do teclado
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && results.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    // Reset selected index when results change
    LaunchedEffect(results) {
        selectedIndex = if (results.isNotEmpty()) 0 else -1
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text(Strings.SEARCH_HINT) },
            modifier = Modifier.fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionDown -> {
                                if (results.isNotEmpty()) {
                                    selectedIndex = (selectedIndex + 1).coerceAtMost(results.size - 1)
                                    true
                                } else false
                            }
                            Key.DirectionUp -> {
                                if (results.isNotEmpty()) {
                                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                    true
                                } else false
                            }
                            Key.Enter -> {
                                if (selectedIndex >= 0 && selectedIndex < results.size) {
                                    onVerseSelect(results[selectedIndex].id)
                                    focusManager.clearFocus()
                                } else if (query.isNotBlank()) {
                                    viewModel.processQuery(query)
                                    focusManager.clearFocus()
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAmber, focusedLabelColor = PrimaryAmber),
            trailingIcon = {
                IconButton(onClick = { 
                    if (query.isNotBlank()) {
                        viewModel.processQuery(query)
                        focusManager.clearFocus()
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryAmber)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (results.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                itemsIndexed(results) { index, res ->
                    val isSelected = index == selectedIndex
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                             onVerseSelect(res.id)
                             focusManager.clearFocus()
                        },
                        color = if (isSelected) PrimaryAmber.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (res.book.isNotEmpty()) {
                                Text(text = "${res.book} ${res.chapter}:${res.verse}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PrimaryAmber)
                            }
                            
                            val highlightedText = buildAnnotatedString {
                                val cleanContent = res.content.replace(Regex("<[^>]*>"), "")
                                val lowerContent = cleanContent.lowercase()
                                val lowerQuery = query.lowercase()
                                var start = 0
                                
                                if (lowerQuery.isNotBlank() && lowerContent.contains(lowerQuery)) {
                                    while (true) {
                                        val idx = lowerContent.indexOf(lowerQuery, start)
                                        if (idx == -1) {
                                            append(cleanContent.substring(start))
                                            break
                                        }
                                        append(cleanContent.substring(start, idx))
                                        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = PrimaryAmber)) {
                                            append(cleanContent.substring(idx, idx + query.length))
                                        }
                                        start = idx + query.length
                                    }
                                } else {
                                    append(cleanContent)
                                }
                            }
                            Text(text = highlightedText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = textColor)
                        }
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                }
            }
        } else if (query.length >= 3) {
             Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                 Text(Strings.NO_RESULTS, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.5f))
             }
        }
    }
}

@Composable
fun VersesView(detectedVerses: List<Pair<VerseRequest, String?>>, uniqueBooks: List<String>, textColor: Color) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        var lastBook = ""
        detectedVerses.forEach { (req, content) ->
            if (uniqueBooks.size > 1 && req.book != lastBook) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(text = req.book.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryAmber)
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = PrimaryAmber.copy(alpha = 0.3f))
                    }
                }
                lastBook = req.book
            }
            item { ContinuousVerseItem(req, content, textColor) }
        }
    }
}

@Composable
fun HistoryView(history: List<HistoryEntry>, onSelect: (String) -> Unit, textColor: Color) {
    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(Strings.NO_HISTORY, color = textColor.copy(alpha = 0.5f)) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(history) { _, entry ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(entry.query) }, color = Color.Transparent, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = entry.query, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                        Text(text = dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.5f))
                    }
                }
                HorizontalDivider(color = textColor.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun ContinuousVerseItem(request: VerseRequest, content: String?, textColor: Color) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = PrimaryAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)) { append("${request.chapter}:${request.verse}  ") }
        if (content != null) {
            val formatted = remember(content) { HtmlTextFormatter.format(content) }
            append(formatted)
        } else {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Red)) { append(Strings.TEXT_NOT_AVAILABLE) }
        }
    }
    Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), lineHeight = 24.sp, color = textColor)
}