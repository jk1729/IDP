package com.idp.smartgait.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.ui.components.AlertCard
import com.idp.smartgait.ui.components.ChartCard
import com.idp.smartgait.ui.components.ConnectionIndicator
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.DualChartCard
import com.idp.smartgait.ui.components.EmptyState
import com.idp.smartgait.ui.components.MetricCard
import com.idp.smartgait.ui.components.PrimaryButton
import com.idp.smartgait.ui.components.RiskCard
import com.idp.smartgait.ui.components.RiskChartCard
import com.idp.smartgait.ui.components.SecondaryButton
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.SimulationBadge
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.ElectricBlueGlow
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.RiskModerate
import com.idp.smartgait.ui.theme.riskColor
import com.idp.smartgait.repository.SimulationScenario
import com.idp.smartgait.viewmodel.MainViewModel

@Composable
fun LiveScreen(state: AppState, vm: MainViewModel, selectedScenario: SimulationScenario, modifier: Modifier = Modifier) {
    val live = state.live
    val risk = state.risk.packet
    val status = state.status

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Live Monitoring", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                if (state.simulation) SimulationBadge()
            }
        }

        // ── Device status ───────────────────
        item { SectionHeader("DEVICE") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Icons.Rounded.BluetoothConnected, "BLE Status", state.connection.name, "", Modifier.weight(1f))
                MetricCard(Icons.Rounded.BatteryChargingFull, "Battery", status?.battery?.toString(), "%", Modifier.weight(1f), iconTint = RiskLow)
            }
        }
        item { ConnectionIndicator(state.connection) }

        // ── Controls ────────────────────────
        item { SectionHeader("CONTROLS") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = if (state.monitoring) "● ACTIVE" else "▶ START",
                    onClick = { vm.startMonitoring() },
                    enabled = !state.monitoring,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "■ STOP",
                    onClick = { vm.stopMonitoring() },
                    enabled = state.monitoring,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "◎ CALIBRATE",
                    onClick = { vm.calibrate() },
                    enabled = !state.calibrating,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calibration progress
        if (state.calibrating) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { state.calibrationProgress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = ElectricBlue,
                        trackColor = NavyLight
                    )
                }
                Text("Calibrating… ${state.calibrationProgress}%", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
            }
        }

        // Calibration result
        state.baseline?.let { b ->
            item {
                Text("Baseline: HR ${b.hr.fmt()} BPM • Temp ${b.temp.fmt()} °C", style = MaterialTheme.typography.bodySmall, color = RiskLow)
            }
        }

        // ── Simulation controls ─────────────
        item { SectionHeader("SIMULATION MODE") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimulationScenario.values().forEach { scenario ->
                    FilterChip(
                        selected = selectedScenario == scenario,
                        onClick = { vm.scenario(scenario) },
                        label = { Text(scenario.name) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = .2f),
                            selectedLabelColor = ElectricBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Live signals ────────────────────
        item { SectionHeader("LIVE SIGNALS") }
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

        // ── Risk ────────────────────────────
        item { SectionHeader("RISK ASSESSMENT") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiskCard("Injury", risk?.injury ?: 0f, risk?.injury_level ?: "—", Modifier.weight(1f))
                RiskCard("Physio", risk?.physio ?: 0f, risk?.physio_level ?: "—", Modifier.weight(1f))
            }
        }
        item { RiskCard("Overall", risk?.overall ?: 0f, risk?.overall_level ?: "—") }

        if (risk?.alert == true) {
            item {
                AlertCard(
                    alertType = "High Risk Alert",
                    message = risk.message.ifBlank { "Abnormal gait and elevated physiological indicators detected." }
                )
            }
        }

        // ── Graphs ──────────────────────────
        item { SectionHeader("REAL-TIME GRAPHS") }
        if (state.samples.isEmpty()) {
            item { EmptyState("Waiting for data", "Sensor data will appear here as it is received.") }
        } else {
            item { ChartCard("Heart Rate", state.samples.mapNotNull { it.hr }, 0f, 180f, RiskHigh, "BPM") }
            item { ChartCard("Temperature", state.samples.mapNotNull { it.temp }, 30f, 42f, RiskModerate, "°C") }
            item { DualChartCard("Heel vs Forefoot Pressure", state.samples.mapNotNull { it.heel }, state.samples.mapNotNull { it.forefoot }, 0f, 600f) }
            item { ChartCard("Cadence", state.samples.mapNotNull { it.cad }, 0f, 220f, ElectricBlueGlow, "steps/min") }
            item { ChartCard("Acceleration", state.samples.mapNotNull { it.acc }, 0f, 3f, RiskLow, "m/s²") }
            item { RiskChartCard("Injury Risk Trend", state.riskHistory.map { it.injury }, currentColor = riskColor(risk?.injury ?: 0f)) }
            item { RiskChartCard("Physiological Risk Trend", state.riskHistory.map { it.physio }, currentColor = riskColor(risk?.physio ?: 0f)) }
            item { RiskChartCard("Overall Risk Trend", state.riskHistory.map { it.overall }, currentColor = riskColor(risk?.overall ?: 0f)) }
        }

        item { DisclaimerFooter() }
    }
}

