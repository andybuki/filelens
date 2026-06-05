package com.filelens.commander.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// FileLens Minimal palette
// Navy header + blue accent on a slate-light surface. Source of truth lives
// alongside the React preview in min/tokens.css.
// ============================================================================

// ---------- Brand ----------
val FlNavy = Color(0xFF0F2A44)
val FlNavy2 = Color(0xFF1E3A5F)
val FlNavyInk = Color(0xFF0F172A)

val FlBlue = Color(0xFF2563EB)
val FlBlueLight = Color(0xFF3B82F6)
val FlBlueDark = Color(0xFF1D4ED8)
val FlBlue50 = Color(0xFFDBEAFE)
val FlBlue900 = Color(0xFF1E3A8A)

// ---------- Slate ramp (default = light theme) ----------
val FlInk = Color(0xFF0F172A)
val FlInk2 = Color(0xFF1E293B)
val FlText = Color(0xFF475569)
val FlText2 = Color(0xFF64748B)
val FlMuted = Color(0xFF94A3B8)
val FlBorder = Color(0xFFE5E7EB)
val FlBorder2 = Color(0xFFCBD5E1)
val FlSurface = Color(0xFFFFFFFF)
val FlSurface2 = Color(0xFFF8FAFC)
val FlSurface3 = Color(0xFFF1F5F9)
val FlDisabled = Color(0xFFCBD5E1)
val FlDisabledInk = Color(0xFF94A3B8)

// Aliases retained for callers that still reference the legacy "Ink3" tertiary.
val FlInk3 = FlText2

// ---------- Accent role tokens ----------
val FlAccent = FlBlue
val FlAccentBright = FlBlueLight
val FlAccentPress = FlBlueDark
// Solid pale-blue chip/selection tint that reads on white surfaces.
val FlAccentTint = FlBlue50
val FlAccentTint2 = Color(0x142563EB)
val FlOnAccent = Color(0xFFFFFFFF)

// ---------- Trust / risk semantics ----------
val RiskDanger = Color(0xFFDC2626)
val RiskWarning = Color(0xFFB45309)
val RiskInfo = FlBlue
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
val CategoryDirectory = FlBlue
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
val CategoryUnknown = Color(0xFF94A3B8)

// ============================================================================
// Dark palette — legacy JetBrains-violet kept for users who pin dark mode.
// ============================================================================
val FlBgDark = Color(0xFF16171A)
val FlSurfaceDark = Color(0xFF1E2024)
val FlSurface2Dark = Color(0xFF25272D)
val FlSurface3Dark = Color(0xFF2E3138)
val FlBorderDark = Color(0xFF34373E)
val FlBorder2Dark = Color(0xFF3F434B)

val FlInkDark = Color(0xFFE4E6EB)
val FlInk2Dark = Color(0xFFA4A8B2)
val FlInk3Dark = Color(0xFF6E727C)
val FlDisabledDark = Color(0xFF4A4D55)

// On a navy/dark surface the brand still reads best in blue; keep the same hue
// but lighten it slightly so it pops on the deep background.
val FlAccentDark = FlBlueLight
val FlAccentBrightDark = Color(0xFF60A5FA)
val FlAccentPressDark = FlBlue
