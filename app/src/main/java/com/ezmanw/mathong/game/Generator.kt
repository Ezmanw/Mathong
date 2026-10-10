package com.ezmanw.mathong.game

import kotlin.random.Random

object Generator {

    class Assignment(val kinds: Map<Int, Int>, val order: List<Pair<Int, Int>>)

    /** Deals a new solvable board for [layout]. */
    fun deal(layout: Layout, seed: Long): Board = dealWithSolution(layout, seed).first

    /** Deals a board and returns the removal order that proves it solvable. */
    fun dealWithSolution(layout: Layout, seed: Long): Pair<Board, List<Pair<Int, Int>>> {
        val random = Random(seed)
        val pairs = TileKind.fullSetPairs().shuffled(random).take(layout.size / 2)
        val slots = (0 until layout.size).toList()
        repeat(500) {
            val assigned = assign(layout, slots, pairs, random)
            if (assigned != null) {
                val kinds = IntArray(layout.size)
                for ((s, k) in assigned.kinds) kinds[s] = k
                return Board(layout, kinds, BooleanArray(layout.size) { true }) to assigned.order
            }
        }
        error("Unable to generate a solvable deal for ${layout.id}")
    }

    /**
     * Simulates removing pairs of free tiles from [slots] (in random order, weighted
     * towards tiles that block others) and assigns each removed pair a kind pair.
     * Because the removal order is valid, playing it forward always solves the board.
     */
    fun assign(
        layout: Layout,
        slots: List<Int>,
        pairs: List<Pair<TileKind, TileKind>>,
        random: Random,
        attempts: Int = 200,
    ): Assignment? {
        require(pairs.size * 2 == slots.size)
        val geo = Geometry.of(layout)
        repeat(attempts) {
            val present = BooleanArray(layout.size)
            for (s in slots) present[s] = true
            val result = HashMap<Int, Int>()
            val removal = ArrayList<Pair<Int, Int>>()
            val order = pairs.shuffled(random)
            var ok = true
            for (pair in order) {
                val free = slots.filter { geo.isFree(it, present) }
                if (free.size < 2) { ok = false; break }
                val a = weightedPick(free, geo, present, random)
                present[a] = false
                // Second tile must have been free together with the first.
                val free2 = free.filter { it != a }
                val b = weightedPick(free2, geo, present, random)
                present[b] = false
                removal += a to b
                if (random.nextBoolean()) {
                    result[a] = pair.first.id; result[b] = pair.second.id
                } else {
                    result[a] = pair.second.id; result[b] = pair.first.id
                }
            }
            if (ok) return Assignment(result, removal)
        }
        return null
    }

    private fun weightedPick(free: List<Int>, geo: Geometry, present: BooleanArray, random: Random): Int {
        // Prefer high, blocking tiles so tall stacks get dismantled before they become dead ends.
        val weights = free.map { s ->
            val z = geo.layout.slots[s].z
            1.0 + z * 1.5 + geo.below[s].count { present[it] } * 2.0 +
                (geo.left[s].count { present[it] } + geo.right[s].count { present[it] }) * 0.5
        }
        var r = random.nextDouble() * weights.sum()
        for (i in free.indices) {
            r -= weights[i]
            if (r <= 0) return free[i]
        }
        return free.last()
    }
}
