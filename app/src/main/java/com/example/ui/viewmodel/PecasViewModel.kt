package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.PecaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PecasUiState(
    val pecas: List<PecaEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedTipo: String? = null,
    val selectedPeca: PecaEntity? = null,
    val videosForSelectedPeca: List<VideoEntity> = emptyList(),
    val isAddEditOpen: Boolean = false,
    val editingPeca: PecaEntity? = null,
    val isDeleteConfirmOpen: Boolean = false,
    val pecaPendingDelete: PecaEntity? = null,
    // Form State
    val formNome: String = "",
    val formTipo: String = TipoPecaOptions.COLAR,
    val formBanho: String = BanhoPecaOptions.OURO_18K,
    val formMilesimos: String = "10",
    val formPedra: String = PedraPecaOptions.ZIRCONIA,
    val formPreco: String = "",
    val formPrecoPromocional: String = "",
    val formSku: String = "",
    val formLinkTikTok: String = "",
    val formFotos: List<String> = emptyList(),
    val formEstoque: String = "1",
    val formObservacoes: String = "",
    // Validation
    val formNomeError: String? = null,
    val formTipoError: String? = null,
    val formPrecoError: String? = null,
    // Gemini AI Features
    val isAnalyzingPhoto: Boolean = false,
    val photoAnalysisIdeas: List<String> = emptyList(),
    val photoAnalysisApplied: Boolean = false,
    val generatingCaptionForVideoId: Long? = null,
    val snackbarMessage: String? = null
)

class PecasViewModel(
    application: Application,
    private val repository: PecaRepository = PecaRepository(
        AppDatabase.getDatabase(application).pecaDao(),
        AppDatabase.getDatabase(application).videoDao()
    ),
    private val geminiService: com.example.data.service.GeminiScriptService = com.example.data.service.GeminiScriptService()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PecasUiState())
    val uiState: StateFlow<PecasUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")
    private val _tipoFilterFlow = MutableStateFlow<String?>(null)

    init {
        // Observe all pieces combined with search and filter
        viewModelScope.launch {
            combine(
                repository.allPecas,
                _searchFlow,
                _tipoFilterFlow
            ) { allList, query, tipo ->
                allList.filter { peca ->
                    val matchesQuery = query.isBlank() ||
                            peca.nome.contains(query, ignoreCase = true) ||
                            peca.codigoSku.contains(query, ignoreCase = true)
                    val matchesTipo = tipo == null || peca.tipo.equals(tipo, ignoreCase = true)
                    matchesQuery && matchesTipo
                }
            }.collect { filteredPecas ->
                _uiState.update { state ->
                    val updatedSelectedPeca = state.selectedPeca?.let { current ->
                        filteredPecas.find { it.id == current.id }
                    }
                    state.copy(
                        pecas = filteredPecas,
                        selectedPeca = updatedSelectedPeca ?: state.selectedPeca
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedTipo(tipo: String?) {
        _tipoFilterFlow.value = tipo
        _uiState.update { it.copy(selectedTipo = tipo) }
    }

    fun selectPeca(peca: PecaEntity?) {
        _uiState.update { it.copy(selectedPeca = peca) }
        if (peca != null) {
            viewModelScope.launch {
                repository.getVideosForPeca(peca.id).collect { videos ->
                    _uiState.update { it.copy(videosForSelectedPeca = videos) }
                }
            }
        } else {
            _uiState.update { it.copy(videosForSelectedPeca = emptyList()) }
        }
    }

    fun openAddPeca() {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingPeca = null,
                formNome = "",
                formTipo = TipoPecaOptions.COLAR,
                formBanho = BanhoPecaOptions.OURO_18K,
                formMilesimos = "10",
                formPedra = PedraPecaOptions.ZIRCONIA,
                formPreco = "",
                formPrecoPromocional = "",
                formSku = generateSuggestedSku(TipoPecaOptions.COLAR),
                formLinkTikTok = "",
                formFotos = emptyList(),
                formEstoque = "1",
                formObservacoes = "",
                formNomeError = null,
                formTipoError = null,
                formPrecoError = null
            )
        }
    }

    fun openEditPeca(peca: PecaEntity) {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingPeca = peca,
                formNome = peca.nome,
                formTipo = peca.tipo,
                formBanho = peca.banho,
                formMilesimos = peca.milesimos.toString(),
                formPedra = peca.pedra,
                formPreco = "%.2f".format(java.util.Locale.US, peca.preco),
                formPrecoPromocional = peca.precoPromocional?.let { p -> "%.2f".format(java.util.Locale.US, p) } ?: "",
                formSku = peca.codigoSku,
                formLinkTikTok = peca.linkTikTokShop ?: "",
                formFotos = peca.fotos,
                formEstoque = peca.estoque.toString(),
                formObservacoes = peca.observacoes,
                formNomeError = null,
                formTipoError = null,
                formPrecoError = null
            )
        }
    }

    fun closeAddEdit() {
        _uiState.update { it.copy(isAddEditOpen = false, editingPeca = null) }
    }

    // Form inputs updating
    fun updateFormNome(v: String) = _uiState.update { it.copy(formNome = v, formNomeError = null) }
    fun updateFormTipo(v: String) = _uiState.update {
        it.copy(
            formTipo = v,
            formTipoError = null,
            formSku = if (it.formSku.isBlank() || it.formSku.startsWith("PEC-") || it.formSku.contains("-0"))
                generateSuggestedSku(v) else it.formSku
        )
    }
    fun updateFormBanho(v: String) = _uiState.update { it.copy(formBanho = v) }
    fun updateFormMilesimos(v: String) = _uiState.update { it.copy(formMilesimos = v.filter { ch -> ch.isDigit() }) }
    fun updateFormPedra(v: String) = _uiState.update { it.copy(formPedra = v) }
    fun updateFormPreco(v: String) = _uiState.update { it.copy(formPreco = v, formPrecoError = null) }
    fun updateFormPrecoPromocional(v: String) = _uiState.update { it.copy(formPrecoPromocional = v) }
    fun updateFormSku(v: String) = _uiState.update { it.copy(formSku = v) }
    fun updateFormLinkTikTok(v: String) = _uiState.update { it.copy(formLinkTikTok = v) }
    fun updateFormEstoque(v: String) = _uiState.update { it.copy(formEstoque = v.filter { ch -> ch.isDigit() }) }
    fun updateFormObservacoes(v: String) = _uiState.update { it.copy(formObservacoes = v) }

    fun addPhoto(uri: String) {
        _uiState.update { state ->
            val updated = state.formFotos + uri
            state.copy(formFotos = updated)
        }
    }

    fun addPhotos(uris: List<String>) {
        _uiState.update { state ->
            val updated = state.formFotos + uris
            state.copy(formFotos = updated.distinct())
        }
    }

    fun removePhoto(index: Int) {
        _uiState.update { state ->
            if (index in state.formFotos.indices) {
                val updated = state.formFotos.toMutableList().apply { removeAt(index) }
                state.copy(formFotos = updated)
            } else state
        }
    }

    fun savePeca(): Boolean {
        val state = _uiState.value
        val nome = state.formNome.trim()
        val tipo = state.formTipo.trim()
        val precoText = state.formPreco.trim().replace(",", ".")
        val preco = precoText.toDoubleOrNull()

        var hasError = false
        var nomeErr: String? = null
        var tipoErr: String? = null
        var precoErr: String? = null

        if (nome.isBlank()) {
            nomeErr = "Informe o nome da semijoia"
            hasError = true
        }

        if (tipo.isBlank() || !TipoPecaOptions.all.contains(tipo)) {
            tipoErr = "Selecione o tipo da semijoia"
            hasError = true
        }

        if (preco == null || preco <= 0.0) {
            precoErr = "Informe um preço válido (ex: 89.90)"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    formNomeError = nomeErr,
                    formTipoError = tipoErr,
                    formPrecoError = precoErr
                )
            }
            return false
        }

        val precoPromocional = state.formPrecoPromocional.trim().replace(",", ".").toDoubleOrNull()
        val milesimos = state.formMilesimos.toIntOrNull() ?: 10
        val estoque = state.formEstoque.toIntOrNull() ?: 1
        val sku = state.formSku.trim().ifBlank { generateSuggestedSku(tipo) }
        val linkTikTok = state.formLinkTikTok.trim().ifBlank { null }
        val fotos = state.formFotos

        viewModelScope.launch {
            val editing = state.editingPeca
            if (editing != null) {
                val updated = editing.copy(
                    nome = nome,
                    tipo = tipo,
                    banho = state.formBanho,
                    milesimos = milesimos,
                    pedra = state.formPedra,
                    preco = preco!!,
                    precoPromocional = precoPromocional,
                    codigoSku = sku,
                    linkTikTokShop = linkTikTok,
                    fotos = fotos,
                    estoque = estoque,
                    observacoes = state.formObservacoes.trim()
                )
                repository.updatePeca(updated)
                _uiState.update {
                    it.copy(
                        isAddEditOpen = false,
                        editingPeca = null,
                        selectedPeca = updated,
                        snackbarMessage = "Peça \"$nome\" atualizada com sucesso! ✨"
                    )
                }
            } else {
                val newPeca = PecaEntity(
                    nome = nome,
                    tipo = tipo,
                    banho = state.formBanho,
                    milesimos = milesimos,
                    pedra = state.formPedra,
                    preco = preco!!,
                    precoPromocional = precoPromocional,
                    codigoSku = sku,
                    linkTikTokShop = linkTikTok,
                    fotos = fotos,
                    estoque = estoque,
                    observacoes = state.formObservacoes.trim()
                )
                val newId = repository.insertPeca(newPeca)
                val createdPeca = newPeca.copy(id = newId)
                _uiState.update {
                    it.copy(
                        isAddEditOpen = false,
                        selectedPeca = createdPeca,
                        snackbarMessage = "Peça \"$nome\" cadastrada com sucesso! 💎"
                    )
                }
            }
        }
        return true
    }

    fun requestDeletePeca(peca: PecaEntity) {
        _uiState.update {
            it.copy(isDeleteConfirmOpen = true, pecaPendingDelete = peca)
        }
    }

    fun dismissDelete() {
        _uiState.update {
            it.copy(isDeleteConfirmOpen = false, pecaPendingDelete = null)
        }
    }

    fun confirmDeletePeca() {
        val peca = _uiState.value.pecaPendingDelete ?: return
        viewModelScope.launch {
            repository.deletePeca(peca)
            _uiState.update {
                it.copy(
                    isDeleteConfirmOpen = false,
                    pecaPendingDelete = null,
                    selectedPeca = if (it.selectedPeca?.id == peca.id) null else it.selectedPeca,
                    snackbarMessage = "Peça \"${peca.nome}\" excluída."
                )
            }
        }
    }

    // Video linkage
    fun recordVideoForPeca(
        pecaId: Long,
        mediaUri: String,
        duracaoSeg: Int = 15,
        gancho: String = ""
    ) {
        viewModelScope.launch {
            val peca = _uiState.value.pecas.find { it.id == pecaId }
            val video = VideoEntity(
                pecaId = pecaId,
                uri = mediaUri,
                duracaoSeg = duracaoSeg,
                gancho = gancho.ifBlank { "Brilho impecável da peça ${peca?.nome ?: ""}" },
                status = StatusVideoOptions.GRAVADO,
                legenda = "Olha o acabamento e o banho ${peca?.milesimos ?: 10}M dessa peça! ✨ Link na sacolinha!",
                hashtags = "#semijoias #${peca?.tipo?.lowercase() ?: "acessorios"} #tiktokshopbrasil #joiasdeluxo"
            )
            repository.insertVideo(video)
            _uiState.update {
                it.copy(snackbarMessage = "Vídeo vinculado à peça com status GRAVADO! 🎥")
            }
        }
    }

    private var photoAnalysisJob: kotlinx.coroutines.Job? = null

    /**
     * Analisa a foto selecionada com o Gemini Multimodal e pré-preenche o formulário.
     */
    fun analyzePhoto(context: android.content.Context, photoUri: String) {
        photoAnalysisJob?.cancel()
        _uiState.update { it.copy(isAnalyzingPhoto = true, photoAnalysisApplied = false) }

        photoAnalysisJob = viewModelScope.launch {
            val uri = android.net.Uri.parse(photoUri)
            val result = geminiService.analyzeJewelryPhoto(context, uri)

            when (result) {
                is com.example.data.service.GeminiServiceResult.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        current.copy(
                            isAnalyzingPhoto = false,
                            formNome = if (current.formNome.isBlank()) data.nomeComercial else current.formNome,
                            formTipo = if (TipoPecaOptions.all.contains(data.tipo)) data.tipo else current.formTipo,
                            formBanho = if (BanhoPecaOptions.all.contains(data.corDoBanho)) data.corDoBanho else current.formBanho,
                            formPedra = if (PedraPecaOptions.all.contains(data.pedra)) data.pedra else current.formPedra,
                            formObservacoes = if (current.formObservacoes.isBlank()) data.descricaoCurta else "${current.formObservacoes}\n${data.descricaoCurta}",
                            photoAnalysisIdeas = data.ideiasDeVideo,
                            photoAnalysisApplied = true,
                            snackbarMessage = "Dados da foto identificados pelo Gemini! Revise os campos ✨"
                        )
                    }
                }
                is com.example.data.service.GeminiServiceResult.Error -> {
                    val fallback = result.fallbackTemplate
                    _uiState.update { current ->
                        current.copy(
                            isAnalyzingPhoto = false,
                            formNome = if (current.formNome.isBlank() && fallback != null) fallback.nomeComercial else current.formNome,
                            photoAnalysisIdeas = fallback?.ideiasDeVideo ?: emptyList(),
                            photoAnalysisApplied = fallback != null,
                            snackbarMessage = "${result.errorMessage} (Modelo pronto sugerido)"
                        )
                    }
                }
            }
        }
    }

    fun cancelPhotoAnalysis() {
        photoAnalysisJob?.cancel()
        _uiState.update {
            it.copy(
                isAnalyzingPhoto = false,
                snackbarMessage = "Análise de foto cancelada."
            )
        }
    }

    /**
     * Gera legenda e hashtags avulsas para um vídeo gravado da peça.
     */
    fun generateCaptionsForVideo(videoId: Long, peca: PecaEntity, gancho: String) {
        _uiState.update { it.copy(generatingCaptionForVideoId = videoId) }

        viewModelScope.launch {
            val result = geminiService.generateCaptionsAndHashtags(peca, gancho)
            when (result) {
                is com.example.data.service.GeminiServiceResult.Success -> {
                    val data = result.data
                    val hashtagsStr = data.hashtags.joinToString(" ")
                    repository.updateCaptionAndHashtags(videoId, data.legenda, hashtagsStr)
                    _uiState.update {
                        it.copy(
                            generatingCaptionForVideoId = null,
                            snackbarMessage = "Legenda e hashtags geradas com IA! ✨"
                        )
                    }
                }
                is com.example.data.service.GeminiServiceResult.Error -> {
                    val fallback = result.fallbackTemplate
                    if (fallback != null) {
                        val hashtagsStr = fallback.hashtags.joinToString(" ")
                        repository.updateCaptionAndHashtags(videoId, fallback.legenda, hashtagsStr)
                    }
                    _uiState.update {
                        it.copy(
                            generatingCaptionForVideoId = null,
                            snackbarMessage = "${result.errorMessage} (Modelo pronto aplicado)"
                        )
                    }
                }
            }
        }
    }

    /**
     * Salva ou atualiza um roteiro gerado para a peça no banco de dados com status ROTEIRIZADO.
     */
    fun saveScriptForPeca(
        peca: PecaEntity,
        scriptResult: com.example.data.model.GeminiScriptResult,
        duracaoSeg: Int
    ) {
        viewModelScope.launch {
            val scriptJson = com.example.data.service.GeminiConfig.jsonParser.encodeToString(
                com.example.data.model.GeminiScriptResult.serializer(),
                scriptResult
            )
            val video = VideoEntity(
                pecaId = peca.id,
                uri = "",
                duracaoSeg = duracaoSeg,
                gancho = scriptResult.gancho,
                roteiroJson = scriptJson,
                legenda = scriptResult.legendaPost,
                hashtags = scriptResult.hashtags.joinToString(" "),
                status = StatusVideoOptions.ROTEIRIZADO
            )
            repository.insertVideo(video)
            _uiState.update {
                it.copy(snackbarMessage = "Roteiro salvo com status ROTEIRIZADO para \"${peca.nome}\"! 🎬")
            }
        }
    }

    fun updateVideoMetrics(
        id: Long,
        views: Long,
        curtidas: Long,
        comentarios: Long,
        compartilhamentos: Long,
        salvamentos: Long,
        vendas: Long,
        dataPostagem: Long
    ) {
        viewModelScope.launch {
            repository.updateVideoMetrics(
                id = id,
                views = views,
                curtidas = curtidas,
                comentarios = comentarios,
                compartilhamentos = compartilhamentos,
                salvamentos = salvamentos,
                vendas = vendas,
                dataPostagem = dataPostagem,
                status = StatusVideoOptions.POSTADO
            )
            _uiState.update { it.copy(snackbarMessage = "Métricas registradas com sucesso! 📊") }
        }
    }

    fun updateVideoMetrics(video: VideoEntity) {
        updateVideoMetrics(
            id = video.id,
            views = video.views,
            curtidas = video.curtidas,
            comentarios = video.comentarios,
            compartilhamentos = video.compartilhamentos,
            salvamentos = video.salvamentos,
            vendas = video.vendas,
            dataPostagem = if (video.dataPostagem != null && video.dataPostagem > 0) video.dataPostagem else System.currentTimeMillis()
        )
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun generateSuggestedSku(tipo: String): String {
        val prefix = when (tipo.lowercase()) {
            "colar" -> "COL"
            "brinco" -> "BRI"
            "anel" -> "ANE"
            "pulseira" -> "PUL"
            "conjunto" -> "CON"
            "tornozeleira" -> "TOR"
            else -> "PEC"
        }
        val randomSuffix = (100..999).random()
        return "$prefix-$randomSuffix"
    }
}
