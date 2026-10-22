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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.vault.VaultEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.model.CredentialType
import com.arinara.arntcli.ui.model.VaultCredential
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable

@Composable
fun AddCredentialSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    activeSpaceId: String,
    onSaveCredential: (VaultCredential) -> Unit,
    vaultPassphrase: String = "master-default-vault-key",
    modifier: Modifier = Modifier
) {
    var title by remember(visible) { mutableStateOf("") }
    var description by remember(visible) { mutableStateOf("") }
    var secret by remember(visible) { mutableStateOf("") }
    var selectedType by remember(visible) { mutableStateOf(CredentialType.SshKey) }
    var isSecretVisible by remember(visible) { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val vaultEngine = remember { VaultEngine() }
    val coroutineScope = rememberCoroutineScope()
    val types = listOf(
        CredentialType.SshKey to "SSH Key",
        CredentialType.Password to "Password",
        CredentialType.ApiToken to "API Token",
        CredentialType.WireGuardConfig to "WireGuard"
    )

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Store Credential",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Credential Type",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(types) { (type, label) ->
                    AppPillChip(
                        label = label,
                        isSelected = selectedType == type,
                        onClick = {
                            selectedType = type
                            if (title.isBlank()) {
                                title = when (type) {
                                    CredentialType.SshKey -> "id_ed25519"
                                    CredentialType.Password -> "Server Admin Password"
                                    CredentialType.ApiToken -> "API Secret Key"
                                    CredentialType.WireGuardConfig -> "wg0-client.conf"
                                }
                            }
                        }
                    )
                }
            }

            Text(
                text = "Credential Label",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Title Input
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
                    value = title,
                    onValueChange = {
                        title = it
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
            }

            Text(
                text = "Description / Metadata",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Description Input
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
                    value = description,
                    onValueChange = { description = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Secret Content",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
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
                            contentDescription = "Toggle Secret Visibility",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.height(14.dp)
                        )
                        Text(
                            text = if (isSecretVisible) "Hide" else "Reveal",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.PrimaryAccent
                        )
                    }
                }
            }

            // Secret Input Box
            val isMultiline = selectedType == CredentialType.SshKey || selectedType == CredentialType.WireGuardConfig
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isMultiline) 110.dp else 48.dp)
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
                    value = secret,
                    onValueChange = {
                        secret = it
                        errorMessage = null
                    },
                    singleLine = !isMultiline,
                    visualTransformation = if (!isSecretVisible && !isMultiline) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (!isMultiline && !isSecretVisible) KeyboardType.Password else KeyboardType.Text),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                text = "Encrypt & Store",
                leadingIcon = Icons.Outlined.Add,
                variant = AppButtonVariant.Primary,
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        errorMessage = "Title cannot be blank"
                        return@AppButton
                    }
                    if (secret.isBlank()) {
                        errorMessage = "Secret cannot be blank"
                        return@AppButton
                    }

                    coroutineScope.launch {
                        try {
                            val encrypted = vaultEngine.encrypt(secret.trim(), vaultPassphrase)
                            val newCred = VaultCredential(
                                id = "cred-${System.currentTimeMillis()}",
                                title = cleanTitle,
                                type = selectedType,
                                description = description.trim().ifBlank { "${selectedType.name} credential" },
                                spaceId = activeSpaceId,
                                encryptedSecret = encrypted
                            )
                            onSaveCredential(newCred)
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Encryption failed"
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
