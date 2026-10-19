// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.model.Space
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

@Composable
fun SpaceSwitcherSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    spaces: List<Space>,
    selectedSpaceId: String,
    onSelectSpace: (Space) -> Unit,
    onAddSpaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Switch Space",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {


            spaces.forEach { space ->
                val isSelected = space.id == selectedSpaceId
                val icon = when (space.id) {
                    "homelab" -> Icons.Outlined.Home
                    "cloud" -> Icons.Outlined.CloudQueue
                    else -> Icons.Outlined.Dns
                }

                AppListItem(
                    title = space.name,
                    subtitle = "${space.hostCount} hosts • ${if (space.isVpnActive) "WireGuard active" else "Direct routing"}",
                    leadingIcon = icon,
                    leadingIconTint = if (isSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                    action = if (isSelected) {
                        AppListItemAction.Custom {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Active",
                                tint = ElectricSlateTokens.PrimaryAccent
                            )
                        }
                    } else {
                        AppListItemAction.Chevron
                    },
                    onClick = {
                        onSelectSpace(space)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                text = "Create New Space",
                leadingIcon = Icons.Outlined.Add,
                variant = AppButtonVariant.Secondary,
                onClick = onAddSpaceClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
