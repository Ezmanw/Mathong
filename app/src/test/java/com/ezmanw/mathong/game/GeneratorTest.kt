package com.ezmanw.mathong.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {

    @Test
    fun layoutsAreValid() {
        val ids = HashSet<String>()
        for (l in Layouts.all) {
            assertTrue("${l.id} duplicate", ids.add(l.id))
            assertEquals("${l.id} odd", 0, l.size % 2)
            assertTrue("${l.id} too large: ${l.size}", l.size <= 144)
            // No two slots on the same layer may overlap.
            for (i in l.slots.indices) for (j in i + 1 until l.slots.size) {
                val a = l.slots[i]; val b = l.slots[j]
                val overlap = a.z == b.z && kotlin.math.abs(a.x - b.x) < 2 && kotlin.math.abs(a.y - b.y) < 2
                assertTrue("${l.id} overlap $a $b", !overlap)
            }
            println("${l.id}: ${l.size} tiles")
        }
    }

    @Test
    fun dealsAreSolvableByReplay() {
        for (l in Layouts.all) {
            for (seed in 0L until 20L) {
                val (board, solution) = Generator.dealWithSolution(l, seed)
                // Kind multiset must form pairs.
                val counts = board.kinds.groupBy { TileKind(it).matchGroup }
                counts.values.forEach { assertEquals(0, it.size % 2) }
                // Replaying the generator's removal order must clear the board.
                var b = board
                for ((x, y) in solution) {
                    assertTrue("${l.id} seed $seed invalid move", b.canMatch(x, y))
                    b = b.remove(x, y)
                }
                assertTrue(b.isCleared)
            }
        }
    }

    @Test
    fun solverFindsMovesOnFreshDeals() {
        var found = 0; var total = 0
        for (l in Layouts.all) for (seed in 0L until 5L) {
            total++
            if (Solver.findWinningMove(Generator.deal(l, seed)) != null) found++
        }
        println("solver success $found/$total")
        assertTrue(found * 10 >= total * 7)
    }

    @Test
    fun shufflePreservesTiles() {
        val board = Generator.deal(Layouts.turtle, 7)
        val m = board.availableMatches().first()
        val b = board.remove(m.first, m.second)
        val s = b.shuffled(kotlin.random.Random(3))!!
        assertEquals(b.remaining, s.remaining)
        val before = (0 until b.layout.size).filter { b.present[it] }.map { b.kinds[it] }.sorted()
        val after = (0 until s.layout.size).filter { s.present[it] }.map { s.kinds[it] }.sorted()
        assertEquals(before, after)
    }
}
