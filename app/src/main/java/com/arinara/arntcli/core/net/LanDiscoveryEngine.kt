// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class DiscoveredHost(
    val ipAddress: String,
    val hostname: String,
    val latencyMs: Double,
    val isReachable: Boolean
)

data class LanDiscoveryResult(
    val subnetPrefix: String,
    val scannedCount: Int,
    val discoveredHosts: List<DiscoveredHost>,
    val durationMs: Double
)

/**
 * On-device local area network (LAN) host discovery engine.
 * Sweeps IPv4 subnet ranges using bounded asynchronous socket probes and reverse DNS mapping.
 */
class LanDiscoveryEngine {

    companion object {
        val PROBE_PORTS = intArrayOf(80, 22, 443, 8080, 53)

        fun deriveSubnetPrefix(ipAddress: String): String {
            val parts = ipAddress.trim().split('.')
            if (parts.size == 4) {
                return "${parts[0]}.${parts[1]}.${parts[2]}."
            }
            return "192.168.1."
        }
    }

    suspend fun probeHost(
        ipAddress: String,
        timeoutMs: Int = 200
    ): DiscoveredHost? = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()

        // 1. Try ICMP / standard isReachable check
        try {
            val inetAddr = InetAddress.getByName(ipAddress)
            if (inetAddr.isReachable(timeoutMs)) {
                val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
                val hostname = try {
                    val canonical = inetAddr.canonicalHostName
                    if (canonical != ipAddress) canonical else ipAddress
                } catch (_: Exception) {
                    ipAddress
                }
                return@withContext DiscoveredHost(
                    ipAddress = ipAddress,
                    hostname = hostname,
                    latencyMs = durationMs,
                    isReachable = true
                )
            }
        } catch (_: Exception) {
            // Fall through to TCP socket probe
        }

        // 2. Try TCP quick handshake on common management ports
        for (port in PROBE_PORTS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
                    val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
                    val hostname = try {
                        val inetAddr = InetAddress.getByName(ipAddress)
                        val canonical = inetAddr.canonicalHostName
                        if (canonical != ipAddress) canonical else ipAddress
                    } catch (_: Exception) {
                        ipAddress
                    }
                    return@withContext DiscoveredHost(
                        ipAddress = ipAddress,
                        hostname = hostname,
                        latencyMs = durationMs,
                        isReachable = true
                    )
                }
            } catch (_: Exception) {
                // Next port
            }
        }

        null
    }

    suspend fun discover(
        subnetPrefix: String = "192.168.1.",
        hostRange: IntRange = 1..254,
        timeoutMs: Int = 200,
        chunkSize: Int = 32
    ): LanDiscoveryResult = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        val cleanPrefix = if (subnetPrefix.endsWith(".")) subnetPrefix else "$subnetPrefix."
        val discoveredList = mutableListOf<DiscoveredHost>()
        val allHosts = hostRange.toList()

        // Chunked concurrency to prevent socket descriptor exhaustion
        for (chunk in allHosts.chunked(chunkSize)) {
            val chunkDeferreds = chunk.map { hostNum ->
                async {
                    val ip = "$cleanPrefix$hostNum"
                    probeHost(ip, timeoutMs)
                }
            }
            val chunkResults = chunkDeferreds.awaitAll().filterNotNull()
            discoveredList.addAll(chunkResults)
        }

        val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
        LanDiscoveryResult(
            subnetPrefix = cleanPrefix,
            scannedCount = allHosts.size,
            discoveredHosts = discoveredList.sortedBy { it.ipAddress },
            durationMs = durationMs
        )
    }
}
