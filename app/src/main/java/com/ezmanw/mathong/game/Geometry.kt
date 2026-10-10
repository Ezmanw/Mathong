package com.ezmanw.mathong.game

import kotlin.math.abs

/** Precomputed blocking relations between slots of a [Layout]. */
class Geometry private constructor(val layout: Layout) {
    val size = layout.size
    /** Slots on the next layer(s) that overlap this slot from above. */
    val above: Array<IntArray>
    /** Slots on the same layer touching the left edge. */
    val left: Array<IntArray>
    /** Slots on the same layer touching the right edge. */
    val right: Array<IntArray>
    /** Slots this slot covers or flanks — used to weight deal generation. */
    val below: Array<IntArray>

    init {
        val s = layout.slots
        val ab = Array(size) { ArrayList<Int>() }
        val be = Array(size) { ArrayList<Int>() }
        val l = Array(size) { ArrayList<Int>() }
        val r = Array(size) { ArrayList<Int>() }
        for (i in 0 until size) for (j in 0 until size) {
            if (i == j) continue
            val a = s[i]; val b = s[j]
            val overlapY = abs(a.y - b.y) < 2
            if (b.z > a.z && abs(a.x - b.x) < 2 && overlapY) { ab[i] += j; be[j] += i }
            if (b.z == a.z && overlapY) {
                if (b.x == a.x - 2) l[i] += j
                if (b.x == a.x + 2) r[i] += j
            }
        }
        above = Array(size) { ab[it].toIntArray() }
        below = Array(size) { be[it].toIntArray() }
        left = Array(size) { l[it].toIntArray() }
        right = Array(size) { r[it].toIntArray() }
    }

    fun isFree(i: Int, present: BooleanArray): Boolean {
        if (!present[i]) return false
        if (above[i].any { present[it] }) return false
        return left[i].none { present[it] } || right[i].none { present[it] }
    }

    companion object {
        private val cache = HashMap<String, Geometry>()
        fun of(layout: Layout): Geometry = synchronized(cache) {
            cache.getOrPut(layout.id) { Geometry(layout) }
        }
    }
}
