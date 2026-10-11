package com.ezmanw.mathong.game

import kotlin.math.abs

enum class Direction { LEFT, RIGHT, UP, DOWN }

/**
 * Keyboard navigation across the board: moves from [from] to the nearest present tile in
 * [dir], preferring free tiles and tiles roughly in line. With no current tile, starts at
 * the top-left free tile.
 */
fun Board.moveCursor(from: Int?, dir: Direction): Int? {
    val slots = layout.slots
    val candidates = (0 until layout.size).filter { present[it] }
    if (candidates.isEmpty()) return null
    val freeFirst = candidates.filter { isFree(it) }.ifEmpty { candidates }
    if (from == null || !present.getOrElse(from) { false }) {
        return freeFirst.minWith(compareBy({ slots[it].y }, { slots[it].x }, { -slots[it].z }))
    }
    val a = slots[from]
    var best: Int? = null
    var bestScore = Double.MAX_VALUE
    for (i in freeFirst) {
        if (i == from) continue
        val b = slots[i]
        val dx = (b.x - b.z) - (a.x - a.z) // account for the visual up-left shift of higher layers
        val dy = (b.y - b.z) - (a.y - a.z)
        val (along, across) = when (dir) {
            Direction.LEFT -> -dx to dy
            Direction.RIGHT -> dx to dy
            Direction.UP -> -dy to dx
            Direction.DOWN -> dy to dx
        }
        if (along <= 0) continue
        val score = along + abs(across) * 2.5
        if (score < bestScore) { bestScore = score; best = i }
    }
    return best ?: from
}

/** True when every remaining tile is free, so the rest of the game can be played automatically. */
fun Board.canAutoFinish(): Boolean =
    !isCleared && (0 until layout.size).all { !present[it] || isFree(it) }
