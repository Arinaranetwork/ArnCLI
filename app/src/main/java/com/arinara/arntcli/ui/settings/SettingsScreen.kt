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
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

/**
 * SettingsScreen - Application preferences, themes, and profile migration.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onExportProfilesClick: () -> Unit,
    onImportProfilesClick: () -> Unit,
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

        // Branding Banner Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            isGlow = true
        ) {
            Column {
                Text(
                    text = "ArnTCLI Settings",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricSlateTokens.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arinara Network • Electric Slate Design System • Elms Sans",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }
        }

        Text(
            text = "Visual System & Terminal Appearance",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Theme: Electric Slate Dark",
            subtitle = "Canvas #0D1117 • Surface #161C26 • Accent #7A5AF8",
            leadingIcon = Icons.Outlined.Palette,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Typography Hierarchy",
            subtitle = "Elms Sans (UI) & Monospace (Terminal Output)",
            leadingIcon = Icons.Outlined.TextFields,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        Text(
            text = "Profiles, Import & Export",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Export Workspace Profiles",
            subtitle = "Export Spaces, hosts, and preferences to encrypted JSON file",
            leadingIcon = Icons.Outlined.Upload,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onExportProfilesClick
        )

        AppListItem(
            title = "Import Workspace Profiles",
            subtitle = "Restore terminal environments, color schemes, and hosts",
            leadingIcon = Icons.Outlined.Download,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = onImportProfilesClick
        )

        Text(
            text = "Privacy & Verification",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Zero Cloud & Telemetry Guarantee",
            subtitle = "All operations run 100% locally on-device. Zero network tracking.",
            leadingIcon = Icons.Outlined.Security,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "About ArnTCLI v0.1.0",
            subtitle = "Arinara Network • VerifyAndroidRulesTask Active",
            leadingIcon = Icons.Outlined.Info,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
