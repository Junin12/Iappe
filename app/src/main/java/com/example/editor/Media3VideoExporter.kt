package com.example.editor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

@androidx.annotation.OptIn(UnstableApi::class)
class Media3VideoExporter(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private var progressJob: Job? = null

    sealed class ExportResult {
        data class Success(val mediaStoreUri: Uri, val localFileUri: Uri, val filePath: String) : ExportResult()
        data class Error(val message: String, val throwable: Throwable? = null) : ExportResult()
        object Cancelled : ExportResult()
    }

    suspend fun exportVideo(
        spec: EditSpec,
        onProgress: (Float) -> Unit
    ): ExportResult = withContext(Dispatchers.Main) {
        val tempOutputFile = File(
            context.cacheDir,
            "export_temp_${System.currentTimeMillis()}.mp4"
        )
        if (tempOutputFile.exists()) {
            tempOutputFile.delete()
        }

        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(spec.trim.startMs)
            .setEndPositionMs(if (spec.trim.endMs > 0L) spec.trim.endMs else Long.MAX_VALUE)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(spec.videoUri)
            .setClippingConfiguration(clipping)
            .build()

        val videoEffects = Media3EffectsFactory.buildEffectsList(spec)
        val audioEffects = emptyList<androidx.media3.common.audio.AudioProcessor>()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(audioEffects, videoEffects))
            .setRemoveAudio(spec.audioMode == AudioMode.MUTE)
            .build()

        val resultCompletable = CompletableDeferred<ExportResult>()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: androidx.media3.transformer.ExportResult) {
                progressJob?.cancel()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val saved = saveToMoviesDirectory(context, tempOutputFile, spec.pecaNome)
                        resultCompletable.complete(saved)
                    } catch (e: Exception) {
                        resultCompletable.complete(
                            ExportResult.Error("Falha ao salvar no diretório Movies: ${e.message}", e)
                        )
                    }
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: androidx.media3.transformer.ExportResult,
                exportException: ExportException
            ) {
                progressJob?.cancel()
                resultCompletable.complete(
                    ExportResult.Error(exportException.localizedMessage ?: "Erro na renderização do vídeo", exportException)
                )
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        activeTransformer = transformer

        try {
            transformer.start(editedMediaItem, tempOutputFile.absolutePath)
        } catch (e: Exception) {
            return@withContext ExportResult.Error("Não foi possível iniciar o Transformer: ${e.message}", e)
        }

        // Monitoramento contínuo de progresso
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            val progressHolder = ProgressHolder()
            while (isActive && !resultCompletable.isCompleted) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val progressNormalized = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                    onProgress(progressNormalized)
                }
                delay(100)
            }
        }

        val finalResult = resultCompletable.await()
        progressJob?.cancel()
        activeTransformer = null
        finalResult
    }

    fun cancelExport() {
        progressJob?.cancel()
        activeTransformer?.cancel()
        activeTransformer = null
    }

    private fun saveToMoviesDirectory(
        context: Context,
        tempFile: File,
        pecaNome: String
    ): ExportResult {
        val safeName = pecaNome.replace("[^a-zA-Z0-9]".toRegex(), "_").take(15)
        val filename = "GLOW_${if (safeName.isNotBlank()) safeName else "Semijoia"}_${System.currentTimeMillis()}.mp4"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/GlowStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Falha ao criar entrada no MediaStore")

        resolver.openOutputStream(videoUri)?.use { out ->
            FileInputStream(tempFile).use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(videoUri, contentValues, null, null)
        }

        // Gera também URI segura com FileProvider caso o app precise
        val fileProviderUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            videoUri
        }

        return ExportResult.Success(
            mediaStoreUri = videoUri,
            localFileUri = fileProviderUri,
            filePath = tempFile.absolutePath
        )
    }
}
