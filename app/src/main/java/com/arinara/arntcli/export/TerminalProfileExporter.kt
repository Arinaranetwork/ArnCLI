// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.export

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

interface TerminalConfigExporter {
    fun exportToJson(config: TerminalEnvironmentConfig): String
    fun exportToFile(config: TerminalEnvironmentConfig, destination: File): Boolean
    fun exportToStream(config: TerminalEnvironmentConfig, outputStream: OutputStream): Boolean
}

class DefaultTerminalConfigExporter(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : TerminalConfigExporter {

    override fun exportToJson(config: TerminalEnvironmentConfig): String {
        return json.encodeToString(config)
    }

    override fun exportToFile(config: TerminalEnvironmentConfig, destination: File): Boolean {
        return try {
            val content = exportToJson(config)
            destination.parentFile?.mkdirs()
            destination.writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun exportToStream(config: TerminalEnvironmentConfig, outputStream: OutputStream): Boolean {
        return try {
            val content = exportToJson(config)
            outputStream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            true
        } catch (_: Exception) {
            false
        }
    }
}
