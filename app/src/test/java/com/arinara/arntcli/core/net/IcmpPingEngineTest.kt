// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcmpPingEngineTest {

    private val engine = IcmpPingEngine()

    @Test
    fun parsePingLine_linuxFormat_parsedCorrectly() {
        val line = "64 bytes from 1.1.1.1: icmp_seq=1 ttl=57 time=14.2 ms"
        val sample = engine.parsePingLine(line, 1, "1.1.1.1", "1.1.1.1")

        assertNotNull(sample)
        assertEquals(1, sample?.sequence)
        assertEquals("1.1.1.1", sample?.ip)
        assertEquals(64, sample?.bytes)
        assertEquals(14.2, sample?.timeMs ?: 0.0, 0.01)
        assertEquals(57, sample?.ttl)
        assertTrue(sample?.isSuccess == true)
    }

    @Test
    fun parsePingLine_windowsFormat_parsedCorrectly() {
        val line = "Reply from 8.8.8.8: bytes=32 time=18ms TTL=117"
        val sample = engine.parsePingLine(line, 2, "8.8.8.8", "8.8.8.8")

        assertNotNull(sample)
        assertEquals(2, sample?.sequence)
        assertEquals("8.8.8.8", sample?.ip)
        assertEquals(32, sample?.bytes)
        assertEquals(18.0, sample?.timeMs ?: 0.0, 0.01)
        assertEquals(117, sample?.ttl)
        assertTrue(sample?.isSuccess == true)
    }

    @Test
    fun parsePingLine_headerAndSummary_ignored() {
        val header = "PING 1.1.1.1 (1.1.1.1) 56(84) bytes of data."
        val summary = "rtt min/avg/max/mdev = 14.2/15.8/18.4/1.2 ms"

        assertNull(engine.parsePingLine(header, 1, "1.1.1.1", "1.1.1.1"))
        assertNull(engine.parsePingLine(summary, 1, "1.1.1.1", "1.1.1.1"))
    }

    @Test
    fun calculateSummary_computesMetricsCorrectly() {
        val samples = listOf(
            PingSample(1, "1.1.1.1", "1.1.1.1", 64, 10.0, 64, true),
            PingSample(2, "1.1.1.1", "1.1.1.1", 64, 20.0, 64, true),
            PingSample(3, "1.1.1.1", "1.1.1.1", 64, 30.0, 64, true),
            PingSample(4, "1.1.1.1", "1.1.1.1", 0, 0.0, 0, false, "Timeout")
        )

        val summary = engine.calculateSummary(samples)

        assertEquals(4, summary.transmitted)
        assertEquals(3, summary.received)
        assertEquals(25.0, summary.packetLossPercent, 0.01)
        assertEquals(10.0, summary.minTimeMs, 0.01)
        assertEquals(20.0, summary.avgTimeMs, 0.01)
        assertEquals(30.0, summary.maxTimeMs, 0.01)
        assertEquals(10.0, summary.jitterMs, 0.01)
    }

    @Test
    fun startPing_localhost_streamsSamples() = runBlocking {
        val results = engine.startPing("127.0.0.1", maxCount = 2, intervalMs = 100L).take(2).toList()

        assertTrue("Should receive at least 1 sample", results.isNotEmpty())
        assertEquals("127.0.0.1", results.first().host)
    }
}
