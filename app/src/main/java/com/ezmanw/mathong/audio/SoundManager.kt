package com.ezmanw.mathong.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { SELECT, MATCH, ERROR, SHUFFLE, WIN, HINT, UNDO }

/** Plays small synthesized sound effects, so the app ships without audio assets. */
class SoundManager(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = HashMap<Sfx, Int>()
    var enabled = true

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        for (sfx in Sfx.entries) {
            val f = File(dir, "${sfx.name.lowercase()}_v1.wav")
            if (!f.exists()) writeWav(f, synth(sfx))
            ids[sfx] = pool.load(f.path, 1)
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!enabled) return
        ids[sfx]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    private fun synth(sfx: Sfx): FloatArray = when (sfx) {
        Sfx.SELECT -> clack(0.05, 2200.0, 0.5)
        Sfx.MATCH -> mix(clack(0.09, 1600.0, 0.6), tone(0.18, listOf(880.0, 1320.0), 0.25, 18.0))
        Sfx.ERROR -> tone(0.18, listOf(180.0, 190.0), 0.4, 12.0)
        Sfx.SHUFFLE -> FloatArray((RATE * 0.45).toInt()).also { out ->
            val r = Random(4)
            for (k in 0 until 9) {
                val c = clack(0.04, 1400.0 + r.nextDouble() * 1200, 0.35)
                val off = (k * RATE * 0.045).toInt()
                for (i in c.indices) if (off + i < out.size) out[off + i] += c[i]
            }
        }
        Sfx.WIN -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), 0.11, 0.35)
        Sfx.HINT -> tone(0.25, listOf(1046.5, 1568.0), 0.25, 10.0)
        Sfx.UNDO -> tone(0.12, listOf(660.0, 440.0), 0.25, 20.0)
    }

    private fun clack(dur: Double, freq: Double, amp: Double): FloatArray {
        val n = (RATE * dur).toInt()
        val r = Random(freq.toInt())
        return FloatArray(n) { i ->
            val t = i / RATE.toDouble()
            val env = exp(-t * 90)
            ((sin(2 * PI * freq * t) * 0.6 + (r.nextDouble() * 2 - 1) * 0.4) * env * amp).toFloat()
        }
    }

    private fun tone(dur: Double, freqs: List<Double>, amp: Double, decay: Double): FloatArray {
        val n = (RATE * dur).toInt()
        return FloatArray(n) { i ->
            val t = i / RATE.toDouble()
            // glide between the given frequencies over the duration
            val p = t / dur * (freqs.size - 1)
            val idx = p.toInt().coerceAtMost(freqs.size - 2).coerceAtLeast(0)
            val f = if (freqs.size == 1) freqs[0] else freqs[idx] + (freqs[idx + 1] - freqs[idx]) * (p - idx).coerceIn(0.0, 1.0)
            val attack = (t / 0.005).coerceAtMost(1.0)
            (sin(2 * PI * f * t) * exp(-t * decay) * attack * amp).toFloat()
        }
    }

    private fun arpeggio(notes: List<Double>, step: Double, amp: Double): FloatArray {
        val out = FloatArray((RATE * (step * notes.size + 0.6)).toInt())
        notes.forEachIndexed { k, f ->
            val t = tone(0.6, listOf(f), amp, 6.0)
            val off = (k * step * RATE).toInt()
            for (i in t.indices) if (off + i < out.size) out[off + i] += t[i]
        }
        return out
    }

    private fun mix(a: FloatArray, b: FloatArray) = FloatArray(maxOf(a.size, b.size)) { i ->
        (if (i < a.size) a[i] else 0f) + (if (i < b.size) b[i] else 0f)
    }

    private fun writeWav(file: File, samples: FloatArray) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.capacity()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(RATE); putInt(RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.capacity())
        }
        FileOutputStream(file).use { it.write(header.array()); it.write(data.array()) }
    }

    fun release() = pool.release()

    companion object { const val RATE = 22050 }
}
