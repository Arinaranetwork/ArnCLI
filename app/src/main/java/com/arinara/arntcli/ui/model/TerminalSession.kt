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
 * @param title Session tab label (e.g. "local:sh", "ssh:prod-db").
 * @param shell Shell binary or remote command path.
 * @param isConnected Whether the PTY/SSH channel is open and responsive.
 * @param lines Output line buffer rendered inside the terminal viewport.
 * @param cwd Current working directory display string (e.g. "~", "~/logs", "/system/bin").
 * @param isRunning Whether a foreground command is actively streaming in this session.
 * @param history Executed command history for Up/Down arrow navigation.
 * @param hostAddress Optional remote target hostname or IP address bound to this session.
 * @param hostPort Optional remote target port bound to this session.
 * @param hostUser Optional remote target username bound to this session.
 * @param hostProtocol Optional remote protocol (e.g. "SSH", "Telnet") bound to this session.
 */
data class TerminalSession(
    val id: String,
    val title: String,
    val shell: String = "/bin/sh",
    val isConnected: Boolean = true,
    val lines: List<String> = emptyList(),
    val cwd: String = "~",
    val isRunning: Boolean = false,
    val history: List<String> = emptyList(),
    val hostAddress: String? = null,
    val hostPort: Int? = null,
    val hostUser: String? = null,
    val hostProtocol: String? = null
)
