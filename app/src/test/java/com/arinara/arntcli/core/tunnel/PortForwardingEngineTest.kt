// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.tunnel

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortForwardingEngineTest {

    private val engine = PortForwardingEngine()

    @Test
    fun tunnelRule_validLocalTunnel() {
        val rule = TunnelRule(
            id = "t-1",
            name = "Postgres Tunnel",
            type = TunnelType.Local,
            localPort = 5432,
            remoteHost = "localhost",
            remotePort = 5432
        )

        assertEquals("-L 5432:localhost:5432", rule.toCliSyntax())
        assertEquals("127.0.0.1:5432 → localhost:5432", rule.toEndpointSummary())
    }

    @Test
    fun tunnelRule_validDynamicSocksTunnel() {
        val rule = TunnelRule(
            id = "t-2",
            name = "SOCKS Proxy",
            type = TunnelType.DynamicSocks5,
            localPort = 1080
        )

        assertEquals("-D 1080", rule.toCliSyntax())
        assertEquals("SOCKS5 Proxy on 127.0.0.1:1080", rule.toEndpointSummary())
    }

    @Test(expected = IllegalArgumentException::class)
    fun tunnelRule_invalidPort_throwsException() {
        TunnelRule(
            id = "t-invalid",
            name = "Bad Port",
            type = TunnelType.Local,
            localPort = 70000,
            remoteHost = "localhost",
            remotePort = 80
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun tunnelRule_blankRemoteHost_throwsException() {
        TunnelRule(
            id = "t-blank",
            name = "Blank Host",
            type = TunnelType.Local,
            localPort = 8080,
            remoteHost = "",
            remotePort = 80
        )
    }

    @Test
    fun engine_addUpdateAndRemoveRules() {
        val rule = TunnelRule(
            id = "rule-1",
            name = "Web",
            type = TunnelType.Local,
            localPort = 8080,
            remoteHost = "10.0.0.1",
            remotePort = 80
        )

        assertTrue(engine.addRule(rule))
        assertFalse(engine.addRule(rule)) // Duplicate ID

        assertEquals(1, engine.rules.value.size)

        val updated = rule.copy(name = "Web Admin")
        engine.updateRule(updated)
        assertEquals("Web Admin", engine.rules.value.first().name)

        engine.removeRule("rule-1")
        assertTrue(engine.rules.value.isEmpty())
    }

    @Test
    fun engine_toggleTunnelLifecycle() = runBlocking {
        val rule = TunnelRule(
            id = "toggle-1",
            name = "Test Tunnel",
            type = TunnelType.DynamicSocks5,
            localPort = 25055 // High unallocated port
        )

        engine.addRule(rule)
        val activeRule = engine.toggleTunnel("toggle-1")

        // Should activate cleanly if port is available
        if (activeRule.status == TunnelStatus.Active) {
            val deactivated = engine.toggleTunnel("toggle-1")
            assertEquals(TunnelStatus.Inactive, deactivated.status)
        } else {
            assertEquals(TunnelStatus.Error, activeRule.status)
            assertNotNull(activeRule.errorMessage)
        }
    }
}
