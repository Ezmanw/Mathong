package com.ezmanw.mathong.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Stats
import com.ezmanw.mathong.game.LayoutRegistry
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.common.BackTopBar
import com.ezmanw.mathong.ui.common.repository
import com.ezmanw.mathong.ui.game.formatTime
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val repo = repository()
    val stats by repo.stats.collectAsState(initial = Stats())
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BackTopBar("Statistics", onBack, scroll) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Played", stats.gamesPlayed.toString(), Modifier.weight(1f))
                    StatTile("Won", stats.gamesWon.toString(), Modifier.weight(1f))
                    StatTile("Win rate", "${(stats.winRate * 100).toInt()}%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Streak", stats.currentStreak.toString(), Modifier.weight(1f))
                    StatTile("Best streak", stats.bestStreak.toString(), Modifier.weight(1f))
                    StatTile("Best combo", "×${stats.bestCombo}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Pairs matched", stats.totalMatches.toString(), Modifier.weight(1f))
                    StatTile("Time played", formatTime(stats.totalPlayMs), Modifier.weight(1f))
                    StatTile("Dailies", stats.dailyCompleted.size.toString(), Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Time Attack", "${stats.timeAttackWins}/${stats.timeAttackWins + stats.timeAttackLosses}", Modifier.weight(1f))
                    StatTile("Zen boards", stats.zenWins.toString(), Modifier.weight(1f))
                    StatTile("Awards", stats.achievements.size.toString(), Modifier.weight(1f))
                }

                Text("Pace", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                PaceChart(stats.recent)

                Text("Daily challenges", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                DailyCalendar(stats.dailyCompleted)

                Text("Layouts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Card {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Header("Layout", Modifier.weight(1.4f), TextAlign.Start)
                            Header("Won", Modifier.weight(0.8f))
                            Header("Best time", Modifier.weight(1f))
                            Header("Best score", Modifier.weight(1f))
                        }
                        HorizontalDivider()
                        LayoutRegistry.all().forEach { l ->
                            val ls = stats.perLayout[l.id]
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Cell(l.name, Modifier.weight(1.4f), TextAlign.Start)
                                Cell(if (ls == null) "–" else "${ls.won}/${ls.played}", Modifier.weight(0.8f))
                                Cell(if (ls == null || ls.bestTimeMs == 0L) "–" else formatTime(ls.bestTimeMs), Modifier.weight(1f))
                                Cell(if (ls == null || ls.bestScore == 0) "–" else ls.bestScore.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.DeleteOutline, null)
                    Text("  Reset statistics")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset statistics?") },
            text = { Text("All statistics, best times and achievements will be permanently erased.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; scope.launch { repo.resetStats() } }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(text: String, modifier: Modifier, align: TextAlign = TextAlign.End) =
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = align)

@Composable
private fun Cell(text: String, modifier: Modifier, align: TextAlign = TextAlign.End) =
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, textAlign = align)

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Last 5 weeks, Monday-first grid; filled cells are completed dailies. */
@Composable
private fun DailyCalendar(done: Set<String>) {
    val today = LocalDate.now()
    val start = today.minusDays((today.dayOfWeek.value - 1).toLong() + 28)
    val cs = MaterialTheme.colorScheme
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
            for (w in 0 until 5) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 0 until 7) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val isDone = date.toString() in done
                        val future = date.isAfter(today)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    when {
                                        isDone -> cs.primary
                                        future -> cs.surfaceContainerLowest.copy(alpha = 0f)
                                        else -> cs.surfaceContainerHighest
                                    },
                                    RoundedCornerShape(6.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isDone -> cs.onPrimary
                                    date == today -> cs.primary
                                    future -> cs.outline
                                    else -> cs.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
