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
import java.net.ServerSocket

class LanPortScannerTest {

    private val scanner = LanPortScanner()

    @Test
    fun resolveServiceName_knownPorts() {
        assertEquals("SSH", LanPortScanner.resolveServiceName(22))
        assertEquals("HTTP", LanPortScanner.resolveServiceName(80))
        assertEquals("HTTPS", LanPortScanner.resolveServiceName(443))
        assertEquals("RDP", LanPortScanner.resolveServiceName(3389))
        assertEquals("MySQL", LanPortScanner.resolveServiceName(3306))
        assertEquals("Unknown", LanPortScanner.resolveServiceName(54321))
    }

    @Test
    fun scanPort_closedPort_returnsClosed() = runBlocking {
        // Port 59999 is typically unused on loopback
        val result = scanner.scanPort("127.0.0.1", 59999, timeoutMs = 200)
        assertEquals(59999, result.port)
        assertFalse(result.isOpen)
        assertNotNull(result.latencyMs)
    }

    @Test
    fun scanPort_openServerSocket_returnsOpen() = runBlocking {
        val serverSocket = ServerSocket(0)
        val boundPort = serverSocket.localPort

        try {
            val result = scanner.scanPort("127.0.0.1", boundPort, timeoutMs = 500)
            assertEquals(boundPort, result.port)
            assertTrue(result.isOpen)
            assertNotNull(result.latencyMs)
            assertTrue((result.latencyMs ?: 0.0) >= 0.0)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun scanPorts_multiplePorts_returnsComprehensiveResult() = runBlocking {
        val serverSocket = ServerSocket(0)
        val openPort = serverSocket.localPort
        val closedPort = 59998

        try {
            val scanResult = scanner.scanPorts(
                targetHost = "127.0.0.1",
                ports = listOf(openPort, closedPort),
                timeoutMs = 300
            )

            assertTrue(scanResult.isSuccess)
            val data = scanResult.getOrNull()
            assertNotNull(data)
            assertEquals("127.0.0.1", data?.targetHost)
            assertEquals(2, data?.scannedPorts?.size)
            assertEquals(1, data?.openPortCount)

            val openReport = data?.scannedPorts?.first { it.port == openPort }
            assertTrue(openReport?.isOpen == true)

            val closedReport = data?.scannedPorts?.first { it.port == closedPort }
            assertFalse(closedReport?.isOpen == true)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun scanPorts_blankHost_fails() = runBlocking {
        val scanResult = scanner.scanPorts("   ", listOf(80))
        assertTrue(scanResult.isFailure)
    }
}
