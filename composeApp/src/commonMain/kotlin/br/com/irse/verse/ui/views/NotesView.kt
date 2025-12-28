package br.com.irse.verse.ui.views

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.Note
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera

@Composable
fun NotesView(
    viewModel: VerseViewModel,
    textColor: Color
) {
    val notes by viewModel.notes.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val fontFamilyName by viewModel.fontFamily.collectAsState()
    
    val clipboard = LocalClipboardManager.current

    val fontFamily = when (fontFamilyName) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(48.dp), tint = textColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Nenhuma anotação", color = textColor.copy(alpha = 0.3f))
            }
        }
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val reference = remember(note.verseId) {
                        note.verseId?.let { viewModel.getVerseReference(it) } ?: "Nota Livre"
                    }
                    NoteItem(
                        note = note,
                        reference = reference,
                        textColor = textColor,
                        fontFamily = fontFamily,
                        onEdit = { viewModel.openNoteEditor(note = note) },
                        onDelete = { viewModel.deleteNote(note.id) },
                        onCopy = { clipboard.setText(AnnotatedString(note.content)) },
                        onSnapshot = { viewModel.captureNoteSnapshot(note = note) }
                    )
                }
            }
        }

        SmallFloatingActionButton(
            onClick = { viewModel.openNoteEditor() },
            containerColor = VerseColors.PrimaryAmber,
            contentColor = VerseColors.HeaderContentColor,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).pointerHoverIconHand()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }

        AnimatedVisibility(
            visible = isNoteEditorOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val reference = remember(editingNote) {
                editingNote?.verseId?.let { viewModel.getVerseReference(it) } ?: "Nota Livre"
            }
            InlineNoteEditor(
                initialText = editingNote?.content ?: "",
                reference = reference,
                textColor = textColor,
                fontFamily = fontFamily,
                onSave = { content ->
                    viewModel.saveNote(editingNote?.verseId, content)
                    viewModel.closeNoteEditor()
                },
                onDismiss = { viewModel.closeNoteEditor() }
            )
        }
    }
}

@Composable
fun NoteItem(
    note: Note, 
    reference: String, 
    textColor: Color, 
    fontFamily: FontFamily,
    onEdit: () -> Unit, 
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onSnapshot: () -> Unit
) {
    Surface(
        color = textColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.titleSmall,
                    color = VerseColors.PrimaryAmber,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onSnapshot, modifier = Modifier.size(32.dp)) { Icon(imageVector = compose.icons.FeatherIcons.Camera, contentDescription = null, modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = VerseColors.ErrorRed.copy(alpha = 0.4f)) }
                }
            }
            Text(text = note.content, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily), color = textColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InlineNoteEditor(
    initialText: String,
    reference: String,
    textColor: Color,
    fontFamily: FontFamily,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember(initialText) { 
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length))) 
    }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .animateContentSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) { detectTapGestures { } } 
        ) {
            HorizontalDivider(thickness = 1.dp, color = VerseColors.PrimaryAmber.copy(alpha = 0.2f))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reference.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = VerseColors.PrimaryAmber, letterSpacing = 1.5.sp)
                Row {
                    IconButton(
                        onClick = { onSave(textFieldValue.text) }, 
                        enabled = textFieldValue.text.isNotBlank(),
                        modifier = Modifier.focusProperties { canFocus = false }
                    ) { 
                        Icon(Icons.Default.Check, contentDescription = null, tint = if(textFieldValue.text.isNotBlank()) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.2f)) 
                    } 
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.focusProperties { canFocus = false }
                    ) { 
                        Icon(Icons.Default.Close, contentDescription = null, tint = textColor.copy(alpha = 0.4f)) 
                    } 
                }
            }

            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Tab) {
                            if (event.type == KeyEventType.KeyDown) {
                                val currentText = textFieldValue.text
                                val selection = textFieldValue.selection
                                val newText = currentText.replaceRange(selection.start, selection.end, "\t")
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selection.start + 1)
                                )
                            }
                            true 
                        } else {
                            false
                        }
                    },
                placeholder = { Text("Digite sua reflexão...", color = textColor.copy(alpha = 0.2f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = VerseColors.PrimaryAmber
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp, fontFamily = fontFamily)
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}