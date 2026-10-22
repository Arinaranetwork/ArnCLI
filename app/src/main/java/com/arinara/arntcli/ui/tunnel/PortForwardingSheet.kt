// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.tunnel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MultipleStop
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.tunnel.PortForwardingEngine
import com.arinara.arntcli.core.tunnel.TunnelRule
import com.arinara.arntcli.core.tunnel.TunnelStatus
import com.arinara.arntcli.core.tunnel.TunnelType
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import kotlinx.coroutines.launch

@Composable
fun PortForwardingSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    engine: PortForwardingEngine,
    rules: List<TunnelRule>,
    onRulesChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val types = listOf(
        TunnelType.Local to "Local (-L)",
        TunnelType.Remote to "Remote (-R)",
        TunnelType.DynamicSocks5 to "Dynamic SOCKS5 (-D)"
    )

    var name by remember(visible) { mutableStateOf("") }
    var selectedType by remember(visible) { mutableStateOf(TunnelType.Local) }
    var localPort by remember(visible) { mutableStateOf("8080") }
    var remoteHost by remember(visible) { mutableStateOf("localhost") }
    var remotePort by remember(visible) { mutableStateOf("80") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Port Forwarding Tunnels",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Tunnel Configuration Section
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add Forwarding Rule",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    // Type Selector
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(types) { (type, label) ->
                            AppPillChip(
                                label = label,
                                isSelected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                        }
                    }

                    // Rule Name
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
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            ),
                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (name.isBlank()) {
                            Text(
                                text = "Rule Name (e.g. Postgres DB)",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }
                    }

                    // Port Inputs Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Local Port
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = localPort,
                                onValueChange = {
                                    localPort = it.filter { ch -> ch.isDigit() }.take(5)
                                    errorMessage = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                ),
                                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (localPort.isBlank()) {
                                Text(
                                    text = "Local Port",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        if (selectedType != TunnelType.DynamicSocks5) {
                            // Remote Host
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = remoteHost,
                                    onValueChange = {
                                        remoteHost = it
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (remoteHost.isBlank()) {
                                    Text(
                                        text = "Remote Host",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                            }

                            // Remote Port
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = remotePort,
                                    onValueChange = {
                                        remotePort = it.filter { ch -> ch.isDigit() }.take(5)
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (remotePort.isBlank()) {
                                    Text(
                                        text = "Remote Port",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.Destructive
                        )
                    }

                    AppButton(
                        text = "Add Tunnel Rule",
                        leadingIcon = Icons.Outlined.Add,
                        variant = AppButtonVariant.Primary,
                        onClick = {
                            val lPort = localPort.toIntOrNull()
                            if (lPort == null || lPort !in 1..65535) {
                                errorMessage = "Local port must be between 1 and 65535"
                                return@AppButton
                            }

                            val rPort = if (selectedType != TunnelType.DynamicSocks5) {
                                val parsed = remotePort.toIntOrNull()
                                if (parsed == null || parsed !in 1..65535) {
                                    errorMessage = "Remote port must be between 1 and 65535"
                                    return@AppButton
                                }
                                parsed
                            } else 80

                            val cleanName = name.trim().ifBlank { "${selectedType.name}-$lPort" }

                            val rule = TunnelRule(
                                id = "tunnel-${System.currentTimeMillis()}",
                                name = cleanName,
                                type = selectedType,
                                localPort = lPort,
                                remoteHost = remoteHost.trim().ifBlank { "localhost" },
                                remotePort = rPort
                            )

                            engine.addRule(rule)
                            name = ""
                            onRulesChanged()
                        }
                    )
                }
            }

            // Configured Tunnels List
            Text(
                text = "Configured Tunnels (${rules.size})",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            if (rules.isEmpty()) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SyncAlt,
                            contentDescription = "No Tunnels",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No Port Forwarding Rules Active",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Text(
                            text = "Add Local (-L), Remote (-R), or SOCKS5 (-D) tunnels above.",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }
            } else {
                rules.forEach { rule ->
                    val isActive = rule.status == TunnelStatus.Active
                    val icon = when (rule.type) {
                        TunnelType.Local -> Icons.Outlined.SyncAlt
                        TunnelType.Remote -> Icons.Outlined.MultipleStop
                        TunnelType.DynamicSocks5 -> Icons.Outlined.SyncAlt
                    }

                    AppListItem(
                        title = rule.name,
                        subtitle = "${rule.toCliSyntax()} • ${rule.toEndpointSummary()}",
                        leadingIcon = icon,
                        leadingIconTint = if (isActive) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                        action = AppListItemAction.Switch(
                            checked = isActive,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    engine.toggleTunnel(rule.id)
                                    onRulesChanged()
                                }
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
