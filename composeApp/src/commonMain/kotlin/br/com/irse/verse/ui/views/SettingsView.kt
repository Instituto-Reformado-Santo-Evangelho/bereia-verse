package br.com.irse.verse.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.PrimaryAmber
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.ui.pointerHoverIconHand

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsView(viewModel: VerseViewModel, textColor: Color) {
    val fontSize by viewModel.fontSize.collectAsState()
    val currentFontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Font Family (Agora no Topo)
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
    }
}