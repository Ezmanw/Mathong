package com.ezmanw.mathong.ui.tiles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ezmanw.mathong.data.TileTheme
import com.ezmanw.mathong.game.Suit
import com.ezmanw.mathong.game.TileKind
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class TilePalette(
    val faceTop: Color,
    val faceBottom: Color,
    val side: Color,
    val sideDark: Color,
    val border: Color,
)

fun TileTheme.palette(): TilePalette = when (this) {
    TileTheme.JADE -> TilePalette(Color(0xFFFFFCF2), Color(0xFFF1E9D2), Color(0xFF2E8B62), Color(0xFF1B5A3F), Color(0xFFCBBF9F))
    TileTheme.IVORY -> TilePalette(Color(0xFFFFFEF9), Color(0xFFF3ECDC), Color(0xFFE0D2AE), Color(0xFFB09D72), Color(0xFFCDBF9B))
    TileTheme.CRIMSON -> TilePalette(Color(0xFFFFFCF4), Color(0xFFF2E8D3), Color(0xFFB8403B), Color(0xFF7A2421), Color(0xFFCFBF9C))
    TileTheme.SAPPHIRE -> TilePalette(Color(0xFFFCFDFF), Color(0xFFE9EDF2), Color(0xFF3466B0), Color(0xFF1E3F74), Color(0xFFBFC8D3))
    TileTheme.AMBER -> TilePalette(Color(0xFFFFFCF2), Color(0xFFF4E8CC), Color(0xFFE2A43A), Color(0xFFA06A17), Color(0xFFD1BE93))
    TileTheme.ONYX -> TilePalette(Color(0xFFF7F5EF), Color(0xFFE6E1D5), Color(0xFF444444), Color(0xFF1F1F1F), Color(0xFFBDB6A6))
}

private val Red = Color(0xFFC62828)
private val Green = Color(0xFF2E7D32)
private val Blue = Color(0xFF1F4E9C)
private val Ink = Color(0xFF1B1B2F)
private val Purple = Color(0xFF7B3FA0)
private val Orange = Color(0xFFDB7A1A)

/** Caches text layouts across frames; tiles redraw often during animations. */
class GlyphCache(private val measurer: TextMeasurer) {
    private val cache = HashMap<Triple<String, Int, Long>, TextLayoutResult>()

    fun layout(text: String, sizePx: Float, color: Color, density: Float, bold: Boolean = true): TextLayoutResult {
        val bucket = sizePx.roundToInt().coerceAtLeast(1)
        val key = Triple(if (bold) text else "~$text", bucket, color.value.toLong())
        return cache.getOrPut(key) {
            measurer.measure(
                text,
                TextStyle(
                    color = color,
                    fontSize = (bucket / density).sp,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
    }

    fun clear() = cache.clear()
}

/** Draws the face artwork of [kind] into the rect at [o] with size [s]. */
fun DrawScope.drawTileFace(kind: TileKind, o: Offset, s: Size, glyphs: GlyphCache, showIndex: Boolean, simple: Boolean = false) {
    val pad = s.width * 0.1f
    val inner = Size(s.width - pad * 2, s.height - pad * 2)
    val io = Offset(o.x + pad, o.y + pad)
    if (simple && drawSimpleFace(kind, io, inner, glyphs)) {
        if (showIndex && (kind.suit == Suit.DOTS || kind.suit == Suit.BAMBOO)) drawIndex(kind, o, s, glyphs)
        return
    }
    when (kind.suit) {
        Suit.DOTS -> drawDots(kind.rank, io, inner)
        Suit.BAMBOO -> drawBamboo(kind.rank, io, inner)
        Suit.CHARACTERS -> {
            glyphCentered(NUMERALS[kind.rank - 1], io, inner, 0.32f, 0.42f, Ink, glyphs)
            glyphCentered("萬", io, inner, 0.72f, 0.44f, Red, glyphs)
        }
        Suit.WIND -> glyphCentered(WINDS[kind.rank - 1], io, inner, 0.5f, 0.7f, Ink, glyphs)
        Suit.DRAGON -> when (kind.rank) {
            1 -> glyphCentered("中", io, inner, 0.5f, 0.72f, Red, glyphs)
            2 -> glyphCentered("發", io, inner, 0.5f, 0.72f, Green, glyphs)
            else -> {
                val w = inner.width * 0.82f; val h = inner.height * 0.82f
                val tl = Offset(io.x + (inner.width - w) / 2, io.y + (inner.height - h) / 2)
                val sw = inner.width * 0.07f
                drawRoundRect(Blue, tl, Size(w, h), CornerRadius(sw * 1.5f), style = Stroke(sw))
                val g = sw * 2.2f
                drawRoundRect(Blue, tl + Offset(g, g), Size(w - 2 * g, h - 2 * g), CornerRadius(sw), style = Stroke(sw * 0.6f))
            }
        }
        Suit.FLOWER -> {
            val c = listOf(Red, Purple, Orange, Green)[kind.rank - 1]
            drawBlossom(Offset(io.x + inner.width * 0.5f, io.y + inner.height * 0.25f), inner.width * 0.22f, c)
            glyphCentered(FLOWERS[kind.rank - 1], io, inner, 0.7f, 0.5f, c, glyphs)
            cornerNumber(kind.rank, io, inner, c, glyphs)
        }
        Suit.SEASON -> {
            val c = listOf(Green, Red, Orange, Blue)[kind.rank - 1]
            drawSun(Offset(io.x + inner.width * 0.5f, io.y + inner.height * 0.24f), inner.width * 0.16f, c)
            glyphCentered(SEASONS[kind.rank - 1], io, inner, 0.7f, 0.5f, c, glyphs)
            cornerNumber(kind.rank, io, inner, c, glyphs)
        }
    }
    if (showIndex) drawIndex(kind, o, s, glyphs)
}

/**
 * Western-style faces for characters, honours and bonus tiles. Dots and bamboo keep their
 * pictures but gain a rank number. Returns false when the traditional face should be drawn.
 */
private fun DrawScope.drawSimpleFace(kind: TileKind, io: Offset, inner: Size, glyphs: GlyphCache): Boolean {
    fun label(text: String, cy: Float, frac: Float, color: Color, bold: Boolean = true) {
        val t = glyphs.layout(text, inner.height * frac, color, density, bold)
        val scale = (inner.width / t.size.width).coerceAtMost(1f)
        if (scale < 1f) {
            val t2 = glyphs.layout(text, inner.height * frac * scale, color, density, bold)
            drawText(t2, topLeft = Offset(io.x + (inner.width - t2.size.width) / 2, io.y + inner.height * cy - t2.size.height / 2))
        } else {
            drawText(t, topLeft = Offset(io.x + (inner.width - t.size.width) / 2, io.y + inner.height * cy - t.size.height / 2))
        }
    }
    when (kind.suit) {
        Suit.DOTS, Suit.BAMBOO -> return false
        Suit.CHARACTERS -> {
            label(kind.rank.toString(), 0.4f, 0.62f, Red)
            label("CRAK", 0.86f, 0.17f, Ink)
        }
        Suit.WIND -> {
            label(listOf("E", "S", "W", "N")[kind.rank - 1], 0.4f, 0.62f, Ink)
            label(listOf("EAST", "SOUTH", "WEST", "NORTH")[kind.rank - 1], 0.86f, 0.17f, Blue)
        }
        Suit.DRAGON -> {
            when (kind.rank) {
                1 -> { drawCircle(Red, inner.width * 0.42f, Offset(io.x + inner.width / 2, io.y + inner.height * 0.4f)); label("R", 0.4f, 0.5f, Color.White) }
                2 -> { drawCircle(Green, inner.width * 0.42f, Offset(io.x + inner.width / 2, io.y + inner.height * 0.4f)); label("G", 0.4f, 0.5f, Color.White) }
                else -> {
                    val c = Offset(io.x + inner.width / 2, io.y + inner.height * 0.4f)
                    drawCircle(Blue, inner.width * 0.42f, c, style = Stroke(inner.width * 0.08f))
                    label("W", 0.4f, 0.45f, Blue)
                }
            }
            label("DRAGON", 0.86f, 0.15f, Ink)
        }
        Suit.FLOWER -> {
            val c = listOf(Red, Purple, Orange, Green)[kind.rank - 1]
            drawBlossom(Offset(io.x + inner.width * 0.5f, io.y + inner.height * 0.38f), inner.width * 0.45f, c)
            label(listOf("PLUM", "ORCHID", "MUM", "BAMBOO")[kind.rank - 1], 0.86f, 0.15f, c)
        }
        Suit.SEASON -> {
            val c = listOf(Green, Red, Orange, Blue)[kind.rank - 1]
            drawSun(Offset(io.x + inner.width * 0.5f, io.y + inner.height * 0.38f), inner.width * 0.3f, c)
            label(listOf("SPRING", "SUMMER", "AUTUMN", "WINTER")[kind.rank - 1], 0.86f, 0.15f, c)
        }
    }
    return true
}

private val NUMERALS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
private val WINDS = listOf("東", "南", "西", "北")
private val FLOWERS = listOf("梅", "蘭", "菊", "竹")
private val SEASONS = listOf("春", "夏", "秋", "冬")

fun TileKind.shortLabel(): String = when (suit) {
    Suit.DOTS, Suit.BAMBOO, Suit.CHARACTERS -> rank.toString()
    Suit.WIND -> listOf("E", "S", "W", "N")[rank - 1]
    Suit.DRAGON -> listOf("R", "G", "Wh")[rank - 1]
    Suit.FLOWER -> "F"
    Suit.SEASON -> "S"
}

internal fun DrawScope.drawIndex(kind: TileKind, o: Offset, s: Size, glyphs: GlyphCache) {
    val label = kind.shortLabel()
    val color = when (kind.suit) {
        Suit.DOTS -> Blue; Suit.BAMBOO -> Green; Suit.CHARACTERS -> Red; else -> Ink
    }
    val t = glyphs.layout(label, s.height * 0.2f, color, density)
    val bgW = t.size.width + s.width * 0.08f
    val bgH = t.size.height * 0.9f
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(o.x + s.width * 0.03f, o.y + s.width * 0.03f), Size(bgW, bgH), CornerRadius(bgH / 3))
    drawText(t, topLeft = Offset(o.x + s.width * 0.03f + (bgW - t.size.width) / 2, o.y + s.width * 0.03f + (bgH - t.size.height) / 2))
}

private fun DrawScope.cornerNumber(n: Int, io: Offset, inner: Size, c: Color, glyphs: GlyphCache) {
    val t = glyphs.layout(n.toString(), inner.height * 0.2f, c, density)
    drawText(t, topLeft = Offset(io.x + inner.width - t.size.width, io.y - inner.height * 0.04f))
}

private fun DrawScope.glyphCentered(
    text: String, io: Offset, inner: Size, cy: Float, sizeFrac: Float, color: Color, glyphs: GlyphCache,
) {
    val t = glyphs.layout(text, inner.height * sizeFrac, color, density)
    val x = io.x + (inner.width - t.size.width) / 2
    val y = io.y + inner.height * cy - t.size.height / 2
    drawText(t, topLeft = Offset(x, y))
}

private fun DrawScope.dot(c: Offset, r: Float, color: Color) {
    drawCircle(color, r, c)
    drawCircle(Color(0xFFFFFCF2), r * 0.62f, c)
    drawCircle(color, r * 0.42f, c)
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.14f, c + Offset(-r * 0.12f, -r * 0.12f))
}

private fun DrawScope.drawDots(rank: Int, io: Offset, s: Size) {
    fun p(x: Float, y: Float) = Offset(io.x + s.width * x, io.y + s.height * y)
    val r = s.width * when (rank) { 1 -> 0.42f; 2, 3 -> 0.2f; 4, 5, 6 -> 0.18f; else -> 0.145f }
    data class D(val x: Float, val y: Float, val c: Color)
    val dots: List<D> = when (rank) {
        1 -> emptyList()
        2 -> listOf(D(.5f, .26f, Green), D(.5f, .74f, Blue))
        3 -> listOf(D(.24f, .2f, Blue), D(.5f, .5f, Red), D(.76f, .8f, Green))
        4 -> listOf(D(.27f, .27f, Blue), D(.73f, .27f, Green), D(.27f, .73f, Green), D(.73f, .73f, Blue))
        5 -> listOf(D(.24f, .22f, Blue), D(.76f, .22f, Green), D(.5f, .5f, Red), D(.24f, .78f, Green), D(.76f, .78f, Blue))
        6 -> listOf(D(.3f, .18f, Green), D(.7f, .18f, Green)) +
            listOf(.5f, .82f).flatMap { y -> listOf(D(.3f, y, Red), D(.7f, y, Red)) }
        7 -> listOf(D(.2f, .12f, Green), D(.5f, .22f, Green), D(.8f, .32f, Green)) +
            listOf(.6f, .86f).flatMap { y -> listOf(D(.3f, y, Red), D(.7f, y, Red)) }
        8 -> listOf(.13f, .38f, .62f, .87f).flatMap { y -> listOf(D(.3f, y, Blue), D(.7f, y, Blue)) }
        else -> listOf(.17f to Blue, .5f to Red, .83f to Green).flatMap { (y, c) ->
            listOf(D(.18f, y, c), D(.5f, y, c), D(.82f, y, c))
        }
    }
    if (rank == 1) {
        val c = p(.5f, .5f)
        drawCircle(Green, r, c)
        drawCircle(Color(0xFFFFFCF2), r * 0.82f, c)
        // petal ring
        for (k in 0 until 12) {
            val a = k * Math.PI / 6
            drawCircle(Blue, r * 0.1f, c + Offset((cos(a) * r * 0.66f).toFloat(), (sin(a) * r * 0.66f).toFloat()))
        }
        drawCircle(Red, r * 0.42f, c)
        drawCircle(Color(0xFFFFFCF2), r * 0.22f, c)
        drawCircle(Red, r * 0.12f, c)
    } else {
        dots.forEach { dot(p(it.x, it.y), r, it.c) }
    }
}

private fun DrawScope.stick(center: Offset, w: Float, h: Float, color: Color) {
    val tl = Offset(center.x - w / 2, center.y - h / 2)
    drawRoundRect(color, tl, Size(w, h), CornerRadius(w / 2))
    // nodes
    val nodeColor = color.copy(red = color.red * 0.6f, green = color.green * 0.6f, blue = color.blue * 0.6f)
    drawLine(nodeColor, Offset(tl.x, center.y), Offset(tl.x + w, center.y), strokeWidth = w * 0.22f)
    drawLine(nodeColor, Offset(tl.x, tl.y + h * 0.15f), Offset(tl.x + w, tl.y + h * 0.15f), strokeWidth = w * 0.16f)
    drawLine(nodeColor, Offset(tl.x, tl.y + h * 0.85f), Offset(tl.x + w, tl.y + h * 0.85f), strokeWidth = w * 0.16f)
    drawLine(Color.White.copy(alpha = 0.45f), Offset(center.x - w * 0.15f, tl.y + h * 0.2f), Offset(center.x - w * 0.15f, tl.y + h * 0.8f), strokeWidth = w * 0.14f)
}

private fun DrawScope.drawBamboo(rank: Int, io: Offset, s: Size) {
    fun p(x: Float, y: Float) = Offset(io.x + s.width * x, io.y + s.height * y)
    if (rank == 1) { drawBird(io, s); return }
    val w = s.width * if (rank >= 7) 0.15f else 0.17f
    val h2 = s.height * 0.4f
    val h3 = s.height * 0.27f
    data class B(val x: Float, val y: Float, val c: Color, val h: Float)
    val sticks: List<B> = when (rank) {
        2 -> listOf(B(.5f, .25f, Blue, h2), B(.5f, .75f, Green, h2))
        3 -> listOf(B(.5f, .25f, Blue, h2), B(.3f, .75f, Green, h2), B(.7f, .75f, Green, h2))
        4 -> listOf(.25f to Blue, .75f to Green).flatMap { (y, c) -> listOf(B(.3f, y, c, h2), B(.7f, y, if (c == Blue) Green else Blue, h2)) }
        5 -> listOf(B(.22f, .25f, Green, h2), B(.78f, .25f, Blue, h2), B(.5f, .5f, Red, h2), B(.22f, .75f, Blue, h2), B(.78f, .75f, Green, h2))
        6 -> listOf(.25f, .75f).flatMap { y -> listOf(B(.2f, y, Green, h2), B(.5f, y, Blue, h2), B(.8f, y, Green, h2)) }
        7 -> listOf(B(.5f, .17f, Red, h3)) + listOf(.5f, .83f).flatMap { y -> listOf(B(.2f, y, Green, h3), B(.5f, y, Blue, h3), B(.8f, y, Green, h3)) }
        8 -> listOf(.25f, .75f).flatMap { y -> listOf(B(.14f, y, Green, h2), B(.38f, y, Blue, h2), B(.62f, y, Blue, h2), B(.86f, y, Green, h2)) }
        else -> listOf(.17f, .5f, .83f).flatMap { y -> listOf(B(.2f, y, Green, h3), B(.5f, y, Red, h3), B(.8f, y, Blue, h3)) }
    }
    sticks.forEach { stick(p(it.x, it.y), w, it.h, it.c) }
}

/** Stylised sparrow for the 1 of bamboo. */
private fun DrawScope.drawBird(io: Offset, s: Size) {
    val cx = io.x + s.width * 0.5f
    val cy = io.y + s.height * 0.55f
    val r = s.width * 0.3f
    // tail
    val tail = Path().apply {
        moveTo(cx - r * 0.4f, cy + r * 0.2f)
        lineTo(cx - r * 1.5f, cy + r * 1.3f)
        lineTo(cx - r * 0.9f, cy + r * 1.4f)
        lineTo(cx - r * 0.1f, cy + r * 0.6f)
        close()
    }
    drawPath(tail, Blue)
    drawOval(Green, Offset(cx - r, cy - r * 0.7f), Size(r * 2, r * 1.5f))
    // wing
    rotate(-20f, Offset(cx, cy)) {
        drawOval(Color(0xFF1B5E20), Offset(cx - r * 0.6f, cy - r * 0.35f), Size(r * 1.1f, r * 0.6f))
    }
    // head
    val head = Offset(cx + r * 0.75f, cy - r * 0.8f)
    drawCircle(Red, r * 0.48f, head)
    drawCircle(Color.White, r * 0.13f, head + Offset(r * 0.12f, -r * 0.08f))
    drawCircle(Ink, r * 0.07f, head + Offset(r * 0.14f, -r * 0.08f))
    val beak = Path().apply {
        moveTo(head.x + r * 0.42f, head.y - r * 0.1f)
        lineTo(head.x + r * 0.85f, head.y + r * 0.05f)
        lineTo(head.x + r * 0.42f, head.y + r * 0.2f)
        close()
    }
    drawPath(beak, Orange)
    // legs
    drawLine(Orange, Offset(cx, cy + r * 0.75f), Offset(cx - r * 0.1f, cy + r * 1.2f), strokeWidth = r * 0.1f)
    drawLine(Orange, Offset(cx + r * 0.3f, cy + r * 0.7f), Offset(cx + r * 0.3f, cy + r * 1.2f), strokeWidth = r * 0.1f)
}

private fun DrawScope.drawBlossom(c: Offset, r: Float, color: Color) {
    for (k in 0 until 5) {
        val a = -Math.PI / 2 + k * 2 * Math.PI / 5
        drawCircle(color.copy(alpha = 0.85f), r * 0.42f, c + Offset((cos(a) * r * 0.55f).toFloat(), (sin(a) * r * 0.55f).toFloat()))
    }
    drawCircle(Color(0xFFFFD54F), r * 0.3f, c)
}

private fun DrawScope.drawSun(c: Offset, r: Float, color: Color) {
    for (k in 0 until 8) {
        val a = k * Math.PI / 4
        drawLine(
            color.copy(alpha = 0.8f),
            c + Offset((cos(a) * r * 0.9f).toFloat(), (sin(a) * r * 0.9f).toFloat()),
            c + Offset((cos(a) * r * 1.4f).toFloat(), (sin(a) * r * 1.4f).toFloat()),
            strokeWidth = r * 0.22f,
        )
    }
    drawCircle(color, r * 0.7f, c)
}

/**
 * Draws a full 3D tile. [faceOrigin] is the face position; depth extends down-right by [depth].
 */
fun DrawScope.drawTile(
    kind: TileKind,
    faceOrigin: Offset,
    size: Size,
    depth: Float,
    style: TileStyle,
    glyphs: GlyphCache,
    highlight: Color? = null,
    dim: Float = 0f,
    alpha: Float = 1f,
) {
    if (alpha < 0.99f) {
        if (alpha <= 0.01f) return
        val pad = depth * 2
        val bounds = androidx.compose.ui.geometry.Rect(
            faceOrigin.x - pad, faceOrigin.y - pad,
            faceOrigin.x + size.width + depth * 2 + pad, faceOrigin.y + size.height + depth * 2 + pad,
        )
        drawContext.canvas.saveLayer(bounds, androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha })
        drawTileOpaque(kind, faceOrigin, size, depth, style, glyphs, highlight, dim)
        drawContext.canvas.restore()
    } else {
        drawTileOpaque(kind, faceOrigin, size, depth, style, glyphs, highlight, dim)
    }
}

internal fun DrawScope.drawTileOpaque(
    kind: TileKind,
    faceOrigin: Offset,
    size: Size,
    depth: Float,
    style: TileStyle,
    glyphs: GlyphCache,
    highlight: Color?,
    dim: Float,
) {
    val palette = style.palette
    val corner = CornerRadius(size.width * style.cornerFraction)
    if (style.face == com.ezmanw.mathong.data.FaceStyle.MATERIAL) {
        // Flat, two-tone body: a soft tonal shadow, an edge tone and a darker base tone.
        drawRoundRect(palette.sideDark.copy(alpha = 0.25f), faceOrigin + Offset(depth * 1.5f, depth * 1.7f), size, corner)
        drawRoundRect(palette.sideDark, faceOrigin + Offset(depth, depth), size, corner)
        drawRoundRect(palette.side, faceOrigin + Offset(depth * 0.5f, depth * 0.5f), size, corner)
        drawRoundRect(palette.faceTop, faceOrigin, size, corner)
        drawMaterialFace(kind, faceOrigin, size, glyphs, style)
    } else {
        // drop shadow
        drawRoundRect(Color.Black.copy(alpha = 0.18f), faceOrigin + Offset(depth * 1.6f, depth * 1.8f), size, corner)
        // body layers
        drawRoundRect(palette.sideDark, faceOrigin + Offset(depth, depth), size, corner)
        drawRoundRect(palette.side, faceOrigin + Offset(depth * 0.55f, depth * 0.55f), size, corner)
        drawRoundRect(palette.border, faceOrigin + Offset(depth * 0.2f, depth * 0.2f), size, corner)
        // face
        drawRoundRect(
            Brush.linearGradient(listOf(palette.faceTop, palette.faceBottom), faceOrigin, faceOrigin + Offset(size.width, size.height)),
            faceOrigin, size, corner,
        )
        drawTileFace(kind, faceOrigin, size, glyphs, style.showIndex, style.face == com.ezmanw.mathong.data.FaceStyle.SIMPLE)
    }
    drawOverlays(faceOrigin, size, corner, highlight, dim, style)
}

private fun DrawScope.drawOverlays(faceOrigin: Offset, size: Size, corner: CornerRadius, highlight: Color?, dim: Float, style: TileStyle) {
    if (highlight != null) {
        drawRoundRect(highlight.copy(alpha = 0.28f * highlight.alpha), faceOrigin, size, corner)
        drawRoundRect(highlight, faceOrigin, size, corner, style = Stroke(size.width * 0.07f))
    }
    if (dim > 0f) {
        if (style.face == com.ezmanw.mathong.data.FaceStyle.MATERIAL) {
            // Material "disabled" look: wash the content out toward the face tone.
            drawRoundRect(style.palette.faceTop.copy(alpha = (dim * 2.6f).coerceAtMost(0.62f)), faceOrigin, size, corner)
        } else {
            drawRoundRect(Color.Black.copy(alpha = dim), faceOrigin, size, corner)
        }
    }
}

/**
 * Records each tile's artwork once into a [GraphicsLayer] (a hardware display list) so the
 * board replays one recorded layer per tile per frame instead of issuing dozens of shapes
 * and glyphs. Create a new cache whenever the tile size or style changes, and call
 * [release] when done.
 */
class TileImageCache(
    private val graphics: androidx.compose.ui.graphics.GraphicsContext,
    private val glyphs: GlyphCache,
    private val tileSize: Size,
    private val depth: Float,
    val style: TileStyle,
    private val density: androidx.compose.ui.unit.Density,
) {
    private val layers = HashMap<Int, androidx.compose.ui.graphics.layer.GraphicsLayer>()
    /** Extra room around the face for the side, shadow and stroke. */
    private val pad = kotlin.math.ceil(depth * 0.5f).coerceAtLeast(2f)
    private val w = kotlin.math.ceil(tileSize.width + depth * 1.7f + pad * 2).toInt().coerceAtLeast(1)
    private val h = kotlin.math.ceil(tileSize.height + depth * 1.9f + pad * 2).toInt().coerceAtLeast(1)

    fun layer(kind: TileKind): androidx.compose.ui.graphics.layer.GraphicsLayer = layers.getOrPut(kind.id) {
        graphics.createGraphicsLayer().apply {
            topLeft = androidx.compose.ui.unit.IntOffset(-pad.toInt(), -pad.toInt())
            record(density, androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.unit.IntSize(w, h)) {
                // Note: inside record{} a bare `size` would be the layer's size, not the tile's.
                drawTileOpaque(kind, Offset(pad.toInt().toFloat(), pad.toInt().toFloat()), tileSize, depth, style, glyphs, null, 0f)
            }
        }
    }

    fun release() {
        layers.values.forEach { graphics.releaseGraphicsLayer(it) }
        layers.clear()
    }
}

/** Draws a recorded tile plus live overlays (selection, hint, dimming). */
fun DrawScope.drawCachedTile(
    cache: TileImageCache,
    kind: TileKind,
    faceOrigin: Offset,
    size: Size,
    highlight: Color? = null,
    dim: Float = 0f,
) {
    translate(faceOrigin.x, faceOrigin.y) {
        drawLayer(cache.layer(kind))
    }
    drawOverlays(faceOrigin, size, CornerRadius(size.width * cache.style.cornerFraction), highlight, dim, cache.style)
}
