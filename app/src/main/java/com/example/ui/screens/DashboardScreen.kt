package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.metrics.HookRanking
import com.example.data.metrics.PecaSalesRanking
import com.example.data.metrics.VideoMetricsSummary
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Painel de Métricas Reais",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Calculado exclusivamente a partir dos seus posts e vendas no TikTok Shop",
                            color = Gold18k,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✓", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "100% Real", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Section: Resumo da Semana e Mês (Cards Principais)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Views e Vendas da Semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = "Views da Semana",
                        value = "${report.weekMetrics.totalViews}",
                        changePercent = report.weekMetrics.viewsChangePercent,
                        icon = "👁️",
                        accentColor = TikTokCyan,
                        modifier = Modifier.weight(1f).testTag("card_views_semana")
                    )

                    MetricHighlightCard(
                        title = "Vendas da Semana",
                        value = "${report.weekMetrics.totalVendas} un",
                        changePercent = report.weekMetrics.vendasChangePercent,
                        icon = "🛒",
                        accentColor = Gold18k,
                        modifier = Modifier.weight(1f).testTag("card_vendas_semana")
                    )
                }

                // Views e Vendas do Mês + Engajamento Médio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricHighlightCard(
                        title = "Views do Mês",
                        value = "${report.monthMetrics.totalViews}",
                        changePercent = report.monthMetrics.viewsChangePercent,
                        icon = "📅",
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f).testTag("card_views_mes")
                    )

                    MetricHighlightCard(
                        title = "Vendas do Mês",
                        value = "${report.monthMetrics.totalVendas} un",
                        changePercent = report.monthMetrics.vendasChangePercent,
                        icon = "💎",
                        accentColor = TikTokPink,
                        modifier = Modifier.weight(1f).testTag("card_vendas_mes")
                    )
                }
            }
        }

        // Card de Taxa Média de Engajamento
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Taxa Média de Engajamento:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", report.overallAvgEngagementRate)}%",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "(curtidas + comentários + compartilhamentos + salvamentos) ÷ views",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TikTokPink.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🔥 ${report.postedVideosCount} posts",
                            color = TikTokPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section: Melhor Dia e Horário de Postagem (100% dos posts dela)
        item {
            val rec = report.scheduleRecommendation
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                border = BorderStroke(1.dp, if (rec.hasEnoughData) Gold18k else ObsidianBorder),
                modifier = Modifier.fillMaxWidth().testTag("schedule_recommendation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏰", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Melhor Dia e Horário para Postar",
                                color = Gold18k,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (rec.hasEnoughData) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Baseado nos seus vídeos",
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (rec.hasEnoughData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Melhor Dia da Semana:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = rec.bestDayOfWeek ?: "Quarta-feira",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Column {
                                Text(text = "Faixa de Maior Retenção:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = rec.bestTimeSlot ?: "18h - 22h",
                                    color = TikTokPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Média de visualizações no seu melhor dia: ${rec.avgViewsOnBestDay} views.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        // Estado Vazio Explicativo
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "⏳", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Aguardando mais dados reais",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Registre métricas de mais ${rec.missingVideosCount} vídeo(s) postado(s) para o painel identificar os dias e horários em que suas clientes mais compram e assistem.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Ranking das Peças que Mais Vendem
        item {
            Text(
                text = "Ranking: Peças que Mais Vendem no TikTok Shop",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (report.topSellingPecas.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "Nenhuma venda registrada ainda nos seus vídeos postados. Atualize as métricas após conferir o TikTok Shop!"
                )
            }
        } else {
            items(report.topSellingPecas.take(5)) { ranking ->
                PecaRankingCard(ranking = ranking)
            }
        }

        // Section: Ranking dos Ganchos com Melhor Média de Views
        item {
            Text(
                text = "Ganchos com Melhor Média de Retenção e Views",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (report.topHooks.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "Nenhum vídeo postado com métricas de visualização ainda."
                )
            }
        } else {
            items(report.topHooks.take(5)) { hookRanking ->
                HookRankingCard(hook = hookRanking)
            }
        }

        // Section: Histórico de Vídeos Postados & Botão Atualizar Métricas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vídeos com Status POSTADO (${report.recentPostedVideos.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        if (report.recentPostedVideos.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "Nenhum vídeo com status POSTADO. Grave ou edite um vídeo na aba 'Peças' ou 'Planejador' para começar!"
                )
            }
        } else {
            items(report.recentPostedVideos) { summary ->
                PostedVideoMetricItem(
                    summary = summary,
                    onEditMetrics = {
                        val video = uiState.allVideos.find { it.id == summary.videoId }
                        if (video != null) viewModel.openUpdateMetrics(video)
                    }
                )
            }
        }
    }

    // Modal de Atualização de Métricas
    if (uiState.editingMetricsVideo != null) {
        val video = uiState.editingMetricsVideo!!
        UpdateMetricsModal(
            pecaNome = uiState.editingPecaNome,
            videoGancho = video.gancho,
            views = uiState.viewsInput,
            curtidas = uiState.curtidasInput,
            comentarios = uiState.comentariosInput,
            compartilhamentos = uiState.compartilhamentosInput,
            salvamentos = uiState.salvamentosInput,
            vendas = uiState.vendasInput,
            dataPostagem = uiState.dataPostagemInput,
            onViewsChange = { viewModel.updateViewsInput(it) },
            onCurtidasChange = { viewModel.updateCurtidasInput(it) },
            onComentariosChange = { viewModel.updateComentariosInput(it) },
            onCompartilhamentosChange = { viewModel.updateCompartilhamentosInput(it) },
            onSalvamentosChange = { viewModel.updateSalvamentosInput(it) },
            onVendasChange = { viewModel.updateVendasInput(it) },
            onDataPostagemChange = { viewModel.updateDataPostagemInput(it) },
            onDismiss = { viewModel.dismissUpdateMetrics() },
            onSave = { viewModel.saveMetrics() }
        )
    }
}

@Composable
private fun MetricHighlightCard(
    title: String,
    value: String,
    changePercent: Double?,
    icon: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = icon, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (changePercent != null) {
                val isPositive = changePercent >= 0
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.1f", changePercent)}% vs anterior",
                    color = if (isPositive) SuccessGreen else TikTokPink,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Primeiro período",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun PecaRankingCard(ranking: PecaSalesRanking) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ranking.pecaNome, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "${ranking.pecaTipo} • R$ ${String.format(java.util.Locale.US, "%.2f", ranking.preco)} • ${ranking.totalViews} views totais",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Gold18k.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Gold18k)
            ) {
                Text(
                    text = "${ranking.totalVendas} vendas",
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HookRankingCard(hook: HookRanking) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${hook.ganchoResumo}\"",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "${hook.totalVideos} vídeo(s) com este gancho • ${String.format(java.util.Locale.US, "%.1f", hook.avgEngagementRate)}% engajamento",
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TikTokCyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, TikTokCyan)
            ) {
                Text(
                    text = "${hook.avgViews} views méd.",
                    color = TikTokCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PostedVideoMetricItem(
    summary: VideoMetricsSummary,
    onEditMetrics: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().testTag("posted_video_item_${summary.videoId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.pecaNome,
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                TextButton(
                    onClick = onEditMetrics,
                    modifier = Modifier.testTag("edit_metrics_button_${summary.videoId}")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Atualizar Métricas", color = TikTokPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "\"${summary.gancho}\"",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Linha de métricas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Views: ${summary.views}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "Likes: ${summary.curtidas}", color = TextMuted, fontSize = 11.sp)
                Text(text = "Vendas: ${summary.vendas}", color = Gold18k, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "Engaj: ${String.format(java.util.Locale.US, "%.1f", summary.engagementRate)}%", color = TikTokPink, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}
