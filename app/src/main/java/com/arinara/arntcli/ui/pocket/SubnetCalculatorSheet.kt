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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.net.SubnetCalculator
import com.arinara.arntcli.core.net.SubnetResult
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

@Composable
fun SubnetCalculatorSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetCidrs = listOf(
        "192.168.1.1/24",
        "10.0.0.1/16",
        "172.16.0.1/20",
        "192.168.0.1/28",
        "10.1.0.1/30"
    )

    var currentCidr by remember { mutableStateOf(presetCidrs.first()) }
    val calculationResult = remember(currentCidr) {
        SubnetCalculator.calculate(currentCidr).getOrNull()
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Subnet & CIDR Calculator",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CIDR / IP Notation",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (calculationResult != null) ElectricSlateTokens.BorderSubtle else ElectricSlateTokens.Destructive,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = currentCidr,
                    onValueChange = { currentCidr = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Preset Network Blocks",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Preset CIDR Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetCidrs) { cidr ->
                    AppPillChip(
                        label = cidr,
                        isSelected = currentCidr == cidr,
                        onClick = { currentCidr = cidr }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (calculationResult != null) {
                // Primary Result Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = calculationResult.networkAddress + "/" + calculationResult.prefixLength,
                                fontFamily = FontFamily.Monospace,
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
                                    text = "${calculationResult.usableHosts} usable hosts",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }
                        }

                        Text(
                            text = calculationResult.ipClass,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                // Breakdown Items
                AppListItem(
                    title = "Usable Host Range",
                    subtitle = "${calculationResult.firstUsableIp} - ${calculationResult.lastUsableIp}",
                    leadingIcon = Icons.Outlined.Calculate,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron
                )

                AppListItem(
                    title = "Subnet Mask",
                    subtitle = "Mask: ${calculationResult.netmask} • Wildcard: ${calculationResult.wildcardMask}",
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron
                )

                AppListItem(
                    title = "Broadcast IP Address",
                    subtitle = calculationResult.broadcastAddress,
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.TextMuted,
                    action = AppListItemAction.Chevron
                )

                AppListItem(
                    title = "Binary Bitmask",
                    subtitle = calculationResult.binaryNetmask,
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.TextMuted,
                    action = AppListItemAction.Chevron
                )
            } else {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = false
                ) {
                    Text(
                        text = "Invalid CIDR prefix. Provide a valid IPv4 address in CIDR format.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.Destructive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
