package com.example

import com.example.data.model.CoverTemplateStyle
import com.example.data.model.LightingEffectType
import com.example.data.repository.CreatorStudioRepository
import com.example.ui.viewmodel.StudioViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val repository = CreatorStudioRepository()

    @Test
    fun testWinningTemplatesLoaded() {
        val templates = repository.getWinningTemplates()
        assertTrue(templates.isNotEmpty())
        templates.forEach { template ->
            assertTrue(template.steps.isNotEmpty())
            assertTrue(template.title.isNotBlank())
            assertTrue(template.avgRetentionRate.isNotBlank())
        }
    }

    @Test
    fun testViralHooksScoring() {
        val hooks = repository.getViralHooks()
        assertTrue(hooks.isNotEmpty())
        hooks.forEach { hook ->
            assertTrue(hook.retentionScore in 90..100)
            assertTrue(hook.psychologicalReason.isNotBlank())
        }
    }

    @Test
    fun testCompetitorProfilesLoaded() {
        val competitors = repository.getCompetitorProfiles()
        assertTrue(competitors.size >= 4)
        competitors.forEach { comp ->
            assertTrue(comp.handle.startsWith("@"))
            assertTrue(comp.winningFormat.isNotBlank())
            assertTrue(comp.topHashtags.isNotEmpty())
        }
    }

    @Test
    fun testStudioViewModelInitialization() {
        val viewModel = StudioViewModel(repository)
        val state = viewModel.uiState.value
        assertNotNull(state.editorState.selectedSample)
        assertTrue(state.auditScore in 1..100)
        assertTrue(state.shopBadges.isNotEmpty())
    }

    @Test
    fun testTeleprompterWorkflow() {
        val viewModel = StudioViewModel(repository)
        viewModel.openTeleprompter("Texto para teste de teleprompter")
        assertTrue(viewModel.uiState.value.editorState.teleprompter.isOpen)
        assertEquals("Texto para teste de teleprompter", viewModel.uiState.value.editorState.teleprompter.scriptText)

        viewModel.setTeleprompterSpeed(180)
        assertEquals(180, viewModel.uiState.value.editorState.teleprompter.scrollSpeedWpm)

        viewModel.toggleTeleprompterScrolling()
        assertTrue(viewModel.uiState.value.editorState.teleprompter.isScrolling)

        viewModel.applyTeleprompterAsCaption()
        assertFalse(viewModel.uiState.value.editorState.teleprompter.isOpen)
        assertEquals("Texto para teste de teleprompter", viewModel.uiState.value.editorState.captionText)
    }

    @Test
    fun testTimelineTrimmingAndGoldenZone() {
        val viewModel = StudioViewModel(repository)
        viewModel.setTimelineTrim(0.5f, 12f)
        val trim = viewModel.uiState.value.editorState.timelineTrim
        assertEquals(0.5f, trim.startSeconds, 0.01f)
        assertEquals(12f, trim.endSeconds, 0.01f)
        assertTrue(trim.isHookInGoldenZone)

        // Late hook test
        viewModel.setTimelineTrim(2.5f, 12f)
        assertFalse(viewModel.uiState.value.editorState.timelineTrim.isHookInGoldenZone)
    }

    @Test
    fun testAudioDuckingAndLighting() {
        val viewModel = StudioViewModel(repository)
        assertTrue(viewModel.uiState.value.editorState.audioMixer.isDuckingEnabled)
        viewModel.toggleAudioDucking()
        assertFalse(viewModel.uiState.value.editorState.audioMixer.isDuckingEnabled)

        viewModel.setLighting(LightingEffectType.RING_LIGHT)
        assertEquals(LightingEffectType.RING_LIGHT, viewModel.uiState.value.editorState.currentLighting)
    }

    @Test
    fun testCoverGenerator() {
        val viewModel = StudioViewModel(repository)
        viewModel.openCoverGenerator()
        assertTrue(viewModel.uiState.value.editorState.coverThumbnail.isOpen)

        viewModel.setCoverStyle(CoverTemplateStyle.OLD_MONEY)
        viewModel.setCoverHeadline("JOIA QUE DURA 5 ANOS")
        viewModel.setCoverPriceTag("R$ 59,90")

        val cover = viewModel.uiState.value.editorState.coverThumbnail
        assertEquals(CoverTemplateStyle.OLD_MONEY, cover.style)
        assertEquals("JOIA QUE DURA 5 ANOS", cover.mainHeadline)
        assertEquals("R$ 59,90", cover.priceTag)
    }
}
