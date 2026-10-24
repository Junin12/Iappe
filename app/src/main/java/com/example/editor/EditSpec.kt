package com.example.editor

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.media3.common.Effect
import androidx.media3.effect.*
import com.google.common.collect.ImmutableList

enum class HookTextStyle(val displayName: String) {
    TIKTOK_BOX("Caixa Branca (TikTok)"),
    GOLD_ELEGANT("Dourado Elegante"),
    WHITE_OUTLINE("Branco com Contorno")
}

enum class HookTextPosition(val displayName: String, val yPercent: Float) {
    TOP("Topo", 0.18f),
    CENTER("Centro", 0.45f),
    BOTTOM("Base (Segura)", 0.68f)
}

enum class AudioMode(val displayName: String, val volumeMultiplier: Float) {
    KEEP("Manter Áudio Original", 1.0f),
    REDUCE("Reduzir Áudio (30% Fundo)", 0.3f),
    MUTE("Remover Áudio (Mudo p/ Som TikTok)", 0.0f)
}

data class VideoTrim(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val videoDurationMs: Long = 0L
) {
    val durationTrimmedMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    val isHookInGoldenZone: Boolean
        get() = startMs <= 3000L
}

data class HookOverlaySpec(
    val text: String = "",
    val durationMs: Long = 3000L,
    val style: HookTextStyle = HookTextStyle.TIKTOK_BOX,
    val position: HookTextPosition = HookTextPosition.TOP
)

data class ColorAdjustments(
    val brightness: Float = 0f, // -0.5f .. 0.5f
    val contrast: Float = 0f,   // -0.8f .. 0.8f
    val saturation: Float = 0f  // -1.0f .. 1.0f
) {
    companion object {
        val JEWELRY_GLOW_PRESET = ColorAdjustments(
            brightness = 0.05f,
            contrast = 0.20f,
            saturation = 0.35f
        )
    }
}

data class EditSpec(
    val videoUri: String = "",
    val pecaId: Long? = null,
    val pecaNome: String = "",
    val videoId: Long? = null,
    val legendaPost: String = "",
    val hashtags: String = "",
    val trim: VideoTrim = VideoTrim(),
    val hookOverlay: HookOverlaySpec = HookOverlaySpec(),
    val colorAdjustments: ColorAdjustments = ColorAdjustments(),
    val isJewelryGlowPresetApplied: Boolean = false,
    val audioMode: AudioMode = AudioMode.KEEP,
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1920
)

object EditSpecValidator {

    fun validateTrim(startMs: Long, endMs: Long, totalDurationMs: Long): VideoTrim {
        val safeTotal = totalDurationMs.coerceAtLeast(0L)
        if (safeTotal == 0L) {
            return VideoTrim(0L, 0L, 0L)
        }

        var safeStart = startMs.coerceIn(0L, safeTotal)
        var safeEnd = if (endMs <= 0L || endMs > safeTotal) safeTotal else endMs

        // Se o fim for antes do início ou igual, ajusta com segurança
        if (safeEnd < safeStart) {
            val temp = safeStart
            safeStart = safeEnd
            safeEnd = temp
        }

        // Garante corte mínimo de 500ms ou até a duração total
        if (safeEnd - safeStart < 500L && safeTotal >= 500L) {
            if (safeStart + 500L <= safeTotal) {
                safeEnd = safeStart + 500L
            } else {
                safeStart = (safeTotal - 500L).coerceAtLeast(0L)
                safeEnd = safeTotal
            }
        }

        return VideoTrim(
            startMs = safeStart,
            endMs = safeEnd,
            videoDurationMs = safeTotal
        )
    }

    fun clampSlider(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }

    fun applyJewelryGlowPreset(): ColorAdjustments {
        return ColorAdjustments.JEWELRY_GLOW_PRESET
    }
}

/**
 * Função pura que constrói a lista de efeitos do Media3 a partir do EditSpec.
 * Pode ser testada em testes unitários.
 */
object Media3EffectsFactory {

    fun buildEffectsList(spec: EditSpec): List<Effect> {
        val effects = mutableListOf<Effect>()

        // 1. Ajuste de Contraste real (-0.8f a 0.8f)
        if (spec.colorAdjustments.contrast != 0f) {
            val factor = EditSpecValidator.clampSlider(spec.colorAdjustments.contrast, -0.8f, 0.8f)
            effects.add(Contrast(factor))
        }

        // 2. Ajuste de Brilho real via RgbAdjustment (-0.5f a 0.5f)
        if (spec.colorAdjustments.brightness != 0f) {
            val scale = 1.0f + EditSpecValidator.clampSlider(spec.colorAdjustments.brightness, -0.5f, 0.5f)
            val rgb = RgbAdjustment.Builder()
                .setRedScale(scale)
                .setGreenScale(scale)
                .setBlueScale(scale)
                .build()
            effects.add(rgb)
        }

        // 3. Ajuste de Saturação real via HslAdjustment (-1.0f a 1.0f)
        if (spec.colorAdjustments.saturation != 0f) {
            val satAdjustment = EditSpecValidator.clampSlider(spec.colorAdjustments.saturation, -1.0f, 1.0f) * 100f
            val hsl = HslAdjustment.Builder()
                .adjustSaturation(satAdjustment)
                .build()
            effects.add(hsl)
        }

        // 4. Formato TikTok Vertical 1080x1920
        effects.add(
            Presentation.createForWidthAndHeight(
                spec.targetWidth,
                spec.targetHeight,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        )

        // 5. Overlay do Gancho nos primeiros segundos (se houver texto)
        if (spec.hookOverlay.text.isNotBlank()) {
            val overlay = createHookBitmapOverlay(
                hookSpec = spec.hookOverlay,
                width = spec.targetWidth,
                height = spec.targetHeight
            )
            effects.add(OverlayEffect(listOf<TextureOverlay>(overlay)))
        }

        return effects
    }

    /**
     * Gera o BitmapOverlay com o texto estilizado queimado nos primeiros 3 segundos.
     */
    fun createHookBitmapOverlay(
        hookSpec: HookOverlaySpec,
        width: Int,
        height: Int
    ): BitmapOverlay {
        val styledBitmap = renderHookCardBitmap(
            text = hookSpec.text,
            style = hookSpec.style,
            position = hookSpec.position,
            targetWidth = width,
            targetHeight = height
        )

        val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val cutoffUs = hookSpec.durationMs * 1000L

        return object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap {
                return if (presentationTimeUs <= cutoffUs) {
                    styledBitmap
                } else {
                    emptyBitmap
                }
            }
        }
    }

    /**
     * Renderiza o cartão visual do gancho com alta qualidade para ser queimado no vídeo.
     */
    fun renderHookCardBitmap(
        text: String,
        style: HookTextStyle,
        position: HookTextPosition,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (text.isBlank()) return bitmap

        val maxCardWidth = (targetWidth * 0.82f).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        when (style) {
            HookTextStyle.TIKTOK_BOX -> {
                textPaint.color = Color.BLACK
            }
            HookTextStyle.GOLD_ELEGANT -> {
                textPaint.color = Color.rgb(255, 215, 0) // Gold
                textPaint.setShadowLayer(8f, 3f, 3f, Color.argb(220, 0, 0, 0))
            }
            HookTextStyle.WHITE_OUTLINE -> {
                textPaint.color = Color.WHITE
                textPaint.setShadowLayer(6f, 2f, 2f, Color.BLACK)
            }
        }

        val staticLayout = StaticLayout.Builder.obtain(
            text.uppercase(),
            0,
            text.length,
            textPaint,
            maxCardWidth - 60
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f, 1.1f)
            .build()

        val cardWidth = (staticLayout.width + 60).coerceAtLeast(300)
        val cardHeight = staticLayout.height + 48

        val cardLeft = (targetWidth - cardWidth) / 2f
        val cardTop = targetHeight * position.yPercent

        // Fundo do card conforme estilo
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (style) {
            HookTextStyle.TIKTOK_BOX -> {
                bgPaint.color = Color.WHITE
                canvas.drawRoundRect(
                    cardLeft,
                    cardTop,
                    cardLeft + cardWidth,
                    cardTop + cardHeight,
                    22f,
                    22f,
                    bgPaint
                )
            }
            HookTextStyle.GOLD_ELEGANT -> {
                bgPaint.color = Color.argb(230, 20, 20, 24)
                canvas.drawRoundRect(
                    cardLeft,
                    cardTop,
                    cardLeft + cardWidth,
                    cardTop + cardHeight,
                    22f,
                    22f,
                    bgPaint
                )
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(212, 175, 55)
                    this.style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawRoundRect(
                    cardLeft,
                    cardTop,
                    cardLeft + cardWidth,
                    cardTop + cardHeight,
                    22f,
                    22f,
                    borderPaint
                )
            }
            HookTextStyle.WHITE_OUTLINE -> {
                bgPaint.color = Color.argb(190, 0, 0, 0)
                canvas.drawRoundRect(
                    cardLeft,
                    cardTop,
                    cardLeft + cardWidth,
                    cardTop + cardHeight,
                    20f,
                    20f,
                    bgPaint
                )
            }
        }

        // Desenho do texto centralizado no card
        canvas.save()
        canvas.translate(cardLeft + 30, cardTop + 24)
        staticLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }
}
