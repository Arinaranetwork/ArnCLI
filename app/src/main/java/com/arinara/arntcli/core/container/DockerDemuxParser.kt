// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import java.io.ByteArrayOutputStream

enum class LogStreamType {
    Stdout,
    Stderr,
    System,
    Stdin
}

data class DockerLogEntry(
    val streamType: LogStreamType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Docker Multiplexed Log Demuxing Parser.
 * Correctly decodes Docker Engine's 8-byte binary frame headers:
 * [stream_type (1 byte), 0, 0, 0 (3 bytes), size (4 bytes big-endian)],
 * separating stdout, stderr, and system error streams.
 * Gracefully falls back to plain text streaming when containers run in TTY mode.
 */
class DockerDemuxParser {

    companion object {
        const val HEADER_SIZE = 8
        const val MAX_FRAME_SIZE = 1_048_576 // 1MB clamp safety against memory exhaustion

        /**
         * Demuxes a byte buffer containing one or more Docker 8-byte frames or raw TTY lines.
         */
        fun demuxStream(bytes: ByteArray): List<DockerLogEntry> {
            if (bytes.isEmpty()) return emptyList()

            // Check if stream looks like Docker 8-byte multiplexed header:
            // Byte 0 must be 1, 2, or 3, and bytes 1, 2, 3 must be 0x00
            val isMultiplexed = bytes.size >= HEADER_SIZE &&
                    (bytes[0] in 0x01..0x03) &&
                    bytes[1] == 0x00.toByte() &&
                    bytes[2] == 0x00.toByte() &&
                    bytes[3] == 0x00.toByte()

            if (!isMultiplexed) {
                // TTY mode fallback: split plain text by newline
                val rawText = String(bytes, Charsets.UTF_8)
                return rawText.lines().filter { it.isNotBlank() }.map { line ->
                    DockerLogEntry(
                        streamType = LogStreamType.Stdout,
                        text = line
                    )
                }
            }

            val entries = mutableListOf<DockerLogEntry>()
            var offset = 0

            while (offset + HEADER_SIZE <= bytes.size) {
                val streamByte = bytes[offset].toInt()
                val streamType = when (streamByte) {
                    1 -> LogStreamType.Stdout
                    2 -> LogStreamType.Stderr
                    3 -> LogStreamType.System
                    0 -> LogStreamType.Stdin
                    else -> LogStreamType.Stdout
                }

                // Decode big-endian 32-bit unsigned size with 1MB safety clamp
                val rawSize = ((bytes[offset + 4].toInt() and 0xFF) shl 24) or
                        ((bytes[offset + 5].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 6].toInt() and 0xFF) shl 8) or
                        (bytes[offset + 7].toInt() and 0xFF)
                val size = rawSize.coerceIn(0, MAX_FRAME_SIZE)

                offset += HEADER_SIZE

                if (size <= 0) continue

                val payloadEnd = (offset + size).coerceAtMost(bytes.size)
                val payloadBytes = bytes.copyOfRange(offset, payloadEnd)
                val payloadText = String(payloadBytes, Charsets.UTF_8).trimEnd('\n', '\r')

                if (payloadText.isNotEmpty()) {
                    entries.add(
                        DockerLogEntry(
                            streamType = streamType,
                            text = payloadText
                        )
                    )
                }

                offset += size
            }

            return entries
        }

        /**
         * Builds an 8-byte Docker frame for tests or outbound stdin injection.
         */
        fun buildFrame(streamType: LogStreamType, text: String): ByteArray {
            val payload = text.toByteArray(Charsets.UTF_8)
            val size = payload.size
            val header = ByteArray(HEADER_SIZE)

            header[0] = when (streamType) {
                LogStreamType.Stdin -> 0x00
                LogStreamType.Stdout -> 0x01
                LogStreamType.Stderr -> 0x02
                LogStreamType.System -> 0x03
            }
            header[1] = 0x00
            header[2] = 0x00
            header[3] = 0x00

            header[4] = ((size shr 24) and 0xFF).toByte()
            header[5] = ((size shr 16) and 0xFF).toByte()
            header[6] = ((size shr 8) and 0xFF).toByte()
            header[7] = (size and 0xFF).toByte()

            return header + payload
        }
    }
}

/**
 * Stateful chunk accumulator for streaming Docker logs over TCP or forwarded UNIX sockets.
 * Handles split headers and payload fragmentation across network packets.
 */
class DockerDemuxAccumulator(
    private val isTty: Boolean = false,
    private val maxFrameSize: Int = DockerDemuxParser.MAX_FRAME_SIZE
) {
    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun feed(chunk: ByteArray): List<DockerLogEntry> {
        if (chunk.isEmpty()) return emptyList()
        if (isTty) {
            val text = String(chunk, Charsets.UTF_8)
            return text.lines().filter { it.isNotBlank() }.map { line ->
                DockerLogEntry(LogStreamType.Stdout, line)
            }
        }

        buffer.write(chunk)
        val currentBytes = buffer.toByteArray()
        val entries = mutableListOf<DockerLogEntry>()
        var offset = 0

        while (offset + DockerDemuxParser.HEADER_SIZE <= currentBytes.size) {
            val streamByte = currentBytes[offset].toInt()
            val streamType = when (streamByte) {
                1 -> LogStreamType.Stdout
                2 -> LogStreamType.Stderr
                3 -> LogStreamType.System
                0 -> LogStreamType.Stdin
                else -> LogStreamType.Stdout
            }

            val isPadded = currentBytes[offset + 1] == 0.toByte() &&
                    currentBytes[offset + 2] == 0.toByte() &&
                    currentBytes[offset + 3] == 0.toByte()

            if (!isPadded) {
                val raw = String(currentBytes, offset, currentBytes.size - offset, Charsets.UTF_8)
                entries.addAll(raw.lines().filter { it.isNotBlank() }.map { DockerLogEntry(LogStreamType.Stdout, it) })
                offset = currentBytes.size
                break
            }

            val rawSize = ((currentBytes[offset + 4].toInt() and 0xFF) shl 24) or
                    ((currentBytes[offset + 5].toInt() and 0xFF) shl 16) or
                    ((currentBytes[offset + 6].toInt() and 0xFF) shl 8) or
                    (currentBytes[offset + 7].toInt() and 0xFF)

            val size = rawSize.coerceIn(0, maxFrameSize)
            val totalFrameSize = DockerDemuxParser.HEADER_SIZE + size

            if (offset + totalFrameSize <= currentBytes.size) {
                val payloadStart = offset + DockerDemuxParser.HEADER_SIZE
                val payloadBytes = currentBytes.copyOfRange(payloadStart, payloadStart + size)
                val payloadText = String(payloadBytes, Charsets.UTF_8).trimEnd('\n', '\r')
                if (payloadText.isNotEmpty()) {
                    entries.add(DockerLogEntry(streamType, payloadText))
                }
                offset += totalFrameSize
            } else {
                break
            }
        }

        buffer.reset()
        if (offset < currentBytes.size) {
            buffer.write(currentBytes, offset, currentBytes.size - offset)
        }

        return entries
    }

    @Synchronized
    fun flush(): List<DockerLogEntry> {
        val remaining = buffer.toByteArray()
        buffer.reset()
        if (remaining.isEmpty()) return emptyList()
        val text = String(remaining, Charsets.UTF_8)
        return text.lines().filter { it.isNotBlank() }.map { DockerLogEntry(LogStreamType.Stdout, it) }
    }
}
