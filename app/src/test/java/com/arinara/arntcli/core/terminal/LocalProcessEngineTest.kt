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
import java.io.File
import java.nio.file.Files

class LocalProcessEngineTest {

    @Test
    fun resolveShellBinary_returnsValidExecutableName() {
        val shell = LocalProcessEngine.resolveShellBinary()
        assertFalse("Shell binary cannot be empty", shell.isBlank())
        assertTrue(
            "Shell binary must be /system/bin/sh, /bin/sh, cmd.exe, or sh",
            shell == "/system/bin/sh" || shell == "/bin/sh" || shell == "cmd.exe" || shell == "sh"
        )
    }

    @Test
    fun execute_echoCommand_returnsExpectedOutput() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_test_home").toFile()
        try {
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val result = engine.execute("echo arntcli_test_ping")

            assertEquals(0, result.exitCode)
            val combinedOutput = result.output.joinToString("\n")
            assertTrue(
                "Output should contain 'arntcli_test_ping', got: $combinedOutput",
                combinedOutput.contains("arntcli_test_ping")
            )
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun executeStreaming_echoCommand_emitsOutputLine() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_stream_home").toFile()
        try {
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val lines = engine.executeStreaming("echo stream_verify_line").toList()

            val combined = lines.joinToString("\n")
            assertTrue(
                "Streamed lines should contain 'stream_verify_line', got: $combined",
                combined.contains("stream_verify_line")
            )
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun statefulCwd_cdAndPwd_persistsAcrossCommandsAndSupportsPreviousDir() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_cwd_home").toFile()
        try {
            val subDir = File(tempHome, "workspace_alpha").apply { mkdirs() }
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val sessionId = "test-cwd-session"

            assertEquals("~", engine.getFormattedPromptCwd(sessionId))

            val cdResult = engine.execute("cd workspace_alpha", sessionId)
            assertEquals(0, cdResult.exitCode)
            assertEquals("~/workspace_alpha", engine.getFormattedPromptCwd(sessionId))

            val pwdResult = engine.execute("pwd", sessionId)
            assertEquals(0, pwdResult.exitCode)
            assertEquals(subDir.canonicalPath, pwdResult.output.first().trim())

            // Test cd - returns to previous directory (~)
            val cdBackResult = engine.execute("cd -", sessionId)
            assertEquals(0, cdBackResult.exitCode)
            assertEquals("~", engine.getFormattedPromptCwd(sessionId))
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun statefulEnv_exportPrintenvAndUnset_persistsAcrossCommands() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_env_home").toFile()
        try {
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val sessionId = "test-env-session"

            val exportRes = engine.execute("export ARN_TOKEN=\"slate_99\"", sessionId)
            assertEquals(0, exportRes.exitCode)

            val printRes = engine.execute("printenv ARN_TOKEN", sessionId)
            assertEquals(0, printRes.exitCode)
            assertEquals("slate_99", printRes.output.first().trim())

            val echoRes = engine.execute("echo \$ARN_TOKEN", sessionId)
            assertEquals(0, echoRes.exitCode)
            assertTrue(echoRes.output.joinToString("\n").contains("slate_99"))

            engine.execute("unset ARN_TOKEN", sessionId)
            val afterUnset = engine.execute("printenv ARN_TOKEN", sessionId)
            assertEquals(1, afterUnset.exitCode)
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun history_tracksExecutedCommandsAndClearsWithFlag() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_hist_home").toFile()
        try {
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val sessionId = "test-hist-session"

            engine.execute("pwd", sessionId)
            engine.execute("export MODE=prod", sessionId)
            val histResult = engine.execute("history", sessionId)

            assertEquals(0, histResult.exitCode)
            assertTrue(histResult.output.any { it.contains("pwd") })
            assertTrue(histResult.output.any { it.contains("export MODE=prod") })

            engine.execute("history -c", sessionId)
            assertTrue(engine.getHistory(sessionId).isEmpty())
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun completeInput_completesBuiltinCommandsAndDirectoryPaths() {
        val tempHome = Files.createTempDirectory("arntcli_tab_home").toFile()
        try {
            File(tempHome, "deploy_script.sh").writeText("#!/bin/sh\necho ok")
            File(tempHome, "configs").mkdirs()

            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val sessionId = "test-tab-session"

            val cmdCompletion = engine.completeInput("his", sessionId)
            assertEquals("history ", cmdCompletion.completedInput)

            val fileCompletion = engine.completeInput("cat dep", sessionId)
            assertEquals("cat deploy_script.sh ", fileCompletion.completedInput)

            val dirCompletion = engine.completeInput("cd conf", sessionId)
            assertEquals("cd configs/", dirCompletion.completedInput)
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun helpAndWhichBuiltins_returnExpectedReferenceAndBuiltinType() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_help_home").toFile()
        try {
            val engine = LocalProcessEngine(workingDirectory = tempHome)
            val helpRes = engine.execute("help")
            assertEquals(0, helpRes.exitCode)
            assertTrue(helpRes.output.joinToString("\n").contains("Built-in Commands:"))

            val whichRes = engine.execute("which ll cd")
            assertEquals(0, whichRes.exitCode)
            assertTrue(whichRes.output.any { it.contains("aliased to `ls -la`") })
            assertTrue(whichRes.output.any { it.contains("shell built-in command") })
        } finally {
            tempHome.deleteRecursively()
        }
    }

    @Test
    fun persistedSessionState_restoresCwdEnvAndHistoryAcrossEngineInstances() = runBlocking {
        val tempHome = Files.createTempDirectory("arntcli_persist_home").toFile()
        try {
            val subDir = File(tempHome, "persisted_dir").apply { mkdirs() }
            val sessionId = "session-persistent-1"

            // First engine instance mutates cwd, exports env var, and runs commands
            val engine1 = LocalProcessEngine(workingDirectory = tempHome)
            engine1.execute("cd persisted_dir", sessionId)
            engine1.execute("export PERSISTED_KEY=\"survives_restart\"", sessionId)
            engine1.execute("pwd", sessionId)

            // Second engine instance simulates cold app restart pointing to same home directory
            val engine2 = LocalProcessEngine(workingDirectory = tempHome)
            assertEquals("~/persisted_dir", engine2.getFormattedPromptCwd(sessionId))

            val envRes = engine2.execute("printenv PERSISTED_KEY", sessionId)
            assertEquals(0, envRes.exitCode)
            assertEquals("survives_restart", envRes.output.first().trim())

            val restoredHistory = engine2.getHistory(sessionId)
            assertTrue(restoredHistory.contains("cd persisted_dir"))
            assertTrue(restoredHistory.contains("export PERSISTED_KEY=\"survives_restart\""))

            val cdPrevRes = engine2.execute("cd -", sessionId)
            assertEquals(0, cdPrevRes.exitCode)
            assertEquals("~", engine2.getFormattedPromptCwd(sessionId))
        } finally {
            tempHome.deleteRecursively()
        }
    }
}
