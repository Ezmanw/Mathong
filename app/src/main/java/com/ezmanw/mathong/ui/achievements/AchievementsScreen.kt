package com.ezmanw.mathong.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Achievements
import com.ezmanw.mathong.data.Stats
import com.ezmanw.mathong.ui.common.BackTopBar
import com.ezmanw.mathong.ui.common.repository
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val stats by repository().stats.collectAsState(initial = Stats())
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val unlocked = stats.achievements
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BackTopBar("Achievements", onBack, scroll) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text("${unlocked.size} of ${Achievements.all.size} unlocked", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    LinearProgressIndicator(
                        progress = { unlocked.size.toFloat() / Achievements.all.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            items(Achievements.all.sortedBy { if (it.id in unlocked) 0 else 1 }, key = { it.id }) { a ->
                val at = unlocked[a.id]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (at != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (at != null) Icons.Filled.EmojiEvents else Icons.Filled.Lock, null,
                            Modifier.size(32.dp),
                            tint = if (at != null) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = MaterialTheme.typography.titleMedium)
                            Text(a.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (at != null) {
                                Text(
                                    "Unlocked " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(at)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
