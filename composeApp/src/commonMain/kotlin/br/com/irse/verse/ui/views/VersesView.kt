package br.com.irse.verse.ui.views
import br.com.irse.verse.PrimaryAmber

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import br.com.irse.verse.PrimaryAmber
import br.com.irse.verse.core.HtmlTextFormatter
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseRequest
import br.com.irse.verse.ui.pointerHoverIconHand
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Minus

@Composable
fun VersesView(
    detectedVerses: List<Pair<VerseRequest, String?>>, 
    uniqueBooks: List<String>, 
    textColor: Color, 
    fontSize: Int = 16, 
    fontFamily: FontFamily = FontFamily.SansSerif,
    lineHeight: Float = 1.4f,
    onLoadContext: (Int) -> Unit = {},
    onRemoveContext: (Int) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp), 
        modifier = Modifier.fillMaxSize()
    ) {
        // Zona Superior (Anterior)
        item {
            ContextControlZone(
                isTop = true,
                canRemove = detectedVerses.size > 1,
                onExpand = { onLoadContext(-1) },
                onRemove = { onRemoveContext(-1) },
                iconColor = PrimaryAmber
            )
        }

        var lastBook = ""
        detectedVerses.forEach { (req, content) ->
            if (uniqueBooks.size > 1 && req.book != lastBook) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = req.book.uppercase(), 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = PrimaryAmber
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp), 
                            thickness = 1.dp, 
                            color = PrimaryAmber.copy(alpha = 0.3f)
                        )
                    }
                }
                lastBook = req.book
            }
            item { ContinuousVerseItem(req, content, textColor, fontSize, fontFamily, lineHeight) }
        }

        // Zona Inferior (Próximo)
        item {
             ContextControlZone(
                isTop = false,
                canRemove = detectedVerses.size > 1,
                onExpand = { onLoadContext(1) },
                onRemove = { onRemoveContext(1) },
                iconColor = PrimaryAmber
            )
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
                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp).clickable { onExpand() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(
                                imageVector = if (isTop) FeatherIcons.ChevronUp else FeatherIcons.ChevronDown,
                                contentDescription = "Expandir",
                                tint = iconColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Botão Recolher (se aplicável)
                    if (canRemove) {
                        Surface(
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp).clickable { onRemove() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                    imageVector = FeatherIcons.Minus,
                                    contentDescription = "Recolher",
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
}

@Composable
fun ContinuousVerseItem(
    request: VerseRequest, 
    content: String?, 
    textColor: Color, 
    fontSize: Int = 16, 
    fontFamily: FontFamily = FontFamily.SansSerif,
    lineHeight: Float = 1.4f
) {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = PrimaryAmber, 
                fontWeight = FontWeight.Bold, 
                fontSize = (fontSize - 2).sp
            )
        ) { 
            append("${request.chapter}:${request.verse}  ") 
        }
        
        if (content != null) {
            val formatted = remember(content) { HtmlTextFormatter.format(content) }
            append(formatted)
        } else {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Red)) { 
                append(Strings.TEXT_NOT_AVAILABLE) 
            }
        }
    }
    
    Text(
        text = annotatedString, 
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            fontFamily = fontFamily
        ), 
        lineHeight = (fontSize * lineHeight).sp, 
        color = textColor
    )
}