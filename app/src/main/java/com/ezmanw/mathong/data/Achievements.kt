package com.ezmanw.mathong.data

import com.ezmanw.mathong.game.Layouts

data class Achievement(val id: String, val title: String, val description: String)

/** Summary of a finished game that achievements are evaluated against. */
data class GameResult(
    val layoutId: String,
    val won: Boolean,
    val timeMs: Long,
    val score: Int,
    val stars: Int,
    val hintsUsed: Int,
    val shufflesUsed: Int,
    val undosUsed: Int,
    val bestCombo: Int,
    val daily: String?,
    val tiles: Int,
    val mode: GameMode = GameMode.CLASSIC,
    val timeLeftMs: Long = 0,
)

object Achievements {
    val all = listOf(
        Achievement("first_win", "First Steps", "Win your first game"),
        Achievement("purist", "Purist", "Win without hints, undos or shuffles"),
        Achievement("speedy", "Quick Hands", "Win a layout of 60+ tiles in under 3 minutes"),
        Achievement("lightning", "Lightning", "Win the Turtle in under 5 minutes"),
        Achievement("turtle", "Turtle Tamer", "Win the classic Turtle layout"),
        Achievement("ziggurat", "Summit", "Conquer the Ziggurat"),
        Achievement("combo5", "Combo Master", "Reach a x5 combo"),
        Achievement("three_stars", "Perfectionist", "Earn three stars"),
        Achievement("streak5", "On a Roll", "Win 5 games in a row"),
        Achievement("streak10", "Unstoppable", "Win 10 games in a row"),
        Achievement("daily1", "Daily Ritual", "Complete a daily challenge"),
        Achievement("daily7", "Devoted", "Complete 7 daily challenges"),
        Achievement("daily30", "Monk", "Complete 30 daily challenges"),
        Achievement("wins10", "Regular", "Win 10 games"),
        Achievement("wins50", "Veteran", "Win 50 games"),
        Achievement("wins100", "Grandmaster", "Win 100 games"),
        Achievement("tour", "World Tour", "Win every layout at least once"),
        Achievement("matches1000", "Thousand Pairs", "Match 1,000 pairs in total"),
        Achievement("beat_clock", "Beat the Clock", "Win a Time Attack game"),
        Achievement("photo_finish", "Photo Finish", "Win a Time Attack game with under 5 seconds left"),
        Achievement("zen5", "Inner Peace", "Clear 5 boards in Zen mode"),
        Achievement("architect", "Architect", "Win on a layout you designed"),
    )

    fun byId(id: String) = all.first { it.id == id }

    /** Returns ids of achievements newly unlocked by [stats] after [result]. */
    fun evaluate(stats: Stats, result: GameResult): List<String> {
        val have = stats.achievements.keys
        val earned = mutableListOf<String>()
        fun check(id: String, cond: Boolean) { if (cond && id !in have) earned += id }
        val r = result
        if (r.won && r.mode != GameMode.ZEN) {
            check("first_win", true)
            check("purist", r.hintsUsed == 0 && r.undosUsed == 0 && r.shufflesUsed == 0)
            check("speedy", r.tiles >= 60 && r.timeMs < 180_000)
            check("lightning", r.layoutId == "turtle" && r.timeMs < 300_000)
            check("turtle", r.layoutId == "turtle")
            check("ziggurat", r.layoutId == "ziggurat")
            check("three_stars", r.stars >= 3)
        }
        check("combo5", r.bestCombo >= 5)
        check("streak5", stats.currentStreak >= 5)
        check("streak10", stats.currentStreak >= 10)
        check("daily1", stats.dailyCompleted.isNotEmpty())
        check("daily7", stats.dailyCompleted.size >= 7)
        check("daily30", stats.dailyCompleted.size >= 30)
        check("wins10", stats.gamesWon >= 10)
        check("wins50", stats.gamesWon >= 50)
        check("wins100", stats.gamesWon >= 100)
        check("tour", Layouts.all.all { (stats.perLayout[it.id]?.won ?: 0) > 0 })
        check("matches1000", stats.totalMatches >= 1000)
        check("beat_clock", r.won && r.mode == GameMode.TIME_ATTACK)
        check("photo_finish", r.won && r.mode == GameMode.TIME_ATTACK && r.timeLeftMs < 5_000)
        check("zen5", stats.zenWins >= 5)
        check("architect", r.won && r.layoutId.startsWith("custom_"))
        return earned
    }
}
