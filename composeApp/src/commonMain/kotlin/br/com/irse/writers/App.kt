package br.com.irse.writers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

// Custom Colors
val PrimaryOrange = Color(0xFFFFAE00)
val BackgroundGray = Color(0xFFF5F7FA)

// --- Custom Icons ---
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
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(x = 8.dp.toPx(), y = 8.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(width = 14.dp.toPx(), height = 14.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Fill
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
    onResize: (height: Dp) -> Unit = {},
    onForceRedraw: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    
    // Determina o livro atual para o subtítulo
    val currentBookName = detectedVerses.firstOrNull()?.first?.book ?: "Versículos"
    
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = PrimaryOrange,
            surface = Color.White
        )
    ) {
        var isCopied by remember { mutableStateOf(false) }
        
        // Dynamic Resize Logic
        LaunchedEffect(detectedVerses) {
            val baseHeight = 140.dp
            val itemHeight = 120.dp 
            
            val contentHeight = if (detectedVerses.isEmpty()) {
                250.dp
            } else {
                baseHeight + (itemHeight * detectedVerses.size)
            }
            
            val finalHeight = contentHeight.value.coerceIn(200f, 600f).dp
            onResize(finalHeight)
        }

        LaunchedEffect(detectedVerses) {
            isCopied = false
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp), 
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // --- Header REFORMULADO ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryOrange)
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Padding vertical reduzido para manter altura
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f) // Ocupa espaço disponível para o texto não empurrar o X
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp) // Levemente menor para alinhar
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        // Coluna de Texto
                        Column {
                            Text(
                                text = "Bereia Verse - IRSE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentBookName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // --- Loading Bar ---
                if (isProcessing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color(0xFFFFD180) // Laranja claro
                    )
                }

                // --- Content ---
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (detectedVerses.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Copie um texto (Ctrl+C)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ex: João 3:16",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(detectedVerses) { (req, content) ->
                                ContinuousVerseItem(req, content)
                            }
                        }
                    }
                }

                // --- Footer ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Almeida Corrigida Fiel (ACF)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
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
                         if (isCopied) {
                             CheckIcon(color = Color(0xFF2E7D32))
                         } else {
                             CopyIcon(color = Color.Gray)
                         }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousVerseItem(request: VerseRequest, content: String?) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(
            color = PrimaryOrange, // Usando Laranja
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp 
        )) {
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

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        lineHeight = 24.sp,
        color = Color(0xFF333333)
    )
}
