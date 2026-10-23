// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppFloatingBar
import com.arinara.arntcli.ui.components.presets.AppFloatingBarItem
import com.arinara.arntcli.ui.components.presets.AppScreen
import com.arinara.arntcli.ui.components.presets.AppTopBar
import com.arinara.arntcli.ui.model.HostItem
import com.arinara.arntcli.ui.model.Space
import com.arinara.arntcli.ui.model.TerminalSession
import com.arinara.arntcli.ui.model.VaultCredential
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import com.arinara.arntcli.core.net.DiscoveredHost
import com.arinara.arntcli.core.terminal.LocalProcessEngine
import com.arinara.arntcli.ui.automation.WorkflowRunnerSheet
import com.arinara.arntcli.ui.container.ContainerExplorerSheet
import com.arinara.arntcli.ui.desktop.DesktopViewerSheet
import com.arinara.arntcli.ui.devlab.DevLabSheet
import com.arinara.arntcli.ui.pocket.DnsSheet
import com.arinara.arntcli.ui.telemetry.ServerTelemetrySheet
import com.arinara.arntcli.ui.pocket.LanDiscoverySheet
import com.arinara.arntcli.ui.pocket.PingSheet
import com.arinara.arntcli.ui.pocket.PocketScreen
import com.arinara.arntcli.ui.pocket.PortScannerSheet
import com.arinara.arntcli.ui.pocket.SslInspectorSheet
import com.arinara.arntcli.ui.storage.StorageBrowserSheet
import com.arinara.arntcli.core.tunnel.PortForwardingEngine
import com.arinara.arntcli.core.tunnel.TunnelRule
import com.arinara.arntcli.core.tunnel.TunnelStatus
import com.arinara.arntcli.ui.pocket.SubnetCalculatorSheet
import com.arinara.arntcli.ui.pocket.TracerouteSheet
import com.arinara.arntcli.ui.pocket.WifiInspectorSheet
import com.arinara.arntcli.ui.pocket.WolSheet
import com.arinara.arntcli.ui.sessions.AddHostSheet
import com.arinara.arntcli.ui.sessions.SessionsScreen
import com.arinara.arntcli.ui.settings.SettingsScreen
import com.arinara.arntcli.ui.space.AddSpaceSheet
import com.arinara.arntcli.ui.space.SpaceSwitcherSheet
import com.arinara.arntcli.ui.terminal.TerminalScreen
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import com.arinara.arntcli.ui.tunnel.PortForwardingSheet
import com.arinara.arntcli.ui.tunnel.WireGuardSheet
import com.arinara.arntcli.ui.vault.AddCredentialSheet
import com.arinara.arntcli.ui.vault.AirGappedSyncSheet
import com.arinara.arntcli.ui.vault.CredentialDetailSheet
import com.arinara.arntcli.ui.vault.VaultScreen

enum class MainDestination {
    Terminal,
    Sessions,
    Pocket,
    Vault,
    Settings
}

@Composable
fun MainScaffold(
    onExportProfiles: () -> Unit,
    onImportProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(MainDestination.Terminal) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var isSpaceSheetVisible by remember { mutableStateOf(false) }
    var isSubnetCalculatorVisible by remember { mutableStateOf(false) }
    var isPingSheetVisible by remember { mutableStateOf(false) }
    var isDnsSheetVisible by remember { mutableStateOf(false) }
    var isSslInspectorVisible by remember { mutableStateOf(false) }
    var isWolSheetVisible by remember { mutableStateOf(false) }
    var isWifiInspectorVisible by remember { mutableStateOf(false) }
    var isPortScannerVisible by remember { mutableStateOf(false) }
    var isTracerouteVisible by remember { mutableStateOf(false) }
    var isAddHostVisible by remember { mutableStateOf(false) }
    var isDiscoveryVisible by remember { mutableStateOf(false) }
    var prefilledDiscoveryHost by remember { mutableStateOf<DiscoveredHost?>(null) }
    var isAddSpaceVisible by remember { mutableStateOf(false) }
    var isAddCredentialVisible by remember { mutableStateOf(false) }
    var selectedCredentialDetail by remember { mutableStateOf<VaultCredential?>(null) }
    var isPortForwardingVisible by remember { mutableStateOf(false) }
    var isWireGuardVisible by remember { mutableStateOf(false) }
    var isAirGappedSyncVisible by remember { mutableStateOf(false) }
    var isStorageBrowserVisible by remember { mutableStateOf(false) }
    var isDesktopViewerVisible by remember { mutableStateOf(false) }
    var isDevLabVisible by remember { mutableStateOf(false) }
    var isServerTelemetryVisible by remember { mutableStateOf(false) }
    var isContainersVisible by remember { mutableStateOf(false) }
    var isWorkflowSheetVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sandboxHomeDir = remember(context) {
        File(context.filesDir, "home").apply { mkdirs() }
    }
    val coroutineScope = rememberCoroutineScope()
    val activeSessionJobs = remember { mutableMapOf<String, Job>() }
    val processEngine = remember(sandboxHomeDir) {
        LocalProcessEngine(workingDirectory = sandboxHomeDir)
    }
    val tunnelEngine = remember { PortForwardingEngine() }
    var tunnelRules by remember { mutableStateOf(emptyList<TunnelRule>()) }
    var sessionSequence by remember { mutableStateOf(2) }

    val initialSpaces = remember {
        listOf(
            Space(
                id = "default",
                name = "Default Space",
                description = "Default workspace",
                hostCount = 0,
                tunnelCount = 0,
                isVpnActive = false,
                isSelected = true
            )
        )
    }

    var spaces by remember { mutableStateOf(initialSpaces) }
    var selectedSpaceId by remember { mutableStateOf("default") }
    var hosts by remember { mutableStateOf(emptyList<HostItem>()) }
    var credentials by remember { mutableStateOf(emptyList<VaultCredential>()) }
    var sessions by remember {
        processEngine.getOrCreateSession("session-1", customHomeDir = sandboxHomeDir)
        mutableStateOf(
            listOf(
                TerminalSession(
                    id = "session-1",
                    title = "local:sh",
                    shell = processEngine.shellExecutable,
                    isConnected = true,
                    cwd = processEngine.getFormattedPromptCwd("session-1"),
                    lines = listOf(
                        "ArnCLI Stateful Shell (${processEngine.shellExecutable})",
                        "Writable Home: ${sandboxHomeDir.absolutePath}",
                        "Type 'help' for built-in commands, aliases, and key shortcuts."
                    )
                )
            )
        )
    }

    val activeSpace = spaces.firstOrNull { it.id == selectedSpaceId } ?: spaces.first()
    val spaceHosts = hosts.filter { it.spaceId == selectedSpaceId }
    val spaceCredentials = credentials.filter { it.spaceId == selectedSpaceId }

    Box(modifier = modifier.fillMaxSize()) {
        AppScreen(
            topBar = {
                AppTopBar(
                    title = "ArnCLI",
                    actions = {
                        // Space Switcher Capsule Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(ElectricSlateTokens.SurfacePanel)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .jigglePressClickable { isSpaceSheetVisible = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.PrimaryAccent)
                                )
                                Text(
                                    text = activeSpace.name,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Switch Space",
                                    tint = ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Vault Security Status Pill
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.SurfacePanel)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = CircleShape
                                )
                                .jigglePressClickable { isVaultUnlocked = !isVaultUnlocked },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVaultUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                contentDescription = "Toggle Vault Lock",
                                tint = if (isVaultUnlocked) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                AppFloatingBar {
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Terminal,
                        contentDescription = "Terminal",
                        isSelected = currentDestination == MainDestination.Terminal,
                        onClick = { currentDestination = MainDestination.Terminal }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Dns,
                        contentDescription = "Sessions",
                        isSelected = currentDestination == MainDestination.Sessions,
                        onClick = { currentDestination = MainDestination.Sessions }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Build,
                        contentDescription = "Pocket",
                        isSelected = currentDestination == MainDestination.Pocket,
                        onClick = { currentDestination = MainDestination.Pocket }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Lock,
                        contentDescription = "Vault",
                        isSelected = currentDestination == MainDestination.Vault,
                        onClick = { currentDestination = MainDestination.Vault }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        isSelected = currentDestination == MainDestination.Settings,
                        onClick = { currentDestination = MainDestination.Settings }
                    )
                }
            }
        ) { contentPadding ->
            when (currentDestination) {
                MainDestination.Terminal -> {
                    TerminalScreen(
                        contentPadding = contentPadding,
                        activeSpaceName = activeSpace.name,
                        sessions = sessions,
                        onStartLocalShell = {
                            val newId = "session-$sessionSequence"
                            sessionSequence += 1
                            processEngine.getOrCreateSession(newId, customHomeDir = sandboxHomeDir)
                            val newSession = TerminalSession(
                                id = newId,
                                title = "local:sh",
                                shell = processEngine.shellExecutable,
                                isConnected = true,
                                cwd = processEngine.getFormattedPromptCwd(newId),
                                lines = listOf(
                                    "ArnCLI Stateful Shell (${processEngine.shellExecutable})",
                                    "Writable Home: ${sandboxHomeDir.absolutePath}",
                                    "Type 'help' for built-in commands, aliases, and key shortcuts."
                                )
                            )
                            sessions = sessions + newSession
                        },
                        onExecuteCommand = { sessionId, cmd ->
                            val normalized = cmd.trim()
                            if (normalized == "clear" || normalized == "cls") {
                                sessions = sessions.map { s ->
                                    if (s.id == sessionId) {
                                        s.copy(
                                            lines = emptyList(),
                                            cwd = processEngine.getFormattedPromptCwd(sessionId),
                                            history = processEngine.getHistory(sessionId)
                                        )
                                    } else s
                                }
                            } else {
                                val currentCwd = processEngine.getFormattedPromptCwd(sessionId)
                                sessions = sessions.map { s ->
                                    if (s.id == sessionId) {
                                        s.copy(
                                            lines = (s.lines + "$currentCwd $ $normalized").takeLast(1000),
                                            isRunning = true
                                        )
                                    } else s
                                }
                                activeSessionJobs[sessionId]?.cancel()
                                activeSessionJobs[sessionId] = coroutineScope.launch {
                                    try {
                                        processEngine.executeStreaming(normalized, sessionId).collect { outputLine ->
                                            sessions = sessions.map { s ->
                                                if (s.id == sessionId) {
                                                    s.copy(
                                                        lines = (s.lines + outputLine).takeLast(1000),
                                                        cwd = processEngine.getFormattedPromptCwd(sessionId)
                                                    )
                                                } else s
                                            }
                                        }
                                    } catch (_: CancellationException) {
                                        // Handled by interrupt
                                    } catch (e: Exception) {
                                        val err = "Error: ${e.message ?: "Execution failed"}"
                                        sessions = sessions.map { s ->
                                            if (s.id == sessionId) {
                                                s.copy(lines = (s.lines + err).takeLast(1000))
                                            } else s
                                        }
                                    } finally {
                                        val updatedCwd = processEngine.getFormattedPromptCwd(sessionId)
                                        val updatedHistory = processEngine.getHistory(sessionId)
                                        sessions = sessions.map { s ->
                                            if (s.id == sessionId) {
                                                s.copy(
                                                    cwd = updatedCwd,
                                                    history = updatedHistory,
                                                    isRunning = false
                                                )
                                            } else s
                                        }
                                    }
                                }
                            }
                        },
                        onInterruptSession = { sessionId ->
                            val wasRunning = processEngine.killActiveProcess(sessionId)
                            activeSessionJobs[sessionId]?.cancel()
                            activeSessionJobs.remove(sessionId)
                            val interruptLine = if (wasRunning) "^C [Process terminated]" else "^C"
                            sessions = sessions.map { s ->
                                if (s.id == sessionId) {
                                    s.copy(
                                        lines = (s.lines + interruptLine).takeLast(1000),
                                        isRunning = false
                                    )
                                } else s
                            }
                        },
                        onTabComplete = { sessionId, currentInput ->
                            processEngine.completeInput(currentInput, sessionId)
                        },
                        onAppendLines = { sessionId, extraLines ->
                            sessions = sessions.map { s ->
                                if (s.id == sessionId) {
                                    s.copy(lines = (s.lines + extraLines).takeLast(1000))
                                } else s
                            }
                        },
                        onCloseSession = { sessionId ->
                            processEngine.closeSession(sessionId)
                            activeSessionJobs[sessionId]?.cancel()
                            activeSessionJobs.remove(sessionId)
                            sessions = sessions.filterNot { it.id == sessionId }
                        },
                        onOpenAutomation = { isWorkflowSheetVisible = true }
                    )
                }
                MainDestination.Sessions -> {
                    SessionsScreen(
                        contentPadding = contentPadding,
                        activeSpaceName = activeSpace.name,
                        hosts = spaceHosts,
                        activeTunnelCount = tunnelRules.count { it.status == TunnelStatus.Active },
                        onManageTunnelsClick = { isPortForwardingVisible = true },
                        onWireGuardClick = { isWireGuardVisible = true },
                        onOpenStorageClick = { isStorageBrowserVisible = true },
                        onOpenDesktopClick = { isDesktopViewerVisible = true },
                        onAddHostClick = { isAddHostVisible = true },
                        onHostClick = { host ->
                            val sessionId = "session-${host.id}-${sessionSequence++}"
                            processEngine.getOrCreateSession(
                                sessionId = sessionId,
                                customHomeDir = sandboxHomeDir,
                                initialEnv = mapOf(
                                    "HOST" to host.address,
                                    "PORT" to host.port.toString(),
                                    "TARGET_USER" to host.username,
                                    "PROTOCOL" to host.protocol
                                )
                            )

                            val bannerLines = mutableListOf(
                                "ArnCLI Host Session [Space: ${activeSpace.name}]",
                                "Target: ${host.name} (${host.address}:${host.port}) via ${host.protocol}",
                                "Bound Env: \$HOST=${host.address}  \$PORT=${host.port}  \$TARGET_USER=${host.username}"
                            )
                            if (!host.jumpHost.isNullOrBlank()) {
                                bannerLines.add("[ProxyJump] Bastion proxy: ${host.jumpHost}")
                            }
                            bannerLines.add("Probing TCP socket ${host.address}:${host.port}...")

                            val session = TerminalSession(
                                id = sessionId,
                                title = "${host.protocol.lowercase()}:${host.name}",
                                shell = "${host.protocol} ${host.username}@${host.address}:${host.port}",
                                isConnected = true,
                                cwd = processEngine.getFormattedPromptCwd(sessionId),
                                lines = bannerLines,
                                hostAddress = host.address,
                                hostPort = host.port,
                                hostUser = host.username,
                                hostProtocol = host.protocol
                            )
                            sessions = sessions + session
                            currentDestination = MainDestination.Terminal

                            // Perform real TCP handshake & banner read on IO dispatcher
                            coroutineScope.launch {
                                val probeResult = withContext(Dispatchers.IO) {
                                    val startMs = System.currentTimeMillis()
                                    try {
                                        Socket().use { socket ->
                                            socket.soTimeout = 2500
                                            socket.connect(InetSocketAddress(host.address, host.port), 3000)
                                            val rtt = System.currentTimeMillis() - startMs
                                            val serverBanner = try {
                                                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                                                if (socket.getInputStream().available() > 0 || host.protocol.equals("SSH", ignoreCase = true)) {
                                                    reader.readLine()?.take(120)
                                                } else null
                                            } catch (_: Exception) {
                                                null
                                            }
                                            val lines = mutableListOf(
                                                "[Connected] TCP ${host.address}:${host.port} reachable (${rtt} ms)"
                                            )
                                            if (!serverBanner.isNullOrBlank()) {
                                                lines.add("[Remote Banner] $serverBanner")
                                            }
                                            lines.add("Shell ready. Run commands with \$HOST (${host.address}) and \$PORT (${host.port}).")
                                            Pair(true, lines)
                                        }
                                    } catch (e: Exception) {
                                        Pair(
                                            false,
                                            listOf(
                                                "[Warning] TCP ${host.address}:${host.port} unreachable: ${e.message ?: "Connection timed out"}",
                                                "Local diagnostic shell active. Use 'ping -c 4 \$HOST' or 'traceroute' to inspect route."
                                            )
                                        )
                                    }
                                }
                                sessions = sessions.map { s ->
                                    if (s.id == sessionId) {
                                        s.copy(
                                            isConnected = probeResult.first,
                                            lines = (s.lines + probeResult.second).takeLast(1000)
                                        )
                                    } else s
                                }
                            }
                        }
                    )
                }
                MainDestination.Pocket -> {
                    PocketScreen(
                        contentPadding = contentPadding,
                        onToolSelected = { toolId ->
                            when (toolId) {
                                "ping" -> isPingSheetVisible = true
                                "dns" -> isDnsSheetVisible = true
                                "subnet" -> isSubnetCalculatorVisible = true
                                "ssl" -> isSslInspectorVisible = true
                                "wol" -> isWolSheetVisible = true
                                "wifi" -> isWifiInspectorVisible = true
                                "portscan" -> isPortScannerVisible = true
                                "discovery" -> isDiscoveryVisible = true
                                "traceroute" -> isTracerouteVisible = true
                                "devlab" -> isDevLabVisible = true
                                "telemetry" -> isServerTelemetryVisible = true
                                "containers" -> isContainersVisible = true
                            }
                        }
                    )
                }
                MainDestination.Vault -> {
                    VaultScreen(
                        contentPadding = contentPadding,
                        isVaultUnlocked = isVaultUnlocked,
                        credentials = spaceCredentials,
                        onToggleVaultLock = { isVaultUnlocked = !isVaultUnlocked },
                        onAddCredentialClick = { isAddCredentialVisible = true },
                        onCredentialClick = { cred -> selectedCredentialDetail = cred },
                        onAirGappedSyncClick = { isAirGappedSyncVisible = true }
                    )
                }
                MainDestination.Settings -> {
                    SettingsScreen(
                        contentPadding = contentPadding,
                        onExportProfilesClick = onExportProfiles,
                        onImportProfilesClick = onImportProfiles
                    )
                }
            }
        }

        // Space Switcher Bottom Sheet
        SpaceSwitcherSheet(
            visible = isSpaceSheetVisible,
            onDismiss = { isSpaceSheetVisible = false },
            spaces = spaces,
            selectedSpaceId = selectedSpaceId,
            onSelectSpace = { chosen ->
                selectedSpaceId = chosen.id
                spaces = spaces.map { it.copy(isSelected = it.id == chosen.id) }
            },
            onAddSpaceClick = {
                isSpaceSheetVisible = false
                isAddSpaceVisible = true
            }
        )

        // Create Space Bottom Sheet
        AddSpaceSheet(
            visible = isAddSpaceVisible,
            onDismiss = { isAddSpaceVisible = false },
            onSpaceCreated = { newSpace ->
                spaces = spaces + newSpace
                selectedSpaceId = newSpace.id
            }
        )

        // Add Vault Credential Bottom Sheet
        AddCredentialSheet(
            visible = isAddCredentialVisible,
            onDismiss = { isAddCredentialVisible = false },
            activeSpaceId = activeSpace.id,
            onSaveCredential = { newCred ->
                credentials = credentials + newCred
            }
        )

        // Credential Detail Bottom Sheet
        CredentialDetailSheet(
            visible = selectedCredentialDetail != null,
            onDismiss = { selectedCredentialDetail = null },
            credential = selectedCredentialDetail,
            onDeleteCredential = { toDelete ->
                credentials = credentials.filterNot { it.id == toDelete.id }
                selectedCredentialDetail = null
            }
        )

        // Route Traceroute Bottom Sheet
        TracerouteSheet(
            visible = isTracerouteVisible,
            onDismiss = { isTracerouteVisible = false }
        )

        // Subnet & CIDR Calculator Bottom Sheet
        SubnetCalculatorSheet(
            visible = isSubnetCalculatorVisible,
            onDismiss = { isSubnetCalculatorVisible = false }
        )

        // ICMP Ping & Jitter Bottom Sheet
        PingSheet(
            visible = isPingSheetVisible,
            onDismiss = { isPingSheetVisible = false }
        )

        // DNS Deep Resolver Bottom Sheet
        DnsSheet(
            visible = isDnsSheetVisible,
            onDismiss = { isDnsSheetVisible = false }
        )

        // SSL / TLS Certificate Inspector Bottom Sheet
        SslInspectorSheet(
            visible = isSslInspectorVisible,
            onDismiss = { isSslInspectorVisible = false }
        )

        // Wake-on-LAN Bottom Sheet
        WolSheet(
            visible = isWolSheetVisible,
            onDismiss = { isWolSheetVisible = false }
        )

        // Wi-Fi Inspector Bottom Sheet
        WifiInspectorSheet(
            visible = isWifiInspectorVisible,
            onDismiss = { isWifiInspectorVisible = false }
        )

        // TCP Port Scanner Bottom Sheet
        PortScannerSheet(
            visible = isPortScannerVisible,
            onDismiss = { isPortScannerVisible = false }
        )

        // LAN Host Discovery Bottom Sheet
        LanDiscoverySheet(
            visible = isDiscoveryVisible,
            onDismiss = { isDiscoveryVisible = false },
            onHostSelected = { discovered ->
                prefilledDiscoveryHost = discovered
                isAddHostVisible = true
            }
        )

        // Register / Add Host Bottom Sheet
        AddHostSheet(
            visible = isAddHostVisible,
            onDismiss = {
                isAddHostVisible = false
                prefilledDiscoveryHost = null
            },
            activeSpaceId = activeSpace.id,
            initialAddress = prefilledDiscoveryHost?.ipAddress ?: "",
            initialName = prefilledDiscoveryHost?.hostname ?: "",
            onSaveHost = { newHost ->
                hosts = hosts + newHost
            }
        )

        // Port Forwarding Manager Bottom Sheet
        PortForwardingSheet(
            visible = isPortForwardingVisible,
            onDismiss = { isPortForwardingVisible = false },
            engine = tunnelEngine,
            rules = tunnelRules,
            onRulesChanged = {
                tunnelRules = tunnelEngine.rules.value
            }
        )

        // WireGuard Profile Inspector Bottom Sheet
        WireGuardSheet(
            visible = isWireGuardVisible,
            onDismiss = { isWireGuardVisible = false },
            activeSpaceId = activeSpace.id,
            onSaveToVault = { cred ->
                credentials = credentials + cred
            }
        )

        // Air-Gapped Vault Sync (BC-UR) Bottom Sheet
        AirGappedSyncSheet(
            visible = isAirGappedSyncVisible,
            onDismiss = { isAirGappedSyncVisible = false },
            onImportPayload = { payloadJson ->
                // Unlock vault and accept new imported credentials if present
                isVaultUnlocked = true
            }
        )

        // Remote Storage Explorer Bottom Sheet
        StorageBrowserSheet(
            visible = isStorageBrowserVisible,
            onDismiss = { isStorageBrowserVisible = false }
        )

        // Remote Desktop (VNC / RDP) Viewer Bottom Sheet
        DesktopViewerSheet(
            visible = isDesktopViewerVisible,
            onDismiss = { isDesktopViewerVisible = false }
        )

        // Developer & Protocol Lab Bottom Sheet
        DevLabSheet(
            visible = isDevLabVisible,
            onDismiss = { isDevLabVisible = false }
        )

        // Server Telemetry & Watchdogs Bottom Sheet
        ServerTelemetrySheet(
            visible = isServerTelemetryVisible,
            onDismiss = { isServerTelemetryVisible = false },
            hostName = activeSpace.name
        )

        // Containers & Cloud-Native Bottom Sheet
        ContainerExplorerSheet(
            visible = isContainersVisible,
            onDismiss = { isContainersVisible = false },
            hostName = activeSpace.name
        )

        // Terminal Workflow Automation Bottom Sheet
        WorkflowRunnerSheet(
            visible = isWorkflowSheetVisible,
            onDismiss = { isWorkflowSheetVisible = false },
            onExecuteCommand = { cmd ->
                processEngine.executeCommand(cmd)
            }
        )
    }
}
