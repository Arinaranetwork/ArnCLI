// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class UptimeWatchdogEngine {

    private val _services = MutableStateFlow<List<WatchdogService>>(emptyList())
    val services: StateFlow<List<WatchdogService>> = _services.asStateFlow()

    companion object {
        const val MAX_LATENCY_HISTORY = 30
    }

    /**
     * Registers a new target to monitor.
     */
    fun addTarget(target: WatchdogProbeTarget) {
        _services.update { current ->
            if (current.any { it.target.id == target.id }) current
            else current + WatchdogService(target = target)
        }
    }

    /**
     * Removes a target.
     */
    fun removeTarget(targetId: String) {
        _services.update { current -> current.filterNot { it.target.id == targetId } }
    }

    /**
     * Executes a single probe against a target and updates service status with flap damping.
     */
    suspend fun executeProbe(target: WatchdogProbeTarget): WatchdogProbeResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val result = try {
            when (target.type) {
                WatchdogProbeType.Http -> probeHttp(target)
                WatchdogProbeType.TcpPort -> probeTcp(target)
                WatchdogProbeType.IcmpPing -> probeIcmp(target)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            WatchdogProbeResult(
                probeId = target.id,
                isUp = false,
                latencyMs = elapsedMs,
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        }

        applyProbeResult(target.id, result)
        result
    }

    private fun applyProbeResult(targetId: String, result: WatchdogProbeResult) {
        _services.update { currentList ->
            currentList.map { service ->
                if (service.target.id != targetId) return@map service

                val newTotal = service.totalChecks + 1
                val newSuccess = if (result.isUp) service.successfulChecks + 1 else service.successfulChecks
                val newFailures = if (result.isUp) 0 else service.consecutiveFailures + 1

                val newStatus = when {
                    result.isUp -> ProbeStatus.Up
                    newFailures >= service.target.failureThreshold -> ProbeStatus.Down
                    else -> ProbeStatus.Degraded // Transient failure within threshold
                }

                val newLatencies = (service.recentLatencies + result.latencyMs).takeLast(MAX_LATENCY_HISTORY)

                service.copy(
                    status = newStatus,
                    consecutiveFailures = newFailures,
                    totalChecks = newTotal,
                    successfulChecks = newSuccess,
                    recentLatencies = newLatencies,
                    lastCheckedTimestamp = result.timestamp,
                    lastErrorMessage = result.errorMessage
                )
            }
        }
    }

    private fun probeHttp(target: WatchdogProbeTarget): WatchdogProbeResult {
        val start = System.nanoTime()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(if (target.endpoint.startsWith("http://") || target.endpoint.startsWith("https://")) target.endpoint else "http://${target.endpoint}")
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = target.timeoutMs
            conn.readTimeout = target.timeoutMs
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ArnTCLI-Watchdog/0.8.0")

            val statusCode = conn.responseCode
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            val isStatusValid = if (target.expectedStatusCode > 0) {
                statusCode == target.expectedStatusCode
            } else {
                statusCode in 200..399
            }

            var regexValid = true
            if (isStatusValid && !target.regexMatch.isNullOrBlank()) {
                val stream = if (statusCode in 200..399) conn.inputStream else conn.errorStream
                val bodySnippet = stream?.use {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).lineSequence().take(200).joinToString("\n")
                } ?: ""
                regexValid = Regex(target.regexMatch).containsMatchIn(bodySnippet)
            }

            val isUp = isStatusValid && regexValid
            val errMsg = if (!isUp) {
                if (!isStatusValid) "HTTP status $statusCode (expected ${target.expectedStatusCode})"
                else "Body regex match failed"
            } else null

            return WatchdogProbeResult(
                probeId = target.id,
                isUp = isUp,
                latencyMs = elapsedMs,
                statusCode = statusCode,
                errorMessage = errMsg
            )
        } finally {
            conn?.disconnect()
        }
    }

    private fun probeTcp(target: WatchdogProbeTarget): WatchdogProbeResult {
        val start = System.nanoTime()
        val socket = Socket()
        try {
            val endpoint = target.endpoint.substringBefore(":")
            val port = if (target.endpoint.contains(":")) {
                target.endpoint.substringAfter(":").toIntOrNull() ?: target.port
            } else target.port

            socket.connect(InetSocketAddress(endpoint, port), target.timeoutMs)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            return WatchdogProbeResult(
                probeId = target.id,
                isUp = true,
                latencyMs = elapsedMs
            )
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            return WatchdogProbeResult(
                probeId = target.id,
                isUp = false,
                latencyMs = elapsedMs,
                errorMessage = e.message ?: "Connection Refused / Timeout"
            )
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun probeIcmp(target: WatchdogProbeTarget): WatchdogProbeResult {
        val start = System.nanoTime()
        val host = target.endpoint.trim()
        val reachable = InetAddress.getByName(host).isReachable(target.timeoutMs)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        return WatchdogProbeResult(
            probeId = target.id,
            isUp = reachable,
            latencyMs = elapsedMs,
            errorMessage = if (!reachable) "ICMP Host Unreachable" else null
        )
    }
}
