package com.nativelap.heartguard.view.component.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardInputBoundsTest {
    @Test
    fun inputBoundsTrackMultipleFieldsAndRemoveDisposedFields() {
        val bounds = KeyboardInputBounds()
        val email = Any()
        val password = Any()
        bounds.update(email, Rect(0f, 0f, 100f, 40f))
        bounds.update(password, Rect(0f, 50f, 100f, 90f))

        assertTrue(bounds.contains(Offset(20f, 20f)))
        assertTrue(bounds.contains(Offset(20f, 60f)))
        assertFalse(bounds.contains(Offset(20f, 45f)))

        bounds.remove(email)
        assertFalse(bounds.contains(Offset(20f, 20f)))
        assertTrue(bounds.contains(Offset(20f, 60f)))
    }

    @Test
    fun updatedInputBoundsReplaceOldPosition() {
        val bounds = KeyboardInputBounds()
        val input = Any()
        bounds.update(input, Rect(0f, 0f, 100f, 40f))
        bounds.update(input, Rect(0f, 80f, 100f, 120f))

        assertFalse(bounds.contains(Offset(20f, 20f)))
        assertTrue(bounds.contains(Offset(20f, 90f)))
    }
}
