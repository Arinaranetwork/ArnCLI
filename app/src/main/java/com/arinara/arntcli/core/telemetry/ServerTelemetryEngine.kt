// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.telemetry

/**
 * Agentless Linux /proc & POSIX Telemetry Engine.
 * Executes a single atomic multiplexed shell command over SSH or local shell,
 * then parses CPU, memory, load averages, network interfaces, and disk mounts
 * with microsecond precision and zero remote daemon dependencies.
 */
class ServerTelemetryEngine {

    companion object {
        const val DELIMITER_STAT = "---EOF-STAT---"
        const val DELIMITER_MEM = "---EOF-MEM---"
        const val DELIMITER_LOAD = "---EOF-LOAD---"
        const val DELIMITER_NET = "---EOF-NET---"
        const val DELIMITER_DF = "---EOF-DF---"

        /**
         * Returns the single-roundtrip POSIX command batch.
         */
        fun buildBatchCommand(): String {
            return "cat /proc/stat 2>/dev/null; echo \"$DELIMITER_STAT\"; " +
                    "cat /proc/meminfo 2>/dev/null; echo \"$DELIMITER_MEM\"; " +
                    "cat /proc/loadavg 2>/dev/null; echo \"$DELIMITER_LOAD\"; " +
                    "cat /proc/net/dev 2>/dev/null; echo \"$DELIMITER_NET\"; " +
                    "df -k -P 2>/dev/null; echo \"$DELIMITER_DF\""
        }

        /**
         * Parses the entire composite batch output into a structured telemetry snapshot.
         */
        fun parseBatchOutput(
            rawOutput: String,
            hostId: String = "remote",
            previousSnapshot: ServerTelemetrySnapshot? = null
        ): ServerTelemetrySnapshot {
            val statIndex = rawOutput.indexOf(DELIMITER_STAT)
            val memIndex = rawOutput.indexOf(DELIMITER_MEM)
            val loadIndex = rawOutput.indexOf(DELIMITER_LOAD)
            val netIndex = rawOutput.indexOf(DELIMITER_NET)
            val dfIndex = rawOutput.indexOf(DELIMITER_DF)

            val statSection = if (statIndex != -1) rawOutput.substring(0, statIndex).trim() else ""
            val memSection = if (statIndex != -1 && memIndex != -1) rawOutput.substring(statIndex + DELIMITER_STAT.length, memIndex).trim() else ""
            val loadSection = if (memIndex != -1 && loadIndex != -1) rawOutput.substring(memIndex + DELIMITER_MEM.length, loadIndex).trim() else ""
            val netSection = if (loadIndex != -1 && netIndex != -1) rawOutput.substring(loadIndex + DELIMITER_LOAD.length, netIndex).trim() else ""
            val dfSection = if (netIndex != -1 && dfIndex != -1) rawOutput.substring(netIndex + DELIMITER_NET.length, dfIndex).trim() else ""

            val now = System.currentTimeMillis()
            val deltaTimeSec = if (previousSnapshot != null) {
                ((now - previousSnapshot.timestamp) / 1000.0).coerceAtLeast(0.1)
            } else 1.0

            val cpu = parseCpuStat(statSection, previousSnapshot?.cpu)
            val memory = parseMemInfo(memSection)
            val load = parseLoadAvg(loadSection, cpu.vCpuCount)
            val network = parseNetDev(netSection, previousSnapshot?.network, deltaTimeSec)
            val disks = parseDf(dfSection)

            return ServerTelemetrySnapshot(
                timestamp = now,
                hostId = hostId,
                cpu = cpu,
                memory = memory,
                load = load,
                disks = disks,
                network = network
            )
        }

        /**
         * Parses /proc/stat with differential utilization calculation.
         */
        fun parseCpuStat(section: String, prevCpu: CpuMetrics?): CpuMetrics {
            if (section.isBlank()) {
                return CpuMetrics(usagePercent = 0f, vCpuCount = 1)
            }

            var overallTick: CpuCoreTick? = null
            val perCoreTicks = mutableListOf<CpuCoreTick>()

            val lines = section.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("cpu ")) {
                    overallTick = parseTickLine(trimmed.substring(4).trim())
                } else if (trimmed.matches(Regex("""^cpu\d+\s+.*"""))) {
                    val parts = trimmed.split(Regex("""\s+"""), limit = 2)
                    if (parts.size == 2) {
                        parseTickLine(parts[1])?.let { perCoreTicks.add(it) }
                    }
                }
            }

            val vCpuCount = perCoreTicks.size.coerceAtLeast(1)

            val overallUsage = if (overallTick != null && prevCpu?.overallTicks != null) {
                calculateUsage(prevCpu.overallTicks, overallTick)
            } else {
                0f
            }

            val perCoreUsage = mutableListOf<Float>()
            for (i in perCoreTicks.indices) {
                val current = perCoreTicks[i]
                val prev = prevCpu?.perCoreTicks?.getOrNull(i)
                val usage = if (prev != null) calculateUsage(prev, current) else 0f
                perCoreUsage.add(usage)
            }

            return CpuMetrics(
                usagePercent = overallUsage,
                vCpuCount = vCpuCount,
                perCoreUsagePercent = perCoreUsage,
                overallTicks = overallTick,
                perCoreTicks = perCoreTicks
            )
        }

        private fun parseTickLine(rawValues: String): CpuCoreTick? {
            val tokens = rawValues.split(Regex("""\s+""")).mapNotNull { it.toLongOrNull() }
            if (tokens.size < 4) return null
            return CpuCoreTick(
                user = tokens.getOrElse(0) { 0L },
                nice = tokens.getOrElse(1) { 0L },
                system = tokens.getOrElse(2) { 0L },
                idle = tokens.getOrElse(3) { 0L },
                iowait = tokens.getOrElse(4) { 0L },
                irq = tokens.getOrElse(5) { 0L },
                softirq = tokens.getOrElse(6) { 0L },
                steal = tokens.getOrElse(7) { 0L }
            )
        }

        private fun calculateUsage(prev: CpuCoreTick, curr: CpuCoreTick): Float {
            val totalDelta = curr.totalTime - prev.totalTime
            val idleDelta = curr.idleTime - prev.idleTime
            if (totalDelta <= 0) return 0f
            val usage = (1.0f - (idleDelta.toFloat() / totalDelta.toFloat())) * 100f
            return usage.coerceIn(0f, 100f)
        }

        /**
         * Parses /proc/meminfo in kilobytes, converts to bytes.
         */
        fun parseMemInfo(section: String): MemoryMetrics {
            var memTotal = 0L
            var memFree = 0L
            var memAvailable = -1L
            var buffers = 0L
            var cached = 0L
            var swapTotal = 0L
            var swapFree = 0L

            for (line in section.lines()) {
                val parts = line.split(":")
                if (parts.size != 2) continue
                val key = parts[0].trim()
                val valueKb = parts[1].trim().split(" ").firstOrNull()?.toLongOrNull() ?: continue
                val valueBytes = valueKb * 1024L

                when (key) {
                    "MemTotal" -> memTotal = valueBytes
                    "MemFree" -> memFree = valueBytes
                    "MemAvailable" -> memAvailable = valueBytes
                    "Buffers" -> buffers = valueBytes
                    "Cached" -> cached = valueBytes
                    "SwapTotal" -> swapTotal = valueBytes
                    "SwapFree" -> swapFree = valueBytes
                }
            }

            // Fallback for Linux kernels < 3.14 without MemAvailable
            val finalAvailable = if (memAvailable >= 0L) {
                memAvailable
            } else {
                (memFree + buffers + cached).coerceAtMost(memTotal)
            }

            return MemoryMetrics(
                totalBytes = memTotal,
                availableBytes = finalAvailable,
                freeBytes = memFree,
                buffersBytes = buffers,
                cachedBytes = cached,
                swapTotalBytes = swapTotal,
                swapFreeBytes = swapFree
            )
        }

        /**
         * Parses /proc/loadavg.
         * Example: "0.18 0.25 0.22 2/480 14820"
         */
        fun parseLoadAvg(section: String, vCpuCount: Int): LoadAvgMetrics {
            val tokens = section.trim().split(Regex("""\s+"""))
            val l1 = tokens.getOrNull(0)?.toFloatOrNull() ?: 0f
            val l5 = tokens.getOrNull(1)?.toFloatOrNull() ?: 0f
            val l15 = tokens.getOrNull(2)?.toFloatOrNull() ?: 0f

            return LoadAvgMetrics(
                load1m = l1,
                load5m = l5,
                load15m = l15,
                vCpuCount = vCpuCount
            )
        }

        /**
         * Parses /proc/net/dev for physical and active network adapters.
         */
        fun parseNetDev(
            section: String,
            prevNet: List<NetDevMetrics>?,
            deltaTimeSec: Double
        ): List<NetDevMetrics> {
            val results = mutableListOf<NetDevMetrics>()
            val lines = section.lines()

            for (line in lines) {
                if (!line.contains(":")) continue
                val parts = line.split(":")
                if (parts.size != 2) continue
                val iface = parts[0].trim()
                if (iface == "lo") continue // Skip loopback

                val cols = parts[1].trim().split(Regex("""\s+""")).mapNotNull { it.toLongOrNull() }
                if (cols.size < 16) continue

                val rxBytes = cols[0]
                val rxErrs = cols[2]
                val txBytes = cols[8]
                val txErrs = cols[10]

                val prev = prevNet?.firstOrNull { it.interfaceName == iface }
                val rxSpeed = if (prev != null && deltaTimeSec > 0) {
                    ((rxBytes - prev.rxBytes).coerceAtLeast(0L) / deltaTimeSec).toLong()
                } else 0L

                val txSpeed = if (prev != null && deltaTimeSec > 0) {
                    ((txBytes - prev.txBytes).coerceAtLeast(0L) / deltaTimeSec).toLong()
                } else 0L

                results.add(
                    NetDevMetrics(
                        interfaceName = iface,
                        rxBytes = rxBytes,
                        txBytes = txBytes,
                        rxSpeedBytesPerSec = rxSpeed,
                        txSpeedBytesPerSec = txSpeed,
                        rxErrors = rxErrs,
                        txErrors = txErrs
                    )
                )
            }

            return results
        }

        /**
         * Parses POSIX 'df -k -P' output.
         * Lines: Filesystem 1024-blocks Used Available Capacity Mounted on
         */
        fun parseDf(section: String): List<DiskMountMetrics> {
            val results = mutableListOf<DiskMountMetrics>()
            val ignoredFs = setOf("tmpfs", "devtmpfs", "udev", "overlay", "squashfs", "none", "shm")

            val lines = section.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("Filesystem") || trimmed.isBlank()) continue

                val tokens = trimmed.split(Regex("""\s+"""))
                if (tokens.size < 6) continue

                val filesystem = tokens[0]
                if (ignoredFs.any { filesystem.startsWith(it) }) continue

                val totalBlocks = tokens[1].toLongOrNull() ?: continue
                val usedBlocks = tokens[2].toLongOrNull() ?: continue
                val availBlocks = tokens[3].toLongOrNull() ?: continue
                val percentStr = tokens[4].replace("%", "").trim()
                val percent = percentStr.toIntOrNull() ?: 0
                val mountPoint = tokens.subList(5, tokens.size).joinToString(" ")

                results.add(
                    DiskMountMetrics(
                        filesystem = filesystem,
                        mountPoint = mountPoint,
                        totalBytes = totalBlocks * 1024L,
                        usedBytes = usedBlocks * 1024L,
                        availableBytes = availBlocks * 1024L,
                        usagePercent = percent
                    )
                )
            }

            return results
        }
    }
}
