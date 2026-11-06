package com.filelens.commander.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// FileLens — light-only palette.
// Violet accent on a near-white canvas. Source of truth lives alongside the
// design tokens in design/colors_and_type.css / design/min/tokens.css.
// ============================================================================

// ---------- Canvas / surface ramp ----------
val FlBg = Color(0xFFF7F7F8)         // app canvas
val FlSurface = Color(0xFFFFFFFF)    // cards, sheets, inspector body
val FlSurface2 = Color(0xFFF0F1F4)   // raised rows, input fills, hovered surfaces
val FlSurface3 = Color(0xFFE8EAEE)   // pressed wash, secondary chips

// ---------- Borders ----------
val FlBorder = Color(0xFFE3E5EA)     // hairline 1px
val FlBorder2 = Color(0xFFCBD0D8)    // stronger hairline / divider

// ---------- Ink (text + icons) ----------
val FlInk = Color(0xFF1A1B1F)        // primary ink — headings, body
val FlText = Color(0xFF5A5E66)       // secondary text, metadata
val FlText2 = Color(0xFF7B8089)      // labels, hint text
val FlMuted = Color(0xFF9098A2)      // tertiary ink, disabled glyphs

// Disabled controls.
val FlDisabled = Color(0xFFD7DAE0)
val FlDisabledInk = Color(0xFF9098A2)

// Legacy alias retained for callers that still reference FlInk3 / FlInk2.
val FlInk2 = FlText
val FlInk3 = FlText2

// ---------- Brand / accent ----------
// Violet accent — the only interaction colour. Used for primary actions,
// active tabs/rows, focused inputs, and the lens mark.
val FlAccent = Color(0xFF7C3AED)        // primary
val FlAccentBright = Color(0xFF8B5CF6)  // active text on light
val FlAccentPress = Color(0xFF6D28D9)   // pressed
val FlAccentTint = Color(0xFFEDE4FF)    // pale wash for selected rows / chips
val FlAccentTint2 = Color(0x147C3AED)   // 8% wash for subtle overlays
val FlOnAccent = Color(0xFFFFFFFF)

// Deep violet used for the optional brand header band (search bar inset, etc.).
// Kept under the legacy FlNavy / FlBlue names for source compatibility.
val FlNavy = Color(0xFF2E1065)
val FlNavy2 = Color(0xFF4C1D95)
val FlNavyInk = Color(0xFF1A1B1F)
val FlBlue = FlAccent
val FlBlueLight = FlAccentBright
val FlBlueDark = FlAccentPress
val FlBlue50 = FlAccentTint
val FlBlue900 = Color(0xFF3B0A8C)

// ---------- Trust / risk semantics ----------
val RiskDanger = Color(0xFFDC2626)
val RiskWarning = Color(0xFFB45309)
val RiskInfo = Color(0xFF2563EB)
val RiskSafe = Color(0xFF16A34A)
val RiskDangerTint = Color(0xFFFEE2E2)
val RiskWarningTint = Color(0xFFFEF3E0)
val RiskInfoTint = Color(0xFFDBEAFE)
val RiskSafeTint = Color(0xFFDCFCE7)

val TrustTrusted = Color(0xFF16A34A)
val TrustLow = Color(0xFF65A30D)
val TrustMedium = Color(0xFFB45309)
val TrustHigh = Color(0xFFDC2626)

// ---------- Category seeds ----------
// Category seeds are deliberately independent of the accent so files keep
// their semantic colour identity regardless of the brand hue.
val CategoryDirectory = Color(0xFFEAB308)    // amber folder
val CategoryImage = Color(0xFF14B8A6)
val CategoryVideo = Color(0xFFEF4444)
val CategoryAudio = Color(0xFFF97316)
val CategoryDocument = Color(0xFF2563EB)
val CategorySpreadsheet = Color(0xFF22C55E)
val CategoryPresentation = Color(0xFFEA580C)
val CategoryArchive = Color(0xFF7C3AED)
val CategoryCode = Color(0xFF0EA5E9)
val CategoryText = Color(0xFF64748B)
val CategoryPdf = Color(0xFFDC2626)
val CategoryApk = Color(0xFF16A34A)
val CategoryDatabase = Color(0xFFC2410C)
val CategoryFont = Color(0xFFDB2777)
val CategoryExecutable = Color(0xFF6B7280)
val CategoryCertificate = Color(0xFF0EA371)
val CategoryUnknown = Color(0xFF94A3B8)

// ============================================================================
// Dark palette
// ============================================================================

val FlBgDark = Color(0xFF121214)
val FlSurfaceDark = Color(0xFF1E1E22)
val FlSurface2Dark = Color(0xFF2A2A2E)
val FlSurface3Dark = Color(0xFF35353A)

val FlBorderDark = Color(0xFF3A3A40)
val FlBorder2Dark = Color(0xFF4A4A52)

val FlInkDark = Color(0xFFE8E8EC)
val FlTextDark = Color(0xFFA0A0A8)
val FlText2Dark = Color(0xFF9898A0)
val FlMutedDark = Color(0xFF606068)

val FlDisabledDark = Color(0xFF3A3A40)
val FlDisabledInkDark = Color(0xFF606068)

val FlAccentDark = Color(0xFF9B6BF7)
val FlAccentBrightDark = Color(0xFFB08DF8)
val FlAccentPressDark = Color(0xFF7C3AED)
val FlAccentTintDark = Color(0xFF2A1F3D)
val FlAccentTint2Dark = Color(0x149B6BF7)
val FlOnAccentDark = Color(0xFFFFFFFF)

val FlNavyDark = Color(0xFF1A0E33)
val FlNavy2Dark = Color(0xFF2E1065)
