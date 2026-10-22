// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.vault

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class VaultEngineTest {

    private val engine = VaultEngine()
    private val passphrase = "super-secret-master-passphrase-2026"

    @Test
    fun encryptAndDecrypt_roundTripSucceeds() = runBlocking {
        val originalSecret = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtz\n-----END OPENSSH PRIVATE KEY-----"

        val encrypted = engine.encrypt(originalSecret, passphrase)

        assertNotEquals(originalSecret, encrypted)
        assertTrue(encrypted.isNotBlank())

        val decrypted = engine.decrypt(encrypted, passphrase)
        assertEquals(originalSecret, decrypted)
    }

    @Test(expected = Exception::class)
    fun decrypt_wrongPassphrase_failsAuthentication() = runBlocking {
        val encrypted = engine.encrypt("my-db-password-1234", passphrase)
        engine.decrypt(encrypted, "wrong-passphrase")
        Unit
    }

    @Test
    fun maskSecret_masksCorrectly() {
        assertEquals("••••••••", VaultEngine.maskSecret("password"))
        assertEquals("", VaultEngine.maskSecret(""))
        assertEquals("••••••••••••••••", VaultEngine.maskSecret("very-long-secret-key-that-exceeds-sixteen-chars"))
    }
}
