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
import kotlinx.coroutines.launch
import com.arinara.arntcli.core.terminal.LocalProcessEngine
import com.arinara.arntcli.ui.pocket.PocketScreen
import com.arinara.arntcli.ui.pocket.SubnetCalculatorSheet
import com.arinara.arntcli.ui.pocket.PingSheet
import com.arinara.arntcli.ui.pocket.DnsSheet
import com.arinara.arntcli.ui.sessions.SessionsScreen
import com.arinara.arntcli.ui.settings.SettingsScreen
import com.arinara.arntcli.ui.space.SpaceSwitcherSheet
import com.arinara.arntcli.ui.terminal.TerminalScreen
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
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

    val coroutineScope = rememberCoroutineScope()
    val processEngine = remember { LocalProcessEngine() }

    val initialSpaces = remember {
        listOf(
            Space(
                id = "default",
                name = "Default Space",
                description = "Primary operational context workspace",
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
    var sessions by remember { mutableStateOf(emptyList<TerminalSession>()) }

    val activeSpace = spaces.firstOrNull { it.id == selectedSpaceId } ?: spaces.first()
    val spaceHosts = hosts.filter { it.spaceId == selectedSpaceId }
    val spaceCredentials = credentials.filter { it.spaceId == selectedSpaceId }

    Box(modifier = modifier.fillMaxSize()) {
        AppScreen(
            topBar = {
                AppTopBar(
                    title = "ArnTCLI",
                    subtitle = "Network Swiss Army Knife",
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
                        contentDescription = "Terminal Workspace",
                        isSelected = currentDestination == MainDestination.Terminal,
                        onClick = { currentDestination = MainDestination.Terminal }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Dns,
                        contentDescription = "Sessions & Hosts",
                        isSelected = currentDestination == MainDestination.Sessions,
                        onClick = { currentDestination = MainDestination.Sessions }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Build,
                        contentDescription = "Pocket Tool Belt",
                        isSelected = currentDestination == MainDestination.Pocket,
                        onClick = { currentDestination = MainDestination.Pocket }
                    )
                    AppFloatingBarItem(
                        icon = Icons.Outlined.Lock,
                        contentDescription = "Cryptographic Vault",
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
                            val newSession = TerminalSession(
                                id = "session-${sessions.size + 1}",
                                title = "local:sh",
                                shell = processEngine.shellExecutable,
                                isConnected = true,
                                lines = listOf(
                                    "ArnTCLI Terminal Environment [Space: ${activeSpace.name}]",
                                    "Local shell ready (${processEngine.shellExecutable}). Ready for execution."
                                )
                            )
                            sessions = sessions + newSession
                        },
                        onExecuteCommand = { sessionId, cmd ->
                            if (cmd == "clear") {
                                sessions = sessions.map { s ->
                                    if (s.id == sessionId) s.copy(lines = emptyList()) else s
                                }
                            } else {
                                sessions = sessions.map { s ->
                                    if (s.id == sessionId) s.copy(lines = (s.lines + "$ $cmd").takeLast(500)) else s
                                }
                                coroutineScope.launch {
                                    try {
                                        processEngine.executeStreaming(cmd).collect { outputLine ->
                                            sessions = sessions.map { s ->
                                                if (s.id == sessionId) {
                                                    s.copy(lines = (s.lines + outputLine).takeLast(500))
                                                } else s
                                            }
                                        }
                                    } catch (e: Exception) {
                                        val err = "Error: ${e.message ?: "Execution failed"}"
                                        sessions = sessions.map { s ->
                                            if (s.id == sessionId) {
                                                s.copy(lines = (s.lines + err).takeLast(500))
                                            } else s
                                        }
                                    }
                                }
                            }
                        },
                        onCloseSession = { sessionId ->
                            sessions = sessions.filterNot { it.id == sessionId }
                        }
                    )
                }
                MainDestination.Sessions -> {
                    SessionsScreen(
                        contentPadding = contentPadding,
                        activeSpaceName = activeSpace.name,
                        hosts = spaceHosts,
                        onAddHostClick = { }
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
                        onAddCredentialClick = { }
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
            onAddSpaceClick = { isSpaceSheetVisible = false }
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
    }
}
