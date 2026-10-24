package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CreatorStudioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioTab(val label: String, val icon: String) {
    GRAVAR("Gravar", "🎥"),
    PECAS("Peças", "💎"),
    AI_WRITER("Roteiros", "✨"),
    PLANNER("Planejar", "📅"),
    DASHBOARD("Painel", "📊")
}

data class StudioUiState(
    val currentTab: StudioTab = StudioTab.GRAVAR,
    val recordedVideoReviewUri: String? = null,
    val recordingForPecaId: Long? = null,
    val recordingForPecaNome: String? = null,
    val editorState: VideoEditorState = VideoEditorState(),
    val hooks: List<HookIdea> = emptyList(),
    val alternativeIdeas: List<ContentAlternativeIdea> = emptyList(),
    // AI Writer Inputs & Outputs
    val aiSelectedPeca: com.example.data.local.PecaEntity? = null,
    val aiSelectedDuration: Int = 15,
    val aiSelectedStyle: String = "Old Money Elegance (Banho 10M)",
    val aiSelectedPiece: String = "Colar & Choker",
    val aiSelectedGoal: String = "Choque & Objeção 'Preteja?'",
    val aiTargetPrice: String = "",
    val aiCustomNotes: String = "",
    val isAiGenerating: Boolean = false,
    val aiScriptResult: com.example.data.model.GeminiScriptResult? = null,
    val aiSelectedHookIndex: Int = 0,
    val aiErrorMessage: String? = null,
    val aiIsOfflineTemplate: Boolean = false,
    val aiGeneratedContent: String = "",
    val aiActiveSubTab: String = "full_script",
    val userSavedFavorites: List<String> = emptyList(),
    val snackbarMessage: String? = null
)

class StudioViewModel(
    private val repository: CreatorStudioRepository = CreatorStudioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val hooks = repository.getViralHooks()
        val alternativeIdeas = repository.getAlternativeContentIdeas()

        _uiState.update {
            it.copy(
                hooks = hooks,
                alternativeIdeas = alternativeIdeas
            )
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun prefillForPeca(pecaNome: String, tipo: String, banho: String, pedra: String, preco: Double, sku: String) {
        _uiState.update {
            it.copy(
                aiSelectedPiece = tipo,
                aiTargetPrice = "R$ %.2f".format(java.util.Locale.US, preco),
                aiCustomNotes = "Peça: $pecaNome | Banho: $banho | Pedra: $pedra | SKU: $sku",
                currentTab = StudioTab.AI_WRITER,
                snackbarMessage = "Dados de \"$pecaNome\" carregados no gerador de roteiro! ✨"
            )
        }
    }

    fun prefillForPeca(peca: com.example.data.local.PecaEntity) {
        _uiState.update {
            it.copy(
                aiSelectedPeca = peca,
                aiSelectedPiece = peca.tipo,
                aiTargetPrice = "R$ %.2f".format(java.util.Locale.US, peca.preco),
                aiCustomNotes = "Peça: ${peca.nome} | Banho: ${peca.banho} (${peca.milesimos}M) | Pedra: ${peca.pedra} | SKU: ${peca.codigoSku}",
                currentTab = StudioTab.AI_WRITER,
                snackbarMessage = "Dados de \"${peca.nome}\" carregados no gerador de roteiro! ✨"
            )
        }
    }

    fun setAiSelectedPeca(peca: com.example.data.local.PecaEntity?) {
        _uiState.update {
            if (peca != null) {
                it.copy(
                    aiSelectedPeca = peca,
                    aiSelectedPiece = peca.tipo,
                    aiTargetPrice = "R$ %.2f".format(java.util.Locale.US, peca.preco),
                    aiCustomNotes = "Peça: ${peca.nome} | Banho: ${peca.banho} (${peca.milesimos}M) | Pedra: ${peca.pedra}"
                )
            } else {
                it.copy(aiSelectedPeca = null)
            }
        }
    }

    fun setAiDuration(durationSeg: Int) {
        _uiState.update { it.copy(aiSelectedDuration = durationSeg) }
    }

    fun setRecordingForPeca(pecaId: Long, pecaNome: String) {
        _uiState.update {
            it.copy(
                recordingForPecaId = pecaId,
                recordingForPecaNome = pecaNome,
                currentTab = StudioTab.GRAVAR,
                snackbarMessage = "Gravando vídeo para \"$pecaNome\" 🎥"
            )
        }
    }

    fun clearRecordingForPeca() {
        _uiState.update {
            it.copy(recordingForPecaId = null, recordingForPecaNome = null)
        }
    }

    // --- CAMERA RECORDING & REVIEW ---
    fun onVideoRecorded(uri: android.net.Uri) {
        _uiState.update {
            it.copy(
                recordedVideoReviewUri = uri.toString(),
                snackbarMessage = "Gravação concluída! Revendo vídeo em 1080p."
            )
        }
    }

    fun retakeVideo() {
        _uiState.update {
            it.copy(
                recordedVideoReviewUri = null,
                currentTab = StudioTab.GRAVAR
            )
        }
    }

    fun dismissReview() {
        _uiState.update {
            it.copy(recordedVideoReviewUri = null)
        }
    }

    fun togglePlayback() {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    isPlaying = !it.editorState.isPlaying
                )
            )
        }
    }

    // --- TIMELINE TRIMMING ---

    // --- CAPTIONS & BADGES ---
    fun toggleCaptions() {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    captionsEnabled = !it.editorState.captionsEnabled
                )
            )
        }
    }

    fun updateCaptionText(newText: String) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    captionText = newText
                )
            )
        }
    }

    fun setCaptionStyle(style: CaptionStyle) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    selectedCaptionStyle = style
                )
            )
        }
    }

    fun toggleShopBadge(badgeId: String) {
        _uiState.update {
            val current = it.editorState.activeBadges.toMutableSet()
            if (current.contains(badgeId)) {
                current.remove(badgeId)
            } else {
                current.add(badgeId)
            }
            it.copy(editorState = it.editorState.copy(activeBadges = current))
        }
    }

    fun selectSoundEffect(sound: SoundEffectItem) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    activeSoundEffect = sound,
                    isSoundEnabled = true
                ),
                snackbarMessage = "Áudio ASMR: ${sound.name}"
            )
        }
    }

    // --- TELEPROMPTER ---
    fun openTeleprompter(scriptText: String? = null) {
        _uiState.update {
            val textToUse = scriptText ?: it.editorState.captionText
            it.copy(
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(
                        isOpen = true,
                        scriptText = textToUse,
                        isScrolling = false
                    )
                )
            )
        }
    }

    fun closeTeleprompter() {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(
                        isOpen = false,
                        isScrolling = false
                    )
                )
            )
        }
    }

    fun toggleTeleprompterScrolling() {
        _uiState.update {
            val current = it.editorState.teleprompter.isScrolling
            it.copy(
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(isScrolling = !current)
                )
            )
        }
    }

    fun setTeleprompterSpeed(wpm: Int) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(scrollSpeedWpm = wpm)
                )
            )
        }
    }

    fun setTeleprompterFontSize(sizeSp: Int) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(fontSizeSp = sizeSp)
                )
            )
        }
    }

    fun applyTeleprompterAsCaption() {
        _uiState.update {
            val text = it.editorState.teleprompter.scriptText.lines().firstOrNull { l -> l.isNotBlank() }
                ?: it.editorState.teleprompter.scriptText
            it.copy(
                editorState = it.editorState.copy(
                    captionText = text,
                    teleprompter = it.editorState.teleprompter.copy(isOpen = false)
                ),
                snackbarMessage = "Texto do teleprompter aplicado como legenda!"
            )
        }
    }

    // --- AI SCRIPTWRITING ---
    fun setAiStyle(style: String) {
        _uiState.update { it.copy(aiSelectedStyle = style) }
    }

    fun setAiPiece(piece: String) {
        _uiState.update { it.copy(aiSelectedPiece = piece) }
    }

    fun setAiGoal(goal: String) {
        _uiState.update { it.copy(aiSelectedGoal = goal) }
    }

    fun setAiTargetPrice(price: String) {
        _uiState.update { it.copy(aiTargetPrice = price) }
    }

    fun setAiCustomNotes(notes: String) {
        _uiState.update { it.copy(aiCustomNotes = notes) }
    }

    fun setAiSubTab(subTab: String) {
        _uiState.update { it.copy(aiActiveSubTab = subTab) }
    }

    private val geminiService = com.example.data.service.GeminiScriptService()
    private var aiScriptJob: kotlinx.coroutines.Job? = null

    fun selectHookVariation(index: Int) {
        val currentResult = _uiState.value.aiScriptResult ?: return
        if (index in currentResult.ganchosVariacoes.indices) {
            val selectedHook = currentResult.ganchosVariacoes[index]
            val updatedResult = currentResult.copy(gancho = selectedHook)
            val updatedTeleprompter = updatedResult.toTeleprompterText()
            _uiState.update {
                it.copy(
                    aiSelectedHookIndex = index,
                    aiScriptResult = updatedResult,
                    aiGeneratedContent = updatedTeleprompter,
                    editorState = it.editorState.copy(
                        teleprompter = it.editorState.teleprompter.copy(scriptText = updatedTeleprompter)
                    ),
                    snackbarMessage = "Gancho ${index + 1} selecionado para o roteiro! ✨"
                )
            }
        }
    }

    fun cancelAiGeneration() {
        aiScriptJob?.cancel()
        _uiState.update {
            it.copy(
                isAiGenerating = false,
                aiErrorMessage = "Geração cancelada.",
                snackbarMessage = "Geração de roteiro cancelada."
            )
        }
    }

    fun generateAiContent() {
        aiScriptJob?.cancel()
        _uiState.update { it.copy(isAiGenerating = true, aiErrorMessage = null) }

        val state = _uiState.value
        if (state.aiActiveSubTab == "full_script") {
            aiScriptJob = viewModelScope.launch {
                // Monta peca real ou sintetizada
                val rawPrice = state.aiTargetPrice.replace("R$", "").replace(",", ".").trim().toDoubleOrNull() ?: 49.90
                val peca = state.aiSelectedPeca ?: com.example.data.local.PecaEntity(
                    nome = "${state.aiSelectedPiece} GlowStudio",
                    tipo = state.aiSelectedPiece,
                    banho = "Ouro 18k",
                    milesimos = 10,
                    pedra = "Zircônia",
                    preco = rawPrice
                )

                val result = geminiService.generateStructuredScript(
                    peca = peca,
                    duracaoSeg = state.aiSelectedDuration,
                    estilo = state.aiSelectedStyle,
                    objetivo = state.aiSelectedGoal,
                    customNotes = state.aiCustomNotes
                )

                when (result) {
                    is com.example.data.service.GeminiServiceResult.Success -> {
                        val scriptData = result.data
                        val teleprompterText = scriptData.toTeleprompterText()
                        _uiState.update {
                            it.copy(
                                isAiGenerating = false,
                                aiScriptResult = scriptData,
                                aiSelectedHookIndex = 0,
                                aiErrorMessage = null,
                                aiIsOfflineTemplate = false,
                                aiGeneratedContent = teleprompterText,
                                editorState = it.editorState.copy(
                                    teleprompter = it.editorState.teleprompter.copy(scriptText = teleprompterText)
                                ),
                                snackbarMessage = "Roteiro Faceless estruturado gerado via Gemini Flash! ✨"
                            )
                        }
                    }
                    is com.example.data.service.GeminiServiceResult.Error -> {
                        val fallback = result.fallbackTemplate
                        val teleprompterText = fallback?.toTeleprompterText() ?: ""
                        _uiState.update {
                            it.copy(
                                isAiGenerating = false,
                                aiScriptResult = fallback,
                                aiSelectedHookIndex = 0,
                                aiErrorMessage = result.errorMessage,
                                aiIsOfflineTemplate = fallback != null,
                                aiGeneratedContent = teleprompterText,
                                editorState = it.editorState.copy(
                                    teleprompter = it.editorState.teleprompter.copy(scriptText = teleprompterText)
                                ),
                                snackbarMessage = "${result.errorMessage} (Modelo pronto)"
                            )
                        }
                    }
                }
            }
        } else {
            viewModelScope.launch {
                val result = repository.generateAiContent(
                    promptType = state.aiActiveSubTab,
                    pieceType = state.aiSelectedPiece,
                    goal = state.aiSelectedGoal,
                    customNotes = state.aiCustomNotes
                )
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiGeneratedContent = result,
                        editorState = it.editorState.copy(
                            teleprompter = it.editorState.teleprompter.copy(scriptText = result)
                        ),
                        snackbarMessage = "Conteúdo gerado via Gemini AI!"
                    )
                }
            }
        }
    }

    fun recordWithCurrentScript() {
        val scriptText = _uiState.value.aiGeneratedContent
        val peca = _uiState.value.aiSelectedPeca
        _uiState.update {
            it.copy(
                currentTab = StudioTab.GRAVAR,
                recordingForPecaId = peca?.id,
                recordingForPecaNome = peca?.nome,
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(
                        scriptText = scriptText,
                        isOpen = true
                    )
                ),
                snackbarMessage = "Teleprompter preparado com o roteiro! Posicione suas mãos e grave 🎥"
            )
        }
    }

    fun setHookForRecording(hookText: String) {
        val peca = _uiState.value.aiSelectedPeca
        _uiState.update {
            it.copy(
                currentTab = StudioTab.GRAVAR,
                recordingForPecaId = peca?.id,
                recordingForPecaNome = peca?.nome,
                aiGeneratedContent = hookText,
                editorState = it.editorState.copy(
                    teleprompter = it.editorState.teleprompter.copy(
                        scriptText = hookText,
                        isOpen = true
                    )
                ),
                snackbarMessage = "Gancho carregado no teleprompter para gravação! 🎥"
            )
        }
    }

    fun toggleSaveFavorite(itemText: String) {
        _uiState.update { state ->
            val current = state.userSavedFavorites.toMutableList()
            if (current.contains(itemText)) {
                current.remove(itemText)
            } else {
                current.add(itemText)
            }
            state.copy(
                userSavedFavorites = current,
                snackbarMessage = if (current.contains(itemText)) "Salvo nos seus favoritos!" else "Removido dos favoritos"
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
