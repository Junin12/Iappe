package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class RetentionPoint(val second: Int, val percentWithHook: Float, val percentWithoutHook: Float, val annotation: String? = null)
data class CompetitorBarData(val handle: String, val value: Float, val displayValue: String, val isUser: Boolean = false)
data class CategoryDonutData(val name: String, val percentage: Float, val color: Color, val amountText: String)

@Composable
fun RetentionAreaChart(
    data: List<RetentionPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Curva de Retenção de Vídeo (Recharts Area)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Comparando Gancho Otimizado (0-3s) vs Vídeo Comum",
                        color = Gold18k,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TikTokPink))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Com Gancho Ouro (82% ret.)", color = TextSecondary, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF6B7280)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Sem Gancho (31% ret.)", color = TextMuted, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val pointWidth = size.width / (data.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x + pointWidth / 2) / pointWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = idx
                        }
                    }
                    .testTag("retention_area_chart")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 20.dp.toPx()
                    val chartHeight = h - paddingBottom
                    val stepX = w / (data.size - 1)

                    // Background Grid Lines
                    for (i in 0..4) {
                        val y = chartHeight * (i / 4f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Build Path for "With Hook" (TikTok Pink & Gold gradient)
                    val pathWithHook = Path()
                    val fillPathWithHook = Path()

                    val pathWithoutHook = Path()

                    data.forEachIndexed { i, p ->
                        val x = i * stepX
                        val yWith = chartHeight - (chartHeight * (p.percentWithHook / 100f) * animProgress.value)
                        val yWithout = chartHeight - (chartHeight * (p.percentWithoutHook / 100f) * animProgress.value)

                        if (i == 0) {
                            pathWithHook.moveTo(x, yWith)
                            fillPathWithHook.moveTo(x, chartHeight)
                            fillPathWithHook.lineTo(x, yWith)

                            pathWithoutHook.moveTo(x, yWithout)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevYWith = chartHeight - (chartHeight * (data[i - 1].percentWithHook / 100f) * animProgress.value)
                            val cx = (prevX + x) / 2
                            pathWithHook.cubicTo(cx, prevYWith, cx, yWith, x, yWith)
                            fillPathWithHook.cubicTo(cx, prevYWith, cx, yWith, x, yWith)

                            val prevYWithout = chartHeight - (chartHeight * (data[i - 1].percentWithoutHook / 100f) * animProgress.value)
                            pathWithoutHook.cubicTo(cx, prevYWithout, cx, yWithout, x, yWithout)
                        }

                        if (i == data.size - 1) {
                            fillPathWithHook.lineTo(x, chartHeight)
                            fillPathWithHook.close()
                        }
                    }

                    // Draw Gradient Fill under "With Hook"
                    drawPath(
                        path = fillPathWithHook,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TikTokPink.copy(alpha = 0.35f),
                                TikTokPink.copy(alpha = 0.03f)
                            )
                        )
                    )

                    // Draw Stroke Lines
                    drawPath(
                        path = pathWithoutHook,
                        color = Color(0xFF6B7280).copy(alpha = 0.8f),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )

                    drawPath(
                        path = pathWithHook,
                        brush = Brush.horizontalGradient(listOf(TikTokPink, Gold18k)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Golden 3s Zone Marker Line
                    val goldenLineX = 1 * stepX // at 3s
                    drawLine(
                        color = Gold18k.copy(alpha = 0.5f),
                        start = Offset(goldenLineX, 0f),
                        end = Offset(goldenLineX, chartHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // Draw points on "With Hook"
                    data.forEachIndexed { i, p ->
                        val x = i * stepX
                        val y = chartHeight - (chartHeight * (p.percentWithHook / 100f) * animProgress.value)
                        drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = TikTokPink, radius = 2.dp.toPx(), center = Offset(x, y))
                    }

                    // Highlight selected point if any
                    selectedIndex?.let { idx ->
                        val p = data[idx]
                        val x = idx * stepX
                        val y = chartHeight - (chartHeight * (p.percentWithHook / 100f) * animProgress.value)
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(x, 0f),
                            end = Offset(x, chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(color = Gold18k, radius = 6.dp.toPx(), center = Offset(x, y))
                    }
                }

                // X-Axis Seconds labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { p ->
                        Text(
                            text = "${p.second}s",
                            color = if (p.second <= 3) Gold18k else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (p.second <= 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Interactive Tooltip Box
            val activePoint = selectedIndex?.let { data.getOrNull(it) } ?: data[1]
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceHighlight,
                border = androidx.compose.foundation.BorderStroke(1.dp, TikTokPink.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⏱️ Tempo: ${activePoint.second}s - ${activePoint.annotation ?: "Transição de Cena"}",
                            color = Gold18k,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Com Gancho: ${activePoint.percentWithHook.toInt()}% espectadores retidos",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "+${(activePoint.percentWithHook - activePoint.percentWithoutHook).toInt()}% Retenção",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompetitorBenchmarkBarChart(
    title: String,
    metricUnit: String,
    data: List<CompetitorBarData>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 100f
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Benchmark Comparativo TikTok Shop ($metricUnit)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.forEach { item ->
                    val fraction = ((item.value / maxValue) * animProgress.value).coerceIn(0.04f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.handle,
                                color = if (item.isUser) Gold18k else TextSecondary,
                                fontWeight = if (item.isUser) FontWeight.Black else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = item.displayValue,
                                color = if (item.isUser) TikTokPink else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianSurfaceHighlight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (item.isUser) {
                                            Brush.horizontalGradient(listOf(TikTokPink, Gold18k))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8)))
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySalesDonutCard(
    data: List<CategoryDonutData>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mix de Vendas por Categoria (Recharts Pie)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Peças mais lucrativas no TikTok Shop",
                color = Gold18k,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart on the left
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidth = 22.dp.toPx()

                        data.forEach { item ->
                            val sweep = (item.percentage / 100f) * 360f
                            drawArc(
                                color = item.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TOP 1", color = Gold18k, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Chokers", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Breakdown list on the right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.color))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = item.name, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(text = "${item.percentage.toInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
