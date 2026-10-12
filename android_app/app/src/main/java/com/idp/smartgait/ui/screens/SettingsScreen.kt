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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.data.ConnectionState
import com.idp.smartgait.data.UserPreferences
import com.idp.smartgait.ui.components.ConnectionIndicator
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.SecondaryButton
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.SimulationBadge
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: AppState,
    vm: MainViewModel,
    preferences: UserPreferences,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        // ── Device ──────────────────────────
        item { SectionHeader("DEVICE") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        ConnectionIndicator(state.connection)
                    }
                    SettingRow("Device Name", state.status?.device ?: "Not connected")
                    SettingRow("Firmware", state.status?.firmware ?: "Unavailable")
                    SettingRow("Battery", if (state.status != null) "${state.status.battery}%" else "—")
                    SettingRow("Sensors", if (state.status?.sensor_ready == true) "Ready" else "Waiting for sensor data")
                    SettingRow("Mode", state.status?.mode ?: "Simulation")
                }
            }
        }

        // ── Calibration ─────────────────────
        item { SectionHeader("CALIBRATION") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Personal Baseline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.baseline != null) {
                        SettingRow("Baseline HR", "${state.baseline.hr.fmt()} BPM")
                        SettingRow("Baseline Temp", "${state.baseline.temp.fmt()} °C")
                        state.baseline.heel?.let { SettingRow("Baseline Heel", it.fmt()) }
                        state.baseline.forefoot?.let { SettingRow("Baseline Forefoot", it.fmt()) }
                    } else {
                        Text("No baseline established yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (state.calibrating) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { state.calibrationProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = ElectricBlue,
                            trackColor = NavyLight
                        )
                        Text("Calibrating… ${state.calibrationProgress}%", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                    } else {
                        SecondaryButton("Calibrate Baseline", onClick = { vm.calibrate() }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // ── App ─────────────────────────────
        item { SectionHeader("APP") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Simulation Mode", style = MaterialTheme.typography.titleMedium)
                        if (state.simulation) SimulationBadge()
                    }
                    SettingRow("Data Source", if (state.simulation) "Simulated" else "BLE Hardware")
                    SettingRow("Monitoring", if (state.monitoring) "Active" else "Inactive")
                    SettingRow("Sessions Recorded", "${state.history.size}")
                }
            }
        }

        // ── About ───────────────────────────
        item { SectionHeader("ABOUT") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Smart Gait", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricBlue)
                    SettingRow("Version", "1.0.0")
                    SettingRow("Project", "IDP — Smart Gait Monitoring & Early Injury-Risk Detection System")
                    SettingRow("Architecture", "ESP32 + 2 FSR + MPU6050 + MAX30102 + Temp → BLE → Android")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This is a student prototype for gait monitoring and early risk estimation. It does not provide medical diagnosis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // ── Logout ──────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        preferences.logout()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RiskHigh.copy(alpha = .15f), contentColor = RiskHigh)
            ) {
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }
        }

        item { DisclaimerFooter() }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

