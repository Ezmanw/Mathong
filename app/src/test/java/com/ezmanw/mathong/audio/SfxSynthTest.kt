package com.ezmanw.mathong.audio

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SfxSynthTest {
    @Test
    fun soundsAreSaneAndDumpable() {
        val out: File? = File("build/sfx-dump").apply { mkdirs() }
        for ((name, samples) in SfxSynth.all()) {
            val peak = samples.maxOf { kotlin.math.abs(it) }
            assertTrue("$name silent", peak > 0.1f)
            assertTrue("$name clips", peak <= 1f)
            assertTrue("$name too long", samples.size < SfxSynth.RATE * 3)
            out?.let { File(it, "$name.raw").writeBytes(java.nio.ByteBuffer.allocate(samples.size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply { samples.forEach { s -> putFloat(s) } }.array()) }
        }
    }
}
