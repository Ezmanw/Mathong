package com.ezmanw.mathong.game

import kotlin.random.Random

/**
 * Immutable board snapshot. [kinds] is indexed by slot; [present] says whether the
 * tile in that slot is still on the board.
 */
class Board(
    val layout: Layout,
    val kinds: IntArray,
    val present: BooleanArray,
) {
    val geometry get() = Geometry.of(layout)
    val remaining: Int get() = present.count { it }
    val isCleared: Boolean get() = remaining == 0

    fun kind(i: Int) = TileKind(kinds[i])
    fun isFree(i: Int) = geometry.isFree(i, present)

    fun freeTiles(): List<Int> = (0 until layout.size).filter { isFree(it) }

    fun canMatch(a: Int, b: Int) =
        a != b && isFree(a) && isFree(b) && kind(a).matches(kind(b))

    fun remove(a: Int, b: Int): Board {
        val p = present.copyOf(); p[a] = false; p[b] = false
        return Board(layout, kinds, p)
    }

    fun restore(a: Int, b: Int): Board {
        val p = present.copyOf(); p[a] = true; p[b] = true
        return Board(layout, kinds, p)
    }

    /** All currently available matching pairs. */
    fun availableMatches(): List<Pair<Int, Int>> {
        val free = freeTiles()
        val out = ArrayList<Pair<Int, Int>>()
        for (i in free.indices) for (j in i + 1 until free.size) {
            if (kind(free[i]).matches(kind(free[j]))) out += free[i] to free[j]
        }
        return out
    }

    fun hasMoves() = availableMatches().isNotEmpty()

    /**
     * Re-deals the remaining tiles over the remaining slots so the result is guaranteed
     * solvable. Returns null if it could not find a solvable arrangement.
     */
    fun shuffled(random: Random): Board? {
        val slots = (0 until layout.size).filter { present[it] }
        // Group remaining tiles into matching pairs.
        val byGroup = slots.groupBy { kind(it).matchGroup }
        val pairs = ArrayList<Pair<TileKind, TileKind>>()
        for ((_, list) in byGroup) {
            val ks = list.map { kind(it) }
            var i = 0
            while (i + 1 < ks.size) { pairs += ks[i] to ks[i + 1]; i += 2 }
        }
        val newKinds = Generator.assign(layout, slots, pairs, random) ?: return null
        val k = kinds.copyOf()
        for ((slot, kind) in newKinds.kinds) k[slot] = kind
        return Board(layout, k, present.copyOf())
    }
}
