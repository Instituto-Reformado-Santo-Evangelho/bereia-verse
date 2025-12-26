package br.com.irse.writers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.VerseRequest
import writers.composeapp.generated.resources.Res
import writers.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun AndroidMiniBubble(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF0000FD))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
         Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Open",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AndroidOverlayExpanded(
    detectedVerses: List<Pair<VerseRequest, String?>>,
    onClose: () -> Unit,
    onDismiss: () -> Unit
) {
    // A Card representing the dialog
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0000FD))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Verse Reader", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Icon(
                    painter = painterResource(Res.drawable.logo), // Using logo as icon placeholder or X
                    contentDescription = "Minimize",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable { onClose() }
                )
            }

            // Content reusing common App logic
            // Since we can't easily import the exact Composable if it's too tied to Desktop, 
            // we'll reimplement a simple list here or refactor App.kt later.
            // For now, let's use the shared `App` but wrapped properly? 
            // `App` expects window resizing logic. Let's use `App` but ignore resize callbacks.
            
            Box(modifier = Modifier.weight(1f)) {
                 App(
                    detectedVerses = detectedVerses,
                    isProcessing = false,
                    onClose = onClose
                 )
            }
        }
    }
}
