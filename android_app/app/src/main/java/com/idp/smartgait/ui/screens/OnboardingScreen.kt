package com.idp.smartgait.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idp.smartgait.ui.components.PrimaryButton
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyDark
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        Icons.Rounded.DirectionsWalk,
        "Gait Monitoring",
        "Track heel and forefoot pressure patterns in real time using dual FSR sensors to understand your gait biomechanics."
    ),
    OnboardingPage(
        Icons.Rounded.MonitorHeart,
        "Physiological Tracking",
        "Monitor heart rate and body temperature through integrated sensors to detect physiological changes during activity."
    ),
    OnboardingPage(
        Icons.Rounded.Shield,
        "Early Risk Detection",
        "Receive real-time risk estimates when abnormal gait patterns or physiological deviations are detected — enabling earlier intervention."
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(NavyDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onComplete) {
                Text("Skip", color = TextSecondary)
            }
        }

        // Pager
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            PageContent(pages[page])
        }

        // Dots
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            repeat(pages.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) ElectricBlue else NavyLight)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (pagerState.currentPage == pages.size - 1) {
            PrimaryButton("Get Started", onClick = onComplete, modifier = Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Text("Back", color = TextSecondary)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                PrimaryButton("Next", onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } })
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon circle
        Canvas(Modifier.size(120.dp)) {
            drawCircle(color = ElectricBlue.copy(alpha = .1f), radius = size.minDimension / 2f)
            drawCircle(color = ElectricBlue.copy(alpha = .2f), radius = size.minDimension / 3f)
        }
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Icon(page.icon, page.title, tint = ElectricBlue, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(page.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(page.description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

