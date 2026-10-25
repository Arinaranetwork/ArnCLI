// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiInspectorEngineTest {

    private val engine = WifiInspectorEngine()

    @Test
    fun calculateSignalPercent_boundaryChecks() {
        assertEquals(100, WifiInspectorEngine.calculateSignalPercent(-50))
        assertEquals(100, WifiInspectorEngine.calculateSignalPercent(-30))
        assertEquals(0, WifiInspectorEngine.calculateSignalPercent(-100))
        assertEquals(0, WifiInspectorEngine.calculateSignalPercent(-115))
        assertEquals(50, WifiInspectorEngine.calculateSignalPercent(-75))
        assertEquals(80, WifiInspectorEngine.calculateSignalPercent(-60))
    }

    @Test
    fun calculateSignalQuality_ratings() {
        assertEquals("Excellent", WifiInspectorEngine.calculateSignalQuality(95))
        assertEquals("Good", WifiInspectorEngine.calculateSignalQuality(70))
        assertEquals("Fair", WifiInspectorEngine.calculateSignalQuality(50))
        assertEquals("Weak", WifiInspectorEngine.calculateSignalQuality(25))
        assertEquals("No Signal", WifiInspectorEngine.calculateSignalQuality(0))
    }

    @Test
    fun calculateBand_frequencies() {
        assertEquals("2.4 GHz", WifiInspectorEngine.calculateBand(2412))
        assertEquals("2.4 GHz", WifiInspectorEngine.calculateBand(2462))
        assertEquals("5 GHz", WifiInspectorEngine.calculateBand(5180))
        assertEquals("5 GHz", WifiInspectorEngine.calculateBand(5825))
        assertEquals("6 GHz (Wi-Fi 6E/7)", WifiInspectorEngine.calculateBand(6125))
        assertEquals("6 GHz (Wi-Fi 6E/7)", WifiInspectorEngine.calculateBand(6985))
    }

    @Test
    fun formatIpAddress_integerConversion() {
        // 192.168.1.100 in little-endian integer
        // 100 + (1 << 8) + (168 << 16) + (192 << 24)
        val ipInt = 100 or (1 shl 8) or (168 shl 16) or (192 shl 24)
        val formatted = WifiInspectorEngine.formatIpAddress(ipInt)
        assertEquals("100.1.168.192", formatted)
        assertEquals("0.0.0.0", WifiInspectorEngine.formatIpAddress(0))
    }

    @Test
    fun inspectNetworkInterfaces_returnsValidDetails() {
        val details = engine.inspectNetworkInterfaces()
        assertNotNull(details)
        assertTrue(details.isConnected)
        assertTrue(details.ipAddress.isNotBlank())
        assertTrue(details.signalPercent in 0..100)
        assertNotNull(details.signalQuality)
        assertNotNull(details.frequencyBand)
    }
}
