// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerTelemetryEngineTest {

    @Test
    fun parseCpuStat_computesAccurateDifferentialUsage() {
        val sample1 = """
            cpu  1000 200 300 8000 100 50 20 10
            cpu0 250 50 75 2000 25 12 5 2
            cpu1 250 50 75 2000 25 12 5 2
            cpu2 250 50 75 2000 25 12 5 2
            cpu3 250 50 75 2000 25 12 5 2
            intr 123456
        """.trimIndent()

        val cpu1 = ServerTelemetryEngine.parseCpuStat(sample1, null)
        assertEquals(4, cpu1.vCpuCount)
        assertEquals(0f, cpu1.usagePercent, 0.01f)

        // Sample 2: total ticks increased by 1000, idle increased by 600 (400 active ticks = 40% usage)
        val sample2 = """
            cpu  1300 250 350 8600 100 50 20 10
            cpu0 325 62 87 2150 25 12 5 2
            cpu1 325 62 87 2150 25 12 5 2
            cpu2 325 62 87 2150 25 12 5 2
            cpu3 325 62 87 2150 25 12 5 2
            intr 123490
        """.trimIndent()

        val cpu2 = ServerTelemetryEngine.parseCpuStat(sample2, cpu1)
        assertEquals(4, cpu2.vCpuCount)
        assertEquals(40.0f, cpu2.usagePercent, 0.5f)
        assertEquals(4, cpu2.perCoreUsagePercent.size)
        assertEquals(40.0f, cpu2.perCoreUsagePercent[0], 0.5f)
    }

    @Test
    fun parseMemInfo_calculatesMemoryAndSwap() {
        val raw = """
            MemTotal:       16384000 kB
            MemFree:         4096000 kB
            MemAvailable:   12288000 kB
            Buffers:          512000 kB
            Cached:          7680000 kB
            SwapTotal:       2097152 kB
            SwapFree:        1048576 kB
        """.trimIndent()

        val mem = ServerTelemetryEngine.parseMemInfo(raw)
        assertEquals(16384000L * 1024L, mem.totalBytes)
        assertEquals(12288000L * 1024L, mem.availableBytes)
        assertEquals((16384000L - 12288000L) * 1024L, mem.usedBytes)
        assertEquals(25.0f, mem.usagePercent, 0.01f)

        assertEquals(2097152L * 1024L, mem.swapTotalBytes)
        assertEquals(1048576L * 1024L, mem.swapUsedBytes)
        assertEquals(50.0f, mem.swapUsagePercent, 0.01f)
    }

    @Test
    fun parseMemInfo_fallbackForLegacyKernels() {
        val rawLegacy = """
            MemTotal:       8000000 kB
            MemFree:        2000000 kB
            Buffers:         500000 kB
            Cached:         1500000 kB
            SwapTotal:            0 kB
            SwapFree:             0 kB
        """.trimIndent()

        val mem = ServerTelemetryEngine.parseMemInfo(rawLegacy)
        // Available = Free (2000) + Buffers (500) + Cached (1500) = 4000 MB
        assertEquals(4000000L * 1024L, mem.availableBytes)
        assertEquals(50.0f, mem.usagePercent, 0.01f)
    }

    @Test
    fun parseLoadAvg_computesSaturationRatio() {
        val raw = "1.50 0.85 0.60 2/450 12345"
        val load4Cores = ServerTelemetryEngine.parseLoadAvg(raw, vCpuCount = 4)
        assertEquals(1.50f, load4Cores.load1m, 0.01f)
        assertEquals(0.85f, load4Cores.load5m, 0.01f)
        assertEquals(0.60f, load4Cores.load15m, 0.01f)
        assertEquals(0.375f, load4Cores.saturationRatio, 0.01f)
        assertFalse(load4Cores.isOverloaded)
        assertEquals("Healthy", load4Cores.statusDescription())

        val load1Core = ServerTelemetryEngine.parseLoadAvg(raw, vCpuCount = 1)
        assertEquals(1.50f, load1Core.saturationRatio, 0.01f)
        assertTrue(load1Core.isOverloaded)
        assertEquals("CPU Queuing Alert", load1Core.statusDescription())
    }

    @Test
    fun parseDf_filtersVirtualFilesystemsAndCalculatesBytes() {
        val raw = """
            Filesystem     1024-blocks      Used Available Capacity Mounted on
            /dev/nvme0n1p2    78643200  19660800  58982400      25% /
            tmpfs              8192000         0   8192000       0% /dev/shm
            /dev/sda1        524288000 314572800 209715200      60% /data
            overlay           78643200  19660800  58982400      25% /var/lib/docker/overlay2/abc
        """.trimIndent()

        val mounts = ServerTelemetryEngine.parseDf(raw)
        assertEquals(2, mounts.size) // tmpfs and overlay excluded

        val root = mounts[0]
        assertEquals("/dev/nvme0n1p2", root.filesystem)
        assertEquals("/", root.mountPoint)
        assertEquals(78643200L * 1024L, root.totalBytes)
        assertEquals(19660800L * 1024L, root.usedBytes)
        assertEquals(25, root.usagePercent)

        val data = mounts[1]
        assertEquals("/data", data.mountPoint)
        assertEquals(60, data.usagePercent)
    }

    @Test
    fun parseBatchOutput_compositeParsing() {
        val composite = """
            cpu  1000 200 300 8000 100 50 20 10
            cpu0 250 50 75 2000 25 12 5 2
            ${ServerTelemetryEngine.DELIMITER_STAT}
            MemTotal:       16384000 kB
            MemAvailable:   12288000 kB
            ${ServerTelemetryEngine.DELIMITER_MEM}
            0.45 0.50 0.55 1/300 9999
            ${ServerTelemetryEngine.DELIMITER_LOAD}
            Inter-|   Receive                                                |  Transmit
             face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                lo: 1234567      10    0    0    0     0          0         0  1234567      10    0    0    0     0       0          0
              eth0: 50000000    5000    0    0    0     0          0         0 25000000    2500    0    0    0     0       0          0
            ${ServerTelemetryEngine.DELIMITER_NET}
            Filesystem     1024-blocks      Used Available Capacity Mounted on
            /dev/sda1         20971520   5242880  15728640      25% /
            ${ServerTelemetryEngine.DELIMITER_DF}
        """.trimIndent()

        val snapshot = ServerTelemetryEngine.parseBatchOutput(composite, "prod-server-01")
        assertEquals("prod-server-01", snapshot.hostId)
        assertEquals(1, snapshot.cpu.vCpuCount)
        assertEquals(25.0f, snapshot.memory.usagePercent, 0.01f)
        assertEquals(0.45f, snapshot.load.load1m, 0.01f)
        assertEquals(1, snapshot.network.size) // lo excluded
        assertEquals("eth0", snapshot.network[0].interfaceName)
        assertEquals(50000000L, snapshot.network[0].rxBytes)
        assertEquals(1, snapshot.disks.size)
        assertEquals("/", snapshot.disks[0].mountPoint)
    }
}
