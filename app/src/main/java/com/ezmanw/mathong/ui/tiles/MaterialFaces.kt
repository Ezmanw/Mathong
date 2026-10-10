package com.ezmanw.mathong.ui.tiles

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import com.ezmanw.mathong.game.Suit
import com.ezmanw.mathong.game.TileKind
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Flat, two-tone Material faces. Every symbol is a soft container tone with a strong accent
 * tone on top, using colors harmonized to the current color scheme.
 */
internal fun DrawScope.drawMaterialFace(kind: TileKind, o: Offset, s: Size, glyphs: GlyphCache, style: TileStyle) {
    val pad = s.width * 0.12f
    val box = FaceBox(Offset(o.x + pad, o.y + pad), Size(s.width - pad * 2, s.height - pad * 2))
    val c = style.suits
    when (kind.suit) {
        Suit.DOTS -> materialDots(kind.rank, box, c)
        Suit.BAMBOO -> materialBamboo(kind.rank, box, c)
        Suit.CHARACTERS -> {
            text(box, kind.rank.toString(), 0.36f, 0.56f, c.red.accent, glyphs)
            badge(box, "萬", c.red, glyphs)
        }
        Suit.WIND -> materialWind(kind.rank, box, c.neutral, glyphs)
        Suit.DRAGON -> materialDragon(kind.rank, box, c, glyphs)
        Suit.FLOWER -> {
            val pair = listOf(c.red, c.purple, c.orange, c.green)[kind.rank - 1]
            blossom(box.at(0.5f, 0.38f), box.w * 0.42f, pair)
            badge(box, kind.rank.toString(), pair, glyphs)
        }
        Suit.SEASON -> {
            when (kind.rank) {
                1 -> sprout(box, 0.08f, c.green)
                2 -> sun(box.at(0.5f, 0.38f), box.w * 0.42f, c.orange)
                3 -> {
                    val leafPair = c.red
                    drawLeaf(box.at(0.28f, 0.62f), box.at(0.78f, 0.12f), 0.32f, leafPair)
                }
                else -> snowflake(box.at(0.5f, 0.38f), box.w * 0.42f, c.blue)
            }
            badge(box, kind.rank.toString(), listOf(c.green, c.orange, c.red, c.blue)[kind.rank - 1], glyphs)
        }
    }
    if (style.showIndex && (kind.suit == Suit.DOTS || kind.suit == Suit.BAMBOO)) {
        // Small rank label in the corner; other suits already carry their label.
        val pair = if (kind.suit == Suit.DOTS) c.blue else c.green
        val t = glyphs.layout(kind.rank.toString(), s.height * 0.19f, pair.accent, density)
        val r = t.size.height * 0.5f
        val center = Offset(o.x + s.width * 0.05f + r, o.y + s.width * 0.05f + r)
        drawCircle(style.palette.faceTop, r, center)
        drawText(t, topLeft = Offset(center.x - t.size.width / 2, center.y - t.size.height / 2))
    }
}

private class FaceBox(val o: Offset, val size: Size) {
    val w get() = size.width
    val h get() = size.height
    fun at(x: Float, y: Float) = Offset(o.x + w * x, o.y + h * y)
}

private fun DrawScope.text(box: FaceBox, t: String, cy: Float, frac: Float, color: androidx.compose.ui.graphics.Color, glyphs: GlyphCache) {
    var layout = glyphs.layout(t, box.h * frac, color, density)
    if (layout.size.width > box.w) layout = glyphs.layout(t, box.h * frac * box.w / layout.size.width, color, density)
    drawText(layout, topLeft = Offset(box.o.x + (box.w - layout.size.width) / 2, box.o.y + box.h * cy - layout.size.height / 2))
}

/** Pill-shaped label at the bottom of the face: container fill, accent text. */
private fun DrawScope.badge(box: FaceBox, label: String, pair: TonalPair, glyphs: GlyphCache) {
    val bw = box.w * 0.78f
    val bh = box.h * 0.24f
    val tl = Offset(box.o.x + (box.w - bw) / 2, box.o.y + box.h - bh)
    drawRoundRect(pair.container, tl, Size(bw, bh), CornerRadius(bh / 2))
    val t = glyphs.layout(label, bh * 0.78f, pair.accent, density)
    drawText(t, topLeft = Offset(tl.x + (bw - t.size.width) / 2, tl.y + (bh - t.size.height) / 2))
}

private fun DrawScope.dot(c: Offset, r: Float, pair: TonalPair) {
    drawCircle(pair.container, r, c)
    drawCircle(pair.accent, r * 0.46f, c)
}

private fun DrawScope.materialDots(rank: Int, box: FaceBox, c: SuitColors) {
    val b = c.blue; val r = c.red; val g = c.green
    if (rank == 1) {
        val center = box.at(0.5f, 0.5f)
        val rad = box.w * 0.46f
        drawCircle(b.container, rad, center)
        drawCircle(b.accent, rad * 0.72f, center, style = Stroke(rad * 0.12f))
        drawCircle(r.container, rad * 0.46f, center)
        drawCircle(r.accent, rad * 0.24f, center)
        return
    }
    val rad = box.w * when (rank) { 2, 3 -> 0.21f; 4, 5, 6 -> 0.19f; else -> 0.15f }
    data class D(val x: Float, val y: Float, val p: TonalPair)
    val dots = when (rank) {
        2 -> listOf(D(.5f, .26f, g), D(.5f, .74f, b))
        3 -> listOf(D(.24f, .2f, b), D(.5f, .5f, r), D(.76f, .8f, g))
        4 -> listOf(D(.27f, .27f, b), D(.73f, .27f, g), D(.27f, .73f, g), D(.73f, .73f, b))
        5 -> listOf(D(.24f, .22f, b), D(.76f, .22f, g), D(.5f, .5f, r), D(.24f, .78f, g), D(.76f, .78f, b))
        6 -> listOf(D(.3f, .18f, g), D(.7f, .18f, g)) + listOf(.5f, .82f).flatMap { y -> listOf(D(.3f, y, r), D(.7f, y, r)) }
        7 -> listOf(D(.2f, .12f, g), D(.5f, .22f, g), D(.8f, .32f, g)) + listOf(.6f, .86f).flatMap { y -> listOf(D(.3f, y, r), D(.7f, y, r)) }
        8 -> listOf(.13f, .38f, .62f, .87f).flatMap { y -> listOf(D(.3f, y, b), D(.7f, y, b)) }
        else -> listOf(.17f to b, .5f to r, .83f to g).flatMap { (y, p) -> listOf(D(.18f, y, p), D(.5f, y, p), D(.82f, y, p)) }
    }
    dots.forEach { dot(box.at(it.x, it.y), rad, it.p) }
}

private fun DrawScope.stick(center: Offset, w: Float, h: Float, pair: TonalPair) {
    drawRoundRect(pair.container, Offset(center.x - w / 2, center.y - h / 2), Size(w, h), CornerRadius(w / 2))
    val band = w * 0.42f
    drawRoundRect(pair.accent, Offset(center.x - w / 2, center.y - band / 2), Size(w, band), CornerRadius(band / 2))
}

private fun DrawScope.materialBamboo(rank: Int, box: FaceBox, c: SuitColors) {
    if (rank == 1) { sprout(box, 0f, c.green); return }
    val g = c.green; val b = c.blue; val r = c.red
    val w = box.w * if (rank >= 7) 0.17f else 0.19f
    val h2 = box.h * 0.4f
    val h3 = box.h * 0.27f
    data class B(val x: Float, val y: Float, val p: TonalPair, val h: Float)
    val sticks = when (rank) {
        2 -> listOf(B(.5f, .25f, b, h2), B(.5f, .75f, g, h2))
        3 -> listOf(B(.5f, .25f, b, h2), B(.3f, .75f, g, h2), B(.7f, .75f, g, h2))
        4 -> listOf(B(.3f, .25f, b, h2), B(.7f, .25f, g, h2), B(.3f, .75f, g, h2), B(.7f, .75f, b, h2))
        5 -> listOf(B(.2f, .25f, g, h2), B(.8f, .25f, b, h2), B(.5f, .5f, r, h2), B(.2f, .75f, b, h2), B(.8f, .75f, g, h2))
        6 -> listOf(.25f, .75f).flatMap { y -> listOf(B(.18f, y, g, h2), B(.5f, y, b, h2), B(.82f, y, g, h2)) }
        7 -> listOf(B(.5f, .17f, r, h3)) + listOf(.5f, .83f).flatMap { y -> listOf(B(.18f, y, g, h3), B(.5f, y, b, h3), B(.82f, y, g, h3)) }
        8 -> listOf(.25f, .75f).flatMap { y -> listOf(B(.12f, y, g, h2), B(.37f, y, b, h2), B(.63f, y, b, h2), B(.88f, y, g, h2)) }
        else -> listOf(.17f, .5f, .83f).flatMap { y -> listOf(B(.18f, y, g, h3), B(.5f, y, r, h3), B(.82f, y, b, h3)) }
    }
    val sw = if (rank == 8) box.w * 0.15f else w
    sticks.forEach { stick(box.at(it.x, it.y), sw, it.h, it.p) }
}

private fun DrawScope.drawLeaf(base: Offset, tip: Offset, bulge: Float, pair: TonalPair) {
    val dx = tip.x - base.x; val dy = tip.y - base.y
    val len = sqrt(dx * dx + dy * dy)
    val nx = -dy / len * len * bulge; val ny = dx / len * len * bulge
    val mid = Offset((base.x + tip.x) / 2, (base.y + tip.y) / 2)
    val path = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(mid.x + nx, mid.y + ny, tip.x, tip.y)
        quadraticTo(mid.x - nx, mid.y - ny, base.x, base.y)
        close()
    }
    drawPath(path, pair.container)
    drawPath(path, pair.accent, style = Stroke(len * 0.06f, join = StrokeJoin.Round))
    drawLine(pair.accent, base, Offset(base.x + dx * 0.8f, base.y + dy * 0.8f), strokeWidth = len * 0.05f, cap = StrokeCap.Round)
}

/** Two-leaf sprout. [lift] raises it to leave room for a badge. */
private fun DrawScope.sprout(box: FaceBox, lift: Float, pair: TonalPair) {
    val stemBase = box.at(0.5f, 0.92f - lift * 3)
    val stemTop = box.at(0.5f, 0.5f - lift)
    drawLine(pair.accent, stemBase, stemTop, strokeWidth = box.w * 0.08f, cap = StrokeCap.Round)
    drawLeaf(box.at(0.5f, 0.6f - lift), box.at(0.06f, 0.3f - lift), 0.3f, pair)
    drawLeaf(box.at(0.5f, 0.5f - lift), box.at(0.94f, 0.12f - lift), 0.3f, pair)
}

private fun DrawScope.blossom(c: Offset, r: Float, pair: TonalPair) {
    for (k in 0 until 5) {
        val a = -Math.PI / 2 + k * 2 * Math.PI / 5
        drawCircle(pair.container, r * 0.42f, c + Offset((cos(a) * r * 0.52f).toFloat(), (sin(a) * r * 0.52f).toFloat()))
    }
    drawCircle(pair.accent, r * 0.3f, c)
}

private fun DrawScope.sun(c: Offset, r: Float, pair: TonalPair) {
    drawCircle(pair.container, r, c)
    for (k in 0 until 8) {
        val a = k * Math.PI / 4
        drawLine(
            pair.accent,
            c + Offset((cos(a) * r * 0.62f).toFloat(), (sin(a) * r * 0.62f).toFloat()),
            c + Offset((cos(a) * r * 0.86f).toFloat(), (sin(a) * r * 0.86f).toFloat()),
            strokeWidth = r * 0.13f, cap = StrokeCap.Round,
        )
    }
    drawCircle(pair.accent, r * 0.4f, c)
}

private fun DrawScope.snowflake(c: Offset, r: Float, pair: TonalPair) {
    drawCircle(pair.container, r, c)
    val sw = r * 0.11f
    for (k in 0 until 6) {
        val a = k * Math.PI / 3 - Math.PI / 2
        val end = c + Offset((cos(a) * r * 0.75f).toFloat(), (sin(a) * r * 0.75f).toFloat())
        drawLine(pair.accent, c, end, strokeWidth = sw, cap = StrokeCap.Round)
        val branch = c + Offset((cos(a) * r * 0.5f).toFloat(), (sin(a) * r * 0.5f).toFloat())
        for (side in listOf(-1, 1)) {
            val ba = a + side * Math.PI / 4
            drawLine(
                pair.accent, branch,
                branch + Offset((cos(ba) * r * 0.22f).toFloat(), (sin(ba) * r * 0.22f).toFloat()),
                strokeWidth = sw, cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.materialWind(rank: Int, box: FaceBox, pair: TonalPair, glyphs: GlyphCache) {
    val c = box.at(0.5f, 0.42f)
    val r = box.w * 0.44f
    drawCircle(pair.container, r, c)
    // Compass pointer on the rim: N up, E right, S down, W left.
    val angle = when (rank) { 1 -> 0.0; 2 -> Math.PI / 2; 3 -> Math.PI; else -> -Math.PI / 2 }
    val t = r * 0.34f
    fun p(dist: Float, side: Float): Offset {
        val ax = cos(angle).toFloat(); val ay = sin(angle).toFloat()
        return Offset(c.x + ax * dist - ay * side, c.y + ay * dist + ax * side)
    }
    val tri = Path().apply {
        val tip = p(r + t * 0.75f, 0f); val a = p(r - t * 0.35f, t * 0.7f); val b = p(r - t * 0.35f, -t * 0.7f)
        moveTo(tip.x, tip.y); lineTo(a.x, a.y); lineTo(b.x, b.y); close()
    }
    drawPath(tri, pair.accent, style = Stroke(t * 0.25f, join = StrokeJoin.Round))
    drawPath(tri, pair.accent)
    val letter = listOf("E", "S", "W", "N")[rank - 1]
    val l = glyphs.layout(letter, r * 1.15f, pair.accent, density)
    drawText(l, topLeft = Offset(c.x - l.size.width / 2, c.y - l.size.height / 2))
    text(box, listOf("EAST", "SOUTH", "WEST", "NORTH")[rank - 1], 0.93f, 0.15f, pair.accent, glyphs)
}

private fun DrawScope.materialDragon(rank: Int, box: FaceBox, c: SuitColors, glyphs: GlyphCache) {
    val side = box.w
    val tl = Offset(box.o.x, box.o.y + box.h * 0.45f - side / 2)
    val corner = CornerRadius(side * 0.26f)
    when (rank) {
        1, 2 -> {
            val pair = if (rank == 1) c.red else c.green
            drawRoundRect(pair.container, tl, Size(side, side), corner)
            val g = glyphs.layout(if (rank == 1) "中" else "發", side * 0.72f, pair.accent, density)
            drawText(g, topLeft = Offset(tl.x + (side - g.size.width) / 2, tl.y + (side - g.size.height) / 2))
        }
        else -> {
            val pair = c.blue
            drawRoundRect(pair.container, tl, Size(side, side), corner)
            val sw = side * 0.08f
            val inset = side * 0.16f
            drawRoundRect(pair.accent, tl + Offset(inset, inset), Size(side - inset * 2, side - inset * 2), CornerRadius(side * 0.14f), style = Stroke(sw))
            val inset2 = side * 0.3f
            drawRoundRect(pair.accent, tl + Offset(inset2, inset2), Size(side - inset2 * 2, side - inset2 * 2), CornerRadius(side * 0.06f), style = Stroke(sw * 0.7f))
        }
    }
    val label = listOf("RED", "GREEN", "WHITE")[rank - 1]
    text(box, label, 0.93f, 0.15f, listOf(c.red, c.green, c.blue)[rank - 1].accent, glyphs)
}
