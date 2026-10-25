// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsResolverEngineTest {

    private val engine = DnsResolverEngine()

    @Test
    fun resolve_loopbackAddress_returnsRecord() = runBlocking {
        val result = engine.resolve("127.0.0.1")

        assertTrue(result.isSuccess)
        val lookup = result.getOrThrow()
        assertEquals("127.0.0.1", lookup.queryHost)
        assertFalse(lookup.records.isEmpty())
        assertTrue(lookup.records.any { it.value == "127.0.0.1" && it.isLoopback })
    }

    @Test
    fun reverseLookup_loopback_returnsHost() = runBlocking {
        val result = engine.reverseLookup("127.0.0.1")

        assertTrue(result.isSuccess)
        val host = result.getOrThrow()
        assertTrue("Host should be localhost or non-empty, got: $host", host.isNotBlank())
    }

    @Test
    fun resolve_blankHost_returnsFailure() = runBlocking {
        val result = engine.resolve("   ")
        assertTrue(result.isFailure)
    }
}
