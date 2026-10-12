package com.idp.smartgait.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.AppState
import com.idp.smartgait.data.SessionSummary
import com.idp.smartgait.ui.components.DisclaimerFooter
import com.idp.smartgait.ui.components.EmptyState
import com.idp.smartgait.ui.components.SectionHeader
import com.idp.smartgait.ui.components.fmt
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.ui.theme.riskColor
import com.idp.smartgait.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: AppState,
    vm: MainViewModel,
    selectedSession: SessionSummary?,
    modifier: Modifier = Modifier
) {
    if (selectedSession != null) {
        SessionDetailScreen(selectedSession, onBack = { vm.selectSession(null) }, modifier)
    } else {
        SessionListScreen(state, vm, modifier)
    }
}

/* ── Session list ──────────────────────────────────────── */
@Composable
private fun SessionListScreen(state: AppState, vm: MainViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Session History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("${state.history.size} sessions recorded", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        if (state.history.isEmpty()) {
            item {
                EmptyState(
                    "No sessions yet",
                    "Start your first monitoring session to build your gait history."
                )
            }
        } else {
            items(state.history.reversed()) { session ->
                SessionCard(session) { vm.selectSession(session) }
            }
        }

        item { DisclaimerFooter() }
    }
}

/* ── Session card ──────────────────────────────────────── */
@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    val dateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    val color = riskColor(session.peakOverall)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(dateFmt.format(Date(session.startedAt)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${timeFmt.format(Date(session.startedAt))} — ${timeFmt.format(Date(session.endedAt))}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                Text(session.finalLevel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatLabel("Duration", "${session.durationMs / 1000}s")
                StatLabel("Peak Risk", session.peakOverall.fmt())
                StatLabel("Avg HR", session.averageHr?.fmt() ?: "—")
            }
        }
    }
}

/* ── Session detail ────────────────────────────────────── */
@Composable
private fun SessionDetailScreen(session: SessionSummary, onBack: () -> Unit, modifier: Modifier) {
    val dateFmt = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Text("Session Detail", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("SESSION INFO")
                    DetailRow("Started", dateFmt.format(Date(session.startedAt)))
                    DetailRow("Ended", dateFmt.format(Date(session.endedAt)))
                    DetailRow("Duration", "${session.durationMs / 1000} seconds")
                    DetailRow("Final Risk Level", session.finalLevel)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("RISK STATISTICS")
                    DetailRow("Peak Overall", session.peakOverall.fmt())
                    DetailRow("Peak Injury", session.peakInjury.fmt())
                    DetailRow("Peak Physio", session.peakPhysio.fmt())
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("SENSOR STATISTICS")
                    DetailRow("Avg Heart Rate", "${session.averageHr?.fmt() ?: "—"} BPM")
                    DetailRow("Avg Temperature", "${session.averageTemp?.fmt() ?: "—"} °C")
                    DetailRow("Avg Cadence", "${session.averageCadence?.fmt() ?: "—"} steps/min")
                }
            }
        }

        item { DisclaimerFooter() }
    }
}

/* ── Helpers ──────────────────────────────────────────── */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricBlue)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

