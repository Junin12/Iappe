package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PecaEntity
import com.example.data.local.TipoPecaOptions
import com.example.ui.theme.*
import com.example.ui.viewmodel.PecasUiState
import com.example.ui.viewmodel.PecasViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PecasScreen(
    viewModel: PecasViewModel,
    onSelectPeca: (PecaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddPeca() },
                containerColor = Gold18k,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_peca_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Semijoia",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Minhas Semijoias",
                        color = Gold18k,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.pecas.size} peças cadastradas para vídeos",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Quick stats chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💎", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Catálogo TikTok",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por nome ou SKU...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = TextMuted)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pecas_search_bar"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold18k,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Gold18k,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            // Filter Chips by Type
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Todos" chip
                item {
                    val isSelected = uiState.selectedTipo == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTipo(null) },
                        label = { Text("Todos (${uiState.pecas.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold18k,
                            selectedLabelColor = Color.Black,
                            containerColor = ObsidianSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Gold18k else ObsidianBorder,
                            selectedBorderColor = Gold18k
                        ),
                        modifier = Modifier.testTag("filter_chip_todos")
                    )
                }

                items(TipoPecaOptions.all.size) { idx ->
                    val tipo = TipoPecaOptions.all[idx]
                    val isSelected = uiState.selectedTipo == tipo
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTipo(if (isSelected) null else tipo) },
                        label = { Text(tipo, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold18k,
                            selectedLabelColor = Color.Black,
                            containerColor = ObsidianSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Gold18k else ObsidianBorder,
                            selectedBorderColor = Gold18k
                        ),
                        modifier = Modifier.testTag("filter_chip_$tipo")
                    )
                }
            }

            // 2-Column Grid
            if (uiState.pecas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "✨", fontSize = 42.sp)
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedTipo != null)
                                "Nenhuma semijoia encontrada" else "Nenhuma semijoia cadastrada",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Toque no botão '+' abaixo para cadastrar suas peças e vincular roteiros e gravações.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pecas_grid")
                ) {
                    items(uiState.pecas, key = { it.id }) { peca ->
                        PecaCard(
                            peca = peca,
                            currencyFormat = currencyFormat,
                            onClick = { onSelectPeca(peca) }
                        )
                    }
                }
            }
        }

        // Add/Edit BottomSheet
        if (uiState.isAddEditOpen) {
            PecaFormModal(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { viewModel.closeAddEdit() }
            )
        }

        // Delete Confirmation Dialog
        if (uiState.isDeleteConfirmOpen && uiState.pecaPendingDelete != null) {
            val peca = uiState.pecaPendingDelete!!
            AlertDialog(
                onDismissRequest = { viewModel.dismissDelete() },
                containerColor = ObsidianSurfaceElevated,
                icon = {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TikTokPink)
                },
                title = {
                    Text(text = "Excluir semijoia?", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "Tem certeza que deseja excluir \"${peca.nome}\"? Todos os vídeos vinculados a esta peça também serão excluídos do banco de dados.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeletePeca() },
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                        modifier = Modifier.testTag("confirm_delete_peca_button")
                    ) {
                        Text(text = "Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDelete() }
                    ) {
                        Text(text = "Cancelar", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
fun PecaCard(
    peca: PecaEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("peca_card_${peca.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(ObsidianSurfaceHighlight)
            ) {
                if (peca.fotos.isNotEmpty()) {
                    AsyncImage(
                        model = peca.fotos.first(),
                        contentDescription = peca.nome,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(ObsidianSurface, ObsidianSurfaceHighlight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = Gold18k.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Type & Banho overlay tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianBackground.copy(alpha = 0.85f),
                    border = BorderStroke(0.5.dp, Gold18k.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${peca.tipo} • ${peca.milesimos}M",
                        color = Gold18k,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Stock indicator
                if (peca.estoque <= 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TikTokPink.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Esgotado",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = peca.nome,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                // Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (peca.precoPromocional != null) {
                        Column {
                            Text(
                                text = currencyFormat.format(peca.preco),
                                color = TextMuted,
                                fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Text(
                                text = currencyFormat.format(peca.precoPromocional),
                                color = Gold18k,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = currencyFormat.format(peca.preco),
                            color = Gold18k,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // SKU / Details icon
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
