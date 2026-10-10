package com.ezmanw.mathong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

    @Test
    fun allLayouts() {
        paparazzi.snapshot {
            MathongTheme(dynamicColor = false) {
                androidx.compose.foundation.layout.FlowRow(
                    Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White),
                ) {
                    Layouts.all.forEach { l ->
                        androidx.compose.foundation.layout.Column(Modifier.padding(4.dp)) {
                            com.ezmanw.mathong.ui.common.LayoutThumbnail(l, Modifier.size(120.dp, 100.dp))
                            androidx.compose.material3.Text(l.name)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun paceChart() {
        val r = kotlin.random.Random(1)
        val games = List(18) { i ->
            val l = Layouts.all[r.nextInt(Layouts.all.size)]
            com.ezmanw.mathong.data.RecentGame(l.id, (l.size * (2500L + r.nextInt(3000))), 1000 + i * 37, 2, l.size, 1_760_000_000_000L + i * 86_400_000L)
        }
        paparazzi.snapshot {
            MathongTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(16.dp)) {
                    com.ezmanw.mathong.ui.stats.PaceChart(games)
                }
            }
        }
    }

    @Test
    fun editorCanvas() {
        val l = Layouts.castle
        val ox = (com.ezmanw.mathong.ui.editor.GRID_W - (l.maxX - l.minX)) / 2
        val st = com.ezmanw.mathong.ui.editor.EditorState(
            slots = l.slots.map { com.ezmanw.mathong.game.Slot(it.x + ox, it.y + 2, it.z) }, layer = 1,
        )
        paparazzi.snapshot {
            MathongTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                    com.ezmanw.mathong.ui.editor.EditorCanvas(st, { _, _ -> }, Modifier.fillMaxSize().padding(12.dp))
                }
            }
        }
    }

    @Test fun turtle() = board("turtle", Settings())
    @Test fun pyramidFelt() = board("pyramid", Settings(background = BoardBackground.FELT, tileTheme = TileTheme.IVORY), 10)
    @Test fun pocketSimple() = board("pocket", Settings(simpleFaces = true, tileTheme = TileTheme.AMBER))
    @Test fun pocketIndices() = board("pocket", Settings(showIndices = true, tileTheme = TileTheme.SAPPHIRE))
}
