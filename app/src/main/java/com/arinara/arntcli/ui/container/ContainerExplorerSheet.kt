// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.container

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.container.ContainerItem
import com.arinara.arntcli.core.container.ContainerPortMapping
import com.arinara.arntcli.core.container.ContainerState
import com.arinara.arntcli.core.container.DockerDemuxParser
import com.arinara.arntcli.core.container.DockerImageItem
import com.arinara.arntcli.core.container.DockerLogEntry
import com.arinara.arntcli.core.container.DockerRemoteEngine
import com.arinara.arntcli.core.container.KubeClusterEngine
import com.arinara.arntcli.core.container.KubePodItem
import com.arinara.arntcli.core.container.LogStreamType
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

enum class ContainerTab {
    Containers,
    Images,
    Kubernetes
}

@Composable
fun ContainerExplorerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hostName: String = "Local Machine",
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ContainerTab.Containers) }
    val coroutineScope = rememberCoroutineScope()

    // Sample / Live Containers State
    var containers by remember { mutableStateOf(emptyList<ContainerItem>()) }
    var images by remember { mutableStateOf(emptyList<DockerImageItem>()) }
    var pods by remember { mutableStateOf(emptyList<KubePodItem>()) }

    var isRefreshing by remember { mutableStateOf(false) }

    // Logs Viewer State
    var selectedContainerForLogs by remember { mutableStateOf<ContainerItem?>(null) }
    var containerLogs by remember { mutableStateOf(emptyList<DockerLogEntry>()) }

    fun refreshData() {
        coroutineScope.launch {
            isRefreshing = true
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://127.0.0.1:2375/v1.43/containers/json?all=1")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 800
                    conn.readTimeout = 800
                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        containers = DockerRemoteEngine.parseContainersJson(body)
                    } else {
                        containers = emptyList()
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                    containers = emptyList()
                }

                try {
                    val url = URL("http://127.0.0.1:2375/v1.43/images/json")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 800
                    conn.readTimeout = 800
                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        images = DockerRemoteEngine.parseImagesJson(body)
                    } else {
                        images = emptyList()
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                    images = emptyList()
                }

                pods = emptyList()
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(visible) {
        if (visible && containers.isEmpty()) {
            refreshData()
        }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Containers & Kubernetes",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Docker, Podman & Kubernetes Orchestration [$hostName]",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted
            )

            // Tabs Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "Containers (${containers.size})",
                        leadingIcon = Icons.Outlined.Layers,
                        isSelected = activeTab == ContainerTab.Containers,
                        onClick = { activeTab = ContainerTab.Containers }
                    )
                }
                item {
                    AppPillChip(
                        label = "Images (${images.size})",
                        leadingIcon = Icons.Outlined.CloudQueue,
                        isSelected = activeTab == ContainerTab.Images,
                        onClick = { activeTab = ContainerTab.Images }
                    )
                }
                item {
                    AppPillChip(
                        label = "Kubernetes (${pods.size})",
                        leadingIcon = Icons.Outlined.CheckCircle,
                        isSelected = activeTab == ContainerTab.Kubernetes,
                        onClick = { activeTab = ContainerTab.Kubernetes }
                    )
                }
            }

            when (activeTab) {
                ContainerTab.Containers -> {
                    if (containers.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Containers Detected",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "Connect to a Docker daemon or forward socket /var/run/docker.sock via SSH tunnel to inspect running containers.",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        containers.forEach { container ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = container.displayName,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "${container.image} • ${container.shortId}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                when (container.state) {
                                                    ContainerState.Running -> ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                                                    ContainerState.Paused -> ElectricSlateTokens.TextMuted.copy(alpha = 0.15f)
                                                    ContainerState.Exited, ContainerState.Dead -> ElectricSlateTokens.Destructive.copy(alpha = 0.15f)
                                                    else -> ElectricSlateTokens.TrackInactive
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = container.state.name.uppercase(),
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (container.state) {
                                                ContainerState.Running -> ElectricSlateTokens.PrimaryAccent
                                                ContainerState.Paused -> ElectricSlateTokens.TextMuted
                                                ContainerState.Exited, ContainerState.Dead -> ElectricSlateTokens.Destructive
                                                else -> ElectricSlateTokens.TextMuted
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = container.statusText,
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )

                                if (container.ports.isNotEmpty()) {
                                    Text(
                                        text = "Ports: " + container.ports.joinToString(", ") { it.formatDisplay() },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppButton(
                                        text = "Logs",
                                        leadingIcon = Icons.Outlined.Terminal,
                                        variant = AppButtonVariant.Secondary,
                                        onClick = {
                                            selectedContainerForLogs = container
                                            containerLogs = listOf(
                                                DockerLogEntry(LogStreamType.Stdout, "[system] Container initialized on $hostName"),
                                                DockerLogEntry(LogStreamType.Stdout, "Starting server on port 80..."),
                                                DockerLogEntry(LogStreamType.Stdout, "Worker processes ready for traffic.")
                                            )
                                        }
                                    )

                                    AppButton(
                                        text = if (container.isRunning) "Restart" else "Start",
                                        leadingIcon = if (container.isRunning) Icons.Outlined.Refresh else Icons.Outlined.PlayArrow,
                                        variant = AppButtonVariant.Primary,
                                        onClick = {
                                            containers = containers.map { c ->
                                                if (c.id == container.id) {
                                                    c.copy(state = ContainerState.Running, statusText = "Up 1 second")
                                                } else c
                                            }
                                        }
                                    )

                                    if (container.isRunning) {
                                        AppButton(
                                            text = "Stop",
                                            leadingIcon = Icons.Outlined.Stop,
                                            variant = AppButtonVariant.Secondary,
                                            onClick = {
                                                containers = containers.map { c ->
                                                    if (c.id == container.id) {
                                                        c.copy(state = ContainerState.Exited, statusText = "Exited (0) Just now")
                                                    } else c
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ContainerTab.Images -> {
                    if (images.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Docker Images Found",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "Images pulled or built on the remote Docker host will appear here.",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        images.forEach { img ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = img.primaryTag,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "ID: ${img.shortId}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }

                                    Text(
                                        text = img.formattedSize(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                }
                            }
                        }
                    }
                }

                ContainerTab.Kubernetes -> {
                    if (pods.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Kubernetes Pods Found",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "Configure ~/.kube/config or establish a cluster proxy to inspect active namespaces and pods.",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        pods.forEach { pod ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pod.name,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                            Text(
                                                text = "Namespace: ${pod.namespace} • Node: ${pod.nodeName ?: "auto"}",
                                                fontFamily = ElmsSans,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (pod.isRunning) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f) else ElectricSlateTokens.Destructive.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = pod.statusPhase.uppercase(),
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (pod.isRunning) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.Destructive
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "IP: ${pod.podIp ?: "none"}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "Restarts: ${pod.restartCount}",
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = if (pod.restartCount > 5) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AppButton(
                text = if (isRefreshing) "Refreshing Endpoints..." else "Refresh Resources",
                leadingIcon = Icons.Outlined.Refresh,
                variant = AppButtonVariant.Primary,
                onClick = { refreshData() }
            )

            BottomClearanceSpacer()
        }
    }

    // Secondary Logs Sheet
    selectedContainerForLogs?.let { c ->
        ContainerLogsSheet(
            visible = true,
            containerName = c.displayName,
            logs = containerLogs,
            onDismiss = { selectedContainerForLogs = null },
            onClearLogs = { containerLogs = emptyList() }
        )
    }
}
