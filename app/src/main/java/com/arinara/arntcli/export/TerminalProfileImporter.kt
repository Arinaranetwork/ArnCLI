// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.export

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

interface TerminalConfigImporter {
    fun importFromJson(rawJson: String): Result<TerminalEnvironmentConfig>
    fun importFromFile(source: File): Result<TerminalEnvironmentConfig>
    fun importFromStream(inputStream: InputStream): Result<TerminalEnvironmentConfig>
}

class DefaultTerminalConfigImporter(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TerminalConfigImporter {

    override fun importFromJson(rawJson: String): Result<TerminalEnvironmentConfig> {
        return runCatching {
            json.decodeFromString<TerminalEnvironmentConfig>(rawJson)
        }
    }

    override fun importFromFile(source: File): Result<TerminalEnvironmentConfig> {
        return runCatching {
            require(source.exists()) { "Source file does not exist: ${source.absolutePath}" }
            val rawJson = source.readText(Charsets.UTF_8)
            json.decodeFromString<TerminalEnvironmentConfig>(rawJson)
        }
    }

    override fun importFromStream(inputStream: InputStream): Result<TerminalEnvironmentConfig> {
        return runCatching {
            val rawJson = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString<TerminalEnvironmentConfig>(rawJson)
        }
    }
}
