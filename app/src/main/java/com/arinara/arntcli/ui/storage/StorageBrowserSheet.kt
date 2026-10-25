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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.storage.StorageNode
import com.arinara.arntcli.core.storage.StorageType
import com.arinara.arntcli.core.terminal.BuiltinToolbox
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import java.io.File

/**
 * StorageBrowserSheet - Real on-device Workspace (~) File Explorer.
 * Browses, previews, copies, and manages real files inside the persistent shell home directory.
 */
@Composable
fun StorageBrowserSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    rootDirectory: File? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val homeDir = remember(rootDirectory, context) {
        (rootDirectory ?: File(context.filesDir, "home")).apply {
            mkdirs()
            BuiltinToolbox.ensureWorkspaceSeeded(this)
        }
    }

    var currentDir by remember(homeDir) { mutableStateOf(homeDir) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            refreshTrigger += 1
        }
    }

    val nodes = remember(currentDir, refreshTrigger) {
        val children = currentDir.listFiles()?.toList().orEmpty()
        children
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                val mode = (if (file.canRead()) 292 else 0) or
                    (if (file.canWrite()) 128 else 0) or
                    (if (file.canExecute() || file.isDirectory) 73 else 0)
                StorageNode(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = if (file.isDirectory) 0L else file.length().coerceAtLeast(0L),
                    isDirectory = file.isDirectory,
                    storageType = StorageType.Smb,
                    permissionsOctal = mode,
                    lastModified = file.lastModified()
                )
            }
    }

    val filteredNodes = remember(nodes, searchQuery) {
        if (searchQuery.isBlank()) nodes
        else nodes.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val relativePath = remember(currentDir, homeDir) {
        val rel = currentDir.absolutePath.removePrefix(homeDir.absolutePath).replace('\\', '/')
        if (rel.isEmpty()) "~" else "~$rel"
    }
    val canGoUp = currentDir.absolutePath != homeDir.absolutePath && currentDir.parentFile != null

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Workspace File Explorer",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDir.absolutePath,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AppButton(
                    text = "Refresh",
                    leadingIcon = Icons.Outlined.Refresh,
                    variant = AppButtonVariant.Secondary,
                    onClick = {
                        refreshTrigger += 1
                        statusBanner = null
                    }
                )
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
                        .background(if (canGoUp) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                        .jigglePressClickable {
                            if (canGoUp) {
                                currentDir = currentDir.parentFile ?: homeDir
                                selectedFile = null
                                statusBanner = null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Parent Directory",
                        tint = if (canGoUp) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
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
                            onValueChange = { searchQuery = it },
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
                                        text = "Filter workspace files...",
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
                    text = "Path: $relativePath (${filteredNodes.size} items)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.PrimaryAccent
                )
            }

            statusBanner?.let { msg ->
                Text(
                    text = msg,
                    fontFamily = ElmsSans,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.PrimaryAccent
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
                            text = "Create files from the terminal using touch, mkdir, npm, pip, or git clone.",
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
                            val targetFile = File(node.path)
                            if (targetFile.isDirectory) {
                                currentDir = targetFile
                                selectedFile = null
                                statusBanner = null
                            } else {
                                selectedFile = targetFile
                                statusBanner = null
                            }
                        }
                    )
                }
            }

            // File Details & Live Content Preview Card (when regular file is selected)
            selectedFile?.let { file ->
                if (file.exists() && file.isFile) {
                    val previewText = remember(file, refreshTrigger) {
                        try {
                            file.readText(Charsets.UTF_8).take(1500)
                        } catch (e: Exception) {
                            "[Binary or unreadable file: ${e.message}]"
                        }
                    }

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
                                    text = file.name,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "${file.length()} B",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                            }

                            Text(
                                text = file.absolutePath,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ElectricSlateTokens.TextMuted
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = previewText.ifEmpty { "(Empty file)" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary,
                                    maxLines = 16,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = "Copy Content",
                                    leadingIcon = Icons.Outlined.ContentCopy,
                                    variant = AppButtonVariant.Primary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(previewText))
                                        statusBanner = "Copied ${file.name} content to clipboard."
                                    }
                                )

                                AppButton(
                                    text = "Delete File",
                                    leadingIcon = Icons.Outlined.DeleteOutline,
                                    variant = AppButtonVariant.Secondary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val name = file.name
                                        if (file.delete()) {
                                            selectedFile = null
                                            refreshTrigger += 1
                                            statusBanner = "Deleted $name from workspace."
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            BottomClearanceSpacer()
        }
    }
}
