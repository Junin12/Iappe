package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompetitorProfile
import com.example.ui.components.GoldPill
import com.example.ui.components.RetentionScoreMeter
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun TrendsAnalyticsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeSubSection by remember { mutableStateOf("trends") } // "trends", "competitors", "audit"
    var expandedCompetitorId by remember { mutableStateOf<String?>(uiState.competitors.firstOrNull()?.id) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Tendências & Concorrentes",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "Inteligência competitiva do nicho de Semijoias e auditoria de retenção",
                    color = Gold18k,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Sub-tabs: Radar / Concorrentes / Auditoria Vídeo
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
                    Triple("trends", "Radar Tendências", "🔥"),
                    Triple("competitors", "Benchmarking Perfis", "📊"),
                    Triple("audit", "Auditoria de Retenção", "🎯")
                ).forEach { (id, label, icon) ->
                    val isSelected = activeSubSection == id
                    Surface(
                        onClick = { activeSubSection = id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) TikTokPink else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trends_subtab_$id")
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

        // 1. Radar de Tendências
        if (activeSubSection == "trends") {
            item {
                Text(
                    text = "Tendências Emergentes no TikTok Shop (Semijoias):",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(uiState.trends) { trend ->
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
                            GoldPill(text = trend.tagCategory, icon = "⚡")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TikTokPink.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = trend.growthPercent,
                                    color = TikTokPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = trend.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = trend.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "💡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Melhoria Estratégica Recomendada:",
                                        color = Gold18k,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = trend.actionAdvice,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Benchmarking de Perfis Concorrentes
        if (activeSubSection == "competitors") {
            item {
                Text(
                    text = "Perfis Top Sellers de Semijoias no TikTok:",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(uiState.competitors) { comp ->
                val isExpanded = expandedCompetitorId == comp.id

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) ObsidianSurfaceElevated else ObsidianSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) Gold18k else ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("competitor_card_${comp.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCompetitorId = if (isExpanded) null else comp.id
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianSurfaceHighlight)
                                        .border(1.5.dp, Gold18k, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = comp.avatarEmoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = comp.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = comp.handle,
                                        color = TikTokPink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            IconButton(onClick = { expandedCompetitorId = if (isExpanded) null else comp.id }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expandir perfil",
                                    tint = Gold18k
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
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
                                    Text(text = "Seguidores", color = TextMuted, fontSize = 10.sp)
                                    Text(text = comp.followersCount, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurfaceHighlight,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "Engajamento", color = TextMuted, fontSize = 10.sp)
                                    Text(text = comp.engagementRate, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurfaceHighlight,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "Vendas Est.", color = TextMuted, fontSize = 10.sp)
                                    Text(text = comp.monthlySalesEstimated, color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                HorizontalDivider(color = ObsidianBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "🏆 FORMATO CAMPEÃO:",
                                    color = TikTokCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = comp.winningFormat,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "🎯 ESTRATÉGIA DE RETENÇÃO & VENDAS:",
                                    color = Gold18k,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = comp.growthStrategy,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Peça Mais Vendida:", color = TextMuted, fontSize = 10.sp)
                                        Text(text = comp.topSellingJewel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Melhores Horários:", color = TextMuted, fontSize = 10.sp)
                                        Text(text = comp.bestPostHours, color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "🏷️ Hashtags usadas: ${comp.topHashtags.joinToString(" ")}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Auditoria & Diagnóstico Estratégico do Vídeo
        if (activeSubSection == "audit") {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Auditoria de Potencial Viral do Seu Vídeo",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Marque os elementos presentes no seu vídeo para simular a pontuação de retenção:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        RetentionScoreMeter(score = uiState.auditScore)

                        Spacer(modifier = Modifier.height(10.dp))

                        val statusFeedback = when {
                            uiState.auditScore >= 80 -> "🔥 Alto Potencial de Viralização e Vendas!"
                            uiState.auditScore >= 60 -> "⚡ Bom vídeo, mas pode melhorar a retenção dos primeiros 3s."
                            else -> "⚠️ Risco de alta evasão. Ajuste os ganchos antes de postar."
                        }

                        Text(
                            text = statusFeedback,
                            color = if (uiState.auditScore >= 80) SuccessGreen else if (uiState.auditScore >= 60) Gold18k else WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = ObsidianBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Criteria checkboxes
                        uiState.auditCriteria.forEach { crit ->
                            Surface(
                                onClick = { viewModel.toggleAuditCriterion(crit.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (crit.isChecked) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (crit.isChecked) TikTokPink.copy(alpha = 0.6f) else ObsidianBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("audit_crit_${crit.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = crit.isChecked,
                                            onCheckedChange = { viewModel.toggleAuditCriterion(crit.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = TikTokPink,
                                                uncheckedColor = ObsidianBorder
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = crit.name,
                                                color = if (crit.isChecked) TextPrimary else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = crit.description,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "+${crit.weightPoints}pts",
                                        color = if (crit.isChecked) SuccessGreen else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
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
