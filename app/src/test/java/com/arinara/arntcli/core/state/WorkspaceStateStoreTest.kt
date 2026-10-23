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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WorkspaceStateStoreTest {

    @Test
    fun saveAndLoad_roundTripsSpacesHostsCredentialsTunnelsAndSessions() {
        val tempDir = Files.createTempDirectory("arntcli_state_test").toFile()
        try {
            val store = WorkspaceStateStore(tempDir)
            val snapshot = WorkspaceSnapshot(
                spaces = listOf(
                    Space(
                        id = "default",
                        name = "Default Space",
                        description = "Primary workspace",
                        hostCount = 1,
                        tunnelCount = 1,
                        isVpnActive = false,
                        isSelected = false
                    ),
                    Space(
                        id = "homelab",
                        name = "Homelab Rack",
                        description = "Core routers | switches",
                        hostCount = 1,
                        tunnelCount = 1,
                        isVpnActive = true,
                        isSelected = true
                    )
                ),
                selectedSpaceId = "homelab",
                hosts = listOf(
                    HostItem(
                        id = "host-1",
                        name = "Core Bastion",
                        address = "10.10.0.1",
                        port = 2222,
                        username = "sysadmin",
                        protocol = "SSH",
                        keyAlias = "cred-1",
                        jumpHost = "jump.internal:22",
                        spaceId = "homelab"
                    )
                ),
                credentials = listOf(
                    VaultCredential(
                        id = "cred-1",
                        title = "id_ed25519_bastion",
                        type = CredentialType.SshKey,
                        description = "SHA256:abc123xyz",
                        spaceId = "homelab",
                        encryptedSecret = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjE=\n-----END OPENSSH PRIVATE KEY-----",
                        createdAt = 1729680000000L
                    )
                ),
                tunnelRules = listOf(
                    TunnelRule(
                        id = "tun-1",
                        name = "Grafana Forward",
                        type = TunnelType.Local,
                        localPort = 3000,
                        remoteHost = "127.0.0.1",
                        remotePort = 3000,
                        hostId = "host-1",
                        status = TunnelStatus.Active
                    )
                ),
                sessions = listOf(
                    TerminalSession(
                        id = "session-1",
                        title = "local:sh",
                        shell = "/system/bin/sh",
                        isConnected = true,
                        lines = listOf("~/scripts $ ls -la", "deploy.sh"),
                        cwd = "~/scripts",
                        isRunning = true,
                        history = listOf("cd scripts", "ls -la"),
                        hostAddress = "10.10.0.1",
                        hostPort = 2222,
                        hostUser = "sysadmin",
                        hostProtocol = "SSH"
                    )
                ),
                selectedSessionIndex = 0,
                sessionSequence = 5
            )

            assertTrue("Save should succeed", store.save(snapshot))
            val loaded = store.load()
            assertNotNull("Loaded snapshot must not be null", loaded)
            requireNotNull(loaded)

            assertEquals("homelab", loaded.selectedSpaceId)
            assertEquals(2, loaded.spaces.size)
            assertEquals("Homelab Rack", loaded.spaces[1].name)
            assertTrue(loaded.spaces[1].isSelected)

            assertEquals(1, loaded.hosts.size)
            assertEquals("Core Bastion", loaded.hosts[0].name)
            assertEquals(2222, loaded.hosts[0].port)
            assertEquals("jump.internal:22", loaded.hosts[0].jumpHost)

            assertEquals(1, loaded.credentials.size)
            assertEquals(CredentialType.SshKey, loaded.credentials[0].type)
            assertTrue(loaded.credentials[0].encryptedSecret.contains("BEGIN OPENSSH PRIVATE KEY"))

            assertEquals(1, loaded.tunnelRules.size)
            assertEquals(3000, loaded.tunnelRules[0].localPort)

            assertEquals(1, loaded.sessions.size)
            assertEquals("~/scripts", loaded.sessions[0].cwd)
            assertEquals(listOf("cd scripts", "ls -la"), loaded.sessions[0].history)
            assertEquals(false, loaded.sessions[0].isRunning)
            assertEquals(5, loaded.sessionSequence)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun exportAndImportBackup_restoresCompleteWorkspaceState() {
        val tempDir = Files.createTempDirectory("arntcli_backup_test").toFile()
        try {
            val store = WorkspaceStateStore(tempDir)
            val original = WorkspaceSnapshot(
                spaces = listOf(
                    Space(
                        id = "default",
                        name = "Default Space",
                        description = "Default workspace",
                        hostCount = 1,
                        isSelected = true
                    )
                ),
                selectedSpaceId = "default",
                hosts = listOf(
                    HostItem(
                        id = "h-99",
                        name = "Edge Gateway",
                        address = "192.168.1.1",
                        port = 22,
                        username = "root",
                        spaceId = "default"
                    )
                ),
                credentials = emptyList(),
                tunnelRules = emptyList(),
                sessions = emptyList(),
                sessionSequence = 3
            )

            assertTrue(store.exportBackup(original))
            val imported = store.importBackup()
            assertNotNull(imported)
            assertEquals(1, imported?.hosts?.size)
            assertEquals("Edge Gateway", imported?.hosts?.first()?.name)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
