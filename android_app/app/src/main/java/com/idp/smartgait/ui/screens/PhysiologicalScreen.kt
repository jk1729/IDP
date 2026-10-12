package com.idp.smartgait.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.ui.components.AlertCard
import com.idp.smartgait.ui.components.ChartCard
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.EmptyState
import com.idp.smartgait.ui.components.MetricCard
import com.idp.smartgait.ui.components.RiskCard
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.SimulationBadge
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskModerate
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.ui.theme.riskColor

/**
 * Physiological monitoring section — shown as part of Live or accessible from Gait.
 * This is also embedded in the Settings/Device area for quick reference.
 */
@Composable
fun PhysiologicalSection(state: AppState, modifier: Modifier = Modifier) {
    val live = state.live
    val risk = state.risk.packet
    val baseline = state.baseline

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Physiological Monitoring", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Prototype estimate — not a medical diagnosis", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (state.simulation) SimulationBadge()
            }
        }

        // ── Heart Rate ──────────────────────
        item { SectionHeader("HEART RATE") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(Icons.Rounded.FavoriteBorder, "Baseline HR", baseline?.hr?.fmt(), "BPM", Modifier.weight(1f), iconTint = RiskModerate)
                        MetricCard(Icons.Rounded.FavoriteBorder, "Current HR", live?.hr?.fmt(), "BPM", Modifier.weight(1f), iconTint = RiskHigh)
                    }
                    if (baseline != null && live?.hr != null) {
                        val delta = live.hr - baseline.hr
                        val deltaStr = (if (delta >= 0) "+" else "") + delta.fmt()
                        Text("ΔHR: $deltaStr BPM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = if (kotlin.math.abs(delta) > 20f) RiskHigh else ElectricBlue)
                    }
                }
            }
        }

        // ── Temperature ─────────────────────
        item { SectionHeader("TEMPERATURE") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(Icons.Rounded.Thermostat, "Baseline", baseline?.temp?.fmt(), "°C", Modifier.weight(1f), iconTint = RiskModerate)
                        MetricCard(Icons.Rounded.Thermostat, "Current", live?.temp?.fmt(), "°C", Modifier.weight(1f), iconTint = RiskHigh)
                    }
                    if (baseline != null && live?.temp != null) {
                        val delta = live.temp - baseline.temp
                        val deltaStr = (if (delta >= 0) "+" else "") + delta.fmt()
                        Text("ΔTemp: $deltaStr °C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = if (kotlin.math.abs(delta) > 1f) RiskHigh else ElectricBlue)
                    }
                }
            }
        }

        // ── Physiological risk ──────────────
        item { SectionHeader("PHYSIOLOGICAL RISK") }
        item { RiskCard("Physiological Risk", risk?.physio ?: 0f, risk?.physio_level ?: "—") }

        if (baseline == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No baseline established", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Use the Calibrate button in Live Monitoring to establish your personal baseline for HR and temperature.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        // ── Trends ──────────────────────────
        item { SectionHeader("TRENDS") }
        if (state.samples.isNotEmpty()) {
            item { ChartCard("Heart Rate Trend", state.samples.mapNotNull { it.hr }, 0f, 180f, RiskHigh, "BPM") }
            item { ChartCard("Temperature Trend", state.samples.mapNotNull { it.temp }, 30f, 42f, RiskModerate, "°C") }
            item { ChartCard("Physio Risk Trend", state.riskHistory.map { it.physio }, 0f, 100f, riskColor(risk?.physio ?: 0f), "0–100") }
        } else {
            item { EmptyState("No physiological data", "Start monitoring to see physiological trends.") }
        }

        item { DisclaimerFooter() }
    }
}

