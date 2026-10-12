package com.idp.smartgait.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.ui.components.ChartCard
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.DualChartCard
import com.idp.smartgait.ui.components.EmptyState
import com.idp.smartgait.ui.components.MetricCard
import com.idp.smartgait.ui.components.RiskCard
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.SimulationBadge
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.ElectricBlueGlow
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.RiskModerate
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.ui.theme.riskColor
import kotlin.math.max

@Composable
fun GaitScreen(state: AppState, modifier: Modifier = Modifier) {
    val live = state.live
    val risk = state.risk.packet

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Gait Analysis", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Early gait-risk estimation", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (state.simulation) SimulationBadge()
            }
        }

        // ── Pressure cards ──────────────────
        item { SectionHeader("FOOT PRESSURE") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.Vibration, "Heel", live?.heel?.fmt(), "FSR", Modifier.weight(1f), iconTint = ElectricBlueGlow)
                MetricCard(Icons.Rounded.Vibration, "Forefoot", live?.forefoot?.fmt(), "FSR", Modifier.weight(1f), iconTint = ElectricBlue)
            }
        }

        // ── Balance indicator ───────────────
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Heel / Forefoot Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (live?.heel != null && live.forefoot != null) {
                        val total = live.heel + live.forefoot
                        val heelPct = if (total > 0) live.heel / total else 0.5f
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Heel ${(heelPct * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = ElectricBlueGlow)
                            Text("Forefoot ${((1 - heelPct) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(12.dp).background(NavyLight, RoundedCornerShape(6.dp))) {
                            Box(
                                Modifier.fillMaxWidth(heelPct).height(12.dp).background(ElectricBlueGlow, RoundedCornerShape(6.dp))
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val ratio = live.heel / max(live.forefoot, 0.01f)
                        Text(
                            "Ratio: ${ratio.fmt()} : 1",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when {
                                ratio > 1.8f -> "⚠ Heavy rearfoot loading"
                                ratio < 0.6f -> "⚠ Heavy forefoot loading"
                                else -> "✓ Balanced distribution"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ratio in 0.6f..1.8f) RiskLow else RiskModerate
                        )
                    } else {
                        Text("Waiting for pressure data", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        // ── Motion ──────────────────────────
        item { SectionHeader("MOTION") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.DirectionsWalk, "Cadence", live?.cad?.fmt(), "steps/min", Modifier.weight(1f), iconTint = RiskLow)
                MetricCard(Icons.Rounded.Speed, "Acceleration", live?.acc?.fmt(), "m/s²", Modifier.weight(1f), iconTint = RiskModerate)
            }
        }

        // ── Injury risk ─────────────────────
        item { SectionHeader("GAIT RISK ESTIMATION") }
        item { RiskCard("Injury Risk", risk?.injury ?: 0f, risk?.injury_level ?: "—") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Risk Components (from ESP32)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    RiskComponent("Pressure deviation", 35)
                    RiskComponent("Gait-motion deviation", 25)
                    RiskComponent("Impact deviation", 20)
                    RiskComponent("Gait-cycle abnormality", 20)
                }
            }
        }

        // ── Graphs ──────────────────────────
        item { SectionHeader("TRENDS") }
        if (state.samples.isNotEmpty()) {
            item { DualChartCard("Heel vs Forefoot Pressure", state.samples.mapNotNull { it.heel }, state.samples.mapNotNull { it.forefoot }, 0f, 600f) }
            item { ChartCard("Cadence Trend", state.samples.mapNotNull { it.cad }, 0f, 220f, ElectricBlueGlow, "steps/min") }
            item { ChartCard("Injury Risk Trend", state.riskHistory.map { it.injury }, 0f, 100f, riskColor(risk?.injury ?: 0f), "0–100") }
        } else {
            item { EmptyState("No trend data", "Start a monitoring session to see gait trends.") }
        }

        item { DisclaimerFooter() }
    }
}

@Composable
private fun RiskComponent(name: String, weight: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text("${weight}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = ElectricBlue)
    }
}

