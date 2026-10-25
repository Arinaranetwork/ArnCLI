// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogEnginesTest {

    @Test
    fun uptimeWatchdog_targetRegistrationAndDamping() = runBlocking {
        val engine = UptimeWatchdogEngine()
        val target = WatchdogProbeTarget(
            id = "probe-test-1",
            name = "Local Mock Port",
            type = WatchdogProbeType.TcpPort,
            endpoint = "127.0.0.1:65530", // Closed port
            failureThreshold = 2
        )

        engine.addTarget(target)
        assertEquals(1, engine.services.value.size)
        assertEquals(ProbeStatus.Unknown, engine.services.value[0].status)

        // First failure -> Status Degraded (within threshold of 2)
        val r1 = engine.executeProbe(target)
        assertFalse(r1.isUp)
        val s1 = engine.services.value[0]
        assertEquals(1, s1.consecutiveFailures)
        assertEquals(ProbeStatus.Degraded, s1.status)
        assertEquals(1, s1.recentLatencies.size)

        // Second failure -> Status Down (reaches threshold of 2)
        val r2 = engine.executeProbe(target)
        assertFalse(r2.isUp)
        val s2 = engine.services.value[0]
        assertEquals(2, s2.consecutiveFailures)
        assertEquals(ProbeStatus.Down, s2.status)
        assertEquals(2, s2.recentLatencies.size)
    }

    @Test
    fun watchdogService_uptimePercentageCalculation() {
        val target = WatchdogProbeTarget(name = "Web", type = WatchdogProbeType.Http, endpoint = "http://example.com")
        val service = WatchdogService(
            target = target,
            totalChecks = 100,
            successfulChecks = 98,
            recentLatencies = listOf(30L, 40L, 50L)
        )

        assertEquals(98.0f, service.uptimePercent, 0.01f)
        assertEquals(40L, service.averageLatencyMs)
    }

    @Test
    fun sslCertStatus_healthLevelGrading() {
        val healthy = SslCertStatus(host = "secure.com", port = 443, daysRemaining = 45, healthLevel = SslHealthLevel.Healthy)
        assertEquals(SslHealthLevel.Healthy, healthy.healthLevel)

        val warning = SslCertStatus(host = "warning.com", port = 443, daysRemaining = 20, healthLevel = SslHealthLevel.Warning)
        assertEquals(SslHealthLevel.Warning, warning.healthLevel)

        val critical = SslCertStatus(host = "critical.com", port = 443, daysRemaining = 5, healthLevel = SslHealthLevel.Critical)
        assertEquals(SslHealthLevel.Critical, critical.healthLevel)

        val expired = SslCertStatus(host = "expired.com", port = 443, daysRemaining = -2, healthLevel = SslHealthLevel.Expired)
        assertEquals(SslHealthLevel.Expired, expired.healthLevel)
    }
}
