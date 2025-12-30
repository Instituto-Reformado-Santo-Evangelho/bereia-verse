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
import androidx.compose.ui.text.font.FontFamily
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
            textLength < 100 -> 48.sp
            textLength < 300 -> 36.sp
            textLength < 600 -> 28.sp
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
                .background(template.backgroundBrush)
                .padding(80.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                if (!reference.isNullOrBlank()) {
                    Text(
                        text = reference.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = template.contentColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(32.dp))
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
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = signature,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 22.sp,
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
                .background(template.backgroundBrush)
                .padding(64.dp)
        ) {
            // Logo no Topo
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter).size(100.dp)
            )

            // Conteúdo Centralizado
            Column(
                modifier = Modifier.align(Alignment.Center).padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Versículos Agrupados
                verses.forEach { (_, content) ->
                    if (content != null) {
                        val cleanText = HtmlTextFormatter.format(content).toString()
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = fontSize,
                                lineHeight = fontSize * 1.4f,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = template.contentColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Referências Consolidadas no Rodapé
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = template.contentColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Rodapé (Linha + IRSE | Bereia Verse)
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 HorizontalDivider(
                     modifier = Modifier.width(80.dp), 
                     thickness = 2.dp, 
                     color = template.contentColor.copy(alpha = 0.3f)
                 )
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(
                     text = "IRSE | Bereia Verse",
                     style = MaterialTheme.typography.labelLarge.copy(
                         fontWeight = FontWeight.Bold,
                         fontSize = 18.sp,
                         letterSpacing = 1.sp
                     ),
                     color = template.contentColor.copy(alpha = 0.6f)
                 )
            }
        }
    }
}