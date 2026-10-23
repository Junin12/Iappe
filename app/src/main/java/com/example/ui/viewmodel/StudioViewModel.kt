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
    EDITOR("Estúdio Vídeo", "🎬"),
    TEMPLATES("Templates Shop", "⭐"),
    AI_WRITER("Roteiro IA & Hooks", "✨"),
    TRENDS("Tendências & Perfis", "📈")
}

data class StudioUiState(
    val currentTab: StudioTab = StudioTab.EDITOR,
    val editorState: VideoEditorState = VideoEditorState(),
    val sampleClips: List<SampleJewelryClip> = emptyList(),
    val shopBadges: List<ShopBadge> = emptyList(),
    val soundEffects: List<SoundEffectItem> = emptyList(),
    val templates: List<VideoTemplate> = emptyList(),
    val selectedTemplate: VideoTemplate? = null,
    val hooks: List<HookIdea> = emptyList(),
    val competitors: List<CompetitorProfile> = emptyList(),
    val selectedCompetitor: CompetitorProfile? = null,
    val trends: List<TrendRadarItem> = emptyList(),
    val alternativeIdeas: List<ContentAlternativeIdea> = emptyList(),
    val auditCriteria: List<VideoAuditCriteria> = emptyList(),
    val auditScore: Int = 85,
    // AI Writer Inputs & Outputs
    val aiSelectedPiece: String = "Colar & Choker",
    val aiSelectedGoal: String = "Venda Rápida TikTok Shop",
    val aiCustomNotes: String = "",
    val isAiGenerating: Boolean = false,
    val aiGeneratedContent: String = "",
    val aiActiveSubTab: String = "hooks",
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
        val samples = repository.getSampleClips()
        val badges = repository.getShopBadges()
        val sounds = repository.getSoundEffects()
        val templates = repository.getWinningTemplates()
        val hooks = repository.getViralHooks()
        val competitors = repository.getCompetitorProfiles()
        val trends = repository.getTrendRadar()
        val alternativeIdeas = repository.getAlternativeContentIdeas()
        val auditCriteria = repository.getInitialAuditCriteria()

        // Set default initial checks
        auditCriteria[0].isChecked = true
        auditCriteria[1].isChecked = true
        auditCriteria[2].isChecked = true
        auditCriteria[3].isChecked = true

        val initialScore = calculateAuditScore(auditCriteria)

        val firstClip = samples.firstOrNull()

        _uiState.update {
            it.copy(
                sampleClips = samples,
                shopBadges = badges,
                soundEffects = sounds,
                templates = templates,
                selectedTemplate = templates.firstOrNull(),
                hooks = hooks,
                competitors = competitors,
                selectedCompetitor = competitors.firstOrNull(),
                trends = trends,
                alternativeIdeas = alternativeIdeas,
                auditCriteria = auditCriteria,
                auditScore = initialScore,
                editorState = it.editorState.copy(
                    selectedSample = firstClip,
                    videoName = firstClip?.title ?: "Video_Semijoia.mp4",
                    captionText = firstClip?.defaultCaption ?: "✨ OLHA ESSE BRILHO NO BANHO 18K ✨",
                    timelineTrim = VideoTimelineTrim(
                        startSeconds = 0f,
                        endSeconds = firstClip?.durationSeconds ?: 15f,
                        totalDurationSeconds = firstClip?.durationSeconds ?: 15f
                    ),
                    teleprompter = it.editorState.teleprompter.copy(
                        scriptText = templates.firstOrNull()?.steps?.joinToString("\n\n") { s -> "${s.timeRange}: ${s.spokenAudio}" } ?: ""
                    )
                )
            )
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- VIDEO EDITOR & TIMELINE CONTROLS ---

    fun selectSampleClip(clip: SampleJewelryClip) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    selectedSample = clip,
                    videoUri = null,
                    videoName = "${clip.title}.mp4",
                    captionText = clip.defaultCaption,
                    timelineTrim = VideoTimelineTrim(
                        startSeconds = 0f,
                        endSeconds = clip.durationSeconds,
                        totalDurationSeconds = clip.durationSeconds
                    ),
                    isExportFinished = false
                ),
                snackbarMessage = "Clipe carregado: ${clip.title}"
            )
        }
    }

    fun setCustomVideoUploaded(name: String, uri: String) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    videoUri = uri,
                    videoName = name,
                    selectedSample = null,
                    timelineTrim = VideoTimelineTrim(startSeconds = 0f, endSeconds = 20f, totalDurationSeconds = 20f),
                    isExportFinished = false
                ),
                snackbarMessage = "Vídeo próprio importado: $name"
            )
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

    fun setVideoFilter(filter: VideoFilterType) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    currentFilter = filter
                )
            )
        }
    }

    fun setFilterIntensity(intensity: Float) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    filterIntensity = intensity
                )
            )
        }
    }

    // --- STUDIO LIGHTING ---
    fun setLighting(lighting: LightingEffectType) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    currentLighting = lighting
                ),
                snackbarMessage = "Iluminação alterada: ${lighting.displayName}"
            )
        }
    }

    fun setLightingIntensity(intensity: Float) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    lightingIntensity = intensity
                )
            )
        }
    }

    // --- TIMELINE TRIMMING ---
    fun setTimelineTrim(start: Float, end: Float) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    timelineTrim = it.editorState.timelineTrim.copy(
                        startSeconds = start,
                        endSeconds = end
                    )
                )
            )
        }
    }

    fun resetTimelineTrim() {
        _uiState.update {
            val total = it.editorState.timelineTrim.totalDurationSeconds
            it.copy(
                editorState = it.editorState.copy(
                    timelineTrim = VideoTimelineTrim(
                        startSeconds = 0f,
                        endSeconds = total,
                        totalDurationSeconds = total
                    )
                ),
                snackbarMessage = "Timeline resetada para duração total."
            )
        }
    }

    // --- AUDIO DUCKING & MIXER ---
    fun setMusicVolume(vol: Float) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    audioMixer = it.editorState.audioMixer.copy(musicVolume = vol)
                )
            )
        }
    }

    fun setAsmrVolume(vol: Float) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    audioMixer = it.editorState.audioMixer.copy(asmrVoiceVolume = vol)
                )
            )
        }
    }

    fun toggleAudioDucking() {
        _uiState.update {
            val current = it.editorState.audioMixer.isDuckingEnabled
            it.copy(
                editorState = it.editorState.copy(
                    audioMixer = it.editorState.audioMixer.copy(isDuckingEnabled = !current)
                ),
                snackbarMessage = if (!current) "Audio Ducking ativado: trilha abaixa quando há voz/ASMR!" else "Audio Ducking desativado."
            )
        }
    }

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

    // --- COVER GENERATOR ---
    fun openCoverGenerator() {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    coverThumbnail = it.editorState.coverThumbnail.copy(isOpen = true)
                )
            )
        }
    }

    fun closeCoverGenerator() {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    coverThumbnail = it.editorState.coverThumbnail.copy(isOpen = false)
                )
            )
        }
    }

    fun setCoverStyle(style: CoverTemplateStyle) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    coverThumbnail = it.editorState.coverThumbnail.copy(style = style)
                )
            )
        }
    }

    fun setCoverHeadline(headline: String) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    coverThumbnail = it.editorState.coverThumbnail.copy(mainHeadline = headline)
                )
            )
        }
    }

    fun setCoverPriceTag(price: String) {
        _uiState.update {
            it.copy(
                editorState = it.editorState.copy(
                    coverThumbnail = it.editorState.coverThumbnail.copy(priceTag = price)
                )
            )
        }
    }

    fun saveCover() {
        _uiState.update {
            it.copy(
                snackbarMessage = "Capa salva com sucesso para a capa do feed do TikTok Shop!"
            )
        }
    }

    // --- EXPORT PIPELINE ---
    fun exportAndPrepareForTikTok() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    editorState = it.editorState.copy(
                        isExporting = true,
                        exportProgress = 0.1f,
                        isExportFinished = false
                    )
                )
            }

            for (step in 2..10) {
                delay(160)
                _uiState.update {
                    it.copy(
                        editorState = it.editorState.copy(
                            exportProgress = step / 10f
                        )
                    )
                }
            }

            val summary = "Vídeo otimizado em 1080x1920 (9:16) com corte (${String.format("%.1f", _uiState.value.editorState.timelineTrim.currentDuration)}s), iluminação ${_uiState.value.editorState.currentLighting.displayName}, áudio ducking ativo e capa estética pronta."

            _uiState.update {
                it.copy(
                    editorState = it.editorState.copy(
                        isExporting = false,
                        exportProgress = 1f,
                        isExportFinished = true,
                        lastExportedSummary = summary
                    ),
                    snackbarMessage = "✅ Vídeo e capa prontos para o TikTok Shop! Texto copiado."
                )
            }
        }
    }

    // --- TEMPLATES LOGIC ---
    fun selectTemplate(template: VideoTemplate) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun applyTemplateToEditor(template: VideoTemplate) {
        _uiState.update {
            it.copy(
                currentTab = StudioTab.EDITOR,
                editorState = it.editorState.copy(
                    captionText = template.steps.firstOrNull()?.onScreenText ?: template.title,
                    teleprompter = it.editorState.teleprompter.copy(
                        scriptText = template.steps.joinToString("\n\n") { s -> "${s.timeRange} (${s.stage}):\nFala: \"${s.spokenAudio}\"\nTexto tela: ${s.onScreenText}" }
                    ),
                    isExportFinished = false
                ),
                snackbarMessage = "Template '${template.title}' carregado no Estúdio e Teleprompter!"
            )
        }
    }

    // --- AI SCRIPTWRITING ---
    fun setAiPiece(piece: String) {
        _uiState.update { it.copy(aiSelectedPiece = piece) }
    }

    fun setAiGoal(goal: String) {
        _uiState.update { it.copy(aiSelectedGoal = goal) }
    }

    fun setAiSubTab(subTab: String) {
        _uiState.update { it.copy(aiActiveSubTab = subTab) }
    }

    fun generateAiContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true) }
            val state = _uiState.value
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
                    snackbarMessage = "Conteúdo gerado com sucesso pela IA e enviado ao Teleprompter!"
                )
            }
        }
    }

    fun applyHookToEditor(hookText: String) {
        _uiState.update {
            it.copy(
                currentTab = StudioTab.EDITOR,
                editorState = it.editorState.copy(
                    captionText = hookText.replace("\"", ""),
                    teleprompter = it.editorState.teleprompter.copy(scriptText = hookText)
                ),
                snackbarMessage = "Gancho aplicado no vídeo e carregado no teleprompter!"
            )
        }
    }

    // --- COMPETITORS & TRENDS ---
    fun selectCompetitor(comp: CompetitorProfile) {
        _uiState.update { it.copy(selectedCompetitor = comp) }
    }

    fun toggleAuditCriterion(criterionId: String) {
        _uiState.update { state ->
            val updatedList = state.auditCriteria.map { c ->
                if (c.id == criterionId) c.copy(isChecked = !c.isChecked) else c
            }
            val newScore = calculateAuditScore(updatedList)
            state.copy(
                auditCriteria = updatedList,
                auditScore = newScore
            )
        }
    }

    private fun calculateAuditScore(criteria: List<VideoAuditCriteria>): Int {
        val totalPossible = criteria.sumOf { it.weightPoints }
        val earned = criteria.filter { it.isChecked }.sumOf { it.weightPoints }
        return if (totalPossible > 0) (earned * 100) / totalPossible else 0
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
