package com.example.data.metrics

import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import java.util.Calendar
import java.util.Locale

data class PeriodMetrics(
    val totalViews: Long = 0L,
    val totalVendas: Long = 0L,
    val viewsChangePercent: Double? = null,
    val vendasChangePercent: Double? = null
)

data class VideoMetricsSummary(
    val videoId: Long,
    val pecaId: Long,
    val pecaNome: String,
    val gancho: String,
    val views: Long,
    val curtidas: Long,
    val comentarios: Long,
    val compartilhamentos: Long,
    val salvamentos: Long,
    val vendas: Long,
    val engagementRate: Double,
    val dataPostagem: Long
)

data class PecaSalesRanking(
    val pecaId: Long,
    val pecaNome: String,
    val pecaTipo: String,
    val preco: Double,
    val totalVendas: Long,
    val totalViews: Long,
    val videoCount: Int
)

data class HookRanking(
    val ganchoResumo: String,
    val avgViews: Long,
    val totalVideos: Int,
    val avgEngagementRate: Double
)

data class BestScheduleRecommendation(
    val bestDayOfWeek: String? = null,
    val bestTimeSlot: String? = null,
    val avgViewsOnBestDay: Long = 0L,
    val hasEnoughData: Boolean = false,
    val videosCountWithMetrics: Int = 0,
    val missingVideosCount: Int = 5
)

data class DashboardMetricsReport(
    val weekMetrics: PeriodMetrics = PeriodMetrics(),
    val monthMetrics: PeriodMetrics = PeriodMetrics(),
    val overallAvgEngagementRate: Double = 0.0,
    val topSellingPecas: List<PecaSalesRanking> = emptyList(),
    val topHooks: List<HookRanking> = emptyList(),
    val scheduleRecommendation: BestScheduleRecommendation = BestScheduleRecommendation(),
    val postedVideosCount: Int = 0,
    val recentPostedVideos: List<VideoMetricsSummary> = emptyList()
)

object MetricsCalculator {

    private const val MIN_VIDEOS_FOR_SCHEDULE_RECOMMENDATION = 5
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val SEVEN_DAYS_MS = 7L * ONE_DAY_MS
    private const val THIRTY_DAYS_MS = 30L * ONE_DAY_MS

    /**
     * Calcula a taxa de engajamento percentual:
     * (curtidas + comentários + compartilhamentos + salvamentos) / views * 100
     * Trata estritamente a divisão por zero quando views == 0.
     */
    fun calculateEngagementRate(
        views: Long,
        curtidas: Long,
        comentarios: Long,
        compartilhamentos: Long,
        salvamentos: Long
    ): Double {
        if (views <= 0L) return 0.0
        val totalInteractions = curtidas + comentarios + compartilhamentos + salvamentos
        return (totalInteractions.toDouble() / views.toDouble()) * 100.0
    }

    /**
     * Gera o relatório consolidado do Painel a partir dos dados reais do Room.
     */
    fun calculateReport(
        videos: List<VideoEntity>,
        pecas: List<PecaEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): DashboardMetricsReport {
        val pecaMap = pecas.associateBy { it.id }

        // Filtra vídeos postados
        val postedVideos = videos.filter { it.status == StatusVideoOptions.POSTADO }
        val videosWithMetrics = postedVideos.filter { it.views > 0 || it.curtidas > 0 || it.vendas > 0 }

        // 1. Resumos individuais com taxa de engajamento
        val summaries = postedVideos.map { video ->
            val peca = pecaMap[video.pecaId]
            val engRate = calculateEngagementRate(
                views = video.views,
                curtidas = video.curtidas,
                comentarios = video.comentarios,
                compartilhamentos = video.compartilhamentos,
                salvamentos = video.salvamentos
            )
            VideoMetricsSummary(
                videoId = video.id,
                pecaId = video.pecaId,
                pecaNome = peca?.nome ?: "Semijoia #${video.pecaId}",
                gancho = video.gancho.ifBlank { "Sem gancho registrado" },
                views = video.views,
                curtidas = video.curtidas,
                comentarios = video.comentarios,
                compartilhamentos = video.compartilhamentos,
                salvamentos = video.salvamentos,
                vendas = video.vendas,
                engagementRate = engRate,
                dataPostagem = if (video.dataPostagem != null && video.dataPostagem > 0) video.dataPostagem else (video.dataAgendada ?: nowMs)
            )
        }.sortedByDescending { it.dataPostagem }

        // 2. Métricas da Semana (últimos 7 dias vs 7 dias anteriores)
        val currentWeekVideos = summaries.filter { it.dataPostagem in (nowMs - SEVEN_DAYS_MS)..nowMs }
        val previousWeekVideos = summaries.filter { it.dataPostagem in (nowMs - (2 * SEVEN_DAYS_MS))..<(nowMs - SEVEN_DAYS_MS) }

        val weekViews = currentWeekVideos.sumOf { it.views }
        val weekVendas = currentWeekVideos.sumOf { it.vendas }
        val prevWeekViews = previousWeekVideos.sumOf { it.views }
        val prevWeekVendas = previousWeekVideos.sumOf { it.vendas }

        val weekMetrics = PeriodMetrics(
            totalViews = weekViews,
            totalVendas = weekVendas,
            viewsChangePercent = calculatePercentageChange(weekViews, prevWeekViews),
            vendasChangePercent = calculatePercentageChange(weekVendas, prevWeekVendas)
        )

        // 3. Métricas do Mês (últimos 30 dias vs 30 dias anteriores)
        val currentMonthVideos = summaries.filter { it.dataPostagem in (nowMs - THIRTY_DAYS_MS)..nowMs }
        val previousMonthVideos = summaries.filter { it.dataPostagem in (nowMs - (2 * THIRTY_DAYS_MS))..<(nowMs - THIRTY_DAYS_MS) }

        val monthViews = currentMonthVideos.sumOf { it.views }
        val monthVendas = currentMonthVideos.sumOf { it.vendas }
        val prevMonthViews = previousMonthVideos.sumOf { it.views }
        val prevMonthVendas = previousMonthVideos.sumOf { it.vendas }

        val monthMetrics = PeriodMetrics(
            totalViews = monthViews,
            totalVendas = monthVendas,
            viewsChangePercent = calculatePercentageChange(monthViews, prevMonthViews),
            vendasChangePercent = calculatePercentageChange(monthVendas, prevMonthVendas)
        )

        // 4. Taxa de engajamento média geral
        val overallAvgEngagement = if (summaries.isNotEmpty()) {
            val sumRates = summaries.sumOf { it.engagementRate }
            sumRates / summaries.size.toDouble()
        } else 0.0

        // 5. Ranking das peças que mais vendem
        val pecaSalesGrouped = summaries.groupBy { it.pecaId }
        val topPecas = pecaSalesGrouped.map { (pecaId, vList) ->
            val peca = pecaMap[pecaId]
            PecaSalesRanking(
                pecaId = pecaId,
                pecaNome = peca?.nome ?: "Semijoia #$pecaId",
                pecaTipo = peca?.tipo ?: "Colar",
                preco = peca?.preco ?: 0.0,
                totalVendas = vList.sumOf { it.vendas },
                totalViews = vList.sumOf { it.views },
                videoCount = vList.size
            )
        }.sortedByDescending { it.totalVendas }

        // 6. Ranking dos tipos de gancho com melhor média de views
        val hookGrouped = summaries.groupBy { normalizeHookForRanking(it.gancho) }
        val topHooks = hookGrouped.map { (normalizedHook, vList) ->
            val avgViews = vList.map { it.views }.average().toLong()
            val avgEng = vList.map { it.engagementRate }.average()
            HookRanking(
                ganchoResumo = normalizedHook,
                avgViews = avgViews,
                totalVideos = vList.size,
                avgEngagementRate = if (avgEng.isNaN()) 0.0 else avgEng
            )
        }.sortedByDescending { it.avgViews }

        // 7. Melhor dia da semana e melhor faixa de horário (apenas se >= 5 vídeos com métricas)
        val scheduleRec = calculateBestSchedule(videosWithMetrics)

        return DashboardMetricsReport(
            weekMetrics = weekMetrics,
            monthMetrics = monthMetrics,
            overallAvgEngagementRate = overallAvgEngagement,
            topSellingPecas = topPecas,
            topHooks = topHooks,
            scheduleRecommendation = scheduleRec,
            postedVideosCount = postedVideos.size,
            recentPostedVideos = summaries
        )
    }

    private fun calculatePercentageChange(current: Long, previous: Long): Double? {
        if (previous == 0L) {
            return if (current > 0L) 100.0 else null
        }
        return ((current - previous).toDouble() / previous.toDouble()) * 100.0
    }

    private fun normalizeHookForRanking(hook: String): String {
        val trimmed = hook.trim().removeSurrounding("\"").removeSurrounding("'")
        if (trimmed.length > 50) {
            return trimmed.take(47) + "..."
        }
        return if (trimmed.isBlank()) "Gancho Geral" else trimmed
    }

    /**
     * Calcula o melhor dia da semana e melhor faixa de horário
     * SOMENTE a partir dos posts da criadora com métricas.
     */
    fun calculateBestSchedule(videosWithMetrics: List<VideoEntity>): BestScheduleRecommendation {
        val count = videosWithMetrics.size
        if (count < MIN_VIDEOS_FOR_SCHEDULE_RECOMMENDATION) {
            return BestScheduleRecommendation(
                hasEnoughData = false,
                videosCountWithMetrics = count,
                missingVideosCount = (MIN_VIDEOS_FOR_SCHEDULE_RECOMMENDATION - count).coerceAtLeast(1)
            )
        }

        val cal = Calendar.getInstance()
        val viewsByDay = mutableMapOf<Int, MutableList<Long>>()
        val viewsByTimeSlot = mutableMapOf<String, MutableList<Long>>()

        videosWithMetrics.forEach { video ->
            val postTime = if (video.dataPostagem != null && video.dataPostagem > 0) video.dataPostagem else (video.dataAgendada ?: 0L)
            if (postTime > 0L) {
                cal.timeInMillis = postTime
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                viewsByDay.getOrPut(dayOfWeek) { mutableListOf() }.add(video.views)

                val slot = when (hour) {
                    in 6..10 -> "06h - 11h (Manhã)"
                    in 11..14 -> "11h - 15h (Almoço)"
                    in 15..17 -> "15h - 18h (Tarde)"
                    in 18..21 -> "18h - 22h (Noite Pico)"
                    else -> "22h - 02h (Fim de Noite)"
                }
                viewsByTimeSlot.getOrPut(slot) { mutableListOf() }.add(video.views)
            }
        }

        val bestDayEntry = viewsByDay.maxByOrNull { entry -> entry.value.average() }
        val bestSlotEntry = viewsByTimeSlot.maxByOrNull { entry -> entry.value.average() }

        val bestDayName = bestDayEntry?.key?.let { dayNumberToPtBr(it) } ?: "Quarta-feira"
        val bestSlotName = bestSlotEntry?.key ?: "18h - 22h (Noite Pico)"
        val avgViewsBestDay = bestDayEntry?.value?.average()?.toLong() ?: 0L

        return BestScheduleRecommendation(
            bestDayOfWeek = bestDayName,
            bestTimeSlot = bestSlotName,
            avgViewsOnBestDay = avgViewsBestDay,
            hasEnoughData = true,
            videosCountWithMetrics = count,
            missingVideosCount = 0
        )
    }

    private fun dayNumberToPtBr(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Segunda-feira"
            Calendar.TUESDAY -> "Terça-feira"
            Calendar.WEDNESDAY -> "Quarta-feira"
            Calendar.THURSDAY -> "Quinta-feira"
            Calendar.FRIDAY -> "Sexta-feira"
            Calendar.SATURDAY -> "Sábado"
            else -> "Quarta-feira"
        }
    }
}
