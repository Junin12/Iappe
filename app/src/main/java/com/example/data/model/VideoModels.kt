package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Gold18k
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink

enum class VideoFilterType(val displayName: String, val icon: String, val description: String) {
    NONE("Normal", "🔘", "Cores originais da câmera"),
    ULTRA_GLOW("Ultra Glow ✨", "✨", "Brilho estelar em zircônias e reflexos dourados"),
    DIAMOND_CRISP("Diamond Crisp 💎", "💎", "Ultra nitidez e micro-contraste para macro"),
    GOLD_18K("Ouro 18k Quente 🏆", "🏆", "Realce sofisticado do banho dourado"),
    PLATINUM_MINIMAL("Platina & Prata 🪞", "🪞", "Tom contemporâneo e elegante para pratas"),
    VELVET_MOOD("Velvet Luxury 🌹", "🌹", "Fundo aveludado e intimista de boutique")
}

enum class LightingEffectType(val displayName: String, val icon: String, val description: String) {
    NATURAL("Luz Natural", "☀️", "Iluminação padrão sem ring light"),
    RING_LIGHT("Ring Light Estelar ✨", "💫", "Anel de luz circular para realce de detalhes"),
    LATERAL_FLASH("Flash Lateral de Luxo 🔦", "🔦", "Facho direcional pontual para reflexo de zircônias"),
    SOFT_DIFFUSE("Luz Difusa Suave 🌤️", "🌤️", "Estilo vitrine de boutique aveludada")
}

enum class CaptionStyle(val displayName: String, val primaryColor: Color, val bgColor: Color) {
    BOUNCE_GOLD("Dourado Magnético", Gold18k, Color(0xDD120C1C)),
    CYBER_PINK("TikTok Viral Pink", TikTokPink, Color(0xE6050505)),
    BOXED_NEON("Neon TikTok Shop", TikTokCyan, Color(0xF0180F29)),
    KARAOKE_WORD("Palavra Pulsante 🔥", Gold18k, Color(0xF20B0714)),
    CLEAN_MINIMAL("Minimalista Luxo", Color.White, Color(0xB3000000))
}

enum class CoverTemplateStyle(val displayName: String, val badgeColor: Color) {
    OLD_MONEY("Old Money Elegance", Gold18k),
    TIKTOK_SHOP_VIRAL("TikTok Shop Viral", TikTokPink),
    MINIMAL_BLACK("Caixa Preta Joalheria", TikTokCyan),
    URGENT_SALE("Oferta Relâmpago", Color(0xFFFF4500))
}

data class ShopBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val accentColor: Color,
    val isEnabled: Boolean = false
)

data class SampleJewelryClip(
    val id: String,
    val title: String,
    val durationText: String,
    val durationSeconds: Float = 15f,
    val category: String,
    val description: String,
    val defaultCaption: String,
    val previewGradient: List<Color>
)

data class SoundEffectItem(
    val id: String,
    val name: String,
    val category: String,
    val duration: String,
    val isViral: Boolean = false
)

data class TeleprompterConfig(
    val scriptText: String = "",
    val scrollSpeedWpm: Int = 140, // 80 - 240 words per minute
    val fontSizeSp: Int = 22,      // 16 - 32 sp
    val isScrolling: Boolean = false,
    val scrollOffset: Float = 0f,
    val isOpen: Boolean = false
)

data class VideoTimelineTrim(
    val startSeconds: Float = 0f,
    val endSeconds: Float = 15f,
    val totalDurationSeconds: Float = 15f,
    val goldenZoneLimitSeconds: Float = 3f
) {
    val isHookInGoldenZone: Boolean get() = startSeconds <= 1.5f
    val currentDuration: Float get() = (endSeconds - startSeconds).coerceAtLeast(1f)
}

data class AudioMixerState(
    val musicVolume: Float = 0.20f, // 20% music
    val asmrVoiceVolume: Float = 1.0f, // 100% voice & ASMR
    val isDuckingEnabled: Boolean = true // auto ducks music when voice/asmr hits
)

data class CoverThumbnailState(
    val isOpen: Boolean = false,
    val style: CoverTemplateStyle = CoverTemplateStyle.OLD_MONEY,
    val mainHeadline: String = "SEMIJOIA QUE NÃO PRETEJA",
    val priceTag: String = "R$ 49,90",
    val subtitleTag: String = "Banho 10 Milésimos • Frete Grátis"
)

data class VideoEditorState(
    val videoUri: String? = null,
    val videoName: String = "Caixinha_Veludo_Macro.mp4",
    val selectedSample: SampleJewelryClip? = null,
    val isPlaying: Boolean = true,
    val currentFilter: VideoFilterType = VideoFilterType.ULTRA_GLOW,
    val filterIntensity: Float = 0.85f,
    val currentLighting: LightingEffectType = LightingEffectType.RING_LIGHT,
    val lightingIntensity: Float = 0.75f,
    val captionsEnabled: Boolean = true,
    val captionText: String = "✨ OLHA ESSE BRILHO NO BANHO 18K ✨",
    val selectedCaptionStyle: CaptionStyle = CaptionStyle.KARAOKE_WORD,
    val activeBadges: Set<String> = setOf("banho_10m", "promo_shop"),
    val activeSoundEffect: SoundEffectItem? = null,
    val isSoundEnabled: Boolean = true,
    val timelineTrim: VideoTimelineTrim = VideoTimelineTrim(),
    val audioMixer: AudioMixerState = AudioMixerState(),
    val coverThumbnail: CoverThumbnailState = CoverThumbnailState(),
    val teleprompter: TeleprompterConfig = TeleprompterConfig(),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isExportFinished: Boolean = false,
    val lastExportedSummary: String? = null
)
