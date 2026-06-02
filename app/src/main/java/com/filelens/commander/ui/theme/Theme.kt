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

private val FlDarkColorScheme = darkColorScheme(
    primary = FlAccent,
    onPrimary = FlOnAccent,
    primaryContainer = FlAccentPress,
    onPrimaryContainer = FlInk,
    secondary = FlAccentBright,
    onSecondary = FlOnAccent,
    secondaryContainer = FlSurface3,
    onSecondaryContainer = FlInk,
    tertiary = FlInk2,
    onTertiary = FlBg,
    background = FlBg,
    onBackground = FlInk,
    surface = FlSurface,
    onSurface = FlInk,
    surfaceVariant = FlSurface2,
    onSurfaceVariant = FlInk2,
    surfaceTint = FlAccent,
    inverseSurface = FlInk,
    inverseOnSurface = FlBg,
    error = RiskDanger,
    onError = FlOnAccent,
    errorContainer = FlSurface2,
    onErrorContainer = RiskDanger,
    outline = FlBorder,
    outlineVariant = FlBorder2,
    scrim = FlBg
)

private val FlLightColorScheme = lightColorScheme(
    primary = FlAccentLight,
    onPrimary = FlOnAccent,
    primaryContainer = FlAccentPressLight,
    onPrimaryContainer = FlInkLight,
    secondary = FlAccentBrightLight,
    onSecondary = FlOnAccent,
    secondaryContainer = FlSurface3Light,
    onSecondaryContainer = FlInkLight,
    background = FlBgLight,
    onBackground = FlInkLight,
    surface = FlSurfaceLight,
    onSurface = FlInkLight,
    surfaceVariant = FlSurface2Light,
    onSurfaceVariant = FlInk2Light,
    surfaceTint = FlAccentLight,
    error = RiskDangerLight,
    onError = FlOnAccent,
    outline = FlBorderLight,
    outlineVariant = FlBorder2Light
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
