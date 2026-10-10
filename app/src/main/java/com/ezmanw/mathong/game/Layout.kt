package com.ezmanw.mathong.game

/**
 * Position of a tile slot. Coordinates are in half-tile units: a tile spans
 * x..x+1 and y..y+1 (so 2x2 units), and [z] is the layer.
 */
data class Slot(val x: Int, val y: Int, val z: Int)

enum class Difficulty { EASY, MEDIUM, HARD, EXPERT }

class Layout(
    val id: String,
    val name: String,
    val difficulty: Difficulty,
    val slots: List<Slot>,
) {
    val size get() = slots.size
    val minX = slots.minOf { it.x }
    val minY = slots.minOf { it.y }
    val maxX = slots.maxOf { it.x } + 2
    val maxY = slots.maxOf { it.y } + 2
    val maxZ = slots.maxOf { it.z }
}

/** Small DSL that works in tile units (0.5 steps allowed) and converts to half-tile units. */
class LayoutBuilder {
    private val slots = LinkedHashSet<Slot>()

    fun tile(x: Double, y: Double, z: Int) {
        slots += Slot((x * 2).toInt(), (y * 2).toInt(), z)
    }

    fun row(x: Double, y: Double, count: Int, z: Int) {
        for (i in 0 until count) tile(x + i, y, z)
    }

    fun col(x: Double, y: Double, count: Int, z: Int) {
        for (i in 0 until count) tile(x, y + i, z)
    }

    fun rect(x: Double, y: Double, w: Int, h: Int, z: Int) {
        for (j in 0 until h) row(x, y + j, w, z)
    }

    /** Hollow rectangle. */
    fun frame(x: Double, y: Double, w: Int, h: Int, z: Int) {
        row(x, y, w, z); row(x, y + h - 1, w, z)
        col(x, y + 1, h - 2, z); col(x + w - 1, y + 1, h - 2, z)
    }

    /**
     * Each string is a row; '#' places a tile. Columns map to x = col, rows to y = row
     * (both offset by [dx]/[dy]).
     */
    fun grid(z: Int, dx: Double = 0.0, dy: Double = 0.0, vararg rows: String) {
        rows.forEachIndexed { r, line ->
            line.forEachIndexed { c, ch -> if (ch == '#') tile(dx + c, dy + r, z) }
        }
    }

    fun build(id: String, name: String, difficulty: Difficulty): Layout {
        val list = slots.toList()
        require(list.size % 2 == 0) { "Layout $id has odd tile count ${list.size}" }
        return Layout(id, name, difficulty, list)
    }
}

fun layout(id: String, name: String, difficulty: Difficulty, block: LayoutBuilder.() -> Unit): Layout =
    LayoutBuilder().apply(block).build(id, name, difficulty)
