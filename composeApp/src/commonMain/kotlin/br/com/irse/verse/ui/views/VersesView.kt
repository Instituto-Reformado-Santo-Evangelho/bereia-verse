package br.com.irse.verse.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        // Área Superior: Expandir Anterior / Recolher Topo
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expandir (Topo)
                IconButton(
                    onClick = { onLoadContext(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = FeatherIcons.ChevronUp,
                        contentDescription = "Expandir Anterior",
                        modifier = Modifier.size(32.dp),
                        tint = PrimaryAmber.copy(alpha = 0.4f)
                    )
                }
                
                // Recolher (Topo) - Só aparece se houver contexto para recolher (size > 1)
                if (detectedVerses.size > 1) {
                    IconButton(
                        onClick = { onRemoveContext(-1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = FeatherIcons.Minus,
                            contentDescription = "Recolher Topo",
                            modifier = Modifier.size(24.dp),
                            tint = textColor.copy(alpha = 0.3f)
                        )
                    }
                }
            }
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

        // Área Inferior: Recolher Fundo / Expandir Próximo
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Expandir (Fundo) - Trocando a ordem para ficar alinhado: [Expandir] [Recolher]
                 // Para manter a simetria visual com o topo (Expandir sempre na ponta esquerda)
                IconButton(
                    onClick = { onLoadContext(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = FeatherIcons.ChevronDown,
                        contentDescription = "Expandir Próximo",
                        modifier = Modifier.size(32.dp),
                        tint = PrimaryAmber.copy(alpha = 0.4f)
                    )
                }

                // Recolher (Fundo)
                if (detectedVerses.size > 1) {
                    IconButton(
                        onClick = { onRemoveContext(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = FeatherIcons.Minus,
                            contentDescription = "Recolher Fundo",
                            modifier = Modifier.size(24.dp),
                            tint = textColor.copy(alpha = 0.3f)
                        )
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