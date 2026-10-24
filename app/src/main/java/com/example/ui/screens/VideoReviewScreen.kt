package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.components.GlowButton
import com.example.ui.theme.*

@OptIn(UnstableApi::class)
@Composable
fun VideoReviewScreen(
    videoUri: Uri,
    onRetake: () -> Unit,
    onUseInStudio: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    // Initialize ExoPlayer for playback loop
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("video_review_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceElevated)
                            .testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar e Regravar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Vídeo Gravado com Sucesso",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "1080x1920 • 9:16 FHD • Movies/GlowStudio",
                            color = Gold18k,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "✨ Semijoia Macro",
                        color = TikTokCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 9:16 Video Player Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Gold18k.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .clickable {
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Play / Pause Indicator overlay
                if (!isPlaying) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Gold18k),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproduzir",
                                tint = Gold18k,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom badge indicating Faceless Protection
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(ObsidianBackground.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔒 Gravação Sem Rosto • Detalhe 100% nas Peças",
                        color = Gold18k,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Usar no Estúdio
                Button(
                    onClick = { onUseInStudio(videoUri) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("use_in_studio_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink)
                ) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Usar no Estúdio & Adicionar Selos",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Retake Button
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("retake_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Regravar", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    // Share to TikTok Button
                    Button(
                        onClick = {
                            shareVideoToTikTok(context, videoUri)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("share_tiktok_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                        border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.6f))
                    ) {
                        Text(text = "🚀", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Postar no TikTok",
                            color = Gold18k,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates an ACTION_SEND intent with video/mp4, targeting TikTok package if installed
 * or offering system chooser with TikTok prominently available.
 */
private fun shareVideoToTikTok(context: Context, videoUri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra(
            Intent.EXTRA_TEXT,
            "Olha esse brilho impecável! ✨ Garanta o seu na sacolinha amarela com cupom de primeira compra! #semijoias #tiktokshopbrasil #unboxingsemijoias"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val pm = context.packageManager
    val tiktokPackages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    var targetInstalledPackage: String? = null

    for (pkg in tiktokPackages) {
        try {
            pm.getPackageInfo(pkg, 0)
            targetInstalledPackage = pkg
            break
        } catch (_: Exception) {
            // Not installed, continue checking
        }
    }

    if (targetInstalledPackage != null) {
        shareIntent.setPackage(targetInstalledPackage)
        try {
            context.startActivity(shareIntent)
            return
        } catch (_: Exception) {
            shareIntent.setPackage(null)
        }
    }

    // Fallback to system share chooser
    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Vídeo da Semijoia"))
}
