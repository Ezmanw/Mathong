package com.ezmanw.mathong.ui.tiles

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Minimal CIELAB / LCh helpers used to build Material-style tonal colors. Lab lightness is
 * what Material's HCT "tone" is based on, so tone 40 / tone 90 pairs land where M3 puts
 * a color role and its container.
 */
object ColorMath {
    data class Lch(val l: Double, val c: Double, val h: Double)

    private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun gam(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055

    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883

    private fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
    private fun fi(t: Double) = if (t * t * t > 216.0 / 24389) t * t * t else (116 * t - 16) / (24389.0 / 27)

    fun toLch(color: Color): Lch {
        val r = lin(color.red.toDouble()); val g = lin(color.green.toDouble()); val b = lin(color.blue.toDouble())
        val x = 0.4124 * r + 0.3576 * g + 0.1805 * b
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val z = 0.0193 * r + 0.1192 * g + 0.9505 * b
        val fx = f(x / XN); val fy = f(y / YN); val fz = f(z / ZN)
        val l = 116 * fy - 16
        val a = 500 * (fx - fy)
        val bb = 200 * (fy - fz)
        var h = Math.toDegrees(atan2(bb, a))
        if (h < 0) h += 360
        return Lch(l, hypot(a, bb), h)
    }

    /** Converts LCh to sRGB, reducing chroma until the color is in gamut. */
    fun fromLch(l: Double, c: Double, h: Double): Color {
        var chroma = c
        while (true) {
            val rgb = lchToRgb(l, chroma, h)
            if (rgb != null || chroma <= 0.5) {
                val (r, g, b) = rgb ?: lchToRgb(l, 0.0, h)!!
                return Color(r.toFloat().coerceIn(0f, 1f), g.toFloat().coerceIn(0f, 1f), b.toFloat().coerceIn(0f, 1f))
            }
            chroma *= 0.94
        }
    }

    private fun lchToRgb(l: Double, c: Double, h: Double): Triple<Double, Double, Double>? {
        val hr = Math.toRadians(h)
        val a = c * cos(hr); val b = c * sin(hr)
        val fy = (l + 16) / 116
        val fx = fy + a / 500
        val fz = fy - b / 200
        val x = XN * fi(fx); val y = YN * fi(fy); val z = ZN * fi(fz)
        val r = gam(3.2406 * x - 1.5372 * y - 0.4986 * z)
        val g = gam(-0.9689 * x + 1.8758 * y + 0.0415 * z)
        val bl = gam(0.0557 * x - 0.2040 * y + 1.0570 * z)
        val eps = 0.001
        if (r < -eps || r > 1 + eps || g < -eps || g > 1 + eps || bl < -eps || bl > 1 + eps) return null
        return Triple(r, g, bl)
    }

    /** Rotates [hue] up to 15° toward [towards], like Material's color harmonization. */
    fun harmonize(hue: Double, towards: Double): Double {
        var diff = towards - hue
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        val shift = (diff * 0.5).coerceIn(-15.0, 15.0)
        return (hue + shift + 360) % 360
    }
}

/** A Material-style color role: strong [accent] tone plus a soft [container] tone. */
data class TonalPair(val accent: Color, val container: Color)
