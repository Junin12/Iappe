package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PecasUiState
import com.example.ui.viewmodel.PecasViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PecaDetailScreen(
    peca: PecaEntity,
    uiState: PecasUiState,
    viewModel: PecasViewModel,
    onBack: () -> Unit,
    onGenerateScript: (PecaEntity) -> Unit,
    onRecordVideoForPeca: (PecaEntity) -> Unit,
    onEditVideo: ((VideoEntity, PecaEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }

    var selectedPhotoIndex by remember { mutableStateOf(0) }
    var videoForMetricsUpdate by remember { mutableStateOf<VideoEntity?>(null) }

    // Intercept back key to return to grid
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = peca.nome,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Gold18k,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_detail_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openEditPeca(peca) },
                        modifier = Modifier.testTag("edit_peca_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = Gold18k)
                    }
                    IconButton(
                        onClick = { viewModel.requestDeletePeca(peca) },
                        modifier = Modifier.testTag("delete_peca_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = TikTokPink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianSurface,
                    titleContentColor = Gold18k
                ),
                modifier = Modifier.border(width = 1.dp, color = ObsidianBorder)
            )
        },
        containerColor = ObsidianBackground
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Photo Gallery
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val currentPhoto = peca.fotos.getOrNull(selectedPhotoIndex) ?: peca.fotos.firstOrNull()
                        if (currentPhoto != null) {
                            AsyncImage(
                                model = currentPhoto,
                                contentDescription = peca.nome,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(ObsidianSurface, ObsidianSurfaceHighlight)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = Gold18k,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = peca.tipo.uppercase(),
                                        color = Gold18k,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }

                        // Gradient overlay at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )

                        // SKU Badge
                        if (peca.codigoSku.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianBackground.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "SKU: ${peca.codigoSku}",
                                    color = Gold18k,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Stock Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (peca.estoque > 0) SuccessGreen.copy(alpha = 0.2f) else TikTokPink.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (peca.estoque > 0) SuccessGreen else TikTokPink),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (peca.estoque > 0) "${peca.estoque} em estoque" else "Esgotado",
                                color = if (peca.estoque > 0) SuccessGreen else TikTokPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Thumbnails strip if multiple photos
                if (peca.fotos.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(peca.fotos) { index, photo ->
                            val isSelected = selectedPhotoIndex == index
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TikTokPink else ObsidianBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedPhotoIndex = index }
                            ) {
                                AsyncImage(
                                    model = photo,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Price and Key Specs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Prices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (peca.precoPromocional != null) {
                                    Text(
                                        text = "De: ${currencyFormat.format(peca.preco)}",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                    )
                                    Text(
                                        text = currencyFormat.format(peca.precoPromocional),
                                        color = Gold18k,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = currencyFormat.format(peca.preco),
                                        color = Gold18k,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Banho chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ObsidianSurfaceHighlight,
                                border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "✨", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${peca.banho} (${peca.milesimos}M)",
                                        color = Gold18k,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = ObsidianBorder)

                        // Specification Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecItem(label = "Tipo", value = peca.tipo)
                            SpecItem(label = "Pedra", value = peca.pedra)
                            SpecItem(label = "Cadastrado em", value = dateFormat.format(Date(peca.criadoEm)))
                        }

                        // Link TikTok Shop if present
                        if (!peca.linkTikTokShop.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianSurfaceHighlight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(peca.linkTikTokShop))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🛍️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Sacolinha TikTok Shop", color = TikTokPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = peca.linkTikTokShop,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Abrir Link", tint = TikTokPink, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Observações de Gravação
                        if (peca.observacoes.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ObsidianBackground, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.TipsAndUpdates, contentDescription = null, tint = Gold18k, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Dicas de Gravação Macro", color = Gold18k, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = peca.observacoes,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Primary Creation CTA Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Gerar Roteiro
                    Button(
                        onClick = { onGenerateScript(peca) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_script_for_peca_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokPink)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gerar Roteiro para esta Peça",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Button 2: Gravar Vídeo
                    Button(
                        onClick = { onRecordVideoForPeca(peca) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("record_video_for_peca_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold18k)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gravar Vídeo desta Peça",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Section: Vídeos Desta Peça
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vídeos Desta Peça (${uiState.videosForSelectedPeca.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.videosForSelectedPeca.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🎬", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum vídeo gravado ainda",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque em 'Gravar Vídeo desta Peça' para filmar o brilho e vincular automaticamente.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.videosForSelectedPeca) { video ->
                    VideoCardItem(
                        video = video,
                        isGenerating = uiState.generatingCaptionForVideoId == video.id,
                        onGenerateCaption = { v ->
                            viewModel.generateCaptionsForVideo(v.id, peca, v.gancho)
                        },
                        onUpdateMetrics = { v ->
                            videoForMetricsUpdate = v
                        },
                        onEditVideo = { v ->
                            onEditVideo?.invoke(v, peca)
                        }
                    )
                }
            }
        }

        // Metrics Modal Sheet
        videoForMetricsUpdate?.let { targetVideo ->
            UpdateMetricsModal(
                video = targetVideo,
                onDismiss = { videoForMetricsUpdate = null },
                onSave = { updatedVideo ->
                    viewModel.updateVideoMetrics(updatedVideo)
                    videoForMetricsUpdate = null
                }
            )
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VideoCardItem(
    video: VideoEntity,
    isGenerating: Boolean = false,
    onGenerateCaption: (VideoEntity) -> Unit = {},
    onUpdateMetrics: (VideoEntity) -> Unit = {},
    onEditVideo: (VideoEntity) -> Unit = {}
) {
    val statusColor = when (video.status) {
        StatusVideoOptions.IDEIA -> Color(0xFF3B82F6) // Blue
        StatusVideoOptions.ROTEIRIZADO -> Color(0xFFA855F7) // Purple
        StatusVideoOptions.GRAVADO -> Color(0xFF10B981) // Emerald / Teal
        StatusVideoOptions.EDITADO -> Gold18k // Gold
        StatusVideoOptions.POSTADO -> TikTokPink // TikTok Pink
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = video.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (video.duracaoSeg > 0) {
                    Text(
                        text = "${video.duracaoSeg}s",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (video.gancho.isNotBlank()) {
                Text(
                    text = "\"${video.gancho}\"",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (video.legenda.isNotBlank()) {
                Text(
                    text = video.legenda,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (video.hashtags.isNotBlank()) {
                Text(
                    text = video.hashtags,
                    color = Gold18k,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Standalone AI Caption Generator Button
            OutlinedButton(
                onClick = { onGenerateCaption(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("generate_caption_button_${video.id}"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.5f)),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = TikTokPink,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerando Legenda IA...", color = TikTokPink, fontSize = 11.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TikTokPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (video.legenda.isBlank()) "Gerar Legenda & Tags com IA" else "Regerar Legenda & Tags IA",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Metrics Summary and "Registrar resultado" Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricChip("Views", video.views)
                MetricChip("Likes", video.curtidas)
                MetricChip("Vendas", video.vendas)
            }

            // Botão Editar Vídeo (Media3)
            Button(
                onClick = { onEditVideo(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("edit_video_media3_button_${video.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHighlight),
                border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.MovieFilter,
                    contentDescription = null,
                    tint = TikTokCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Editar no Estúdio Media3",
                    color = TikTokCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Shortcut to update metrics
            OutlinedButton(
                onClick = { onUpdateMetrics(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("update_metrics_button_${video.id}"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Gold18k
                )
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Gold18k,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (video.views > 0 || video.status == StatusVideoOptions.POSTADO) "Atualizar Métricas" else "Registrar Resultado",
                    color = Gold18k,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", color = TextMuted, fontSize = 11.sp)
        Text(text = value.toString(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
