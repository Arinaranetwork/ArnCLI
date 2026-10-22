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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VpnKey
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.wireguard.WireGuardConfig
import com.arinara.arntcli.core.wireguard.WireGuardConfigParser
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.model.CredentialType
import com.arinara.arntcli.ui.model.VaultCredential
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

@Composable
fun WireGuardSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaveToVault: (VaultCredential) -> Unit = {},
    activeSpaceId: String = "default",
    modifier: Modifier = Modifier
) {
    val sampleConf = remember {
        """
        [Interface]
        PrivateKey = aA11bB22cC33dD44eE55fF66gG77hH88iI99jJ00kK8=
        Address = 10.8.0.2/24, fd00::2/64
        DNS = 1.1.1.1, 1.0.0.1
        MTU = 1420

        [Peer]
        PublicKey = 88Hh77Gg66Ff55Ee44Dd33Cc22Bb11Aa00Jj99Ii88M=
        Endpoint = vpn.home.arntcli.net:51820
        AllowedIPs = 0.0.0.0/0, ::/0
        PersistentKeepalive = 25
        """.trimIndent()
    }

    val sampleCloudConf = remember {
        """
        [Interface]
        PrivateKey = aA11bB22cC33dD44eE55fF66gG77hH88iI99jJ00kK8=
        Address = 10.10.0.5/24
        DNS = 8.8.8.8

        [Peer]
        PublicKey = 11Aa22Bb33Cc44Dd55Ee66Ff77Gg88Hh99Ii00Jj11K=
        Endpoint = vpc.cloud.arntcli.net:51820
        AllowedIPs = 10.10.0.0/16
        PersistentKeepalive = 30
        """.trimIndent()
    }

    var profileName by remember(visible) { mutableStateOf("wg0-home") }
    var rawConf by remember(visible) { mutableStateOf(sampleConf) }
    var isCopied by remember(visible) { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    val parseResult = remember(profileName, rawConf) {
        WireGuardConfigParser.parse(profileName, rawConf)
    }
    val parsedConfig = parseResult.getOrNull()
    val parseError = parseResult.exceptionOrNull()?.message

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "WireGuard Profile",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Preset Profiles",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "Home Gateway",
                        isSelected = rawConf == sampleConf,
                        onClick = {
                            profileName = "wg0-home"
                            rawConf = sampleConf
                        }
                    )
                }
                item {
                    AppPillChip(
                        label = "Cloud VPC",
                        isSelected = rawConf == sampleCloudConf,
                        onClick = {
                            profileName = "wg0-cloud"
                            rawConf = sampleCloudConf
                        }
                    )
                }
            }

            Text(
                text = "Profile Name",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Name Input
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
                    value = profileName,
                    onValueChange = { profileName = it },
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
            }

            Text(
                text = "Configuration Syntax (.conf)",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Raw Configuration Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (parseError != null) ElectricSlateTokens.Destructive else ElectricSlateTokens.BorderSubtle,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = rawConf,
                    onValueChange = { rawConf = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (parseError != null) {
                Text(
                    text = "Syntax Error: $parseError",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.Destructive
                )
            }

            // Parsed Tunnel Parameters Card
            parsedConfig?.let { config ->
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = "WireGuard",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tunnel Validated",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                        }

                        Text(
                            text = "Assigned IP: ${config.iface.addresses.joinToString(", ")}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )

                        if (config.iface.dnsServers.isNotEmpty()) {
                            Text(
                                text = "DNS Servers: ${config.iface.dnsServers.joinToString(", ")}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }

                        val peer = config.peers.firstOrNull()
                        if (peer != null) {
                            peer.endpoint?.let {
                                Text(
                                    text = "Endpoint: $it",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                            Text(
                                text = "Allowed IPs: ${peer.allowedIps.joinToString(", ")}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                            peer.persistentKeepalive?.let {
                                Text(
                                    text = "Keepalive: ${it}s",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = if (isCopied) "Copied" else "Copy .conf",
                        leadingIcon = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        variant = AppButtonVariant.Secondary,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(config.toConfString()))
                            isCopied = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AppButton(
                        text = "Store in Vault",
                        leadingIcon = Icons.Outlined.Lock,
                        variant = AppButtonVariant.Primary,
                        onClick = {
                            val cred = VaultCredential(
                                id = "wg-${System.currentTimeMillis()}",
                                title = config.name,
                                type = CredentialType.WireGuardConfig,
                                description = "WireGuard Tunnel (${config.iface.addresses.firstOrNull() ?: ""})",
                                spaceId = activeSpaceId,
                                encryptedSecret = config.toConfString()
                            )
                            onSaveToVault(cred)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
