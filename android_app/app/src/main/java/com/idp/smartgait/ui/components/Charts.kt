package com.idp.smartgait.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idp.smartgait.ui.theme.ElectricBlue
import com.idp.smartgait.ui.theme.ElectricBlueGlow
import com.idp.smartgait.ui.theme.NavyCard
import com.idp.smartgait.ui.theme.NavyLight
import com.idp.smartgait.ui.theme.RiskHigh
import com.idp.smartgait.ui.theme.RiskLow
import com.idp.smartgait.ui.theme.TextMuted
import com.idp.smartgait.ui.theme.TextSecondary

/* ================================================================
 *  SINGLE-LINE CHART CARD
 * ================================================================ */
@Composable
fun ChartCard(
    title: String,
    values: List<Float>,
    minY: Float,
    maxY: Float,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (values.isNotEmpty()) {
                    Text(values.last().fmt(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = lineColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            LineChart(values, minY, maxY, lineColor, Modifier.fillMaxWidth().height(100.dp))
        }
    }
}

/* ================================================================
 *  DUAL-LINE CHART CARD (heel vs forefoot)
 * ================================================================ */
@Composable
fun DualChartCard(
    title: String,
    first: List<Float>,
    second: List<Float>,
    minY: Float,
    maxY: Float,
    modifier: Modifier = Modifier,
    firstColor: Color = ElectricBlueGlow,
    secondColor: Color = RiskHigh.copy(alpha = .8f),
    firstLabel: String = "Heel",
    secondLabel: String = "Forefoot"
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NavyCard), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row {
                LegendDot(firstColor, firstLabel)
                Spacer(Modifier.width(16.dp))
                LegendDot(secondColor, secondLabel)
            }
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                drawLine(firstColor, first, minY, maxY)
                drawLine(secondColor, second, minY, maxY)
            }
        }
    }
}

/* ================================================================
 *  LINE CHART (Canvas)
 * ================================================================ */
@Composable
fun LineChart(
    values: List<Float>,
    minY: Float,
    maxY: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        drawLine(color, values, minY, maxY)
    }
}

/* ── Shared draw helper ───────────────────────────────────── */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLine(
    color: Color,
    values: List<Float>,
    minY: Float,
    maxY: Float
) {
    if (values.size < 2) return
    val range = (maxY - minY).coerceAtLeast(0.01f)
    val path = Path()
    values.forEachIndexed { i, v ->
        val x = size.width * i / (values.size - 1)
        val y = size.height * (1f - ((v - minY) / range).coerceIn(0f, 1f))
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // Draw end-dot
    val lastX = size.width
    val lastY = size.height * (1f - ((values.last() - minY) / range).coerceIn(0f, 1f))
    drawCircle(color, 4f, Offset(lastX, lastY))
}

/* ── Legend dot ─────────────────────────────────────────── */
@Composable
private fun LegendDot(color: Color, label: String) {
    Row {
        Canvas(Modifier.size(10.dp).padding(top = 4.dp)) { drawCircle(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

/* ================================================================
 *  RISK CHART CARD (colored by risk level)
 * ================================================================ */
@Composable
fun RiskChartCard(
    title: String,
    values: List<Float>,
    modifier: Modifier = Modifier,
    currentColor: Color = ElectricBlue
) {
    ChartCard(title, values, 0f, 100f, currentColor, "0–100", modifier)
}

