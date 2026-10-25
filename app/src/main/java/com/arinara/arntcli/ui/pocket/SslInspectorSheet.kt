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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.net.SslInspectionResult
import com.arinara.arntcli.core.net.SslInspectorEngine
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
fun SslInspectorSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetHosts = listOf("cloudflare.com", "google.com", "github.com", "arinara.network")
    var targetHost by remember { mutableStateOf(presetHosts.first()) }
    var isInspecting by remember { mutableStateOf(false) }
    var inspectionResult by remember { mutableStateOf<SslInspectionResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val inspectorEngine = remember { SslInspectorEngine() }
    val coroutineScope = rememberCoroutineScope()

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "SSL / TLS Certificate Inspector",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Target Hostname (HTTPS / Port 443)",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Target Input Box
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
                    onValueChange = { targetHost = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {}),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Preset Target Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetHosts) { host ->
                    AppPillChip(
                        label = host,
                        isSelected = targetHost == host,
                        onClick = { targetHost = host }
                    )
                }
            }

            // Action Button: Inspect
            AppButton(
                text = if (isInspecting) "Negotiating TLS Handshake..." else "Inspect Certificate",
                leadingIcon = Icons.Outlined.Security,
                variant = AppButtonVariant.Primary,
                onClick = {
                    isInspecting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val res = inspectorEngine.inspect(targetHost)
                        res.fold(
                            onSuccess = {
                                inspectionResult = it
                                errorMessage = null
                            },
                            onFailure = {
                                inspectionResult = null
                                errorMessage = it.message ?: "TLS Handshake or Certificate validation failed"
                            }
                        )
                        isInspecting = false
                    }
                }
            )

            if (errorMessage != null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = false
                ) {
                    Text(
                        text = errorMessage ?: "Unknown inspection error",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.Destructive
                    )
                }
            }

            inspectionResult?.let { result ->
                val cert = result.certificate

                // Primary Certificate Card
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
                                text = cert.subjectCn,
                                fontFamily = FontFamily.Monospace,
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
                                    text = if (cert.isExpired) "EXPIRED" else "${cert.daysRemaining}d remaining",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (cert.isExpired || cert.daysRemaining < 14) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                                )
                            }
                        }

                        Text(
                            text = "${cert.protocol} • ${cert.cipherSuite}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }

                Text(
                    text = "Certificate Properties",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                AppListItem(
                    title = "Issuer Authority",
                    subtitle = cert.issuerCn,
                    leadingIcon = Icons.Outlined.Lock,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron
                )

                AppListItem(
                    title = "Validity Window",
                    subtitle = "${cert.validFrom} to ${cert.validUntil}",
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.PrimaryAccent,
                    action = AppListItemAction.Chevron
                )

                AppListItem(
                    title = "SHA-256 Fingerprint",
                    subtitle = cert.sha256Fingerprint,
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.TextMuted,
                    action = AppListItemAction.Chevron
                )

                if (cert.subjectAltNames.isNotEmpty()) {
                    AppListItem(
                        title = "SAN DNS Names (${cert.subjectAltNames.size})",
                        subtitle = cert.subjectAltNames.take(5).joinToString(", ") + if (cert.subjectAltNames.size > 5) "..." else "",
                        leadingIcon = Icons.Outlined.Check,
                        leadingIconTint = ElectricSlateTokens.TextMuted,
                        action = AppListItemAction.Chevron
                    )
                }

                AppListItem(
                    title = "Certificate Chain & Latency",
                    subtitle = "${cert.chainLength} certificates in chain • ${result.handshakeDurationMs}ms handshake",
                    leadingIcon = Icons.Outlined.Check,
                    leadingIconTint = ElectricSlateTokens.TextMuted,
                    action = AppListItemAction.Chevron
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
