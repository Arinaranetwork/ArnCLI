// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arinara.arntcli.export.DefaultTerminalConfigExporter
import com.arinara.arntcli.export.DefaultTerminalConfigImporter
import com.arinara.arntcli.ui.scaffold.MainScaffold
import com.arinara.arntcli.ui.theme.ArnTCLITheme

class MainActivity : ComponentActivity() {

    private val profileExporter = DefaultTerminalConfigExporter()
    private val profileImporter = DefaultTerminalConfigImporter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArnTCLITheme {
                MainScaffold(
                    onExportProfiles = {
                        // Centralized profile export contract
                    },
                    onImportProfiles = {
                        // Centralized profile import contract
                    }
                )
            }
        }
    }
}
