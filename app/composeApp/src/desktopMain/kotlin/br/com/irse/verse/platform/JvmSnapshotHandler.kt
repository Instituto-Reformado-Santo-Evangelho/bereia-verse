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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import br.com.irse.verse.ui.components.SnapshotLayout
import br.com.irse.verse.ui.components.NoteSnapshotLayout

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
}