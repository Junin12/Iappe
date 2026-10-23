package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun VideoPreviewPlayer(
    editorState: VideoEditorState,
    shopBadges: List<ShopBadge>,
    onTogglePlay: () -> Unit,
    onEditCaptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sparkle pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_pulse")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )
    val captionBounce by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caption_bounce"
    )

    // Karaoke active word cycle animation
    val wordIndexAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "word_cycle"
    )

    val backgroundColors = editorState.selectedSample?.previewGradient
        ?: listOf(Color(0xFF140D24), Color(0xFF28133E), Color(0xFF0F0818))

    Box(
        modifier = modifier
            .widthIn(max = 340.dp)
            .aspectRatio(9f / 16f)
            .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = TikTokPink.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, ObsidianBorder, RoundedCornerShape(26.dp))
            .background(Color.Black)
            .clickable { onTogglePlay() }
            .testTag("video_preview_player")
    ) {
        // Base Simulated Video Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(backgroundColors))
        )

        // Studio Lighting Effects
        when (editorState.currentLighting) {
            LightingEffectType.RING_LIGHT -> {
                // Circular Ring Light Halo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.32f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f * editorState.lightingIntensity),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 16.dp.toPx())
                    )
                    drawCircle(
                        color = Gold18k.copy(alpha = 0.15f * editorState.lightingIntensity),
                        radius = radius + 6.dp.toPx(),
                        center = center,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            }
            LightingEffectType.LATERAL_FLASH -> {
                // Luxury Directional Flash Beam from top right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f * editorState.lightingIntensity),
                                    Gold18k.copy(alpha = 0.12f * editorState.lightingIntensity),
                                    Color.Transparent
                                ),
                                start = Offset(800f, 0f),
                                end = Offset(0f, 900f)
                            )
                        )
                )
            }
            LightingEffectType.SOFT_DIFFUSE -> {
                // Soft Boutique Light Bloom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Gold18k.copy(alpha = 0.18f * editorState.lightingIntensity),
                                    Color.Transparent
                                ),
                                radius = 750f
                            )
                        )
                )
            }
            LightingEffectType.NATURAL -> { /* standard */ }
        }

        // Color Grading Filter Overlays
        when (editorState.currentFilter) {
            VideoFilterType.ULTRA_GLOW -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Gold18k.copy(alpha = 0.22f * editorState.filterIntensity),
                                    TikTokPink.copy(alpha = 0.15f * editorState.filterIntensity),
                                    Color.Transparent
                                ),
                                radius = 600f
                            )
                        )
                )
            }
            VideoFilterType.DIAMOND_CRISP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    TikTokCyan.copy(alpha = 0.12f * editorState.filterIntensity),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f * editorState.filterIntensity)
                                ),
                                radius = 700f
                            )
                        )
                )
            }
            VideoFilterType.GOLD_18K -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GoldWarm.copy(alpha = 0.20f * editorState.filterIntensity),
                                    Color.Transparent,
                                    Gold18k.copy(alpha = 0.25f * editorState.filterIntensity)
                                )
                            )
                        )
                )
            }
            VideoFilterType.PLATINUM_MINIMAL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f * editorState.filterIntensity),
                                    Color.Transparent,
                                    DiamondSparkle.copy(alpha = 0.18f * editorState.filterIntensity)
                                )
                            )
                        )
                )
            }
            VideoFilterType.VELVET_MOOD -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF831843).copy(alpha = 0.25f * editorState.filterIntensity),
                                    Color.Black.copy(alpha = 0.65f * editorState.filterIntensity)
                                )
                            )
                        )
                )
            }
            VideoFilterType.NONE -> { /* No overlay */ }
        }

        // Animated Sparkle Glints on the Jewelry
        if (editorState.isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Sparkle 1 (Upper right stone)
                val c1 = Offset(w * 0.68f, h * 0.38f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 4.dp.toPx() * sparkleScale,
                    center = c1
                )
                drawLine(
                    color = Gold18k,
                    start = Offset(c1.x - 14.dp.toPx() * sparkleScale, c1.y),
                    end = Offset(c1.x + 14.dp.toPx() * sparkleScale, c1.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Gold18k,
                    start = Offset(c1.x, c1.y - 14.dp.toPx() * sparkleScale),
                    end = Offset(c1.x, c1.y + 14.dp.toPx() * sparkleScale),
                    strokeWidth = 2.dp.toPx()
                )

                // Sparkle 2 (Center stone)
                val c2 = Offset(w * 0.42f, h * 0.48f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 3.dp.toPx() * (2f - sparkleScale),
                    center = c2
                )
                drawLine(
                    color = TikTokPink,
                    start = Offset(c2.x - 10.dp.toPx(), c2.y),
                    end = Offset(c2.x + 10.dp.toPx(), c2.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = TikTokPink,
                    start = Offset(c2.x, c2.y - 10.dp.toPx()),
                    end = Offset(c2.x, c2.y + 10.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Sparkle 3 (Lower reflection)
                val c3 = Offset(w * 0.55f, h * 0.62f)
                drawCircle(
                    color = TikTokCyan.copy(alpha = 0.8f),
                    radius = 3.dp.toPx() * sparkleScale,
                    center = c3
                )
            }
        }

        // Top Header of TikTok Video
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (editorState.isPlaying) SuccessGreen else WarningAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (editorState.isPlaying) "AO VIVO 9:16" else "PAUSADO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Audio & Ducking pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (editorState.audioMixer.isDuckingEnabled) Gold18k.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎧", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (editorState.audioMixer.isDuckingEnabled) "Ducking Ativo (Voz 100% / Trilha 20%)" else "Áudio Normal",
                        color = Gold18k,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Right-side TikTok Interactive Icons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Gold18k, CircleShape)
                        .background(Brush.linearGradient(listOf(TikTokPink, VibrantPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 18.sp)
                }
                Box(
                    modifier = Modifier
                        .offset(y = 6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(TikTokPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Likes", tint = TikTokPink, modifier = Modifier.size(28.dp))
                Text(text = "34.2k", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.ModeComment, contentDescription = "Comentários", tint = Color.White, modifier = Modifier.size(26.dp))
                Text(text = "892", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Salvos", tint = Gold18k, modifier = Modifier.size(26.dp))
                Text(text = "6.1k", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White, modifier = Modifier.size(26.dp))
                Text(text = "1.5k", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Center Play / Pause Indicator
        AnimatedVisibility(
            visible = !editorState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(2.dp, Gold18k, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Reproduzir", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Active Badges Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 56.dp, start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shopBadges.filter { editorState.activeBadges.contains(it.id) }.forEach { badge ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badge.accentColor.copy(alpha = 0.8f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = badge.icon, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badge.title,
                            color = badge.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom Captions and TikTok Shop Yellow Basket Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 68.dp, bottom = 16.dp)
        ) {
            // Dynamic Viral Caption Box with Karaoke Word Pulsing
            if (editorState.captionsEnabled && editorState.captionText.isNotBlank()) {
                val captionStyle = editorState.selectedCaptionStyle

                if (captionStyle == CaptionStyle.KARAOKE_WORD) {
                    val words = editorState.captionText.split(" ")
                    val currentWordIndex = (wordIndexAnimation.toInt()) % words.size.coerceAtLeast(1)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .border(1.5.dp, Gold18k, RoundedCornerShape(12.dp))
                            .clickable { onEditCaptionClick() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            words.forEachIndexed { idx, word ->
                                val isHighlighted = idx == currentWordIndex
                                Text(
                                    text = "$word ",
                                    color = if (isHighlighted) Color.Black else Color.White,
                                    fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (isHighlighted) 15.sp else 13.sp,
                                    modifier = if (isHighlighted) {
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Gold18k)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    } else Modifier
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .scale(if (editorState.isPlaying) captionBounce else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(captionStyle.bgColor)
                            .border(1.5.dp, captionStyle.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .clickable { onEditCaptionClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = editorState.captionText,
                            color = captionStyle.primaryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // TikTok Profile Handle & Description
            Text(
                text = "@sua.semijoias.oficial",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "✨ Banho 10 milésimos de Ouro 18k. Peças que não descascam! #semijoias #tiktokshopbrasil",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // TikTok Shop Yellow Shopping Basket Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFE600),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛍️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Comprar com Desconto TikTok Shop",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Cupom de Frete Grátis na sacolinha",
                                color = Color(0xFF4A4300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = "Ir para o produto", tint = Color.Black, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
