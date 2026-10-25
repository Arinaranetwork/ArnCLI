// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalAnsiKeyEngineTest {

    @Test
    fun computeCtrlCode_translatesAlphabetToAsciiControlBytes() {
        assertEquals("\u0001", TerminalAnsiKeyEngine.computeCtrlCode('a')) // Ctrl+A
        assertEquals("\u0003", TerminalAnsiKeyEngine.computeCtrlCode('c')) // Ctrl+C
        assertEquals("\u0004", TerminalAnsiKeyEngine.computeCtrlCode('d')) // Ctrl+D
        assertEquals("\u000c", TerminalAnsiKeyEngine.computeCtrlCode('l')) // Ctrl+L
        assertEquals("\u001a", TerminalAnsiKeyEngine.computeCtrlCode('z')) // Ctrl+Z
    }

    @Test
    fun computeAltCode_prependsEscape() {
        assertEquals("\u001bb", TerminalAnsiKeyEngine.computeAltCode('b'))
        assertEquals("\u001bf", TerminalAnsiKeyEngine.computeAltCode('f'))
    }

    @Test
    fun standardSequences_verifyVt100Constants() {
        assertEquals("\u001b[A", TerminalAnsiKeyEngine.ARROW_UP)
        assertEquals("\u001b[B", TerminalAnsiKeyEngine.ARROW_DOWN)
        assertEquals("\u001b[C", TerminalAnsiKeyEngine.ARROW_RIGHT)
        assertEquals("\u001b[D", TerminalAnsiKeyEngine.ARROW_LEFT)
        assertEquals("\t", TerminalAnsiKeyEngine.TAB)
        assertEquals("\u001b", TerminalAnsiKeyEngine.ESCAPE)
    }
}
