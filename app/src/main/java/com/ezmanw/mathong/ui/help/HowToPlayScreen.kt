package com.ezmanw.mathong.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.TileKind
import com.ezmanw.mathong.ui.common.BackTopBar
import com.ezmanw.mathong.ui.common.TilePreview
import com.ezmanw.mathong.ui.common.repository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit, onTutorial: () -> Unit = {}) {
    val settings by repository().settings.collectAsState(initial = Settings())
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BackTopBar("How to play", onBack, scroll) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.FilledTonalButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) {
                    Text("Play the interactive tutorial")
                }
                Block("Goal", "Clear every tile from the board by removing matching pairs.")
                Block(
                    "Free tiles",
                    "You can only pick a tile that is free: no tile may rest on top of it (even partially), and at least one of its long sides — left or right — must be open. Blocked tiles are dimmed when \"Dim blocked tiles\" is on.",
                )
                Block("Matching", "Tap two free tiles with the same face to remove them. Any two Flowers match each other, and any two Seasons match each other.")
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("These pairs match", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pair(TileKind(4), TileKind(4), settings)
                            Pair(TileKind(34), TileKind(36), settings)
                            Pair(TileKind(38), TileKind(41), settings)
                        }
                    }
                }
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("The tile set", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        SuitRow("Dots", (0..8).map { TileKind(it) }, settings)
                        SuitRow("Bamboo", (9..17).map { TileKind(it) }, settings)
                        SuitRow("Characters", (18..26).map { TileKind(it) }, settings)
                        SuitRow("Winds & Dragons", (27..33).map { TileKind(it) }, settings)
                        SuitRow("Flowers & Seasons", (34..41).map { TileKind(it) }, settings)
                    }
                }
                Block(
                    "Tools",
                    "• Hint highlights a pair you can match (smart hints look ahead to avoid dead ends).\n• Undo and Redo step through your moves.\n• Shuffle rearranges the remaining tiles into a solvable position.\n• Pinch to zoom, drag to pan when zoomed, double-tap to toggle zoom.",
                )
                Block(
                    "Scoring",
                    "Each pair is worth 10 points. Match quickly — within 4 seconds of the previous pair — to build a combo multiplier up to ×9. Hints cost 20 points and shuffles 50. Finishing fast earns a time bonus, and stars are awarded for clearing the board quickly without help.",
                )
                Block("Every deal is solvable", "Mathong only deals boards that can be cleared, and shuffles always keep the remaining tiles solvable. If you get stuck, it's because of the order you chose — undo and try a different path!")
            }
        }
    }
}

@Composable
private fun Block(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Pair(a: TileKind, b: TileKind, s: Settings) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TilePreview(a, s.tileTheme, 36.dp, showIndex = s.showIndices, simple = s.simpleFaces)
        TilePreview(b, s.tileTheme, 36.dp, showIndex = s.showIndices, simple = s.simpleFaces)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuitRow(name: String, kinds: List<TileKind>, s: Settings) {
    Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        kinds.forEach { TilePreview(it, s.tileTheme, 30.dp, showIndex = s.showIndices, simple = s.simpleFaces) }
    }
}
