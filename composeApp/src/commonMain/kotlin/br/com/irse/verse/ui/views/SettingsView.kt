package br.com.irse.verse.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun SettingsView(viewModel: VerseViewModel, textColor: Color) {
    val fontSize by viewModel.fontSize.collectAsState()
    val isSerif by viewModel.isSerif.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Font Size
        Column {
            Text(
                Strings.FONT_SIZE, 
                style = MaterialTheme.typography.titleSmall, 
                color = PrimaryAmber, 
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
                    modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand),
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
                color = PrimaryAmber, 
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
                    modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand),
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

        // Font Family
        Column {
            Text(
                Strings.FONT_FAMILY, 
                style = MaterialTheme.typography.titleSmall, 
                color = PrimaryAmber, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isSerif,
                    onClick = { viewModel.toggleFontSerif(false) },
                    label = { Text(Strings.SANS_SERIF) },
                    modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                )
                FilterChip(
                    selected = isSerif,
                    onClick = { viewModel.toggleFontSerif(true) },
                    label = { Text(Strings.SERIF) },
                    modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
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
                val previewFontFamily = if (isSerif) FontFamily.Serif else FontFamily.SansSerif
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