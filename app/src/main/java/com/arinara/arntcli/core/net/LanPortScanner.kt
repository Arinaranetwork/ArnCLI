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

data class ScannedPort(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val latencyMs: Double?,
    val error: String? = null
)

data class PortScanResult(
    val targetHost: String,
    val resolvedIp: String,
    val scannedPorts: List<ScannedPort>,
    val openPortCount: Int,
    val durationMs: Double
)

/**
 * On-device TCP port scanner and service reachability engine.
 * Tests TCP 3-way handshakes concurrently using Kotlin Coroutines with socket timeouts.
 */
class LanPortScanner {

    companion object {
        val COMMON_PORTS = listOf(
            21 to "FTP",
            22 to "SSH",
            23 to "Telnet",
            25 to "SMTP",
            53 to "DNS",
            80 to "HTTP",
            110 to "POP3",
            143 to "IMAP",
            443 to "HTTPS",
            445 to "SMB",
            3306 to "MySQL",
            3389 to "RDP",
            5432 to "PostgreSQL",
            6379 to "Redis",
            8080 to "HTTP-Alt",
            8443 to "HTTPS-Alt"
        )

        fun resolveServiceName(port: Int): String {
            return COMMON_PORTS.firstOrNull { it.first == port }?.second ?: "Unknown"
        }
    }

    suspend fun scanPort(
        host: String,
        port: Int,
        timeoutMs: Int = 1000
    ): ScannedPort = withContext(Dispatchers.IO) {
        val serviceName = resolveServiceName(port)
        val startNano = System.nanoTime()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
                ScannedPort(
                    port = port,
                    serviceName = serviceName,
                    isOpen = true,
                    latencyMs = durationMs
                )
            }
        } catch (e: Exception) {
            val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
            ScannedPort(
                port = port,
                serviceName = serviceName,
                isOpen = false,
                latencyMs = durationMs,
                error = e.javaClass.simpleName
            )
        }
    }

    suspend fun scanPorts(
        targetHost: String,
        ports: List<Int> = COMMON_PORTS.map { it.first },
        timeoutMs: Int = 1000
    ): Result<PortScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            var cleanHost = targetHost.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')
            if (cleanHost.startsWith("[") && cleanHost.contains("]")) {
                cleanHost = cleanHost.substringAfter("[").substringBefore("]")
            } else if (cleanHost.count { it == ':' } == 1) {
                cleanHost = cleanHost.substringBefore(':')
            }
            require(cleanHost.isNotBlank()) { "Target host or IP address cannot be blank" }
            require(ports.isNotEmpty()) { "Port list cannot be empty" }

            val startNano = System.nanoTime()
            val resolvedAddress = InetAddress.getByName(cleanHost)
            val resolvedIp = resolvedAddress.hostAddress ?: cleanHost

            val results = mutableListOf<PortStatus>()
            // Chunk concurrency to 30 simultaneous sockets to prevent EMFILE socket exhaustion
            for (chunk in ports.chunked(30)) {
                val chunkResults = chunk.map { port ->
                    async { scanPort(cleanHost, port, timeoutMs) }
                }.awaitAll()
                results.addAll(chunkResults)
            }
            val sortedResults = results.sortedBy { it.port }
            val openCount = sortedResults.count { it.isOpen }
            val durationMs = (System.nanoTime() - startNano) / 1_000_000.0

            PortScanResult(
                targetHost = cleanHost,
                resolvedIp = resolvedIp,
                scannedPorts = sortedResults,
                openPortCount = openCount,
                durationMs = durationMs
            )
        }
    }
}
