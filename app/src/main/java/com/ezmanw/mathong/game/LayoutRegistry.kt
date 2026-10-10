package com.ezmanw.mathong.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Built-in layouts plus the player's custom layouts. */
object LayoutRegistry {
    private val _custom = MutableStateFlow<List<Layout>>(emptyList())
    val custom: StateFlow<List<Layout>> = _custom.asStateFlow()

    @Volatile
    var loaded = false
        private set

    fun setCustom(layouts: List<Layout>) {
        layouts.forEach { Geometry.invalidate(it.id) }
        _custom.value = layouts
        loaded = true
    }

    fun all(): List<Layout> = Layouts.all + _custom.value

    fun find(id: String): Layout? = all().firstOrNull { it.id == id }

    fun byId(id: String): Layout = find(id) ?: Layouts.turtle

    fun isCustom(id: String) = id.startsWith(CUSTOM_PREFIX)

    const val CUSTOM_PREFIX = "custom_"

    fun estimateDifficulty(slots: List<Slot>): Difficulty {
        val layers = slots.maxOf { it.z } + 1
        return when {
            slots.size < 60 && layers <= 2 -> Difficulty.EASY
            slots.size < 110 && layers <= 3 -> Difficulty.MEDIUM
            layers >= 5 -> Difficulty.EXPERT
            else -> Difficulty.HARD
        }
    }

    /** Checks that [slots] can be dealt. Returns an error message, or null if valid. */
    fun validate(slots: List<Slot>): String? {
        if (slots.size < 4) return "Place at least 4 tiles"
        if (slots.size % 2 != 0) return "Tile count must be even (${slots.size})"
        if (slots.size > 144) return "At most 144 tiles (${slots.size})"
        val layout = Layout("__validate_${slots.hashCode()}", "", Difficulty.EASY, slots)
        val ok = runCatching { Generator.deal(layout, 1); Generator.deal(layout, 2) }.isSuccess
        Geometry.invalidate(layout.id)
        return if (ok) null else "This shape can't be dealt solvably — spread the tiles out or lower the stacks"
    }
}
