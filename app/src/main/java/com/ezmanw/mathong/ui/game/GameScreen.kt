package com.ezmanw.mathong.ui.game

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezmanw.mathong.data.BoardBackground
import com.ezmanw.mathong.data.GameMode
import kotlinx.coroutines.delay

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun boardBrush(bg: BoardBackground): Brush {
    val cs = MaterialTheme.colorScheme
    return when (bg) {
        BoardBackground.THEME -> Brush.verticalGradient(listOf(cs.surfaceContainerLow, cs.surfaceContainer))
        BoardBackground.FELT -> Brush.radialGradient(listOf(Color(0xFF2F7A52), Color(0xFF14432B)))
        BoardBackground.WOOD -> Brush.linearGradient(listOf(Color(0xFF6B4226), Color(0xFF3E2414), Color(0xFF5A3720)))
        BoardBackground.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF1A2340), Color(0xFF0A0F1F)))
        BoardBackground.SAKURA -> Brush.verticalGradient(listOf(Color(0xFFFFE4EC), Color(0xFFF8BBD0)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    onChooseLayout: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    vm: GameViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val view = LocalView.current
    val currentSettings by rememberUpdatedState(settings)
    val currentState by rememberUpdatedState(state)
    val snackbar = remember { SnackbarHostState() }
    var comboText by remember { mutableStateOf<String?>(null) }
    var comboKey by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.setVisible(false); vm.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.setVisible(true) }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is GameEvent.Match -> {
                    if (currentSettings.haptics) {
                        view.performHapticFeedback(
                            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                        )
                    }
                    comboText = when (currentState.mode) {
                        GameMode.ZEN -> null
                        GameMode.TIME_ATTACK -> "+${(GameViewModel.TIME_PER_MATCH + (e.combo - 1) * 1000) / 1000}s" + if (e.combo > 1) "  ×${e.combo}" else ""
                        GameMode.CLASSIC -> if (e.combo > 1) "Combo ×${e.combo}  +${e.points}" else "+${e.points}"
                    }
                    comboKey++
                }
                GameEvent.Invalid -> if (currentSettings.haptics) {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                    )
                }
                GameEvent.Shuffled -> snackbar.showSnackbar("Tiles shuffled")
            }
        }
    }

    val title = when {
        state.daily != null -> "Daily Challenge"
        state.mode != GameMode.CLASSIC -> "${state.layout?.name ?: ""} · ${state.mode.label}"
        else -> state.layout?.name ?: ""
    }
    val paused = state.status == GameStatus.PAUSED
    val menu: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("New game") }, onClick = { menuOpen = false; confirmNew = true })
                DropdownMenuItem(text = { Text("Restart this deal") }, onClick = { menuOpen = false; vm.restart() })
                DropdownMenuItem(text = { Text("Change layout") }, onClick = { menuOpen = false; onChooseLayout() })
                DropdownMenuItem(text = { Text("How to play") }, onClick = { menuOpen = false; vm.pause(); onHelp() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; vm.pause(); onSettings() })
            }
        }
    }
    val pauseButton: @Composable () -> Unit = {
        IconButton(onClick = { if (paused) vm.resume() else vm.pause() }) {
            Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (paused) "Resume" else "Pause")
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight && maxHeight < 600.dp
        Scaffold(
            topBar = {
                if (!landscape) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(title, style = MaterialTheme.typography.titleLarge)
                                if (state.daily != null) {
                                    Text(
                                        "${state.daily} · ${state.layout?.name ?: ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        },
                        actions = { pauseButton(); menu() },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            },
            bottomBar = {
                if (!landscape) {
                    BottomAppBar(
                        actions = {
                            ToolButton(Icons.AutoMirrored.Filled.Undo, "Undo", state.history.isNotEmpty()) { vm.undo() }
                            ToolButton(Icons.AutoMirrored.Filled.Redo, "Redo", state.redo.isNotEmpty()) { vm.redo() }
                            ToolButton(Icons.Filled.Shuffle, "Shuffle", state.status != GameStatus.WON) { vm.shuffle() }
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { vm.hint() }) { Icon(Icons.Filled.Lightbulb, "Hint") }
                        },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Row(Modifier.padding(padding).fillMaxSize()) {
                if (landscape) {
                    NavigationRail(
                        header = {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                            pauseButton()
                            menu()
                        },
                    ) {
                        Spacer(Modifier.weight(1f))
                        ToolButton(Icons.AutoMirrored.Filled.Undo, "Undo", state.history.isNotEmpty()) { vm.undo() }
                        ToolButton(Icons.AutoMirrored.Filled.Redo, "Redo", state.redo.isNotEmpty()) { vm.redo() }
                        ToolButton(Icons.Filled.Shuffle, "Shuffle", state.status != GameStatus.WON) { vm.shuffle() }
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(onClick = { vm.hint() }, Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Filled.Lightbulb, "Hint")
                        }
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(boardBrush(settings.background))
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Hud(
                            state, settings.showTimer,
                            onDarkBg = settings.background in setOf(BoardBackground.FELT, BoardBackground.WOOD, BoardBackground.NIGHT),
                            title = if (landscape) title else null,
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (!paused) {
                                state.board?.let { b ->
                                    BoardView(
                                        board = b,
                                        selected = state.selected,
                                        hint = state.hint,
                                        shakeTile = state.shakeTile,
                                        shakeKey = state.shakeKey,
                                        settings = settings,
                                        onTap = vm::tapTile,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            ComboPopup(comboText, comboKey, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                            val canFinish = remember(state.board) { vm.canAutoFinish(state.board) }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = canFinish && state.status == GameStatus.PLAYING,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut(),
                            ) {
                                androidx.compose.material3.ExtendedFloatingActionButton(
                                    onClick = { vm.autoFinish() },
                                    icon = { Icon(Icons.Filled.DoneAll, null) },
                                    text = { Text("Finish") },
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                )
                            }
                        }
                    }
                    if (paused) PausedOverlay { vm.resume() }
                    if (state.status == GameStatus.LOADING) {
                        Text("Dealing…", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (state.status == GameStatus.STUCK) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("No more moves") },
            text = { Text("There are no matching free pairs left. Shuffle the remaining tiles, undo your last move or start over.") },
            confirmButton = { TextButton(onClick = { vm.shuffle() }) { Text("Shuffle") } },
            dismissButton = {
                Row {
                    if (state.history.isNotEmpty()) TextButton(onClick = { vm.undo() }) { Text("Undo") }
                    TextButton(onClick = { vm.newGame() }) { Text("New game") }
                }
            },
        )
    }

    if (state.status == GameStatus.TIME_UP) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Outlined.Timer, null) },
            title = { Text("Time's up!") },
            text = {
                val left = (state.board?.remaining ?: 0) / 2
                Text("You ran out of time with $left pair${if (left == 1) "" else "s"} left. Match quickly to earn more time — combos add bonus seconds.")
            },
            confirmButton = { TextButton(onClick = { vm.restart() }) { Text("Try again") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.newGame() }) { Text("New deal") }
                    TextButton(onClick = onBack) { Text("Home") }
                }
            },
        )
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Start a new game?") },
            text = { Text("Your current progress on this deal will be lost.") },
            confirmButton = { TextButton(onClick = { confirmNew = false; vm.newGame() }) { Text("New game") } },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Cancel") } },
        )
    }

    if (state.status == GameStatus.WON && state.result != null) {
        WinDialog(
            state = state,
            onNext = { vm.newGame() },
            onLayouts = onChooseLayout,
            onHome = onBack,
            onAchievementsShown = { vm.consumeAchievements() },
        )
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, label) }
}

@Composable
private fun Hud(state: GameUiState, showTimer: Boolean, onDarkBg: Boolean, title: String? = null) {
    val fg = if (onDarkBg) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
        }
        when (state.mode) {
            GameMode.TIME_ATTACK -> {
                val low = state.timeLeftMs < 10_000
                HudChip(
                    Icons.Outlined.Timer, formatTime(state.timeLeftMs + 999),
                    if (low) MaterialTheme.colorScheme.error else fg,
                )
                HudChip(null, "Score ${state.score}", fg)
            }
            GameMode.ZEN -> Unit
            GameMode.CLASSIC -> {
                if (showTimer) HudChip(Icons.Outlined.Timer, formatTime(state.elapsedMs), fg)
                HudChip(null, "Score ${state.score}", fg)
            }
        }
        Spacer(Modifier.weight(1f))
        val board = state.board
        val remaining = remember(board) { board?.remaining ?: 0 }
        val moves = remember(board) { board?.availableMatches()?.size ?: 0 }
        HudChip(null, "${remaining / 2} pairs · $moves moves", fg)
    }
}

@Composable
private fun HudChip(icon: androidx.compose.ui.graphics.vector.ImageVector?, text: String, fg: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = fg.copy(alpha = 0.1f),
        contentColor = fg,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun ComboPopup(text: String?, key: Int, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (text == null) return@LaunchedEffect
        visible = true
        lift.snapTo(0f)
        lift.animateTo(1f, tween(900))
        delay(100)
        visible = false
    }
    AnimatedVisibility(visible, modifier, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shadowElevation = 4.dp,
            modifier = Modifier.graphicsLayer { translationY = -lift.value * 24.dp.toPx() },
        ) {
            Text(text ?: "", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onResume),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Pause, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Paused", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text("The board is hidden while paused", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onResume) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Resume")
            }
        }
    }
}
