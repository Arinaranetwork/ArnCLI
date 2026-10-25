// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockerDemuxParserTest {

    @Test
    fun demuxStream_multiplexedStdoutAndStderr() {
        val f1 = DockerDemuxParser.buildFrame(LogStreamType.Stdout, "Server starting on port 8080")
        val f2 = DockerDemuxParser.buildFrame(LogStreamType.Stderr, "Warning: deprecated TLS cipher detected")
        val f3 = DockerDemuxParser.buildFrame(LogStreamType.Stdout, "Database connection pool established")

        val combined = f1 + f2 + f3
        val results = DockerDemuxParser.demuxStream(combined)

        assertEquals(3, results.size)
        assertEquals(LogStreamType.Stdout, results[0].streamType)
        assertEquals("Server starting on port 8080", results[0].text)

        assertEquals(LogStreamType.Stderr, results[1].streamType)
        assertEquals("Warning: deprecated TLS cipher detected", results[1].text)

        assertEquals(LogStreamType.Stdout, results[2].streamType)
        assertEquals("Database connection pool established", results[2].text)
    }

    @Test
    fun demuxStream_fallbackForTtyMode() {
        val rawTtyText = "root@container:/# ls -la\ntotal 64\ndrwxr-xr-x 1 root root 4096 Oct 22 10:00 .\n"
        val results = DockerDemuxParser.demuxStream(rawTtyText.toByteArray(Charsets.UTF_8))

        assertEquals(3, results.size)
        assertTrue(results.all { it.streamType == LogStreamType.Stdout })
        assertEquals("root@container:/# ls -la", results[0].text)
        assertEquals("total 64", results[1].text)
    }

    @Test
    fun demuxStream_emptyStream() {
        val results = DockerDemuxParser.demuxStream(ByteArray(0))
        assertTrue(results.isEmpty())
    }

    @Test
    fun demuxAccumulator_handlesChunkFragmentation() {
        val f1 = DockerDemuxParser.buildFrame(LogStreamType.Stdout, "Chunked message line 1")
        val f2 = DockerDemuxParser.buildFrame(LogStreamType.Stderr, "Chunked error line 2")
        val combined = f1 + f2

        val accumulator = DockerDemuxAccumulator(isTty = false)
        // Split right down the middle across frame boundaries
        val mid = combined.size / 2
        val chunk1 = combined.copyOfRange(0, mid)
        val chunk2 = combined.copyOfRange(mid, combined.size)

        val r1 = accumulator.feed(chunk1)
        val r2 = accumulator.feed(chunk2)
        val total = r1 + r2

        assertEquals(2, total.size)
        assertEquals("Chunked message line 1", total[0].text)
        assertEquals(LogStreamType.Stdout, total[0].streamType)
        assertEquals("Chunked error line 2", total[1].text)
        assertEquals(LogStreamType.Stderr, total[1].streamType)
    }
}
