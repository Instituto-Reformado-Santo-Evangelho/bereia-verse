package br.com.irse.verse.ui.views

import br.com.irse.verse.PrimaryAmber
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.pointerHoverIconHand
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsView(viewModel: VerseViewModel, textColor: Color) {
    val fontSize by viewModel.fontSize.collectAsState()
    val currentFontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Font Family
        Column {
            Text(
                Strings.FONT_FAMILY, 
                style = MaterialTheme.typography.titleSmall, 
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val fontOptions = listOf(
                "sans-serif" to Strings.SANS_SERIF,
                "serif" to Strings.SERIF,
                "monospace" to Strings.MONOSPACE,
                "cursive" to Strings.CURSIVE
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontOptions.forEach { (key, label) ->
                    val isSelected = currentFontFamily == key
                    Surface(
                        onClick = { viewModel.updateFontFamily(key) },
                        selected = isSelected,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryAmber.copy(alpha = 0.15f) else textColor.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, if (isSelected) PrimaryAmber else Color.Transparent),
                        modifier = Modifier.fillMaxWidth().pointerHoverIconHand()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = when (key) {
                                        "serif" -> FontFamily.Serif
                                        "monospace" -> FontFamily.Monospace
                                        "cursive" -> FontFamily.Cursive
                                        else -> FontFamily.SansSerif
                                    }
                                ),
                                color = if (isSelected) PrimaryAmber else textColor
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fire Animation Toggle
        val showFireAnimation by viewModel.showFireAnimation.collectAsState()
        Surface(
            modifier = Modifier.fillMaxWidth().pointerHoverIconHand(),
            color = textColor.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp),
            onClick = { viewModel.updateShowFireAnimation(!showFireAnimation) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Strings.FIRE_ANIMATION, 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = textColor
                    )
                    Text(
                        Strings.FIRE_ANIMATION_DESC, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = showFireAnimation,
                    onCheckedChange = { viewModel.updateShowFireAnimation(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryAmber,
                        checkedTrackColor = PrimaryAmber.copy(alpha = 0.5f),
                        uncheckedThumbColor = textColor.copy(alpha = 0.4f),
                        uncheckedTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )
            }
        }

        // Font Size
        Column {
            Text(
                Strings.FONT_SIZE, 
                style = MaterialTheme.typography.titleSmall, 
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                    valueRange = 12f..32f,
                    modifier = Modifier.weight(1f).pointerHoverIconHand(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAmber, 
                        activeTrackColor = PrimaryAmber
                    )
                )
                Text(
                    "${fontSize}px", 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = textColor
                )
            }
        }

        // Line Height
        Column {
            Text(
                Strings.LINE_HEIGHT, 
                style = MaterialTheme.typography.titleSmall, 
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Slider(
                    value = lineHeight,
                    onValueChange = { viewModel.updateLineHeight(it) },
                    valueRange = 1.0f..2.5f,
                    modifier = Modifier.weight(1f).pointerHoverIconHand(),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAmber, 
                        activeTrackColor = PrimaryAmber
                    )
                )
                Text(
                    String.format("%.1fx", lineHeight),
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = textColor
                )
            }
        }

        // Preview
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            color = textColor.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                val previewFontFamily = when (currentFontFamily) {
                    "serif" -> FontFamily.Serif
                    "monospace" -> FontFamily.Monospace
                    "cursive" -> FontFamily.Cursive
                    else -> FontFamily.SansSerif
                }
                Text(
                    "No princípio criou Deus os céus e a terra. E a terra era sem forma e vazia; e havia trevas sobre a face do abismo; e o Espírito de Deus se movia sobre a face das águas.", 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        fontFamily = previewFontFamily,
                        lineHeight = (fontSize * lineHeight).sp
                    ),
                    color = textColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Snapshot Template Selector
        Column {
            Text(
                "Modelo de Compartilhamento", 
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.first().scrollDelta
                                    // Scroll vertical do mouse (y) vira horizontal na lista
                                    val scrollAmount = delta.y * 30 
                                    coroutineScope.launch {
                                        listState.scrollBy(scrollAmount)
                                    }
                                }
                            }
                        }
                    }
            ) {
                items(viewModel.templatesList) { template ->
                    val isSelected = selectedTemplate.id == template.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(120.dp)
                            .pointerHoverIconHand()
                            .clickable { viewModel.setTemplate(template) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent, 
                            border = if (isSelected) BorderStroke(3.dp, PrimaryAmber) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.size(120.dp, 80.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(template.backgroundBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Jesus chorou.\nJo 11:35",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (template.fontFamilyName == "Serif") FontFamily.Serif else FontFamily.SansSerif,
                                        fontSize = 10.sp
                                    ),
                                    color = template.contentColor,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            template.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PrimaryAmber else textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
