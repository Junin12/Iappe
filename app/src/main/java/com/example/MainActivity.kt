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
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val studioViewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlowStudioTheme {
                MainStudioApp(viewModel = studioViewModel)
            }
        }
    }
}

@Composable
fun MainStudioApp(
    viewModel: StudioViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display snackbars when requested
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    // Handle back button on sub-screens
    BackHandler(enabled = uiState.currentTab != StudioTab.EDITOR) {
        viewModel.selectTab(StudioTab.EDITOR)
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
                    Pair(StudioTab.EDITOR, Icons.Default.MovieFilter to "Estúdio"),
                    Pair(StudioTab.TEMPLATES, Icons.Default.AutoAwesome to "Templates"),
                    Pair(StudioTab.AI_WRITER, Icons.Default.EditNote to "Roteiro IA"),
                    Pair(StudioTab.TRENDS, Icons.Default.TrendingUp to "Tendências")
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
                                fontSize = 11.sp
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
        ) {
            AnimatedContent(
                targetState = uiState.currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { currentTab ->
                when (currentTab) {
                    StudioTab.EDITOR -> StudioScreen(viewModel = viewModel)
                    StudioTab.TEMPLATES -> TemplatesScreen(viewModel = viewModel)
                    StudioTab.AI_WRITER -> AiScriptwriterScreen(viewModel = viewModel)
                    StudioTab.TRENDS -> TrendsAnalyticsScreen(viewModel = viewModel)
                }
            }

            // Global Teleprompter Modal (Accessible from any tab: Editor, Templates, AI Writer)
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

            // Global Cover Generator Modal
            if (uiState.editorState.coverThumbnail.isOpen) {
                com.example.ui.components.CoverGeneratorModal(
                    state = uiState.editorState.coverThumbnail,
                    onClose = { viewModel.closeCoverGenerator() },
                    onStyleChange = { viewModel.setCoverStyle(it) },
                    onHeadlineChange = { viewModel.setCoverHeadline(it) },
                    onPriceTagChange = { viewModel.setCoverPriceTag(it) },
                    onSaveCover = { viewModel.saveCover() }
                )
            }
        }
    }
}
