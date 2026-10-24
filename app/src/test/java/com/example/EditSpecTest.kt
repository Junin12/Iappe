package com.example

import com.example.editor.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditSpecTest {

    @Test
    fun testValidateTrimValidRange() {
        val trim = EditSpecValidator.validateTrim(
            startMs = 2000L,
            endMs = 10000L,
            totalDurationMs = 15000L
        )

        assertEquals(2000L, trim.startMs)
        assertEquals(10000L, trim.endMs)
        assertEquals(15000L, trim.videoDurationMs)
        assertEquals(8000L, trim.durationTrimmedMs)
        assertTrue(trim.isHookInGoldenZone)
    }

    @Test
    fun testValidateTrimEndBeforeStart() {
        // Fim antes do início: deve corrigir invertendo ou ajustando
        val trim = EditSpecValidator.validateTrim(
            startMs = 8000L,
            endMs = 2000L,
            totalDurationMs = 15000L
        )

        assertTrue("Início deve ser menor ou igual ao fim", trim.startMs <= trim.endMs)
        assertEquals(2000L, trim.startMs)
        assertEquals(8000L, trim.endMs)
    }

    @Test
    fun testValidateTrimNegativeOrZeroValues() {
        val trim = EditSpecValidator.validateTrim(
            startMs = -500L,
            endMs = 0L,
            totalDurationMs = 10000L
        )

        assertEquals(0L, trim.startMs)
        assertEquals(10000L, trim.endMs) // default to duration when 0
    }

    @Test
    fun testValidateTrimExceedingTotalDuration() {
        val trim = EditSpecValidator.validateTrim(
            startMs = 5000L,
            endMs = 25000L,
            totalDurationMs = 12000L
        )

        assertEquals(5000L, trim.startMs)
        assertEquals(12000L, trim.endMs)
        assertEquals(12000L, trim.videoDurationMs)
    }

    @Test
    fun testValidateTrimZeroDurationVideo() {
        val trim = EditSpecValidator.validateTrim(
            startMs = 1000L,
            endMs = 5000L,
            totalDurationMs = 0L
        )

        assertEquals(0L, trim.startMs)
        assertEquals(0L, trim.endMs)
        assertEquals(0L, trim.videoDurationMs)
    }

    @Test
    fun testClampSliderLimits() {
        // Brilho: -0.5f .. 0.5f
        assertEquals(-0.5f, EditSpecValidator.clampSlider(-1.2f, -0.5f, 0.5f), 0.001f)
        assertEquals(0.5f, EditSpecValidator.clampSlider(0.9f, -0.5f, 0.5f), 0.001f)
        assertEquals(0.2f, EditSpecValidator.clampSlider(0.2f, -0.5f, 0.5f), 0.001f)

        // Contraste: -0.8f .. 0.8f
        assertEquals(-0.8f, EditSpecValidator.clampSlider(-0.95f, -0.8f, 0.8f), 0.001f)
        assertEquals(0.8f, EditSpecValidator.clampSlider(1.5f, -0.8f, 0.8f), 0.001f)

        // Saturação: -1.0f .. 1.0f
        assertEquals(-1.0f, EditSpecValidator.clampSlider(-2.0f, -1.0f, 1.0f), 0.001f)
        assertEquals(1.0f, EditSpecValidator.clampSlider(2.0f, -1.0f, 1.0f), 0.001f)
    }

    @Test
    fun testJewelryGlowPresetValues() {
        val preset = EditSpecValidator.applyJewelryGlowPreset()

        assertEquals(0.05f, preset.brightness, 0.001f)
        assertEquals(0.20f, preset.contrast, 0.001f)
        assertEquals(0.35f, preset.saturation, 0.001f)
    }

    @Test
    fun testGoldenZoneDetection() {
        val inZone = VideoTrim(startMs = 1500L, endMs = 8000L, videoDurationMs = 10000L)
        assertTrue(inZone.isHookInGoldenZone)

        val outOfZone = VideoTrim(startMs = 4500L, endMs = 9000L, videoDurationMs = 10000L)
        assertFalse(outOfZone.isHookInGoldenZone)
    }

    @Test
    fun testEffectsListConstruction() {
        val spec = EditSpec(
            videoUri = "content://test/video.mp4",
            trim = VideoTrim(0L, 10000L, 10000L),
            colorAdjustments = ColorAdjustments(
                brightness = 0.1f,
                contrast = 0.2f,
                saturation = 0.3f
            ),
            hookOverlay = HookOverlaySpec(
                text = "BRILHO 10 MILÉSIMOS",
                style = HookTextStyle.TIKTOK_BOX,
                position = HookTextPosition.TOP
            ),
            targetWidth = 1080,
            targetHeight = 1920
        )

        val effects = Media3EffectsFactory.buildEffectsList(spec)
        // Deve conter Contrast, RgbAdjustment, HslAdjustment, Presentation e OverlayEffect
        assertTrue("Deve conter múltiplos efeitos", effects.size >= 4)
    }
}
