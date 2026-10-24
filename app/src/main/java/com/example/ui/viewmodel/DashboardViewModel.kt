package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.data.metrics.DashboardMetricsReport
import com.example.data.metrics.MetricsCalculator
import com.example.data.repository.PecaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val report: DashboardMetricsReport = DashboardMetricsReport(),
    val allPecas: List<PecaEntity> = emptyList(),
    val allVideos: List<VideoEntity> = emptyList(),
    val isLoading: Boolean = false,
    // Metrics update modal
    val editingMetricsVideo: VideoEntity? = null,
    val editingPecaNome: String = "",
    val viewsInput: String = "",
    val curtidasInput: String = "",
    val comentariosInput: String = "",
    val compartilhamentosInput: String = "",
    val salvamentosInput: String = "",
    val vendasInput: String = "",
    val dataPostagemInput: String = "",
    val snackbarMessage: String? = null
)

class DashboardViewModel(
    application: Application,
    private val repository: PecaRepository = PecaRepository(
        AppDatabase.getDatabase(application).pecaDao(),
        AppDatabase.getDatabase(application).videoDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        viewModelScope.launch {
            combine(repository.allVideos, repository.allPecas) { videos, pecas ->
                val report = MetricsCalculator.calculateReport(videos, pecas)
                Pair(videos, pecas) to report
            }.collect { (dataPair, report) ->
                val (videos, pecas) = dataPair
                _uiState.update { current ->
                    current.copy(
                        report = report,
                        allVideos = videos,
                        allPecas = pecas,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun openUpdateMetrics(video: VideoEntity) {
        val peca = _uiState.value.allPecas.find { it.id == video.pecaId }
        val postDate = if (video.dataPostagem != null && video.dataPostagem > 0) {
            dateFormat.format(Date(video.dataPostagem))
        } else {
            dateFormat.format(Date())
        }

        _uiState.update {
            it.copy(
                editingMetricsVideo = video,
                editingPecaNome = peca?.nome ?: "Semijoia #${video.pecaId}",
                viewsInput = if (video.views > 0) video.views.toString() else "",
                curtidasInput = if (video.curtidas > 0) video.curtidas.toString() else "",
                comentariosInput = if (video.comentarios > 0) video.comentarios.toString() else "",
                compartilhamentosInput = if (video.compartilhamentos > 0) video.compartilhamentos.toString() else "",
                salvamentosInput = if (video.salvamentos > 0) video.salvamentos.toString() else "",
                vendasInput = if (video.vendas > 0) video.vendas.toString() else "",
                dataPostagemInput = postDate
            )
        }
    }

    fun dismissUpdateMetrics() {
        _uiState.update { it.copy(editingMetricsVideo = null) }
    }

    fun updateViewsInput(v: String) = _uiState.update { it.copy(viewsInput = v.filter { ch -> ch.isDigit() }) }
    fun updateCurtidasInput(v: String) = _uiState.update { it.copy(curtidasInput = v.filter { ch -> ch.isDigit() }) }
    fun updateComentariosInput(v: String) = _uiState.update { it.copy(comentariosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateCompartilhamentosInput(v: String) = _uiState.update { it.copy(compartilhamentosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateSalvamentosInput(v: String) = _uiState.update { it.copy(salvamentosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateVendasInput(v: String) = _uiState.update { it.copy(vendasInput = v.filter { ch -> ch.isDigit() }) }
    fun updateDataPostagemInput(v: String) = _uiState.update { it.copy(dataPostagemInput = v) }

    fun saveMetrics() {
        val state = _uiState.value
        val video = state.editingMetricsVideo ?: return

        val views = state.viewsInput.toLongOrNull() ?: 0L
        val curtidas = state.curtidasInput.toLongOrNull() ?: 0L
        val comentarios = state.comentariosInput.toLongOrNull() ?: 0L
        val compartilhamentos = state.compartilhamentosInput.toLongOrNull() ?: 0L
        val salvamentos = state.salvamentosInput.toLongOrNull() ?: 0L
        val vendas = state.vendasInput.toLongOrNull() ?: 0L

        val postTimeMs = try {
            dateFormat.parse(state.dataPostagemInput)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        viewModelScope.launch {
            repository.updateVideoMetrics(
                id = video.id,
                views = views,
                curtidas = curtidas,
                comentarios = comentarios,
                compartilhamentos = compartilhamentos,
                salvamentos = salvamentos,
                vendas = vendas,
                dataPostagem = postTimeMs,
                status = StatusVideoOptions.POSTADO
            )

            _uiState.update {
                it.copy(
                    editingMetricsVideo = null,
                    snackbarMessage = "Métricas reais salvas com sucesso! 📊"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
