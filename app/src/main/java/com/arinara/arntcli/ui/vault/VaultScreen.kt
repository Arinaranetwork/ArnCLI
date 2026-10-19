// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

/**
 * VaultScreen - Cryptographic hardware-backed keyring.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun VaultScreen(
    contentPadding: PaddingValues,
    isVaultUnlocked: Boolean,
    onToggleVaultLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Vault Cryptographic Security Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            isGlow = isVaultUnlocked
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isVaultUnlocked) "Vault Active & Unlocked" else "Vault Locked",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AES-256-GCM • Argon2id (m=64M) • StrongBox TEE",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                AppButton(
                    text = if (isVaultUnlocked) "Lock" else "Unlock",
                    leadingIcon = if (isVaultUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    variant = if (isVaultUnlocked) AppButtonVariant.Secondary else AppButtonVariant.Primary,
                    onClick = onToggleVaultLock
                )
            }
        }

        Text(
            text = "Hardware Keyring & Stored Credentials",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "id_ed25519 (Production Infrastructure)",
            subtitle = "SSH Private Key • Ed25519 256-bit • Fingerprint: SHA256:4x8k..",
            leadingIcon = Icons.Outlined.Key,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "wireguard-homelab.conf",
            subtitle = "WireGuard Tunnel Profile • Endpoint: vpn.home.lan:51820",
            leadingIcon = Icons.Outlined.VpnKey,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Hardware Token: YubiKey 5C NFC",
            subtitle = "FIDO2 hmac-secret & PIV 9D • Bound via NFC / USB-OTG",
            leadingIcon = Icons.Outlined.Fingerprint,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Air-Gapped Vault Sync (BC-UR)",
            subtitle = "Transmit encrypted credentials via animated QR stream",
            leadingIcon = Icons.Outlined.QrCode,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
