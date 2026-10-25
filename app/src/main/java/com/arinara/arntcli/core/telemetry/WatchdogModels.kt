// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import java.util.UUID

enum class WatchdogProbeType {
    Http,
    TcpPort,
    IcmpPing
}

enum class ProbeStatus {
    Up,
    Down,
    Degraded,
    Checking,
    Unknown
}

data class WatchdogProbeTarget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: WatchdogProbeType,
    val endpoint: String,
    val port: Int = 80,
    val intervalSeconds: Int = 10,
    val timeoutMs: Int = 3000,
    val expectedStatusCode: Int = 200,
    val regexMatch: String? = null,
    val failureThreshold: Int = 2
)

data class WatchdogProbeResult(
    val probeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUp: Boolean,
    val latencyMs: Long,
    val statusCode: Int? = null,
    val errorMessage: String? = null
)

data class WatchdogService(
    val target: WatchdogProbeTarget,
    val status: ProbeStatus = ProbeStatus.Unknown,
    val consecutiveFailures: Int = 0,
    val totalChecks: Long = 0L,
    val successfulChecks: Long = 0L,
    val recentLatencies: List<Long> = emptyList(),
    val lastCheckedTimestamp: Long = 0L,
    val lastErrorMessage: String? = null
) {
    val uptimePercent: Float
        get() = if (totalChecks > 0) (successfulChecks.toFloat() / totalChecks.toFloat()) * 100f else 100f

    val averageLatencyMs: Long
        get() = if (recentLatencies.isNotEmpty()) recentLatencies.average().toLong() else 0L
}
