// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import java.util.Locale

data class CpuCoreTick(
    val user: Long,
    val nice: Long,
    val system: Long,
    val idle: Long,
    val iowait: Long,
    val irq: Long,
    val softirq: Long,
    val steal: Long
) {
    val totalTime: Long get() = user + nice + system + idle + iowait + irq + softirq + steal
    val idleTime: Long get() = idle + iowait
}

data class CpuMetrics(
    val usagePercent: Float,
    val vCpuCount: Int,
    val perCoreUsagePercent: List<Float> = emptyList(),
    val overallTicks: CpuCoreTick? = null,
    val perCoreTicks: List<CpuCoreTick> = emptyList()
)

data class MemoryMetrics(
    val totalBytes: Long,
    val availableBytes: Long,
    val freeBytes: Long,
    val buffersBytes: Long,
    val cachedBytes: Long,
    val swapTotalBytes: Long,
    val swapFreeBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usagePercent: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

    val swapUsedBytes: Long get() = (swapTotalBytes - swapFreeBytes).coerceAtLeast(0L)
    val swapUsagePercent: Float get() = if (swapTotalBytes > 0) (swapUsedBytes.toFloat() / swapTotalBytes.toFloat()) * 100f else 0f

    fun formattedTotal(): String = formatBytes(totalBytes)
    fun formattedUsed(): String = formatBytes(usedBytes)
    fun formattedAvailable(): String = formatBytes(availableBytes)
    fun formattedSwapUsed(): String = formatBytes(swapUsedBytes)
    fun formattedSwapTotal(): String = formatBytes(swapTotalBytes)
}

data class LoadAvgMetrics(
    val load1m: Float,
    val load5m: Float,
    val load15m: Float,
    val vCpuCount: Int
) {
    val saturationRatio: Float get() = if (vCpuCount > 0) load1m / vCpuCount else load1m
    val isOverloaded: Boolean get() = saturationRatio >= 1.0f

    fun statusDescription(): String = when {
        saturationRatio < 0.70f -> "Healthy"
        saturationRatio < 1.00f -> "High Load"
        else -> "CPU Queuing Alert"
    }
}

data class DiskMountMetrics(
    val filesystem: String,
    val mountPoint: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usagePercent: Int
) {
    fun formattedTotal(): String = formatBytes(totalBytes)
    fun formattedUsed(): String = formatBytes(usedBytes)
    fun formattedAvailable(): String = formatBytes(availableBytes)
}

data class NetDevMetrics(
    val interfaceName: String,
    val rxBytes: Long,
    val txBytes: Long,
    val rxSpeedBytesPerSec: Long = 0L,
    val txSpeedBytesPerSec: Long = 0L,
    val rxErrors: Long = 0L,
    val txErrors: Long = 0L
) {
    fun formattedRxSpeed(): String = "${formatBytes(rxSpeedBytesPerSec)}/s"
    fun formattedTxSpeed(): String = "${formatBytes(txSpeedBytesPerSec)}/s"
}

data class ServerTelemetrySnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val hostId: String = "local",
    val cpu: CpuMetrics,
    val memory: MemoryMetrics,
    val load: LoadAvgMetrics,
    val disks: List<DiskMountMetrics>,
    val network: List<NetDevMetrics>
)

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}
