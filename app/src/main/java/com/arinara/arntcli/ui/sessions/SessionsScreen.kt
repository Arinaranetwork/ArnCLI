// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.sessions

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Storage
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
 * SessionsScreen - Manage remote hosts and tunnels.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun SessionsScreen(
    contentPadding: PaddingValues,
    activeSpaceName: String,
    onAddHostClick: () -> Unit,
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

        // Space Overview Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            isGlow = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$activeSpaceName Space",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "4 configured hosts • 2 persistent tunnels",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                AppButton(
                    text = "Add Host",
                    leadingIcon = Icons.Outlined.Add,
                    variant = AppButtonVariant.Primary,
                    onClick = onAddHostClick
                )
            }
        }

        Text(
            text = "Configured Remote Hosts",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Bastion Production Bastion",
            subtitle = "ssh root@10.0.1.1:22 • Jump Host • Ed25519",
            leadingIcon = Icons.Outlined.Dns,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Homelab TrueNAS SCALE",
            subtitle = "ssh admin@192.168.1.100 • ZFS Pool • WireGuard",
            leadingIcon = Icons.Outlined.Storage,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Edge Gateway Mikrotik Router",
            subtitle = "ssh admin@192.168.1.1:22 • RouterOS 7.15",
            leadingIcon = Icons.Outlined.Router,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Dev Workstation Linux",
            subtitle = "ssh sepli@192.168.1.50 • Port Forward :8080",
            leadingIcon = Icons.Outlined.Computer,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
