package br.com.irse.verse.ui.views
import br.com.irse.verse.PrimaryAmber

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.PrimaryAmber
import br.com.irse.verse.core.Strings
import br.com.irse.verse.ui.components.ExternalLinkIcon
import br.com.irse.verse.ui.pointerHoverIconHand
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import verse.composeapp.generated.resources.Res
import verse.composeapp.generated.resources.logo
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AboutView(textColor: Color) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho Principal
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            Strings.APP_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            Strings.APP_VERSION,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryAmber,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card de Funcionamento (Destaque)
        Surface(
            color = PrimaryAmber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = Strings.ABOUT_DESC_1,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Strings.ABOUT_DESC_2,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Seção ACF
        SectionHeader(
            title = Strings.ABOUT_ACF_TITLE, 
            textColor = textColor,
            trailing = { IconButton(onClick = { openUrl(Strings.ABOUT_ACF_URL) }, modifier = Modifier.pointerHoverIconHand()) { ExternalLinkIcon(color = PrimaryAmber) } }
        )
        Text(
            text = Strings.ABOUT_ACF_DESC,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Seção IRSE
        SectionHeader(
            title = Strings.ABOUT_IRSE_TITLE, 
            textColor = textColor,
            trailing = { IconButton(onClick = { openUrl(Strings.ABOUT_IRSE_URL) }, modifier = Modifier.pointerHoverIconHand()) { ExternalLinkIcon(color = PrimaryAmber) } }
        )
        Text(
            text = Strings.ABOUT_IRSE_DESC,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = textColor.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Atalhos
        SectionHeader(Strings.SHORTCUTS_TITLE, textColor)
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
                Text(Strings.SHORTCUT_MINIMIZE, style = shortcutStyle)
                Text(Strings.SHORTCUT_SEARCH, style = shortcutStyle)
                Text(Strings.SHORTCUT_HISTORY, style = shortcutStyle)
                Text(Strings.SHORTCUT_VERSES, style = shortcutStyle)
                Text(Strings.SHORTCUT_SETTINGS, style = shortcutStyle)
                Text(Strings.SHORTCUT_ABOUT, style = shortcutStyle)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            Strings.COPYRIGHT,
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
                color = PrimaryAmber,
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