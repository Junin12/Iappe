package com.example

import com.example.data.repository.CreatorStudioRepository
import com.example.ui.viewmodel.StudioViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val repository = CreatorStudioRepository()

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
    fun testAlternativeContentIdeasLoaded() {
        val ideas = repository.getAlternativeContentIdeas()
        assertTrue(ideas.isNotEmpty())
        ideas.forEach { idea ->
            assertTrue(idea.title.isNotBlank())
            assertTrue(idea.stepByStep.isNotEmpty())
        }
    }

    @Test
    fun testStudioViewModelInitialization() {
        val viewModel = StudioViewModel(repository)
        val state = viewModel.uiState.value
        assertTrue(state.hooks.isNotEmpty())
        assertTrue(state.alternativeIdeas.isNotEmpty())
        assertEquals(com.example.ui.viewmodel.StudioTab.GRAVAR, state.currentTab)
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
    fun testDashboardTabNavigation() {
        val viewModel = StudioViewModel(repository)
        viewModel.selectTab(com.example.ui.viewmodel.StudioTab.DASHBOARD)
        assertEquals(com.example.ui.viewmodel.StudioTab.DASHBOARD, viewModel.uiState.value.currentTab)
    }

    @Test
    fun testGeminiFacelessScriptGeneration() {
        val service = com.example.data.service.GeminiScriptService()
        val script = service.generateFallbackFacelessScript(
            jewelryStyle = "Old Money Elegance (Banho 10M)",
            pieceCategory = "Choker Fita 4mm",
            hookStrategy = "Choque & Objeção 'Preteja?'",
            targetPrice = "R$ 49,90",
            storeDetails = "Cupom FRETEGRATIS"
        )
        assertTrue(script.contains("GANCHO"))
        assertTrue(script.contains("CENA"))
        assertTrue(script.contains("CTA"))
        assertTrue(script.contains("49"))
    }
}
