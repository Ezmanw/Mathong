package com.ezmanw.mathong.data

import com.ezmanw.mathong.game.Slot
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** Compact text codes for sharing custom layouts, e.g. `MATHONG1:...`. */
object ShareCodes {
    private const val PREFIX = "MATHONG1:"

    fun encode(name: String, slots: List<Slot>): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF(name.take(24))
            out.writeShort(slots.size)
            for (s in slots) { out.writeByte(s.x); out.writeByte(s.y); out.writeByte(s.z) }
        }
        return PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
    }

    /** Returns the decoded name and slots, or null for an invalid code. */
    fun decode(code: String): Pair<String, List<Slot>>? = runCatching {
        val trimmed = code.trim().substringAfter(PREFIX, missingDelimiterValue = "")
        if (trimmed.isEmpty()) return null
        val bytes = java.util.Base64.getUrlDecoder().decode(trimmed)
        DataInputStream(bytes.inputStream()).use { input ->
            val name = input.readUTF()
            val n = input.readUnsignedShort()
            if (n !in 2..144) return null
            val slots = List(n) { Slot(input.readUnsignedByte(), input.readUnsignedByte(), input.readUnsignedByte()) }
            if (slots.any { it.x > 60 || it.y > 60 || it.z > 10 } || slots.toSet().size != n) return null
            name to slots
        }
    }.getOrNull()
}
