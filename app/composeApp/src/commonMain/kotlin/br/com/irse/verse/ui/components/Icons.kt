package br.com.irse.verse.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import androidx.compose.material3.Icon

@Composable
fun ExternalLinkIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.ExternalLink,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(14.dp)
    )
}

@Composable
fun CopyIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.Copy,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun HistoryIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.RotateCcw, // Feather usa RotateCcw para histórico
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun SearchIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.Search,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun CheckIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.Check,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun BibleIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.BookOpen,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
fun SettingsIcon(color: Color) {
    Icon(
        imageVector = FeatherIcons.Settings,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}