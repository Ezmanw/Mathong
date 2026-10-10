package com.ezmanw.mathong.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ezmanw.mathong.MathongApp
import com.ezmanw.mathong.audio.Sfx
import com.ezmanw.mathong.data.Achievements
import com.ezmanw.mathong.data.GameResult
import com.ezmanw.mathong.data.LayoutStats
import com.ezmanw.mathong.data.SavedGame
import com.ezmanw.mathong.data.SavedMove
import com.ezmanw.mathong.game.Board
import com.ezmanw.mathong.game.Generator
import com.ezmanw.mathong.game.Layout
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.game.Solver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

enum class GameStatus { LOADING, PLAYING, PAUSED, STUCK, WON }

data class GameUiState(
    val board: Board? = null,
    val seed: Long = 0,
    val selected: Int? = null,
    val hint: Pair<Int, Int>? = null,
    val history: List<SavedMove> = emptyList(),
    val redo: List<SavedMove> = emptyList(),
    val elapsedMs: Long = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val undosUsed: Int = 0,
    val status: GameStatus = GameStatus.LOADING,
    val daily: String? = null,
    val result: GameResult? = null,
    val newAchievements: List<String> = emptyList(),
    val newBest: Boolean = false,
    val shakeTile: Int? = null,
    val shakeKey: Int = 0,
) {
    val layout: Layout? get() = board?.layout
}

sealed interface GameEvent {
    data class Match(val a: Int, val b: Int, val combo: Int, val points: Int) : GameEvent
    data object Invalid : GameEvent
    data object Shuffled : GameEvent
}

class GameViewModel(app: Application, savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val mathong = app as MathongApp
    private val repo = mathong.repository
    private val sounds = mathong.sounds

    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, com.ezmanw.mathong.data.Settings())

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var lastMatchAt = 0L
    private var screenVisible = true
    private var timerJob: Job? = null
    private var saveJob: Job? = null
    private var playedRecorded = false

    init {
        val layoutId: String? = savedState.get<String>("layout")?.takeIf { it.isNotBlank() }
        val daily: Boolean = savedState["daily"] ?: false
        val resume: Boolean = savedState["resume"] ?: false
        val seedArg: Long = savedState["seed"] ?: -1L
        viewModelScope.launch {
            settings.collect { sounds.enabled = it.sound }
        }
        viewModelScope.launch {
            val saved = repo.savedGame.first()
            val today = LocalDate.now().toString()
            when {
                resume && saved != null -> restore(saved)
                daily && saved?.daily == today -> restore(saved)
                daily -> startDaily()
                else -> newGame(Layouts.byId(layoutId ?: repo.lastLayout.first() ?: "turtle"), if (seedArg >= 0) seedArg else null)
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(250)
                val now = System.currentTimeMillis()
                val s = _state.value
                if (s.status == GameStatus.PLAYING && screenVisible) {
                    _state.update { it.copy(elapsedMs = it.elapsedMs + (now - last)) }
                }
                last = now
            }
        }
    }

    fun setVisible(visible: Boolean) {
        screenVisible = visible
        if (!visible) persist()
    }

    private suspend fun recordAbandonIfNeeded() {
        val s = _state.value
        val abandonedSaved = s.board == null && repo.savedGame.first()?.history?.isNotEmpty() == true
        if (abandonedSaved || (s.status != GameStatus.WON && s.history.isNotEmpty())) {
            repo.updateStats { it.copy(currentStreak = 0) }
        }
    }

    fun newGame(layout: Layout = _state.value.layout ?: Layouts.turtle, seed: Long? = null) {
        viewModelScope.launch {
            recordAbandonIfNeeded()
            val sd = seed ?: Random.nextLong(0, Long.MAX_VALUE)
            val board = withContext(Dispatchers.Default) { Generator.deal(layout, sd) }
            repo.setLastLayout(layout.id)
            playedRecorded = false
            lastMatchAt = 0
            _state.value = GameUiState(board = board, seed = sd, status = GameStatus.PLAYING)
            repo.saveGame(null)
        }
    }

    fun restart() {
        val s = _state.value
        val layout = s.layout ?: return
        viewModelScope.launch {
            val board = withContext(Dispatchers.Default) { Generator.deal(layout, s.seed) }
            lastMatchAt = 0
            _state.value = GameUiState(board = board, seed = s.seed, status = GameStatus.PLAYING, daily = s.daily)
            playedRecorded = false
        }
    }

    private suspend fun startDaily() {
        recordAbandonIfNeeded()
        repo.saveGame(null)
        val today = LocalDate.now()
        val (layout, seed) = dailyFor(today)
        val board = withContext(Dispatchers.Default) { Generator.deal(layout, seed) }
        playedRecorded = false
        _state.value = GameUiState(board = board, seed = seed, status = GameStatus.PLAYING, daily = today.toString())
    }

    private fun restore(saved: SavedGame) {
        val layout = Layouts.byId(saved.layoutId)
        if (saved.kinds.size != layout.size) {
            newGame(layout); return
        }
        val board = Board(layout, saved.kinds.toIntArray(), saved.present.toBooleanArray())
        playedRecorded = saved.history.isNotEmpty()
        _state.value = GameUiState(
            board = board,
            seed = saved.seed,
            history = saved.history,
            elapsedMs = saved.elapsedMs,
            score = saved.score,
            hintsUsed = saved.hintsUsed,
            shufflesUsed = saved.shufflesUsed,
            undosUsed = saved.undosUsed,
            daily = saved.daily,
            bestCombo = saved.bestCombo,
            status = if (board.hasMoves()) GameStatus.PAUSED else GameStatus.STUCK,
        )
    }

    fun pause() = _state.update { if (it.status == GameStatus.PLAYING) it.copy(status = GameStatus.PAUSED) else it }
        .also { persist() }

    fun resume() = _state.update { if (it.status == GameStatus.PAUSED) it.copy(status = GameStatus.PLAYING) else it }

    fun tapTile(i: Int) {
        val s = _state.value
        val board = s.board ?: return
        if (s.status == GameStatus.PAUSED) { resume(); return }
        if (s.status != GameStatus.PLAYING) return
        if (!board.present[i]) return
        if (!board.isFree(i)) {
            sounds.play(Sfx.ERROR, 0.6f)
            _state.update { it.copy(shakeTile = i, shakeKey = it.shakeKey + 1) }
            _events.tryEmit(GameEvent.Invalid)
            return
        }
        val sel = s.selected
        when {
            sel == null -> {
                sounds.play(Sfx.SELECT)
                _state.update { it.copy(selected = i) }
            }
            sel == i -> _state.update { it.copy(selected = null) }
            board.canMatch(sel, i) -> match(sel, i)
            else -> {
                sounds.play(Sfx.SELECT)
                _state.update { it.copy(selected = i) }
            }
        }
    }

    private fun match(a: Int, b: Int) {
        val s = _state.value
        val board = s.board ?: return
        val now = System.currentTimeMillis()
        val combo = if (lastMatchAt != 0L && now - lastMatchAt < 4000) (s.combo + 1).coerceAtMost(9) else 1
        lastMatchAt = now
        val points = 10 * combo
        val newBoard = board.remove(a, b)
        val move = SavedMove(a, b, points)
        sounds.play(Sfx.MATCH)
        _events.tryEmit(GameEvent.Match(a, b, combo, points))
        _state.update {
            it.copy(
                board = newBoard,
                selected = null,
                hint = null,
                history = it.history + move,
                redo = emptyList(),
                score = it.score + points,
                combo = combo,
                bestCombo = maxOf(it.bestCombo, combo),
            )
        }
        viewModelScope.launch {
            repo.updateStats { st ->
                var n = st.copy(totalMatches = st.totalMatches + 1, bestCombo = maxOf(st.bestCombo, combo))
                if (!playedRecorded) {
                    val ls = n.perLayout[newBoard.layout.id] ?: LayoutStats()
                    n = n.copy(perLayout = n.perLayout + (newBoard.layout.id to ls.copy(played = ls.played + 1)))
                }
                n
            }
            playedRecorded = true
        }
        if (newBoard.isCleared) {
            win()
        } else if (!newBoard.hasMoves()) {
            if (settings.value.autoShuffle) {
                shuffle(penalty = false)
            } else {
                _state.update { it.copy(status = GameStatus.STUCK) }
            }
            persist()
        } else {
            schedulePersist()
        }
    }

    private fun win() {
        val s = _state.value
        val layout = s.layout ?: return
        val targetMs = layout.size * 4_000L
        val timeBonus = ((targetMs - s.elapsedMs) / 1000).coerceAtLeast(0).toInt() * 2
        val helps = s.hintsUsed + s.shufflesUsed
        val stars = when {
            helps == 0 && s.undosUsed <= 3 && s.elapsedMs <= targetMs -> 3
            helps <= 2 -> 2
            else -> 1
        }
        val finalScore = s.score + timeBonus + stars * 50
        val result = GameResult(
            layoutId = layout.id,
            won = true,
            timeMs = s.elapsedMs,
            score = finalScore,
            stars = stars,
            hintsUsed = s.hintsUsed,
            shufflesUsed = s.shufflesUsed,
            undosUsed = s.undosUsed,
            bestCombo = s.bestCombo,
            daily = s.daily,
            tiles = layout.size,
        )
        sounds.play(Sfx.WIN)
        _state.update { it.copy(status = GameStatus.WON, score = finalScore, result = result, selected = null, hint = null) }
        viewModelScope.launch {
            repo.saveGame(null)
            var newBest = false
            val updated = repo.updateStats { st ->
                val ls = st.perLayout[layout.id] ?: LayoutStats(played = 1)
                newBest = ls.bestTimeMs == 0L || s.elapsedMs < ls.bestTimeMs
                val nls = ls.copy(
                    won = ls.won + 1,
                    bestTimeMs = if (newBest) s.elapsedMs else ls.bestTimeMs,
                    bestScore = maxOf(ls.bestScore, finalScore),
                    bestStars = maxOf(ls.bestStars, stars),
                )
                val streak = st.currentStreak + 1
                st.copy(
                    perLayout = st.perLayout + (layout.id to nls),
                    currentStreak = streak,
                    bestStreak = maxOf(st.bestStreak, streak),
                    totalPlayMs = st.totalPlayMs + s.elapsedMs,
                    hintsUsed = st.hintsUsed + s.hintsUsed,
                    dailyCompleted = if (s.daily != null) st.dailyCompleted + s.daily else st.dailyCompleted,
                )
            }
            val earned = Achievements.evaluate(updated, result)
            if (earned.isNotEmpty()) {
                val now = System.currentTimeMillis()
                repo.updateStats { st -> st.copy(achievements = st.achievements + earned.associateWith { now }) }
            }
            _state.update { it.copy(newAchievements = earned, newBest = newBest) }
        }
    }

    fun undo() {
        val s = _state.value
        val board = s.board ?: return
        if (s.history.isEmpty() || s.status == GameStatus.WON) return
        val last = s.history.last()
        sounds.play(Sfx.UNDO)
        _state.update {
            it.copy(
                board = board.restore(last.a, last.b),
                history = it.history.dropLast(1),
                redo = it.redo + last,
                score = (it.score - last.points).coerceAtLeast(0),
                undosUsed = it.undosUsed + 1,
                selected = null,
                hint = null,
                combo = 0,
                status = GameStatus.PLAYING,
            )
        }
        lastMatchAt = 0
        schedulePersist()
    }

    fun redo() {
        val s = _state.value
        val board = s.board ?: return
        val next = s.redo.lastOrNull() ?: return
        if (!board.canMatch(next.a, next.b)) { _state.update { it.copy(redo = emptyList()) }; return }
        sounds.play(Sfx.MATCH)
        val nb = board.remove(next.a, next.b)
        _state.update {
            it.copy(
                board = nb,
                history = it.history + next,
                redo = it.redo.dropLast(1),
                score = it.score + next.points,
                selected = null, hint = null,
            )
        }
        if (nb.isCleared) win() else if (!nb.hasMoves()) _state.update { it.copy(status = GameStatus.STUCK) }
        schedulePersist()
    }

    fun hint() {
        val s = _state.value
        val board = s.board ?: return
        if (s.status != GameStatus.PLAYING && s.status != GameStatus.PAUSED) return
        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) {
                (if (settings.value.smartHints) Solver.findWinningMove(board, 15_000) else null)
                    ?: board.availableMatches().randomOrNull()
            }
            if (move == null) {
                _state.update { it.copy(status = GameStatus.STUCK) }
                return@launch
            }
            sounds.play(Sfx.HINT)
            _state.update {
                it.copy(
                    hint = move,
                    selected = null,
                    hintsUsed = it.hintsUsed + 1,
                    score = (it.score - 20).coerceAtLeast(0),
                    status = GameStatus.PLAYING,
                )
            }
            delay(3500)
            _state.update { if (it.hint == move) it.copy(hint = null) else it }
        }
    }

    fun shuffle(penalty: Boolean = true) {
        val s = _state.value
        val board = s.board ?: return
        if (s.status == GameStatus.WON) return
        viewModelScope.launch {
            val nb = withContext(Dispatchers.Default) {
                val r = Random(System.nanoTime())
                var b: Board? = null
                repeat(20) { if (b == null) b = board.shuffled(r) }
                b
            } ?: return@launch
            sounds.play(Sfx.SHUFFLE)
            _events.tryEmit(GameEvent.Shuffled)
            _state.update {
                it.copy(
                    board = nb,
                    selected = null,
                    hint = null,
                    redo = emptyList(),
                    shufflesUsed = it.shufflesUsed + if (penalty) 1 else 0,
                    score = if (penalty) (it.score - 50).coerceAtLeast(0) else it.score,
                    status = GameStatus.PLAYING,
                )
            }
            schedulePersist()
        }
    }

    fun consumeAchievements() = _state.update { it.copy(newAchievements = emptyList()) }

    private fun schedulePersist() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { delay(800); persistNow() }
    }

    private fun persist() {
        saveJob?.cancel()
        viewModelScope.launch { persistNow() }
    }

    private suspend fun persistNow() {
        val s = _state.value
        val board = s.board ?: return
        if (s.status == GameStatus.WON || s.status == GameStatus.LOADING) return
        if (s.history.isEmpty()) return
        repo.saveGame(
            SavedGame(
                layoutId = board.layout.id,
                seed = s.seed,
                kinds = board.kinds.toList(),
                present = board.present.toList(),
                history = s.history,
                elapsedMs = s.elapsedMs,
                score = s.score,
                hintsUsed = s.hintsUsed,
                shufflesUsed = s.shufflesUsed,
                undosUsed = s.undosUsed,
                daily = s.daily,
                bestCombo = s.bestCombo,
            )
        )
    }

    override fun onCleared() {
        // Best effort save; viewModelScope is cancelled at this point.
        val s = _state.value
        if (s.status != GameStatus.WON && s.history.isNotEmpty()) {
            kotlinx.coroutines.runBlocking { persistNow() }
        }
        super.onCleared()
    }

    companion object {
        fun dailyFor(date: LocalDate): Pair<Layout, Long> {
            val pool = Layouts.all.filter { it.size >= 60 }
            val day = date.toEpochDay()
            val layout = pool[Math.floorMod(day * 31 + 7, pool.size.toLong()).toInt()]
            return layout to (day * 7919 + 104729)
        }
    }
}
