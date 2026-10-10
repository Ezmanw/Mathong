package com.ezmanw.mathong.ui.help

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.MathongApp
import com.ezmanw.mathong.audio.Sfx
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.Board
import com.ezmanw.mathong.game.Difficulty
import com.ezmanw.mathong.game.layout
import com.ezmanw.mathong.ui.common.repository
import com.ezmanw.mathong.ui.game.BoardView
import com.ezmanw.mathong.ui.game.boardBrush
import kotlinx.coroutines.launch

private val tutorialLayout = layout("tutorial", "Tutorial", Difficulty.EASY) {
    row(0.0, 0.0, 6, 0)
    tile(1.5, 0.0, 1)
    tile(3.5, 0.0, 1)
}

// Slots: 0..5 bottom row, 6 and 7 on top.
private val tutorialKinds = intArrayOf(27, 0, 1, 1, 0, 27, 31, 31)

private fun step(board: Board): Pair<String, Pair<Int, Int>?> = when {
    board.present[6] -> "A tile is free when nothing sits on top of it and its left or right side is open.\n\nThe two Red Dragons on top are free — tap both to match them." to (6 to 7)
    board.present[0] -> "Nice! Tiles at the ends of a row are free too.\n\nMatch the two East Winds at each end." to (0 to 5)
    board.present[1] -> "Tiles in the middle of a row are blocked on both sides.\n\nOnly the new ends are free: match the 1 of Dots." to (1 to 4)
    board.present[2] -> "Last pair — clear the board!" to (2 to 3)
    else -> "You've got it! Every deal in Mathong can be solved — choose your order wisely, and use hints or undo if you get stuck." to null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as MathongApp
    val repo = repository()
    val settings by repo.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    var board by remember { mutableStateOf(Board(tutorialLayout, tutorialKinds, BooleanArray(8) { true })) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var shake by remember { mutableStateOf<Int?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var nudge by remember { mutableStateOf<String?>(null) }
    val (text, hint) = step(board)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorial") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LinearProgressIndicator(
                progress = { 1f - board.remaining / 8f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                AnimatedContent(text, label = "step") { t ->
                    Column(Modifier.padding(16.dp)) {
                        Text(t, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        nudge?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().background(boardBrush(settings.background))) {
                if (!board.isCleared) {
                    BoardView(
                        board = board,
                        selected = selected,
                        hint = hint,
                        shakeTile = shake,
                        shakeKey = shakeKey,
                        settings = settings.copy(highlightFree = true),
                        onTap = { i ->
                            if (!board.isFree(i)) {
                                app.sounds.play(Sfx.ERROR, 0.6f)
                                shake = i; shakeKey++
                                nudge = "That tile is blocked — look for a free one."
                                return@BoardView
                            }
                            nudge = null
                            val sel = selected
                            when {
                                sel == null || sel == i -> { selected = if (sel == i) null else i; app.sounds.play(Sfx.SELECT) }
                                board.canMatch(sel, i) -> {
                                    app.sounds.play(Sfx.MATCH)
                                    board = board.remove(sel, i)
                                    selected = null
                                    if (board.isCleared) app.sounds.play(Sfx.WIN)
                                }
                                else -> {
                                    selected = i
                                    nudge = "Those two don't match — pick identical tiles."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (board.isCleared) {
                Button(
                    onClick = { scope.launch { repo.setSeenTutorial() }; onDone() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("Start playing") }
            }
        }
    }
}
