// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

data class DnsRecord(
    val type: String,
    val host: String,
    val value: String,
    val isLoopback: Boolean = false,
    val isPrivate: Boolean = false
)

data class DnsLookupResult(
    val queryHost: String,
    val durationMs: Double,
    val records: List<DnsRecord>,
    val canonicalHostName: String
)

/**
 * Pure on-device DNS resolution engine.
 * Resolves A, AAAA, and reverse PTR records using local system resolvers.
 */
class DnsResolverEngine {

    suspend fun resolve(queryHost: String): Result<DnsLookupResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanHost = queryHost.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
            require(cleanHost.isNotBlank()) { "Target hostname or IP address cannot be blank" }

            val startNano = System.nanoTime()
            val addresses = InetAddress.getAllByName(cleanHost)
            val durationMs = (System.nanoTime() - startNano) / 1_000_000.0

            val records = addresses.map { addr ->
                val type = when (addr) {
                    is Inet4Address -> "A"
                    is Inet6Address -> "AAAA"
                    else -> "ADDR"
                }

                val ipStr = addr.hostAddress ?: ""
                val isPrivate = addr.isSiteLocalAddress || isPrivateIpv4(ipStr)

                DnsRecord(
                    type = type,
                    host = cleanHost,
                    value = ipStr,
                    isLoopback = addr.isLoopbackAddress,
                    isPrivate = isPrivate
                )
            }

            val canonicalName = addresses.firstOrNull()?.canonicalHostName ?: cleanHost

            DnsLookupResult(
                queryHost = cleanHost,
                durationMs = String.format(java.util.Locale.US, "%.1f", durationMs).toDouble(),
                records = records,
                canonicalHostName = canonicalName
            )
        }
    }

    suspend fun reverseLookup(ipAddress: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanIp = ipAddress.trim()
            val addr = InetAddress.getByName(cleanIp)
            addr.canonicalHostName
        }
    }

    private fun isPrivateIpv4(ip: String): Boolean {
        return ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                (ip.startsWith("172.") && ip.split('.').getOrNull(1)?.toIntOrNull() in 16..31)
    }
}
