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
            val height = 1080 // Instagram Square default, mas pode ser ajustável se conteúdo for longo
            
            // Carregar logo (hacky way for JVM resource loading if painterResource fails off-screen)
            // Para simplificar, vamos tentar usar um texto "IRSE | Bereia Verse" se a imagem falhar,
            // ou assumir que o ImageComposeScene consegue renderizar o painterResource se o contexto estiver certo.
            // ImageComposeScene suporta composição completa.
            
            val scene = ImageComposeScene(
                width = width,
                height = height,
                density = Density(2f), // Alta densidade para qualidade (2x)
                coroutineContext = Dispatchers.Unconfined
            )

            scene.setContent {
                SnapshotLayout(verses, template)
            }

            val image = scene.render() // Renderiza para org.jetbrains.skia.Image
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: return@withContext

            // 2. Salvar Arquivo (Dialog na Thread da UI/Main, mas FileDialog do AWT bloqueia, então cuidado)
            // AWT deve rodar na thread dispatch, mas FileDialog é nativo.
            // Vamos lançar o diálogo.
            
            val fileName = "verse_snapshot_${System.currentTimeMillis()}.png"
            
            // Usando AWT FileDialog (Nativo do SO)
            // Em Linux/Mac/Windows isso abre a janela nativa
            val dialog = FileDialog(null as Frame?, "Salvar Imagem", FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            
            if (dialog.directory != null && dialog.file != null) {
                val file = File(dialog.directory, dialog.file)
                file.writeBytes(data.bytes)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(template.backgroundBrush)
                .padding(64.dp) // Margem generosa
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Conteúdo dos Versículos
                verses.forEach { (req, content) ->
                    if (content != null) {
                        val cleanText = HtmlTextFormatter.format(content).toString()
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 32.sp,
                                lineHeight = 44.sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = template.contentColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Referência
                if (verses.isNotEmpty()) {
                    val first = verses.first().first
                    val last = verses.last().first
                    val refText = if (verses.size == 1) {
                        "${first.book} ${first.chapter}:${first.verse}"
                    } else if (first.book == last.book) {
                        "${first.book} ${first.chapter}:${first.verse}-${last.verse}"
                    } else {
                        "${first.book} ${first.chapter}:${first.verse}..."
                    }

                    Text(
                        text = refText.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = template.contentColor.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "ACF (Almeida Corrigida Fiel)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Normal
                        ),
                        color = template.contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Rodapé / Branding
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 // Linha decorativa
                 HorizontalDivider(
                     modifier = Modifier.width(60.dp), 
                     thickness = 2.dp, 
                     color = template.contentColor.copy(alpha = 0.3f)
                 )
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(
                     text = "Bereia Verse",
                     style = MaterialTheme.typography.labelLarge.copy(
                         fontWeight = FontWeight.Bold,
                         fontSize = 16.sp
                     ),
                     color = template.contentColor.copy(alpha = 0.6f)
                 )
            }
        }
    }
}