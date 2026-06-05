package com.filelens.commander.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.filelens.commander.R

// =============================================================================
// Fonts
// The minimal design tokens (min/tokens.css) call for Roboto + Roboto Mono so
// the Android build matches the web preview. Stock Android resolves
// FontFamily.Default to Roboto, but OEM skins (Samsung One UI, Xiaomi MIUI…)
// substitute their own house font — same goes for FontFamily.Monospace which
// is almost never Roboto Mono. To avoid that drift we pull both families from
// Google Fonts' downloadable provider so every device renders identically.
// =============================================================================

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun robotoFont(weight: FontWeight) = Font(
    googleFont = GoogleFont("Roboto"),
    fontProvider = GoogleFontsProvider,
    weight = weight
)

private fun robotoMonoFont(weight: FontWeight) = Font(
    googleFont = GoogleFont("Roboto Mono"),
    fontProvider = GoogleFontsProvider,
    weight = weight
)

val Roboto: FontFamily = FontFamily(
    robotoFont(FontWeight.Normal),
    robotoFont(FontWeight.Medium),
    robotoFont(FontWeight.SemiBold),
    robotoFont(FontWeight.Bold),
    robotoFont(FontWeight.Black)
)

val RobotoMono: FontFamily = FontFamily(
    robotoMonoFont(FontWeight.Normal),
    robotoMonoFont(FontWeight.Medium),
    robotoMonoFont(FontWeight.SemiBold)
)

private val Sans = Roboto
private val Mono = RobotoMono

// Reusable mono styles for data-dense surfaces (sizes, hashes, hex, scores).
val MonoBody = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp
)

val MonoSmall = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 16.sp
)

val Eyebrow = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 11.sp,
    letterSpacing = 0.08.em
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.01).em
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Caption / mono-sm — uppercase ext badges and dense data labels use the
    // mono family so digits and short codes stay column-aligned.
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em
    )
)

