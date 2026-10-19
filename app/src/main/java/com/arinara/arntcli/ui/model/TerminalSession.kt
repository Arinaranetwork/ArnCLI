// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.model

/**
 * Data model representing an active or background terminal session.
 *
 * @param id Unique identifier of the terminal session.
 * @param title Session tab label (e.g. "local:sh", "prod-ssh").
 * @param shell Shell binary or remote command path.
 * @param isConnected Whether the PTY/SSH channel is open and responsive.
 * @param lines Output line buffer rendered inside the terminal viewport.
 */
data class TerminalSession(
    val id: String,
    val title: String,
    val shell: String = "/bin/sh",
    val isConnected: Boolean = true,
    val lines: List<String> = emptyList()
)
