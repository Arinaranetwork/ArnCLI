// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

class SslInspectorEngineTest {

    private val engine = SslInspectorEngine()

    @Test
    fun parseCertificate_validCert_extractsPropertiesCorrectly() {
        val tenDaysAgo = Date(System.currentTimeMillis() - 86400000L * 10)
        val eightyDaysLater = Date(System.currentTimeMillis() + 86400000L * 80)

        val mockCert = createMockX509Certificate(
            subject = "CN=arinara.network, O=Arinara Network",
            issuer = "CN=Let's Encrypt R3, O=Let's Encrypt",
            notBefore = tenDaysAgo,
            notAfter = eightyDaysLater,
            serial = BigInteger("A1B2C3D4E5", 16),
            sans = listOf(listOf(2, "arinara.network"), listOf(2, "*.arinara.network"))
        )

        val info = engine.parseCertificate(
            cert = mockCert,
            protocol = "TLSv1.3",
            cipherSuite = "TLS_AES_256_GCM_SHA384",
            chainLength = 2
        )

        assertEquals("arinara.network", info.subjectCn)
        assertEquals("Arinara Network", info.subjectOrg)
        assertEquals("Let's Encrypt R3", info.issuerCn)
        assertFalse(info.isExpired)
        assertTrue(info.daysRemaining in 78..81)
        assertEquals("A1B2C3D4E5", info.serialNumber)
        assertEquals("TLSv1.3", info.protocol)
        assertEquals("TLS_AES_256_GCM_SHA384", info.cipherSuite)
        assertEquals(2, info.chainLength)
        assertEquals(2, info.subjectAltNames.size)
        assertTrue(info.subjectAltNames.contains("arinara.network"))
        assertTrue(info.subjectAltNames.contains("*.arinara.network"))
        assertNotNull(info.sha256Fingerprint)
        assertTrue(info.sha256Fingerprint.contains(":"))
    }

    @Test
    fun parseCertificate_expiredCert_flagsExpiredState() {
        val oneHundredDaysAgo = Date(System.currentTimeMillis() - 86400000L * 100)
        val tenDaysAgo = Date(System.currentTimeMillis() - 86400000L * 10)

        val mockCert = createMockX509Certificate(
            subject = "CN=expired.example.com",
            issuer = "CN=Example Root CA",
            notBefore = oneHundredDaysAgo,
            notAfter = tenDaysAgo,
            serial = BigInteger("9999", 16),
            sans = emptyList()
        )

        val info = engine.parseCertificate(mockCert)

        assertEquals("expired.example.com", info.subjectCn)
        assertTrue(info.isExpired)
        assertTrue(info.daysRemaining < 0)
    }

    @Test
    fun inspect_blankHost_returnsFailure() = runBlocking {
        val result = engine.inspect("   ")
        assertTrue(result.isFailure)
    }

    private fun createMockX509Certificate(
        subject: String,
        issuer: String,
        notBefore: Date,
        notAfter: Date,
        serial: BigInteger,
        sans: List<List<Any>>
    ): X509Certificate {
        return object : X509Certificate() {
            override fun checkValidity() {}
            override fun checkValidity(date: Date?) {}
            override fun getVersion(): Int = 3
            override fun getSerialNumber(): BigInteger = serial
            override fun getIssuerDN(): Principal = X500Principal(issuer)
            override fun getIssuerX500Principal(): X500Principal = X500Principal(issuer)
            override fun getSubjectDN(): Principal = X500Principal(subject)
            override fun getSubjectX500Principal(): X500Principal = X500Principal(subject)
            override fun getNotBefore(): Date = notBefore
            override fun getNotAfter(): Date = notAfter
            override fun getTBSCertificate(): ByteArray = byteArrayOf(1, 2, 3)
            override fun getSignature(): ByteArray = byteArrayOf(4, 5, 6)
            override fun getSigAlgName(): String = "SHA256withRSA"
            override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
            override fun getSigAlgParams(): ByteArray? = null
            override fun getIssuerUniqueID(): BooleanArray? = null
            override fun getSubjectUniqueID(): BooleanArray? = null
            override fun getKeyUsage(): BooleanArray? = null
            override fun getBasicConstraints(): Int = -1
            override fun getEncoded(): ByteArray = byteArrayOf(10, 20, 30, 40, 50)
            override fun verify(key: PublicKey?) {}
            override fun verify(key: PublicKey?, sigProvider: String?) {}
            override fun toString(): String = subject
            override fun getPublicKey(): PublicKey? = null
            override fun getCriticalExtensionOIDs(): Set<String>? = null
            override fun getNonCriticalExtensionOIDs(): Set<String>? = null
            override fun getExtensionValue(oid: String?): ByteArray? = null
            override fun hasUnsupportedCriticalExtension(): Boolean = false
            override fun getSubjectAlternativeNames(): MutableCollection<MutableList<*>> {
                return sans.map { it.toMutableList() }.toMutableList()
            }
        }
    }
}
