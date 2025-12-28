package br.com.irse.verse.ui.views

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
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.components.FireAnimation
import br.com.irse.verse.ui.pointerHoverIconHand
import br.com.irse.verse.ui.theme.VerseColors

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
    val lineHeight by viewModel.lineHeight.collectAsState()
    
    var selectedIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && results.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(results) {
        selectedIndex = if (results.isNotEmpty()) 0 else -1
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(Strings.SEARCH_HINT, color = textColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily, color = textColor),
                modifier = Modifier.fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    if (results.isNotEmpty()) {
                                        selectedIndex = (selectedIndex + 1).coerceAtMost(results.size - 1)
                                        true
                                    } else false
                                }
                                Key.DirectionUp -> {
                                    if (results.isNotEmpty()) {
                                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                        true
                                    } else false
                                }
                                Key.Enter -> {
                                    if (selectedIndex >= 0 && selectedIndex < results.size) {
                                        onVerseSelect(results[selectedIndex].id)
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
        }

        if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp), 
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(results) { index, res ->
                    val isSelected = index == selectedIndex
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .pointerHoverIconHand()
                            .clickable {
                                onVerseSelect(res.id)
                                focusManager.clearFocus()
                            },
                        color = if (isSelected) VerseColors.PrimaryAmber.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (res.book.isNotEmpty()) {
                                Text(
                                    text = "${res.book} ${res.chapter}:${res.verse}", 
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = (fontSize - 2).sp, 
                                        fontFamily = fontFamily
                                    ), 
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
                                        if (idx == -1) {
                                            append(cleanContent.substring(start))
                                            break
                                        }
                                        append(cleanContent.substring(start, idx))
                                        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = VerseColors.PrimaryAmber)) {
                                            append(cleanContent.substring(idx, idx + query.length))
                                        }
                                        start = idx + query.length
                                    }
                                } else {
                                    append(cleanContent)
                                }
                            }
                            Text(
                                text = highlightedText, 
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (fontSize - 1).sp, 
                                    fontFamily = fontFamily,
                                    lineHeight = ((fontSize - 1) * lineHeight).sp
                                ), 
                                maxLines = 3, 
                                overflow = TextOverflow.Ellipsis, 
                                color = textColor
                            )
                        }
                    }
                    HorizontalDivider(color = textColor.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        } else {
             Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                 if (query.length >= 2) {
                     Text(
                        Strings.NO_RESULTS, 
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
                                text = Strings.SEARCH_EXAMPLES_TITLE, 
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
                                 text = Strings.SEARCH_EXAMPLES_SUBTITLE, 
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
