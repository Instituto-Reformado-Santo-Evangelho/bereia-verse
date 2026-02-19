package br.com.irse.verse.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.HistoryEntry
import br.com.irse.verse.ui.pointerHoverIconHand
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryView(
    history: List<HistoryEntry>, 
    onSelect: (String) -> Unit, 
    textColor: Color, 
    fontSize: Int, 
    fontFamily: FontFamily
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
            Text(stringResource(Res.string.no_history), color = textColor.copy(alpha = 0.5f)) 
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(history) { _, entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .pointerHoverIconHand()
                        .clickable { onSelect(entry.query) }, 
                    color = Color.Transparent, 
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = entry.query, 
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSize.sp, 
                                fontFamily = fontFamily
                            ), 
                            fontWeight = FontWeight.Bold, 
                            color = textColor
                        )
                        Text(
                            text = dateFormat.format(Date(entry.timestamp)), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                }
                HorizontalDivider(color = textColor.copy(alpha = 0.05f))
            }
        }
    }
}