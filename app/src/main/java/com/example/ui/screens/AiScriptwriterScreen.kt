package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CenaScript
import com.example.data.model.GeminiScriptResult
import com.example.ui.components.GlowButton
import com.example.ui.components.GoldPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.PecasViewModel
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScriptwriterScreen(
    viewModel: StudioViewModel,
    pecasViewModel: PecasViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pecasUiState = pecasViewModel?.uiState?.collectAsState()?.value

    val jewelryStyles = listOf(
        "Old Money Elegance (Banho 10M)",
        "Zircônias Cravejadas & Riviera",
        "Resistentes à Água (Treino & Banho)",
        "Delicadas Românticas & Pérolas",
        "Correntaria Chunky Moderna"
    )

    val pieceTypes = listOf(
        "Colar & Choker",
        "Brinco Gota Chunky",
        "Anel Solitário Regulável",
        "Pulseira Riviera Dupla",
        "Mix de 3 Camadas"
    )

    val hookStrategies = listOf(
        "Choque & Objeção 'Preteja?'",
        "Ancoragem de Preço ('Parece R$ 3.000')",
        "ASMR Sensorial Caixinha Veludo",
        "Psicologia Reversa ('Não compre')",
        "Dica de Estilo ('Sem embolar')"
    )

    val videoDurations = listOf(15, 30, 60)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Roteirista com Gemini AI",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Scripts Faceless estruturados com ganchos virais e CTA TikTok Shop",
                            color = Gold18k,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TikTokPink.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Gemini Flash", color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Subtabs: Roteiro Faceless / Ganchos / Além do Vídeo
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("full_script", "Roteiro Faceless", "🎬"),
                    Triple("hooks", "Ganchos Virais", "⚡"),
                    Triple("content_ideas", "Além de Vídeos", "💡")
                ).forEach { (id, label, icon) ->
                    val isSelected = uiState.aiActiveSubTab == id
                    Surface(
                        onClick = { viewModel.setAiSubTab(id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) TikTokPink else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_subtab_$id")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Subtab 1: Roteiro Faceless com Gemini API
        if (uiState.aiActiveSubTab == "full_script") {
            // Card de Peça Selecionada do Catálogo
            if (uiState.aiSelectedPeca != null) {
                val peca = uiState.aiSelectedPeca!!
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Gold18k.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("selected_peca_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "💎", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = peca.nome,
                                        color = Gold18k,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Banho ${peca.banho} (${peca.milesimos}M) • Pedra: ${peca.pedra} • R$ ${String.format(java.util.Locale.US, "%.2f", peca.preco)}",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setAiSelectedPeca(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remover seleção", tint = TextMuted)
                            }
                        }
                    }
                }
            } else if (pecasUiState != null && pecasUiState.pecas.isNotEmpty()) {
                // Seletor rápido de peças do catálogo
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Vincular a uma peça do catálogo:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pecasUiState.pecas) { pecaItem ->
                                Surface(
                                    onClick = { viewModel.prefillForPeca(pecaItem) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ObsidianSurfaceHighlight,
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "💎", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = pecaItem.nome,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Parâmetros do Gerador
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Parametrizar Gerador Faceless Gemini AI:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Duração do Vídeo
                        Text(text = "1. Duração do Vídeo:", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            videoDurations.forEach { sec ->
                                val isSelected = uiState.aiSelectedDuration == sec
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiDuration(sec) },
                                    label = { Text("${sec}s ${if (sec == 15) "(Flash)" else if (sec == 30) "(Padrão)" else "(Storytelling)"}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold18k,
                                        selectedLabelColor = ObsidianBackground
                                    ),
                                    modifier = Modifier.testTag("chip_duration_$sec")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Estilo de Semijoia
                        Text(text = "2. Estilo da Semijoia:", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(jewelryStyles) { style ->
                                val isSelected = uiState.aiSelectedStyle == style
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiStyle(style) },
                                    label = { Text(style, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold18k,
                                        selectedLabelColor = ObsidianBackground
                                    ),
                                    modifier = Modifier.testTag("chip_style_$style")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Categoria da Peça
                        Text(text = "3. Tipo de Peça:", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pieceTypes) { piece ->
                                val isSelected = uiState.aiSelectedPiece == piece
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiPiece(piece) },
                                    label = { Text(piece, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TikTokPink,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("chip_piece_$piece")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Estratégia de Gancho
                        Text(text = "4. Estratégia do Gancho Viral (0-3s):", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(hookStrategies) { strategy ->
                                val isSelected = uiState.aiSelectedGoal == strategy
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiGoal(strategy) },
                                    label = { Text(strategy, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TikTokCyan,
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.testTag("chip_goal_$strategy")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Preço & Detalhes da Loja
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.aiTargetPrice,
                                onValueChange = { viewModel.setAiTargetPrice(it) },
                                label = { Text("Preço / Oferta") },
                                placeholder = { Text("Ex: R$ 49,90") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold18k,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_target_price_input")
                            )

                            OutlinedTextField(
                                value = uiState.aiCustomNotes,
                                onValueChange = { viewModel.setAiCustomNotes(it) },
                                label = { Text("Cupom ou Mimo") },
                                placeholder = { Text("Ex: Cupom FRETEGRATIS") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TikTokPink,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("ai_custom_notes_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button
                        GlowButton(
                            text = if (uiState.isAiGenerating) "Gerando com Gemini Flash..." else "Gerar Roteiro Faceless com Gemini API",
                            icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White) },
                            enabled = !uiState.isAiGenerating,
                            onClick = { viewModel.generateAiContent() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "generate_script_button"
                        )
                    }
                }
            }

            // Skeleton Loading State com botão Cancelar
            if (uiState.isAiGenerating) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                        border = BorderStroke(1.5.dp, TikTokPink.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("ai_generating_skeleton")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = TikTokPink, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Criando roteiro comercial Faceless com Gemini...",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Elaborando 3 variações de gancho, dividindo em tomadas macro e calculando CTA...",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelAiGeneration() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TikTokPink),
                                border = BorderStroke(1.dp, TikTokPink),
                                modifier = Modifier.testTag("cancel_ai_generation_button")
                            ) {
                                Text("Cancelar Geração", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Aviso de Erro ou Modo Offline "Modelo pronto"
            if (uiState.aiErrorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.aiIsOfflineTemplate) Gold18k.copy(alpha = 0.15f) else TikTokPink.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (uiState.aiIsOfflineTemplate) Gold18k else TikTokPink),
                        modifier = Modifier.fillMaxWidth().testTag("ai_error_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (uiState.aiIsOfflineTemplate) "📋" else "⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (uiState.aiIsOfflineTemplate) "Modo Offline (Modelo pronto)" else "Aviso de Conexão com Gemini",
                                    color = if (uiState.aiIsOfflineTemplate) Gold18k else TikTokPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = uiState.aiErrorMessage ?: "",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Exibição do Roteiro Estruturado (GeminiScriptResult)
            if (uiState.aiScriptResult != null) {
                val script = uiState.aiScriptResult!!

                // 1. Escolha de 3 Variações de Gancho (Cards lado a lado)
                if (script.ganchosVariacoes.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Escolha 1 dos 3 Ganchos Virais (0s - 3s):",
                                    color = Gold18k,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (script.isOfflineTemplate) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Gold18k.copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, Gold18k)
                                    ) {
                                        Text(
                                            text = "Modelo pronto",
                                            color = Gold18k,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(script.ganchosVariacoes) { index, hookText ->
                                    val isSelected = uiState.aiSelectedHookIndex == index
                                    Card(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .clickable { viewModel.selectHookVariation(index) }
                                            .testTag("hook_variation_card_$index"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurface
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) TikTokPink else ObsidianBorder
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Gancho ${index + 1}",
                                                    color = if (isSelected) TikTokPink else TextMuted,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = TikTokPink,
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selecionado",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = hookText,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Detalhes das Cenas Faceless e Ações
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                        border = BorderStroke(1.5.dp, Gold18k),
                        modifier = Modifier.fillMaxWidth().testTag("script_detail_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header com botão Copiar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🎬", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Estrutura do Vídeo (${uiState.aiSelectedDuration}s)",
                                        color = Gold18k,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Roteiro Gemini AI", script.toTeleprompterText())
                                        clipboard.setPrimaryClip(clip)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TikTokPink)
                                }
                            }

                            // Texto na tela
                            if (script.textoNaTela.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianBackground,
                                    border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📱 Texto na Tela (0-3s):", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = script.textoNaTela, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Cenas Faceless
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                script.cenas.forEachIndexed { idx, cena ->
                                    CenaItemView(index = idx + 1, cena = cena)
                                }
                            }

                            // CTA
                            if (script.cta.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TikTokPink.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "🛒", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(text = "CTA TikTok Shop:", color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(text = script.cta, color = TextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Legenda Post & Hashtags
                            if (script.legendaPost.isNotBlank() || script.hashtags.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ObsidianBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (script.legendaPost.isNotBlank()) {
                                            Text(text = "📝 Legenda sugerida:", color = TextMuted, fontSize = 10.sp)
                                            Text(text = script.legendaPost, color = TextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
                                        }
                                        if (script.hashtags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = script.hashtags.joinToString(" "),
                                                color = Gold18k,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Trilha Sonora
                            if (script.sugestaoDeSom.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🎵", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Estilo de Trilha: ${script.sugestaoDeSom}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Botão Salvar Roteiro (se houver peça vinculada)
                                val peca = uiState.aiSelectedPeca
                                if (pecasViewModel != null && peca != null) {
                                    Button(
                                        onClick = {
                                            pecasViewModel.saveScriptForPeca(
                                                peca = peca,
                                                scriptResult = script,
                                                duracaoSeg = uiState.aiSelectedDuration
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                                        border = BorderStroke(1.dp, Gold18k),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("save_script_to_peca_button")
                                    ) {
                                        Text(text = "💾 Salvar Roteiro", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // Botão Gravar com este Roteiro
                                Button(
                                    onClick = { viewModel.recordWithCurrentScript() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(48.dp)
                                        .testTag("record_with_script_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Gravar com Roteiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtab 2: Ganchos Virais (Hooks)
        if (uiState.aiActiveSubTab == "hooks") {
            item {
                Text(
                    text = "Ganchos Agressivos de Retenção (Primeiros 3 segundos):",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(uiState.hooks) { hook ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hook_card_${hook.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldPill(text = hook.category, icon = "🎯")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${hook.retentionScore}% Retenção",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hook.hookText,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "🧠", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = hook.psychologicalReason,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💎 Recomendado para: ${hook.recommendedPiece}",
                            color = Gold18k,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Viral Hook", hook.hookText)
                                    clipboard.setPrimaryClip(clip)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TikTokCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", color = TextPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.setHookForRecording(hook.hookText) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gravar Gancho", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Subtab 3: Além de Vídeos (Content Ideas)
        if (uiState.aiActiveSubTab == "content_ideas") {
            item {
                Text(
                    text = "Ideias de Conteúdo Além de Vídeos Gravados:",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(uiState.alternativeIdeas) { idea ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = idea.title, color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ObsidianBackground,
                                border = BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Text(
                                    text = idea.formatType,
                                    color = TikTokPink,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = idea.objective, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceHighlight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Como executar: ${idea.scriptOrCopy}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenaItemView(index: Int, cena: CenaScript) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBackground,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cena $index (${cena.segundos}s)",
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TikTokPink.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = cena.enquadramento,
                        color = TikTokPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (cena.acaoDasMaos.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = "👐 ", fontSize = 11.sp)
                    Text(
                        text = cena.acaoDasMaos,
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            if (cena.narracao.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = "🗣️ ", fontSize = 11.sp)
                    Text(
                        text = "\"${cena.narracao}\"",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
