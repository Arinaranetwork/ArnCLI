// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

/**
 * ANSI Escape Sequences & VT100 Key Byte Generator.
 * Maps mobile on-screen hotkeys to exact terminal control sequences.
 */
object TerminalAnsiKeyEngine {

    const val ESCAPE = "\u001b"
    const val TAB = "\t"
    const val ENTER = "\r"
    const val BACKSPACE = "\u007f"

    // VT100 / ANSI Directional Arrows
    const val ARROW_UP = "\u001b[A"
    const val ARROW_DOWN = "\u001b[B"
    const val ARROW_RIGHT = "\u001b[C"
    const val ARROW_LEFT = "\u001b[D"

    // Common Control Signals
    const val CTRL_C = "\u0003" // Interrupt (SIGINT)
    const val CTRL_D = "\u0004" // End of transmission (EOF)
    const val CTRL_Z = "\u001a" // Suspend (SIGTSTP)
    const val CTRL_L = "\u000c" // Clear screen
    const val CTRL_A = "\u0001" // Beginning of line
    const val CTRL_E = "\u0005" // End of line
    const val CTRL_R = "\u0012" // Reverse search history
    const val CTRL_U = "\u0015" // Cut line before cursor
    const val CTRL_K = "\u000b" // Cut line after cursor
    const val CTRL_W = "\u0017" // Cut word before cursor

    // VT100 Function Keys F1-F12
    val FUNCTION_KEYS = mapOf(
        1 to "\u001bOP",
        2 to "\u001bOQ",
        3 to "\u001bOR",
        4 to "\u001bOS",
        5 to "\u001b[15~",
        6 to "\u001b[17~",
        7 to "\u001b[18~",
        8 to "\u001b[19~",
        9 to "\u001b[20~",
        10 to "\u001b[21~",
        11 to "\u001b[23~",
        12 to "\u001b[24~"
    )

    /**
     * Translates a character into its ASCII Control code representation (1-26).
     * For example: 'c' or 'C' -> 0x03 (Ctrl+C).
     */
    fun computeCtrlCode(char: Char): String {
        val upper = char.uppercaseChar()
        return if (upper in 'A'..'Z') {
            val code = upper.code - 64
            code.toChar().toString()
        } else {
            char.toString()
        }
    }

    /**
     * Translates a character into its Alt / Meta sequence (ESC + char).
     */
    fun computeAltCode(char: Char): String {
        return "$ESCAPE$char"
    }
}
