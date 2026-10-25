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
import androidx.compose.material.icons.outlined.Power
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
import com.arinara.arntcli.core.net.IeeeOuiLookupEngine
import com.arinara.arntcli.core.net.WolEngine
import com.arinara.arntcli.core.net.WolResult
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
fun WolSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetMacs = listOf(
        "B8:27:EB:4A:89:12",
        "E4:5F:01:92:AB:33",
        "00:15:6D:88:99:AA"
    )

    var targetMac by remember { mutableStateOf(presetMacs.first()) }
    var broadcastIp by remember { mutableStateOf(WolEngine.DEFAULT_BROADCAST_IP) }
    var selectedPort by remember { mutableStateOf(WolEngine.DEFAULT_PORT) }
    var isSending by remember { mutableStateOf(false) }
    var resultReport by remember { mutableStateOf<WolResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val engine = remember { WolEngine() }
    val ouiEngine = remember { IeeeOuiLookupEngine() }
    val detectedVendor = remember(targetMac) { ouiEngine.lookupVendor(targetMac) }
    val coroutineScope = rememberCoroutineScope()

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Wake-on-LAN",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Target MAC Address",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Target MAC Input
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
                    value = targetMac,
                    onValueChange = {
                        targetMac = it
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

            if (detectedVendor != null) {
                Text(
                    text = "Hardware Vendor: $detectedVendor",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.PrimaryAccent
                )
            }

            // MAC Presets
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetMacs) { mac ->
                    AppPillChip(
                        label = mac,
                        isSelected = targetMac == mac,
                        onClick = {
                            targetMac = mac
                            errorMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Broadcast Destination",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Broadcast IP Box
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
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = broadcastIp,
                        onValueChange = { broadcastIp = it },
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

                // Port 9 Pill
                AppPillChip(
                    label = "Port 9",
                    isSelected = selectedPort == 9,
                    onClick = { selectedPort = 9 }
                )

                // Port 7 Pill
                AppPillChip(
                    label = "Port 7",
                    isSelected = selectedPort == 7,
                    onClick = { selectedPort = 7 }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            AppButton(
                text = if (isSending) "Broadcasting..." else "Send Magic Packet",
                leadingIcon = Icons.Outlined.Power,
                variant = AppButtonVariant.Primary,
                enabled = !isSending,
                onClick = {
                    isSending = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = engine.sendMagicPacket(
                            macAddress = targetMac,
                            broadcastIp = broadcastIp,
                            port = selectedPort
                        )
                        isSending = false
                        result.onSuccess { report ->
                            resultReport = report
                        }.onFailure { err ->
                            errorMessage = err.localizedMessage ?: "Failed to broadcast magic packet"
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

            resultReport?.let { report ->
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
                                text = "Magic Packet Dispatched",
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
                                    text = "${report.bytesSent} bytes",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }
                        }

                        Text(
                            text = "Destination: ${report.broadcastIp}:${report.port}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                AppListItem(
                    title = "Target MAC: ${report.formattedMac}",
                    subtitle = "UDP broadcast sent in ${String.format("%.2f", report.durationMs)}ms",
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
