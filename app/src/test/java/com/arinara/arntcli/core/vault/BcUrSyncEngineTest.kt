// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BcUrSyncEngineTest {

    @Test
    fun computeCrc32Hex_producesExact8CharacterHex() {
        val data = "Arinara Network Security".toByteArray(Charsets.UTF_8)
        val crcHex = BcUrSyncEngine.computeCrc32Hex(data)
        assertEquals(8, crcHex.length)
        assertTrue(crcHex.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun encode_singleChunk_producesOneUniformResourceFrame() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 200)
        val payload = "Single small payload"
        val frames = engine.encode(payload)

        assertEquals(1, frames.size)
        assertTrue(frames[0].startsWith("ur:bytes/1-1/"))
    }

    @Test
    fun encode_multiChunk_partitionsEqually() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 30)
        val payload = "A".repeat(100) // 100 bytes -> 30 + 30 + 30 + 10 = 4 chunks
        val frames = engine.encode(payload)

        assertEquals(4, frames.size)
        assertTrue(frames[0].startsWith("ur:bytes/1-4/"))
        assertTrue(frames[1].startsWith("ur:bytes/2-4/"))
        assertTrue(frames[2].startsWith("ur:bytes/3-4/"))
        assertTrue(frames[3].startsWith("ur:bytes/4-4/"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encode_blankPayload_throwsException() {
        val engine = BcUrSyncEngine()
        engine.encode("   ")
    }

    @Test
    fun roundtrip_orderedIngestion_reassemblesExactPayload() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 40)
        val originalPayload = """
            {
              "vaultVersion": 1,
              "salt": "aGVsbG8td29ybGQ=",
              "iv": "MDEyMzQ1Njc4OWFi",
              "ciphertext": "k7a1v2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z="
            }
        """.trimIndent()

        val frames = engine.encode(originalPayload)
        assertTrue(frames.size > 1)

        val receiver = BcUrSyncEngine.Receiver()
        assertEquals(0f, receiver.progress, 0.001f)
        assertFalse(receiver.isComplete)

        frames.forEachIndexed { index, frame ->
            val accepted = receiver.ingestFrame(frame)
            assertTrue(accepted)
            val expectedProgress = (index + 1).toFloat() / frames.size.toFloat()
            assertEquals(expectedProgress, receiver.progress, 0.001f)
        }

        assertTrue(receiver.isComplete)
        val assembleResult = receiver.assemble()
        assertTrue(assembleResult.isSuccess)
        assertEquals(originalPayload, assembleResult.getOrThrow())
    }

    @Test
    fun roundtrip_outOfOrderFrames_reassemblesSuccessfully() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 25)
        val originalPayload = "Cryptographic Keyring: [ed25519, secp256k1, rsa4096, wireguard-pre-shared]"
        val frames = engine.encode(originalPayload)
        assertTrue(frames.size >= 3)

        val receiver = BcUrSyncEngine.Receiver()
        // Ingest in reverse order
        frames.reversed().forEach { frame ->
            assertTrue(receiver.ingestFrame(frame))
        }

        assertTrue(receiver.isComplete)
        val result = receiver.assemble()
        assertTrue(result.isSuccess)
        assertEquals(originalPayload, result.getOrThrow())
    }

    @Test
    fun receiver_duplicateFrame_doesNotBreakAssembly() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 50)
        val original = "Offline Air-Gapped Transmission Test"
        val frames = engine.encode(original)

        val receiver = BcUrSyncEngine.Receiver()
        // Ingest frame 1 multiple times
        assertTrue(receiver.ingestFrame(frames[0]))
        assertTrue(receiver.ingestFrame(frames[0]))

        // Ingest remaining frames
        for (i in 1 until frames.size) {
            assertTrue(receiver.ingestFrame(frames[i]))
        }

        assertTrue(receiver.isComplete)
        val result = receiver.assemble()
        assertTrue(result.isSuccess)
        assertEquals(original, result.getOrThrow())
    }

    @Test
    fun receiver_malformedFrame_rejected() {
        val receiver = BcUrSyncEngine.Receiver()
        assertFalse(receiver.ingestFrame("not-a-ur-scheme"))
        assertFalse(receiver.ingestFrame("ur:bytes/"))
        assertFalse(receiver.ingestFrame("ur:bytes/1-2/invalid-no-chunk"))
        assertFalse(receiver.ingestFrame("ur:bytes/abc-def/crc/chunk"))
        assertFalse(receiver.ingestFrame("ur:bytes/1-2/crc/!@#$not-base64"))
        assertFalse(receiver.ingestFrame("ur:bytes/5-2/crc/dGVzdA==")) // seq > total
        assertFalse(receiver.ingestFrame("ur:bytes/0-2/crc/dGVzdA==")) // seq 0
        assertFalse(receiver.ingestFrame("ur:bytes/1-0/crc/dGVzdA==")) // total 0
    }

    @Test
    fun receiver_conflictingSessionFrame_rejected() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 20)
        val session1Frames = engine.encode("Session One Secret Data")
        val session2Frames = engine.encode("Session Two Completely Different Payload")

        val receiver = BcUrSyncEngine.Receiver()
        assertTrue(receiver.ingestFrame(session1Frames[0]))

        // Attempt to ingest frame from session 2 into active session 1
        assertFalse(receiver.ingestFrame(session2Frames[0]))
    }

    @Test
    fun assemble_beforeComplete_fails() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 20)
        val frames = engine.encode("Multiple chunk payload for incomplete check")

        val receiver = BcUrSyncEngine.Receiver()
        assertTrue(receiver.ingestFrame(frames[0]))
        assertFalse(receiver.isComplete)

        val result = receiver.assemble()
        assertFalse(result.isSuccess)
    }

    @Test
    fun receiver_reset_clearsState() {
        val engine = BcUrSyncEngine(chunkSizeBytes = 30)
        val frames = engine.encode("Reset test payload")

        val receiver = BcUrSyncEngine.Receiver()
        receiver.ingestFrame(frames[0])
        assertTrue(receiver.progress > 0f)

        receiver.reset()
        assertEquals(0f, receiver.progress, 0.001f)
        assertFalse(receiver.isComplete)
    }
}
