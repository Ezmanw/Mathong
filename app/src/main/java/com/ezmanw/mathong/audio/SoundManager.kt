package com.ezmanw.mathong.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class Sfx { SELECT, MATCH, ERROR, SHUFFLE, WIN, HINT, UNDO }

/**
 * Sound effects built with modal synthesis: each sound is a handful of exponentially
 * decaying, inharmonic resonances plus a short strike transient — the way real ceramic
 * tiles and marimba bars ring — so the app ships without audio assets.
 */
class SoundManager(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = HashMap<String, Int>()
    var enabled = true

    init {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        dir.listFiles()?.filter { !it.name.endsWith("_$VERSION.wav") }?.forEach { it.delete() }
        val sounds = SfxSynth.all()
        for ((name, samples) in sounds) {
            val f = File(dir, "${name}_$VERSION.wav")
            if (!f.exists()) writeWav(f, samples)
            ids[name] = pool.load(f.path, 1)
        }
    }

    /** [level] picks the pitch of the match chime (combo level, 1-based). */
    fun play(sfx: Sfx, volume: Float = 1f, level: Int = 1) {
        if (!enabled) return
        val key = when (sfx) {
            Sfx.SELECT -> "select"
            Sfx.MATCH -> "match${(level - 1).coerceIn(0, MATCH_NOTES.lastIndex)}"
            Sfx.ERROR -> "error"
            Sfx.SHUFFLE -> "shuffle"
            Sfx.WIN -> "win"
            Sfx.HINT -> "hint"
            Sfx.UNDO -> "undo"
        }
        ids[key]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() = pool.release()

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

    companion object {
        const val RATE = SfxSynth.RATE
        private const val VERSION = "v2"
        private val MATCH_NOTES = SfxSynth.MATCH_NOTES
    }
}
