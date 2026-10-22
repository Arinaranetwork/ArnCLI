// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.vault

import java.util.Base64
import java.util.zip.CRC32

/**
 * Air-gapped multipart optical data synchronization engine (BC-UR format).
 * Partitions encrypted payloads into verifiable QR fountain frames without network emissions.
 */
class BcUrSyncEngine(
    private val chunkSizeBytes: Int = 120
) {

    companion object {
        const val SCHEME = "ur:bytes"

        fun computeCrc32Hex(data: ByteArray): String {
            val crc = CRC32()
            crc.update(data)
            return java.lang.Long.toHexString(crc.value).padStart(8, '0')
        }
    }

    /**
     * Encodes a payload string into an ordered sequence of multipart BC-UR frame strings.
     */
    fun encode(payload: String): List<String> {
        require(payload.isNotBlank()) { "Payload cannot be blank" }

        val bytes = payload.toByteArray(Charsets.UTF_8)
        val checksumHex = computeCrc32Hex(bytes)

        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + chunkSizeBytes, bytes.size)
            chunks.add(bytes.copyOfRange(offset, end))
            offset = end
        }

        val total = chunks.size
        return chunks.mapIndexed { idx, chunkBytes ->
            val seq = idx + 1
            val chunkBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(chunkBytes)
            "$SCHEME/$seq-$total/$checksumHex/$chunkBase64"
        }
    }

    /**
     * Parser & accumulator for incoming optical QR stream frames.
     */
    class Receiver {
        private var expectedTotal: Int? = null
        private var expectedCrcHex: String? = null
        private val collectedChunks = mutableMapOf<Int, ByteArray>()

        val progress: Float
            get() = if (expectedTotal != null && expectedTotal!! > 0) {
                collectedChunks.size.toFloat() / expectedTotal!!.toFloat()
            } else 0f

        val isComplete: Boolean
            get() = expectedTotal != null && collectedChunks.size == expectedTotal

        /**
         * Ingests a scanned single frame. Returns true if valid frame accepted.
         */
        fun ingestFrame(frameString: String): Boolean {
            val clean = frameString.trim()
            if (!clean.startsWith("$SCHEME/")) return false

            val parts = clean.removePrefix("$SCHEME/").split('/')
            if (parts.size != 3) return false

            val seqParts = parts[0].split('-')
            if (seqParts.size != 2) return false

            val seq = seqParts[0].toIntOrNull() ?: return false
            val total = seqParts[1].toIntOrNull() ?: return false
            val crcHex = parts[1]
            val chunkBase64 = parts[2]

            val chunkBytes = try {
                Base64.getUrlDecoder().decode(chunkBase64)
            } catch (_: Exception) {
                return false
            }

            if (expectedTotal == null) {
                expectedTotal = total
                expectedCrcHex = crcHex
            } else if (expectedTotal != total || expectedCrcHex != crcHex) {
                // Different transmission session; reject frame
                return false
            }

            collectedChunks[seq] = chunkBytes
            return true
        }

        /**
         * Assembles all collected chunks and verifies CRC32 integrity.
         * Returns reconstructed payload or failure.
         */
        fun assemble(): Result<String> {
            val total = expectedTotal ?: return Result.failure(IllegalStateException("No frames ingested"))
            val expectedCrc = expectedCrcHex ?: return Result.failure(IllegalStateException("No checksum"))

            if (collectedChunks.size != total) {
                return Result.failure(IllegalStateException("Incomplete transmission (${collectedChunks.size}/$total frames)"))
            }

            val totalBytes = (1..total).mapNotNull { collectedChunks[it] }
            if (totalBytes.size != total) {
                return Result.failure(IllegalStateException("Missing frames in sequence"))
            }

            val fullBuffer = totalBytes.reduce { acc, bytes -> acc + bytes }
            val actualCrc = computeCrc32Hex(fullBuffer)

            if (!actualCrc.equals(expectedCrc, ignoreCase = true)) {
                return Result.failure(SecurityException("CRC32 integrity check failed ($actualCrc != $expectedCrc)"))
            }

            return Result.success(String(fullBuffer, Charsets.UTF_8))
        }

        fun reset() {
            expectedTotal = null
            expectedCrcHex = null
            collectedChunks.clear()
        }
    }
}
