package com.ezmanw.mathong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ezmanw.mathong.data.BoardBackground
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.data.TileTheme
import com.ezmanw.mathong.game.Generator
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.game.BoardView
import com.ezmanw.mathong.ui.game.boardBrush
import com.ezmanw.mathong.ui.theme.MathongTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 1.0)

    private fun board(layoutId: String, settings: Settings, removeSome: Int = 0) {
        var b = Generator.deal(Layouts.byId(layoutId), 42)
        repeat(removeSome) {
            val m = b.availableMatches().firstOrNull() ?: return@repeat
            b = b.remove(m.first, m.second)
        }
        val free = b.freeTiles()
        paparazzi.snapshot {
            MathongTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(boardBrush(settings.background))) {
                    BoardView(b, free.firstOrNull(), null, null, 0, settings, onTap = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @Test fun turtle() = board("turtle", Settings())
    @Test fun pyramidFelt() = board("pyramid", Settings(background = BoardBackground.FELT, tileTheme = TileTheme.IVORY), 10)
    @Test fun pocketIndices() = board("pocket", Settings(showIndices = true, tileTheme = TileTheme.SAPPHIRE))
}
