// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProcessEngineTest {

    @Test
    fun resolveShellBinary_returnsValidExecutableName() {
        val shell = LocalProcessEngine.resolveShellBinary()
        assertFalse("Shell binary cannot be empty", shell.isBlank())
        assertTrue(
            "Shell binary must be /system/bin/sh, cmd.exe, or sh",
            shell == "/system/bin/sh" || shell == "cmd.exe" || shell == "sh"
        )
    }

    @Test
    fun execute_echoCommand_returnsExpectedOutput() = runBlocking {
        val engine = LocalProcessEngine()
        val result = engine.execute("echo arntcli_test_ping")

        assertEquals(0, result.exitCode)
        val combinedOutput = result.output.joinToString("\n")
        assertTrue(
            "Output should contain 'arntcli_test_ping', got: $combinedOutput",
            combinedOutput.contains("arntcli_test_ping")
        )
    }

    @Test
    fun executeStreaming_echoCommand_emitsOutputLine() = runBlocking {
        val engine = LocalProcessEngine()
        val lines = engine.executeStreaming("echo stream_verify_line").toList()

        val combined = lines.joinToString("\n")
        assertTrue(
            "Streamed lines should contain 'stream_verify_line', got: $combined",
            combined.contains("stream_verify_line")
        )
    }
}
