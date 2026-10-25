// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.pocket

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.net.DiscoveredHost
import com.arinara.arntcli.core.net.LanDiscoveryEngine
import com.arinara.arntcli.core.net.LanDiscoveryResult
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.launch

@Composable
fun LanDiscoverySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onHostSelected: (DiscoveredHost) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val presetSubnets = listOf("192.168.1.", "192.168.0.", "10.0.0.", "172.16.0.")
    var subnetPrefix by remember { mutableStateOf(presetSubnets.first()) }
    var isScanning by remember { mutableStateOf(false) }
    var discoveryResult by remember { mutableStateOf<LanDiscoveryResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val engine = remember { LanDiscoveryEngine() }
    val coroutineScope = rememberCoroutineScope()

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "LAN Host Discovery",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Target Subnet Prefix",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Subnet Input
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
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = subnetPrefix,
                    onValueChange = {
                        subnetPrefix = it
                        errorMessage = null
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subnet Presets
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetSubnets) { sub ->
                    AppPillChip(
                        label = "${sub}0/24",
                        isSelected = subnetPrefix == sub,
                        onClick = {
                            subnetPrefix = sub
                            errorMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                text = if (isScanning) "Sweeping Subnet..." else "Start Subnet Sweep",
                leadingIcon = Icons.Outlined.Search,
                variant = AppButtonVariant.Primary,
                enabled = !isScanning,
                onClick = {
                    isScanning = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = engine.discover(
                                subnetPrefix = subnetPrefix,
                                hostRange = 1..254,
                                timeoutMs = 150
                            )
                            discoveryResult = result
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Subnet sweep failed"
                        } finally {
                            isScanning = false
                        }
                    }
                }
            )

            errorMessage?.let { err ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = false
                ) {
                    Text(
                        text = "Error: $err",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.Destructive
                    )
                }
            }

            discoveryResult?.let { result ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = result.discoveredHosts.isNotEmpty()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subnet ${result.subnetPrefix}0/24",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
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
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${result.discoveredHosts.size} Hosts Found",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (result.discoveredHosts.isNotEmpty()) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        Text(
                            text = "Scanned ${result.scannedCount} addresses in ${String.format("%.1f", result.durationMs)}ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                Text(
                    text = "Discovered Hosts (${result.discoveredHosts.size})",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                if (result.discoveredHosts.isEmpty()) {
                    AppListItem(
                        title = "No Responsive Hosts",
                        subtitle = "No active hosts responded on this subnet range",
                        leadingIcon = Icons.Outlined.Dns,
                        leadingIconTint = ElectricSlateTokens.TextMuted,
                        action = AppListItemAction.Chevron
                    )
                } else {
                    result.discoveredHosts.forEach { host ->
                        AppListItem(
                            title = host.ipAddress,
                            subtitle = "${host.hostname} • ${String.format("%.1f", host.latencyMs)}ms",
                            leadingIcon = Icons.Outlined.Dns,
                            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                            action = AppListItemAction.Chevron,
                            onClick = {
                                onHostSelected(host)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
