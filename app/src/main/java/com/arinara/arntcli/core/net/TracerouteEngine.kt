// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.regex.Pattern

data class RouteHop(
    val hopNumber: Int,
    val ipAddress: String?,
    val hostname: String?,
    val rttMs: Double?,
    val isDestination: Boolean,
    val isTimedOut: Boolean
)

data class TracerouteResult(
    val targetHost: String,
    val resolvedIp: String,
    val hops: List<RouteHop>,
    val durationMs: Double,
    val isReached: Boolean
)

/**
 * On-device rootless network traceroute and hop-by-hop latency inspector.
 * Employs time-to-live (TTL) stepping via system execution to map routing topology.
 */
class TracerouteEngine(
    private val pingExecutable: String = resolvePingExecutable()
) {

    companion object {
        const val DEFAULT_MAX_HOPS = 20
        const val DEFAULT_TIMEOUT_SECS = 2

        fun resolvePingExecutable(): String {
            val systemPing = File("/system/bin/ping")
            if (systemPing.exists() && systemPing.canExecute()) {
                return "/system/bin/ping"
            }
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
            return if (isWindows) "ping.exe" else "ping"
        }

        private val IP_PATTERN = Pattern.compile("(?:from|From|reply from|Reply from)\\s+([0-9a-fA-F:.]+)")
        private val TIME_PATTERN = Pattern.compile("time[=<]?\\s*([0-9.]+)\\s*ms")

        fun parseHopOutput(hopNumber: Int, outputLines: List<String>, targetIp: String): RouteHop {
            val fullText = outputLines.joinToString("\n")

            var extractedIp: String? = null
            var rttMs: Double? = null

            for (line in outputLines) {
                val ipMatcher = IP_PATTERN.matcher(line)
                if (ipMatcher.find()) {
                    val rawIp = ipMatcher.group(1)?.trimEnd(':', ',')
                    if (rawIp != null && rawIp.isNotBlank()) {
                        extractedIp = rawIp
                    }
                }

                val timeMatcher = TIME_PATTERN.matcher(line)
                if (timeMatcher.find()) {
                    rttMs = timeMatcher.group(1)?.toDoubleOrNull()
                }
            }

            val isDestination = (extractedIp != null && (extractedIp == targetIp || fullText.contains("64 bytes from $targetIp")))
            val isTimedOut = extractedIp == null && (fullText.contains("timed out") || fullText.contains("100% packet loss") || fullText.contains("100% loss") || fullText.isEmpty())

            val hostname = if (extractedIp != null) {
                try {
                    val inet = InetAddress.getByName(extractedIp)
                    val canon = inet.canonicalHostName
                    if (canon != extractedIp) canon else extractedIp
                } catch (_: Exception) {
                    extractedIp
                }
            } else null

            return RouteHop(
                hopNumber = hopNumber,
                ipAddress = extractedIp,
                hostname = hostname,
                rttMs = rttMs ?: if (!isTimedOut) 1.0 else null,
                isDestination = isDestination,
                isTimedOut = isTimedOut
            )
        }
    }

    private fun buildHopCommand(target: String, ttl: Int): List<String> {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true && !File("/system/bin/ping").exists()
        return if (isWindows) {
            listOf(pingExecutable, "-n", "1", "-i", ttl.toString(), "-w", (DEFAULT_TIMEOUT_SECS * 1000).toString(), target)
        } else {
            listOf(pingExecutable, "-c", "1", "-t", ttl.toString(), "-W", DEFAULT_TIMEOUT_SECS.toString(), target)
        }
    }

    suspend fun executeHop(targetHost: String, targetIp: String, hopNumber: Int): RouteHop = withContext(Dispatchers.IO) {
        val cmd = buildHopCommand(targetHost, hopNumber)
        val lines = mutableListOf<String>()

        try {
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { lines.add(it) }
                }
            }
            process.waitFor()
        } catch (_: Exception) {
            // Handled as timeout
        }

        parseHopOutput(hopNumber, lines, targetIp)
    }

    fun traceStreaming(
        targetHost: String,
        maxHops: Int = DEFAULT_MAX_HOPS
    ): Flow<RouteHop> = flow {
        var cleanHost = targetHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
        if (cleanHost.startsWith("[") && cleanHost.contains("]")) {
            cleanHost = cleanHost.substringAfter("[").substringBefore("]")
        } else if (cleanHost.count { it == ':' } == 1) {
            cleanHost = cleanHost.substringBefore(':')
        }
        require(cleanHost.isNotBlank()) { "Target host cannot be blank" }

        val resolvedIp = try {
            InetAddress.getByName(cleanHost).hostAddress ?: cleanHost
        } catch (_: Exception) {
            cleanHost
        }

        for (ttl in 1..maxHops) {
            val hop = executeHop(cleanHost, resolvedIp, ttl)
            emit(hop)
            if (hop.isDestination) {
                break
            }
        }
    }.flowOn(Dispatchers.IO)
}
