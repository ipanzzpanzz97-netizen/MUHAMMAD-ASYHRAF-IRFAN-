package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LunoIntelDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = TextPrimary,
    primaryContainer = CyberCyanTint,
    onPrimaryContainer = CyberCyanBright,
    secondary = EmeraldGradeA,
    onSecondary = ObsidianBg,
    secondaryContainer = EmeraldTint,
    onSecondaryContainer = EmeraldBright,
    tertiary = AmberWatch,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberTint,
    onTertiaryContainer = AmberBright,
    error = CrimsonVeto,
    onError = TextPrimary,
    errorContainer = CrimsonTint,
    onErrorContainer = CrimsonBright,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = ObsidianBorder,
    outlineVariant = SlateHoldTint
)

val LunoIntelShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun LunoIntelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LunoIntelDarkColorScheme,
        typography = LunoIntelTypography,
        shapes = LunoIntelShapes,
        content = content
    )
}
