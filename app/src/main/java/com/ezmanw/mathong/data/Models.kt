package com.ezmanw.mathong.data

import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class GameMode(val label: String, val description: String) {
    CLASSIC("Classic", "Timer, score and stars"),
    TIME_ATTACK("Time Attack", "Beat the countdown — every match adds time"),
    ZEN("Zen", "No timer, no score, free hints"),
}

enum class TileTheme(val label: String) {
    JADE("Jade"), IVORY("Ivory"), CRIMSON("Crimson"), SAPPHIRE("Sapphire"), AMBER("Amber"), ONYX("Onyx")
}

enum class BoardBackground(val label: String) {
    THEME("Material"), FELT("Green felt"), WOOD("Walnut"), NIGHT("Midnight"), SAKURA("Sakura")
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val tileTheme: TileTheme = TileTheme.JADE,
    val background: BoardBackground = BoardBackground.THEME,
    val showIndices: Boolean = false,
    val highlightFree: Boolean = true,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val showTimer: Boolean = true,
    val smartHints: Boolean = true,
    val autoShuffle: Boolean = false,
    val showMatches: Boolean = false,
    val simpleFaces: Boolean = false,
    val dailyReminder: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
)

@Serializable
data class LayoutStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestTimeMs: Long = 0,
    val bestScore: Int = 0,
    val bestStars: Int = 0,
)

@Serializable
data class Stats(
    val perLayout: Map<String, LayoutStats> = emptyMap(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalMatches: Int = 0,
    val totalPlayMs: Long = 0,
    val hintsUsed: Int = 0,
    val bestCombo: Int = 0,
    val dailyCompleted: Set<String> = emptySet(),
    val achievements: Map<String, Long> = emptyMap(),
    val recent: List<RecentGame> = emptyList(),
    val timeAttackWins: Int = 0,
    val timeAttackLosses: Int = 0,
    val zenWins: Int = 0,
) {
    val gamesPlayed get() = perLayout.values.sumOf { it.played }
    val gamesWon get() = perLayout.values.sumOf { it.won }
    val winRate get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed
}

@Serializable
data class RecentGame(
    val layoutId: String,
    val timeMs: Long,
    val score: Int,
    val stars: Int,
    val tiles: Int,
    val at: Long,
    val daily: Boolean = false,
)

@Serializable
data class SavedMove(val a: Int, val b: Int, val points: Int)

@Serializable
data class SavedGame(
    val layoutId: String,
    val seed: Long,
    val kinds: List<Int>,
    val present: List<Boolean>,
    val history: List<SavedMove>,
    val elapsedMs: Long,
    val score: Int,
    val hintsUsed: Int,
    val shufflesUsed: Int,
    val undosUsed: Int,
    val daily: String? = null,
    val bestCombo: Int = 0,
    val mode: GameMode = GameMode.CLASSIC,
    val timeLeftMs: Long = 0,
)

@Serializable
data class CustomLayoutData(
    val id: String,
    val name: String,
    val created: Long,
    /** Flattened x, y, z triples in half-tile units. */
    val slots: List<Int>,
)
