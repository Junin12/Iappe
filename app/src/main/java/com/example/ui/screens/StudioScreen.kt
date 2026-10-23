package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val editorState = uiState.editorState

    var showCaptionDialog by remember { mutableStateOf(false) }
    var captionInput by remember(editorState.captionText) { mutableStateOf(editorState.captionText) }

    // Media Picker for user videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomVideoUploaded("Video_Galeria_${System.currentTimeMillis() % 1000}.mp4", uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TikTok Creator Studio",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "Edição, Iluminação & Retenção de Semijoias",
                    color = Gold18k,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💎", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "9:16 HD",
                        color = TikTokCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Creator Tools Bar: Teleprompter & Cover Generator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.openTeleprompter() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                border = BorderStroke(1.dp, TikTokPink.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("open_teleprompter_button")
            ) {
                Text(text = "🎙️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Teleprompter", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.openCoverGenerator() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("open_cover_generator_button")
            ) {
                Text(text = "🖼️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Criar Capa", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live 9:16 Video Player Preview
        VideoPreviewPlayer(
            editorState = editorState,
            shopBadges = uiState.shopBadges,
            onTogglePlay = { viewModel.togglePlayback() },
            onEditCaptionClick = { showCaptionDialog = true },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Timeline Trimmer with 0-3s Golden Zone
        TimelineTrimmer(
            trim = editorState.timelineTrim,
            onTrimChange = { start, end -> viewModel.setTimelineTrim(start, end) },
            onResetTrim = { viewModel.resetTimelineTrim() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Clipes & Gravação
        Card(
            shape = RoundedCornerShape(18.dp),
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
                        text = "📹 Clipes & Gravação",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    TextButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("upload_video_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = TikTokPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Galeria", color = TikTokPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Escolha um clipe de semijoia ou importe seu arquivo:",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.sampleClips) { clip ->
                        val isSelected = editorState.selectedSample?.id == clip.id
                        Surface(
                            onClick = { viewModel.selectSampleClip(clip) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) TikTokPink else ObsidianBorder
                            ),
                            modifier = Modifier
                                .width(150.dp)
                                .testTag("clip_item_${clip.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.horizontalGradient(clip.previewGradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) TikTokPink else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = clip.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = clip.category,
                                    color = Gold18k,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Iluminação de Estúdio & Ring Light Virtual
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💡 Iluminação de Estúdio & Ring Light Virtual",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Luz óptica simulada que ressalta o brilho das pedras e o polimento:",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LightingEffectType.values()) { lighting ->
                        val isSelected = editorState.currentLighting == lighting
                        Surface(
                            onClick = { viewModel.setLighting(lighting) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Gold18k else ObsidianBorder
                            ),
                            modifier = Modifier.testTag("lighting_${lighting.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = lighting.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lighting.displayName,
                                    color = if (isSelected) Gold18k else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (editorState.currentLighting != LightingEffectType.NATURAL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Intensidade da Iluminação", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "${(editorState.lightingIntensity * 100).toInt()}%", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = editorState.lightingIntensity,
                        onValueChange = { viewModel.setLightingIntensity(it) },
                        valueRange = 0.2f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Gold18k,
                            activeTrackColor = Gold18k,
                            inactiveTrackColor = ObsidianBorder
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filtros Estéticos Especializados
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "✨ Filtros de Cor & Contraste para Semijoias",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Realce o banho de ouro 18k, platina e brilho de zircônias:",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VideoFilterType.values()) { filter ->
                        val isSelected = editorState.currentFilter == filter
                        Surface(
                            onClick = { viewModel.setVideoFilter(filter) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Gold18k else ObsidianBorder
                            ),
                            modifier = Modifier.testTag("filter_${filter.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = filter.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = filter.displayName,
                                    color = if (isSelected) Gold18k else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (editorState.currentFilter != VideoFilterType.NONE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Intensidade do Filtro", color = TextSecondary, fontSize = 12.sp)
                        Text(text = "${(editorState.filterIntensity * 100).toInt()}%", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = editorState.filterIntensity,
                        onValueChange = { viewModel.setFilterIntensity(it) },
                        valueRange = 0.2f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Gold18k,
                            activeTrackColor = Gold18k,
                            inactiveTrackColor = ObsidianBorder
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legendas Automáticas & Estilos (inclui Karaokê / Palavra Pulsante)
        Card(
            shape = RoundedCornerShape(18.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💬", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Legenda Viral TikTok",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Retém espectadores que assistem no mudo",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = editorState.captionsEnabled,
                        onCheckedChange = { viewModel.toggleCaptions() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TikTokPink,
                            uncheckedTrackColor = ObsidianSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("toggle_captions_switch")
                    )
                }

                if (editorState.captionsEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editorState.captionText,
                        onValueChange = { viewModel.updateCaptionText(it) },
                        label = { Text("Texto da Legenda na Tela") },
                        placeholder = { Text("Ex: OLHA ESSE BRILHO NO BANHO 18K") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikTokPink,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showCaptionDialog = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = Gold18k)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caption_text_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Estilo Visual da Legenda (inclui Karaokê Bouncing):",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CaptionStyle.values()) { style ->
                            val isSelected = editorState.selectedCaptionStyle == style
                            Surface(
                                onClick = { viewModel.setCaptionStyle(style) },
                                shape = RoundedCornerShape(10.dp),
                                color = style.bgColor,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) style.primaryColor else ObsidianBorder
                                ),
                                modifier = Modifier.testTag("caption_style_${style.name}")
                            ) {
                                Text(
                                    text = style.displayName,
                                    color = style.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mixer de Áudio com Audio Ducking Automático
        Card(
            shape = RoundedCornerShape(18.dp),
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
                    Column {
                        Text(
                            text = "🎛️ Mixer de Áudio & Ducking",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Abaixa a música automaticamente quando há som ASMR ou voz",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = editorState.audioMixer.isDuckingEnabled,
                        onCheckedChange = { viewModel.toggleAudioDucking() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Gold18k,
                            uncheckedTrackColor = ObsidianSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("toggle_ducking_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Music Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Trilha Sonora TikTok", color = TextSecondary, fontSize = 11.sp)
                    Text(text = "${(editorState.audioMixer.musicVolume * 100).toInt()}%", color = Gold18k, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Slider(
                    value = editorState.audioMixer.musicVolume,
                    onValueChange = { viewModel.setMusicVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold18k,
                        activeTrackColor = Gold18k,
                        inactiveTrackColor = ObsidianBorder
                    )
                )

                // ASMR Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Efeitos ASMR & Voz", color = TextSecondary, fontSize = 11.sp)
                    Text(text = "${(editorState.audioMixer.asmrVoiceVolume * 100).toInt()}%", color = TikTokPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Slider(
                    value = editorState.audioMixer.asmrVoiceVolume,
                    onValueChange = { viewModel.setAsmrVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = TikTokPink,
                        activeTrackColor = TikTokPink,
                        inactiveTrackColor = ObsidianBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selos de Conversão TikTok Shop
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🏷️ Selos de Conversão TikTok Shop",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Gatilhos visuais que aumentam cliques na sacolinha amarela:",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                uiState.shopBadges.forEach { badge ->
                    val isActive = editorState.activeBadges.contains(badge.id)
                    Surface(
                        onClick = { viewModel.toggleShopBadge(badge.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isActive) badge.accentColor else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("badge_toggle_${badge.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = badge.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = badge.title,
                                        color = if (isActive) badge.accentColor else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = badge.subtitle,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Checkbox(
                                checked = isActive,
                                onCheckedChange = { viewModel.toggleShopBadge(badge.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = badge.accentColor,
                                    uncheckedColor = ObsidianBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Exportação e Publicação no TikTok
        if (editorState.isExporting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { editorState.exportProgress },
                    color = TikTokPink,
                    trackColor = ObsidianSurfaceHighlight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Renderizando vídeo em 1080x1920 60fps (${(editorState.exportProgress * 100).toInt()}%)...",
                    color = Gold18k,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else if (editorState.isExportFinished) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vídeo & Capa Prontos para Publicar no TikTok!",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Legenda, hashtags e capa de alta conversão copiadas para sua área de transferência.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowButton(
                        text = "Abrir TikTok & Publicar Agora",
                        icon = { Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White) },
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Semijoia TikTok Shop")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${editorState.captionText}\n\nGaranta a sua na sacolinha amarela aqui embaixo com cupom de primeira compra! ✨ #semijoias #tiktokshopbrasil #unboxingsemijoias #achadinhos"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Publicar com TikTok"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "publish_tiktok_button"
                    )
                }
            }
        } else {
            GlowButton(
                text = "Exportar & Postar no TikTok",
                icon = { Text(text = "🚀", fontSize = 18.sp) },
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        "TikTok Caption",
                        "${editorState.captionText}\n\nCorre na sacolinha amarela aqui embaixo com cupom de desconto! ✨ #semijoias #tiktokshopbrasil #ouro18k"
                    )
                    clipboard.setPrimaryClip(clip)
                    viewModel.exportAndPrepareForTikTok()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "export_button"
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Dialog for fast caption edition
    if (showCaptionDialog) {
        AlertDialog(
            onDismissRequest = { showCaptionDialog = false },
            title = { Text("Editar Legenda do Vídeo", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = captionInput,
                    onValueChange = { captionInput = it },
                    label = { Text("Texto que prenderá a atenção") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TikTokPink,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCaptionText(captionInput)
                        showCaptionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptionDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = ObsidianSurface,
            textContentColor = TextPrimary
        )
    }
}
