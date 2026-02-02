package br.com.irse.verse

import br.com.irse.verse.ui.theme.VerseColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.irse.verse.core.VerseViewModel
import org.jetbrains.compose.resources.painterResource
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo

@Composable
fun AndroidMiniBubble(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(60.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        color = VerseColors.PrimaryAmber,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Open",
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@Composable
fun AndroidOverlayExpanded(
    viewModel: VerseViewModel,
    onClose: () -> Unit
) {
    val detectedVerses by viewModel.detectedVerses.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    
    val uniqueBooks = remember(detectedVerses) {
        detectedVerses.map { it.first.book }.distinct()
    }

    // Overlay transparente para fechar ao clicar fora
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clickable(enabled = false) {}, // Evita fechar ao clicar no card
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            // Reuso do App principal diretamente, ele já tem seu próprio Header
            App(
                viewModel = viewModel,
                onClose = onClose,
                isTransparent = false
            )
        }
    }
}