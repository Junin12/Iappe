package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.components.GlowButton
import com.example.ui.components.TeleprompterOverlay
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun CameraRecordScreen(
    onVideoRecorded: (Uri) -> Unit,
    scriptText: String = "",
    onOpenAiScripts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission states
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedScreen(
            onRequestPermission = {
                permissionsLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            },
            modifier = modifier
        )
    } else {
        CameraPreviewAndRecordingContent(
            context = context,
            hasAudioPermission = hasAudioPermission,
            scriptText = scriptText,
            onVideoRecorded = onVideoRecorded,
            onOpenAiScripts = onOpenAiScripts,
            modifier = modifier
        )
    }
}

@Composable
private fun CameraPermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, Gold18k.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, TikTokPink),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TikTokPink,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "Acesso à Câmera & Áudio Necessário",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Para filmar suas semijoias em macro 1080p, destacar o brilho das zircônias e gravar o unboxing ASMR sem cortes, precisamos de acesso à câmera traseira e microfone.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("request_camera_permission_button")
                ) {
                    Text("Permitir Acesso à Câmera", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    border = BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir Configurações do App", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CameraPreviewAndRecordingContent(
    context: Context,
    hasAudioPermission: Boolean,
    scriptText: String,
    onVideoRecorded: (Uri) -> Unit,
    onOpenAiScripts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera control references
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var videoCaptureRef by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // Jewelry filming macro states
    var isTorchOn by remember { mutableStateOf(false) }
    var isAeAfLocked by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var showFacelessGuide by remember { mutableStateOf(true) }
    var showTeleprompterOverlay by remember { mutableStateOf(false) }
    var exposureIndex by remember { mutableFloatStateOf(0f) }
    var exposureRange by remember { mutableStateOf(-4f..4f) }
    var exposureStep by remember { mutableFloatStateOf(0.5f) }
    var currentZoom by remember { mutableFloatStateOf(0f) } // 0f to 1f linear zoom
    var zoomRatioText by remember { mutableStateOf("1.0x") }

    // Focus indicator animation
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }

    // Countdown and Recording duration states
    var selectedCountdownSec by remember { mutableIntStateOf(3) } // 0, 3, 5, 10
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    var selectedTargetDurationSec by remember { mutableIntStateOf(15) } // 15s, 30s, 60s
    var isRecording by remember { mutableStateOf(false) }
    var recordedSeconds by remember { mutableIntStateOf(0) }

    // Error and alert states
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Check available storage space (min 50 MB)
    fun hasSufficientStorage(): Boolean {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable > 50 * 1024 * 1024 // 50MB
        } catch (_: Exception) {
            true
        }
    }

    // Stop recording cleanly
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecording = false
    }

    // Start recording after countdown finishes
    fun startActualRecording() {
        val videoCapture = videoCaptureRef
        if (videoCapture == null) {
            errorMessage = "Câmera não inicializada corretamente. Tente novamente."
            return
        }

        if (!hasSufficientStorage()) {
            errorMessage = "Espaço de armazenamento insuficiente no celular para gravar vídeo."
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "GlowStudio_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GlowStudio")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val recordingBuilder = videoCapture.output.prepareRecording(context, mediaStoreOutput)
        if (hasAudioPermission) {
            recordingBuilder.withAudioEnabled()
        }

        try {
            activeRecording = recordingBuilder.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        recordedSeconds = 0
                    }
                    is VideoRecordEvent.Status -> {
                        val durationSec = (recordEvent.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                        recordedSeconds = durationSec
                        if (durationSec >= selectedTargetDurationSec && isRecording) {
                            stopRecording()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            if (uri != Uri.EMPTY) {
                                onVideoRecorded(uri)
                            }
                        } else {
                            if (recordEvent.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                                Log.e("CameraRecord", "Recording finalized with error: ${recordEvent.error}")
                                errorMessage = "Gravação finalizada com aviso: código ${recordEvent.error}"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraRecord", "Error starting recording", e)
            errorMessage = "Não foi possível iniciar a gravação: ${e.localizedMessage}"
            isRecording = false
        }
    }

    // Trigger recording workflow (with optional countdown)
    fun initiateRecordButton() {
        if (isRecording) {
            stopRecording()
        } else if (isCountingDown) {
            isCountingDown = false
        } else {
            if (selectedCountdownSec > 0) {
                coroutineScope.launch {
                    isCountingDown = true
                    countdownRemaining = selectedCountdownSec
                    while (countdownRemaining > 0 && isCountingDown) {
                        delay(1000L)
                        countdownRemaining -= 1
                    }
                    if (isCountingDown) {
                        isCountingDown = false
                        startActualRecording()
                    }
                }
            } else {
                startActualRecording()
            }
        }
    }

    // Tap to focus and long press to lock focus & exposure
    fun handleTapPoint(offset: Offset, isLongPress: Boolean) {
        val previewView = previewViewRef ?: return
        val control = cameraControl ?: return

        tapFocusPoint = offset
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(offset.x, offset.y)

        val actionBuilder = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )

        if (isLongPress) {
            // Lock AF/AE continuously without auto cancel
            actionBuilder.disableAutoCancel()
            isAeAfLocked = true
        } else {
            actionBuilder.setAutoCancelDuration(3, TimeUnit.SECONDS)
            isAeAfLocked = false
        }

        try {
            control.startFocusAndMetering(actionBuilder.build())
        } catch (e: Exception) {
            Log.e("CameraRecord", "Focus error", e)
        }

        // Auto hide visual indicator after 2.5s if not locked
        if (!isLongPress) {
            coroutineScope.launch {
                delay(2500L)
                if (!isAeAfLocked) {
                    tapFocusPoint = null
                }
            }
        }
    }

    fun unlockAfAe() {
        cameraControl?.cancelFocusAndMetering()
        isAeAfLocked = false
        tapFocusPoint = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_record_screen")
    ) {
        // 9:16 Full Screen Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val qualitySelector = QualitySelector.from(
                            Quality.FHD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
                        )
                        val recorder = Recorder.Builder()
                            .setQualitySelector(qualitySelector)
                            .build()
                        val videoCapture = VideoCapture.withOutput(recorder)
                        videoCaptureRef = videoCapture

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            videoCapture
                        )

                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo

                        // Query exposure compensation bounds
                        val state = camera.cameraInfo.exposureState
                        exposureRange = state.exposureCompensationRange.lower.toFloat()..state.exposureCompensationRange.upper.toFloat()
                        exposureStep = state.exposureCompensationStep.toFloat()

                        // Monitor zoom state
                        camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                            zoomState?.let {
                                zoomRatioText = String.format("%.1fx", it.zoomRatio)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraRecord", "Camera binding failed", e)
                        errorMessage = "Falha ao abrir câmera traseira: ${e.localizedMessage}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            handleTapPoint(offset, isLongPress = false)
                        },
                        onLongPress = { offset ->
                            handleTapPoint(offset, isLongPress = true)
                        }
                    )
                }
        )

        // 3x3 Rule of thirds grid
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color.White.copy(alpha = 0.22f)

                // Vertical grid lines
                drawLine(lineColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
                drawLine(lineColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1f)

                // Horizontal grid lines
                drawLine(lineColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
                drawLine(lineColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1f)
            }
        }

        // Faceless Framing Guide (Guia de Enquadramento Sem Rosto)
        if (showFacelessGuide) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenHeight = maxHeight
                val forbiddenHeight = screenHeight * 0.24f

                // Top Forbidden Zone (Zona Proibida - Rosto)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(forbiddenHeight)
                        .background(Color(0x7A26050C))
                        .border(BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.6f)))
                        .padding(top = 38.dp, start = 12.dp, end = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xD91E0A10),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🚫", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZONA PROIBIDA: NUNCA O ROSTO",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Central Jewelry & Hands Framing Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(screenHeight * 0.44f)
                        .align(Alignment.Center)
                        .border(
                            BorderStroke(1.5.dp, Gold18k.copy(alpha = 0.75f)),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(8.dp)
                ) {
                    // Corner targeting marks
                    Text(
                        text = "💍 ÁREA DA JOIA & MÃOS (MACRO 1080p)",
                        color = Gold18k,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .background(ObsidianBackground.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )

                    Text(
                        text = "Foco ideal para brilho de zircônia e banho 18k",
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(ObsidianBackground.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Tap-to-Focus and AE/AF Lock Marker
        tapFocusPoint?.let { pos ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x.toInt() - 36, pos.y.toInt() - 36) }
                    .size(72.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (isAeAfLocked) Gold18k else Color.Yellow,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isAeAfLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "AE/AF Travado",
                        tint = Gold18k,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Top Overlay: Status, Progress Bar, Hook Zone Highlight & Quick Toggles
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Recording Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRecording) {
                            Surface(
                                shape = CircleShape,
                                color = TikTokPink,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GRAVANDO",
                                color = TikTokPink,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "PRONTA PARA GRAVAR",
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Timer string: e.g. 00:03 / 00:15
                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", recordedSeconds / 60, recordedSeconds % 60, selectedTargetDurationSec / 60, selectedTargetDurationSec % 60),
                        color = if (isRecording && recordedSeconds <= 3) Gold18k else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar with Golden 0-3s Hook Marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    val progressFraction = (recordedSeconds.toFloat() / selectedTargetDurationSec.toFloat()).coerceIn(0f, 1f)
                    val hookFraction = (3f / selectedTargetDurationSec.toFloat()).coerceIn(0f, 1f)

                    // Hook Zone Highlight (First 3s)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hookFraction)
                            .fillMaxHeight()
                            .background(Gold18k.copy(alpha = 0.45f))
                    )

                    // Current recording progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .background(
                                if (recordedSeconds <= 3) TikTokPink else TikTokCyan
                            )
                    )
                }

                if (isRecording && recordedSeconds <= 3) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ZONA DE OURO: Mostre o brilho máximo nos primeiros 3s!",
                            color = Gold18k,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Control Bar: Torch, Grid, Face Guide, Teleprompter, Lock Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left group: Quick helpers
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Torch Button
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) Gold18k else ObsidianSurfaceElevated.copy(alpha = 0.8f))
                            .testTag("camera_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Lanterna",
                            tint = if (isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Grid Toggle
                    IconButton(
                        onClick = { showGrid = !showGrid },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (showGrid) TikTokCyan.copy(alpha = 0.25f) else ObsidianSurfaceElevated.copy(alpha = 0.8f))
                            .border(1.dp, if (showGrid) TikTokCyan else Color.Transparent, CircleShape)
                            .testTag("camera_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Grade",
                            tint = if (showGrid) TikTokCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Faceless Guide Toggle
                    IconButton(
                        onClick = { showFacelessGuide = !showFacelessGuide },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (showFacelessGuide) Color(0x66FF5252) else ObsidianSurfaceElevated.copy(alpha = 0.8f))
                            .border(1.dp, if (showFacelessGuide) Color(0xFFFF5252) else Color.Transparent, CircleShape)
                            .testTag("camera_faceless_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Guia Sem Rosto",
                            tint = if (showFacelessGuide) Color(0xFFFF8A80) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Right group: AE/AF unlock & Teleprompter
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isAeAfLocked) {
                        Button(
                            onClick = { unlockAfAe() },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold18k),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(34.dp).testTag("camera_unlock_ae_af")
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Destravar AF", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Floating Teleprompter Toggle
                    IconButton(
                        onClick = { showTeleprompterOverlay = !showTeleprompterOverlay },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (showTeleprompterOverlay) TikTokPink else ObsidianSurfaceElevated.copy(alpha = 0.8f))
                            .testTag("camera_teleprompter_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Teleprompter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Floating Teleprompter overlay if opened
            AnimatedVisibility(
                visible = showTeleprompterOverlay,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TeleprompterOverlay(
                    scriptText = scriptText,
                    onClose = { showTeleprompterOverlay = false },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Full Screen Countdown Animation Overlay
        if (isCountingDown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("countdown_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$countdownRemaining",
                        color = Gold18k,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Posicione as mãos com a semijoia...",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Controls: Macro Exposure, Zoom & Record Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBackground.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Error banner if any
            errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xDD3A0A14),
                    border = BorderStroke(1.dp, Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = err, color = Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Macro Sliders Row: Exposure Compensation & Zoom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exposure slider (prevents gold blowout)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "☀️ Exposição",
                            color = Gold18k,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%+.1f EV", exposureIndex * exposureStep),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Slider(
                        value = exposureIndex,
                        onValueChange = { newVal ->
                            exposureIndex = newVal
                            cameraControl?.setExposureCompensationIndex(newVal.toInt())
                        },
                        valueRange = exposureRange,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Gold18k,
                            activeTrackColor = Gold18k,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.height(28.dp).testTag("camera_exposure_slider")
                    )
                }

                // Zoom Slider for zirconia close-ups
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔍 Zoom Macro",
                            color = TikTokCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = zoomRatioText,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Slider(
                        value = currentZoom,
                        onValueChange = { newZoom ->
                            currentZoom = newZoom
                            cameraControl?.setLinearZoom(newZoom)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = TikTokCyan,
                            activeTrackColor = TikTokCyan,
                            inactiveTrackColor = ObsidianBorder
                        ),
                        modifier = Modifier.height(28.dp).testTag("camera_zoom_slider")
                    )
                }
            }

            // Target Duration & Countdown Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "⏱️", fontSize = 12.sp)
                    listOf(0 to "0s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                        val isSelected = selectedCountdownSec == sec
                        Surface(
                            onClick = { if (!isRecording) selectedCountdownSec = sec },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) Gold18k else Color.Transparent),
                            modifier = Modifier.testTag("countdown_$sec")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Gold18k else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Target duration selector (15s, 30s, 60s)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Duração:", color = TextMuted, fontSize = 10.sp)
                    listOf(15 to "15s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                        val isSelected = selectedTargetDurationSec == sec
                        Surface(
                            onClick = { if (!isRecording) selectedTargetDurationSec = sec },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) TikTokPink else Color.Transparent),
                            modifier = Modifier.testTag("duration_$sec")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) TikTokPink else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Big Record Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(
                            width = 4.dp,
                            color = if (isRecording) TikTokPink else Gold18k,
                            shape = CircleShape
                        )
                        .padding(6.dp)
                ) {
                    // Inner shutter button
                    Surface(
                        onClick = { initiateRecordButton() },
                        shape = if (isRecording) RoundedCornerShape(12.dp) else CircleShape,
                        color = if (isRecording) TikTokPink else Color(0xFFE53935),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera_shutter_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                // Square stop icon
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
