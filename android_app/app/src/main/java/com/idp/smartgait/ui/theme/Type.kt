package com.idp.smartgait.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SmartGaitTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 48.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 36.sp, letterSpacing = (-0.5).sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 24.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp),
    bodyLarge     = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 0.5.sp)
)

