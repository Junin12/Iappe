package com.example

import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.data.metrics.MetricsCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MetricsCalculatorTest {

    @Test
    fun testEngagementRateCalculation() {
        val views = 1000L
        val curtidas = 50L
        val comentarios = 10L
        val compartilhamentos = 5L
        val salvamentos = 15L

        val rate = MetricsCalculator.calculateEngagementRate(
            views = views,
            curtidas = curtidas,
            comentarios = comentarios,
            compartilhamentos = compartilhamentos,
            salvamentos = salvamentos
        )

        // (50 + 10 + 5 + 15) / 1000 * 100 = 80 / 10 = 8.0%
        assertEquals(8.0, rate, 0.001)
    }

    @Test
    fun testEngagementRateDivisionByZeroWhenViewsZero() {
        // Quando views for 0 ou negativo, nunca deve gerar NaN ou divisão por zero
        val rateZero = MetricsCalculator.calculateEngagementRate(
            views = 0L,
            curtidas = 10L,
            comentarios = 5L,
            compartilhamentos = 2L,
            salvamentos = 1L
        )
        assertEquals(0.0, rateZero, 0.0)
        assertFalse(rateZero.isNaN())
        assertFalse(rateZero.isInfinite())

        val rateNegative = MetricsCalculator.calculateEngagementRate(
            views = -100L,
            curtidas = 5L,
            comentarios = 1L,
            compartilhamentos = 0L,
            salvamentos = 0L
        )
        assertEquals(0.0, rateNegative, 0.0)
    }

    @Test
    fun testScheduleRecommendationWithFewVideos() {
        // Menos de 5 vídeos com métricas: deve retornar hasEnoughData = false com contagem honesta
        val videos = listOf(
            createTestVideo(id = 1L, pecaId = 1L, views = 500L, status = StatusVideoOptions.POSTADO),
            createTestVideo(id = 2L, pecaId = 1L, views = 800L, status = StatusVideoOptions.POSTADO),
            createTestVideo(id = 3L, pecaId = 2L, views = 300L, status = StatusVideoOptions.POSTADO)
        )

        val recommendation = MetricsCalculator.calculateBestSchedule(videos)

        assertFalse(recommendation.hasEnoughData)
        assertEquals(3, recommendation.videosCountWithMetrics)
        assertEquals(2, recommendation.missingVideosCount)
        assertNull(recommendation.bestDayOfWeek)
        assertNull(recommendation.bestTimeSlot)
    }

    @Test
    fun testScheduleRecommendationWithZeroVideos() {
        val recommendation = MetricsCalculator.calculateBestSchedule(emptyList())

        assertFalse(recommendation.hasEnoughData)
        assertEquals(0, recommendation.videosCountWithMetrics)
        assertEquals(5, recommendation.missingVideosCount)
    }

    @Test
    fun testScheduleRecommendationWithFiveOrMoreVideos() {
        val cal = Calendar.getInstance()
        // Configura uma Quarta-feira às 20h (Noite Pico)
        cal.set(2026, Calendar.OCTOBER, 21, 20, 0, 0) // Quarta-feira
        val wednesdayNight = cal.timeInMillis

        // Configura uma Segunda-feira às 08h (Manhã)
        cal.set(2026, Calendar.OCTOBER, 19, 8, 0, 0) // Segunda-feira
        val mondayMorning = cal.timeInMillis

        val videos = listOf(
            createTestVideo(id = 1L, pecaId = 1L, views = 5000L, postTime = wednesdayNight),
            createTestVideo(id = 2L, pecaId = 1L, views = 4500L, postTime = wednesdayNight),
            createTestVideo(id = 3L, pecaId = 2L, views = 6000L, postTime = wednesdayNight),
            createTestVideo(id = 4L, pecaId = 2L, views = 500L, postTime = mondayMorning),
            createTestVideo(id = 5L, pecaId = 3L, views = 300L, postTime = mondayMorning)
        )

        val recommendation = MetricsCalculator.calculateBestSchedule(videos)

        assertTrue(recommendation.hasEnoughData)
        assertEquals(5, recommendation.videosCountWithMetrics)
        assertEquals(0, recommendation.missingVideosCount)
        assertEquals("Quarta-feira", recommendation.bestDayOfWeek)
        assertEquals("18h - 22h (Noite Pico)", recommendation.bestTimeSlot)
        assertTrue(recommendation.avgViewsOnBestDay > 4000L)
    }

    @Test
    fun testPieceSalesRanking() {
        val pecas = listOf(
            PecaEntity(id = 1L, nome = "Colar Riviera", tipo = "Colar", banho = "Ouro 18k", preco = 120.0),
            PecaEntity(id = 2L, nome = "Brinco Gota", tipo = "Brinco", banho = "Ouro 18k", preco = 45.0),
            PecaEntity(id = 3L, nome = "Anel Solitário", tipo = "Anel", banho = "Ródio", preco = 89.0)
        )

        val videos = listOf(
            createTestVideo(id = 1L, pecaId = 1L, views = 1000L, vendas = 15L),
            createTestVideo(id = 2L, pecaId = 1L, views = 1500L, vendas = 25L),
            createTestVideo(id = 3L, pecaId = 2L, views = 3000L, vendas = 8L),
            createTestVideo(id = 4L, pecaId = 3L, views = 500L, vendas = 50L)
        )

        val report = MetricsCalculator.calculateReport(videos, pecas)

        assertEquals(3, report.topSellingPecas.size)
        // Anel Solitário tem 50 vendas (1º colocado)
        assertEquals("Anel Solitário", report.topSellingPecas[0].pecaNome)
        assertEquals(50L, report.topSellingPecas[0].totalVendas)

        // Colar Riviera tem 15 + 25 = 40 vendas (2º colocado)
        assertEquals("Colar Riviera", report.topSellingPecas[1].pecaNome)
        assertEquals(40L, report.topSellingPecas[1].totalVendas)

        // Brinco Gota tem 8 vendas (3º colocado)
        assertEquals("Brinco Gota", report.topSellingPecas[2].pecaNome)
        assertEquals(8L, report.topSellingPecas[2].totalVendas)
    }

    @Test
    fun testHookRankingByAverageViews() {
        val pecas = listOf(
            PecaEntity(id = 1L, nome = "Colar Riviera", tipo = "Colar", banho = "Ouro 18k", preco = 120.0)
        )

        val videos = listOf(
            createTestVideo(id = 1L, pecaId = 1L, views = 10000L, gancho = "Disseram que ia escurecer..."),
            createTestVideo(id = 2L, pecaId = 1L, views = 8000L, gancho = "Disseram que ia escurecer..."),
            createTestVideo(id = 3L, pecaId = 1L, views = 1200L, gancho = "Olha esse brinco lindo"),
            createTestVideo(id = 4L, pecaId = 1L, views = 800L, gancho = "Olha esse brinco lindo")
        )

        val report = MetricsCalculator.calculateReport(videos, pecas)

        assertTrue(report.topHooks.isNotEmpty())
        val topHook = report.topHooks.first()
        assertEquals("Disseram que ia escurecer...", topHook.ganchoResumo)
        assertEquals(9000L, topHook.avgViews)
        assertEquals(2, topHook.totalVideos)
    }

    @Test
    fun testWeeklyAndMonthlyMetricsComparison() {
        val now = 1770000000000L // Ponto fixo no tempo
        val oneDay = 24L * 60L * 60L * 1000L

        val pecas = listOf(PecaEntity(id = 1L, nome = "Choker Fita", tipo = "Colar", banho = "Ouro 18k", preco = 79.90))

        val videos = listOf(
            // Semana atual (nos últimos 7 dias)
            createTestVideo(id = 1L, pecaId = 1L, views = 2000L, vendas = 10L, postTime = now - (2 * oneDay)),
            createTestVideo(id = 2L, pecaId = 1L, views = 3000L, vendas = 15L, postTime = now - (4 * oneDay)),
            // Semana anterior (entre 7 e 14 dias atrás)
            createTestVideo(id = 3L, pecaId = 1L, views = 2500L, vendas = 10L, postTime = now - (9 * oneDay))
        )

        val report = MetricsCalculator.calculateReport(videos, pecas, nowMs = now)

        assertEquals(5000L, report.weekMetrics.totalViews)
        assertEquals(25L, report.weekMetrics.totalVendas)

        // Comparação da semana: atual 5000 vs anterior 2500 -> +100.0%
        assertNotNull(report.weekMetrics.viewsChangePercent)
        assertEquals(100.0, report.weekMetrics.viewsChangePercent!!, 0.01)

        // Vendas: atual 25 vs anterior 10 -> +150.0%
        assertNotNull(report.weekMetrics.vendasChangePercent)
        assertEquals(150.0, report.weekMetrics.vendasChangePercent!!, 0.01)
    }

    private fun createTestVideo(
        id: Long,
        pecaId: Long,
        views: Long = 0L,
        curtidas: Long = 0L,
        vendas: Long = 0L,
        gancho: String = "Gancho de teste",
        status: String = StatusVideoOptions.POSTADO,
        postTime: Long = System.currentTimeMillis()
    ): VideoEntity {
        return VideoEntity(
            id = id,
            pecaId = pecaId,
            uri = "content://media/test/$id",
            duracaoSeg = 15,
            gancho = gancho,
            roteiroJson = "{}",
            legenda = "Legenda teste",
            hashtags = "#semijoias",
            status = status,
            dataAgendada = postTime,
            dataPostagem = postTime,
            views = views,
            curtidas = curtidas,
            comentarios = 0L,
            compartilhamentos = 0L,
            salvamentos = 0L,
            vendas = vendas
        )
    }
}
