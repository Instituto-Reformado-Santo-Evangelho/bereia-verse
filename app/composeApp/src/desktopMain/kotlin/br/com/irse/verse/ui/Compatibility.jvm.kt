package br.com.irse.verse.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.ExperimentalComposeUiApi

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun Modifier.pointerHoverIconHand(): Modifier = this.pointerHoverIcon(PointerIcon.Hand)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onHover(onEnter: () -> Unit, onExit: () -> Unit): Modifier = this
    .onPointerEvent(PointerEventType.Enter) { onEnter() }
    .onPointerEvent(PointerEventType.Exit) { onExit() }

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun copyToClipboard(clipboard: Clipboard, text: String) {
    try {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback para o clipboard do Compose se o AWT falhar (raro em Desktop)
        clipboard.setClipEntry(ClipEntry(AnnotatedString(text)))
    }
}
