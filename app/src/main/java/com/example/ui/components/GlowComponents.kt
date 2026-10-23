package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    testTag: String = "glow_button"
) {
    val gradientBrush = if (isSecondary) {
        Brush.horizontalGradient(
            colors = listOf(ObsidianSurfaceElevated, ObsidianSurfaceHighlight)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(TikTokPink, VibrantPurple)
        )
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 52.dp)
            .shadow(
                elevation = if (enabled && !isSecondary) 10.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (!isSecondary) TikTokPink else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .border(
                width = 1.dp,
                color = if (isSecondary) ObsidianBorder else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun GoldPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Gold18k.copy(alpha = 0.15f))
            .border(1.dp, Gold18k.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = Gold18k,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun CopyableTextCard(
    textToCopy: String,
    title: String? = null,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Gold18k,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied Text", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            onCopied()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar texto",
                            tint = TikTokPink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = textToCopy,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun RetentionScoreMeter(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_anim"
    )

    val scoreColor = when {
        score >= 80 -> SuccessGreen
        score >= 60 -> Gold18k
        else -> WarningAmber
    }

    Box(
        modifier = modifier
            .size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            color = ObsidianSurfaceHighlight,
            strokeWidth = 8.dp,
            modifier = Modifier.fillMaxSize()
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            color = scoreColor,
            strokeWidth = 8.dp,
            modifier = Modifier.fillMaxSize()
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                color = scoreColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Retenção",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
