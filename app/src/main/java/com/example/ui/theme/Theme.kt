package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TikTokPink,
    onPrimary = ObsidianBackground,
    primaryContainer = VibrantPurple,
    onPrimaryContainer = TextPrimary,
    secondary = Gold18k,
    onSecondary = ObsidianBackground,
    secondaryContainer = ObsidianSurfaceHighlight,
    onSecondaryContainer = Gold18k,
    tertiary = TikTokCyan,
    onTertiary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianSurfaceHighlight
)

@Composable
fun GlowStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
