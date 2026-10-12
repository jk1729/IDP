package com.idp.smartgait.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary palette ──────────────────────────────────────
val NavyDark       = Color(0xFF0B1426)
val NavyMedium     = Color(0xFF132036)
val NavyLight      = Color(0xFF1C2D44)
val NavyCard       = Color(0xFF172A42)

// ── Accent ───────────────────────────────────────────────
val ElectricBlue      = Color(0xFF00B4D8)
val ElectricBlueDark  = Color(0xFF0077B6)
val ElectricBlueLight = Color(0xFF90E0EF)
val ElectricBlueGlow  = Color(0xFF48CAE4)

// ── Text ─────────────────────────────────────────────────
val TextPrimary   = Color(0xFFE8EDF3)
val TextSecondary = Color(0xFF8899AA)
val TextMuted     = Color(0xFF5A6577)

// ── Risk colors ──────────────────────────────────────────
val RiskLow      = Color(0xFF00E676)
val RiskModerate = Color(0xFFFFB300)
val RiskHigh     = Color(0xFFFF5252)

val RiskLowBg      = Color(0xFF0D2818)
val RiskModerateBg = Color(0xFF2C2200)
val RiskHighBg     = Color(0xFF2D0A0A)

// ── Status ───────────────────────────────────────────────
val StatusConnected    = Color(0xFF00E676)
val StatusDisconnected = Color(0xFF78909C)
val StatusError        = Color(0xFFFF5252)

// ── Utilities ────────────────────────────────────────────
fun riskColor(value: Float): Color = when {
    value >= 70f -> RiskHigh
    value >= 40f -> RiskModerate
    else         -> RiskLow
}

fun riskBgColor(value: Float): Color = when {
    value >= 70f -> RiskHighBg
    value >= 40f -> RiskModerateBg
    else         -> RiskLowBg
}

fun riskLevel(value: Float): String = when {
    value >= 70f -> "HIGH"
    value >= 40f -> "MODERATE"
    else         -> "LOW"
}

