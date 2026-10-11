package com.ezmanw.mathong.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.ezmanw.mathong.data.LayoutStats
import com.ezmanw.mathong.data.SavedGame
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.data.Stats
import com.ezmanw.mathong.game.Generator
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.game.GameActions
import com.ezmanw.mathong.ui.game.GameContent
import com.ezmanw.mathong.ui.game.GameStatus
import com.ezmanw.mathong.ui.game.GameUiState
import com.ezmanw.mathong.ui.home.HomeActions
import com.ezmanw.mathong.ui.home.HomeContent
import com.ezmanw.mathong.ui.theme.MathongTheme
import org.junit.Rule
import org.junit.Test

private fun gameState(layoutId: String, removed: Int): GameUiState {
    var b = Generator.deal(Layouts.byId(layoutId), 42)
    repeat(removed) { b.availableMatches().firstOrNull()?.let { (x, y) -> b = b.remove(x, y) } }
    return GameUiState(board = b, status = GameStatus.PLAYING, elapsedMs = 83_000, score = 340, selected = b.freeTiles().first())
}

private val sampleStats = Stats(
    perLayout = mapOf("turtle" to LayoutStats(played = 12, won = 9, bestTimeMs = 301_000, bestScore = 2140, bestStars = 3)),
    currentStreak = 4, bestStreak = 7, totalMatches = 812, dailyCompleted = setOf("2026-10-09", "2026-10-10"),
    recent = List(12) { i -> com.ezmanw.mathong.data.RecentGame("turtle", 250_000L + (i * 37_000L) % 120_000, 1800, 2, 144, 1_760_000_000_000L + i * 86_400_000L) },
)

private fun savedGame(): SavedGame {
    val b = Generator.deal(Layouts.pyramid, 5)
    return SavedGame("pyramid", 5, b.kinds.toList(), b.present.toList().mapIndexed { i, p -> p && i % 3 != 0 }, emptyList(), 125_000, 220, 0, 0, 0)
}

abstract class AdaptiveShots(device: DeviceConfig) {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device, maxPercentDifference = 1.0)

    @Test
    fun game() = paparazzi.snapshot {
        MathongTheme(dynamicColor = false) { GameContent(gameState("turtle", 8), Settings(), GameActions()) }
    }

    @Test
    fun home() = paparazzi.snapshot {
        MathongTheme(dynamicColor = false) { HomeContent(savedGame(), sampleStats, Settings(), seenTutorial = true, actions = HomeActions()) }
    }
}

class PhoneShots : AdaptiveShots(DeviceConfig.PIXEL_6)

class TabletShots : AdaptiveShots(DeviceConfig.PIXEL_C)

class DesktopShots : AdaptiveShots(
    DeviceConfig.PIXEL_C.copy(
        screenWidth = 1920, screenHeight = 1080, xdpi = 160, ydpi = 160,
        density = Density.MEDIUM, orientation = ScreenOrientation.LANDSCAPE,
    ),
)
