// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket

data class SslCertificateInfo(
    val subjectDn: String,
    val subjectCn: String,
    val subjectOrg: String?,
    val issuerDn: String,
    val issuerCn: String,
    val validFrom: String,
    val validUntil: String,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val serialNumber: String,
    val sha256Fingerprint: String,
    val subjectAltNames: List<String>,
    val protocol: String,
    val cipherSuite: String,
    val chainLength: Int
)

data class SslInspectionResult(
    val host: String,
    val port: Int,
    val certificate: SslCertificateInfo,
    val handshakeDurationMs: Double
)

/**
 * Pure on-device SSL/TLS Certificate Inspector engine.
 * Connects via SSLSocket, extracts X.509 certificate chains, SANs, and measures validity windows.
 */
class SslInspectorEngine {

    suspend fun inspect(host: String, port: Int = 443, timeoutMs: Int = 5000): Result<SslInspectionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
            require(cleanHost.isNotBlank()) { "Target hostname or IP address cannot be blank" }

            val startNano = System.nanoTime()
            val socketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
            val socket = socketFactory.createSocket() as SSLSocket

            try {
                socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                socket.soTimeout = timeoutMs

                // Set Server Name Indication (SNI)
                val params = socket.sslParameters
                try {
                    params.serverNames = listOf(SNIHostName(cleanHost))
                    socket.sslParameters = params
                } catch (_: Exception) {
                    // Ignore if SNI not supported for raw IP address
                }

                socket.startHandshake()
                val handshakeDurationMs = (System.nanoTime() - startNano) / 1_000_000.0

                val session = socket.session
                val certs = session.peerCertificates
                require(certs.isNotEmpty()) { "No peer certificates presented by remote host" }

                val primaryCert = certs[0] as X509Certificate
                val info = parseCertificate(primaryCert, session.protocol, session.cipherSuite, certs.size)

                SslInspectionResult(
                    host = cleanHost,
                    port = port,
                    certificate = info,
                    handshakeDurationMs = String.format(Locale.US, "%.1f", handshakeDurationMs).toDouble()
                )
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    fun parseCertificate(
        cert: X509Certificate,
        protocol: String = "TLSv1.3",
        cipherSuite: String = "TLS_AES_256_GCM_SHA384",
        chainLength: Int = 1
    ): SslCertificateInfo {
        val subjectDn = cert.subjectX500Principal.name
        val issuerDn = cert.issuerX500Principal.name

        val subjectCn = extractCn(subjectDn)
        val subjectOrg = extractOrg(subjectDn)
        val issuerCn = extractCn(issuerDn)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val notBefore = cert.notBefore
        val notAfter = cert.notAfter

        val now = System.currentTimeMillis()
        val daysRemaining = (notAfter.time - now) / (1000 * 60 * 60 * 24)
        val isExpired = notAfter.time < now

        val serialHex = cert.serialNumber.toString(16).uppercase()
        val fingerprint = computeSha256Fingerprint(cert.encoded)

        val sanList = mutableListOf<String>()
        try {
            cert.subjectAlternativeNames?.forEach { entry ->
                val sanVal = entry.getOrNull(1)?.toString()
                if (!sanVal.isNullOrBlank()) {
                    sanList.add(sanVal)
                }
            }
        } catch (_: Exception) {}

        return SslCertificateInfo(
            subjectDn = subjectDn,
            subjectCn = subjectCn,
            subjectOrg = subjectOrg,
            issuerDn = issuerDn,
            issuerCn = issuerCn,
            validFrom = dateFormat.format(notBefore),
            validUntil = dateFormat.format(notAfter),
            daysRemaining = daysRemaining,
            isExpired = isExpired,
            serialNumber = serialHex,
            sha256Fingerprint = fingerprint,
            subjectAltNames = sanList,
            protocol = protocol,
            cipherSuite = cipherSuite,
            chainLength = chainLength
        )
    }

    private fun extractCn(dn: String): String {
        val regex = Regex("""CN=([^,]+)""", RegexOption.IGNORE_CASE)
        return regex.find(dn)?.groupValues?.get(1)?.trim() ?: dn
    }

    private fun extractOrg(dn: String): String? {
        val regex = Regex("""O=([^,]+)""", RegexOption.IGNORE_CASE)
        return regex.find(dn)?.groupValues?.get(1)?.trim()
    }

    private fun computeSha256Fingerprint(encodedBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(encodedBytes)
        return digest.joinToString(":") { String.format("%02X", it) }
    }
}
