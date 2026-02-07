package br.com.irse.verse.ui.views

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.ui.theme.VerseColors
import br.com.irse.verse.ui.components.ExternalLinkIcon
import br.com.irse.verse.ui.pointerHoverIconHand
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AboutView(textColor: Color) {
    val scrollState = rememberScrollState()
    
    val isDark = isSystemInDarkTheme()
    val logoResource = if (isDark) Res.drawable.logo_dark else Res.drawable.logo
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho Principal
        Image(
            painter = painterResource(logoResource),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(Res.string.app_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            stringResource(Res.string.app_version),
            style = MaterialTheme.typography.titleMedium,
            color = VerseColors.PrimaryAmber,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card de Funcionamento (Destaque)
        Surface(
            color = VerseColors.PrimaryAmber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.about_desc_1),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.about_desc_2),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Seção ACF
        SectionHeader(
            title = stringResource(Res.string.about_acf_title), 
            textColor = textColor,
            trailing = { IconButton(onClick = { openUrl("https://biblias.com.br/artigo/introducao-a-edicao-almeida-corrigida-fiel-acf") }, modifier = Modifier.pointerHoverIconHand()) { ExternalLinkIcon(color = VerseColors.PrimaryAmber) } }
        )
        Text(
            text = stringResource(Res.string.about_acf_desc),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Seção IRSE
        SectionHeader(
            title = stringResource(Res.string.about_irse_title), 
            textColor = textColor,
            trailing = { IconButton(onClick = { openUrl("https://tech.santoevangelho.com.br") }, modifier = Modifier.pointerHoverIconHand()) { ExternalLinkIcon(color = VerseColors.PrimaryAmber) } }
        )
        Text(
            text = stringResource(Res.string.about_irse_desc),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Atalhos
        SectionHeader(stringResource(Res.string.shortcuts_title), textColor)
        Surface(
            color = textColor.copy(alpha = 0.04f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val shortcutStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f)
                )
                Text(stringResource(Res.string.shortcut_minimize), style = shortcutStyle)
                Text(stringResource(Res.string.shortcut_search), style = shortcutStyle)
                Text(stringResource(Res.string.shortcut_history), style = shortcutStyle)
                Text(stringResource(Res.string.shortcut_verses), style = shortcutStyle)
                Text(stringResource(Res.string.shortcut_settings), style = shortcutStyle)
                Text(stringResource(Res.string.shortcut_about), style = shortcutStyle)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            stringResource(Res.string.copyright),
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionHeader(
    title: String, 
    textColor: Color, 
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp),
                fontWeight = FontWeight.ExtraBold,
                color = VerseColors.PrimaryAmber,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(32.dp)) {
                    trailing()
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
    }
}

private fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}