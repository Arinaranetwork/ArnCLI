// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.export

import kotlinx.serialization.Serializable

@Serializable
data class TerminalEnvironmentConfig(
    val profileName: String,
    val shellPath: String = "/system/bin/sh",
    val workingDirectory: String = "~",
    val fontSizeSp: Int = 12,
    val fontScale: Float = 1.0f,
    val environmentVariables: Map<String, String> = emptyMap(),
    val exportTimestamp: Long = System.currentTimeMillis()
)
