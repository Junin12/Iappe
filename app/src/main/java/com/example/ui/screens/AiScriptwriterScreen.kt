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
import com.example.ui.components.CopyableTextCard
import com.example.ui.components.GlowButton
import com.example.ui.components.GoldPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScriptwriterScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val pieceTypes = listOf("Colar & Choker", "Brinco Gota", "Anel Solitário", "Pulseira Riviera", "Mix 3-Camadas")
    val goalTypes = listOf("Venda Rápida TikTok Shop", "Quebra de Objeção 'Preteja?'", "Viralização / Topo Funil")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Roteirização com IA & Hooks",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "Ganchos hipnóticos para os primeiros 3 segundos e roteiros 100% sem aparecer",
                    color = Gold18k,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Subtabs: Hooks / Roteiro Completo / Além do Vídeo
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
                    Triple("hooks", "Ganchos Virais", "⚡"),
                    Triple("full_script", "Roteiro Faceless", "🎬"),
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

        // Subtab 1: Ganchos Virais (Hooks)
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
                                onClick = { viewModel.applyHookToEditor(hook.hookText) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Usar no Vídeo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Subtab 2: Gerador de Roteiro Completo com IA
        if (uiState.aiActiveSubTab == "full_script") {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Parametrizar Roteiro com IA:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Tipo de Semijoia:", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pieceTypes) { piece ->
                                val isSelected = uiState.aiSelectedPiece == piece
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiPiece(piece) },
                                    label = { Text(piece, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold18k,
                                        selectedLabelColor = ObsidianBackground
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Objetivo do Vídeo:", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(goalTypes) { goal ->
                                val isSelected = uiState.aiSelectedGoal == goal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiGoal(goal) },
                                    label = { Text(goal, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TikTokPink,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GlowButton(
                            text = if (uiState.isAiGenerating) "Escrevendo com IA..." else "Gerar Roteiro Faceless com IA",
                            icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White) },
                            enabled = !uiState.isAiGenerating,
                            onClick = { viewModel.generateAiContent() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "generate_script_button"
                        )
                    }
                }
            }

            if (uiState.aiGeneratedContent.isNotBlank()) {
                item {
                    Column {
                        CopyableTextCard(
                            title = "✨ Roteiro Pronto para Gravar",
                            textToCopy = uiState.aiGeneratedContent,
                            onCopied = { }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.openTeleprompter(uiState.aiGeneratedContent) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                            border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_generated_teleprompter_button")
                        ) {
                            Text(text = "🎙️ Gravar este Roteiro com o Teleprompter", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Subtab 3: Além de Vídeos (Carrosséis, Live Shop, Enquetes)
        if (uiState.aiActiveSubTab == "content_ideas") {
            item {
                Text(
                    text = "Estratégias de Conteúdo Além de Vídeos:",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(uiState.alternativeIdeas) { idea ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldPill(text = idea.formatType, icon = "📸")
                            Text(
                                text = idea.estimatedReach,
                                color = TikTokCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = idea.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = idea.objective,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "📋 Passo a Passo de Execução:",
                            color = Gold18k,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        idea.stepByStep.forEach { step ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "•", color = TikTokPink, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = step, color = TextPrimary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        CopyableTextCard(
                            title = "Legenda / Copy Sugerida:",
                            textToCopy = idea.scriptOrCopy,
                            onCopied = { }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
