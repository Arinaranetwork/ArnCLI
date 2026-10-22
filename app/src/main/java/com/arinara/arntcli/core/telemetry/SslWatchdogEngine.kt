// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class SslHealthLevel {
    Healthy,   // > 30 days
    Warning,   // 14 - 30 days
    Critical,  // 1 - 13 days
    Expired,   // <= 0 days
    Failed
}

data class SslCertStatus(
    val host: String,
    val port: Int,
    val commonName: String = "",
    val issuer: String = "",
    val subjectAltNames: List<String> = emptyList(),
    val validFrom: String = "",
    val validUntil: String = "",
    val daysRemaining: Long = 0L,
    val sanMatches: Boolean = true,
    val healthLevel: SslHealthLevel = SslHealthLevel.Healthy,
    val errorMessage: String? = null
)

class SslWatchdogEngine {

    companion object {
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        suspend fun inspectCertificate(
            host: String,
            port: Int = 443,
            timeoutMs: Int = 5000
        ): SslCertStatus = withContext(Dispatchers.IO) {
            val cleanHost = host.trim().removePrefix("https://").substringBefore("/").substringBefore(":")
            var sslSocket: SSLSocket? = null

            try {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                sslSocket = factory.createSocket() as SSLSocket
                sslSocket.soTimeout = timeoutMs
                sslSocket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                sslSocket.startHandshake()

                val certs = sslSocket.session.peerCertificates
                val cert = certs.firstOrNull() as? X509Certificate
                    ?: throw IllegalStateException("No X.509 certificate found on peer session")

                val notAfter = cert.notAfter
                val notBefore = cert.notBefore
                val now = System.currentTimeMillis()

                val daysRemaining = ((notAfter.time - now) / (1000L * 60 * 60 * 24))

                val sanList = mutableListOf<String>()
                try {
                    val altNames = cert.subjectAlternativeNames
                    if (altNames != null) {
                        for (item in altNames) {
                            if (item.size >= 2 && item[1] is String) {
                                sanList.add(item[1] as String)
                            }
                        }
                    }
                } catch (_: Exception) {}

                val subject = cert.subjectX500Principal.name
                val cn = subject.split(",").firstOrNull { it.trim().startsWith("CN=") }?.substringAfter("CN=")?.trim() ?: cleanHost

                val sanMatches = verifyHostnameMatch(cleanHost, cn, sanList)

                val health = when {
                    daysRemaining <= 0 -> SslHealthLevel.Expired
                    daysRemaining <= 7 -> SslHealthLevel.Critical
                    daysRemaining <= 30 -> SslHealthLevel.Warning
                    else -> SslHealthLevel.Healthy
                }

                SslCertStatus(
                    host = cleanHost,
                    port = port,
                    commonName = cn,
                    issuer = cert.issuerX500Principal.name.split(",").firstOrNull { it.trim().startsWith("O=") }?.substringAfter("O=")?.trim() ?: cert.issuerX500Principal.name,
                    subjectAltNames = sanList,
                    validFrom = ISO_DATE_FORMAT.format(notBefore),
                    validUntil = ISO_DATE_FORMAT.format(notAfter),
                    daysRemaining = daysRemaining,
                    sanMatches = sanMatches,
                    healthLevel = health,
                    errorMessage = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SslCertStatus(
                    host = cleanHost,
                    port = port,
                    daysRemaining = 0L,
                    sanMatches = false,
                    healthLevel = SslHealthLevel.Failed,
                    errorMessage = e.message ?: e.javaClass.simpleName
                )
            } finally {
                try { sslSocket?.close() } catch (_: Exception) {}
            }
        }

        private fun verifyHostnameMatch(targetHost: String, cn: String, sans: List<String>): Boolean {
            val namesToCheck = (sans + cn).map { it.lowercase() }
            val hostLower = targetHost.lowercase()

            for (pattern in namesToCheck) {
                if (pattern == hostLower) return true
                if (pattern.startsWith("*.")) {
                    val suffix = pattern.substring(2)
                    if (hostLower.endsWith(suffix) && hostLower.count { it == '.' } == pattern.count { it == '.' }) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
