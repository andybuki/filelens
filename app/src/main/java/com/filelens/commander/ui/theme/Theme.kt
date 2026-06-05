package com.filelens.commander.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Minimal light scheme: navy header lives outside the Material color roles, so
// the scheme itself is anchored on white surfaces + the blue primary accent.
private val FlLightColorScheme = lightColorScheme(
    primary = FlBlue,
    onPrimary = FlOnAccent,
    primaryContainer = FlBlue50,
    onPrimaryContainer = FlBlue900,
    secondary = FlBlueDark,
    onSecondary = FlOnAccent,
    secondaryContainer = FlSurface3,
    onSecondaryContainer = FlInk,
    tertiary = FlNavy,
    onTertiary = FlOnAccent,
    background = FlSurface,
    onBackground = FlInk,
    surface = FlSurface,
    onSurface = FlInk,
    surfaceVariant = FlSurface3,
    onSurfaceVariant = FlText,
    surfaceTint = FlBlue,
    error = RiskDanger,
    onError = FlOnAccent,
    errorContainer = RiskDangerTint,
    onErrorContainer = RiskDanger,
    outline = FlBorder,
    outlineVariant = FlBorder2
)

private val FlDarkColorScheme = darkColorScheme(
    primary = FlAccentDark,
    onPrimary = FlOnAccent,
    primaryContainer = FlAccentPressDark,
    onPrimaryContainer = FlInkDark,
    secondary = FlAccentBrightDark,
    onSecondary = FlOnAccent,
    secondaryContainer = FlSurface3Dark,
    onSecondaryContainer = FlInkDark,
    tertiary = FlInk2Dark,
    onTertiary = FlBgDark,
    background = FlBgDark,
    onBackground = FlInkDark,
    surface = FlSurfaceDark,
    onSurface = FlInkDark,
    surfaceVariant = FlSurface2Dark,
    onSurfaceVariant = FlInk2Dark,
    surfaceTint = FlAccentDark,
    inverseSurface = FlInkDark,
    inverseOnSurface = FlBgDark,
    error = RiskDanger,
    onError = FlOnAccent,
    errorContainer = FlSurface2Dark,
    onErrorContainer = RiskDanger,
    outline = FlBorderDark,
    outlineVariant = FlBorder2Dark,
    scrim = FlBgDark
)

@Composable
fun FileLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FlDarkColorScheme
        else -> FlLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
