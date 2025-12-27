package br.com.irse.verse.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.ui.text.AnnotatedString

actual fun Modifier.pointerHoverIconHand(): Modifier = this

actual fun Modifier.onHover(onEnter: () -> Unit, onExit: () -> Unit): Modifier = this

actual suspend fun copyToClipboard(clipboard: Clipboard, text: String) {
    // No Android, o ClipEntry do Compose Multiplatform 1.7+ envolve o ClipData
    val clipData = ClipData.newPlainText("Bible Verse", text)
    clipboard.setClipEntry(ClipEntry(clipData))
}
