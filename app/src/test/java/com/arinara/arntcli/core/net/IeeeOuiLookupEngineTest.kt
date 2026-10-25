// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IeeeOuiLookupEngineTest {

    private val engine = IeeeOuiLookupEngine()

    @Test
    fun lookupVendor_raspberryPiColonSeparated() {
        val vendor = engine.lookupVendor("B8:27:EB:12:34:56")
        assertNotNull(vendor)
        assertEquals("Raspberry Pi Foundation", vendor)
    }

    @Test
    fun lookupVendor_raspberryPiHyphenSeparated() {
        val vendor = engine.lookupVendor("dc-a6-32-aa-bb-cc")
        assertNotNull(vendor)
        assertEquals("Raspberry Pi Foundation", vendor)
    }

    @Test
    fun lookupVendor_espressifContiguousHex() {
        val vendor = engine.lookupVendor("240AC4001122")
        assertNotNull(vendor)
        assertEquals("Espressif Inc.", vendor)
    }

    @Test
    fun lookupVendor_ciscoSystems() {
        val vendor = engine.lookupVendor("00:1A:2B:33:44:55")
        assertNotNull(vendor)
        assertEquals("Cisco Systems", vendor)
    }

    @Test
    fun lookupVendor_vmwareVirtualMac() {
        val vendor = engine.lookupVendor("00:50:56:c0:00:08")
        assertNotNull(vendor)
        assertEquals("VMware, Inc.", vendor)
    }

    @Test
    fun lookupVendor_unknownMac_returnsNull() {
        val vendor = engine.lookupVendor("FF:FF:FF:FF:FF:FF")
        assertNull(vendor)
    }

    @Test
    fun extractOui_handlesShortAndMalformedInputs() {
        assertNull(IeeeOuiLookupEngine.extractOui(""))
        assertNull(IeeeOuiLookupEngine.extractOui("12:34"))
        assertEquals("B827EB", IeeeOuiLookupEngine.extractOui("b8-27-eb-01-02-03"))
    }
}
