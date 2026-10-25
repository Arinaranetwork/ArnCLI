// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VncSessionControllerTest {

    @Test
    fun initialCoordinates_startAtDisplayCenter() {
        val controller = VncSessionController(serverWidth = 1920, serverHeight = 1080)
        assertEquals(960f, controller.pointerX.value, 0.01f)
        assertEquals(540f, controller.pointerY.value, 0.01f)
        assertEquals(DesktopConnectionStatus.Disconnected, controller.status.value)
    }

    @Test
    fun movePointerDelta_translatesAndClampsWithinResolution() {
        val controller = VncSessionController(serverWidth = 1000, serverHeight = 800)
        // Center is 500, 400
        controller.movePointerDelta(50f, -50f)
        assertEquals(550f, controller.pointerX.value, 0.01f)
        assertEquals(350f, controller.pointerY.value, 0.01f)

        // Clamp beyond upper bounds
        controller.movePointerDelta(1000f, 1000f)
        assertEquals(1000f, controller.pointerX.value, 0.01f)
        assertEquals(800f, controller.pointerY.value, 0.01f)

        // Clamp beyond lower bounds
        controller.movePointerDelta(-2000f, -2000f)
        assertEquals(0f, controller.pointerX.value, 0.01f)
        assertEquals(0f, controller.pointerY.value, 0.01f)
    }

    @Test
    fun toggleModifier_addsAndRemovesState() {
        val controller = VncSessionController()
        assertTrue(controller.activeModifiers.value.isEmpty())

        controller.toggleModifier("CTRL")
        assertTrue(controller.activeModifiers.value.contains("CTRL"))

        controller.toggleModifier("ALT")
        assertTrue(controller.activeModifiers.value.contains("CTRL"))
        assertTrue(controller.activeModifiers.value.contains("ALT"))

        controller.toggleModifier("CTRL")
        assertFalse(controller.activeModifiers.value.contains("CTRL"))
        assertTrue(controller.activeModifiers.value.contains("ALT"))
    }

    @Test
    fun sendKey_includesActiveModifiersInAction() {
        val controller = VncSessionController()
        controller.toggleModifier("CTRL")
        controller.toggleModifier("ALT")
        controller.sendKey("DELETE")

        assertTrue(controller.lastAction.value.contains("CTRL+ALT+DELETE") || controller.lastAction.value.contains("ALT+CTRL+DELETE"))
    }
}
