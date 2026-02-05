package br.com.irse.verse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.HtmlTextFormatter
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.core.VerseRequest
import org.jetbrains.compose.resources.painterResource
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo
import verse.composeapp.generated.resources.bg1
import verse.composeapp.generated.resources.bg2
import verse.composeapp.generated.resources.bg3
import verse.composeapp.generated.resources.bg4
import verse.composeapp.generated.resources.bg5
import verse.composeapp.generated.resources.bg6
import verse.composeapp.generated.resources.note_bg_5
import verse.composeapp.generated.resources.note_bg_6
import verse.composeapp.generated.resources.note_bg_8
import verse.composeapp.generated.resources.note_bg_9
import verse.composeapp.generated.resources.note_bg_10
import verse.composeapp.generated.resources.note_bg_11
import verse.composeapp.generated.resources.note_bg_12
import verse.composeapp.generated.resources.note_bg_13
import verse.composeapp.generated.resources.note_bg_14
import verse.composeapp.generated.resources.note_bg_15
import verse.composeapp.generated.resources.note_bg_16
import verse.composeapp.generated.resources.note_bg_17

@Composable
fun getSnapshotBackground(imageName: String): Painter? {
    val resource = when (imageName) {
        "bg1.png" -> Res.drawable.bg1
        "bg2.png" -> Res.drawable.bg2
        "bg3.png" -> Res.drawable.bg3
        "bg4.png" -> Res.drawable.bg4
        "bg5.png" -> Res.drawable.bg5
        "bg6.png" -> Res.drawable.bg6
        "note_bg_5.png" -> Res.drawable.note_bg_5
        "note_bg_6.png" -> Res.drawable.note_bg_6
        "note_bg_8.png" -> Res.drawable.note_bg_8
        "note_bg_9.png" -> Res.drawable.note_bg_9
        "note_bg_10.png" -> Res.drawable.note_bg_10
        "note_bg_11.png" -> Res.drawable.note_bg_11
        "note_bg_12.png" -> Res.drawable.note_bg_12
        "note_bg_13.png" -> Res.drawable.note_bg_13
        "note_bg_14.png" -> Res.drawable.note_bg_14
        "note_bg_15.png" -> Res.drawable.note_bg_15
        "note_bg_16.png" -> Res.drawable.note_bg_16
        "note_bg_17.png" -> Res.drawable.note_bg_17
        else -> null
    }
    return if (resource != null) painterResource(resource) else null
}

@Composable
fun NoteSnapshotLayout(
    content: String,
    reference: String?,
    signature: String?,
    template: VerseViewModel.SnapshotTemplate,
    isPreview: Boolean = false
) {
    val fontFamily = when (template.fontFamilyName) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    
    // Ajuste de escala para preview
    val scaleFactor = if (isPreview) 0.5f else 1.0f
    
    // Cálculo de fonte base (mesma lógica do SnapshotHandler)
    val baseFontSize = calculateNoteFontSize(content.length)
    val fontSize = baseFontSize * scaleFactor
    
    val padding = if (isPreview) 16.dp else 50.dp
    val spacingRef = if (isPreview) 8.dp else 24.dp
    val spacingSig = if (isPreview) 12.dp else 32.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagem de Fundo
        template.backgroundImage?.let { imageName ->
            getSnapshotBackground(imageName)?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(template.imageAlpha),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Logo de Fundo (se ativado e sem imagem custom)
        if (template.useLogoBackground && template.backgroundImage == null) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.6f),
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradiente sobre a imagem
        Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))

        // Conteúdo
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            if (!reference.isNullOrBlank()) {
                Text(
                    text = reference.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp * scaleFactor,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp * scaleFactor
                    ),
                    color = template.contentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(spacingRef))
            }

            Text(
                text = content,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.25f,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = template.contentColor,
                textAlign = TextAlign.Start
            )

            if (!signature.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacingSig))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            thickness = 1.dp * scaleFactor,
                            color = template.contentColor.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp * scaleFactor))
                        Text(
                            text = signature,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp * scaleFactor,
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal
                            ),
                            color = template.contentColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnapshotLayout(
    verses: List<Pair<VerseRequest, String?>>,
    template: VerseViewModel.SnapshotTemplate,
    isPreview: Boolean = false
) {
    val fontFamily = when (template.fontFamilyName) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    // Calcula texto total e número de versículos
    val totalTextLength = verses.sumOf { it.second?.length ?: 0 }
    val verseCount = verses.size
    
    val scaleFactor = if (isPreview) 0.5f else 1.0f
    
    val baseFontSize = calculateFontSize(totalTextLength, verseCount)
    val fontSize = baseFontSize * scaleFactor
    
    val padding = if (isPreview) 16.dp else 50.dp
    val topLogoSize = if (isPreview) 30.dp else 80.dp
    val contentPaddingV = if (template.showLogo) (if (isPreview) 20.dp else 60.dp) else (if (isPreview) 10.dp else 40.dp)
    val footerWidth = if (isPreview) 20.dp else 60.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagem de Fundo Custom
        template.backgroundImage?.let { imageName ->
            getSnapshotBackground(imageName)?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(template.imageAlpha),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Logo de Fundo (se ativado e sem imagem custom)
        if (template.useLogoBackground && template.backgroundImage == null) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.5f),
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradiente sobre a imagem
        Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Logo no Topo
            if (template.showLogo) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopCenter).size(topLogoSize),
                    alpha = template.logoAlpha
                )
            }

            // Conteúdo
            Column(
                modifier = Modifier.align(Alignment.Center).padding(vertical = contentPaddingV),
                horizontalAlignment = if (template.textAlignment == TextAlign.Start) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Texto dos Versículos
                val fullText = verses.joinToString(" ") { (_, content) ->
                    HtmlTextFormatter.format(content ?: "").toString().trim()
                }
                
                if (fullText.isNotBlank()) {
                    Text(
                        text = fullText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.25f,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp * scaleFactor
                        ),
                        color = template.contentColor,
                        textAlign = template.textAlignment,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(if (isPreview) 10.dp else 32.dp))

                // Referências
                if (verses.isNotEmpty()) {
                    val groupedRefs = verses.map { it.first }.groupBy { it.book }
                    val refText = groupedRefs.entries.joinToString("; ") { (book, reqs) ->
                        val chapters = reqs.groupBy { it.chapter }
                        "$book " + chapters.entries.joinToString(", ") { (chap, vReqs) ->
                            if (vReqs.size == 1) "$chap:${vReqs.first().verse}"
                            else "$chap:${vReqs.first().verse}-${vReqs.last().verse}"
                        }
                    }

                    Text(
                        text = refText.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 16.sp * scaleFactor,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp * scaleFactor
                        ),
                        color = template.contentColor.copy(alpha = 0.75f),
                        textAlign = template.textAlignment
                    )
                }
            }

            // Rodapé
            if (template.showFooter) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                     HorizontalDivider(
                         modifier = Modifier.width(footerWidth), 
                         thickness = 0.5.dp * scaleFactor, 
                         color = template.contentColor.copy(alpha = 0.15f)
                     )
                     Spacer(modifier = Modifier.height(if (isPreview) 4.dp else 12.dp))
                     Text(
                         text = "IRSE | Bereia Verse",
                         style = MaterialTheme.typography.labelSmall.copy(
                             fontWeight = FontWeight.ExtraLight, 
                             fontSize = 10.sp * scaleFactor,
                             letterSpacing = 2.sp * scaleFactor
                         ),
                         color = template.contentColor.copy(alpha = 0.3f)
                     )
                }
            }
        }
    }
}

// Funções auxiliares de cálculo de fonte (trazidas do JvmSnapshotHandler)
private fun calculateFontSize(textLength: Int, verseCount: Int): androidx.compose.ui.unit.TextUnit {
    val maxCharsPerVerse = 400
    val totalMaxChars = maxCharsPerVerse * verseCount.coerceAtMost(3)
    
    return when {
        textLength > totalMaxChars -> 18.sp
        verseCount == 1 && textLength < 150 -> 38.sp
        verseCount == 1 && textLength < 300 -> 32.sp
        verseCount == 2 && textLength < 400 -> 28.sp
        verseCount == 3 && textLength < 600 -> 24.sp
        textLength < 400 -> 28.sp
        textLength < 700 -> 24.sp
        textLength < 1000 -> 22.sp
        else -> 20.sp
    }
}

private fun calculateNoteFontSize(textLength: Int): androidx.compose.ui.unit.TextUnit {
    val maxChars = 600
    return when {
        textLength > maxChars -> 18.sp
        textLength < 150 -> 34.sp
        textLength < 300 -> 28.sp
        textLength < 500 -> 24.sp
        else -> 22.sp
    }
}