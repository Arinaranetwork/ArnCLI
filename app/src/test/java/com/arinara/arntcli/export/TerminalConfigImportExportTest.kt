// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalConfigImportExportTest {

    @Test
    fun exportAndImportJson_retainsExactValues() {
        val exporter = DefaultTerminalConfigExporter()
        val importer = DefaultTerminalConfigImporter()

        val original = TerminalEnvironmentConfig(
            profileName = "DevWorkspace",
            shellPath = "/bin/bash",
            workingDirectory = "/data/data/com.arinara.arntcli/files/home",
            fontSizeSp = 14,
            fontScale = 1.15f,
            environmentVariables = mapOf(
                "TERM" to "xterm-256color",
                "COLORTERM" to "truecolor"
            )
        )

        val jsonOutput = exporter.exportToJson(original)
        assertTrue(jsonOutput.contains("DevWorkspace"))
        assertTrue(jsonOutput.contains("xterm-256color"))

        val importResult = importer.importFromJson(jsonOutput)
        assertTrue(importResult.isSuccess)

        val imported = importResult.getOrThrow()
        assertEquals(original.profileName, imported.profileName)
        assertEquals(original.shellPath, imported.shellPath)
        assertEquals(original.workingDirectory, imported.workingDirectory)
        assertEquals(original.fontSizeSp, imported.fontSizeSp)
        assertEquals(original.fontScale, imported.fontScale, 0.001f)
        assertEquals(original.environmentVariables, imported.environmentVariables)
    }
}
