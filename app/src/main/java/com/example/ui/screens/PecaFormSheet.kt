package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.BanhoPecaOptions
import com.example.data.local.PedraPecaOptions
import com.example.data.local.TipoPecaOptions
import com.example.ui.theme.*
import com.example.ui.viewmodel.PecasUiState
import com.example.ui.viewmodel.PecasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PecaFormModal(
    viewModel: PecasViewModel,
    uiState: PecasUiState,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Android Photo Picker (zero-permission, compliant with Android policies)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(uris.map { it.toString() })
        }
    }

    val isEditing = uiState.editingPeca != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        contentColor = TextPrimary,
        tonalElevation = 12.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Gold18k.copy(alpha = 0.5f))
        },
        modifier = Modifier.testTag("peca_form_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Editar Semijoia" else "Nova Semijoia",
                        color = Gold18k,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Catálogo para criação de conteúdo faceless",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_peca_form_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = TextMuted
                    )
                }
            }

            HorizontalDivider(color = ObsidianBorder)

            // Section: Fotos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fotos da Peça (${uiState.formFotos.size}/5)",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("add_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Adicionar Fotos", color = TikTokPink, fontSize = 13.sp)
                    }
                }

                if (uiState.formFotos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, ObsidianBorder), RoundedCornerShape(12.dp))
                            .background(ObsidianBackground)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Toque para selecionar fotos da galeria", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(uiState.formFotos) { index, photoUri ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Gold18k.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Foto da peça",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Remove button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        .clickable { viewModel.removePhoto(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gemini Photo Analysis Section
            val context = androidx.compose.ui.platform.LocalContext.current
            if (uiState.formFotos.isNotEmpty()) {
                if (uiState.isAnalyzingPhoto) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_analysis_loading_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceHighlight),
                        border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TikTokPink,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Analisando foto da peça com Gemini AI...",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Redimensionando para 1024px e identificando banho, pedra e tipo...",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.cancelPhotoAnalysis() },
                                modifier = Modifier.testTag("cancel_photo_analysis_button")
                            ) {
                                Text(text = "Cancelar", color = TikTokPink, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            uiState.formFotos.firstOrNull()?.let { uri ->
                                viewModel.analyzePhoto(context, uri)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("analyze_photo_with_ai_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                        border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.7f))
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analisar foto da peça com IA (Gemini)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // AI Suggestion Banner & Video Ideas
            if (uiState.photoAnalysisApplied && uiState.photoAnalysisIdeas.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Gold18k.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ideias de Vídeo Sugeridas pela IA:",
                                color = Gold18k,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        uiState.photoAnalysisIdeas.forEachIndexed { i, ideia ->
                            Text(
                                text = "• $ideia",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Field: Nome (Obrigatório)
            OutlinedTextField(
                value = uiState.formNome,
                onValueChange = { viewModel.updateFormNome(it) },
                label = { Text("Nome da Peça *") },
                placeholder = { Text("Ex: Colar Gravatinha Zircônia") },
                isError = uiState.formNomeError != null,
                supportingText = uiState.formNomeError?.let { { Text(it, color = TikTokPink) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_peca_nome"),
                colors = outlinedTextFieldColors()
            )

            // Field: Tipo (Obrigatório)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tipo de Semijoia *",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TipoPecaOptions.all.size) { idx ->
                        val tipo = TipoPecaOptions.all[idx]
                        val isSelected = uiState.formTipo == tipo
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFormTipo(tipo) },
                            label = { Text(tipo, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold18k,
                                selectedLabelColor = Color.Black,
                                containerColor = ObsidianSurfaceHighlight,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Gold18k else ObsidianBorder,
                                selectedBorderColor = Gold18k
                            ),
                            modifier = Modifier.testTag("chip_tipo_$tipo")
                        )
                    }
                }
                if (uiState.formTipoError != null) {
                    Text(text = uiState.formTipoError, color = TikTokPink, fontSize = 12.sp)
                }
            }

            // Field: Banho & Milésimos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Banho Selection
                Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Banho", color = TextMuted, fontSize = 12.sp)
                    var banhoExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = banhoExpanded,
                        onExpandedChange = { banhoExpanded = !banhoExpanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.formBanho,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = banhoExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("dropdown_peca_banho"),
                            colors = outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = banhoExpanded,
                            onDismissRequest = { banhoExpanded = false },
                            modifier = Modifier.background(ObsidianSurfaceElevated)
                        ) {
                            BanhoPecaOptions.all.forEach { banho ->
                                DropdownMenuItem(
                                    text = { Text(banho, color = TextPrimary) },
                                    onClick = {
                                        viewModel.updateFormBanho(banho)
                                        banhoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Milésimos
                OutlinedTextField(
                    value = uiState.formMilesimos,
                    onValueChange = { viewModel.updateFormMilesimos(it) },
                    label = { Text("Milésimos") },
                    placeholder = { Text("10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_peca_milesimos"),
                    colors = outlinedTextFieldColors()
                )
            }

            // Field: Pedra
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pedra / Detalhe",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PedraPecaOptions.all.size) { idx ->
                        val pedra = PedraPecaOptions.all[idx]
                        val isSelected = uiState.formPedra == pedra
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFormPedra(pedra) },
                            label = { Text(pedra, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TikTokPink,
                                selectedLabelColor = Color.White,
                                containerColor = ObsidianSurfaceHighlight,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TikTokPink else ObsidianBorder,
                                selectedBorderColor = TikTokPink
                            )
                        )
                    }
                }
            }

            // Preço (Obrigatório) & Preço Promocional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.formPreco,
                    onValueChange = { viewModel.updateFormPreco(it) },
                    label = { Text("Preço (R$) *") },
                    placeholder = { Text("89.90") },
                    isError = uiState.formPrecoError != null,
                    supportingText = uiState.formPrecoError?.let { { Text(it, color = TikTokPink) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_peca_preco"),
                    colors = outlinedTextFieldColors()
                )

                OutlinedTextField(
                    value = uiState.formPrecoPromocional,
                    onValueChange = { viewModel.updateFormPrecoPromocional(it) },
                    label = { Text("Promoção (R$)") },
                    placeholder = { Text("69.90") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_peca_preco_promocional"),
                    colors = outlinedTextFieldColors()
                )
            }

            // SKU & Estoque
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.formSku,
                    onValueChange = { viewModel.updateFormSku(it) },
                    label = { Text("Código SKU") },
                    placeholder = { Text("COL-01") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("input_peca_sku"),
                    colors = outlinedTextFieldColors()
                )

                OutlinedTextField(
                    value = uiState.formEstoque,
                    onValueChange = { viewModel.updateFormEstoque(it) },
                    label = { Text("Estoque") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_peca_estoque"),
                    colors = outlinedTextFieldColors()
                )
            }

            // Link TikTok Shop
            OutlinedTextField(
                value = uiState.formLinkTikTok,
                onValueChange = { viewModel.updateFormLinkTikTok(it) },
                label = { Text("Link da Sacolinha TikTok Shop") },
                placeholder = { Text("https://shop.tiktok.com/...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_peca_link_tiktok"),
                colors = outlinedTextFieldColors()
            )

            // Observações
            OutlinedTextField(
                value = uiState.formObservacoes,
                onValueChange = { viewModel.updateFormObservacoes(it) },
                label = { Text("Observações de Gravação & Dicas de Macro") },
                placeholder = { Text("Ex: Iluminação lateral ressalta o brilho da zircônia. Gravar com luva preta.") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_peca_observacoes"),
                colors = outlinedTextFieldColors()
            )

            // Save Button
            Button(
                onClick = {
                    val ok = viewModel.savePeca()
                    if (ok) onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_peca_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold18k)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Salvar Alterações" else "Cadastrar Semijoia",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold18k,
    unfocusedBorderColor = ObsidianBorder,
    errorBorderColor = TikTokPink,
    focusedLabelColor = Gold18k,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Gold18k
)
