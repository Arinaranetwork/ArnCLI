// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

class LanDiscoveryEngineTest {

    private val engine = LanDiscoveryEngine()

    @Test
    fun deriveSubnetPrefix_validIps() {
        assertEquals("192.168.1.", LanDiscoveryEngine.deriveSubnetPrefix("192.168.1.100"))
        assertEquals("10.0.2.", LanDiscoveryEngine.deriveSubnetPrefix("10.0.2.15"))
        assertEquals("172.16.0.", LanDiscoveryEngine.deriveSubnetPrefix("172.16.0.1"))
        assertEquals("192.168.1.", LanDiscoveryEngine.deriveSubnetPrefix("bad-ip"))
    }

    @Test
    fun probeHost_loopbackWithOpenPort_discoversHost() = runBlocking {
        val serverSocket = ServerSocket(0)
        val boundPort = serverSocket.localPort

        try {
            // Even if ICMP is blocked, the engine probes common ports
            val host = engine.probeHost("127.0.0.1", timeoutMs = 300)
            assertNotNull(host)
            assertEquals("127.0.0.1", host?.ipAddress)
            assertTrue(host?.isReachable == true)
            assertTrue((host?.latencyMs ?: 0.0) >= 0.0)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun probeHost_unreachableIp_returnsNull() = runBlocking {
        // 192.0.2.1 is reserved for documentation (RFC 5737 TEST-NET-1) and unreachable
        val host = engine.probeHost("192.0.2.1", timeoutMs = 150)
        assertNull(host)
    }

    @Test
    fun discover_smallRange_completesSuccessfully() = runBlocking {
        val result = engine.discover(
            subnetPrefix = "127.0.0.",
            hostRange = 1..2,
            timeoutMs = 150,
            chunkSize = 2
        )

        assertNotNull(result)
        assertEquals("127.0.0.", result.subnetPrefix)
        assertEquals(2, result.scannedCount)
        assertTrue(result.durationMs >= 0.0)
    }
}
