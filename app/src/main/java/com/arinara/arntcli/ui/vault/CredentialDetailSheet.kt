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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.vault.VaultEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.model.CredentialType
import com.arinara.arntcli.ui.model.VaultCredential
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CredentialDetailSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    credential: VaultCredential?,
    onDeleteCredential: (VaultCredential) -> Unit,
    vaultPassphrase: String = "master-default-vault-key",
    modifier: Modifier = Modifier
) {
    if (credential == null) return

    val clipboardManager = LocalClipboardManager.current
    val vaultEngine = remember { VaultEngine() }
    var isSecretVisible by remember(credential.id) { mutableStateOf(false) }
    var isCopied by remember(credential.id) { mutableStateOf(false) }

    val formattedDate = remember(credential.createdAt) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sdf.format(Date(credential.createdAt))
    }

    val decryptedSecret = remember(credential.encryptedSecret, vaultPassphrase) {
        if (credential.encryptedSecret.isBlank()) {
            ""
        } else {
            try {
                vaultEngine.decrypt(credential.encryptedSecret, vaultPassphrase)
            } catch (_: Exception) {
                "[Decryption Error: Check Master Key]"
            }
        }
    }

    val typeIcon = when (credential.type) {
        CredentialType.SshKey -> Icons.Outlined.Key
        CredentialType.WireGuardConfig -> Icons.Outlined.VpnKey
        CredentialType.Password -> Icons.Outlined.Password
        CredentialType.ApiToken -> Icons.Outlined.Lock
    }

    val typeLabel = when (credential.type) {
        CredentialType.SshKey -> "SSH Key"
        CredentialType.WireGuardConfig -> "WireGuard Config"
        CredentialType.Password -> "Password"
        CredentialType.ApiToken -> "API Token"
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Credential Details",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                            imageVector = typeIcon,
                            contentDescription = typeLabel,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = credential.title,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Text(
                            text = credential.description,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }

                    AppPillChip(
                        label = typeLabel,
                        isSelected = true,
                        onClick = {}
                    )
                }
            }

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created: $formattedDate",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )

                Text(
                    text = "Cipher: AES-256-GCM",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }

            // Secret Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protected Secret",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reveal/Mask Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .jigglePressClickable { isSecretVisible = !isSecretVisible }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSecretVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle Secret",
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSecretVisible) "Mask" else "Reveal",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }
                    }

                    // Copy Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .jigglePressClickable {
                                if (decryptedSecret.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(decryptedSecret))
                                    isCopied = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Secret",
                                tint = if (isCopied) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isCopied) "Copied" else "Copy",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (isCopied) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                            )
                        }
                    }
                }
            }

            // Secret Value Container
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
                    .padding(14.dp)
            ) {
                val displayText = when {
                    decryptedSecret.isBlank() -> "(No payload stored)"
                    isSecretVisible -> decryptedSecret
                    else -> VaultEngine.maskSecret(decryptedSecret)
                }

                Text(
                    text = displayText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isSecretVisible) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Delete Action Button
            AppButton(
                text = "Delete Credential",
                leadingIcon = Icons.Outlined.Delete,
                variant = AppButtonVariant.Destructive,
                onClick = {
                    onDeleteCredential(credential)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
