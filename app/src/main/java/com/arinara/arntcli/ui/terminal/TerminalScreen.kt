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
    modifier: Modifier = Modifier
) {
    var selectedSessionIndex by remember { mutableIntStateOf(0) }
    var commandInput by remember { mutableStateOf("") }
    val auxiliaryKeys = listOf("ESC", "TAB", "CTRL", "ALT", "|", "/", "~", "-", ">")

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
                        text = "Space: $activeSpaceName\nSpawn a local shell process or connect to a remote host.",
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

            // Session Quick Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                        Text(
                            text = if (currentSession.isConnected) "Online • Offline-First" else "Disconnected",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Terminal Output Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
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
                            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                Text(
                                    text = if (commandInput.isEmpty()) "Enter command..." else commandInput,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (commandInput.isEmpty()) ElectricSlateTokens.TextMuted else ElectricSlateTokens.TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElectricSlateTokens.PrimaryAccent)
                                    .jigglePressClickable {
                                        commandInput = ""
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

            // Operational Status Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Terminal Session Manager",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sessions.size} active sessions • Space: $activeSpaceName",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "100% Offline",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }
                }
            }
        }

        BottomClearanceSpacer()
    }
}
