package com.ezmanw.mathong.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ezmanw.mathong.MathongApp
import com.ezmanw.mathong.data.CustomLayoutData
import com.ezmanw.mathong.game.Layout
import com.ezmanw.mathong.game.LayoutRegistry
import com.ezmanw.mathong.game.Slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

const val GRID_W = 36   // half-tile units (18 tiles)
const val GRID_H = 20   // half-tile units (10 tiles)
const val MAX_LAYERS = 8

data class EditorState(
    val id: String? = null,
    val name: String = "My layout",
    val slots: List<Slot> = emptyList(),
    val layer: Int = 0,
    val mirror: Boolean = true,
    val halfStep: Boolean = false,
    val undo: List<List<Slot>> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
) {
    val count get() = slots.size
    val layers get() = (slots.maxOfOrNull { it.z } ?: -1) + 1
    val quickProblem: String?
        get() = when {
            count < 4 -> "Place at least 4 tiles"
            count % 2 != 0 -> "Tile count must be even"
            count > 144 -> "At most 144 tiles"
            else -> null
        }
}

class EditorViewModel(app: Application, savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = (app as MathongApp).repository
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        val id = savedState.get<String>("id")?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            if (!LayoutRegistry.loaded) repo.syncCustomLayouts()
            val existing = id?.let { LayoutRegistry.find(it) }
            if (existing != null && LayoutRegistry.isCustom(existing.id)) {
                _state.value = EditorState(id = existing.id, name = existing.name, slots = centered(existing))
            }
        }
    }

    private fun overlaps(a: Slot, b: Slot) = a.z == b.z && abs(a.x - b.x) < 2 && abs(a.y - b.y) < 2

    private fun push(newSlots: List<Slot>) = _state.update {
        it.copy(slots = newSlots, undo = (it.undo + listOf(it.slots)).takeLast(100), message = null)
    }

    /** Toggles a tile at half-unit position ([ux], [uy]) — the tap point — on the current layer. */
    fun tap(ux: Float, uy: Float) {
        val s = _state.value
        val z = s.layer
        // Remove the tile under the finger if there is one.
        val hit = s.slots.firstOrNull { it.z == z && ux >= it.x && ux < it.x + 2 && uy >= it.y && uy < it.y + 2 }
        if (hit != null) {
            val mirrored = Slot(GRID_W - 2 - hit.x, hit.y, z)
            push(s.slots.filter { it != hit && !(s.mirror && it == mirrored) })
            return
        }
        val x: Int
        val y: Int
        if (s.halfStep) {
            x = Math.round(ux - 1f); y = Math.round(uy - 1f)
        } else {
            x = (Math.floor(ux / 2.0).toInt()) * 2; y = (Math.floor(uy / 2.0).toInt()) * 2
        }
        val slot = Slot(x.coerceIn(0, GRID_W - 2), y.coerceIn(0, GRID_H - 2), z)
        if (s.slots.any { overlaps(it, slot) }) return
        val add = mutableListOf(slot)
        if (s.mirror) {
            val m = Slot(GRID_W - 2 - slot.x, slot.y, z)
            if (m != slot && !overlaps(m, slot) && s.slots.none { overlaps(it, m) }) add += m
        }
        push(s.slots + add)
    }

    fun setLayer(z: Int) = _state.update { it.copy(layer = z.coerceIn(0, MAX_LAYERS - 1)) }
    fun setName(n: String) = _state.update { it.copy(name = n.take(24)) }
    fun toggleMirror() = _state.update { it.copy(mirror = !it.mirror) }
    fun toggleHalf() = _state.update { it.copy(halfStep = !it.halfStep) }
    fun clearLayer() = push(_state.value.slots.filter { it.z != _state.value.layer })
    fun clearAll() = push(emptyList())
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun undo() = _state.update {
        if (it.undo.isEmpty()) it else it.copy(slots = it.undo.last(), undo = it.undo.dropLast(1))
    }

    /** Fills the canvas with a centred copy of a built-in layout. */
    fun loadTemplate(layout: Layout) = push(centered(layout))

    private fun centered(layout: Layout): List<Slot> {
        val w = layout.maxX - layout.minX
        val h = layout.maxY - layout.minY
        // Keep offsets even so templates stay on the full-tile grid and symmetric about the mirror axis.
        val ox = (GRID_W - w) / 2 - layout.minX
        val oy = ((GRID_H - h) / 4) * 2 - layout.minY
        val slots = layout.slots.map { Slot(it.x + ox, it.y + oy, it.z) }
            .filter { it.x in 0..GRID_W - 2 && it.y in 0..GRID_H - 2 && it.z < MAX_LAYERS }
        return slots
    }

    /** Validates and saves. Calls [onSaved] with the layout id on success. */
    fun save(onSaved: (String) -> Unit) {
        val s = _state.value
        if (s.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val normalized = normalize(s.slots)
            val error = withContext(Dispatchers.Default) { LayoutRegistry.validate(normalized) }
            if (error != null) {
                _state.update { it.copy(busy = false, message = error) }
                return@launch
            }
            val id = s.id ?: (LayoutRegistry.CUSTOM_PREFIX + System.currentTimeMillis().toString(36))
            repo.saveCustomLayout(
                CustomLayoutData(
                    id = id,
                    name = s.name.ifBlank { "My layout" },
                    created = System.currentTimeMillis(),
                    slots = normalized.flatMap { listOf(it.x, it.y, it.z) },
                )
            )
            _state.update { it.copy(busy = false, id = id, message = "Saved") }
            onSaved(id)
        }
    }

    fun shareCode(): String = com.ezmanw.mathong.data.ShareCodes.encode(_state.value.name, normalize(_state.value.slots))

    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.id ?: run { onDeleted(); return }
        viewModelScope.launch {
            repo.deleteCustomLayout(id)
            onDeleted()
        }
    }

    /** Shifts the layout to the origin and drops empty layers. */
    private fun normalize(slots: List<Slot>): List<Slot> {
        if (slots.isEmpty()) return slots
        val minX = slots.minOf { it.x }
        val minY = slots.minOf { it.y }
        val zs = slots.map { it.z }.distinct().sorted()
        return slots.map { Slot(it.x - minX, it.y - minY, zs.indexOf(it.z)) }
    }
}
