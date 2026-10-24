package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

private const val PREFS_NAME = "glowstudio_prefs"
private const val KEY_SEEN_WELCOME = "has_seen_onboarding_v2"

@Composable
fun WelcomeOnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var shouldShow by remember { mutableStateOf(!prefs.getBoolean(KEY_SEEN_WELCOME, false)) }

    if (!shouldShow) return

    Dialog(
        onDismissRequest = {
            prefs.edit().putBoolean(KEY_SEEN_WELCOME, true).apply()
            shouldShow = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, Gold18k.copy(alpha = 0.8f)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Gold18k.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✨", fontSize = 28.sp)
                }

                // Title & Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bem-vinda ao GlowStudio!",
                        color = Gold18k,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seu estúdio completo para criar, gravar, editar e vender semijoias no TikTok Shop.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                // 3 Steps
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OnboardingStepRow(
                        stepNumber = "1",
                        badgeColor = Gold18k,
                        title = "Cadastre suas Peças",
                        description = "Tire fotos ou use a IA (Gemini) para identificar banho, pedra e precificação real."
                    )
                    OnboardingStepRow(
                        stepNumber = "2",
                        badgeColor = TikTokPink,
                        title = "Roteiros & Teleprompter",
                        description = "Crie ganchos magnéticos faceless para os primeiros 3 segundos e grave lendo na tela."
                    )
                    OnboardingStepRow(
                        stepNumber = "3",
                        badgeColor = TikTokCyan,
                        title = "Edite, Exporte & Acompanhe",
                        description = "Corte início/fim, queime o gancho no vídeo, aplique o preset Brilho de Joia e acompanhe métricas reais."
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Button
                Button(
                    onClick = {
                        prefs.edit().putBoolean(KEY_SEEN_WELCOME, true).apply()
                        shouldShow = false
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold18k,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("welcome_start_button")
                ) {
                    Text(
                        text = "Começar a Criar ✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "As permissões de câmera e notificações serão solicitadas apenas quando você gravar ou agendar.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(
    stepNumber: String,
    badgeColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, badgeColor),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    color = badgeColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}
