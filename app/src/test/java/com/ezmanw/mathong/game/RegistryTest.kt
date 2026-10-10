package com.ezmanw.mathong.game

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import com.ezmanw.mathong.data.toLayout

class RegistryTest {
    @Test
    fun validatesShapes() {
        assertNull(LayoutRegistry.validate(Layouts.pyramid.slots))
        assertNotNull(LayoutRegistry.validate(listOf(Slot(0, 0, 0), Slot(2, 0, 0), Slot(4, 0, 0))))
        // Four tiles stacked in one column can never be paired.
        assertNotNull(LayoutRegistry.validate((0 until 4).map { Slot(0, 0, it) }))
        assertNotNull(LayoutRegistry.validate(List(146) { Slot((it % 20) * 2, (it / 20) * 2, 0) }))
    }

    @Test
    fun customFromData() {
        val data = com.ezmanw.mathong.data.CustomLayoutData("custom_x", "X", 0, listOf(0, 0, 0, 2, 0, 0, 4, 0, 0, 6, 0, 0))
        val layout = data.toLayout()!!
        LayoutRegistry.setCustom(listOf(layout))
        assertNotNull(LayoutRegistry.find("custom_x"))
        LayoutRegistry.setCustom(emptyList())
    }
}
