package com.idp.smartgait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.data.UserProfile
import com.idp.smartgait.ui.components.AlertCard
import com.idp.smartgait.ui.components.ConnectionIndicator
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.MetricCard
import com.idp.smartgait.ui.components.RiskCard
import com.idp.smartgait.ui.components.RiskGauge
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.SimulationBadge
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.ElectricBlueGlow
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.RiskModerate
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.ui.theme.riskColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(state: AppState, userProfile: UserProfile, modifier: Modifier = Modifier) {
    val risk = state.risk.packet
    val live = state.live
    val status = state.status

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        userProfile.name.ifBlank { "Athlete" },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    ConnectionIndicator(state.connection)
                    if (state.simulation) { Spacer(Modifier.height(4.dp)); SimulationBadge() }
                }
            }
        }

        // ── Risk gauge ──────────────────────
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RiskGauge(
                    score = risk?.overall ?: 0f,
                    level = risk?.overall_level ?: "—",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    "Updated ${timeAgo(state.risk.receivedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // ── Alert ───────────────────────────
        if (risk?.alert == true) {
            item {
                AlertCard(
                    alertType = when (risk.alert_type) {
                        "BOTH" -> "High Injury & Physiological Risk"
                        "INJURY" -> "High Injury Risk"
                        "PHYSIO" -> "High Physiological Risk"
                        else -> "Risk Alert"
                    },
                    message = "Abnormal gait and elevated physiological indicators detected. Overall risk: ${risk.overall.fmt()} (${risk.overall_level})"
                )
            }
        }

        // ── Risk breakdown ──────────────────
        item { SectionHeader("RISK BREAKDOWN") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiskCard("Injury", risk?.injury ?: 0f, risk?.injury_level ?: "—", Modifier.weight(1f))
                RiskCard("Physio", risk?.physio ?: 0f, risk?.physio_level ?: "—", Modifier.weight(1f))
            }
        }

        // ── Sensor cards ────────────────────
        item { SectionHeader("LIVE SENSORS") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.FavoriteBorder, "Heart Rate", live?.hr?.fmt(), "BPM", Modifier.weight(1f), iconTint = RiskHigh)
                MetricCard(Icons.Rounded.Thermostat, "Temperature", live?.temp?.fmt(), "°C", Modifier.weight(1f), iconTint = RiskModerate)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.Vibration, "Heel FSR", live?.heel?.fmt(), "", Modifier.weight(1f), iconTint = ElectricBlueGlow)
                MetricCard(Icons.Rounded.Vibration, "Forefoot FSR", live?.forefoot?.fmt(), "", Modifier.weight(1f), iconTint = ElectricBlue)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.DirectionsWalk, "Cadence", live?.cad?.fmt(), "steps/min", Modifier.weight(1f), iconTint = RiskLow)
                MetricCard(Icons.Rounded.Speed, "Acceleration", live?.acc?.fmt(), "m/s²", Modifier.weight(1f), iconTint = RiskModerate)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.BatteryChargingFull, "Battery", status?.battery?.toString(), "%", Modifier.weight(1f), iconTint = RiskLow)
                MetricCard(Icons.Rounded.BluetoothConnected, "Device", status?.device ?: "—", "", Modifier.weight(1f))
            }
        }

        // ── Monitoring status ───────────────
        if (state.monitoring) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("● ", color = RiskLow, style = MaterialTheme.typography.bodyLarge)
                    Text("Monitoring active", style = MaterialTheme.typography.bodyMedium, color = RiskLow)
                    state.sessionStartedAt?.let {
                        Spacer(Modifier.width(8.dp))
                        Text("• ${((System.currentTimeMillis() - it) / 1000)}s", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        item { DisclaimerFooter() }
    }
}

/* ── Helpers ──────────────────────────────────────────── */
private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun timeAgo(ts: Long): String {
    if (ts == 0L) return "—"
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 5_000  -> "just now"
        diff < 60_000 -> "${diff / 1000}s ago"
        else          -> SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))
    }
}

