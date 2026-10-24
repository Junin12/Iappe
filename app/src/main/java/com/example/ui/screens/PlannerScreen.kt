package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StatusVideoOptions
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel
import com.example.ui.viewmodel.VideoWithPeca
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateToRecord: ((Long, String) -> Unit)? = null,
    onEditVideo: ((com.example.data.local.VideoEntity, com.example.data.local.PecaEntity?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    // Request POST_NOTIFICATIONS permission for Android 13+ only when scheduling
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled
    }

    // Default to today if available
    LaunchedEffect(uiState.currentWeekDays) {
        val todayIdx = uiState.currentWeekDays.indexOfFirst { it.isToday }
        if (todayIdx != -1) {
            selectedDayIndex = todayIdx
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Planejador Semanal",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Agendamento e acompanhamento de status de cada vídeo",
                            color = Gold18k,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Week navigation buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.previousWeek() },
                            modifier = Modifier.size(36.dp).testTag("planner_prev_week_button")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Semana anterior", tint = TextPrimary)
                        }

                        if (uiState.weekOffset != 0) {
                            TextButton(onClick = { viewModel.resetToCurrentWeek() }) {
                                Text(text = "Hoje", color = TikTokPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.nextWeek() },
                            modifier = Modifier.size(36.dp).testTag("planner_next_week_button")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Próxima semana", tint = TextPrimary)
                        }
                    }
                }
            }
        }

        // Week Days Selector (7 Days)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    uiState.currentWeekDays.forEachIndexed { index, dayItem ->
                        val isSelected = selectedDayIndex == index
                        Surface(
                            onClick = { selectedDayIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) TikTokPink else if (dayItem.isToday) ObsidianSurfaceHighlight else Color.Transparent,
                            border = if (dayItem.isToday && !isSelected) BorderStroke(1.dp, Gold18k) else null,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .testTag("day_tab_$index")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayItem.dayOfWeekName,
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayItem.dayOfMonth.toString(),
                                    color = if (isSelected) Color.White else if (dayItem.isToday) Gold18k else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Video count dot
                                if (dayItem.videos.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isSelected) Color.White else Gold18k, CircleShape)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Videos Section
        val activeDay = uiState.currentWeekDays.getOrNull(selectedDayIndex)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vídeos de ${activeDay?.dayOfWeekName ?: "Hoje"}, dia ${activeDay?.dayOfMonth ?: ""}:",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${activeDay?.videos?.size ?: 0} vídeo(s)",
                    color = Gold18k,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (activeDay == null || activeDay.videos.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📅", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum vídeo programado para este dia",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escolha um vídeo da lista abaixo para agendar a postagem com lembrete no celular.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeDay.videos) { vwp ->
                PlannerVideoCard(
                    vwp = vwp,
                    timeFormat = timeFormat,
                    onAdvanceStatus = { viewModel.advanceStatus(vwp.video) },
                    onSchedule = { viewModel.openScheduleModal(vwp) },
                    onMarkAsPosted = { viewModel.openMetricsRegistration(vwp) },
                    onRecord = {
                        onNavigateToRecord?.invoke(vwp.video.pecaId, vwp.peca?.nome ?: "Semijoia")
                    },
                    onEdit = {
                        onEditVideo?.invoke(vwp.video, vwp.peca)
                    }
                )
            }
        }

        // Section: Backlog / Vídeos a Agendar
        if (uiState.unscheduledVideos.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vídeos Não Agendados (${uiState.unscheduledVideos.size})",
                        color = Gold18k,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(uiState.unscheduledVideos) { vwp ->
                PlannerVideoCard(
                    vwp = vwp,
                    timeFormat = timeFormat,
                    onAdvanceStatus = { viewModel.advanceStatus(vwp.video) },
                    onSchedule = { viewModel.openScheduleModal(vwp) },
                    onMarkAsPosted = { viewModel.openMetricsRegistration(vwp) },
                    onRecord = {
                        onNavigateToRecord?.invoke(vwp.video.pecaId, vwp.peca?.nome ?: "Semijoia")
                    },
                    onEdit = {
                        onEditVideo?.invoke(vwp.video, vwp.peca)
                    }
                )
            }
        }
    }

    // Modal de Agendamento de Data e Hora
    if (uiState.schedulingVideo != null) {
        val vwp = uiState.schedulingVideo!!
        ScheduleDatePickerModal(
            pecaNome = vwp.peca?.nome ?: "Semijoia #${vwp.video.pecaId}",
            hour = uiState.selectedScheduleHour,
            minute = uiState.selectedScheduleMinute,
            onHourChange = { viewModel.updateScheduleHour(it) },
            onMinuteChange = { viewModel.updateScheduleMinute(it) },
            weekDays = uiState.currentWeekDays,
            selectedDayMs = uiState.selectedScheduleDayMs,
            onSelectDayMs = { viewModel.updateScheduleDay(it) },
            onDismiss = { viewModel.dismissScheduleModal() },
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.confirmSchedule()
            }
        )
    }

    // Modal de Registro de Métricas ao Marcar como Postado
    if (uiState.registeringMetricsVideo != null) {
        val vwp = uiState.registeringMetricsVideo!!
        UpdateMetricsModal(
            pecaNome = vwp.peca?.nome ?: "Semijoia #${vwp.video.pecaId}",
            videoGancho = vwp.video.gancho,
            views = uiState.viewsInput,
            curtidas = uiState.curtidasInput,
            comentarios = uiState.comentariosInput,
            compartilhamentos = uiState.compartilhamentosInput,
            salvamentos = uiState.salvamentosInput,
            vendas = uiState.vendasInput,
            dataPostagem = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            onViewsChange = { viewModel.updateMetricsViews(it) },
            onCurtidasChange = { viewModel.updateMetricsCurtidas(it) },
            onComentariosChange = { viewModel.updateMetricsComentarios(it) },
            onCompartilhamentosChange = { viewModel.updateMetricsCompartilhamentos(it) },
            onSalvamentosChange = { viewModel.updateMetricsSalvamentos(it) },
            onVendasChange = { viewModel.updateMetricsVendas(it) },
            onDataPostagemChange = {},
            onDismiss = { viewModel.dismissMetricsRegistration() },
            onSave = { viewModel.saveMetricsFromPlanner() }
        )
    }
}

@Composable
private fun PlannerVideoCard(
    vwp: VideoWithPeca,
    timeFormat: SimpleDateFormat,
    onAdvanceStatus: () -> Unit,
    onSchedule: () -> Unit,
    onMarkAsPosted: () -> Unit,
    onRecord: () -> Unit,
    onEdit: () -> Unit
) {
    val statusColor = when (vwp.video.status) {
        StatusVideoOptions.IDEIA -> Color(0xFF3B82F6) // Blue
        StatusVideoOptions.ROTEIRIZADO -> Color(0xFFA855F7) // Purple
        StatusVideoOptions.GRAVADO -> Color(0xFF10B981) // Emerald
        StatusVideoOptions.EDITADO -> Gold18k // Gold
        StatusVideoOptions.POSTADO -> TikTokPink // TikTok Pink
        else -> TextMuted
    }

    val scheduledTimeStr = vwp.video.dataAgendada?.let { timeFormat.format(Date(it)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().testTag("planner_video_card_${vwp.video.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1: Peça & Status Pill com 1-tap change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vwp.peca?.nome ?: "Semijoia #${vwp.video.pecaId}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${vwp.peca?.tipo ?: "Peça"} • ${vwp.video.duracaoSeg}s",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Status Chip com 1-tap para avançar
                Surface(
                    onClick = onAdvanceStatus,
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, statusColor),
                    modifier = Modifier.testTag("status_pill_${vwp.video.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vwp.video.status,
                            color = statusColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Mudar status",
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Gancho
            if (vwp.video.gancho.isNotBlank()) {
                Text(
                    text = "\"${vwp.video.gancho}\"",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }

            // Horário Agendado
            if (scheduledTimeStr != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = Gold18k, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Agendado para às $scheduledTimeStr (Lembrete ativo)",
                        color = Gold18k,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Se o vídeo estiver gravado ou editado, botão para abrir no Estúdio Media3
                if (vwp.video.status == StatusVideoOptions.GRAVADO || vwp.video.status == StatusVideoOptions.EDITADO) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.6f)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Gold18k, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Editar", color = Gold18k, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Botão Agendar Horário
                OutlinedButton(
                    onClick = onSchedule,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (scheduledTimeStr == null) "Agendar" else "Reagendar", color = TextPrimary, fontSize = 11.sp)
                }

                // Botão Marcar como Postado / Gravar
                if (vwp.video.status != StatusVideoOptions.POSTADO) {
                    Button(
                        onClick = onMarkAsPosted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                        modifier = Modifier.weight(1.3f).height(38.dp).testTag("mark_as_posted_button_${vwp.video.id}")
                    ) {
                        Text(text = "Marcar como Postado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onMarkAsPosted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                        border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1.3f).height(38.dp)
                    ) {
                        Text(text = "Editar Métricas", color = TikTokPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDatePickerModal(
    pecaNome: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    weekDays: List<com.example.ui.viewmodel.WeekDayItem>,
    selectedDayMs: Long,
    onSelectDayMs: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        contentColor = TextPrimary,
        tonalElevation = 12.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Gold18k.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Agendar Postagem & Lembrete",
                color = Gold18k,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Defina o dia e horário para receber notificação local no celular para postar o vídeo de \"$pecaNome\".",
                color = TextMuted,
                fontSize = 12.sp
            )

            // Seletor de dia da semana
            Text(text = "1. Selecione o Dia:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(weekDays) { day ->
                    val isSel = selectedDayMs == day.dateMidnightMs
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelectDayMs(day.dateMidnightMs) },
                        label = { Text("${day.dayOfWeekName} ${day.dayOfMonth}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TikTokPink,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Seletor de Horário
            Text(text = "2. Horário da Postagem: %02d:%02d".format(hour, minute), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sliders de Hora e Minuto
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Hora: $hour h", color = TextMuted, fontSize = 11.sp)
                    Slider(
                        value = hour.toFloat(),
                        onValueChange = { onHourChange(it.toInt()) },
                        valueRange = 6f..23f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = Gold18k, activeTrackColor = Gold18k)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Minutos: $minute m", color = TextMuted, fontSize = 11.sp)
                    Slider(
                        value = minute.toFloat(),
                        onValueChange = { onMinuteChange(it.toInt()) },
                        valueRange = 0f..55f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = TikTokPink, activeTrackColor = TikTokPink)
                    )
                }
            }

            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold18k),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Confirmar Agendamento", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
