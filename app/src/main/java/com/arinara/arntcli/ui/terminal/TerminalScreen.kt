// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.model.TerminalSession
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

/**
 * TerminalScreen - Active terminal emulator session workspace.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun TerminalScreen(
    contentPadding: PaddingValues,
    activeSpaceName: String,
    sessions: List<TerminalSession>,
    onStartLocalShell: () -> Unit,
    onExecuteCommand: (sessionId: String, command: String) -> Unit = { _, _ -> },
    onCloseSession: (sessionId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSessionIndex by remember { mutableIntStateOf(0) }
    var commandInput by remember { mutableStateOf("") }
    val auxiliaryKeys = listOf("ESC", "TAB", "CTRL", "ALT", "|", "/", "~", "-", ">")
    val quickCommands = listOf("uname -a", "id", "pwd", "date", "uptime", "clear")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            // Empty State
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = "No Terminal Sessions",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "No Active Terminal Sessions",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    Text(
                        text = "Spawn a local shell process or connect to a host.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AppButton(
                        text = "Start Local Shell",
                        leadingIcon = Icons.Outlined.Add,
                        variant = AppButtonVariant.Primary,
                        onClick = onStartLocalShell
                    )
                }
            }
        } else {
            val safeIndex = selectedSessionIndex.coerceIn(0, sessions.size - 1)
            val currentSession = sessions[safeIndex]
            val terminalScrollState = rememberScrollState()

            LaunchedEffect(currentSession.lines.size) {
                terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
            }

            // Session Quick Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "+ New Shell",
                        isSelected = false,
                        onClick = onStartLocalShell
                    )
                }
                items(sessions.indices.toList()) { index ->
                    AppPillChip(
                        label = sessions[index].title,
                        isSelected = safeIndex == index,
                        onClick = { selectedSessionIndex = index },
                        leadingIcon = Icons.Outlined.Terminal
                    )
                }
            }

            // Primary Terminal Buffer Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                isGlow = true
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Terminal Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentSession.isConnected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.TextMuted
                                    )
                            )
                            Text(
                                text = currentSession.title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (currentSession.isConnected) "Connected" else "Disconnected",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .jigglePressClickable {
                                        val closingIndex = safeIndex
                                        onCloseSession(currentSession.id)
                                        if (closingIndex >= sessions.size - 1) {
                                            selectedSessionIndex = (sessions.size - 2).coerceAtLeast(0)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.Destructive
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Terminal Output Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(terminalScrollState)
                    ) {
                        if (currentSession.lines.isEmpty()) {
                            Text(
                                text = "ArnTCLI Terminal Environment [Space: $activeSpaceName]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            Text(
                                text = "Shell: ${currentSession.shell}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$ Ready for execution. Type a command below.",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        } else {
                            currentSession.lines.forEach { line ->
                                if (line.startsWith("$ ")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.PrimaryAccent
                                        )
                                        Text(
                                            text = line.removePrefix("$ "),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (line.startsWith("Error:") || line.startsWith("sh: ") || line.contains("not found")) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Prompt Input Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                                BasicTextField(
                                    value = commandInput,
                                    onValueChange = { commandInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (commandInput.isNotBlank()) {
                                                onExecuteCommand(currentSession.id, commandInput.trim())
                                                commandInput = ""
                                            }
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (commandInput.isEmpty()) {
                                            Text(
                                                text = "Enter command...",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.PrimaryAccent)
                                    .jigglePressClickable {
                                        if (commandInput.isNotBlank()) {
                                            onExecuteCommand(currentSession.id, commandInput.trim())
                                            commandInput = ""
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Execute Command",
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Diagnostic Commands
            Text(
                text = "Quick Commands",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    AppPillChip(
                        label = cmd,
                        isSelected = false,
                        onClick = {
                            onExecuteCommand(currentSession.id, cmd)
                        }
                    )
                }
            }

            // Auxiliary Keyboard Keys Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auxiliaryKeys) { key ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfacePanel)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .jigglePressClickable {
                                commandInput += key
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }
        }

        BottomClearanceSpacer()
    }
}
