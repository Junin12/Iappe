@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.PecaEntity
import com.example.data.local.StatusVideoOptions
import com.example.data.local.VideoEntity
import com.example.editor.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PecasViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun StudioScreen(
    editorViewModel: EditorViewModel,
    pecasViewModel: PecasViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by editorViewModel.uiState.collectAsState()
    val allVideos by editorViewModel.allVideos.collectAsState()
    val editSpec = uiState.editSpec
    val pecasState by pecasViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            editorViewModel.clearSnackbar()
        }
    }

    // Photo Picker launcher para importar vídeo da galeria vinculado a uma peça
    var pecaIdForImport by remember { mutableStateOf<Long?>(null) }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val selectedPeca = pecasState.pecas.find { it.id == pecaIdForImport }
                ?: pecasState.pecas.firstOrNull()
            editorViewModel.loadImportedVideo(uri.toString(), selectedPeca)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header com botão voltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao aplicativo",
                            tint = Gold18k
                        )
                    }
                    Column {
                        Text(
                            text = "Estúdio de Edição",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Renderização real H.264 1080x1920",
                            color = Gold18k,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "9:16 HD",
                        color = Gold18k,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Seção de Seleção de Vídeo (se nenhum vídeo estiver selecionado ou se quiser trocar)
            VideoSelectorSection(
                currentVideoUri = editSpec.videoUri,
                currentPecaNome = editSpec.pecaNome,
                recordedVideos = allVideos.filter {
                    it.status == StatusVideoOptions.GRAVADO ||
                            it.status == StatusVideoOptions.ROTEIRIZADO ||
                            it.status == StatusVideoOptions.EDITADO
                },
                pecas = pecasState.pecas,
                onSelectVideo = { video, peca ->
                    editorViewModel.loadVideo(video, peca)
                },
                onImportFromGallery = { peca ->
                    pecaIdForImport = peca?.id
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )

            // Player de Preview Real com Overlay do Gancho
            if (editSpec.videoUri.isNotBlank()) {
                RealVideoPlayerPreview(
                    videoUri = editSpec.videoUri,
                    spec = editSpec,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = uiState.currentPositionMs,
                    onTogglePlay = { editorViewModel.setPlaying(!uiState.isPlaying) },
                    onDurationKnown = { duration -> editorViewModel.setVideoDuration(duration) },
                    onPositionUpdate = { pos -> editorViewModel.setPlaybackPosition(pos) },
                    onSeekTo = { pos -> editorViewModel.setPlaybackPosition(pos) }
                )

                // 1. Corte Início / Fim (Linha do tempo real com Zona de Ouro 0-3s)
                TrimSection(
                    trim = editSpec.trim,
                    onTrimChange = { start, end ->
                        editorViewModel.setTrim(start, end)
                    }
                )

                // 2. Texto do Gancho nos primeiros 3 segundos
                HookOverlaySection(
                    hookSpec = editSpec.hookOverlay,
                    onTextChange = { editorViewModel.setHookText(it) },
                    onStyleChange = { editorViewModel.setHookStyle(it) },
                    onPositionChange = { editorViewModel.setHookPosition(it) }
                )

                // 3. Ajustes de Imagem Reais Media3 (Brilho, Contraste, Saturação e Preset)
                ColorAdjustmentsSection(
                    adjustments = editSpec.colorAdjustments,
                    isPresetActive = editSpec.isJewelryGlowPresetApplied,
                    onTogglePreset = { editorViewModel.toggleJewelryGlowPreset() },
                    onBrightnessChange = { editorViewModel.setBrightness(it) },
                    onContrastChange = { editorViewModel.setContrast(it) },
                    onSaturationChange = { editorViewModel.setSaturation(it) },
                    onReset = { editorViewModel.resetColorAdjustments() }
                )

                // 4. Áudio Original (Manter, Reduzir, Remover)
                AudioModeSection(
                    currentMode = editSpec.audioMode,
                    onModeChange = { editorViewModel.setAudioMode(it) }
                )

                // 5. Gerador de Capa a partir de Frame Real
                CoverGeneratorButton(
                    onOpenModal = { editorViewModel.openCoverModal() }
                )

                // 6. Exportação MP4 Real & Compartilhamento TikTok
                ExportSection(
                    uiState = uiState,
                    onStartExport = { editorViewModel.startExport() },
                    onCancelExport = { editorViewModel.cancelExport() },
                    onShareToTikTok = { editorViewModel.shareToTikTok(context) }
                )
            } else {
                // Estado vazio: nenhum vídeo carregado
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🎬", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum vídeo carregado no estúdio",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Selecione um vídeo gravado acima ou importe um arquivo da galeria vinculado a uma peça para editar e exportar em 1080x1920.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Modal para Gerador de Capa Real
        if (uiState.isCoverModalOpen) {
            RealCoverGeneratorModal(
                uiState = uiState,
                onClose = { editorViewModel.closeCoverModal() },
                onScrubFrame = { timeMs -> editorViewModel.scrubCoverFrame(timeMs) },
                onHeadlineChange = { editorViewModel.setCoverHeadline(it) },
                onPriceTagChange = { editorViewModel.setCoverPriceTag(it) },
                onStyleChange = { editorViewModel.setCoverStyle(it) },
                onSaveCover = { editorViewModel.saveCover() }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

// -------------------------------------------------------------
// Componente de Seleção de Vídeo Gravado / Importação
// -------------------------------------------------------------
@Composable
private fun VideoSelectorSection(
    currentVideoUri: String,
    currentPecaNome: String,
    recordedVideos: List<VideoEntity>,
    pecas: List<PecaEntity>,
    onSelectVideo: (VideoEntity, PecaEntity?) -> Unit,
    onImportFromGallery: (PecaEntity?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vídeo em Edição",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = { onImportFromGallery(pecas.firstOrNull()) },
                    modifier = Modifier.testTag("import_video_photo_picker_button")
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Importar Galeria", color = TikTokPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (currentVideoUri.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurfaceHighlight,
                    border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Peça: $currentPecaNome",
                                color = Gold18k,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vídeo ativo pronto para cortes e efeitos",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (recordedVideos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ou selecione outro vídeo gravado do catálogo:",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordedVideos) { video ->
                        val peca = pecas.find { it.id == video.pecaId }
                        val isCurrent = currentVideoUri == video.uri
                        Surface(
                            onClick = { onSelectVideo(video, peca) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (isCurrent) TikTokPink else ObsidianBorder),
                            modifier = Modifier.testTag("select_recorded_video_${video.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = peca?.nome ?: "Peça #${video.pecaId}",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${video.status} • ${video.duracaoSeg}s",
                                    color = Gold18k,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Player de Preview Real com ExoPlayer + WYSIWYG Hook Overlay
// -------------------------------------------------------------
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun RealVideoPlayerPreview(
    videoUri: String,
    spec: EditSpec,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onTogglePlay: () -> Unit,
    onDurationKnown: (Long) -> Unit,
    onPositionUpdate: (Long) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            setMediaItem(mediaItem)
            prepare()
        }
    }

    // Gerencia o estado de play/pause do player
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Monitoramento contínuo de tempo e loop no intervalo de corte selecionado
    LaunchedEffect(exoPlayer, spec.trim.startMs, spec.trim.endMs) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0L) {
                    onDurationKnown(exoPlayer.duration)
                }
            }
        })

        while (isActive) {
            val currentPos = exoPlayer.currentPosition
            onPositionUpdate(currentPos)

            // Loop dentro do trecho de corte selecionado
            if (spec.trim.endMs > 0L && currentPos >= spec.trim.endMs) {
                exoPlayer.seekTo(spec.trim.startMs)
                if (isPlaying) exoPlayer.play()
            }
            delay(50)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val isHookVisible = remember(currentPositionMs, spec.trim.startMs, spec.hookOverlay.durationMs, spec.hookOverlay.text) {
        spec.hookOverlay.text.isNotBlank() &&
                (currentPositionMs >= spec.trim.startMs) &&
                (currentPositionMs <= spec.trim.startMs + spec.hookOverlay.durationMs)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            .background(Color.Black)
            .clickable { onTogglePlay() },
        contentAlignment = Alignment.Center
    ) {
        // Player Surface Media3
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay WYSIWYG do Gancho nos primeiros 3 segundos
        AnimatedVisibility(
            visible = isHookVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = when (spec.hookOverlay.position) {
                    HookTextPosition.TOP -> Alignment.TopCenter
                    HookTextPosition.CENTER -> Alignment.Center
                    HookTextPosition.BOTTOM -> Alignment.BottomCenter
                }
            ) {
                val topPadding = if (spec.hookOverlay.position == HookTextPosition.TOP) 44.dp else 0.dp
                val bottomPadding = if (spec.hookOverlay.position == HookTextPosition.BOTTOM) 64.dp else 0.dp

                Box(
                    modifier = Modifier
                        .padding(top = topPadding, bottom = bottomPadding)
                        .then(
                            when (spec.hookOverlay.style) {
                                HookTextStyle.TIKTOK_BOX -> Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                HookTextStyle.GOLD_ELEGANT -> Modifier
                                    .background(Color(0xE6141418), RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Gold18k, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                HookTextStyle.WHITE_OUTLINE -> Modifier
                                    .background(Color(0xB3000000), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            }
                        )
                ) {
                    Text(
                        text = spec.hookOverlay.text.uppercase(),
                        color = when (spec.hookOverlay.style) {
                            HookTextStyle.TIKTOK_BOX -> Color.Black
                            HookTextStyle.GOLD_ELEGANT -> Gold18k
                            HookTextStyle.WHITE_OUTLINE -> Color.White
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
            }
        }

        // Zona de Ouro 0-3s Badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (spec.trim.isHookInGoldenZone) Gold18k.copy(alpha = 0.85f) else Color.Red.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = if (spec.trim.isHookInGoldenZone) "⚡ ZONA DE OURO 0–3s ATIVA" else "⚠️ GANCHO FORA DA ZONA 0-3s",
                color = if (spec.trim.isHookInGoldenZone) Color.Black else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Play / Pause Icon Indicador quando pausado
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Corte Início / Fim (Linha do Tempo com Zona de Ouro 0-3s)
// -------------------------------------------------------------
@Composable
private fun TrimSection(
    trim: VideoTrim,
    onTrimChange: (Long, Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✂️ Corte de Início e Fim",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${"%.1f".format(trim.durationTrimmedMs / 1000f)}s selecionados",
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Arraste os marcadores para cortar. O preview toca apenas o trecho em loop.",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Slider Início
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Início do Vídeo:", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${"%.1f".format(trim.startMs / 1000f)}s", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = trim.startMs.toFloat(),
                onValueChange = { newStart ->
                    onTrimChange(newStart.toLong(), trim.endMs)
                },
                valueRange = 0f..(trim.videoDurationMs.coerceAtLeast(1000L).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = Gold18k,
                    activeTrackColor = Gold18k,
                    inactiveTrackColor = ObsidianBorder
                ),
                modifier = Modifier.testTag("trim_start_slider")
            )

            // Slider Fim
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Fim do Vídeo:", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${"%.1f".format(trim.endMs / 1000f)}s", color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = trim.endMs.toFloat(),
                onValueChange = { newEnd ->
                    onTrimChange(trim.startMs, newEnd.toLong())
                },
                valueRange = 0f..(trim.videoDurationMs.coerceAtLeast(1000L).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = TikTokPink,
                    activeTrackColor = TikTokPink,
                    inactiveTrackColor = ObsidianBorder
                ),
                modifier = Modifier.testTag("trim_end_slider")
            )
        }
    }
}

// -------------------------------------------------------------
// 2. Texto do Gancho nos primeiros 3 segundos
// -------------------------------------------------------------
@Composable
private fun HookOverlaySection(
    hookSpec: HookOverlaySpec,
    onTextChange: (String) -> Unit,
    onStyleChange: (HookTextStyle) -> Unit,
    onPositionChange: (HookTextPosition) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "⚡ Texto do Gancho (Queimado nos 3s)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Texto que retém o espectador imediatamente ao rolar o feed do TikTok:",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = hookSpec.text,
                onValueChange = onTextChange,
                label = { Text("Texto do Gancho (máx 6 palavras)") },
                placeholder = { Text("Ex: O SEGREDO DO OURO 18K") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TikTokPink,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_hook_overlay_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Estilos
            Text(text = "Estilo Visual:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HookTextStyle.values().forEach { style ->
                    val isSelected = hookSpec.style == style
                    Surface(
                        onClick = { onStyleChange(style) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) Gold18k else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hook_style_${style.name}")
                    ) {
                        Text(
                            text = style.displayName,
                            color = if (isSelected) Gold18k else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Posição
            Text(text = "Posição na Tela (Segura contra botões do TikTok):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HookTextPosition.values().forEach { pos ->
                    val isSelected = hookSpec.position == pos
                    Surface(
                        onClick = { onPositionChange(pos) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) TikTokCyan else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hook_position_${pos.name}")
                    ) {
                        Text(
                            text = pos.displayName,
                            color = if (isSelected) TikTokCyan else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Ajustes de Imagem Reais Media3 (Sliders & Preset)
// -------------------------------------------------------------
@Composable
private fun ColorAdjustmentsSection(
    adjustments: ColorAdjustments,
    isPresetActive: Boolean,
    onTogglePreset: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ Ajustes de Imagem Reais",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                TextButton(onClick = onReset) {
                    Text(text = "Redefinir", color = TextMuted, fontSize = 11.sp)
                }
            }

            Text(
                text = "Efeitos reais do Media3 aplicados no transcode H.264:",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Preset "Brilho de Joia"
            Button(
                onClick = onTogglePreset,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPresetActive) Gold18k else ObsidianSurfaceElevated
                ),
                border = BorderStroke(1.dp, Gold18k),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jewelry_glow_preset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isPresetActive) Color.Black else Gold18k,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPresetActive) "Preset 'Brilho de Joia' Ativado ✨" else "Ativar Preset 'Brilho de Joia'",
                    color = if (isPresetActive) Color.Black else Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slider Brilho (-0.5 a +0.5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Brilho:", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${(adjustments.brightness * 100).toInt()}%", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = adjustments.brightness,
                onValueChange = onBrightnessChange,
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(thumbColor = Gold18k, activeTrackColor = Gold18k),
                modifier = Modifier.testTag("slider_brightness")
            )

            // Slider Contraste (-0.8 a +0.8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Contraste:", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${(adjustments.contrast * 100).toInt()}%", color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = adjustments.contrast,
                onValueChange = onContrastChange,
                valueRange = -0.8f..0.8f,
                colors = SliderDefaults.colors(thumbColor = TikTokPink, activeTrackColor = TikTokPink),
                modifier = Modifier.testTag("slider_contrast")
            )

            // Slider Saturação (-1.0 a +1.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Saturação (Realce Dourado):", color = TextSecondary, fontSize = 11.sp)
                Text(text = "${(adjustments.saturation * 100).toInt()}%", color = TikTokCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Slider(
                value = adjustments.saturation,
                onValueChange = onSaturationChange,
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(thumbColor = TikTokCyan, activeTrackColor = TikTokCyan),
                modifier = Modifier.testTag("slider_saturation")
            )
        }
    }
}

// -------------------------------------------------------------
// 4. Áudio do Vídeo (Manter, Reduzir, Remover)
// -------------------------------------------------------------
@Composable
private fun AudioModeSection(
    currentMode: AudioMode,
    onModeChange: (AudioMode) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "🎵 Áudio Original",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Escolha o tratamento do som original para facilitar a trilha no TikTok:",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            AudioMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                Surface(
                    onClick = { onModeChange(mode) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) TikTokPink else ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("audio_mode_${mode.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) TikTokPink else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onModeChange(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = TikTokPink)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Botão Gerador de Capa
// -------------------------------------------------------------
@Composable
private fun CoverGeneratorButton(
    onOpenModal: () -> Unit
) {
    OutlinedButton(
        onClick = onOpenModal,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Gold18k),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold18k),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("open_real_cover_generator_button")
    ) {
        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Gold18k, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Escolher Frame & Gerar Capa JPG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// -------------------------------------------------------------
// 6. Exportação MP4 Real & Compartilhamento no TikTok
// -------------------------------------------------------------
@Composable
private fun ExportSection(
    uiState: EditorUiState,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onShareToTikTok: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🚀 Exportação & Publicação no TikTok",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isExporting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.exportProgress },
                        color = TikTokPink,
                        trackColor = ObsidianSurfaceHighlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Renderizando MP4 1080x1920 H.264: ${(uiState.exportProgress * 100).toInt()}%",
                        color = Gold18k,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCancelExport,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Cancelar", color = TextMuted, fontSize = 11.sp)
                    }
                }
            } else if (uiState.exportResultUri != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceHighlight,
                    border = BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vídeo renderizado com sucesso!",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Salvo em Movies/GlowStudio. Status atualizado para EDITADO.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onShareToTikTok,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("share_to_tiktok_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compartilhar no TikTok (Copia Legenda)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                if (uiState.exportErrorMessage != null) {
                    Text(
                        text = "Erro: ${uiState.exportErrorMessage}",
                        color = TikTokPink,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onStartExport,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_video_media3_button")
                ) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exportar MP4 1080x1920 (H.264)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. Modal Real de Criação de Capa com Frame do Vídeo
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealCoverGeneratorModal(
    uiState: EditorUiState,
    onClose: () -> Unit,
    onScrubFrame: (Long) -> Unit,
    onHeadlineChange: (String) -> Unit,
    onPriceTagChange: (String) -> Unit,
    onStyleChange: (HookTextStyle) -> Unit,
    onSaveCover: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = ObsidianSurfaceElevated,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gerador de Capa a partir do Frame",
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextMuted)
                }
            }

            // Preview do Frame Selecionado
            val frameBitmap = uiState.extractedCoverBitmap
            if (frameBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Gold18k.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = frameBitmap.asImageBitmap(),
                        contentDescription = "Frame da Capa",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay do título da capa em tempo real
                    if (uiState.coverHeadline.isNotBlank()) {
                        Text(
                            text = uiState.coverHeadline.uppercase(),
                            color = when (uiState.coverSelectedStyle) {
                                HookTextStyle.TIKTOK_BOX -> Color.White
                                HookTextStyle.GOLD_ELEGANT -> Gold18k
                                HookTextStyle.WHITE_OUTLINE -> Color.White
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(ObsidianSurface, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold18k)
                }
            }

            // Slider para escolher o momento do vídeo
            Text(text = "Escolha o melhor frame do vídeo:", color = TextSecondary, fontSize = 11.sp)
            Slider(
                value = uiState.currentPositionMs.toFloat(),
                onValueChange = { onScrubFrame(it.toLong()) },
                valueRange = 0f..(uiState.videoDurationMs.coerceAtLeast(1000L).toFloat()),
                colors = SliderDefaults.colors(thumbColor = Gold18k, activeTrackColor = Gold18k)
            )

            // Título da Capa
            OutlinedTextField(
                value = uiState.coverHeadline,
                onValueChange = onHeadlineChange,
                label = { Text("Título da Capa") },
                placeholder = { Text("Ex: JOIA QUE NUNCA PRETEJA") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tag de Preço Opcional
            OutlinedTextField(
                value = uiState.coverPriceTag,
                onValueChange = onPriceTagChange,
                label = { Text("Preço em Destaque (Opcional)") },
                placeholder = { Text("Ex: R$ 49,90") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Botão Salvar Capa
            Button(
                onClick = onSaveCover,
                enabled = !uiState.isSavingCover && uiState.extractedCoverBitmap != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold18k),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_cover_gallery_button")
            ) {
                if (uiState.isSavingCover) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Salvando...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Salvar Capa JPG na Galeria", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
