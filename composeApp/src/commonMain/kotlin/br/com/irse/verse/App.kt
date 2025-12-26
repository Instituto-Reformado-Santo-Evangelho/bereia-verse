package br.com.irse.verse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow

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
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRect(
            color = color.copy(alpha = 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(x = 4.dp.toPx(), y = 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(width = 14.dp.toPx(), height = 14.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x = 8.dp.toPx(), y = 8.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(width = 14.dp.toPx(), height = 14.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CheckIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(4.dp.toPx(), 12.dp.toPx())
            lineTo(9.dp.toPx(), 17.dp.toPx())
            lineTo(20.dp.toPx(), 6.dp.toPx())
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App(
    detectedVerses: List<Pair<VerseRequest, String?>>,
    isProcessing: Boolean,
    onClose: () -> Unit = {},
    onHeightRequest: (Dp) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    
    val uniqueBooks = remember(detectedVerses) { detectedVerses.map { it.first.book }.distinct() }
    val currentBookDisplay = when {
        uniqueBooks.isEmpty() -> "Versículos"
        uniqueBooks.size == 1 -> uniqueBooks.first()
        else -> "${uniqueBooks.size} Livros Detectados"
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
        
        // Cálculo de altura alvo (Header ~60dp + Footer ~50dp + Padding ~40dp + Itens)
        LaunchedEffect(detectedVerses) {
            isCopied = false
            val baseHeight = 150.dp
            val itemHeight = 130.dp 
            val targetHeight = if (detectedVerses.isEmpty()) 350.dp else baseHeight + (itemHeight * detectedVerses.size)
            onHeightRequest(targetHeight.value.coerceIn(350f, 600f).dp)
        }

        Surface(
            modifier = Modifier.fillMaxSize(), // Preenche a janela 100% para evitar vácuos no Hyprland
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryAmber)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(painter = painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Bereia Verse - IRSE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HeaderContentColor, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = currentBookDisplay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = HeaderContentColor.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(HeaderContentColor.copy(alpha = 0.1f)).clickable { onClose() }, contentAlignment = Alignment.Center) {
                        Text("✕", color = HeaderContentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = PrimaryAmber.copy(alpha = 0.5f))
                }

                // Conteúdo
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (detectedVerses.isEmpty()) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Copie um texto (Ctrl+C)", style = MaterialTheme.typography.titleMedium, color = textColor.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ex: João 3:16", style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.4f))
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            var lastBook = ""
                            detectedVerses.forEach { (req, content) ->
                                if (uniqueBooks.size > 1 && req.book != lastBook) {
                                    item {
                                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                            Text(text = req.book.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryAmber, letterSpacing = 1.sp)
                                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = PrimaryAmber.copy(alpha = 0.3f))
                                        }
                                    }
                                    lastBook = req.book
                                }
                                item {
                                    ContinuousVerseItem(req, content, textColor)
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(footerColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Almeida Corrigida Fiel (ACF)", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = {
                            if (detectedVerses.isNotEmpty()) {
                                val fullText = detectedVerses.joinToString("\n\n") { (req, content) ->
                                    val cleanContent = content?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                                    "$cleanContent (${req.book} ${req.chapter}:${req.verse} - ACF)"
                                }
                                clipboardManager.setText(AnnotatedString(fullText))
                                isCopied = true
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                         if (isCopied) CheckIcon(color = Color(0xFF2E7D32)) 
                         else CopyIcon(color = textColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousVerseItem(request: VerseRequest, content: String?, textColor: Color) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = PrimaryAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
            append("${request.chapter}:${request.verse}  ")
        }
        if (content != null) {
            val formatted = remember(content) { HtmlTextFormatter.format(content) }
            append(formatted)
        } else {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Red)) {
                append("Texto não disponível.")
            }
        }
    }
    Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), lineHeight = 24.sp, color = textColor)
}
