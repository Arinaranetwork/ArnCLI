// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.storage

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.storage.StorageEngine
import com.arinara.arntcli.core.storage.StorageNode
import com.arinara.arntcli.core.storage.StorageType
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable

/**
 * StorageBrowserSheet - Multi-protocol cloud & NAS remote storage explorer.
 */
@Composable
fun StorageBrowserSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { StorageEngine(StorageType.Smb) }
    val currentType by engine.storageType.collectAsState()
    val currentPath by engine.currentPath.collectAsState()
    val nodes by engine.nodes.collectAsState()
    val searchQuery by engine.searchQuery.collectAsState()

    var selectedNode by remember { mutableStateOf<StorageNode?>(null) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }

    val filteredNodes = remember(nodes, searchQuery) {
        if (searchQuery.isBlank()) nodes
        else nodes.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Remote Storage Explorer",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = engine.currentUri(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AppButton(
                    text = "Close",
                    variant = AppButtonVariant.Secondary,
                    onClick = onDismiss
                )
            }

            // Protocol Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StorageType.values()) { type ->
                    AppPillChip(
                        label = type.displayName,
                        isSelected = currentType == type,
                        onClick = {
                            engine.setStorageType(type)
                            selectedNode = null
                            downloadStatus = null
                        }
                    )
                }
            }

            // Breadcrumbs & Search Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Up Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (currentPath != "/") ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                        .jigglePressClickable {
                            engine.cdUp()
                            selectedNode = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Parent Directory",
                        tint = if (currentPath != "/") ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Search Filter Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricSlateTokens.SurfaceElevated)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { engine.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = ElmsSans,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            ),
                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Filter files in current folder...",
                                        fontFamily = ElmsSans,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }

            // Path Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(ElectricSlateTokens.SurfacePanel)
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Path: $currentPath (${filteredNodes.size} items)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }

            // Node Listing
            if (filteredNodes.isEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Directory is empty",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Text(
                            text = "No remote objects or files found under this path.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                }
            } else {
                filteredNodes.forEach { node ->
                    val icon = if (node.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile
                    val iconTint = if (node.isDirectory) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                    val subtitle = if (node.isDirectory) {
                        "${node.toPermissionsString()} • Directory"
                    } else {
                        "${node.toPermissionsString()} • ${node.formattedSize()}"
                    }

                    AppListItem(
                        title = node.name,
                        subtitle = subtitle,
                        leadingIcon = icon,
                        leadingIconTint = iconTint,
                        action = if (node.isDirectory) AppListItemAction.Chevron else null,
                        onClick = {
                            if (node.isDirectory) {
                                engine.cd(node.name)
                                selectedNode = null
                            } else {
                                selectedNode = node
                                downloadStatus = null
                            }
                        }
                    )
                }
            }

            // File Details Inspector Card (when regular file is selected)
            selectedNode?.let { node ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "File Details: ${node.name}",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = node.formattedSize(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.PrimaryAccent
                            )
                        }

                        Text(
                            text = "Full Path: ${node.path}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        if (node.etag != null) {
                            Text(
                                text = "ETag: ${node.etag}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        AppButton(
                            text = if (downloadStatus != null) "Downloaded" else "Download via SAF",
                            leadingIcon = if (downloadStatus != null) Icons.Outlined.Check else Icons.Outlined.CloudDownload,
                            variant = AppButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                downloadStatus = "Queued for download: ${node.name}"
                            }
                        )
                    }
                }
            }

            BottomClearanceSpacer()
        }
    }
}
