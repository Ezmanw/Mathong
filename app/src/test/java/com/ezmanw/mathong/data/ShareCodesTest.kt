package com.ezmanw.mathong.data

import com.ezmanw.mathong.game.Layouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareCodesTest {
    @Test
    fun roundTrip() {
        for (l in Layouts.all) {
            val code = ShareCodes.encode(l.name, l.slots)
            val (name, slots) = ShareCodes.decode(code)!!
            assertEquals(l.name, name)
            assertEquals(l.slots, slots)
        }
    }

    @Test
    fun rejectsGarbage() {
        assertNull(ShareCodes.decode("hello"))
        assertNull(ShareCodes.decode("MATHONG1:!!!"))
        assertNull(ShareCodes.decode("MATHONG1:AAAA"))
    }
}
