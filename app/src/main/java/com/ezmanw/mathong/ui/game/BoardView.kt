package com.ezmanw.mathong.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.Board
import com.ezmanw.mathong.ui.tiles.GlyphCache
import com.ezmanw.mathong.ui.tiles.TileImageCache
import com.ezmanw.mathong.ui.tiles.drawCachedTile
import com.ezmanw.mathong.ui.tiles.drawTile
import com.ezmanw.mathong.ui.tiles.rememberTileStyle
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin

private const val ASPECT = 1.32f      // tile height / width
private const val DEPTH = 0.13f       // depth as a fraction of tile width

class Flight(val progress: Animatable<Float, *>, val from: Offset, val to: Offset)

/** Zoom and pan of the board, hoisted so keyboard shortcuts and buttons can drive it. */
class BoardZoom {
    var zoom by mutableFloatStateOf(1f)
        private set
    var pan by mutableStateOf(Offset.Zero)
        private set
    var viewport = Size.Zero

    val isZoomed get() = zoom > 1.01f

    /**
     * Zooms by [factor], keeping the point [focus] (in viewport pixels, default the
     * centre) fixed on screen, then pans by [drag].
     */
    fun apply(factor: Float, focus: Offset? = null, drag: Offset = Offset.Zero) {
        val center = Offset(viewport.width / 2, viewport.height / 2)
        val f = focus ?: center
        val newZoom = (zoom * factor).coerceIn(1f, MAX_ZOOM)
        val k = newZoom / zoom
        val rel = f - center
        var p = rel - (rel - pan) * k + drag
        val maxX = viewport.width * (newZoom - 1) / 2
        val maxY = viewport.height * (newZoom - 1) / 2
        p = Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
        zoom = newZoom
        pan = p
    }

    fun reset() { zoom = 1f; pan = Offset.Zero }

    companion object { const val MAX_ZOOM = 3.5f }
}

@Composable
fun rememberBoardZoom() = remember { BoardZoom() }

/** Pixel geometry for drawing a layout into a given box. */
class BoardMetrics(board: Board, width: Float, height: Float, zoomPad: Float, maxTileW: Float = Float.MAX_VALUE) {
    private val layout = board.layout
    val tileW: Float
    val tileH: Float
    val depth: Float
    private val ox: Float
    private val oy: Float

    init {
        val unitsW = (layout.maxX - layout.minX) / 2f
        val unitsH = (layout.maxY - layout.minY) / 2f
        val layers = layout.maxZ + 1
        val wFit = (width - zoomPad * 2) / (unitsW + DEPTH * (layers + 1))
        val hFit = (height - zoomPad * 2) / (unitsH * ASPECT + DEPTH * (layers + 1))
        // Cap the size so tiles don't balloon on big tablets and desktop monitors.
        tileW = min(min(wFit, hFit), maxTileW)
        tileH = tileW * ASPECT
        depth = tileW * DEPTH
        val totalW = unitsW * tileW + depth * (layers + 1)
        val totalH = unitsH * tileH + depth * (layers + 1)
        // Higher layers shift up-left, so reserve the shift on the left/top.
        ox = (width - totalW) / 2 + depth * layers
        oy = (height - totalH) / 2 + depth * layers
    }

    fun faceOrigin(slot: Int): Offset {
        val s = layout.slots[slot]
        return Offset(
            ox + (s.x - layout.minX) * tileW / 2 - s.z * depth,
            oy + (s.y - layout.minY) * tileH / 2 - s.z * depth,
        )
    }

    val faceSize get() = Size(tileW, tileH)
}

@Composable
fun BoardView(
    board: Board,
    selected: Int?,
    hint: Pair<Int, Int>?,
    shakeTile: Int?,
    shakeKey: Int,
    settings: Settings,
    dealId: Long = 0L,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((Int) -> Unit)? = null,
    zoomState: BoardZoom = rememberBoardZoom(),
    /** Tile focused by keyboard navigation. */
    cursor: Int? = null,
    /** Replay recorded tile layers. Screenshot tests turn this off (layoutlib scales offscreen layers). */
    cacheTiles: Boolean = true,
) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val glyphs = remember(measurer) { GlyphCache(measurer) }
    val style = rememberTileStyle(settings)
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // Slot order for drawing: lower layers first, then left-to-right, top-to-bottom.
    val drawOrder = remember(board.layout) {
        board.layout.slots.indices.sortedWith(compareBy({ board.layout.slots[it].z }, { board.layout.slots[it].x }, { board.layout.slots[it].y }))
    }

    // Animate tiles that just disappeared: a matched pair slides together, lifts and fades.
    val flying = remember { mutableStateMapOf<Int, Flight>() }
    var previous by remember { mutableStateOf<BooleanArray?>(null) }
    val metricsHolder = remember { arrayOfNulls<BoardMetrics>(1) }
    LaunchedEffect(board) {
        val prev = previous
        previous = board.present.copyOf()
        val m = metricsHolder[0]
        if (prev != null && prev.size == board.present.size && m != null) {
            val gone = prev.indices.filter { prev[it] && !board.present[it] }
            if (gone.size == 2) {
                val (a, b) = gone.sortedBy { m.faceOrigin(it).x }
                val oa = m.faceOrigin(a); val ob = m.faceOrigin(b)
                val mid = (oa + ob) / 2f
                val ta = mid - Offset(m.tileW * 0.52f, m.tileH * 0.15f)
                val tb = mid + Offset(m.tileW * 0.52f, -m.tileH * 0.15f)
                listOf(a to ta, b to tb).forEach { (i, target) ->
                    val anim = Animatable(0f)
                    flying[i] = Flight(anim, m.faceOrigin(i), target)
                    scope.launch {
                        anim.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                        flying.remove(i)
                    }
                }
            } else {
                gone.forEach { i ->
                    val anim = Animatable(0f)
                    flying[i] = Flight(anim, m.faceOrigin(i), m.faceOrigin(i) - Offset(0f, m.tileH * 0.3f))
                    scope.launch {
                        anim.animateTo(1f, tween(320))
                        flying.remove(i)
                    }
                }
            }
        }
    }
    val freeSet = remember(board) { board.freeTiles().toHashSet() }
    val matchSet = remember(board, selected, settings.showMatches) {
        if (selected == null || !settings.showMatches || !board.present[selected]) emptySet()
        else freeSet.filter { it != selected && board.kind(it).matches(board.kind(selected)) }.toSet()
    }

    // Deal-in animation: tiles drop onto the table layer by layer.
    val deal = remember { Animatable(1f) }
    val dealRank = remember(board.layout) {
        val order = drawOrder
        IntArray(order.size).also { r -> order.forEachIndexed { k, slot -> r[slot] = k } }
    }
    LaunchedEffect(dealId) {
        if (dealId != 0L) {
            deal.snapTo(0f)
            deal.animateTo(1f, tween(900, easing = LinearEasing))
        }
    }

    // Shake a blocked tile that was tapped.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeTile != null) {
            shake.snapTo(1f)
            shake.animateTo(0f, tween(380, easing = LinearEasing))
        }
    }

    // Hint pulse only animates while a hint is showing, so an idle board never redraws.
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(hint) {
        if (hint == null) return@LaunchedEffect
        while (true) {
            pulseAnim.animateTo(0.35f, tween(600))
            pulseAnim.animateTo(1f, tween(600))
        }
    }

    // Tile currently under the finger, drawn pressed for instant feedback.
    var pressed by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(board.layout) { zoomState.reset() }
    // Tile under the mouse pointer (desktop / ChromeOS / stylus hover).
    var hovered by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        zoomState.viewport = Size(w, h)
        val metrics = remember(board.layout, w, h) {
            BoardMetrics(board, w, h, with(density) { 8.dp.toPx() }, maxTileW = with(density) { 104.dp.toPx() })
        }
        metricsHolder[0] = metrics
        val graphicsContext = androidx.compose.ui.platform.LocalGraphicsContext.current
        val tileImages = remember(metrics, style, glyphs) {
            TileImageCache(graphicsContext, glyphs, metrics.faceSize, metrics.depth, style, density)
        }
        androidx.compose.runtime.DisposableEffect(tileImages) { onDispose { tileImages.release() } }

        val freeCount = freeSet.size
        val remainingPairs = board.remaining / 2
        val selectedName = selected?.let { board.kind(it).displayName }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = buildString {
                        append("Mahjong board, $remainingPairs pairs left, $freeCount free tiles.")
                        if (selectedName != null) append(" Selected: $selectedName.")
                    }
                }
                .pointerInput(w, h) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var moved = 0f
                        do {
                            val event = awaitPointerEvent()
                            val multi = event.changes.count { it.pressed } >= 2
                            val p = event.calculatePan()
                            moved += p.getDistance()
                            if (multi || (zoomState.isZoomed && moved > viewConfiguration.touchSlop)) {
                                zoomState.apply(event.calculateZoom(), event.calculateCentroid(useCurrent = true).takeIf { it.isSpecified }, p)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    // Mouse wheel / trackpad scroll zooms toward the pointer.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.first()
                                val dy = change.scrollDelta.y
                                if (dy != 0f) {
                                    zoomState.apply(if (dy < 0) 1.15f else 1 / 1.15f, change.position)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = zoomState.zoom; scaleY = zoomState.zoom
                    translationX = zoomState.pan.x; translationY = zoomState.pan.y
                }
                .pointerInput(board, metrics) {
                    // Topmost tile under the finger wins; a near miss picks the closest tile.
                    fun hit(pos: Offset, slop: Float): Int? {
                        for (k in drawOrder.indices.reversed()) {
                            val i = drawOrder[k]
                            if (!board.present[i]) continue
                            val o = metrics.faceOrigin(i)
                            if (pos.x >= o.x - slop && pos.x <= o.x + metrics.tileW + metrics.depth + slop &&
                                pos.y >= o.y - slop && pos.y <= o.y + metrics.tileH + metrics.depth + slop
                            ) return i
                        }
                        return null
                    }
                    fun find(pos: Offset) = hit(pos, 0f) ?: hit(pos, metrics.tileW * 0.25f)
                    // No double-tap handler: it would delay every single tap.
                    detectTapGestures(
                        onPress = { pos ->
                            pressed = find(pos)
                            tryAwaitRelease()
                            pressed = null
                        },
                        onLongPress = { pos -> find(pos)?.let { onLongPress?.invoke(it) } },
                        onTap = { pos -> find(pos)?.let(onTap) },
                    )
                }
                .pointerInput(board, metrics) {
                    // Hover highlight and right-click inspect for mouse and stylus.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.type == PointerType.Touch) continue
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> hovered = hitTest(board, metrics, drawOrder, change.position)
                                PointerEventType.Exit -> hovered = null
                                PointerEventType.Press -> if (event.buttons.isSecondaryPressed) {
                                    hitTest(board, metrics, drawOrder, change.position)?.let { onLongPress?.invoke(it) }
                                    change.consume()
                                }
                                else -> Unit
                            }
                        }
                    }
                }
                .pointerHoverIcon(if (hovered?.let { freeSet.contains(it) } == true) PointerIcon.Hand else PointerIcon.Default),
        ) {
            val size = metrics.faceSize
            for (i in drawOrder) {
                if (!board.present[i]) continue
                var origin = metrics.faceOrigin(i)
                var dealAlpha = 1f
                if (deal.value < 1f) {
                    val delay = dealRank[i].toFloat() / dealRank.size * 0.6f
                    val t = ((deal.value - delay) / 0.4f).coerceIn(0f, 1f)
                    val e = 1 - t
                    origin -= Offset(0f, e * e * metrics.tileH * 1.2f)
                    dealAlpha = t
                }
                if (shakeTile == i && shake.value > 0f) {
                    origin += Offset(sin(shake.value * 30f) * metrics.tileW * 0.08f * shake.value, 0f)
                }
                if (pressed == i) origin += Offset(metrics.depth * 0.45f, metrics.depth * 0.45f)
                val highlight = when {
                    selected == i -> colors.primary
                    hint?.let { it.first == i || it.second == i } == true -> colors.tertiary.copy(alpha = pulseAnim.value)
                    i in matchSet -> colors.secondary
                    else -> null
                }
                val dim = if (settings.highlightFree && highlight == null && i !in freeSet) 0.22f else 0f
                if (hovered == i && highlight == null && i in freeSet) origin -= Offset(0f, metrics.depth * 0.25f)
                val d = if (pressed == i) dim + 0.08f else dim
                if (cacheTiles && settings.fastRendering && dealAlpha >= 0.99f) {
                    drawCachedTile(tileImages, board.kind(i), origin, size, highlight, d)
                } else {
                    drawTile(board.kind(i), origin, size, metrics.depth, style, glyphs, highlight, d, dealAlpha)
                }
            }
            // Keyboard focus ring (Material focus indicator: thick outline outside the tile).
            cursor?.takeIf { board.present.getOrElse(it) { false } }?.let { c ->
                val o = metrics.faceOrigin(c)
                val inset = metrics.tileW * 0.06f
                drawRoundRect(
                    colors.secondary,
                    o - Offset(inset, inset),
                    Size(size.width + inset * 2, size.height + inset * 2),
                    androidx.compose.ui.geometry.CornerRadius(size.width * 0.26f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(metrics.tileW * 0.07f),
                )
            }
            // Removed tiles fly above the board.
            for ((i, f) in flying) {
                if (board.present[i]) continue
                val t = f.progress.value
                val move = (t / 0.6f).coerceAtMost(1f)
                val fade = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                val origin = f.from + (f.to - f.from) * move - Offset(0f, metrics.tileH * 0.25f * fade)
                drawTile(board.kind(i), origin, size, metrics.depth, style, glyphs, colors.primary.copy(alpha = 1f - fade), 0f, 1f - fade)
            }
        }
        if (zoomState.isZoomed) {
            androidx.compose.material3.SmallFloatingActionButton(
                onClick = { zoomState.reset() },
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(8.dp),
            ) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.ZoomOutMap, "Reset zoom") }
        }
    }
}


/** Topmost present tile under [pos], or null. */
internal fun hitTest(board: Board, metrics: BoardMetrics, drawOrder: List<Int>, pos: Offset, slop: Float = 0f): Int? {
    for (k in drawOrder.indices.reversed()) {
        val i = drawOrder[k]
        if (!board.present[i]) continue
        val o = metrics.faceOrigin(i)
        if (pos.x >= o.x - slop && pos.x <= o.x + metrics.tileW + metrics.depth + slop &&
            pos.y >= o.y - slop && pos.y <= o.y + metrics.tileH + metrics.depth + slop
        ) return i
    }
    return null
}
