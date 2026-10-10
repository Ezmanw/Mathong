package com.ezmanw.mathong.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ezmanw.mathong.data.Achievements
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WinDialog(
    state: GameUiState,
    onNext: () -> Unit,
    onLayouts: () -> Unit,
    onHome: () -> Unit,
    onAchievementsShown: () -> Unit,
) {
    val result = state.result ?: return
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Confetti(Modifier.fillMaxSize())
            Card(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (state.daily != null) "Daily complete!" else "Board cleared!",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1..3) AnimatedStar(filled = i <= result.stars, delayMs = i * 220)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Metric("Time", formatTime(result.timeMs))
                        Metric("Score", result.score.toString())
                        Metric("Best combo", "×${result.bestCombo}")
                    }
                    if (state.newBest) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                "New best time for ${state.layout?.name}!",
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    val helps = buildList {
                        if (result.hintsUsed > 0) add("${result.hintsUsed} hint${if (result.hintsUsed > 1) "s" else ""}")
                        if (result.shufflesUsed > 0) add("${result.shufflesUsed} shuffle${if (result.shufflesUsed > 1) "s" else ""}")
                        if (result.undosUsed > 0) add("${result.undosUsed} undo${if (result.undosUsed > 1) "s" else ""}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (helps.isEmpty()) "No help used. Impressive!" else "Used " + helps.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (state.newAchievements.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Achievements unlocked", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        state.newAchievements.forEach { id ->
                            val a = Achievements.byId(id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFFB300))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(a.title, style = MaterialTheme.typography.titleSmall)
                                    Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onAchievementsShown(); onNext() }, Modifier.fillMaxWidth()) {
                        Text(if (state.daily != null) "Play a regular game" else "Next deal")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onAchievementsShown(); onLayouts() }, Modifier.fillMaxWidth()) {
                        Text("Choose layout")
                    }
                    TextButton(onClick = { onAchievementsShown(); onHome() }) { Text("Home") }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnimatedStar(filled: Boolean, delayMs: Int) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Icon(
        if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
        null,
        Modifier
            .size(52.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        tint = if (filled) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
    )
}

private class Piece(val x: Float, val speed: Float, val drift: Float, val rot: Float, val color: Color, val size: Float, val phase: Float)

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(System.nanoTime())
        val palette = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFFB300), Color(0xFF8E24AA), Color(0xFF00ACC1))
        List(120) {
            Piece(r.nextFloat(), 0.25f + r.nextFloat() * 0.5f, r.nextFloat() * 2 - 1, r.nextFloat() * 720, palette.random(r), 6f + r.nextFloat() * 8f, r.nextFloat())
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(4500, easing = LinearEasing)) }
    Canvas(modifier) {
        val time = t.value * 4.5f
        for (p in pieces) {
            val y = (-0.1f - p.phase * 0.6f + time * p.speed) * size.height
            if (y < -20 || y > size.height + 20) continue
            val x = (p.x + sin((time + p.phase) * 3f) * 0.03f * p.drift) * size.width
            rotate(p.rot * time, Offset(x, y)) {
                drawRect(p.color, Offset(x - p.size / 2, y - p.size / 4), Size(p.size * density / 2, p.size * density / 4))
            }
        }
    }
}
