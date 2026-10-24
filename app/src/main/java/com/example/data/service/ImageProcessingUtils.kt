package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageProcessingUtils {

    private const val MAX_DIMENSION = 1024

    /**
     * Carrega a imagem da Uri, redimensiona para no máximo 1024px no maior lado e codifica em Base64.
     */
    suspend fun loadAndResizeImageAsBase64(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            // 1. Decodificar apenas as dimensões primeiro (subamostragem eficiente)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxSide = max(options.outWidth, options.outHeight)
            if (maxSide > MAX_DIMENSION) {
                var currentSize = maxSide
                while (currentSize / 2 >= MAX_DIMENSION) {
                    inSampleSize *= 2
                    currentSize /= 2
                }
            }

            // 2. Decodificar imagem real com inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = resolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 3. Redimensionamento final preciso para garantir max dimension <= 1024
            val scaledBitmap = if (rawBitmap.width > MAX_DIMENSION || rawBitmap.height > MAX_DIMENSION) {
                val ratio = if (rawBitmap.width >= rawBitmap.height) {
                    MAX_DIMENSION.toFloat() / rawBitmap.width
                } else {
                    MAX_DIMENSION.toFloat() / rawBitmap.height
                }
                val targetW = (rawBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (rawBitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                if (scaled != rawBitmap) {
                    rawBitmap.recycle()
                }
                scaled
            } else {
                rawBitmap
            }

            // 4. Comprimir como JPEG e converter para Base64
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            scaledBitmap.recycle()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
