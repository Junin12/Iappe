package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TeleprompterConfig
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TeleprompterModal(
    config: TeleprompterConfig,
    onClose: () -> Unit,
    onToggleScroll: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onApplyAsCaption: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Smooth autoscroll loop when isScrolling is true
    LaunchedEffect(config.isScrolling, config.scrollSpeedWpm) {
        if (config.isScrolling) {
            val stepDelayMs = 40L
            val pixelsPerStep = (config.scrollSpeedWpm / 60f) * 1.8f
            while (config.isScrolling && scrollState.value < scrollState.maxValue) {
                delay(stepDelayMs)
                scrollState.dispatchRawDelta(pixelsPerStep)
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xF00A0612))
        ) {
            // Main Teleprompter Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TikTokPink.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TikTokPink)
                        ) {
                            Text(
                                text = if (config.isScrolling) "● GRAVANDO / ROLANDO" else "PAUSADO",
                                color = if (config.isScrolling) TikTokPink else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onApplyAsCaption,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ObsidianSurfaceElevated)
                                .testTag("apply_teleprompter_caption")
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Usar no Vídeo", tint = Gold18k)
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ObsidianSurfaceElevated)
                                .testTag("close_teleprompter_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                }

                // Scrollable text area (Faceless: hands & jewelry focus, no eye gaze constraint)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = config.scriptText.ifBlank {
                                "✨ Roteiro não selecionado. Escolha um roteiro ou gancho na aba Roteiro IA ou Templates para carregar aqui!"
                            },
                            color = TextPrimary,
                            fontSize = config.fontSizeSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            lineHeight = (config.fontSizeSp * 1.45f).sp
                        )
                    }
                }

                // Bottom Control Panel
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed control
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Velocidade: ${config.scrollSpeedWpm} WPM",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = config.scrollSpeedWpm.toFloat(),
                                    onValueChange = { onSpeedChange(it.toInt()) },
                                    valueRange = 80f..240f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Gold18k,
                                        activeTrackColor = Gold18k,
                                        inactiveTrackColor = ObsidianBorder
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Font size control
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tamanho Fonte: ${config.fontSizeSp}sp",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Slider(
                                    value = config.fontSizeSp.toFloat(),
                                    onValueChange = { onFontSizeChange(it.toInt()) },
                                    valueRange = 16f..34f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = TikTokPink,
                                        activeTrackColor = TikTokPink,
                                        inactiveTrackColor = ObsidianBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Big Play / Pause Button
                        Button(
                            onClick = onToggleScroll,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.isScrolling) WarningAmber else TikTokPink
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("teleprompter_toggle_scroll")
                        ) {
                            Icon(
                                imageVector = if (config.isScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (config.isScrolling) "Pausar Rolagem" else "Iniciar Rolagem do Roteiro",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight floating teleprompter overlay positioned above the jewelry framing area
 * during camera recording, allowing the creator to read the narration effortlessly.
 */
@Composable
fun TeleprompterOverlay(
    scriptText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isScrolling by remember { mutableStateOf(false) }
    var scrollSpeedWpm by remember { mutableIntStateOf(140) }
    var fontSizeSp by remember { mutableIntStateOf(18) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isScrolling, scrollSpeedWpm) {
        if (isScrolling) {
            val stepDelayMs = 40L
            val pixelsPerStep = (scrollSpeedWpm / 60f) * 1.6f
            while (isScrolling && scrollState.value < scrollState.maxValue) {
                delay(stepDelayMs)
                scrollState.dispatchRawDelta(pixelsPerStep)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 220.dp)
            .padding(horizontal = 12.dp)
            .testTag("teleprompter_camera_overlay"),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianBackground.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎙️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Teleprompter de Narração",
                        color = Gold18k,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed adjustments
                    IconButton(
                        onClick = { scrollSpeedWpm = (scrollSpeedWpm - 20).coerceAtLeast(80) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(text = "-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "${scrollSpeedWpm}wpm",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    IconButton(
                        onClick = { scrollSpeedWpm = (scrollSpeedWpm + 20).coerceAtMost(260) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Play / Pause
                    IconButton(
                        onClick = { isScrolling = !isScrolling },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isScrolling) WarningAmber else TikTokPink)
                            .testTag("teleprompter_overlay_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Rolar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Close overlay
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceElevated)
                            .testTag("teleprompter_overlay_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable Text Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = scriptText.ifBlank {
                            "✨ Cole ou selecione um roteiro na aba 'Roteiro IA & Hooks' para acompanhar aqui durante a gravação da semijoia!"
                        },
                        color = TextPrimary,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = (fontSizeSp * 1.35f).sp
                    )
                }
            }
        }
    }
}

