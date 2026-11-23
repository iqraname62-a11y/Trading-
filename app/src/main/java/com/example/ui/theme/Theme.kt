package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SignalPulseColorScheme = darkColorScheme(
    primary = QuantCyan,
    onPrimary = ObsidianBg,
    primaryContainer = QuantCyanMuted,
    onPrimaryContainer = QuantCyan,
    secondary = BullEmerald,
    onSecondary = ObsidianBg,
    secondaryContainer = BullEmeraldMuted,
    onSecondaryContainer = BullEmerald,
    tertiary = IndigoAccent,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = SurfaceHighlight,
    outline = BorderSlate,
    outlineVariant = BorderSlate,
    error = BearCrimson,
    onError = TextPrimary,
    errorContainer = BearCrimsonMuted,
    onErrorContainer = BearCrimson
)

private val SignalPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SignalPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SignalPulseColorScheme,
        typography = SignalPulseTypography,
        shapes = SignalPulseShapes,
        content = content
    )
}
