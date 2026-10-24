package com.example.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object CoverGeneratorHelper {

    suspend fun extractFrame(
        context: Context,
        videoUri: String,
        timeMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (videoUri.startsWith("content://") || videoUri.startsWith("android.resource://")) {
                retriever.setDataSource(context, Uri.parse(videoUri))
            } else {
                retriever.setDataSource(videoUri)
            }
            val timeUs = (timeMs * 1000L).coerceAtLeast(0L)
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(timeUs)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun renderCoverWithHeadline(
        baseBitmap: Bitmap,
        headline: String,
        priceTag: String = "",
        style: HookTextStyle = HookTextStyle.GOLD_ELEGANT
    ): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = mutableBitmap.width
        val height = mutableBitmap.height

        // Sombra / Vinheta sutil na parte inferior e superior para garantir legibilidade
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.45f,
                0f, height * 0.95f,
                Color.TRANSPARENT,
                Color.argb(210, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.45f, width.toFloat(), height.toFloat(), gradientPaint)

        if (headline.isNotBlank()) {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (width * 0.065f).coerceIn(36f, 72f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = when (style) {
                    HookTextStyle.TIKTOK_BOX -> Color.WHITE
                    HookTextStyle.GOLD_ELEGANT -> Color.rgb(255, 215, 0)
                    HookTextStyle.WHITE_OUTLINE -> Color.WHITE
                }
                setShadowLayer(10f, 4f, 4f, Color.BLACK)
            }

            val maxTextWidth = (width * 0.85f).toInt()
            val staticLayout = StaticLayout.Builder.obtain(
                headline.uppercase(),
                0,
                headline.length,
                textPaint,
                maxTextWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

            val textY = height * 0.65f
            val textX = (width - staticLayout.width) / 2f

            canvas.save()
            canvas.translate(textX, textY)
            staticLayout.draw(canvas)
            canvas.restore()
        }

        // Tag de Preço opcional no canto
        if (priceTag.isNotBlank()) {
            val tagBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(254, 44, 85) // TikTok Red/Pink
            }
            val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (width * 0.042f).coerceIn(24f, 48f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textBounds = Rect()
            tagTextPaint.getTextBounds(priceTag, 0, priceTag.length, textBounds)

            val tagPaddingH = 24f
            val tagPaddingV = 16f
            val tagLeft = width * 0.08f
            val tagTop = height * 0.82f
            val tagRight = tagLeft + textBounds.width() + (tagPaddingH * 2)
            val tagBottom = tagTop + textBounds.height() + (tagPaddingV * 2)

            canvas.drawRoundRect(tagLeft, tagTop, tagRight, tagBottom, 16f, 16f, tagBgPaint)
            canvas.drawText(
                priceTag,
                tagLeft + tagPaddingH,
                tagBottom - tagPaddingV,
                tagTextPaint
            )
        }

        return mutableBitmap
    }

    suspend fun saveCoverToGallery(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "Capa_Semijoia"
    ): Uri? = withContext(Dispatchers.IO) {
        val filename = "${filenamePrefix}_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GlowStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext null

        try {
            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            imageUri
        } catch (e: Exception) {
            resolver.delete(imageUri, null, null)
            null
        }
    }
}
