package br.com.irse.verse.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.components.FireAnimation
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*

@Composable
fun SearchView(
    viewModel: VerseViewModel, 
    onVerseSelect: (Int) -> Unit, 
    textColor: Color, 
    fontSize: Int, 
    fontFamily: FontFamily
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val noteResults by viewModel.noteSearchResults.collectAsState()
    val searchScope by viewModel.searchScope.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    
    var selectedIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    val hasResults = if (searchScope == VerseViewModel.SearchScope.VERSES) results.isNotEmpty() else noteResults.isNotEmpty()
    
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && hasResults) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(results, noteResults) {
        selectedIndex = if (hasResults) 0 else -1
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(Res.string.search_hint), color = textColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily, color = textColor),
                modifier = Modifier.fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val maxIndex = if (searchScope == VerseViewModel.SearchScope.VERSES) results.size - 1 else noteResults.size - 1
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    if (hasResults) {
                                        selectedIndex = (selectedIndex + 1).coerceAtMost(maxIndex)
                                        true
                                    } else false
                                }
                                Key.DirectionUp -> {
                                    if (hasResults) {
                                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                        true
                                    } else false
                                }
                                Key.Enter -> {
                                    if (selectedIndex >= 0 && selectedIndex <= maxIndex) {
                                        if (searchScope == VerseViewModel.SearchScope.VERSES) {
                                            onVerseSelect(results[selectedIndex].id)
                                        } else {
                                            val note = noteResults[selectedIndex]
                                            viewModel.openNoteEditor(note = note)
                                        }
                                        focusManager.clearFocus()
                                    } else if (query.isNotBlank()) {
                                        viewModel.processQuery(query)
                                        focusManager.clearFocus()
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VerseColors.PrimaryAmber, 
                    unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                    focusedContainerColor = textColor.copy(alpha = 0.02f),
                    unfocusedContainerColor = textColor.copy(alpha = 0.02f)
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = VerseColors.PrimaryAmber)
                    }
                }
            )
            
            // Filtro de Escopo (Toast/Chips)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeChip(
                    text = stringResource(Res.string.search_scope_verses),
                    selected = searchScope == VerseViewModel.SearchScope.VERSES,
                    onClick = { viewModel.setSearchScope(VerseViewModel.SearchScope.VERSES) }
                )
                ScopeChip(
                    text = stringResource(Res.string.search_scope_notes),
                    selected = searchScope == VerseViewModel.SearchScope.NOTES,
                    onClick = { viewModel.setSearchScope(VerseViewModel.SearchScope.NOTES) }
                )
            }
        }

        if (hasResults) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp), 
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (searchScope == VerseViewModel.SearchScope.VERSES) {
                    itemsIndexed(results) { index, res ->
                        val isSelected = index == selectedIndex
                        VerseResultItem(res, isSelected, query, fontSize, fontFamily, lineHeight, textColor) {
                            onVerseSelect(res.id)
                            focusManager.clearFocus()
                        }
                    }
                } else {
                    itemsIndexed(noteResults) { index, note ->
                        val isSelected = index == selectedIndex
                        NoteResultItem(note, isSelected, query, fontSize, fontFamily, textColor, viewModel) {
                            viewModel.openNoteEditor(note = note)
                            focusManager.clearFocus()
                        }
                    }
                }
            }
        } else {
             Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                 if (query.length >= 2) {
                     Text(
                        stringResource(Res.string.no_results), 
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily), 
                        color = textColor.copy(alpha = 0.5f)
                    )
                 } else {
                     val showFireAnimation by viewModel.showFireAnimation.collectAsState()
                     if (showFireAnimation) {
                        FireAnimation(modifier = Modifier.fillMaxSize().alpha(0.5f))
                     } else {
                         Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                             Text(
                                text = stringResource(Res.string.search_examples_title), 
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        blurRadius = 8f
                                    )
                                ), 
                                color = textColor
                            )
                             Text(
                                 text = stringResource(Res.string.search_examples_subtitle), 
                                 style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily), 
                                 color = textColor.copy(alpha = 0.6f),
                                 textAlign = TextAlign.Center
                             )
                         }
                     }
                 }
             }
        }
    }
}

@Composable
fun ScopeChip(text: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
fun VerseResultItem(
    res: br.com.irse.verse.core.SearchResult,
    isSelected: Boolean,
    query: String,
    fontSize: Int,
    fontFamily: FontFamily,
    lineHeight: Float,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().pointerHoverIconHand().clickable(onClick = onClick),
        color = if (isSelected) VerseColors.PrimaryAmber.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (res.book.isNotEmpty()) {
                Text(
                    text = "${res.book} ${res.chapter}:${res.verse}", 
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = (fontSize - 2).sp, fontFamily = fontFamily), 
                    fontWeight = FontWeight.Bold, 
                    color = VerseColors.PrimaryAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            val highlightedText = buildAnnotatedString {
                val cleanContent = res.content.replace(Regex("<[^>]*>"), "")
                val lowerContent = cleanContent.lowercase()
                val lowerQuery = query.lowercase()
                var start = 0
                
                if (lowerQuery.isNotBlank() && lowerContent.contains(lowerQuery)) {
                    while (true) {
                        val idx = lowerContent.indexOf(lowerQuery, start)
                        if (idx == -1) { append(cleanContent.substring(start)); break }
                        append(cleanContent.substring(start, idx))
                        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = VerseColors.PrimaryAmber)) { append(cleanContent.substring(idx, idx + query.length)) }
                        start = idx + query.length
                    }
                } else { append(cleanContent) }
            }
            Text(
                text = highlightedText, 
                style = MaterialTheme.typography.bodySmall.copy(fontSize = (fontSize - 1).sp, fontFamily = fontFamily, lineHeight = ((fontSize - 1) * lineHeight).sp), 
                maxLines = 3, overflow = TextOverflow.Ellipsis, color = textColor
            )
        }
    }
    HorizontalDivider(color = textColor.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
fun NoteResultItem(
    note: br.com.irse.verse.core.Note,
    isSelected: Boolean,
    query: String,
    fontSize: Int,
    fontFamily: FontFamily,
    textColor: Color,
    viewModel: VerseViewModel,
    onClick: () -> Unit
) {
    val ref = note.verseId?.let { viewModel.getVerseReference(it) } ?: stringResource(Res.string.note_general)
    
    Surface(
        modifier = Modifier.fillMaxWidth().pointerHoverIconHand().clickable(onClick = onClick),
        color = if (isSelected) VerseColors.PrimaryAmber.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, tint = VerseColors.PrimaryAmber.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = ref, 
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = fontFamily), 
                    fontWeight = FontWeight.Bold, 
                    color = VerseColors.PrimaryAmber.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            val highlightedText = buildAnnotatedString {
                val content = note.content
                val lowerContent = content.lowercase()
                val lowerQuery = query.lowercase()
                var start = 0
                if (lowerQuery.isNotBlank() && lowerContent.contains(lowerQuery)) {
                    while (true) {
                        val idx = lowerContent.indexOf(lowerQuery, start)
                        if (idx == -1) { append(content.substring(start)); break }
                        append(content.substring(start, idx))
                        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = VerseColors.PrimaryAmber)) { append(content.substring(idx, idx + query.length)) }
                        start = idx + query.length
                    }
                } else { append(content) }
            }
            
            Text(
                text = highlightedText, 
                style = MaterialTheme.typography.bodySmall.copy(fontSize = (fontSize - 1).sp, fontFamily = fontFamily), 
                maxLines = 3, overflow = TextOverflow.Ellipsis, color = textColor
            )
        }
    }
    HorizontalDivider(color = textColor.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 8.dp))
}
