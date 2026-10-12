package com.idp.smartgait.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.ui.components.AppLogo
import com.idp.smartgait.ui.theme.NavyDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    preferences: UserPreferences,
    onNavigate: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(2200)
        val route = when {
            preferences.isLoggedInNow()  -> "main"
            preferences.isOnboardingDone() -> "login"
            else -> "onboarding"
        }
        onNavigate(route)
    }

    Box(
        Modifier.fillMaxSize().background(NavyDark),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.85f, animationSpec = tween(1000))
        ) {
            AppLogo(showTagline = true)
        }
    }
}

