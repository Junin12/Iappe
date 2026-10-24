package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.data.repository.PecaRepository
import com.example.data.worker.PostReminderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class WeekDayItem(
    val dayOfWeekName: String,
    val dayOfMonth: Int,
    val dateMidnightMs: Long,
    val isToday: Boolean,
    val videos: List<VideoWithPeca> = emptyList()
)

data class VideoWithPeca(
    val video: VideoEntity,
    val peca: PecaEntity?
)

data class PlannerUiState(
    val currentWeekDays: List<WeekDayItem> = emptyList(),
    val unscheduledVideos: List<VideoWithPeca> = emptyList(),
    val allVideos: List<VideoEntity> = emptyList(),
    val allPecas: List<PecaEntity> = emptyList(),
    val weekOffset: Int = 0, // 0 = current week, -1 = past, +1 = next
    // Scheduling Dialog
    val schedulingVideo: VideoWithPeca? = null,
    val selectedScheduleDayMs: Long = 0L,
    val selectedScheduleHour: Int = 18,
    val selectedScheduleMinute: Int = 30,
    // Metrics Registration Dialog
    val registeringMetricsVideo: VideoWithPeca? = null,
    val viewsInput: String = "",
    val curtidasInput: String = "",
    val comentariosInput: String = "",
    val compartilhamentosInput: String = "",
    val salvamentosInput: String = "",
    val vendasInput: String = "",
    val snackbarMessage: String? = null
)

class PlannerViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PecaRepository = PecaRepository(
        AppDatabase.getDatabase(application).pecaDao(),
        AppDatabase.getDatabase(application).videoDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    init {
        viewModelScope.launch {
            combine(
                repository.allVideos,
                repository.allPecas,
                _uiState.map { it.weekOffset }.distinctUntilChanged()
            ) { videos, pecas, offset ->
                buildPlannerState(videos, pecas, offset)
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        schedulingVideo = current.schedulingVideo,
                        selectedScheduleDayMs = current.selectedScheduleDayMs,
                        selectedScheduleHour = current.selectedScheduleHour,
                        selectedScheduleMinute = current.selectedScheduleMinute,
                        registeringMetricsVideo = current.registeringMetricsVideo,
                        viewsInput = current.viewsInput,
                        curtidasInput = current.curtidasInput,
                        comentariosInput = current.comentariosInput,
                        compartilhamentosInput = current.compartilhamentosInput,
                        salvamentosInput = current.salvamentosInput,
                        vendasInput = current.vendasInput,
                        snackbarMessage = current.snackbarMessage
                    )
                }
            }
        }
    }

    private fun buildPlannerState(
        videos: List<VideoEntity>,
        pecas: List<PecaEntity>,
        weekOffset: Int
    ): PlannerUiState {
        val pecaMap = pecas.associateBy { it.id }
        val videosWithPeca = videos.map { VideoWithPeca(it, pecaMap[it.pecaId]) }

        // Gera 7 dias da semana com base no offset
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        // Começa na Segunda-feira da semana
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayCal = Calendar.getInstance()
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val todayYear = todayCal.get(Calendar.YEAR)

        val weekDays = (0..6).map { i ->
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val dateMs = dayCal.timeInMillis
            val isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear && dayCal.get(Calendar.YEAR) == todayYear

            val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Seg"
                Calendar.TUESDAY -> "Ter"
                Calendar.WEDNESDAY -> "Qua"
                Calendar.THURSDAY -> "Qui"
                Calendar.FRIDAY -> "Sex"
                Calendar.SATURDAY -> "Sáb"
                Calendar.SUNDAY -> "Dom"
                else -> "Dia"
            }

            // Filtra vídeos agendados ou postados neste dia
            val dayVideos = videosWithPeca.filter { vwp ->
                val time = vwp.video.dataAgendada ?: vwp.video.dataPostagem
                if (time != null && time > 0) {
                    val vCal = Calendar.getInstance().apply { timeInMillis = time }
                    vCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                            vCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                } else false
            }

            WeekDayItem(
                dayOfWeekName = dayName,
                dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                dateMidnightMs = dateMs,
                isToday = isToday,
                videos = dayVideos
            )
        }

        // Vídeos sem data agendada
        val unscheduled = videosWithPeca.filter {
            (it.video.dataAgendada == null || it.video.dataAgendada == 0L) &&
                    it.video.status != StatusVideoOptions.POSTADO
        }

        return PlannerUiState(
            currentWeekDays = weekDays,
            unscheduledVideos = unscheduled,
            allVideos = videos,
            allPecas = pecas,
            weekOffset = weekOffset
        )
    }

    fun nextWeek() {
        _uiState.update { it.copy(weekOffset = it.weekOffset + 1) }
    }

    fun previousWeek() {
        _uiState.update { it.copy(weekOffset = it.weekOffset - 1) }
    }

    fun resetToCurrentWeek() {
        _uiState.update { it.copy(weekOffset = 0) }
    }

    // 1-Tap Status Switch
    fun advanceStatus(video: VideoEntity) {
        val nextStatus = when (video.status) {
            StatusVideoOptions.IDEIA -> StatusVideoOptions.ROTEIRIZADO
            StatusVideoOptions.ROTEIRIZADO -> StatusVideoOptions.GRAVADO
            StatusVideoOptions.GRAVADO -> StatusVideoOptions.EDITADO
            StatusVideoOptions.EDITADO -> StatusVideoOptions.POSTADO
            StatusVideoOptions.POSTADO -> StatusVideoOptions.IDEIA
            else -> StatusVideoOptions.ROTEIRIZADO
        }

        if (nextStatus == StatusVideoOptions.POSTADO) {
            // Abrir registro de métricas automaticamente
            val peca = _uiState.value.allPecas.find { it.id == video.pecaId }
            openMetricsRegistration(VideoWithPeca(video, peca))
        } else {
            viewModelScope.launch {
                repository.updateVideoStatus(video.id, nextStatus)
                _uiState.update { it.copy(snackbarMessage = "Status alterado para $nextStatus") }
            }
        }
    }

    fun setSpecificStatus(video: VideoEntity, status: String) {
        if (status == StatusVideoOptions.POSTADO) {
            val peca = _uiState.value.allPecas.find { it.id == video.pecaId }
            openMetricsRegistration(VideoWithPeca(video, peca))
        } else {
            viewModelScope.launch {
                repository.updateVideoStatus(video.id, status)
                _uiState.update { it.copy(snackbarMessage = "Status atualizado: $status") }
            }
        }
    }

    // Scheduling Modal
    fun openScheduleModal(vwp: VideoWithPeca) {
        val currentSchedule = vwp.video.dataAgendada ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = currentSchedule }
        _uiState.update {
            it.copy(
                schedulingVideo = vwp,
                selectedScheduleDayMs = currentSchedule,
                selectedScheduleHour = cal.get(Calendar.HOUR_OF_DAY),
                selectedScheduleMinute = cal.get(Calendar.MINUTE)
            )
        }
    }

    fun dismissScheduleModal() {
        _uiState.update { it.copy(schedulingVideo = null) }
    }

    fun updateScheduleHour(hour: Int) = _uiState.update { it.copy(selectedScheduleHour = hour.coerceIn(0, 23)) }
    fun updateScheduleMinute(minute: Int) = _uiState.update { it.copy(selectedScheduleMinute = minute.coerceIn(0, 59)) }
    fun updateScheduleDay(dayMs: Long) = _uiState.update { it.copy(selectedScheduleDayMs = dayMs) }

    fun confirmSchedule() {
        val state = _uiState.value
        val vwp = state.schedulingVideo ?: return

        val cal = Calendar.getInstance().apply {
            timeInMillis = state.selectedScheduleDayMs
            set(Calendar.HOUR_OF_DAY, state.selectedScheduleHour)
            set(Calendar.MINUTE, state.selectedScheduleMinute)
            set(Calendar.SECOND, 0)
        }
        val targetTimestamp = cal.timeInMillis
        val pecaNome = vwp.peca?.nome ?: "Semijoia #${vwp.video.pecaId}"

        viewModelScope.launch {
            repository.updateVideoSchedule(vwp.video.id, targetTimestamp)

            // Configurar WorkManager com notificação local agendada
            val delayMs = targetTimestamp - System.currentTimeMillis()
            val workTag = "video_reminder_${vwp.video.id}"
            workManager.cancelAllWorkByTag(workTag)

            if (delayMs > 0) {
                val inputData = Data.Builder()
                    .putString(PostReminderWorker.KEY_PECA_NOME, pecaNome)
                    .putLong(PostReminderWorker.KEY_VIDEO_ID, vwp.video.id)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<PostReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag(workTag)
                    .build()

                workManager.enqueue(workRequest)
            }

            _uiState.update {
                it.copy(
                    schedulingVideo = null,
                    snackbarMessage = "Lembrete agendado com sucesso para ${vwp.peca?.nome ?: "o vídeo"}! ⏰"
                )
            }
        }
    }

    fun removeSchedule(video: VideoEntity) {
        viewModelScope.launch {
            repository.updateVideoSchedule(video.id, null)
            workManager.cancelAllWorkByTag("video_reminder_${video.id}")
            _uiState.update { it.copy(snackbarMessage = "Agendamento removido.") }
        }
    }

    // Metrics Registration
    fun openMetricsRegistration(vwp: VideoWithPeca) {
        _uiState.update {
            it.copy(
                registeringMetricsVideo = vwp,
                viewsInput = if (vwp.video.views > 0) vwp.video.views.toString() else "",
                curtidasInput = if (vwp.video.curtidas > 0) vwp.video.curtidas.toString() else "",
                comentariosInput = if (vwp.video.comentarios > 0) vwp.video.comentarios.toString() else "",
                compartilhamentosInput = if (vwp.video.compartilhamentos > 0) vwp.video.compartilhamentos.toString() else "",
                salvamentosInput = if (vwp.video.salvamentos > 0) vwp.video.salvamentos.toString() else "",
                vendasInput = if (vwp.video.vendas > 0) vwp.video.vendas.toString() else ""
            )
        }
    }

    fun dismissMetricsRegistration() {
        _uiState.update { it.copy(registeringMetricsVideo = null) }
    }

    fun updateMetricsViews(v: String) = _uiState.update { it.copy(viewsInput = v.filter { ch -> ch.isDigit() }) }
    fun updateMetricsCurtidas(v: String) = _uiState.update { it.copy(curtidasInput = v.filter { ch -> ch.isDigit() }) }
    fun updateMetricsComentarios(v: String) = _uiState.update { it.copy(comentariosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateMetricsCompartilhamentos(v: String) = _uiState.update { it.copy(compartilhamentosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateMetricsSalvamentos(v: String) = _uiState.update { it.copy(salvamentosInput = v.filter { ch -> ch.isDigit() }) }
    fun updateMetricsVendas(v: String) = _uiState.update { it.copy(vendasInput = v.filter { ch -> ch.isDigit() }) }

    fun saveMetricsFromPlanner() {
        val state = _uiState.value
        val vwp = state.registeringMetricsVideo ?: return

        val views = state.viewsInput.toLongOrNull() ?: 0L
        val curtidas = state.curtidasInput.toLongOrNull() ?: 0L
        val comentarios = state.comentariosInput.toLongOrNull() ?: 0L
        val compartilhamentos = state.compartilhamentosInput.toLongOrNull() ?: 0L
        val salvamentos = state.salvamentosInput.toLongOrNull() ?: 0L
        val vendas = state.vendasInput.toLongOrNull() ?: 0L

        viewModelScope.launch {
            repository.updateVideoMetrics(
                id = vwp.video.id,
                views = views,
                curtidas = curtidas,
                comentarios = comentarios,
                compartilhamentos = compartilhamentos,
                salvamentos = salvamentos,
                vendas = vendas,
                dataPostagem = System.currentTimeMillis(),
                status = StatusVideoOptions.POSTADO
            )

            _uiState.update {
                it.copy(
                    registeringMetricsVideo = null,
                    snackbarMessage = "Vídeo marcado como POSTADO com métricas registradas! ✨"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
