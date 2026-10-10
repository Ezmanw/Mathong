package com.ezmanw.mathong.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.data.Stats
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.game.TileKind
import com.ezmanw.mathong.ui.common.TilePreview
import com.ezmanw.mathong.ui.common.repository
import com.ezmanw.mathong.ui.game.GameViewModel
import com.ezmanw.mathong.ui.game.formatTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onQuickPlay: () -> Unit,
    onDaily: () -> Unit,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    val repo = repository()
    val saved by repo.savedGame.collectAsState(initial = null)
    val stats by repo.stats.collectAsState(initial = Stats())
    val settings by repo.settings.collectAsState(initial = Settings())
    val seenTutorial by repo.seenTutorial.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header(settings)

                if (!seenTutorial) {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("New to Mahjong Solitaire?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Match pairs of identical free tiles to clear the board. A tile is free when nothing sits on top of it and its left or right side is open.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { repo.setSeenTutorial() } }) { Text("Got it") }
                                TextButton(onClick = { scope.launch { repo.setSeenTutorial() }; onHelp() }) { Text("Learn more") }
                            }
                        }
                    }
                }

                saved?.let { g ->
                    val layout = Layouts.byId(g.layoutId)
                    val left = g.present.count { it }
                    ElevatedCard(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Continue", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    (if (g.daily != null) "Daily · " else "") + "${layout.name} · ${left / 2} pairs left · ${formatTime(g.elapsedMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { 1f - left.toFloat() / layout.size },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigAction("New game", "Pick from ${Layouts.all.size} layouts", Icons.Filled.GridView, Modifier.weight(1f), onNewGame)
                    BigAction("Quick play", "Random layout", Icons.Filled.Shuffle, Modifier.weight(1f), onQuickPlay)
                }

                DailyCard(stats, onDaily)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallAction("Stats", Icons.Filled.Insights, Modifier.weight(1f), onStats)
                    SmallAction("Awards", Icons.Filled.EmojiEvents, Modifier.weight(1f), onAchievements)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallAction("Settings", Icons.Filled.Settings, Modifier.weight(1f), onSettings)
                    SmallAction("How to play", Icons.AutoMirrored.Filled.HelpOutline, Modifier.weight(1f), onHelp)
                }

                if (stats.gamesWon > 0) {
                    Text(
                        "${stats.gamesWon} wins · best streak ${stats.bestStreak} · ${stats.achievements.size} awards",
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun Header(settings: Settings) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "b",
    )
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), verticalAlignment = Alignment.Bottom) {
            val tiles = listOf(TileKind(31), TileKind(0), TileKind(32), TileKind(9), TileKind(27))
            tiles.forEachIndexed { i, k ->
                TilePreview(
                    k, settings.tileTheme, 46.dp,
                    Modifier.graphicsLayer {
                        rotationZ = (i - 2) * 6f
                        translationY = ((if (i % 2 == 0) bob else -bob) * 4f - kotlin.math.abs(i - 2) * -6f).dp.toPx()
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Mathong", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("Mahjong Solitaire", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BigAction(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun SmallAction(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp)) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(title)
    }
}

@Composable
private fun DailyCard(stats: Stats, onDaily: () -> Unit) {
    val today = LocalDate.now()
    val (layout, _) = GameViewModel.dailyFor(today)
    val done = today.toString() in stats.dailyCompleted
    var streak = 0
    var d = if (done) today else today.minusDays(1)
    while (d.toString() in stats.dailyCompleted) { streak++; d = d.minusDays(1) }
    Card(
        onClick = onDaily,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.CalendarMonth, null,
                Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily Challenge", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) + " · ${layout.name}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    when {
                        done -> "Completed! Streak: $streak day${if (streak == 1) "" else "s"}"
                        streak > 0 -> "Keep your $streak day streak alive"
                        else -> "Same deal for everyone, every day"
                    },
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
