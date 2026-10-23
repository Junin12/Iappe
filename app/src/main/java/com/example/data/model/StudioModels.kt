package com.example.data.model

data class HookIdea(
    val id: String,
    val category: String, // Curiosidade, Choque, Objeção, Preço, ASMR, Status
    val hookText: String,
    val psychologicalReason: String,
    val retentionScore: Int, // e.g. 98%
    val recommendedPiece: String,
    val isFavorite: Boolean = false
)

data class TimingStep(
    val timeRange: String, // "0s - 3s"
    val stage: String,     // "Gancho Magnético"
    val visualAction: String, // "Close macro na zircônia com luz lateral e som de tilintar"
    val spokenAudio: String,  // "Se você ainda compra bijuteria barata que preteja, para tudo..."
    val onScreenText: String  // "NÃO ACREDITO QUE CUSTA SÓ ISSO 😱"
)

data class VideoTemplate(
    val id: String,
    val title: String,
    val hookSummary: String,
    val nicheCategory: String,
    val avgRetentionRate: String,
    val salesConversionRate: String,
    val difficulty: String,
    val filmingStyle: String, // Faceless (Mãos e caixas), Apresentado, ASMR Macro
    val steps: List<TimingStep>,
    val suggestedAudio: String,
    val recommendedHashtags: List<String>,
    val shopCtaText: String
)

data class CompetitorProfile(
    val id: String,
    val handle: String,
    val name: String,
    val avatarEmoji: String,
    val followersCount: String,
    val monthlySalesEstimated: String,
    val avgViewsPerVideo: String,
    val engagementRate: String,
    val postingFrequency: String,
    val winningFormat: String,
    val bestPostHours: String,
    val topSellingJewel: String,
    val growthStrategy: String,
    val topHashtags: List<String>
)

data class TrendRadarItem(
    val id: String,
    val title: String,
    val growthPercent: String,
    val tagCategory: String, // Peça Viral, Áudio em Alta, Gatilho de Venda
    val description: String,
    val actionAdvice: String,
    val viralScore: Int
)

data class ContentAlternativeIdea(
    val id: String,
    val formatType: String, // Carrossel TikTok, Live Shop, Guia Estético, Quiz de Estilo
    val title: String,
    val objective: String,
    val stepByStep: List<String>,
    val scriptOrCopy: String,
    val estimatedReach: String
)

data class VideoAuditCriteria(
    val id: String,
    val name: String,
    val weightPoints: Int,
    val description: String,
    var isChecked: Boolean = false
)
