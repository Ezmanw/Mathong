package com.ezmanw.mathong.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.RecentGame
import com.ezmanw.mathong.game.LayoutRegistry
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.game.formatTime
import java.text.DateFormat
import java.util.Date

private fun RecentGame.secondsPerPair() = timeMs / 1000f / (tiles / 2f).coerceAtLeast(1f)

/** Bar chart of solving pace (seconds per pair) over the most recent wins. Tap a bar for details. */
@Composable
fun PaceChart(recent: List<RecentGame>) {
    val cs = MaterialTheme.colorScheme
    var selected by remember(recent) { mutableStateOf(recent.lastIndex) }
    Card {
        Column(Modifier.padding(16.dp)) {
            if (recent.isEmpty()) {
                Text("Win a game to start tracking your pace.", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val paces = recent.map { it.secondsPerPair() }
            val avg = paces.average().toFloat()
            val g = recent.getOrNull(selected)
            Row {
                Column(Modifier.weight(1f)) {
                    Text("%.1fs".format(avg), style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
                    Text("average per pair · last ${recent.size} wins", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
                if (g != null) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text("${LayoutRegistry.byId(g.layoutId).name}${if (g.daily) " (daily)" else ""}", style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
                        Text("${formatTime(g.timeMs)} · %.1fs/pair · ${g.score} pts".format(g.secondsPerPair()), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(g.at)), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val maxPace = (paces.max() * 1.1f).coerceAtLeast(1f)
            val slots = 30
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(recent) {
                        detectTapGestures { pos ->
                            val slotW = size.width / slots.toFloat()
                            val idx = (pos.x / slotW).toInt()
                            if (idx in recent.indices) selected = idx
                        }
                    },
            ) {
                val slotW = size.width / slots
                val barW = (slotW - 2.dp.toPx()).coerceAtMost(10.dp.toPx())
                val r = 4.dp.toPx().coerceAtMost(barW / 2)
                // Recessive average guide.
                val avgY = size.height * (1 - avg / maxPace)
                drawLine(
                    cs.outlineVariant, Offset(0f, avgY), Offset(size.width, avgY),
                    strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                drawLine(cs.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                paces.forEachIndexed { i, p ->
                    val h = (size.height * p / maxPace).coerceAtLeast(2.dp.toPx())
                    val x = i * slotW + (slotW - barW) / 2
                    val color = if (i == selected) cs.primary else cs.primary.copy(alpha = 0.45f)
                    // Bar with rounded top, square at the baseline.
                    val path = Path().apply {
                        val top = size.height - h
                        moveTo(x, size.height)
                        lineTo(x, top + r)
                        quadraticBezierTo(x, top, x + r, top)
                        lineTo(x + barW - r, top)
                        quadraticBezierTo(x + barW, top, x + barW, top + r)
                        lineTo(x + barW, size.height)
                        close()
                    }
                    drawPath(path, color)
                    if (i == selected) {
                        drawRoundRect(cs.onSurface, Offset(x - 2.dp.toPx(), size.height - h - 2.dp.toPx()), Size(barW + 4.dp.toPx(), h + 2.dp.toPx()), CornerRadius(r + 2.dp.toPx()), style = Stroke(1.dp.toPx()))
                    }
                }
            }
            Text("Lower is faster · dashed line is your average", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
