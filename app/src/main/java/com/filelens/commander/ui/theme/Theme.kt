package com.filelens.commander.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.filelens.commander.domain.model.ThemeMode

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

private val FlDarkColorScheme = darkColorScheme(
    primary = FlAccentDark,
    onPrimary = FlOnAccentDark,
    primaryContainer = FlAccentTintDark,
    onPrimaryContainer = FlAccentBrightDark,
    secondary = FlAccentPressDark,
    onSecondary = FlOnAccentDark,
    secondaryContainer = FlSurface2Dark,
    onSecondaryContainer = FlInkDark,
    tertiary = FlNavyDark,
    onTertiary = FlOnAccentDark,
    background = FlBgDark,
    onBackground = FlInkDark,
    surface = FlSurfaceDark,
    onSurface = FlInkDark,
    surfaceVariant = FlSurface2Dark,
    onSurfaceVariant = FlTextDark,
    surfaceTint = FlAccentDark,
    error = RiskDanger,
    onError = FlOnAccent,
    errorContainer = Color(0xFF3D1010),
    onErrorContainer = RiskDanger,
    outline = FlBorderDark,
    outlineVariant = FlBorder2Dark
)

@Composable
fun FileLensTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val base = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        base.withBrandHeader(useDark).copy(
            // Risk colours carry meaning; keep them stable across wallpapers.
            error = RiskDanger,
            errorContainer = if (useDark) FlDarkColorScheme.errorContainer else RiskDangerTint,
            onErrorContainer = RiskDanger
        )
    } else {
        if (useDark) FlDarkColorScheme else FlLightColorScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}

/**
 * Screens paint their header band with `tertiary` under white text and light
 * status-bar icons, so the band must stay dark whatever the wallpaper is.
 */
private fun ColorScheme.withBrandHeader(dark: Boolean): ColorScheme =
    if (dark) copy(tertiary = primaryContainer, onTertiary = onPrimaryContainer)
    else copy(tertiary = primary, onTertiary = onPrimary)
