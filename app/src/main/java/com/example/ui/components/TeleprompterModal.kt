package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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

                // Eye-level Reading Marker line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Gold18k.copy(alpha = 0.4f)))
                    Text(
                        text = " 👀 LINHA DE LEITURA (OLHE PARA A LENTE) ",
                        color = Gold18k,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Gold18k.copy(alpha = 0.4f)))
                }

                // Scrollable text area
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
