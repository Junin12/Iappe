package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoTemplate
import com.example.ui.components.GlowButton
import com.example.ui.components.GoldPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun TemplatesScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedTemplateId by remember { mutableStateOf<String?>(uiState.templates.firstOrNull()?.id) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Templates Validados TikTok Shop",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "Formatos gravados em 15-25 segundos que mais geram vendas por impulso",
                    color = Gold18k,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(uiState.templates) { template ->
            val isExpanded = expandedTemplateId == template.id

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) ObsidianSurfaceElevated else ObsidianSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isExpanded) TikTokPink else ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("template_card_${template.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header of Template
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTemplateId = if (isExpanded) null else template.id
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GoldPill(text = template.nicheCategory, icon = "🔥")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = template.difficulty,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = template.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = template.hookSummary,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = if (isExpanded) 4 else 2
                            )
                        }

                        IconButton(
                            onClick = {
                                expandedTemplateId = if (isExpanded) null else template.id
                            }
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expandir detalhes",
                                tint = TikTokPink
                            )
                        }
                    }

                    // Stat Pills
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Retenção", color = TextMuted, fontSize = 10.sp)
                                Text(text = template.avgRetentionRate, color = TikTokCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Conversão Shop", color = TextMuted, fontSize = 10.sp)
                                Text(text = template.salesConversionRate, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceHighlight,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Formato", color = TextMuted, fontSize = 10.sp)
                                Text(text = template.filmingStyle, color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    // Expanded Steps & Script Breakdown
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = ObsidianBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "⏱️ ROTEIRO SEGUNDO A SEGUNDO:",
                                color = Gold18k,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            template.steps.forEachIndexed { index, step ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianSurfaceHighlight,
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${step.timeRange} • ${step.stage}",
                                                color = TikTokPink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🎬 Câmera: ${step.visualAction}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "🎙️ Fala/Áudio: ${step.spokenAudio}",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "Texto tela: ${step.onScreenText}",
                                                color = Gold18k,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Audio & Hashtags recommendation
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "🎵 Áudio Recomendado: ${template.suggestedAudio}",
                                        color = TikTokCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🏷️ Hashtags: ${template.recommendedHashtags.joinToString(" ")}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val fullScript = template.steps.joinToString("\n\n") { s ->
                                            "${s.timeRange} (${s.stage}):\nFala: \"${s.spokenAudio}\"\nTexto na tela: ${s.onScreenText}"
                                        }
                                        viewModel.openTeleprompter(fullScript)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                                    border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "🎙️ Teleprompter", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                GlowButton(
                                    text = "Usar no Estúdio",
                                    icon = { Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Color.White) },
                                    onClick = { viewModel.applyTemplateToEditor(template) },
                                    modifier = Modifier.weight(1.3f),
                                    testTag = "use_template_button_${template.id}"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
