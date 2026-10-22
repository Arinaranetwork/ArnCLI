// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.vault

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.model.CredentialType
import com.arinara.arntcli.ui.model.VaultCredential
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
    credentials: List<VaultCredential>,
    onToggleVaultLock: () -> Unit,
    onAddCredentialClick: () -> Unit,
    onCredentialClick: (VaultCredential) -> Unit = {},
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

        // Vault Security Card
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
                        text = if (isVaultUnlocked) "Vault Unlocked" else "Vault Locked",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Encrypted • AES-256-GCM",
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

        if (credentials.isEmpty()) {
            // Clean Empty State
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = false
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
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Empty Vault",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Keyring Is Empty",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    Text(
                        text = "Store SSH keys, WireGuard profiles, and API tokens.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AppButton(
                        text = "Add First Key",
                        leadingIcon = Icons.Outlined.Add,
                        variant = AppButtonVariant.Secondary,
                        onClick = onAddCredentialClick
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stored Credentials (${credentials.size})",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                if (isVaultUnlocked) {
                    AppButton(
                        text = "Add",
                        leadingIcon = Icons.Outlined.Add,
                        variant = AppButtonVariant.Secondary,
                        onClick = onAddCredentialClick
                    )
                }
            }

            credentials.forEach { cred ->
                val icon = when (cred.type) {
                    CredentialType.SshKey -> Icons.Outlined.Key
                    CredentialType.WireGuardConfig -> Icons.Outlined.VpnKey
                    CredentialType.Password -> Icons.Outlined.Password
                    CredentialType.ApiToken -> Icons.Outlined.Lock
                }

                AppListItem(
                    title = cred.title,
                    subtitle = cred.description,
                    leadingIcon = icon,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron,
                    onClick = { onCredentialClick(cred) }
                )
            }
        }

        Text(
            text = "Data Portability & Synchronization",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Air-Gapped Vault Sync (BC-UR)",
            subtitle = "Transmit encrypted credentials via animated QR stream without network emissions",
            leadingIcon = Icons.Outlined.QrCode,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
