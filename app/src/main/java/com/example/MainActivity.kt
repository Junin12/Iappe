package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.editor.EditorViewModel
import com.example.ui.components.WelcomeOnboardingDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.PecasViewModel
import com.example.ui.viewmodel.PlannerViewModel
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val studioViewModel: StudioViewModel by viewModels()
    private val pecasViewModel: PecasViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val plannerViewModel: PlannerViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlowStudioTheme {
                MainStudioApp(
                    viewModel = studioViewModel,
                    pecasViewModel = pecasViewModel,
                    dashboardViewModel = dashboardViewModel,
                    plannerViewModel = plannerViewModel,
                    editorViewModel = editorViewModel
                )
            }
        }
    }
}

@Composable
fun MainStudioApp(
    viewModel: StudioViewModel,
    pecasViewModel: PecasViewModel,
    dashboardViewModel: DashboardViewModel,
    plannerViewModel: PlannerViewModel,
    editorViewModel: EditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pecasUiState by pecasViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sub-tela do Editor de Vídeo Media3 (acessada a partir de um vídeo ou peça, não como aba)
    var isEditorOpen by rememberSaveable { mutableStateOf(false) }

    // Display snackbars when requested from StudioViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    // Display snackbars when requested from PecasViewModel
    LaunchedEffect(pecasUiState.snackbarMessage) {
        pecasUiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            pecasViewModel.clearSnackbar()
        }
    }

    // Trata o botão voltar do Android em todas as sub-telas, detalhes, editor e gravação
    BackHandler(
        enabled = isEditorOpen ||
                pecasUiState.selectedPeca != null ||
                uiState.recordedVideoReviewUri != null ||
                uiState.currentTab != StudioTab.GRAVAR
    ) {
        when {
            isEditorOpen -> {
                isEditorOpen = false
            }
            pecasUiState.selectedPeca != null -> {
                pecasViewModel.selectPeca(null)
            }
            uiState.recordedVideoReviewUri != null -> {
                viewModel.retakeVideo()
            }
            else -> {
                viewModel.selectTab(StudioTab.GRAVAR)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 8.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = ObsidianSurfaceElevated,
                    contentColor = TextPrimary,
                    actionColor = Gold18k,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, TikTokPink, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                )
            }
        },
        bottomBar = {
            if (!isEditorOpen && uiState.recordedVideoReviewUri == null) {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = ObsidianBorder,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .testTag("main_bottom_nav")
                ) {
                    listOf(
                        Pair(StudioTab.GRAVAR, Icons.Default.Videocam to "Gravar"),
                        Pair(StudioTab.PECAS, Icons.Default.Diamond to "Peças"),
                        Pair(StudioTab.AI_WRITER, Icons.Default.EditNote to "Roteiros"),
                        Pair(StudioTab.PLANNER, Icons.Default.CalendarToday to "Planejar"),
                        Pair(StudioTab.DASHBOARD, Icons.Default.BarChart to "Painel")
                    ).forEach { (tab, iconTitle) ->
                        val isSelected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = iconTitle.first,
                                    contentDescription = iconTitle.second,
                                    tint = if (isSelected) TikTokPink else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = iconTitle.second,
                                    color = if (isSelected) Gold18k else TextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = ObsidianSurfaceHighlight,
                                selectedIconColor = TikTokPink,
                                unselectedIconColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
        ) {
            if (isEditorOpen) {
                // Editor de Vídeo Media3 (sub-tela acessada a partir de um vídeo)
                StudioScreen(
                    editorViewModel = editorViewModel,
                    pecasViewModel = pecasViewModel,
                    onBack = { isEditorOpen = false }
                )
            } else if (uiState.recordedVideoReviewUri != null) {
                // Post-recording Review Screen
                VideoReviewScreen(
                    videoUri = android.net.Uri.parse(uiState.recordedVideoReviewUri),
                    onRetake = { viewModel.retakeVideo() },
                    onUseInStudio = { uri ->
                        uiState.recordingForPecaId?.let { pecaId ->
                            pecasViewModel.recordVideoForPeca(
                                pecaId = pecaId,
                                mediaUri = uri.toString(),
                                duracaoSeg = 15,
                                gancho = uiState.editorState.teleprompter.scriptText.take(60).ifBlank { "Brilho e acabamento impecável da peça" }
                            )
                            viewModel.clearRecordingForPeca()
                        }
                        val peca = pecasUiState.pecas.find { it.id == uiState.recordingForPecaId }
                        editorViewModel.loadImportedVideo(uri.toString(), peca)
                        viewModel.dismissReview()
                        isEditorOpen = true
                    }
                )
            } else {
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screen_transition"
                ) { currentTab ->
                    when (currentTab) {
                        StudioTab.GRAVAR -> CameraRecordScreen(
                            onVideoRecorded = { uri -> viewModel.onVideoRecorded(uri) },
                            scriptText = uiState.editorState.teleprompter.scriptText.ifBlank { uiState.aiGeneratedContent },
                            onOpenAiScripts = { viewModel.selectTab(StudioTab.AI_WRITER) }
                        )
                        StudioTab.PECAS -> {
                            if (pecasUiState.selectedPeca != null) {
                                PecaDetailScreen(
                                    peca = pecasUiState.selectedPeca!!,
                                    uiState = pecasUiState,
                                    viewModel = pecasViewModel,
                                    onBack = { pecasViewModel.selectPeca(null) },
                                    onGenerateScript = { peca ->
                                        viewModel.prefillForPeca(peca)
                                        viewModel.selectTab(StudioTab.AI_WRITER)
                                    },
                                    onRecordVideoForPeca = { peca ->
                                        viewModel.setRecordingForPeca(peca.id, peca.nome)
                                        viewModel.selectTab(StudioTab.GRAVAR)
                                    },
                                    onEditVideo = { v, peca ->
                                        editorViewModel.loadVideo(v, peca)
                                        isEditorOpen = true
                                    }
                                )
                            } else {
                                PecasScreen(
                                    viewModel = pecasViewModel,
                                    onSelectPeca = { peca -> pecasViewModel.selectPeca(peca) }
                                )
                            }
                        }
                        StudioTab.AI_WRITER -> AiScriptwriterScreen(
                            viewModel = viewModel,
                            pecasViewModel = pecasViewModel
                        )
                        StudioTab.PLANNER -> PlannerScreen(
                            viewModel = plannerViewModel,
                            onNavigateToRecord = { pecaId, pecaNome ->
                                viewModel.setRecordingForPeca(pecaId, pecaNome)
                                viewModel.selectTab(StudioTab.GRAVAR)
                            },
                            onEditVideo = { v, peca ->
                                editorViewModel.loadVideo(v, peca)
                                isEditorOpen = true
                            }
                        )
                        StudioTab.DASHBOARD -> DashboardScreen(viewModel = dashboardViewModel)
                    }
                }
            }

            // Global Teleprompter Modal (Accessible from tabs)
            if (uiState.editorState.teleprompter.isOpen) {
                com.example.ui.components.TeleprompterModal(
                    config = uiState.editorState.teleprompter,
                    onClose = { viewModel.closeTeleprompter() },
                    onToggleScroll = { viewModel.toggleTeleprompterScrolling() },
                    onSpeedChange = { viewModel.setTeleprompterSpeed(it) },
                    onFontSizeChange = { viewModel.setTeleprompterFontSize(it) },
                    onApplyAsCaption = { viewModel.applyTeleprompterAsCaption() }
                )
            }

            // Tela de boas-vindas curta na primeira abertura (3 passos, sem pedir permissão antecipada)
            WelcomeOnboardingDialog(onDismiss = {})
        }
    }
}
