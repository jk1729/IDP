package com.idp.smartgait.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SmartGaitColorScheme = darkColorScheme(
    primary            = ElectricBlue,
    onPrimary          = Color.White,
    primaryContainer   = ElectricBlueDark,
    onPrimaryContainer = ElectricBlueLight,
    secondary          = NavyLight,
    onSecondary        = TextPrimary,
    secondaryContainer = NavyCard,
    onSecondaryContainer = TextPrimary,
    background         = NavyDark,
    onBackground       = TextPrimary,
    surface            = NavyMedium,
    onSurface          = TextPrimary,
    surfaceVariant     = NavyLight,
    onSurfaceVariant   = TextSecondary,
    error              = RiskHigh,
    onError            = Color.White,
    errorContainer     = RiskHighBg,
    onErrorContainer   = RiskHigh,
    outline            = TextMuted,
    outlineVariant     = NavyLight
)

@Composable
fun SmartGaitTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = NavyDark.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = NavyMedium.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = SmartGaitColorScheme,
        typography = SmartGaitTypography,
        content = content
    )
}

