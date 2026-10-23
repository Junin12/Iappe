package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoTimelineTrim
import com.example.ui.theme.*

@Composable
fun TimelineTrimmer(
    trim: VideoTimelineTrim,
    onTrimChange: (start: Float, end: Float) -> Unit,
    onResetTrim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Timeline & Zona de Ouro (0-3s)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Corte silêncios para o gancho entrar imediatamente",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onResetTrim,
                    modifier = Modifier.size(32.dp).testTag("reset_trim_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Resetar", tint = Gold18k, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Timeline Bar with Golden 3s Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceHighlight)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            ) {
                // Golden 3s Zone Indicator (0 to 3s fraction of total)
                val goldenFraction = (3f / trim.totalDurationSeconds).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = goldenFraction)
                        .background(Gold18k.copy(alpha = 0.25f))
                        .border(1.dp, Gold18k.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⭐ ZONA DE OURO (0-3s)",
                        color = Gold18k,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Active Clip Window
                val startFraction = (trim.startSeconds / trim.totalDurationSeconds).coerceIn(0f, 1f)
                val endFraction = (trim.endSeconds / trim.totalDurationSeconds).coerceIn(startFraction, 1f)

                Row(modifier = Modifier.fillMaxSize()) {
                    if (startFraction > 0f) {
                        Spacer(modifier = Modifier.fillMaxWidth(startFraction))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (endFraction - startFraction) / (1f - startFraction).coerceAtLeast(0.01f))
                            .border(2.dp, TikTokPink, RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider Controls for Trim In and Trim Out
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Início: ${String.format("%.1fs", trim.startSeconds)}",
                    color = if (trim.isHookInGoldenZone) SuccessGreen else WarningAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Fim: ${String.format("%.1fs", trim.endSeconds)} (Total: ${String.format("%.1fs", trim.currentDuration)})",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Slider(
                value = trim.startSeconds,
                onValueChange = { newStart ->
                    if (newStart < trim.endSeconds - 1f) {
                        onTrimChange(newStart, trim.endSeconds)
                    }
                },
                valueRange = 0f..trim.totalDurationSeconds,
                colors = SliderDefaults.colors(
                    thumbColor = TikTokPink,
                    activeTrackColor = TikTokPink,
                    inactiveTrackColor = ObsidianBorder
                ),
                modifier = Modifier.testTag("trim_start_slider")
            )

            // Diagnostic feedback message
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (trim.isHookInGoldenZone) SuccessGreen.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (trim.isHookInGoldenZone) SuccessGreen.copy(alpha = 0.35f) else WarningAmber.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (trim.isHookInGoldenZone) "⚡" else "⚠️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (trim.isHookInGoldenZone)
                            "Gancho imediato ativado! O vídeo prende o espectador sem silêncio inicial."
                        else
                            "Atenção: Gancho após 1.5s pode aumentar a evasão no TikTok. Puxe o início para a esquerda.",
                        color = if (trim.isHookInGoldenZone) SuccessGreen else WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
