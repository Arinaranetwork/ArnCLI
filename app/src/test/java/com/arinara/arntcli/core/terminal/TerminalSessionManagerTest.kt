// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import com.arinara.arntcli.ui.model.TerminalSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSessionManagerTest {

    @Test
    fun registerSession_populatesRingBuffer() {
        val manager = TerminalSessionManager(bufferCapacity = 10)
        val session = TerminalSession(
            id = "s-1",
            title = "local:sh",
            shell = "/bin/sh",
            lines = listOf("Welcome to ArnTCLI")
        )

        manager.registerSession(session)
        assertEquals(1, manager.sessions.value.size)
        assertEquals(listOf("Welcome to ArnTCLI"), manager.getLines("s-1"))
    }

    @Test
    fun appendOutput_retainsBoundedCapacity() {
        val manager = TerminalSessionManager(bufferCapacity = 3)
        val session = TerminalSession(id = "s-2", title = "sh", shell = "sh")
        manager.registerSession(session)

        manager.appendOutput("s-2", "line 1")
        manager.appendOutput("s-2", "line 2")
        manager.appendOutput("s-2", "line 3")
        manager.appendOutput("s-2", "line 4") // Overwrites line 1

        val lines = manager.getLines("s-2")
        assertEquals(3, lines.size)
        assertEquals(listOf("line 2", "line 3", "line 4"), lines)
    }

    @Test
    fun clearAndCloseSession_removesState() {
        val manager = TerminalSessionManager()
        val session = TerminalSession(id = "s-3", title = "ssh", shell = "ssh")
        manager.registerSession(session)
        manager.appendOutput("s-3", "test")

        manager.clearSession("s-3")
        assertTrue(manager.getLines("s-3").isEmpty())

        manager.closeSession("s-3")
        assertTrue(manager.sessions.value.isEmpty())
    }
}
