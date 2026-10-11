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
import androidx.compose.runtime.DisposableEffect
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
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.Direction
import com.ezmanw.mathong.game.canAutoFinish
import com.ezmanw.mathong.game.moveCursor
import androidx.compose.foundation.focusable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.PlainTooltip
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

/** Everything the game UI can ask the game to do. */
class GameActions(
    val tap: (Int) -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val shuffle: () -> Unit = {},
    val hint: () -> Unit = {},
    val pause: () -> Unit = {},
    val resume: () -> Unit = {},
    val newGame: () -> Unit = {},
    val restart: () -> Unit = {},
    val autoFinish: () -> Unit = {},
    val consumeAchievements: () -> Unit = {},
    val back: () -> Unit = {},
    val chooseLayout: () -> Unit = {},
    val help: () -> Unit = {},
    val settings: () -> Unit = {},
)

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

    val back by rememberUpdatedState(onBack)
    val chooseLayout by rememberUpdatedState(onChooseLayout)
    val help by rememberUpdatedState(onHelp)
    val openSettings by rememberUpdatedState(onSettings)
    // Stable callbacks so timer ticks don't recompose and redraw the board.
    val actions = remember(vm) {
        GameActions(
            tap = vm::tapTile, undo = vm::undo, redo = vm::redo, shuffle = { vm.shuffle() }, hint = vm::hint,
            pause = { vm.pause() }, resume = { vm.resume() }, newGame = { vm.newGame() }, restart = vm::restart,
            autoFinish = vm::autoFinish, consumeAchievements = { vm.consumeAchievements() },
            back = { back() }, chooseLayout = { chooseLayout() },
            help = { vm.pause(); help() }, settings = { vm.pause(); openSettings() },
        )
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
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

    GameContent(state, settings, actions, snackbar, comboText, comboKey)
}

/** Window shape the game UI adapts to. */
enum class GameLayoutMode { PHONE, LANDSCAPE_RAIL, WIDE }

fun gameLayoutMode(width: Dp, height: Dp): GameLayoutMode = when {
    width >= 840.dp && height >= 480.dp -> GameLayoutMode.WIDE
    width > height && height < 600.dp -> GameLayoutMode.LANDSCAPE_RAIL
    else -> GameLayoutMode.PHONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    state: GameUiState,
    settings: Settings,
    actions: GameActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    comboText: String? = null,
    comboKey: Int = 0,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    var inspect by remember { mutableStateOf<Int?>(null) }
    var cursor by remember { mutableStateOf<Int?>(null) }
    val zoom = rememberBoardZoom()
    val focus = remember { FocusRequester() }
    val onTileTap = remember(actions) { { i: Int -> inspect = null; cursor = null; actions.tap(i) } }
    val onTileLongPress = remember { { i: Int -> inspect = i } }
    val canFinish = remember(state.board) { state.board?.canAutoFinish() == true }

    val title = when {
        state.daily != null -> "Daily Challenge"
        state.mode != GameMode.CLASSIC -> "${state.layout?.name ?: ""} · ${state.mode.label}"
        else -> state.layout?.name ?: ""
    }
    val paused = state.status == GameStatus.PAUSED
    val playing = state.status == GameStatus.PLAYING || paused

    // Keep keyboard focus on the game whenever no dialog is open.
    LaunchedEffect(state.status, confirmNew, shortcutsOpen) {
        if (!confirmNew && !shortcutsOpen) runCatching { focus.requestFocus() }
    }

    fun onKey(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        val ctrl = e.isCtrlPressed || e.isMetaPressed
        val board = state.board
        when {
            ctrl && e.key == Key.Z && !e.isShiftPressed -> actions.undo()
            ctrl && (e.key == Key.Y || (e.key == Key.Z && e.isShiftPressed)) -> actions.redo()
            ctrl -> return false
            e.key == Key.Slash && e.isShiftPressed || e.key == Key.F1 -> shortcutsOpen = true
            !playing -> return false
            e.key == Key.H -> actions.hint()
            e.key == Key.S -> actions.shuffle()
            e.key == Key.Spacebar || e.key == Key.P -> if (paused) actions.resume() else actions.pause()
            e.key == Key.N -> confirmNew = true
            e.key == Key.F && canFinish -> actions.autoFinish()
            e.key == Key.Escape -> {
                when {
                    inspect != null -> inspect = null
                    state.selected != null -> actions.tap(state.selected)
                    cursor != null -> cursor = null
                    else -> return false
                }
            }
            e.key == Key.DirectionLeft || e.key == Key.DirectionRight || e.key == Key.DirectionUp || e.key == Key.DirectionDown -> {
                if (board == null || paused) return false
                val dir = when (e.key) {
                    Key.DirectionLeft -> Direction.LEFT
                    Key.DirectionRight -> Direction.RIGHT
                    Key.DirectionUp -> Direction.UP
                    else -> Direction.DOWN
                }
                cursor = board.moveCursor(cursor, dir)
            }
            e.key == Key.Enter || e.key == Key.NumPadEnter -> cursor?.let(actions.tap) ?: return false
            e.key == Key.Equals || e.key == Key.Plus || e.key == Key.NumPadAdd -> zoom.apply(1.25f)
            e.key == Key.Minus || e.key == Key.NumPadSubtract -> zoom.apply(0.8f)
            e.key == Key.Zero || e.key == Key.NumPad0 -> zoom.reset()
            else -> return false
        }
        return true
    }

    val menu: @Composable () -> Unit = {
        Box {
            TipIconButton(Icons.Filled.MoreVert, "More") { menuOpen = true }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("New game") }, onClick = { menuOpen = false; confirmNew = true }, trailingIcon = { ShortcutLabel("N") })
                DropdownMenuItem(text = { Text("Restart this deal") }, onClick = { menuOpen = false; actions.restart() })
                DropdownMenuItem(text = { Text("Change layout") }, onClick = { menuOpen = false; actions.chooseLayout() })
                DropdownMenuItem(text = { Text("Keyboard shortcuts") }, onClick = { menuOpen = false; shortcutsOpen = true }, trailingIcon = { ShortcutLabel("?") })
                DropdownMenuItem(text = { Text("How to play") }, onClick = { menuOpen = false; actions.help() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; actions.settings() })
            }
        }
    }
    val pauseButton: @Composable () -> Unit = {
        TipIconButton(
            if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            if (paused) "Resume" else "Pause", "Space",
        ) { if (paused) actions.resume() else actions.pause() }
    }
    val backButton: @Composable () -> Unit = {
        TipIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { actions.back() }
    }
    val onDarkBg = settings.background in setOf(BoardBackground.FELT, BoardBackground.WOOD, BoardBackground.NIGHT)

    @Composable
    fun BoardArea(modifier: Modifier, hud: Boolean, hudTitle: String?, finishFab: Boolean) {
        Box(modifier.background(boardBrush(settings.background))) {
            Column(Modifier.fillMaxSize()) {
                if (hud) Hud(state, settings.showTimer, onDarkBg = onDarkBg, title = hudTitle)
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
                                dealId = state.dealId,
                                onTap = onTileTap,
                                onLongPress = onTileLongPress,
                                zoomState = zoom,
                                cursor = cursor,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    ComboPopup(comboText, comboKey, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                    state.board?.let { b ->
                        TileInspector(b, inspect, settings, onDismiss = { inspect = null }, Modifier.align(Alignment.BottomStart).padding(12.dp))
                    }
                    if (finishFab) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = canFinish && state.status == GameStatus.PLAYING,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                        ) {
                            androidx.compose.material3.ExtendedFloatingActionButton(
                                onClick = actions.autoFinish,
                                icon = { Icon(Icons.Filled.DoneAll, null) },
                                text = { Text("Finish") },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            )
                        }
                    }
                }
            }
            if (paused) PausedOverlay(actions.resume)
            if (state.status == GameStatus.LOADING) {
                Text("Dealing…", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent(::onKey)
            .focusRequester(focus)
            .focusable(),
    ) {
        val mode = gameLayoutMode(maxWidth, maxHeight)
        Scaffold(
            topBar = {
                if (mode == GameLayoutMode.PHONE) {
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
                        navigationIcon = { backButton() },
                        actions = { pauseButton(); menu() },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            },
            bottomBar = {
                if (mode == GameLayoutMode.PHONE) {
                    BottomAppBar(
                        actions = {
                            TipIconButton(Icons.AutoMirrored.Filled.Undo, "Undo", "Ctrl+Z", state.history.isNotEmpty(), actions.undo)
                            TipIconButton(Icons.AutoMirrored.Filled.Redo, "Redo", "Ctrl+Y", state.redo.isNotEmpty(), actions.redo)
                            TipIconButton(Icons.Filled.Shuffle, "Shuffle", "S", state.status != GameStatus.WON, actions.shuffle)
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = actions.hint) { Icon(Icons.Filled.Lightbulb, "Hint") }
                        },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Row(Modifier.padding(padding).fillMaxSize()) {
                when (mode) {
                    GameLayoutMode.LANDSCAPE_RAIL -> {
                        NavigationRail(
                            header = {
                                backButton()
                                pauseButton()
                                menu()
                            },
                        ) {
                            Spacer(Modifier.weight(1f))
                            TipIconButton(Icons.AutoMirrored.Filled.Undo, "Undo", "Ctrl+Z", state.history.isNotEmpty(), actions.undo)
                            TipIconButton(Icons.AutoMirrored.Filled.Redo, "Redo", "Ctrl+Y", state.redo.isNotEmpty(), actions.redo)
                            TipIconButton(Icons.Filled.Shuffle, "Shuffle", "S", state.status != GameStatus.WON, actions.shuffle)
                            Spacer(Modifier.height(8.dp))
                            FloatingActionButton(onClick = actions.hint, Modifier.padding(bottom = 12.dp)) {
                                Icon(Icons.Filled.Lightbulb, "Hint")
                            }
                        }
                        BoardArea(Modifier.weight(1f).fillMaxHeight(), hud = true, hudTitle = title, finishFab = true)
                    }
                    GameLayoutMode.WIDE -> {
                        BoardArea(Modifier.weight(1f).fillMaxHeight(), hud = false, hudTitle = null, finishFab = false)
                        GameSidePanel(
                            state = state,
                            settings = settings,
                            title = title,
                            canFinish = canFinish,
                            actions = actions,
                            topBar = { backButton(); Spacer(Modifier.weight(1f)); pauseButton(); menu() },
                            onShortcuts = { shortcutsOpen = true },
                        )
                    }
                    GameLayoutMode.PHONE -> BoardArea(Modifier.weight(1f).fillMaxHeight(), hud = true, hudTitle = null, finishFab = true)
                }
            }
        }
    }

    if (state.status == GameStatus.STUCK) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("No more moves") },
            text = { Text("There are no matching free pairs left. Shuffle the remaining tiles, undo your last move or start over.") },
            confirmButton = { TextButton(onClick = actions.shuffle) { Text("Shuffle") } },
            dismissButton = {
                Row {
                    if (state.history.isNotEmpty()) TextButton(onClick = actions.undo) { Text("Undo") }
                    TextButton(onClick = actions.newGame) { Text("New game") }
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
            confirmButton = { TextButton(onClick = actions.restart) { Text("Try again") } },
            dismissButton = {
                Row {
                    TextButton(onClick = actions.newGame) { Text("New deal") }
                    TextButton(onClick = actions.back) { Text("Home") }
                }
            },
        )
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Start a new game?") },
            text = { Text("Your current progress on this deal will be lost.") },
            confirmButton = { TextButton(onClick = { confirmNew = false; actions.newGame() }) { Text("New game") } },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Cancel") } },
        )
    }

    if (shortcutsOpen) ShortcutsDialog { shortcutsOpen = false }

    if (state.status == GameStatus.WON && state.result != null) {
        WinDialog(
            state = state,
            onNext = actions.newGame,
            onLayouts = actions.chooseLayout,
            onHome = actions.back,
            onAchievementsShown = actions.consumeAchievements,
        )
    }
}

/** Icon button with a Material tooltip (shown on hover with a mouse, long-press on touch). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    shortcut: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    androidx.compose.material3.TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(if (shortcut != null) "$label ($shortcut)" else label) } },
        state = androidx.compose.material3.rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled) { Icon(icon, label) }
    }
}

@Composable
private fun ShortcutLabel(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            text,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Side panel for wide windows (tablets, foldables, ChromeOS and desktop). */
@Composable
private fun GameSidePanel(
    state: GameUiState,
    settings: Settings,
    title: String,
    canFinish: Boolean,
    actions: GameActions,
    topBar: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    onShortcuts: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val board = state.board
    val remaining = remember(board) { board?.remaining ?: 0 }
    val moves = remember(board) { board?.availableMatches()?.size ?: 0 }
    val total = board?.layout?.size ?: 1
    Surface(color = cs.surfaceContainerLow, modifier = Modifier.width(320.dp).fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, content = topBar)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                when {
                    state.daily != null -> "${state.daily} · ${state.layout?.name ?: ""}"
                    else -> board?.layout?.let { "${it.size} tiles · ${it.maxZ + 1} layers" } ?: ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            when (state.mode) {
                GameMode.ZEN -> Text("Zen", style = MaterialTheme.typography.displaySmall, color = cs.primary)
                GameMode.TIME_ATTACK -> {
                    Text("Time left", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Text(
                        formatTime(state.timeLeftMs + 999),
                        style = MaterialTheme.typography.displayMedium,
                        color = if (state.timeLeftMs < 10_000) cs.error else cs.onSurface,
                    )
                }
                GameMode.CLASSIC -> if (settings.showTimer) {
                    Text("Time", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Text(formatTime(state.elapsedMs), style = MaterialTheme.typography.displayMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.mode != GameMode.ZEN) PanelStat("Score", state.score.toString(), Modifier.weight(1f))
                PanelStat("Pairs", (remaining / 2).toString(), Modifier.weight(1f))
                PanelStat("Moves", moves.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { 1f - remaining.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${((1f - remaining.toFloat() / total) * 100).toInt()}% cleared",
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            val live = state.status == GameStatus.PLAYING || state.status == GameStatus.PAUSED
            androidx.compose.material3.Button(onClick = actions.hint, enabled = live, modifier = Modifier.fillMaxWidth()) {
                PanelButtonContent(Icons.Filled.Lightbulb, "Hint", "H")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = actions.undo, enabled = state.history.isNotEmpty() && live, modifier = Modifier.fillMaxWidth()) {
                PanelButtonContent(Icons.AutoMirrored.Filled.Undo, "Undo", "Ctrl+Z")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = actions.redo, enabled = state.redo.isNotEmpty() && live, modifier = Modifier.fillMaxWidth()) {
                PanelButtonContent(Icons.AutoMirrored.Filled.Redo, "Redo", "Ctrl+Y")
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = actions.shuffle, enabled = live, modifier = Modifier.fillMaxWidth()) {
                PanelButtonContent(Icons.Filled.Shuffle, "Shuffle", "S")
            }
            if (canFinish && state.status == GameStatus.PLAYING) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = actions.autoFinish,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary),
                ) { PanelButtonContent(Icons.Filled.DoneAll, "Finish", "F") }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onShortcuts) { Text("Keyboard shortcuts") }
        }
    }
}

@Composable
private fun PanelStat(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PanelButtonContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    shortcut: String,
) {
    Icon(icon, null, Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(label, Modifier.weight(1f))
    Text(shortcut, style = MaterialTheme.typography.labelSmall, modifier = Modifier.graphicsLayer { alpha = 0.7f })
}

@Composable
private fun ShortcutsDialog(onDismiss: () -> Unit) {
    val rows = listOf(
        "Arrow keys" to "Move between tiles",
        "Enter" to "Select tile",
        "Esc" to "Deselect",
        "H" to "Hint",
        "S" to "Shuffle",
        "F" to "Finish (when all tiles are free)",
        "Ctrl+Z" to "Undo",
        "Ctrl+Y / Ctrl+Shift+Z" to "Redo",
        "Space / P" to "Pause or resume",
        "N" to "New game",
        "+ / − / 0" to "Zoom in, out, reset",
        "Scroll wheel" to "Zoom toward the pointer",
        "Right-click" to "Inspect a tile",
        "?" to "Show this list",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyboard & mouse") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                rows.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(150.dp)) { ShortcutLabel(k) }
                        Text(v, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
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

@Composable
private fun TileInspector(
    board: com.ezmanw.mathong.game.Board,
    tile: Int?,
    settings: com.ezmanw.mathong.data.Settings,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(tile) {
        if (tile != null) { delay(3500); onDismiss() }
    }
    AnimatedVisibility(tile != null && board.present.getOrElse(tile) { false }, modifier, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        val i = tile ?: return@AnimatedVisibility
        val kind = board.kind(i)
        val free = board.isFree(i)
        val left = (0 until board.layout.size).count { board.present[it] && board.kind(it).matches(kind) }
        val freeMatches = board.freeTiles().count { it != i && board.kind(it).matches(kind) }
        Surface(
            onClick = onDismiss,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                com.ezmanw.mathong.ui.common.TilePreview(kind, com.ezmanw.mathong.ui.tiles.rememberTileStyle(settings), 52.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(kind.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (free) "Free" else "Blocked",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (free) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "$left on the board · $freeMatches free match${if (freeMatches == 1) "" else "es"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
