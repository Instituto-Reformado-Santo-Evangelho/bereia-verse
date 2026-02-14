package br.com.irse.verse.ui.views

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import br.com.irse.verse.ui.copyToClipboard
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.BibleParser
import br.com.irse.verse.core.Note
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*

@Composable
fun NotesView(
    viewModel: VerseViewModel,
    textColor: Color
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val noteFilter by viewModel.noteFilter.collectAsState()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val editingVerseRequest by viewModel.editingVerseRequest.collectAsState()
    val viewingNote by viewModel.viewingNote.collectAsState()
    val noteToDelete by viewModel.noteToDelete.collectAsState()
    val showSnapshotAction by viewModel.showSnapshotAction.collectAsState()
    val fontFamilyName by viewModel.fontFamily.collectAsState()
    
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val fontFamily = when (fontFamilyName) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    
    // Delete Confirmation Dialog
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteNote() },
            title = { Text(stringResource(Res.string.delete_note_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.delete_note_confirm), color = textColor.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = { viewModel.performDeleteNote() }) {
                    Text(stringResource(Res.string.delete), color = VerseColors.ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteNote() }) {
                    Text(stringResource(Res.string.cancel), color = textColor.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (viewingNote != null) {
            // MODO VISUALIZAÇÃO (LEITURA)
            val noteFreeLabel = stringResource(Res.string.note_free)
            val reference = remember(viewingNote) {
                viewingNote?.verseId?.let { viewModel.getVerseReference(it) } ?: noteFreeLabel
            }
            NoteViewer(
                note = viewingNote!!,
                reference = reference,
                textColor = textColor,
                fontFamily = fontFamily,
                parser = viewModel.parser,
                onClose = { viewModel.closeNoteViewer() },
                onEdit = { viewModel.openNoteEditor(note = viewingNote) },
                onDelete = { viewModel.confirmDeleteNote(viewingNote!!) }, // Passando callback de delete
                onLinkClick = { linkText -> viewModel.processQuery(linkText) }
            )
        } else {
            // MODO LISTA
            Column(modifier = Modifier.fillMaxSize()) {
                // Filtros
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteFilterChip(
                        text = stringResource(Res.string.filter_all),
                        selected = noteFilter == VerseViewModel.NoteFilter.ALL,
                        onClick = { viewModel.setNoteFilter(VerseViewModel.NoteFilter.ALL) }
                    )
                    NoteFilterChip(
                        text = stringResource(Res.string.filter_free),
                        selected = noteFilter == VerseViewModel.NoteFilter.FREE,
                        onClick = { viewModel.setNoteFilter(VerseViewModel.NoteFilter.FREE) }
                    )
                    NoteFilterChip(
                        text = stringResource(Res.string.filter_verses),
                        selected = noteFilter == VerseViewModel.NoteFilter.VERSE,
                        onClick = { viewModel.setNoteFilter(VerseViewModel.NoteFilter.VERSE) }
                    )
                }

                if (notes.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(FeatherIcons.FilePlus, contentDescription = null, modifier = Modifier.size(48.dp), tint = textColor.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(Res.string.no_notes_found), color = textColor.copy(alpha = 0.3f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val noteFreeLabel = stringResource(Res.string.note_free)
                            val noteCopiedLabel = stringResource(Res.string.note_copied)
                            val reference = remember(note.verseId) {
                                note.verseId?.let { viewModel.getVerseReference(it) } ?: noteFreeLabel
                            }
                            NoteItem(
                                note = note,
                                reference = reference,
                                textColor = textColor,
                                fontFamily = fontFamily,
                                parser = viewModel.parser,
                                showSnapshotAction = showSnapshotAction,
                                onEdit = { viewModel.openNoteEditor(note = note) },
                                onView = { viewModel.openNoteViewer(note) },
                                onCopy = { 
                                    scope.launch { copyToClipboard(clipboard, note.content) }
                                    viewModel.showToast(noteCopiedLabel) 
                                },
                                onSnapshot = { viewModel.captureNoteSnapshot(note = note) },
                                onLinkClick = { linkText -> 
                                    viewModel.processQuery(linkText)
                                }
                            )
                        }
                    }
                }
            }

            SmallFloatingActionButton(
                onClick = { viewModel.openNoteEditor() },
                containerColor = VerseColors.PrimaryAmber,
                contentColor = VerseColors.HeaderContentColor,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).pointerHoverIconHand()
            ) {
                Icon(FeatherIcons.Plus, contentDescription = null)
            }
        }

        AnimatedVisibility(
            visible = isNoteEditorOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val noteFreeLabel = stringResource(Res.string.note_free)
            val reference = if (editingNote != null) {
                editingNote!!.verseId?.let { viewModel.getVerseReference(it) } ?: noteFreeLabel
            } else {
                editingVerseRequest?.let { "${it.book} ${it.chapter}:${it.verse}" } ?: noteFreeLabel
            }
            
            InlineNoteEditor(
                initialText = editingNote?.content ?: "",
                reference = reference,
                textColor = textColor,
                fontFamily = fontFamily,
                onSave = { content ->
                    viewModel.saveNote(content)
                    viewModel.closeNoteEditor()
                },
                onDismiss = { viewModel.closeNoteEditor() }
            )
        }
    }
}

@Composable
fun NoteViewer(
    note: Note,
    reference: String,
    textColor: Color,
    fontFamily: FontFamily,
    parser: BibleParser,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalDivider(thickness = 1.dp, color = VerseColors.PrimaryAmber.copy(alpha = 0.2f))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reference.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = VerseColors.PrimaryAmber,
                letterSpacing = 1.5.sp
            )
            Row {
                IconButton(onClick = onEdit) { 
                    Icon(FeatherIcons.Edit3, contentDescription = stringResource(Res.string.edit), tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) 
                }
                IconButton(onClick = onDelete) { 
                    Icon(FeatherIcons.Trash2, contentDescription = stringResource(Res.string.delete), tint = VerseColors.ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) 
                }
                IconButton(onClick = onClose) { 
                    Icon(FeatherIcons.X, contentDescription = stringResource(Res.string.close), tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) 
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            NoteSmartText(
                text = note.content,
                textColor = textColor,
                fontFamily = fontFamily,
                parser = parser,
                onLinkClick = onLinkClick,
                isViewerMode = true,
                linksEnabled = true
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
    parser: BibleParser,
    showSnapshotAction: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onCopy: () -> Unit,
    onSnapshot: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    Surface(
        color = textColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onView() }
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
                    IconButton(onClick = onView, modifier = Modifier.size(32.dp)) { 
                        Icon(imageVector = FeatherIcons.Eye, contentDescription = stringResource(Res.string.read), modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) 
                    }
                    
                    if (showSnapshotAction) {
                        IconButton(onClick = onSnapshot, modifier = Modifier.size(32.dp)) { 
                            Icon(imageVector = FeatherIcons.Camera, contentDescription = stringResource(Res.string.save_image), modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) 
                        }
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) { 
                        Icon(imageVector = FeatherIcons.Copy, contentDescription = null, modifier = Modifier.size(18.dp), tint = textColor.copy(alpha = 0.4f)) 
                    }
                }
            }
            
            NoteSmartText(
                text = note.content,
                textColor = textColor,
                fontFamily = fontFamily,
                parser = parser,
                onLinkClick = onLinkClick,
                isViewerMode = false,
                linksEnabled = false
            )
        }
    }
}

@Composable
fun NoteSmartText(
    text: String,
    textColor: Color,
    fontFamily: FontFamily,
    parser: BibleParser,
    onLinkClick: (String) -> Unit,
    isViewerMode: Boolean,
    linksEnabled: Boolean = true
) {
    val annotatedString = remember(text, isViewerMode, linksEnabled) {
        buildAnnotatedString {
            append(text)
            
            if (linksEnabled) {
                val matches = parser.refRegex.findAll(text)
                for (match in matches) {
                    val start = match.range.first
                    val end = match.range.last + 1
                    
                    addStyle(
                        style = SpanStyle(
                            color = VerseColors.PrimaryAmber,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isViewerMode) TextDecoration.None else TextDecoration.Underline
                        ),
                        start = start,
                        end = end
                    )
                    
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = "VERSE_LINK",
                            linkInteractionListener = { onLinkClick(match.value) }
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (isViewerMode) 18.sp else 14.sp,
            lineHeight = if (isViewerMode) 28.sp else 20.sp,
            fontFamily = fontFamily, 
            color = textColor
        ),
        maxLines = if (isViewerMode) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis
    )
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
                        Icon(FeatherIcons.Check, contentDescription = null, tint = if(textFieldValue.text.isNotBlank()) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.2f)) 
                    } 
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.focusProperties { canFocus = false }
                    ) { 
                        Icon(FeatherIcons.X, contentDescription = null, tint = textColor.copy(alpha = 0.4f)) 
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
                placeholder = { Text(stringResource(Res.string.note_placeholder), color = textColor.copy(alpha = 0.2f)) },
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

@Composable
fun NoteFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) VerseColors.PrimaryAmber.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) VerseColors.PrimaryAmber else Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.pointerHoverIconHand()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) VerseColors.PrimaryAmber else Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
