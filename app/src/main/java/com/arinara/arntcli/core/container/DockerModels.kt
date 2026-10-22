// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import java.util.Locale

enum class ContainerState {
    Created,
    Running,
    Paused,
    Restarting,
    Removing,
    Exited,
    Dead,
    Unknown;

    companion object {
        fun fromString(value: String): ContainerState {
            val normalized = value.lowercase().trim().split(" ", "(", "-").firstOrNull() ?: ""
            return when (normalized) {
                "created" -> Created
                "running" -> Running
                "paused" -> Paused
                "restarting" -> Restarting
                "removing" -> Removing
                "exited" -> Exited
                "dead" -> Dead
                else -> Unknown
            }
        }
    }
}

data class ContainerPortMapping(
    val ip: String = "0.0.0.0",
    val privatePort: Int,
    val publicPort: Int? = null,
    val type: String = "tcp"
) {
    fun formatDisplay(): String {
        return if (publicPort != null) "$publicPort:$privatePort/$type" else "$privatePort/$type"
    }
}

data class ContainerMount(
    val type: String,
    val source: String,
    val destination: String,
    val mode: String = "rw",
    val rw: Boolean = true
)

data class ContainerItem(
    val id: String,
    val names: List<String>,
    val image: String,
    val imageId: String = "",
    val command: String = "",
    val created: Long = 0L,
    val state: ContainerState = ContainerState.Unknown,
    val statusText: String = "",
    val ports: List<ContainerPortMapping> = emptyList(),
    val mounts: List<ContainerMount> = emptyList(),
    val sizeRw: Long = 0L,
    val sizeRootFs: Long = 0L
) {
    val displayName: String
        get() = names.firstOrNull()?.removePrefix("/") ?: id.take(12)

    val shortId: String
        get() = id.take(12)

    val isRunning: Boolean
        get() = state == ContainerState.Running
}

data class ContainerStatsSnapshot(
    val containerId: String,
    val cpuUsagePercent: Float,
    val memoryUsageBytes: Long,
    val memoryLimitBytes: Long,
    val memoryUsagePercent: Float,
    val networkRxBytes: Long = 0L,
    val networkTxBytes: Long = 0L,
    val blockIoReadBytes: Long = 0L,
    val blockIoWriteBytes: Long = 0L
) {
    fun formattedMemoryUsed(): String = formatBytes(memoryUsageBytes)
    fun formattedMemoryLimit(): String = formatBytes(memoryLimitBytes)
}

data class DockerImageItem(
    val id: String,
    val repoTags: List<String>,
    val created: Long,
    val sizeBytes: Long
) {
    val shortId: String get() = id.removePrefix("sha256:").take(12)
    val primaryTag: String get() = repoTags.firstOrNull { it != "<none>:<none>" } ?: shortId
    fun formattedSize(): String = formatBytes(sizeBytes)
}

data class DockerSystemDf(
    val imagesTotalSize: Long,
    val imagesReclaimable: Long,
    val containersTotalSize: Long,
    val volumesTotalSize: Long,
    val buildCacheTotalSize: Long
) {
    fun formattedTotalSize(): String = formatBytes(imagesTotalSize + containersTotalSize + volumesTotalSize + buildCacheTotalSize)
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}
