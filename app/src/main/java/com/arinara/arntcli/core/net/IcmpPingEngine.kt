// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PingSample(
    val sequence: Int,
    val host: String,
    val ip: String,
    val bytes: Int,
    val timeMs: Double,
    val ttl: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class PingSummary(
    val transmitted: Int,
    val received: Int,
    val packetLossPercent: Double,
    val minTimeMs: Double,
    val avgTimeMs: Double,
    val maxTimeMs: Double,
    val jitterMs: Double
)

/**
 * Pure on-device ICMP Ping and round-trip latency engine.
 * Streams sequential latency probes with rolling statistics calculation.
 */
class IcmpPingEngine {

    companion object {
        fun resolvePingBinary(): String {
            val systemPing = File("/system/bin/ping")
            if (systemPing.exists() && systemPing.canExecute()) {
                return "/system/bin/ping"
            }
            return "ping"
        }
    }

    /**
     * Executes ping probes to the target host and streams real-time PingSample events.
     */
    fun startPing(targetHost: String, maxCount: Int = 10, intervalMs: Long = 1000L): Flow<PingSample> = flow {
        val cleanHost = targetHost.trim()
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true && !File("/system/bin/ping").exists()

        var sequence = 1
        var resolvedIp = cleanHost

        try {
            val address = withContext(Dispatchers.IO) { InetAddress.getByName(cleanHost) }
            resolvedIp = address.hostAddress ?: cleanHost
        } catch (_: Exception) {
            // If DNS lookup fails immediately, emit error sample and exit
            emit(
                PingSample(
                    sequence = 1,
                    host = cleanHost,
                    ip = cleanHost,
                    bytes = 0,
                    timeMs = 0.0,
                    ttl = 0,
                    isSuccess = false,
                    errorMessage = "Unable to resolve host '$cleanHost'"
                )
            )
            return@flow
        }

        // Try native process ping
        var nativePingSuccess = false
        try {
            val pingCmd = if (isWindows) {
                listOf("ping", "-n", maxCount.toString(), "-w", "1000", cleanHost)
            } else {
                listOf(resolvePingBinary(), "-c", maxCount.toString(), "-W", "1", cleanHost)
            }

            val process = ProcessBuilder(pingCmd).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))

            reader.use { r ->
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    val sample = parsePingLine(currentLine, sequence, cleanHost, resolvedIp)
                    if (sample != null) {
                        nativePingSuccess = true
                        emit(sample)
                        sequence++
                    }
                }
            }
            process.waitFor()
        } catch (_: Exception) {
            nativePingSuccess = false
        }

        // Fallback to socket/isReachable probes if native ping could not produce samples
        if (!nativePingSuccess) {
            while (currentCoroutineContext().isActive && sequence <= maxCount) {
                val sample = executeFallbackProbe(cleanHost, resolvedIp, sequence)
                emit(sample)
                sequence++
                if (sequence <= maxCount) {
                    delay(intervalMs)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    internal fun parsePingLine(line: String, defaultSeq: Int, host: String, fallbackIp: String): PingSample? {
        val lower = line.lowercase()
        if (!lower.contains("bytes") || (!lower.contains("time") && !lower.contains("ms"))) {
            return null
        }

        // Linux format: 64 bytes from 1.1.1.1: icmp_seq=1 ttl=57 time=14.2 ms
        // Windows format: Reply from 1.1.1.1: bytes=32 time=15ms TTL=57
        val timeRegex = Regex("""time[=<]\s*([0-9.]+)\s*ms?""", RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.find(line)
        val timeMs = timeMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

        val ttlRegex = Regex("""ttl[=:]\s*([0-9]+)""", RegexOption.IGNORE_CASE)
        val ttl = ttlRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 64

        val seqRegex = Regex("""icmp_seq[=:]\s*([0-9]+)""", RegexOption.IGNORE_CASE)
        val seq = seqRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: defaultSeq

        val bytesRegex = Regex("""(?:([0-9]+)\s*bytes|bytes[=:]\s*([0-9]+))""", RegexOption.IGNORE_CASE)
        val bytesMatch = bytesRegex.find(line)
        val bytes = bytesMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: bytesMatch?.groupValues?.get(2)?.toIntOrNull()
            ?: 64

        val parenthesizedIpRegex = Regex("""\(([0-9a-fA-F:.]+)\)""")
        val parenthesizedMatch = parenthesizedIpRegex.find(line)?.groupValues?.get(1)

        val ipRegex = Regex("""from\s+([^\s:]+)""", RegexOption.IGNORE_CASE)
        val directMatch = ipRegex.find(line)?.groupValues?.get(1)?.trimEnd(':')

        val ipFound = parenthesizedMatch ?: directMatch ?: fallbackIp

        return PingSample(
            sequence = seq,
            host = host,
            ip = ipFound,
            bytes = bytes,
            timeMs = timeMs,
            ttl = ttl,
            isSuccess = true
        )
    }

    private suspend fun executeFallbackProbe(host: String, ip: String, sequence: Int): PingSample = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        return@withContext try {
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(1000)
            val durationMs = (System.nanoTime() - startNano) / 1_000_000.0

            if (reachable) {
                PingSample(
                    sequence = sequence,
                    host = host,
                    ip = ip,
                    bytes = 32,
                    timeMs = String.format(java.util.Locale.US, "%.1f", durationMs).toDouble(),
                    ttl = 64,
                    isSuccess = true
                )
            } else {
                PingSample(
                    sequence = sequence,
                    host = host,
                    ip = ip,
                    bytes = 0,
                    timeMs = 0.0,
                    ttl = 0,
                    isSuccess = false,
                    errorMessage = "Request timed out"
                )
            }
        } catch (e: Exception) {
            PingSample(
                sequence = sequence,
                host = host,
                ip = ip,
                bytes = 0,
                timeMs = 0.0,
                ttl = 0,
                isSuccess = false,
                errorMessage = e.message ?: "Ping failed"
            )
        }
    }

    /**
     * Calculates rolling summary metrics (Min, Avg, Max, Jitter, Packet Loss).
     */
    fun calculateSummary(samples: List<PingSample>): PingSummary {
        if (samples.isEmpty()) {
            return PingSummary(
                transmitted = 0,
                received = 0,
                packetLossPercent = 0.0,
                minTimeMs = 0.0,
                avgTimeMs = 0.0,
                maxTimeMs = 0.0,
                jitterMs = 0.0
            )
        }

        val successful = samples.filter { it.isSuccess }
        val transmitted = samples.size
        val received = successful.size
        val packetLoss = ((transmitted - received).toDouble() / transmitted) * 100.0

        if (successful.isEmpty()) {
            return PingSummary(
                transmitted = transmitted,
                received = 0,
                packetLossPercent = 100.0,
                minTimeMs = 0.0,
                avgTimeMs = 0.0,
                maxTimeMs = 0.0,
                jitterMs = 0.0
            )
        }

        var minMs = Double.MAX_VALUE
        var maxMs = 0.0
        var totalMs = 0.0
        var jitterSum = 0.0

        for (i in successful.indices) {
            val t = successful[i].timeMs
            minMs = min(minMs, t)
            maxMs = max(maxMs, t)
            totalMs += t
            if (i > 0) {
                jitterSum += abs(t - successful[i - 1].timeMs)
            }
        }

        val avgMs = totalMs / successful.size
        val jitterMs = if (successful.size > 1) jitterSum / (successful.size - 1) else 0.0

        return PingSummary(
            transmitted = transmitted,
            received = received,
            packetLossPercent = packetLoss,
            minTimeMs = minMs,
            avgTimeMs = avgMs,
            maxTimeMs = maxMs,
            jitterMs = jitterMs
        )
    }
}
