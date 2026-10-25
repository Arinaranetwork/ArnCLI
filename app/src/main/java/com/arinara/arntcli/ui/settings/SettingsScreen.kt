// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppNoticeBanner
import com.arinara.arntcli.ui.components.presets.AppNoticeVariant
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

/**
 * SettingsScreen - Application preferences, themes, persistent state, and profile migration.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onExportProfilesClick: () -> Unit,
    onImportProfilesClick: () -> Unit,
    statusMessage: String? = null,
    isStatusError: Boolean = false,
    onDismissStatus: () -> Unit = {},
    persistedSummary: String = "Active (Spaces, Hosts, Vault, Tunnels & Shell CWD/Env)",
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

        AppNoticeBanner(
            visible = !statusMessage.isNullOrBlank(),
            title = if (isStatusError) "Profile Operation Failed" else "Workspace Profile Saved",
            message = statusMessage ?: "",
            variant = if (isStatusError) AppNoticeVariant.ERROR else AppNoticeVariant.SUCCESS,
            onDismiss = onDismissStatus
        )

        // Preferences Header Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            isGlow = true
        ) {
            Column {
                Text(
                    text = "Preferences",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Application configuration and persistent environment state",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }
        }

        Text(
            text = "Appearance",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Theme",
            subtitle = "Electric Slate Dark",
            leadingIcon = Icons.Outlined.Palette,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Terminal Font",
            subtitle = "System Monospace",
            leadingIcon = Icons.Outlined.TextFields,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        Text(
            text = "Workspace & Persistence",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Persistent State Engine",
            subtitle = persistedSummary,
            leadingIcon = Icons.Outlined.Storage,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Export Profiles",
            subtitle = "Export spaces, hosts, credentials, tunnels, and sessions to backup snapshot",
            leadingIcon = Icons.Outlined.Upload,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onExportProfilesClick
        )

        AppListItem(
            title = "Import Profiles",
            subtitle = "Restore workspace environments, hosts, and sessions from backup snapshot",
            leadingIcon = Icons.Outlined.Download,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onImportProfilesClick
        )

        Text(
            text = "Security & Privacy",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Telemetry",
            subtitle = "Disabled (100% On-device only)",
            leadingIcon = Icons.Outlined.Security,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "About ArnCLI",
            subtitle = "Version 1.0.2",
            leadingIcon = Icons.Outlined.Info,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
