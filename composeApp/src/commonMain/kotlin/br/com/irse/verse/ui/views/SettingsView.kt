package br.com.irse.verse.ui.views

import br.com.irse.verse.ui.theme.VerseColors
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.irse.verse.core.Strings
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.core.CloudSyncState
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cloud
import compose.icons.feathericons.CloudOff
import br.com.irse.verse.ui.pointerHoverIconHand
import kotlinx.coroutines.launch

import br.com.irse.verse.ui.components.SnapshotLayout
import br.com.irse.verse.core.VerseRequest

@Composable
fun SettingsView(viewModel: VerseViewModel, textColor: Color) {
    val fontSize by viewModel.fontSize.collectAsState()
    val currentFontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val showFireAnimation by viewModel.showFireAnimation.collectAsState()
    val animatedWindow by viewModel.animatedWindow.collectAsState()
    val signature by viewModel.signature.collectAsState()

    // TextFieldValue para evitar o bug do cursor
    var signatureValue by remember(signature) { 
        mutableStateOf(TextFieldValue(text = signature, selection = TextRange(signature.length))) 
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- SEÇÃO: APARÊNCIA DO TEXTO ---
        SettingsSection(title = "Aparência do Texto") {
            // Font Family
            Column {
                Text(Strings.FONT_FAMILY, style = MaterialTheme.typography.labelMedium, color = VerseColors.PrimaryAmber, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                val fontOptions = listOf("sans-serif" to Strings.SANS_SERIF, "serif" to Strings.SERIF, "monospace" to Strings.MONOSPACE, "cursive" to Strings.CURSIVE)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fontOptions.forEach { (key, label) ->
                        val isSelected = currentFontFamily == key
                        Surface(
                            onClick = { viewModel.updateFontFamily(key) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VerseColors.PrimaryAmber.copy(alpha = 0.15f) else textColor.copy(alpha = 0.03f),
                            border = BorderStroke(1.dp, if (isSelected) VerseColors.PrimaryAmber else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().pointerHoverIconHand()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = getFontFamily(key)), color = if (isSelected) VerseColors.PrimaryAmber else textColor)
                                if (isSelected) Icon(Icons.Default.Check, null, tint = VerseColors.PrimaryAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sliders (Fonte e Linha)
            SettingsSlider(label = Strings.FONT_SIZE, value = fontSize.toFloat(), range = 12f..32f, valueLabel = "${fontSize}px") { viewModel.updateFontSize(it.toInt()) }
            SettingsSlider(label = Strings.LINE_HEIGHT, value = lineHeight, range = 1.0f..2.5f, valueLabel = String.format("%.1fx", lineHeight)) { viewModel.updateLineHeight(it) }
        }

        // --- SEÇÃO: COMPARTILHAMENTO ---
        SettingsSection(title = "Compartilhamento (Fotos)") {
            // SUBSEÇÃO: VERSÍCULOS
            Text("Versículos", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Selecione o modelo padrão para compartilhamento de versículos.", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            
            TemplateSelector(viewModel, selectedTemplate, textColor)
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            // SUBSEÇÃO: NOTAS
            Text("Notas", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Personalize o compartilhamento de suas anotações.", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Assinatura
            Text("Assinatura", style = MaterialTheme.typography.labelMedium, color = VerseColors.PrimaryAmber, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = signatureValue,
                onValueChange = { 
                    signatureValue = it
                    viewModel.updateSignature(it.text) 
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: Agostinho", fontSize = 14.sp, color = textColor.copy(alpha = 0.3f)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = textColor.copy(alpha = 0.03f),
                    unfocusedContainerColor = textColor.copy(alpha = 0.03f),
                    focusedIndicatorColor = VerseColors.PrimaryAmber,
                    cursorColor = VerseColors.PrimaryAmber
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Exibida exclusivamente no rodapé dos snapshots de notas.", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))
            
            // Selector de Modelos de Notas
            NoteTemplateSelector(viewModel, selectedTemplate, textColor)
        }

        // --- SEÇÃO: PERFORMANCE E SISTEMA ---
        SettingsSection(title = "Sistema") {
            SettingsToggle(title = Strings.FIRE_ANIMATION, desc = Strings.FIRE_ANIMATION_DESC, checked = showFireAnimation, textColor = textColor) { viewModel.updateShowFireAnimation(it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.05f))
            SettingsToggle(title = Strings.WINDOW_ANIMATION, desc = Strings.WINDOW_ANIMATION_DESC, checked = animatedWindow, textColor = textColor) { viewModel.updateAnimatedWindow(it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.05f))
            
            val showSnapshotAction by viewModel.showSnapshotAction.collectAsState()
            SettingsToggle(
                title = "Captura de Imagem (Snapshot)", 
                desc = "Exibir botão de captura na lista de notas e versículos.", 
                checked = showSnapshotAction, 
                textColor = textColor
            ) { viewModel.updateShowSnapshotAction(it) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.05f))
            
            val isTransparent by viewModel.isTransparent.collectAsState()
            SettingsToggle(
                title = "Bordas Arredondadas (Transparência)", 
                desc = "Desative se o app piscar ou fechar ao abrir. O app será reiniciado.", 
                checked = isTransparent, 
                textColor = textColor
            ) { viewModel.updateIsTransparent(it) }
        }
        
        // --- SEÇÃO: SINCRONIZAÇÃO ---
        val isSyncAuthorized by viewModel.isSyncAuthorized.collectAsState()
        val syncState by viewModel.syncState.collectAsState()
        
        SettingsSection(title = "Sincronização") {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Título e Descrição
                Text(
                    text = if (isSyncAuthorized) "Google Drive Conectado" else "Backup na Nuvem",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = if (isSyncAuthorized) 
                        "Suas notas estão sendo sincronizadas automaticamente com sua conta Google." 
                    else 
                        "Faça login para salvar suas anotações e acessá-las em qualquer dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Botão de Ação (Ícone + Texto)
                Button(
                    onClick = { if (isSyncAuthorized) viewModel.logoutDrive() else viewModel.loginToDrive() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSyncAuthorized) VerseColors.ErrorRed.copy(alpha = 0.1f) else VerseColors.PrimaryAmber,
                        contentColor = if (isSyncAuthorized) VerseColors.ErrorRed else VerseColors.HeaderContentColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isSyncAuthorized) FeatherIcons.CloudOff else FeatherIcons.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncAuthorized) "Desconectar Conta" else "Conectar Google Drive",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status discreto abaixo do botão (apenas quando conectado)
                if (isSyncAuthorized) {
                    Text(
                        text = when(syncState) {
                            CloudSyncState.SYNCING -> "Status: Sincronizando..."
                            CloudSyncState.SUCCESS -> "Status: Tudo atualizado"
                            CloudSyncState.ERROR -> "Status: Erro na sincronização"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.6f), letterSpacing = 1.2.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = VerseColors.PrimaryAmber, activeTrackColor = VerseColors.PrimaryAmber)
        )
    }
}

@Composable
fun SettingsToggle(title: String, desc: String, checked: Boolean, textColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = VerseColors.PrimaryAmber, checkedTrackColor = VerseColors.PrimaryAmber.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun TemplateSelector(viewModel: VerseViewModel, selectedTemplate: VerseViewModel.SnapshotTemplate, textColor: Color) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Dados fake para preview
    val previewVerses = listOf(
        VerseRequest(1, "João", 11, 35) to "Jesus chorou."
    )

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth().height(160.dp).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.first().scrollDelta
                        event.changes.forEach { it.consume() }
                        coroutineScope.launch { listState.scrollBy(delta.y * 30) }
                    }
                }
            }
        }
    ) {
        items(viewModel.templatesList) { template ->
            val isSelected = selectedTemplate.id == template.id
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent, 
                    border = if (isSelected) BorderStroke(3.dp, VerseColors.PrimaryAmber) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                    shadowElevation = if (isSelected) 8.dp else 2.dp,
                    modifier = Modifier
                        .size(130.dp, 130.dp)
                        .clickable { viewModel.setTemplate(template) }
                        .pointerHoverIconHand()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Renderiza o Layout Real em escala reduzida
                        SnapshotLayout(
                            verses = previewVerses, 
                            template = template,
                            isPreview = true
                        )
                        
                        // Overlay de seleção
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) { 
                                Icon(
                                    imageVector = Icons.Default.Check, 
                                    contentDescription = "Selecionado", 
                                    tint = VerseColors.PrimaryAmber, 
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, CircleShape)
                                        .padding(4.dp)
                                ) 
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = template.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun NoteTemplateSelector(viewModel: VerseViewModel, selectedTemplate: VerseViewModel.SnapshotTemplate, textColor: Color) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Dados fake para preview
    val content = "O SENHOR é o meu pastor, nada me faltará."
    val signature = "Davi"

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth().height(160.dp).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.first().scrollDelta
                        event.changes.forEach { it.consume() }
                        coroutineScope.launch { listState.scrollBy(delta.y * 30) }
                    }
                }
            }
        }
    ) {
        items(viewModel.templatesList) { template ->
            val isSelected = selectedTemplate.id == template.id
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent, 
                    border = if (isSelected) BorderStroke(3.dp, VerseColors.PrimaryAmber) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                    shadowElevation = if (isSelected) 8.dp else 2.dp,
                    modifier = Modifier
                        .size(130.dp, 130.dp)
                        .clickable { viewModel.setTemplate(template) }
                        .pointerHoverIconHand()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Renderiza o Layout de NOTA em escala reduzida
                        br.com.irse.verse.ui.components.NoteSnapshotLayout(
                            content = content,
                            reference = "Salmos 23:1",
                            signature = signature,
                            template = template,
                            isPreview = true
                        )
                        
                        // Overlay de seleção
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) { 
                                Icon(
                                    imageVector = Icons.Default.Check, 
                                    contentDescription = "Selecionado", 
                                    tint = VerseColors.PrimaryAmber, 
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, CircleShape)
                                        .padding(4.dp)
                                ) 
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = template.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) VerseColors.PrimaryAmber else textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun getFontFamily(key: String) = when (key) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}
