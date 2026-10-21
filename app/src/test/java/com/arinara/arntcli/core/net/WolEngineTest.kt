// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WolEngineTest {

    private val engine = WolEngine()

    @Test
    fun parseMac_withColons_returnsCorrectBytes() {
        val mac = "00:11:22:33:44:55"
        val bytes = WolEngine.parseMac(mac)
        assertEquals(6, bytes.size)
        assertEquals(0x00.toByte(), bytes[0])
        assertEquals(0x11.toByte(), bytes[1])
        assertEquals(0x22.toByte(), bytes[2])
        assertEquals(0x33.toByte(), bytes[3])
        assertEquals(0x44.toByte(), bytes[4])
        assertEquals(0x55.toByte(), bytes[5])
    }

    @Test
    fun parseMac_withHyphens_returnsCorrectBytes() {
        val mac = "AA-BB-CC-DD-EE-FF"
        val bytes = WolEngine.parseMac(mac)
        assertEquals(6, bytes.size)
        assertEquals(0xAA.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xCC.toByte(), bytes[2])
        assertEquals(0xDD.toByte(), bytes[3])
        assertEquals(0xEE.toByte(), bytes[4])
        assertEquals(0xFF.toByte(), bytes[5])
    }

    @Test
    fun parseMac_withoutSeparators_returnsCorrectBytes() {
        val mac = "0123456789ab"
        val bytes = WolEngine.parseMac(mac)
        assertEquals(6, bytes.size)
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x23.toByte(), bytes[1])
        assertEquals(0x45.toByte(), bytes[2])
        assertEquals(0x67.toByte(), bytes[3])
        assertEquals(0x89.toByte(), bytes[4])
        assertEquals(0xAB.toByte(), bytes[5])
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseMac_invalidLength_throwsException() {
        WolEngine.parseMac("00:11:22")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseMac_invalidChars_throwsException() {
        WolEngine.parseMac("00:11:22:33:44:ZZ")
    }

    @Test
    fun buildMagicPacket_createsExact102BytesPayload() {
        val macBytes = WolEngine.parseMac("12:34:56:78:9A:BC")
        val packet = WolEngine.buildMagicPacket(macBytes)

        assertEquals(102, packet.size)

        // Verify 6 bytes of 0xFF
        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i])
        }

        // Verify 16 repetitions of the 6-byte MAC
        for (rep in 0 until 16) {
            val offset = 6 + rep * 6
            for (byteIndex in 0 until 6) {
                assertEquals(macBytes[byteIndex], packet[offset + byteIndex])
            }
        }
    }

    @Test
    fun formatMac_formatsColonSeparatedUppercase() {
        val macBytes = byteArrayOf(0x0a.toByte(), 0x1b.toByte(), 0x2c.toByte(), 0x3d.toByte(), 0x4e.toByte(), 0x5f.toByte())
        val formatted = WolEngine.formatMac(macBytes)
        assertEquals("0A:1B:2C:3D:4E:5F", formatted)
    }

    @Test
    fun sendMagicPacket_toLoopback_succeeds() = runBlocking {
        val result = engine.sendMagicPacket(
            macAddress = "00:11:22:33:44:55",
            broadcastIp = "127.0.0.1",
            port = 9999
        )

        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals(102, data?.bytesSent)
        assertEquals("00:11:22:33:44:55", data?.formattedMac)
        assertEquals("127.0.0.1", data?.broadcastIp)
    }
}
