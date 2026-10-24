package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Gold18k
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink

enum class CaptionStyle(val displayName: String, val primaryColor: Color, val bgColor: Color) {
    BOUNCE_GOLD("Dourado Magnético", Gold18k, Color(0xDD120C1C)),
    CYBER_PINK("TikTok Viral Pink", TikTokPink, Color(0xE6050505)),
    BOXED_NEON("Neon TikTok Shop", TikTokCyan, Color(0xF0180F29)),
    KARAOKE_WORD("Palavra Pulsante 🔥", Gold18k, Color(0xF20B0714)),
    CLEAN_MINIMAL("Minimalista Luxo", Color.White, Color(0xB3000000))
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

data class VideoEditorState(
    val videoUri: String? = null,
    val videoName: String = "Caixinha_Veludo_Macro.mp4",
    val selectedSample: SampleJewelryClip? = null,
    val isPlaying: Boolean = true,
    val captionsEnabled: Boolean = true,
    val captionText: String = "✨ OLHA ESSE BRILHO NO BANHO 18K ✨",
    val selectedCaptionStyle: CaptionStyle = CaptionStyle.KARAOKE_WORD,
    val activeBadges: Set<String> = setOf("banho_10m", "promo_shop"),
    val activeSoundEffect: SoundEffectItem? = null,
    val isSoundEnabled: Boolean = true,
    val timelineTrim: VideoTimelineTrim = VideoTimelineTrim(),
    val teleprompter: TeleprompterConfig = TeleprompterConfig(),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isExportFinished: Boolean = false,
    val lastExportedSummary: String? = null
)

