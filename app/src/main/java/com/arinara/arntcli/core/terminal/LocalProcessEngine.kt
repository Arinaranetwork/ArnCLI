// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val output: List<String>,
    val errors: List<String>
)

/**
 * High-performance on-device shell execution engine.
 * Spawns native Linux shell processes rootlessly on Android with non-blocking stream capture.
 */
class LocalProcessEngine(
    private val workingDirectory: File? = null,
    val shellExecutable: String = resolveShellBinary()
) {

    companion object {
        fun resolveShellBinary(): String {
            val systemSh = File("/system/bin/sh")
            if (systemSh.exists() && systemSh.canExecute()) {
                return "/system/bin/sh"
            }
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
            return if (isWindows) "cmd.exe" else "sh"
        }
    }

    private fun createProcessBuilder(command: String): ProcessBuilder {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true && !File("/system/bin/sh").exists()
        val commandArgs = if (isWindows) {
            listOf(shellExecutable, "/c", command)
        } else {
            listOf(shellExecutable, "-c", command)
        }
        val builder = ProcessBuilder(commandArgs)
        workingDirectory?.let { builder.directory(it) }
        builder.environment().apply {
            put("TERM", "xterm-256color")
            put("COLORTERM", "truecolor")
            if (!containsKey("PATH")) {
                put("PATH", "/system/bin:/system/xbin:/vendor/bin:/sbin")
            }
        }
        return builder
    }

    /**
     * Executes a single command string and streams output lines asynchronously.
     */
    fun executeStreaming(command: String): Flow<String> = flow {
        val processBuilder = createProcessBuilder(command)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { emit(it) }
            }
            process.waitFor()
        } finally {
            reader.close()
            process.destroyForcibly()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes a command synchronously and returns the complete exit code and output.
     * Reads stdout and stderr concurrently to prevent pipe buffer deadlocks.
     */
    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        val processBuilder = createProcessBuilder(command)
        val process = processBuilder.start()

        try {
            val outDeferred = async(Dispatchers.IO) {
                val lines = mutableListOf<String>()
                BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { out ->
                    var line: String?
                    while (out.readLine().also { line = it } != null) {
                        line?.let { lines.add(it) }
                    }
                }
                lines
            }

            val errDeferred = async(Dispatchers.IO) {
                val lines = mutableListOf<String>()
                BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8)).use { err ->
                    var line: String?
                    while (err.readLine().also { line = it } != null) {
                        line?.let { lines.add(it) }
                    }
                }
                lines
            }

            val outputLines = outDeferred.await()
            val errorLines = errDeferred.await()
            val exitCode = process.waitFor()

            ShellResult(exitCode = exitCode, output = outputLines, errors = errorLines)
        } finally {
            process.destroyForcibly()
        }
    }

    /**
     * Executes a command and returns the combined stdout and stderr as a string.
     */
    suspend fun executeCommand(command: String): String {
        val result = execute(command)
        return (result.output + result.errors).joinToString("\n")
    }
}
