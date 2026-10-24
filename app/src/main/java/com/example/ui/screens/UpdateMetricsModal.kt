package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdateMetricsModal(
    video: VideoEntity,
    pecaNome: String = "Peça",
    onDismiss: () -> Unit,
    onSave: (VideoEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")) }
    var viewsText by remember { mutableStateOf(if (video.views > 0) video.views.toString() else "") }
    var curtidasText by remember { mutableStateOf(if (video.curtidas > 0) video.curtidas.toString() else "") }
    var comentariosText by remember { mutableStateOf(if (video.comentarios > 0) video.comentarios.toString() else "") }
    var compartilhamentosText by remember { mutableStateOf(if (video.compartilhamentos > 0) video.compartilhamentos.toString() else "") }
    var salvamentosText by remember { mutableStateOf(if (video.salvamentos > 0) video.salvamentos.toString() else "") }
    var vendasText by remember { mutableStateOf(if (video.vendas > 0) video.vendas.toString() else "") }
    var dataPostagemText by remember {
        mutableStateOf(
            if (video.dataPostagem != null && video.dataPostagem > 0) {
                dateFormat.format(Date(video.dataPostagem))
            } else {
                dateFormat.format(Date())
            }
        )
    }

    UpdateMetricsModal(
        pecaNome = pecaNome,
        videoGancho = video.gancho.ifBlank { "Vídeo #${video.id}" },
        views = viewsText,
        curtidas = curtidasText,
        comentarios = comentariosText,
        compartilhamentos = compartilhamentosText,
        salvamentos = salvamentosText,
        vendas = vendasText,
        dataPostagem = dataPostagemText,
        onViewsChange = { viewsText = it },
        onCurtidasChange = { curtidasText = it },
        onComentariosChange = { comentariosText = it },
        onCompartilhamentosChange = { compartilhamentosText = it },
        onSalvamentosChange = { salvamentosText = it },
        onVendasChange = { vendasText = it },
        onDataPostagemChange = { dataPostagemText = it },
        onDismiss = onDismiss,
        onSave = {
            val parsedDateMs = try {
                dateFormat.parse(dataPostagemText)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val updatedVideo = video.copy(
                views = viewsText.toLongOrNull() ?: 0L,
                curtidas = curtidasText.toLongOrNull() ?: 0L,
                comentarios = comentariosText.toLongOrNull() ?: 0L,
                compartilhamentos = compartilhamentosText.toLongOrNull() ?: 0L,
                salvamentos = salvamentosText.toLongOrNull() ?: 0L,
                vendas = vendasText.toLongOrNull() ?: 0L,
                dataPostagem = parsedDateMs,
                status = StatusVideoOptions.POSTADO
            )
            onSave(updatedVideo)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateMetricsModal(
    pecaNome: String,
    videoGancho: String,
    views: String,
    curtidas: String,
    comentarios: String,
    compartilhamentos: String,
    salvamentos: String,
    vendas: String,
    dataPostagem: String,
    onViewsChange: (String) -> Unit,
    onCurtidasChange: (String) -> Unit,
    onComentariosChange: (String) -> Unit,
    onCompartilhamentosChange: (String) -> Unit,
    onSalvamentosChange: (String) -> Unit,
    onVendasChange: (String) -> Unit,
    onDataPostagemChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        contentColor = TextPrimary,
        tonalElevation = 12.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Gold18k.copy(alpha = 0.5f))
        },
        modifier = Modifier.testTag("update_metrics_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Registrar Métricas Reais",
                        color = Gold18k,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alimenta o Dashboard com dados honestos do TikTok",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_update_metrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = TextMuted
                    )
                }
            }

            // Piece and Video identifier card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = pecaNome,
                        color = Gold18k,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"$videoGancho\"",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }

            HorizontalDivider(color = ObsidianBorder)

            Text(
                text = "Preencha os números reais obtidos no TikTok:",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Views & Vendas (Campos grandes destacados)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = views,
                    onValueChange = onViewsChange,
                    label = { Text("Visualizações (Views) *") },
                    placeholder = { Text("Ex: 12500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_views")
                )

                OutlinedTextField(
                    value = vendas,
                    onValueChange = onVendasChange,
                    label = { Text("Peças Vendidas *") },
                    placeholder = { Text("Ex: 8") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_vendas")
                )
            }

            // Curtidas & Comentários
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = curtidas,
                    onValueChange = onCurtidasChange,
                    label = { Text("Curtidas (Likes)") },
                    placeholder = { Text("Ex: 940") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_curtidas")
                )

                OutlinedTextField(
                    value = comentarios,
                    onValueChange = onComentariosChange,
                    label = { Text("Comentários") },
                    placeholder = { Text("Ex: 48") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_comentarios")
                )
            }

            // Compartilhamentos & Salvamentos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = compartilhamentos,
                    onValueChange = onCompartilhamentosChange,
                    label = { Text("Compartilhamentos") },
                    placeholder = { Text("Ex: 32") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_compartilhamentos")
                )

                OutlinedTextField(
                    value = salvamentos,
                    onValueChange = onSalvamentosChange,
                    label = { Text("Salvos") },
                    placeholder = { Text("Ex: 115") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = modalTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_metric_salvamentos")
                )
            }

            // Data/Hora da Postagem
            OutlinedTextField(
                value = dataPostagem,
                onValueChange = onDataPostagemChange,
                label = { Text("Data e Hora da Postagem (dd/MM/yyyy HH:mm)") },
                placeholder = { Text("Ex: 23/10/2026 19:30") },
                singleLine = true,
                colors = modalTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_metric_data_postagem")
            )

            // Fórmula de Engajamento explicada
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceHighlight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📊 A taxa de engajamento é calculada honestamente como: (curtidas + comentários + compartilhamentos + salvos) ÷ views.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Botão Salvar
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_metrics_confirm_button")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salvar Métricas & Marcar como POSTADO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun modalTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TikTokPink,
        unfocusedBorderColor = ObsidianBorder,
        focusedLabelColor = TikTokPink,
        unfocusedLabelColor = TextMuted,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = TikTokPink,
        focusedContainerColor = ObsidianSurface,
        unfocusedContainerColor = ObsidianSurface
    )
}
