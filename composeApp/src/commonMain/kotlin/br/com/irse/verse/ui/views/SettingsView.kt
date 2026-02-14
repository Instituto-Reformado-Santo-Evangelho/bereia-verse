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
import compose.icons.feathericons.Check
import br.com.irse.verse.ui.pointerHoverIconHand
import kotlinx.coroutines.launch

import br.com.irse.verse.ui.components.SnapshotLayout
import br.com.irse.verse.core.VerseRequest
import org.jetbrains.compose.resources.stringResource
import verse.composeapp.generated.resources.*

@Composable
fun SettingsView(viewModel: VerseViewModel, textColor: Color) {
    val fontSize by viewModel.fontSize.collectAsState()
    val currentFontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val showFireAnimation by viewModel.showFireAnimation.collectAsState()

    val signature by viewModel.signature.collectAsState()
    val isTransparencySupported by viewModel.isTransparencySupported.collectAsState()

    // TextFieldValue para evitar o bug do cursor
    var signatureValue by remember(signature) { 
        mutableStateOf(TextFieldValue(text = signature, selection = TextRange(signature.length))) 
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- SEÇÃO: APARÊNCIA DO TEXTO ---
        SettingsSection(title = stringResource(Res.string.settings_appearance)) {
            // Font Family
            Column {
                Text(stringResource(Res.string.font_family), style = MaterialTheme.typography.labelMedium, color = VerseColors.PrimaryAmber, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                val fontOptions = listOf(
                    "sans-serif" to stringResource(Res.string.font_sans_serif), 
                    "serif" to stringResource(Res.string.font_serif), 
                    "monospace" to stringResource(Res.string.font_monospace), 
                    "cursive" to stringResource(Res.string.font_cursive)
                )
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
                                if (isSelected) Icon(FeatherIcons.Check, null, tint = VerseColors.PrimaryAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sliders (Fonte e Linha)
            SettingsSlider(label = stringResource(Res.string.font_size), value = fontSize.toFloat(), range = 12f..32f, valueLabel = "${fontSize}px") { viewModel.updateFontSize(it.toInt()) }
            SettingsSlider(label = stringResource(Res.string.line_height), value = lineHeight, range = 1.0f..2.5f, valueLabel = String.format("%.1fx", lineHeight)) { viewModel.updateLineHeight(it) }
        }

        // --- SEÇÃO: COMPARTILHAMENTO ---
        SettingsSection(title = stringResource(Res.string.settings_sharing)) {
            // SUBSEÇÃO: VERSÍCULOS
            Text(stringResource(Res.string.tab_verses), style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(Res.string.settings_sharing_verses_desc), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            
            TemplateSelector(viewModel, selectedTemplate, textColor)
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            // SUBSEÇÃO: NOTAS
            Text(stringResource(Res.string.tab_notes), style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(Res.string.settings_sharing_notes_desc), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Assinatura
            Text(stringResource(Res.string.settings_signature), style = MaterialTheme.typography.labelMedium, color = VerseColors.PrimaryAmber, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val signatureHint = stringResource(Res.string.settings_signature_hint)
            TextField(
                value = signatureValue,
                onValueChange = { 
                    signatureValue = it
                    viewModel.updateSignature(it.text) 
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(signatureHint, fontSize = 14.sp, color = textColor.copy(alpha = 0.3f)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = textColor.copy(alpha = 0.03f),
                    unfocusedContainerColor = textColor.copy(alpha = 0.03f),
                    focusedIndicatorColor = VerseColors.PrimaryAmber,
                    cursorColor = VerseColors.PrimaryAmber
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(Res.string.settings_signature_desc), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))
            
            // Selector de Modelos de Notas
            NoteTemplateSelector(viewModel, selectedTemplate, textColor)
        }

        // --- SEÇÃO: PERFORMANCE E SISTEMA ---
        SettingsSection(title = stringResource(Res.string.settings_system)) {
            SettingsToggle(title = stringResource(Res.string.fire_animation), desc = stringResource(Res.string.fire_animation_desc), checked = showFireAnimation, textColor = textColor) { viewModel.updateShowFireAnimation(it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.05f))

            
            val showSnapshotAction by viewModel.showSnapshotAction.collectAsState()
            SettingsToggle(
                title = stringResource(Res.string.settings_snapshot_title), 
                desc = stringResource(Res.string.settings_snapshot_desc), 
                checked = showSnapshotAction, 
                textColor = textColor
            ) { viewModel.updateShowSnapshotAction(it) }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = textColor.copy(alpha = 0.05f))
            
            val isTransparent by viewModel.isTransparent.collectAsState()
                var showTransparencyDialog by remember { mutableStateOf(false) }
                val restartRequired by viewModel.restartRequired.collectAsState()
                val scope = rememberCoroutineScope()
            // Diálogo 1: Confirmação para ativar a transparência
            if (showTransparencyDialog) {
                AlertDialog(
                    onDismissRequest = { showTransparencyDialog = false },
                    title = { Text(stringResource(Res.string.dialog_transparency_title), fontWeight = FontWeight.Bold) },
                    text = { Text(stringResource(Res.string.dialog_transparency_message), style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.updateIsTransparent(true)
                                    showTransparencyDialog = false
                                    viewModel.signalRestartRequired() // Use public function
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VerseColors.PrimaryAmber)
                        ) {
                            Text(stringResource(Res.string.dialog_ok)) // Changed from dialog_yes to dialog_ok
                        }
                    }
                )
            }

            // Diálogo 2: Aviso de reinicialização necessária
            if (restartRequired) {
                AlertDialog(
                    onDismissRequest = { /* Não pode ser dispensado */ },
                    title = { Text(stringResource(Res.string.dialog_restart_title), fontWeight = FontWeight.Bold) },
                    text = { Text(stringResource(Res.string.dialog_restart_message), style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        Button(
                            onClick = { System.exit(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = VerseColors.PrimaryAmber)
                        ) {
                            Text(stringResource(Res.string.dialog_ok))
                        }
                    }
                )
            }

            SettingsToggle(
                title = stringResource(Res.string.settings_transparency_title),
                desc = if (isTransparencySupported) stringResource(Res.string.settings_transparency_desc) else stringResource(Res.string.settings_transparency_desc) + " (Não suportado)",
                checked = isTransparent && isTransparencySupported,
                enabled = isTransparencySupported,
                textColor = textColor
            ) { enabled ->
                scope.launch {
                    if (enabled) {
                        showTransparencyDialog = true
                    } else {
                        viewModel.updateIsTransparent(false)
                        viewModel.signalRestartRequired() // Use public function
                    }
                }
            }
        }
        
        // --- SEÇÃO: SINCRONIZAÇÃO ---
        val isSyncAuthorized by viewModel.isSyncAuthorized.collectAsState()
        val syncState by viewModel.syncState.collectAsState()
        
        SettingsSection(title = stringResource(Res.string.settings_sync)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Título e Descrição
                Text(
                    text = if (isSyncAuthorized) stringResource(Res.string.settings_sync_connected) else stringResource(Res.string.settings_sync_disconnected),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = if (isSyncAuthorized) 
                        stringResource(Res.string.settings_sync_desc_connected) 
                    else 
                        stringResource(Res.string.settings_sync_desc_disconnected) + " (Necessita navegador padrão configurado)",
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
                        text = if (isSyncAuthorized) stringResource(Res.string.settings_sync_logout) else stringResource(Res.string.settings_sync_login),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status discreto abaixo do botão (apenas quando conectado)
                if (isSyncAuthorized) {
                    Text(
                        text = when(syncState) {
                            CloudSyncState.SYNCING -> stringResource(Res.string.settings_sync_status_syncing)
                            CloudSyncState.SUCCESS -> stringResource(Res.string.settings_sync_status_success)
                            CloudSyncState.ERROR -> stringResource(Res.string.settings_sync_status_error)
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
fun SettingsToggle(title: String, desc: String, checked: Boolean, textColor: Color, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (enabled) textColor else textColor.copy(alpha = 0.5f))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = if (enabled) textColor.copy(alpha = 0.6f) else textColor.copy(alpha = 0.3f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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
                                    imageVector = FeatherIcons.Check, 
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
                                    imageVector = FeatherIcons.Check, 
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
