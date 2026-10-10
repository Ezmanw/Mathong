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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.Board
import com.ezmanw.mathong.ui.tiles.GlyphCache
import com.ezmanw.mathong.ui.tiles.drawTile
import com.ezmanw.mathong.ui.tiles.palette
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin

private const val ASPECT = 1.32f      // tile height / width
private const val DEPTH = 0.13f       // depth as a fraction of tile width

/** Pixel geometry for drawing a layout into a given box. */
class BoardMetrics(board: Board, width: Float, height: Float, zoomPad: Float) {
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
        tileW = min(wFit, hFit)
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
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val glyphs = remember(measurer) { GlyphCache(measurer) }
    val palette = settings.tileTheme.palette()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // Slot order for drawing: lower layers first, then left-to-right, top-to-bottom.
    val drawOrder = remember(board.layout) {
        board.layout.slots.indices.sortedWith(compareBy({ board.layout.slots[it].z }, { board.layout.slots[it].x }, { board.layout.slots[it].y }))
    }

    // Animate tiles that just disappeared.
    val fading = remember { mutableStateMapOf<Int, Animatable<Float, *>>() }
    var previous by remember { mutableStateOf<BooleanArray?>(null) }
    LaunchedEffect(board) {
        val prev = previous
        previous = board.present.copyOf()
        if (prev != null && prev.size == board.present.size) {
            for (i in prev.indices) if (prev[i] && !board.present[i]) {
                val a = Animatable(1f)
                fading[i] = a
                scope.launch {
                    a.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                    fading.remove(i)
                }
            }
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

    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse",
    )

    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(board.layout) { zoom = 1f; pan = Offset.Zero }

    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val metrics = remember(board.layout, w, h) { BoardMetrics(board, w, h, with(density) { 8.dp.toPx() }) }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(w, h) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var moved = 0f
                        do {
                            val event = awaitPointerEvent()
                            val multi = event.changes.count { it.pressed } >= 2
                            val p = event.calculatePan()
                            moved += p.getDistance()
                            if (multi || (zoom > 1.01f && moved > viewConfiguration.touchSlop)) {
                                val newZoom = (zoom * event.calculateZoom()).coerceIn(1f, 3.5f)
                                val maxX = w * (newZoom - 1) / 2
                                val maxY = h * (newZoom - 1) / 2
                                pan = Offset(
                                    (pan.x + p.x).coerceIn(-maxX, maxX),
                                    (pan.y + p.y).coerceIn(-maxY, maxY),
                                )
                                zoom = newZoom
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = zoom; scaleY = zoom
                    translationX = pan.x; translationY = pan.y
                }
                .pointerInput(board, metrics) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoom > 1.01f) { zoom = 1f; pan = Offset.Zero } else zoom = 2f
                        },
                        onTap = { pos ->
                            // Topmost tile under the finger wins.
                            for (k in drawOrder.indices.reversed()) {
                                val i = drawOrder[k]
                                if (!board.present[i]) continue
                                val o = metrics.faceOrigin(i)
                                if (pos.x >= o.x && pos.x <= o.x + metrics.tileW + metrics.depth &&
                                    pos.y >= o.y && pos.y <= o.y + metrics.tileH + metrics.depth
                                ) { onTap(i); break }
                            }
                        },
                    )
                },
        ) {
            val size = metrics.faceSize
            for (i in drawOrder) {
                val present = board.present[i]
                val fade = fading[i]
                if (!present && fade == null) continue
                var origin = metrics.faceOrigin(i)
                var alpha = 1f
                if (!present && fade != null) {
                    val v = fade.value
                    alpha = v
                    origin += Offset(0f, -(1 - v) * metrics.tileH * 0.35f)
                }
                if (shakeTile == i && shake.value > 0f) {
                    origin += Offset(sin(shake.value * 30f) * metrics.tileW * 0.08f * shake.value, 0f)
                }
                val highlight = when {
                    !present -> null
                    selected == i -> colors.primary
                    hint?.let { it.first == i || it.second == i } == true -> colors.tertiary.copy(alpha = pulse)
                    else -> null
                }
                val dim = if (present && settings.highlightFree && highlight == null && !board.isFree(i)) 0.22f else 0f
                drawTile(
                    kind = board.kind(i),
                    faceOrigin = origin,
                    size = size,
                    depth = metrics.depth,
                    palette = palette,
                    glyphs = glyphs,
                    showIndex = settings.showIndices,
                    highlight = highlight,
                    dim = dim,
                    alpha = alpha,
                )
            }
        }
    }
}

