package br.com.irse.verse.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

expect fun Modifier.pointerHoverIconHand(): Modifier

expect fun Modifier.onHover(onEnter: () -> Unit, onExit: () -> Unit): Modifier

expect suspend fun copyToClipboard(clipboard: Clipboard, text: String)
