package com.example.editor

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.data.repository.PecaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class EditorUiState(
    val editSpec: EditSpec = EditSpec(),
    val videoEntity: VideoEntity? = null,
    val pecaEntity: PecaEntity? = null,
    val videoDurationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportResultUri: String? = null,
    val exportErrorMessage: String? = null,
    // Cover Generator
    val isCoverModalOpen: Boolean = false,
    val coverHeadline: String = "",
    val coverPriceTag: String = "",
    val coverSelectedStyle: HookTextStyle = HookTextStyle.GOLD_ELEGANT,
    val extractedCoverBitmap: Bitmap? = null,
    val isSavingCover: Boolean = false,
    val savedCoverUri: String? = null,
    val snackbarMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PecaRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        PecaRepository(db.pecaDao(), db.videoDao())
    }

    private val exporter by lazy { Media3VideoExporter(application) }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<VideoEntity>()
        )

    fun loadVideo(video: VideoEntity, peca: PecaEntity?) {
        val initialHookText = extractHookTextFromScript(video)
        val initialPriceTag = peca?.let {
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(it.preco)
        } ?: ""

        val initialTrim = EditSpecValidator.validateTrim(
            startMs = 0L,
            endMs = if (video.duracaoSeg > 0) video.duracaoSeg * 1000L else 15000L,
            totalDurationMs = if (video.duracaoSeg > 0) video.duracaoSeg * 1000L else 15000L
        )

        val spec = EditSpec(
            videoUri = video.uri,
            pecaId = peca?.id ?: video.pecaId,
            pecaNome = peca?.nome ?: "Semijoia",
            videoId = video.id,
            legendaPost = video.legenda,
            hashtags = video.hashtags,
            trim = initialTrim,
            hookOverlay = HookOverlaySpec(
                text = initialHookText,
                durationMs = 3000L,
                style = HookTextStyle.TIKTOK_BOX,
                position = HookTextPosition.TOP
            ),
            colorAdjustments = ColorAdjustments(),
            audioMode = AudioMode.KEEP
        )

        _uiState.update {
            it.copy(
                editSpec = spec,
                videoEntity = video,
                pecaEntity = peca,
                videoDurationMs = initialTrim.videoDurationMs,
                exportResultUri = null,
                exportErrorMessage = null,
                coverHeadline = initialHookText.ifBlank { peca?.nome ?: "Semijoia de Luxo" },
                coverPriceTag = initialPriceTag
            )
        }
    }

    fun loadImportedVideo(uri: String, peca: PecaEntity?) {
        val dummyVideo = VideoEntity(
            pecaId = peca?.id ?: 0L,
            uri = uri,
            duracaoSeg = 15,
            gancho = "Brilho e acabamento impecável",
            roteiroJson = "{}",
            status = StatusVideoOptions.GRAVADO
        )
        loadVideo(dummyVideo, peca)
    }

    fun setVideoDuration(durationMs: Long) {
        val currentTrim = _uiState.value.editSpec.trim
        val validated = EditSpecValidator.validateTrim(
            startMs = currentTrim.startMs,
            endMs = if (currentTrim.endMs > 0L) currentTrim.endMs else durationMs,
            totalDurationMs = durationMs
        )
        _uiState.update {
            it.copy(
                videoDurationMs = durationMs,
                editSpec = it.editSpec.copy(trim = validated)
            )
        }
    }

    fun setTrim(startMs: Long, endMs: Long) {
        val duration = _uiState.value.videoDurationMs
        val validated = EditSpecValidator.validateTrim(startMs, endMs, duration)
        _uiState.update {
            it.copy(editSpec = it.editSpec.copy(trim = validated))
        }
    }

    fun setPlaybackPosition(posMs: Long) {
        _uiState.update { it.copy(currentPositionMs = posMs) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setBrightness(brightness: Float) {
        val clamped = EditSpecValidator.clampSlider(brightness, -0.5f, 0.5f)
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    colorAdjustments = it.editSpec.colorAdjustments.copy(brightness = clamped),
                    isJewelryGlowPresetApplied = false
                )
            )
        }
    }

    fun setContrast(contrast: Float) {
        val clamped = EditSpecValidator.clampSlider(contrast, -0.8f, 0.8f)
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    colorAdjustments = it.editSpec.colorAdjustments.copy(contrast = clamped),
                    isJewelryGlowPresetApplied = false
                )
            )
        }
    }

    fun setSaturation(saturation: Float) {
        val clamped = EditSpecValidator.clampSlider(saturation, -1.0f, 1.0f)
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    colorAdjustments = it.editSpec.colorAdjustments.copy(saturation = clamped),
                    isJewelryGlowPresetApplied = false
                )
            )
        }
    }

    fun toggleJewelryGlowPreset() {
        val current = _uiState.value.editSpec.isJewelryGlowPresetApplied
        if (!current) {
            _uiState.update {
                it.copy(
                    editSpec = it.editSpec.copy(
                        colorAdjustments = ColorAdjustments.JEWELRY_GLOW_PRESET,
                        isJewelryGlowPresetApplied = true
                    ),
                    snackbarMessage = "Preset 'Brilho de Joia' ativado! ✨"
                )
            }
        } else {
            resetColorAdjustments()
        }
    }

    fun resetColorAdjustments() {
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    colorAdjustments = ColorAdjustments(),
                    isJewelryGlowPresetApplied = false
                )
            )
        }
    }

    fun setAudioMode(mode: AudioMode) {
        _uiState.update {
            it.copy(editSpec = it.editSpec.copy(audioMode = mode))
        }
    }

    fun setHookText(text: String) {
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    hookOverlay = it.editSpec.hookOverlay.copy(text = text)
                )
            )
        }
    }

    fun setHookStyle(style: HookTextStyle) {
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    hookOverlay = it.editSpec.hookOverlay.copy(style = style)
                )
            )
        }
    }

    fun setHookPosition(pos: HookTextPosition) {
        _uiState.update {
            it.copy(
                editSpec = it.editSpec.copy(
                    hookOverlay = it.editSpec.hookOverlay.copy(position = pos)
                )
            )
        }
    }

    fun startExport() {
        val spec = _uiState.value.editSpec
        if (spec.videoUri.isBlank()) {
            _uiState.update { it.copy(exportErrorMessage = "Nenhum vídeo selecionado para exportação.") }
            return
        }

        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = 0f,
                exportResultUri = null,
                exportErrorMessage = null
            )
        }

        viewModelScope.launch {
            val result = exporter.exportVideo(spec) { progress ->
                _uiState.update { it.copy(exportProgress = progress) }
            }

            when (result) {
                is Media3VideoExporter.ExportResult.Success -> {
                    val newUri = result.mediaStoreUri.toString()
                    val videoId = spec.videoId
                    if (videoId != null && videoId > 0L) {
                        repository.updateVideoStatus(videoId, StatusVideoOptions.EDITADO)
                        val currentVideo = repository.getVideoByIdSync(videoId)
                        if (currentVideo != null) {
                            repository.updateVideo(
                                currentVideo.copy(
                                    uri = newUri,
                                    status = StatusVideoOptions.EDITADO,
                                    duracaoSeg = (spec.trim.durationTrimmedMs / 1000L).toInt().coerceAtLeast(1)
                                )
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportProgress = 1f,
                            exportResultUri = newUri,
                            snackbarMessage = "Vídeo MP4 renderizado com sucesso em Movies/GlowStudio! 🎬"
                        )
                    }
                }
                is Media3VideoExporter.ExportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportErrorMessage = result.message
                        )
                    }
                }
                is Media3VideoExporter.ExportResult.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            snackbarMessage = "Exportação cancelada."
                        )
                    }
                }
            }
        }
    }

    fun cancelExport() {
        exporter.cancelExport()
        _uiState.update {
            it.copy(isExporting = false, snackbarMessage = "Cancelando exportação...")
        }
    }

    // Cover Generator Methods
    fun openCoverModal() {
        val spec = _uiState.value.editSpec
        val position = _uiState.value.currentPositionMs
        viewModelScope.launch {
            val frame = CoverGeneratorHelper.extractFrame(
                context = getApplication(),
                videoUri = spec.videoUri,
                timeMs = position
            )
            _uiState.update {
                it.copy(
                    isCoverModalOpen = true,
                    extractedCoverBitmap = frame,
                    savedCoverUri = null
                )
            }
        }
    }

    fun closeCoverModal() {
        _uiState.update { it.copy(isCoverModalOpen = false) }
    }

    fun setCoverHeadline(headline: String) {
        _uiState.update { it.copy(coverHeadline = headline) }
    }

    fun setCoverPriceTag(price: String) {
        _uiState.update { it.copy(coverPriceTag = price) }
    }

    fun setCoverStyle(style: HookTextStyle) {
        _uiState.update { it.copy(coverSelectedStyle = style) }
    }

    fun scrubCoverFrame(timeMs: Long) {
        val spec = _uiState.value.editSpec
        viewModelScope.launch {
            val frame = CoverGeneratorHelper.extractFrame(
                context = getApplication(),
                videoUri = spec.videoUri,
                timeMs = timeMs
            )
            _uiState.update {
                it.copy(
                    extractedCoverBitmap = frame,
                    currentPositionMs = timeMs
                )
            }
        }
    }

    fun saveCover() {
        val baseBitmap = _uiState.value.extractedCoverBitmap ?: return
        val headline = _uiState.value.coverHeadline
        val priceTag = _uiState.value.coverPriceTag
        val style = _uiState.value.coverSelectedStyle
        val pecaNome = _uiState.value.editSpec.pecaNome

        _uiState.update { it.copy(isSavingCover = true) }

        viewModelScope.launch {
            val rendered = CoverGeneratorHelper.renderCoverWithHeadline(
                baseBitmap = baseBitmap,
                headline = headline,
                priceTag = priceTag,
                style = style
            )

            val uri = CoverGeneratorHelper.saveCoverToGallery(
                context = getApplication(),
                bitmap = rendered,
                filenamePrefix = "Capa_${pecaNome.replace(" ", "_")}"
            )

            _uiState.update {
                it.copy(
                    isSavingCover = false,
                    savedCoverUri = uri?.toString(),
                    isCoverModalOpen = false,
                    snackbarMessage = if (uri != null) "Capa salva com sucesso em Fotos/GlowStudio! 🖼️" else "Falha ao salvar capa."
                )
            }
        }
    }

    fun shareToTikTok(context: Context) {
        val state = _uiState.value
        val resultUriStr = state.exportResultUri ?: state.editSpec.videoUri
        if (resultUriStr.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Nenhum vídeo exportado para compartilhar.") }
            return
        }

        val fullCaption = buildString {
            if (state.editSpec.legendaPost.isNotBlank()) {
                append(state.editSpec.legendaPost)
            } else {
                append("Olha o acabamento e brilho dessa ${state.editSpec.pecaNome}! ✨")
            }
            if (state.editSpec.hashtags.isNotBlank()) {
                append("\n\n")
                append(state.editSpec.hashtags)
            } else {
                append("\n\n#semijoias #tiktokshopbrasil #ouro18k")
            }
        }

        // Copia a legenda para a área de transferência
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Legenda TikTok", fullCaption)
        clipboard?.setPrimaryClip(clip)

        // Prepara intent ACTION_SEND para o TikTok
        val videoUri = Uri.parse(resultUriStr)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            putExtra(Intent.EXTRA_TEXT, fullCaption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Tenta direcionar preferencialmente para o app oficial do TikTok
        val tiktokPackage = "com.zhiliaoapp.musically"
        val pm = context.packageManager
        val tiktokIntent = Intent(shareIntent).apply { `package` = tiktokPackage }
        val resolveInfo = pm.resolveActivity(tiktokIntent, 0)

        if (resolveInfo != null) {
            context.startActivity(tiktokIntent)
        } else {
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar no TikTok"))
        }

        _uiState.update {
            it.copy(snackbarMessage = "Legenda copiada para a área de transferência — cole no TikTok! 📋")
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun extractHookTextFromScript(video: VideoEntity): String {
        if (video.roteiroJson.isNotBlank() && video.roteiroJson != "{}") {
            try {
                val json = JSONObject(video.roteiroJson)
                if (json.has("textoNaTela")) {
                    val txt = json.getString("textoNaTela").trim()
                    if (txt.isNotBlank()) return txt
                }
                if (json.has("gancho")) {
                    val txt = json.getString("gancho").trim()
                    if (txt.isNotBlank()) return txt.take(35)
                }
            } catch (_: Exception) {}
        }
        return video.gancho.take(35)
    }
}
