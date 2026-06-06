package com.filelens.commander.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// FileLens ships a single light theme. The brand identity, contrast, and risk
// semantics are all tuned for a near-white canvas; the dark scheme was removed
// in v1.0 after light tested better in usability sessions.
private val FlLightColorScheme = lightColorScheme(
    primary = FlAccent,
    onPrimary = FlOnAccent,
    primaryContainer = FlAccentTint,
    onPrimaryContainer = FlBlue900,
    secondary = FlAccentPress,
    onSecondary = FlOnAccent,
    secondaryContainer = FlSurface2,
    onSecondaryContainer = FlInk,
    tertiary = FlNavy,
    onTertiary = FlOnAccent,
    background = FlBg,
    onBackground = FlInk,
    surface = FlSurface,
    onSurface = FlInk,
    surfaceVariant = FlSurface2,
    onSurfaceVariant = FlText,
    surfaceTint = FlAccent,
    error = RiskDanger,
    onError = FlOnAccent,
    errorContainer = RiskDangerTint,
    onErrorContainer = RiskDanger,
    outline = FlBorder,
    outlineVariant = FlBorder2
)

@Composable
fun FileLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlLightColorScheme,
        typography = Typography,
        content = content
    )
}
