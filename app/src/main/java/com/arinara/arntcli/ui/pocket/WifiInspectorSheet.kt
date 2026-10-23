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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.net.WifiConnectionDetails
import com.arinara.arntcli.core.net.WifiInspectorEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.launch

@Composable
fun WifiInspectorSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = remember { WifiInspectorEngine() }
    val coroutineScope = rememberCoroutineScope()

    var details by remember { mutableStateOf<WifiConnectionDetails?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshMetrics() {
        isRefreshing = true
        coroutineScope.launch {
            details = engine.inspect(context)
            isRefreshing = false
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            refreshMetrics()
        }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Wi-Fi Inspector",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            details?.let { info ->
                if (!info.isConnected) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        isGlow = false
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = "Disconnected",
                                tint = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Wi-Fi Disconnected",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = "No active Wi-Fi association detected on this device. Connect to a wireless access point in Settings to inspect metrics.",
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Primary Network Card
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        isGlow = true
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = info.ssid,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
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
                                        text = "${info.signalQuality} (${info.signalPercent}%)",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "RSSI: ${info.rssiDbm} dBm",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Text(
                                    text = "•",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Text(
                                    text = "Band: ${info.frequencyBand}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }

                    Text(
                        text = "Connection Metrics",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    AppListItem(
                        title = "Link Speed",
                        subtitle = "${info.linkSpeedMbps} Mbps",
                        leadingIcon = Icons.Outlined.Speed,
                        leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                        action = AppListItemAction.Chevron
                    )

                    AppListItem(
                        title = "Local IP Address",
                        subtitle = info.ipAddress,
                        leadingIcon = Icons.Outlined.Dns,
                        leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                        action = AppListItemAction.Chevron
                    )

                    AppListItem(
                        title = "Access Point BSSID",
                        subtitle = info.bssid,
                        leadingIcon = Icons.Outlined.Wifi,
                        leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                        action = AppListItemAction.Chevron
                    )

                    AppListItem(
                        title = "Interface Adapter",
                        subtitle = info.interfaceName,
                        leadingIcon = Icons.Outlined.Check,
                        leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                        action = AppListItemAction.Chevron
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                text = if (isRefreshing) "Refreshing..." else "Refresh Link Metrics",
                leadingIcon = Icons.Outlined.Refresh,
                variant = AppButtonVariant.Primary,
                enabled = !isRefreshing,
                onClick = { refreshMetrics() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
