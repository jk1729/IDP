package com.idp.smartgait.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.*
import com.idp.smartgait.protocol.LiveDataPacket
import com.idp.smartgait.protocol.RiskPacket
import com.idp.smartgait.repository.*
import com.idp.smartgait.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.max

private enum class Tab(val title: String) { DASHBOARD("Dashboard"), LIVE("Live"), GAIT("Gait"), HISTORY("History"), SETTINGS("Settings") }

@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }
    Scaffold(bottomBar = {
        NavigationBar { Tab.values().forEach { item ->
            NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(item.title.take(1)) }, label = { Text(item.title) })
        } }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item { Header(state) }
            if (state.message != null) item { InfoBanner(state.message!!, state.message == "Session complete") }
            when (tab) {
                Tab.DASHBOARD -> item { Dashboard(state, vm) }
                Tab.LIVE -> item { LiveScreen(state) }
                Tab.GAIT -> item { GaitScreen(state) }
                Tab.HISTORY -> item { HistoryScreen(state) }
                Tab.SETTINGS -> item { SettingsScreen(state, vm) }
            }
            item { Text("Prototype risk estimation — not a medical diagnosis.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun Header(s: AppState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("Smart Gait", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Early warning dashboard", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        AssistChip(onClick = {}, label = { Text(if (s.simulation) "SIMULATION" else s.connection.name) })
    }
}

@Composable private fun Dashboard(s: AppState, vm: MainViewModel) {
    val risk = s.risk.packet
    Card(colors = CardDefaults.cardColors(containerColor = riskColor(risk?.overall ?: 0f).copy(alpha = .14f)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("OVERALL RISK", style = MaterialTheme.typography.labelLarge)
            Text(risk?.overall?.format() ?: "Waiting for data", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(risk?.overall_level ?: "—", style = MaterialTheme.typography.titleLarge, color = riskColor(risk?.overall ?: 0f), fontWeight = FontWeight.Bold)
            if (risk?.alert == true) Text("HIGH RISK DETECTED\nAbnormal gait and elevated physiological indicators detected.", color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Injury", risk?.injury?.format() ?: "—")
                Metric("Physio", risk?.physio?.format() ?: "—")
            }
        }
    }
    SensorGrid(s)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.startMonitoring() }, enabled = !s.monitoring, modifier = Modifier.weight(1f)) { Text(if (s.monitoring) "ACTIVE" else "START") }
        OutlinedButton(onClick = { vm.stopMonitoring() }, enabled = s.monitoring, modifier = Modifier.weight(1f)) { Text("STOP") }
        OutlinedButton(onClick = { vm.calibrate() }, enabled = !s.calibrating, modifier = Modifier.weight(1f)) { Text("CALIBRATE") }
    }
    Text("Demo / Simulation Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SimulationScenario.values().forEach { scenario ->
        FilterChip(selected = s.risk.packet?.status == "SIMULATION" && scenario.name == s.message?.uppercase()?.removeSuffix(" SIMULATION SELECTED"), onClick = { vm.scenario(scenario) }, label = { Text(scenario.name) })
    } }
}

@Composable private fun SensorGrid(s: AppState) {
    val l = s.live; val status = s.status
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Sensor(Modifier.weight(1f), "Heart rate", l?.hr?.format(), "BPM"); Sensor(Modifier.weight(1f), "Temperature", l?.temp?.format(), "°C") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Sensor(Modifier.weight(1f), "Cadence", l?.cad?.format(), "steps/min"); Sensor(Modifier.weight(1f), "Acceleration", l?.acc?.format(), "m/s²") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Sensor(Modifier.weight(1f), "Heel pressure", l?.heel?.format(), ""); Sensor(Modifier.weight(1f), "Forefoot pressure", l?.forefoot?.format(), "") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Sensor(Modifier.weight(1f), "Battery", status?.battery?.toString(), "%"); Sensor(Modifier.weight(1f), "Device", status?.device, "") }
    }
}

@Composable private fun Sensor(modifier: Modifier, label: String, value: String?, unit: String) { Card(modifier) { Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value ?: "Waiting for data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); if (value != null) Text(unit, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun Metric(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable private fun InfoBanner(text: String, positive: Boolean) { Text(text, Modifier.fillMaxWidth().background(if (positive) Color(0xFFE6F4EA) else MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(12.dp), color = if (positive) Color(0xFF176B37) else MaterialTheme.colorScheme.onPrimaryContainer) }

@Composable private fun LiveScreen(s: AppState) {
    Text("Live metrics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (s.samples.isEmpty()) Text("Waiting for sensor data") else {
        ChartCard("Heart rate over time", s.samples.mapNotNull { it.hr }, 0f, 180f, Color(0xFF6750A4), "BPM")
        ChartCard("Temperature over time", s.samples.mapNotNull { it.temp }, 30f, 42f, Color(0xFFE76F51), "°C")
        DualChartCard("Heel vs forefoot pressure", s.samples.mapNotNull { it.heel }, s.samples.mapNotNull { it.forefoot }, 0f, 600f)
        ChartCard("Cadence over time", s.samples.mapNotNull { it.cad }, 0f, 220f, Color(0xFF457B9D), "steps/min")
        ChartCard("Acceleration over time", s.samples.mapNotNull { it.acc }, 0f, 3f, Color(0xFFF4A261), "m/s²")
        ChartCard("Risk trend", s.riskHistory.map { it.overall }, 0f, 100f, riskColor(s.risk.packet?.overall ?: 0f), "0–100")
    }
}

@Composable private fun ChartCard(title: String, values: List<Float>, min: Float, maxValue: Float, color: Color, unit: String) {
    Card { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
        if (values.size > 1) { val path = Path(); values.forEachIndexed { i, value -> val x = size.width * i / (values.size - 1); val y = size.height * (1f - ((value - min) / (maxValue - min)).coerceIn(0f, 1f)); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)); values.forEachIndexed { i, value -> val x = size.width * i / (values.size - 1); val y = size.height * (1f - ((value - min) / (maxValue - min)).coerceIn(0f, 1f)); drawCircle(color, 4f, Offset(x, y)) } }
    } } }
}

@Composable private fun GaitScreen(s: AppState) {
    Text("Gait Analysis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Early gait-risk estimation", color = MaterialTheme.colorScheme.onSurfaceVariant)
    val l = s.live; val r = s.risk.packet
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Heel pressure", l?.heel?.format() ?: "Waiting for data"); Metric("Forefoot pressure", l?.forefoot?.format() ?: "Waiting for data"); Metric("Loading relationship", if (l?.heel != null && l.forefoot != null) "${(l.heel / max(l.forefoot, .01f)).format()} : 1" else "Waiting for data"); Metric("Cadence", l?.cad?.format() ?: "Waiting for data"); Metric("Gait-related risk", r?.injury?.format() ?: "Waiting for data") } }
}

@Composable private fun HistoryScreen(s: AppState) {
    Text("Session History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (s.history.isEmpty()) Text("No sessions recorded yet.") else s.history.asReversed().forEach { session -> Card { Column(Modifier.padding(16.dp)) { Text("${session.finalLevel} risk session", fontWeight = FontWeight.Bold); Text("Duration ${session.durationMs / 1000}s • Peak overall ${session.peakOverall.format()}"); Text("Avg HR ${session.averageHr?.format() ?: "—"} BPM • Avg cadence ${session.averageCadence?.format() ?: "—"}") } } }
}

@Composable private fun SettingsScreen(s: AppState, vm: MainViewModel) {
    Text("Settings & Device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Device status", fontWeight = FontWeight.Bold); Text("Connection: ${s.connection}"); Text("BLE: ${if (s.connection == ConnectionState.CONNECTED) "Connected" else "Waiting for device"}"); Text("Sensors: ${if (s.status?.sensor_ready == true) "Ready" else "Waiting for sensor data"}"); Text("Firmware: ${s.status?.firmware ?: "Unavailable"}"); Text("Mode: ${s.status?.mode ?: "Simulation"}") } }
    Card { Column(Modifier.padding(16.dp)) { Text("Personal baseline", fontWeight = FontWeight.Bold); if (s.baseline == null) Text("Calibrate to establish your personal baseline") else { Text("Baseline HR ${s.baseline.hr.format()} BPM"); Text("Baseline temperature ${s.baseline.temp.format()} °C") }; if (s.calibrating) { LinearProgressIndicator({ s.calibrationProgress / 100f }, Modifier.fillMaxWidth()); Text("Calibrating... ${s.calibrationProgress}%") } else OutlinedButton(onClick = { vm.calibrate() }) { Text("Calibrate baseline") } } }
}

private fun Float.format(): String = String.format(Locale.US, "%.1f", this)
private fun riskColor(value: Float): Color = when { value >= 70f -> Color(0xFFBA1A1A); value >= 40f -> Color(0xFF9A6700); else -> Color(0xFF2E7D32) }

@Composable private fun DualChartCard(title: String, first: List<Float>, second: List<Float>, min: Float, maxValue: Float) {
    Card { Column(Modifier.padding(12.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text("Heel  •  Forefoot", style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
            listOf(first to Color(0xFF2A9D8F), second to Color(0xFFE76F51)).forEach { (values, color) ->
                if (values.size > 1) {
                    val path = Path()
                    values.forEachIndexed { i, value ->
                        val x = size.width * i / (values.size - 1)
                        val y = size.height * (1f - ((value - min) / (maxValue - min)).coerceIn(0f, 1f))
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                }
            }
        }
    } }
}
