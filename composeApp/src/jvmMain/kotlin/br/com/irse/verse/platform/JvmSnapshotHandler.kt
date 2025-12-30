package br.com.irse.verse.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.irse.verse.core.HtmlTextFormatter
import br.com.irse.verse.core.SnapshotHandler
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseRequest
import br.com.irse.verse.core.VerseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import androidx.compose.ui.ImageComposeScene
import org.jetbrains.compose.resources.painterResource
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo

class JvmSnapshotHandler : SnapshotHandler {

    override suspend fun captureAndSave(
        verses: List<Pair<VerseRequest, String?>>,
        template: VerseViewModel.SnapshotTemplate
    ) {
        withContext(Dispatchers.IO) {
            // 1. Renderizar Imagem
            val width = 1080
            val height = 1080
            
            val scene = ImageComposeScene(
                width = width,
                height = height,
                density = Density(2f),
                coroutineContext = Dispatchers.Unconfined
            )

            scene.setContent {
                SnapshotLayout(verses, template)
            }

            val image = scene.render()
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: return@withContext

            // 2. Salvar Arquivo
            val fileName = "verse_snapshot_${System.currentTimeMillis()}.png"
            val dialog = FileDialog(null as Frame?, "Salvar Imagem", FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            
            if (dialog.directory != null && dialog.file != null) {
                val file = File(dialog.directory, dialog.file)
                file.writeBytes(data.bytes)
            }
        }
    }

    override suspend fun captureNoteAndSave(
        content: String,
        reference: String?,
        signature: String?,
        template: VerseViewModel.SnapshotTemplate
    ) {
        withContext(Dispatchers.IO) {
            val width = 1080
            val height = 1080
            
            val scene = ImageComposeScene(
                width = width,
                height = height,
                density = Density(2f),
                coroutineContext = Dispatchers.Unconfined
            )

            scene.setContent {
                NoteSnapshotLayout(content, reference, signature, template)
            }

            val image = scene.render()
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: return@withContext

            val fileName = "note_snapshot_${System.currentTimeMillis()}.png"
            val dialog = FileDialog(null as Frame?, "Salvar Nota como Imagem", FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            
            if (dialog.directory != null && dialog.file != null) {
                val file = File(dialog.directory, dialog.file)
                file.writeBytes(data.bytes)
            }
        }
    }

    private fun calculateFontSize(textLength: Int): androidx.compose.ui.unit.TextUnit {
        return when {
            textLength < 150 -> 34.sp  // Reduzido de 48sp
            textLength < 400 -> 28.sp  // Reduzido de 36sp
            textLength < 800 -> 24.sp  // Reduzido de 28sp
            else -> 20.sp
        }
    }

    @Composable
    fun NoteSnapshotLayout(
        content: String,
        reference: String?,
        signature: String?,
        template: VerseViewModel.SnapshotTemplate
    ) {
        val fontFamily = when (template.fontFamilyName) {
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            "Cursive" -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }

        val fontSize = calculateFontSize(content.length)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (template.useLogoBackground) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Transparent)
        ) {
            // Imagem de Fundo (Se ativado)
            if (template.useLogoBackground) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.6f),
                    contentScale = ContentScale.Crop
                )
                // Scrim/Gradiente sobre a imagem
                Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))
            } else {
                // Fundo Padrão
                Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))
            }

            // Conteúdo (Padding aplicado aqui para não afetar o fundo)
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                if (template.showLogo) {
                    // Se tiver logo background, não mostra logo pequena no topo (regra visual implícita do Exclusivo, mas respeitando a flag)
                    // ... implementação futura se necessário, por enquanto Notes não tem logo no topo por padrão
                }

                if (!reference.isNullOrBlank()) {
                    Text(
                        text = reference.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp, // Levemente menor
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = template.contentColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = content,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.4f,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = template.contentColor,
                    textAlign = TextAlign.Start
                )

                if (!signature.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = signature,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontFamily = fontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light
                        ),
                        color = template.contentColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    @Composable
    fun SnapshotLayout(
        verses: List<Pair<VerseRequest, String?>>,
        template: VerseViewModel.SnapshotTemplate
    ) {
        val fontFamily = when (template.fontFamilyName) {
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            "Cursive" -> FontFamily.Cursive
            else -> FontFamily.SansSerif
        }

        // Calcula texto total para definir fonte
        val totalTextLength = verses.sumOf { it.second?.length ?: 0 }
        val fontSize = calculateFontSize(totalTextLength)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (template.useLogoBackground) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Transparent)
        ) {
            // Imagem de Fundo (Exclusivo)
            if (template.useLogoBackground) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.5f), // Reduzido para 0.5f para não competir
                    contentScale = ContentScale.Crop
                )
                // Scrim (Gradiente Revelador)
                Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(template.backgroundBrush))
            }

            Box(modifier = Modifier.fillMaxSize().padding(40.dp)) {
                // Logo no Topo
                if (template.showLogo) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.TopCenter).size(80.dp),
                        alpha = template.logoAlpha
                    )
                }

                // Conteúdo
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(vertical = 40.dp),
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
                                lineHeight = fontSize * 1.4f,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = template.contentColor,
                            textAlign = template.textAlignment,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                                fontSize = 14.sp, // Reduzido de 18.sp para 14.sp
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = template.contentColor.copy(alpha = 0.7f),
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
                         HorizontalDivider(modifier = Modifier.width(60.dp), thickness = 1.dp, color = template.contentColor.copy(alpha = 0.2f))
                         Spacer(modifier = Modifier.height(12.dp))
                         Text(
                             text = "IRSE | Bereia Verse",
                             style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                             color = template.contentColor.copy(alpha = 0.5f)
                         )
                    }
                }
            }
        }
    }
}