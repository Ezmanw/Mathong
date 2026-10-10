package com.ezmanw.mathong.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Pure sound synthesis (no Android dependencies) so it can be tested on the JVM. */
object SfxSynth {
    const val RATE = 44100

    /** Pentatonic scale for rising combo chimes. */
    val MATCH_NOTES = listOf(783.99, 880.0, 1046.5, 1174.66, 1318.51, 1567.98)

    fun all(): Map<String, FloatArray> = buildMap {
        put("select", select())
        put("error", knock())
        put("shuffle", shuffle())
        put("win", win())
        put("hint", hint())
        put("undo", undo())
        MATCH_NOTES.forEachIndexed { i, f -> put("match$i", match(f)) }
    }

    // ---- Instruments -------------------------------------------------------------------

    private data class Mode(val ratio: Double, val amp: Double, val decay: Double)

    /** Ceramic tile partials (inharmonic, quick decay). */
    private val tileModes = listOf(
        Mode(1.0, 1.0, 55.0), Mode(1.58, 0.7, 70.0), Mode(2.31, 0.5, 90.0),
        Mode(3.41, 0.3, 120.0), Mode(4.7, 0.18, 150.0),
    )

    /** Marimba bar partials: tuned 1 : 4 : 10 with a soft mallet. */
    private val marimbaModes = listOf(Mode(1.0, 1.0, 6.5), Mode(3.99, 0.3, 20.0), Mode(10.65, 0.08, 55.0))

    private fun ring(
        out: FloatArray, start: Double, f0: Double, modes: List<Mode>, amp: Double,
        decayScale: Double = 1.0, transient: Double = 0.0, transientCutoff: Double = 0.5, seed: Int = 1,
    ) {
        val r = Random(seed)
        val offset = (start * RATE).toInt()
        val phases = modes.map { r.nextDouble() * 2 * PI }
        var lp = 0.0
        for (i in 0 until out.size - offset) {
            val t = i / RATE.toDouble()
            var v = 0.0
            modes.forEachIndexed { k, m ->
                val env = exp(-t * m.decay * decayScale)
                if (env > 1e-4) v += m.amp * env * sin(2 * PI * f0 * m.ratio * t + phases[k])
            }
            if (transient > 0 && t < 0.01) {
                lp += transientCutoff * ((r.nextDouble() * 2 - 1) - lp)
                v += transient * lp * exp(-t * 900)
            }
            val attack = (t / 0.0008).coerceAtMost(1.0)
            out[offset + i] += (v * amp * attack).toFloat()
        }
    }

    private fun buffer(seconds: Double) = FloatArray((RATE * seconds).toInt())

    private fun clack(out: FloatArray, start: Double, f0: Double, amp: Double, seed: Int) =
        ring(out, start, f0, tileModes, amp, transient = 0.9, transientCutoff = 0.55, seed = seed)

    private fun marimba(out: FloatArray, start: Double, f0: Double, amp: Double) =
        ring(out, start, f0, marimbaModes, amp, transient = 0.25, transientCutoff = 0.2, seed = f0.toInt())

    // ---- Sounds ------------------------------------------------------------------------

    /** Light tick of a fingertip on a tile. */
    private fun select() = buffer(0.07).also {
        ring(it, 0.0, 2900.0, tileModes, 0.35, decayScale = 1.6, transient = 0.6, transientCutoff = 0.6, seed = 3)
    }.normalize(0.55)

    /** Two tiles knocking together, then a soft marimba note pitched by combo. */
    private fun match(note: Double) = buffer(0.75).also {
        clack(it, 0.0, 2150.0, 0.6, 5)
        clack(it, 0.016, 2480.0, 0.45, 9)
        marimba(it, 0.03, note, 0.22)
    }.normalize(0.8)

    /** Muted wooden knock for a blocked tile. */
    private fun knock() = buffer(0.16).also {
        ring(
            it, 0.0, 150.0, listOf(Mode(1.0, 1.0, 38.0), Mode(2.27, 0.45, 55.0), Mode(3.9, 0.2, 80.0)),
            0.8, transient = 0.5, transientCutoff = 0.08, seed = 11,
        )
    }.normalize(0.6)

    /** Tiles sliding and clicking against each other. */
    private fun shuffle() = buffer(0.62).also { out ->
        val r = Random(21)
        // Low, soft slide bed (filtered noise).
        var lp = 0.0
        for (i in out.indices) {
            val t = i / RATE.toDouble()
            lp += 0.03 * ((r.nextDouble() * 2 - 1) - lp)
            val env = sin(PI * (t / 0.6).coerceAtMost(1.0))
            out[i] += (lp * env * 0.5).toFloat()
        }
        repeat(14) { k ->
            val start = 0.02 + k * 0.038 + r.nextDouble() * 0.015
            clack(out, start, 1800.0 + r.nextDouble() * 1400, 0.18 + r.nextDouble() * 0.25, 30 + k)
        }
    }.normalize(0.6)

    /** Rising marimba arpeggio resolving to a soft chord. */
    private fun win() = buffer(2.2).also {
        listOf(523.25, 659.25, 783.99, 1046.5, 1318.5).forEachIndexed { i, f -> marimba(it, i * 0.11, f, 0.35) }
        listOf(523.25, 783.99, 1318.5).forEach { f -> marimba(it, 0.68, f, 0.28) }
    }.normalize(0.75)

    /** Two gentle notes. */
    private fun hint() = buffer(0.9).also {
        marimba(it, 0.0, 783.99, 0.3)
        marimba(it, 0.12, 1174.66, 0.26)
    }.normalize(0.5)

    /** Softer, lower tile tick. */
    private fun undo() = buffer(0.08).also {
        ring(it, 0.0, 1650.0, tileModes, 0.4, decayScale = 1.3, transient = 0.5, transientCutoff = 0.45, seed = 13)
    }.normalize(0.5)

    private fun FloatArray.normalize(peak: Double): FloatArray {
        val max = maxOf(1e-6f, maxOf { abs(it) })
        val g = (peak / max).toFloat()
        for (i in indices) this[i] *= g
        // Short fade-out so nothing clicks when truncated.
        val fade = minOf(size, (RATE * 0.01).toInt())
        for (i in 0 until fade) this[size - 1 - i] *= i / fade.toFloat()
        return this
    }

}
