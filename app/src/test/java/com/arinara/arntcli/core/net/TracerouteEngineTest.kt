// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TracerouteEngineTest {

    @Test
    fun parseHopOutput_ttlExceededLinuxFormat() {
        val output = listOf(
            "From 10.0.0.1 icmp_seq=1 Time to live exceeded",
            "1 packets transmitted, 0 received, +1 errors, 100% packet loss"
        )

        val hop = TracerouteEngine.parseHopOutput(1, output, "8.8.8.8")

        assertEquals(1, hop.hopNumber)
        assertEquals("10.0.0.1", hop.ipAddress)
        assertFalse(hop.isDestination)
        assertFalse(hop.isTimedOut)
    }

    @Test
    fun parseHopOutput_ttlExceededWindowsFormat() {
        val output = listOf(
            "Reply from 192.168.1.1: TTL expired in transit."
        )

        val hop = TracerouteEngine.parseHopOutput(1, output, "1.1.1.1")

        assertEquals(1, hop.hopNumber)
        assertEquals("192.168.1.1", hop.ipAddress)
        assertFalse(hop.isDestination)
        assertFalse(hop.isTimedOut)
    }

    @Test
    fun parseHopOutput_destinationReachedWithLatency() {
        val output = listOf(
            "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms"
        )

        val hop = TracerouteEngine.parseHopOutput(5, output, "8.8.8.8")

        assertEquals(5, hop.hopNumber)
        assertEquals("8.8.8.8", hop.ipAddress)
        assertTrue(hop.isDestination)
        assertEquals(14.2, hop.rttMs ?: 0.0, 0.01)
        assertFalse(hop.isTimedOut)
    }

    @Test
    fun parseHopOutput_timedOutHop() {
        val output = listOf(
            "Request timed out.",
            "100% packet loss"
        )

        val hop = TracerouteEngine.parseHopOutput(3, output, "8.8.8.8")

        assertEquals(3, hop.hopNumber)
        assertNull(hop.ipAddress)
        assertFalse(hop.isDestination)
        assertTrue(hop.isTimedOut)
    }
}
