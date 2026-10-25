// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.vault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * On-device zero-knowledge cryptographic vault engine.
 * Employs authenticated AES-256-GCM encryption with PBKDF2 key derivation.
 */
class VaultEngine {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128

        private val secureRandom = SecureRandom()

        fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
            try {
                val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
                val keyBytes = factory.generateSecret(spec).encoded
                return SecretKeySpec(keyBytes, KEY_ALGORITHM)
            } finally {
                spec.clearPassword()
            }
        }

        fun maskSecret(secret: String): String {
            if (secret.isEmpty()) return ""
            return "•".repeat(secret.length.coerceAtMost(16))
        }
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns Base64-encoded payload: salt [16 bytes] + iv [12 bytes] + ciphertext & tag.
     */
    fun encryptBlocking(plaintext: String, passphrase: String): String {
        require(passphrase.isNotBlank()) { "Vault passphrase cannot be blank" }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val buffer = ByteBuffer.allocate(salt.size + iv.size + ciphertext.size)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)

        return Base64.getEncoder().encodeToString(buffer.array())
    }

    suspend fun encrypt(plaintext: String, passphrase: String): String = withContext(Dispatchers.IO) {
        encryptBlocking(plaintext, passphrase)
    }

    /**
     * Decrypts Base64-encoded AES-256-GCM payload.
     */
    fun decryptBlocking(encryptedBase64: String, passphrase: String): String {
        require(passphrase.isNotBlank()) { "Vault passphrase cannot be blank" }

        val payload = Base64.getDecoder().decode(encryptedBase64)
        require(payload.size > SALT_LENGTH_BYTES + IV_LENGTH_BYTES) { "Invalid vault payload length" }

        val buffer = ByteBuffer.wrap(payload)
        val salt = ByteArray(SALT_LENGTH_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    suspend fun decrypt(encryptedBase64: String, passphrase: String): String = withContext(Dispatchers.IO) {
        decryptBlocking(encryptedBase64, passphrase)
    }
}
