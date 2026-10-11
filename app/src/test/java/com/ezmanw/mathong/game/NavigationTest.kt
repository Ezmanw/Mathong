package com.ezmanw.mathong.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    @Test
    fun cursorWalksARow() {
        val layout = layout("row", "Row", Difficulty.EASY) { row(0.0, 0.0, 4, 0) }
        val board = Board(layout, intArrayOf(0, 1, 1, 0), BooleanArray(4) { true })
        val start = board.moveCursor(null, Direction.RIGHT)
        assertEquals(0, start) // top-left free tile
        // Only the two ends are free, so moving right jumps to the other end.
        assertEquals(3, board.moveCursor(0, Direction.RIGHT))
        assertEquals(0, board.moveCursor(3, Direction.LEFT))
        // Nothing further right: stay put.
        assertEquals(3, board.moveCursor(3, Direction.RIGHT))
    }

    @Test
    fun cursorAlwaysLandsOnPresentTile() {
        val board = Generator.deal(Layouts.turtle, 3)
        var c: Int? = null
        for (d in listOf(Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.RIGHT)) {
            c = board.moveCursor(c, d)
            assertNotNull(c)
            assertTrue(board.present[c!!])
        }
    }
}
