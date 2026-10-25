// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetCalculatorTest {

    @Test
    fun calculate_standardSlash24_computesCorrectly() {
        val result = SubnetCalculator.calculate("192.168.1.100/24").getOrThrow()

        assertEquals("192.168.1.100", result.ipAddress)
        assertEquals(24, result.prefixLength)
        assertEquals("255.255.255.0", result.netmask)
        assertEquals("0.0.0.255", result.wildcardMask)
        assertEquals("192.168.1.0", result.networkAddress)
        assertEquals("192.168.1.255", result.broadcastAddress)
        assertEquals("192.168.1.1", result.firstUsableIp)
        assertEquals("192.168.1.254", result.lastUsableIp)
        assertEquals(254L, result.usableHosts)
        assertEquals(256L, result.totalHosts)
        assertEquals("11111111.11111111.11111111.00000000", result.binaryNetmask)
        assertEquals("Class C Private (RFC 1918)", result.ipClass)
    }

    @Test
    fun calculate_slash16ClassA_computesCorrectly() {
        val result = SubnetCalculator.calculate("10.0.5.1/16").getOrThrow()

        assertEquals("10.0.0.0", result.networkAddress)
        assertEquals("10.0.255.255", result.broadcastAddress)
        assertEquals("255.255.0.0", result.netmask)
        assertEquals("0.0.255.255", result.wildcardMask)
        assertEquals("10.0.0.1", result.firstUsableIp)
        assertEquals("10.0.255.254", result.lastUsableIp)
        assertEquals(65534L, result.usableHosts)
        assertEquals(65536L, result.totalHosts)
        assertEquals("Class A Private (RFC 1918)", result.ipClass)
    }

    @Test
    fun calculate_slash30PointToPoint_computesCorrectly() {
        val result = SubnetCalculator.calculate("172.16.1.1/30").getOrThrow()

        assertEquals("172.16.1.0", result.networkAddress)
        assertEquals("172.16.1.3", result.broadcastAddress)
        assertEquals("255.255.255.252", result.netmask)
        assertEquals("172.16.1.1", result.firstUsableIp)
        assertEquals("172.16.1.2", result.lastUsableIp)
        assertEquals(2L, result.usableHosts)
        assertEquals(4L, result.totalHosts)
        assertEquals("Class B Private (RFC 1918)", result.ipClass)
    }

    @Test
    fun calculate_slash31Rfc3021_computesCorrectly() {
        val result = SubnetCalculator.calculate("10.1.1.0/31").getOrThrow()

        assertEquals("10.1.1.0", result.networkAddress)
        assertEquals("10.1.1.1", result.broadcastAddress)
        assertEquals("10.1.1.0", result.firstUsableIp)
        assertEquals("10.1.1.1", result.lastUsableIp)
        assertEquals(2L, result.usableHosts)
        assertEquals(2L, result.totalHosts)
    }

    @Test
    fun calculate_slash32HostRoute_computesCorrectly() {
        val result = SubnetCalculator.calculate("1.1.1.1/32").getOrThrow()

        assertEquals("1.1.1.1", result.networkAddress)
        assertEquals("1.1.1.1", result.broadcastAddress)
        assertEquals("1.1.1.1", result.firstUsableIp)
        assertEquals("1.1.1.1", result.lastUsableIp)
        assertEquals(1L, result.usableHosts)
        assertEquals(1L, result.totalHosts)
    }

    @Test
    fun calculate_apipaLinkLocal_classifiesCorrectly() {
        val result = SubnetCalculator.calculate("169.254.10.20/16").getOrThrow()
        assertEquals("Link-Local / APIPA (RFC 3927)", result.ipClass)
    }

    @Test
    fun calculate_cgnatSharedSpace_classifiesCorrectly() {
        val result = SubnetCalculator.calculate("100.64.0.1/10").getOrThrow()
        assertEquals("Shared CGNAT Space (RFC 6598)", result.ipClass)
    }

    @Test
    fun calculate_invalidInputs_returnFailure() {
        assertTrue(SubnetCalculator.calculate("192.168.1.1").isFailure)
        assertTrue(SubnetCalculator.calculate("192.168.1.1/33").isFailure)
        assertTrue(SubnetCalculator.calculate("192.168.1.1/-1").isFailure)
        assertTrue(SubnetCalculator.calculate("256.0.0.1/24").isFailure)
        assertTrue(SubnetCalculator.calculate("invalid.ip.here/24").isFailure)
        assertTrue(SubnetCalculator.calculate("").isFailure)
    }
}
