package com.ezmanw.mathong.game

/**
 * Bounded depth-first solver used to check whether the current position can still be
 * won, so hints can steer away from dead ends.
 */
object Solver {

    /** Returns a move that keeps the board winnable, or null if none was found within [budget]. */
    fun findWinningMove(board: Board, budget: Int = 30_000): Pair<Int, Int>? {
        val geo = board.geometry
        val kinds = board.kinds
        var nodes = 0
        val failed = HashSet<Long>()

        fun hash(p: BooleanArray): Long {
            var h = -3750763034362895579L
            for (i in p.indices) if (p[i]) { h = h xor i.toLong(); h *= 1099511628211L }
            return h
        }

        fun moves(present: BooleanArray): List<Pair<Int, Int>> {
            val free = (present.indices).filter { geo.isFree(it, present) }
            val out = ArrayList<Pair<Int, Int>>()
            for (i in free.indices) for (j in i + 1 until free.size) {
                if (TileKind(kinds[free[i]]).matches(TileKind(kinds[free[j]]))) out += free[i] to free[j]
            }
            // Explore moves that unblock the most tiles first.
            fun weight(s: Int) = geo.below[s].count { present[it] } * 3 +
                geo.left[s].count { present[it] } + geo.right[s].count { present[it] } + geo.layout.slots[s].z
            return out.sortedByDescending { weight(it.first) + weight(it.second) }
        }

        /** A group whose remaining tiles are all free can be cleared without losing anything. */
        fun safeMove(present: BooleanArray, ms: List<Pair<Int, Int>>): Pair<Int, Int>? {
            for (m in ms) {
                val g = TileKind(kinds[m.first]).matchGroup
                val all = present.indices.filter { present[it] && TileKind(kinds[it]).matchGroup == g }
                if (all.size == 2 || (all.size == 4 && all.all { geo.isFree(it, present) })) return m
            }
            return null
        }

        fun solve(present: BooleanArray): Boolean {
            if (present.none { it }) return true
            if (++nodes > budget) return false
            val h = hash(present)
            if (h in failed) return false
            val ms = moves(present)
            val safe = safeMove(present, ms)
            val candidates = if (safe != null) listOf(safe) else ms
            for ((x, y) in candidates) {
                present[x] = false; present[y] = false
                val ok = solve(present)
                present[x] = true; present[y] = true
                if (ok) return true
                if (nodes > budget) return false
            }
            failed += h
            return false
        }

        val present = board.present.copyOf()
        for ((x, y) in moves(present)) {
            present[x] = false; present[y] = false
            val ok = solve(present)
            present[x] = true; present[y] = true
            if (ok) return x to y
            if (nodes > budget) return null
        }
        return null
    }
}
