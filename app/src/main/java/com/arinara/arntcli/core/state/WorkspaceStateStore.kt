// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.state

import com.arinara.arntcli.core.tunnel.TunnelRule
import com.arinara.arntcli.core.tunnel.TunnelStatus
import com.arinara.arntcli.core.tunnel.TunnelType
import com.arinara.arntcli.ui.model.CredentialType
import com.arinara.arntcli.ui.model.HostItem
import com.arinara.arntcli.ui.model.Space
import com.arinara.arntcli.ui.model.TerminalSession
import com.arinara.arntcli.ui.model.VaultCredential
import java.io.File
import java.util.Base64

/**
 * Immutable snapshot of the complete ArnTCLI application workspace state.
 */
data class WorkspaceSnapshot(
    val spaces: List<Space>,
    val selectedSpaceId: String,
    val hosts: List<HostItem>,
    val credentials: List<VaultCredential>,
    val tunnelRules: List<TunnelRule>,
    val sessions: List<TerminalSession>,
    val selectedSessionIndex: Int = 0,
    val sessionSequence: Int = 2
)

/**
 * Atomic, zero-dependency persistent state store for ArnTCLI.
 * Saves and restores Spaces, Hosts, encrypted Vault Credentials, Port Forwarding Rules,
 * and active Terminal Sessions (including scrollback buffers, CWD, and command history)
 * across app restarts and configuration changes.
 */
class WorkspaceStateStore(
    private val stateDirectory: File
) {
    init {
        stateDirectory.mkdirs()
    }

    val primaryStateFile: File
        get() = File(stateDirectory, "workspace_state.arnstate")

    val defaultBackupFile: File
        get() = File(stateDirectory, "arncli_profile_backup.arnstate")

    companion object {
        private const val HEADER_MAGIC = "ARNCLI_STATE_V1"
        private const val MAX_PERSISTED_LINES_PER_SESSION = 500
        private const val MAX_PERSISTED_HISTORY_PER_SESSION = 300

        private fun encode(value: String?): String {
            if (value == null) return "~NULL~"
            return Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        }

        private fun decode(encoded: String): String? {
            if (encoded == "~NULL~") return null
            return String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        }

        private fun decodeNonNull(encoded: String, fallback: String = ""): String {
            return decode(encoded) ?: fallback
        }
    }

    /**
     * Serializes a [WorkspaceSnapshot] into a deterministic line-oriented format.
     */
    fun serialize(snapshot: WorkspaceSnapshot): String {
        val sb = StringBuilder()
        sb.append(HEADER_MAGIC).append('\n')
        sb.append("META|")
            .append(encode(snapshot.selectedSpaceId)).append('|')
            .append(snapshot.selectedSessionIndex).append('|')
            .append(snapshot.sessionSequence).append('\n')

        for (space in snapshot.spaces) {
            sb.append("SPACE|")
                .append(encode(space.id)).append('|')
                .append(encode(space.name)).append('|')
                .append(encode(space.description)).append('|')
                .append(space.hostCount).append('|')
                .append(space.tunnelCount).append('|')
                .append(space.isVpnActive).append('|')
                .append(space.isSelected).append('\n')
        }

        for (host in snapshot.hosts) {
            sb.append("HOST|")
                .append(encode(host.id)).append('|')
                .append(encode(host.name)).append('|')
                .append(encode(host.address)).append('|')
                .append(host.port).append('|')
                .append(encode(host.username)).append('|')
                .append(encode(host.protocol)).append('|')
                .append(encode(host.keyAlias)).append('|')
                .append(encode(host.jumpHost)).append('|')
                .append(encode(host.spaceId)).append('\n')
        }

        for (cred in snapshot.credentials) {
            sb.append("CRED|")
                .append(encode(cred.id)).append('|')
                .append(encode(cred.title)).append('|')
                .append(encode(cred.type.name)).append('|')
                .append(encode(cred.description)).append('|')
                .append(encode(cred.spaceId)).append('|')
                .append(encode(cred.encryptedSecret)).append('|')
                .append(cred.createdAt).append('\n')
        }

        for (rule in snapshot.tunnelRules) {
            sb.append("TUNNEL|")
                .append(encode(rule.id)).append('|')
                .append(encode(rule.name)).append('|')
                .append(encode(rule.type.name)).append('|')
                .append(rule.localPort).append('|')
                .append(encode(rule.remoteHost)).append('|')
                .append(rule.remotePort).append('|')
                .append(encode(rule.hostId)).append('|')
                .append(encode(rule.status.name)).append('\n')
        }

        for (session in snapshot.sessions) {
            val boundedLines = session.lines.takeLast(MAX_PERSISTED_LINES_PER_SESSION)
            val encodedLines = boundedLines.joinToString(",") { encode(it) }
            val boundedHistory = session.history.takeLast(MAX_PERSISTED_HISTORY_PER_SESSION)
            val encodedHistory = boundedHistory.joinToString(",") { encode(it) }

            sb.append("SESSION|")
                .append(encode(session.id)).append('|')
                .append(encode(session.title)).append('|')
                .append(encode(session.shell)).append('|')
                .append(session.isConnected).append('|')
                .append(encode(session.cwd)).append('|')
                .append(encode(session.hostAddress)).append('|')
                .append(session.hostPort ?: -1).append('|')
                .append(encode(session.hostUser)).append('|')
                .append(encode(session.hostProtocol)).append('|')
                .append(encodedLines).append('|')
                .append(encodedHistory).append('\n')
        }

        return sb.toString()
    }

    /**
     * Deserializes a [WorkspaceSnapshot] from its string representation, or returns null if invalid.
     */
    fun deserialize(payload: String): WorkspaceSnapshot? {
        val lines = payload.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty() || lines.first() != HEADER_MAGIC) return null

        var selectedSpaceId = "default"
        var selectedSessionIndex = 0
        var sessionSequence = 2
        val spaces = mutableListOf<Space>()
        val hosts = mutableListOf<HostItem>()
        val credentials = mutableListOf<VaultCredential>()
        val tunnelRules = mutableListOf<TunnelRule>()
        val sessions = mutableListOf<TerminalSession>()

        for (line in lines.drop(1)) {
            val parts = line.split('|')
            if (parts.isEmpty()) continue
            try {
                when (parts[0]) {
                    "META" -> {
                        if (parts.size >= 4) {
                            selectedSpaceId = decodeNonNull(parts[1], "default")
                            selectedSessionIndex = parts[2].toIntOrNull() ?: 0
                            sessionSequence = parts[3].toIntOrNull() ?: 2
                        }
                    }

                    "SPACE" -> {
                        if (parts.size >= 8) {
                            spaces.add(
                                Space(
                                    id = decodeNonNull(parts[1]),
                                    name = decodeNonNull(parts[2]),
                                    description = decodeNonNull(parts[3]),
                                    hostCount = parts[4].toIntOrNull() ?: 0,
                                    tunnelCount = parts[5].toIntOrNull() ?: 0,
                                    isVpnActive = parts[6].toBooleanStrictOrNull() ?: false,
                                    isSelected = parts[7].toBooleanStrictOrNull() ?: false
                                )
                            )
                        }
                    }

                    "HOST" -> {
                        if (parts.size >= 10) {
                            hosts.add(
                                HostItem(
                                    id = decodeNonNull(parts[1]),
                                    name = decodeNonNull(parts[2]),
                                    address = decodeNonNull(parts[3]),
                                    port = parts[4].toIntOrNull() ?: 22,
                                    username = decodeNonNull(parts[5]),
                                    protocol = decodeNonNull(parts[6], "SSH"),
                                    keyAlias = decode(parts[7]),
                                    jumpHost = decode(parts[8]),
                                    spaceId = decodeNonNull(parts[9], "default")
                                )
                            )
                        }
                    }

                    "CRED" -> {
                        if (parts.size >= 8) {
                            val typeName = decodeNonNull(parts[3], CredentialType.SshKey.name)
                            val credType = try {
                                CredentialType.valueOf(typeName)
                            } catch (_: Exception) {
                                CredentialType.SshKey
                            }
                            credentials.add(
                                VaultCredential(
                                    id = decodeNonNull(parts[1]),
                                    title = decodeNonNull(parts[2]),
                                    type = credType,
                                    description = decodeNonNull(parts[4]),
                                    spaceId = decodeNonNull(parts[5], "default"),
                                    encryptedSecret = decodeNonNull(parts[6]),
                                    createdAt = parts[7].toLongOrNull() ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    "TUNNEL" -> {
                        if (parts.size >= 9) {
                            val typeName = decodeNonNull(parts[3], TunnelType.Local.name)
                            val tunnelType = try {
                                TunnelType.valueOf(typeName)
                            } catch (_: Exception) {
                                TunnelType.Local
                            }
                            val statusName = decodeNonNull(parts[8], TunnelStatus.Inactive.name)
                            val savedStatus = try {
                                TunnelStatus.valueOf(statusName)
                            } catch (_: Exception) {
                                TunnelStatus.Inactive
                            }
                            val localPort = (parts[4].toIntOrNull() ?: 8080).coerceIn(1, 65535)
                            val remotePort = (parts[6].toIntOrNull() ?: 80).coerceIn(1, 65535)
                            val remoteHost = decodeNonNull(parts[5], "localhost").ifBlank { "localhost" }

                            tunnelRules.add(
                                TunnelRule(
                                    id = decodeNonNull(parts[1]),
                                    name = decodeNonNull(parts[2]),
                                    type = tunnelType,
                                    localPort = localPort,
                                    remoteHost = remoteHost,
                                    remotePort = remotePort,
                                    hostId = decodeNonNull(parts[7]),
                                    // Reset transient socket status to Inactive on cold boot unless re-bound
                                    status = if (savedStatus == TunnelStatus.Connecting) TunnelStatus.Inactive else savedStatus
                                )
                            )
                        }
                    }

                    "SESSION" -> {
                        if (parts.size >= 12) {
                            val rawPort = parts[7].toIntOrNull() ?: -1
                            val sessionLines = if (parts[10].isBlank()) {
                                emptyList()
                            } else {
                                parts[10].split(',').mapNotNull { decode(it) }
                            }
                            val sessionHistory = if (parts[11].isBlank()) {
                                emptyList()
                            } else {
                                parts[11].split(',').mapNotNull { decode(it) }
                            }
                            sessions.add(
                                TerminalSession(
                                    id = decodeNonNull(parts[1]),
                                    title = decodeNonNull(parts[2]),
                                    shell = decodeNonNull(parts[3], "/bin/sh"),
                                    isConnected = parts[4].toBooleanStrictOrNull() ?: true,
                                    lines = sessionLines,
                                    cwd = decodeNonNull(parts[5], "~"),
                                    isRunning = false,
                                    history = sessionHistory,
                                    hostAddress = decode(parts[6]),
                                    hostPort = if (rawPort > 0) rawPort else null,
                                    hostUser = decode(parts[8]),
                                    hostProtocol = decode(parts[9])
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip malformed record without aborting entire snapshot
            }
        }

        val resolvedSpaces = if (spaces.isEmpty()) {
            listOf(
                Space(
                    id = "default",
                    name = "Default Space",
                    description = "Default workspace",
                    hostCount = hosts.count { it.spaceId == "default" },
                    tunnelCount = tunnelRules.size,
                    isVpnActive = false,
                    isSelected = true
                )
            )
        } else {
            spaces.map { sp ->
                sp.copy(
                    hostCount = hosts.count { it.spaceId == sp.id },
                    isSelected = sp.id == selectedSpaceId
                )
            }
        }

        val validSelectedSpaceId = if (resolvedSpaces.any { it.id == selectedSpaceId }) {
            selectedSpaceId
        } else {
            resolvedSpaces.first().id
        }

        return WorkspaceSnapshot(
            spaces = resolvedSpaces,
            selectedSpaceId = validSelectedSpaceId,
            hosts = hosts,
            credentials = credentials,
            tunnelRules = tunnelRules,
            sessions = sessions,
            selectedSessionIndex = selectedSessionIndex.coerceAtLeast(0),
            sessionSequence = sessionSequence.coerceAtLeast(sessions.size + 1)
        )
    }

    /**
     * Atomically writes the [WorkspaceSnapshot] to [targetFile] (defaults to [primaryStateFile]).
     */
    @Synchronized
    fun save(snapshot: WorkspaceSnapshot, targetFile: File = primaryStateFile): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            tempFile.writeText(serialize(snapshot), Charsets.UTF_8)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads the persisted [WorkspaceSnapshot] from [sourceFile] (defaults to [primaryStateFile]),
     * or returns null if no persisted state exists yet.
     */
    @Synchronized
    fun load(sourceFile: File = primaryStateFile): WorkspaceSnapshot? {
        if (!sourceFile.exists() || !sourceFile.isFile) return null
        return try {
            val content = sourceFile.readText(Charsets.UTF_8)
            deserialize(content)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exports the current [WorkspaceSnapshot] to the backup profile file.
     */
    fun exportBackup(snapshot: WorkspaceSnapshot, backupFile: File = defaultBackupFile): Boolean {
        return save(snapshot, backupFile)
    }

    /**
     * Imports a [WorkspaceSnapshot] from the backup profile file and saves it as the active state.
     */
    fun importBackup(backupFile: File = defaultBackupFile): WorkspaceSnapshot? {
        val imported = load(backupFile) ?: return null
        save(imported, primaryStateFile)
        return imported
    }
}
