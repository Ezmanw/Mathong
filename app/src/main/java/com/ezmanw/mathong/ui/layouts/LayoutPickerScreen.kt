package com.ezmanw.mathong.ui.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Stats
import com.ezmanw.mathong.game.Difficulty
import com.ezmanw.mathong.game.Layout
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.common.BackTopBar
import com.ezmanw.mathong.ui.common.LayoutThumbnail
import com.ezmanw.mathong.ui.common.repository
import com.ezmanw.mathong.ui.game.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutPickerScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val stats by repository().stats.collectAsState(initial = Stats())
    var filter by rememberSaveable { mutableStateOf<Difficulty?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BackTopBar("Choose a layout", onBack, scroll) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = filter == d,
                            onClick = { filter = if (filter == d) null else d },
                            label = { Text(d.label()) },
                        )
                    }
                }
            }
            items(Layouts.all.filter { filter == null || it.difficulty == filter }, key = { it.id }) { l ->
                LayoutCard(l, stats, onClick = { onPick(l.id) })
            }
        }
    }
}

fun Difficulty.label() = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun Difficulty.color(): Color = when (this) {
    Difficulty.EASY -> Color(0xFF43A047)
    Difficulty.MEDIUM -> Color(0xFF1E88E5)
    Difficulty.HARD -> Color(0xFFFB8C00)
    Difficulty.EXPERT -> Color(0xFFE53935)
}

@Composable
private fun LayoutCard(layout: Layout, stats: Stats, onClick: () -> Unit) {
    val ls = stats.perLayout[layout.id]
    Card(onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            LayoutThumbnail(layout, Modifier.fillMaxWidth().aspectRatio(1.2f))
            Spacer(Modifier.height(8.dp))
            Text(layout.name, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    layout.difficulty.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = layout.difficulty.color(),
                )
                Text(
                    " · ${layout.size} tiles · ${layout.maxZ + 1} layers",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..3) {
                    Icon(
                        Icons.Filled.Star, null, Modifier.size(16.dp),
                        tint = if ((ls?.bestStars ?: 0) >= i) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (ls != null && ls.bestTimeMs > 0) "Best ${formatTime(ls.bestTimeMs)}" else "Unbeaten",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
