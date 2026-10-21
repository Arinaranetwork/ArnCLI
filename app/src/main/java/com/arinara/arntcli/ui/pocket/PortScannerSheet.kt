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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import com.arinara.arntcli.core.net.LanPortScanner
import com.arinara.arntcli.core.net.PortScanResult
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
fun PortScannerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetTargets = listOf("127.0.0.1", "1.1.1.1", "8.8.8.8", "cloudflare.com")
    var targetHost by remember { mutableStateOf(presetTargets.first()) }
    var selectedProfile by remember { mutableStateOf("Common (16)") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<PortScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scanner = remember { LanPortScanner() }
    val coroutineScope = rememberCoroutineScope()

    val profilePorts = remember(selectedProfile) {
        when (selectedProfile) {
            "Web" -> listOf(80, 443, 8080, 8443)
            "Remote" -> listOf(22, 23, 3389)
            "Database" -> listOf(3306, 5432, 6379)
            else -> LanPortScanner.COMMON_PORTS.map { it.first }
        }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "TCP Port Scanner",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Target Host or IP",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Target Host Input
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
                    value = targetHost,
                    onValueChange = {
                        targetHost = it
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

            // Target Presets
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetTargets) { target ->
                    AppPillChip(
                        label = target,
                        isSelected = targetHost == target,
                        onClick = {
                            targetHost = target
                            errorMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Port Profile",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Profile Selection Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Common (16)", "Web", "Remote", "Database")) { profile ->
                    AppPillChip(
                        label = profile,
                        isSelected = selectedProfile == profile,
                        onClick = { selectedProfile = profile }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                text = if (isScanning) "Scanning Ports..." else "Start Port Scan",
                leadingIcon = Icons.Outlined.Search,
                variant = AppButtonVariant.Primary,
                enabled = !isScanning,
                onClick = {
                    isScanning = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = scanner.scanPorts(
                            targetHost = targetHost,
                            ports = profilePorts,
                            timeoutMs = 800
                        )
                        isScanning = false
                        result.onSuccess { res ->
                            scanResult = res
                        }.onFailure { err ->
                            errorMessage = err.localizedMessage ?: "Port scan failed"
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

            scanResult?.let { result ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = result.openPortCount > 0
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.targetHost,
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
                                    text = "${result.openPortCount} Open / ${result.scannedPorts.size} Scanned",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (result.openPortCount > 0) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                )
                            }
                        }

                        Text(
                            text = "IP: ${result.resolvedIp} • Completed in ${String.format("%.1f", result.durationMs)}ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                Text(
                    text = "Scanned Ports (${result.scannedPorts.size})",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                result.scannedPorts.forEach { port ->
                    val statusText = if (port.isOpen) {
                        "Open • ${String.format("%.1f", port.latencyMs ?: 0.0)}ms"
                    } else {
                        "Closed / Filtered"
                    }

                    AppListItem(
                        title = "${port.port} • ${port.serviceName}",
                        subtitle = statusText,
                        leadingIcon = if (port.isOpen) Icons.Outlined.Check else Icons.Outlined.Close,
                        leadingIconTint = if (port.isOpen) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                        action = AppListItemAction.Chevron
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
