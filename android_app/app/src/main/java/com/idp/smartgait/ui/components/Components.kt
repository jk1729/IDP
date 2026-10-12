package com.idp.smartgait.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idp.smartgait.data.ConnectionState
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.StatusConnected
import com.idp.smartgait.ui.theme.StatusDisconnected
import com.idp.smartgait.ui.theme.StatusError
import com.idp.smartgait.ui.theme.TextMuted
import com.idp.smartgait.ui.theme.TextSecondary
import com.idp.smartgait.ui.theme.riskBgColor
import com.idp.smartgait.ui.theme.riskColor
import java.util.Locale

/* ================================================================
 *  SECTION HEADER
 * ================================================================ */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/* ================================================================
 *  PRIMARY / SECONDARY BUTTON
 * ================================================================ */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/* ================================================================
 *  METRIC CARD
 * ================================================================ */
@Composable
fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String?,
    unit: String,
    modifier: Modifier = Modifier,
    iconTint: Color = ElectricBlue
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (value != null && unit.isNotEmpty()) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

/* ================================================================
 *  RISK GAUGE (circular arc)
 * ================================================================ */
@Composable
fun RiskGauge(
    score: Float,
    level: String,
    modifier: Modifier = Modifier,
    label: String = "OVERALL RISK"
) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(800),
        label = "risk_gauge"
    )
    val color = riskColor(score)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.width, size.height)
                // Background arc
                drawArc(
                    color = NavyLight,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcSize
                )
                // Foreground arc
                val sweep = (animatedScore / 100f).coerceIn(0f, 1f) * 270f
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcSize
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${animatedScore.toInt()}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(level, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ================================================================
 *  RISK CARD
 * ================================================================ */
@Composable
fun RiskCard(
    title: String,
    score: Float,
    level: String,
    modifier: Modifier = Modifier
) {
    val color = riskColor(score)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = riskBgColor(score)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(score.fmt(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            }
            Box(
                Modifier.background(color.copy(alpha = .15f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(level, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ================================================================
 *  ALERT CARD
 * ================================================================ */
@Composable
fun AlertCard(
    alertType: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskHigh.copy(alpha = .12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Warning, "Alert", tint = RiskHigh, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("⚠ $alertType", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RiskHigh)
                Spacer(Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Consider reducing activity intensity and reviewing gait pattern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/* ================================================================
 *  CONNECTION INDICATOR
 * ================================================================ */
@Composable
fun ConnectionIndicator(state: ConnectionState, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (state) {
        ConnectionState.CONNECTED    -> Triple(Icons.Rounded.BluetoothConnected, StatusConnected, "Connected")
        ConnectionState.CONNECTING   -> Triple(Icons.Rounded.Bluetooth, ElectricBlue, "Connecting")
        ConnectionState.SCANNING     -> Triple(Icons.Rounded.Search, ElectricBlue, "Scanning")
        ConnectionState.ERROR        -> Triple(Icons.Rounded.Error, StatusError, "Error")
        ConnectionState.DISCONNECTED -> Triple(Icons.Rounded.BluetoothDisabled, StatusDisconnected, "Disconnected")
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Spacer(Modifier.width(6.dp))
        Icon(icon, label, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/* ================================================================
 *  SIMULATION BADGE
 * ================================================================ */
@Composable
fun SimulationBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(ElectricBlue.copy(alpha = .15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("SIMULATION", style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
    }
}

/* ================================================================
 *  EMPTY / LOADING STATES
 * ================================================================ */
@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingState(message: String = "Loading…", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.CircularProgressIndicator(color = ElectricBlue)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/* ================================================================
 *  APP LOGO
 * ================================================================ */
@Composable
fun AppLogo(modifier: Modifier = Modifier, showTagline: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Stylized icon using Canvas
        Canvas(Modifier.size(64.dp)) {
            drawCircle(color = ElectricBlue.copy(alpha = .15f), radius = size.minDimension / 2f)
            drawCircle(color = ElectricBlue.copy(alpha = .3f), radius = size.minDimension / 3f)
            drawCircle(color = ElectricBlue, radius = size.minDimension / 5f)
        }
        Spacer(Modifier.height(12.dp))
        Text("SMART GAIT", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = ElectricBlue)
        if (showTagline) {
            Spacer(Modifier.height(4.dp))
            Text("Intelligent Gait & Performance Monitoring", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

/* ================================================================
 *  DISCLAIMER FOOTER
 * ================================================================ */
@Composable
fun DisclaimerFooter(modifier: Modifier = Modifier) {
    Text(
        "Prototype risk estimation — not a medical diagnosis.",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

/* ── Formatting helper ──────────────────────────────────── */
fun Float.fmt(): String = String.format(Locale.US, "%.1f", this)

