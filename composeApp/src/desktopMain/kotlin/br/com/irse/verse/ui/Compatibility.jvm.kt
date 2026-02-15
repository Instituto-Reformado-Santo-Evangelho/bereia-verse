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

actual fun Modifier.pointerHoverIconHand(): Modifier = this.pointerHoverIcon(PointerIcon.Hand)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onHover(onEnter: () -> Unit, onExit: () -> Unit): Modifier = this
    .onPointerEvent(PointerEventType.Enter) { onEnter() }
    .onPointerEvent(PointerEventType.Exit) { onExit() }

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun copyToClipboard(clipboard: Clipboard, text: String) {
    clipboard.setClipEntry(ClipEntry(AnnotatedString(text)))
}
