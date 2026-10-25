// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

/**
 * Thread-safe fixed-capacity circular ring buffer for terminal lines.
 * Prevents memory bloat and Garbage Collection pauses during high-throughput
 * streaming commands (e.g. dmesg, docker logs -f, top).
 */
class CircularLogBuffer(
    val capacity: Int = DEFAULT_CAPACITY
) {
    init {
        require(capacity > 0) { "Capacity must be positive" }
    }

    private val lock = Any()
    private val buffer = arrayOfNulls<String>(capacity)
    private var head = 0
    private var count = 0

    val size: Int
        get() = synchronized(lock) { count }

    val isFull: Boolean
        get() = synchronized(lock) { count == capacity }

    /**
     * Appends a single line to the buffer, overwriting the oldest entry when full.
     */
    fun append(line: String) {
        synchronized(lock) {
            val insertIndex = (head + count) % capacity
            if (count < capacity) {
                buffer[insertIndex] = line
                count++
            } else {
                buffer[head] = line
                head = (head + 1) % capacity
            }
        }
    }

    /**
     * Appends multiple lines in batch.
     */
    fun appendAll(lines: Collection<String>) {
        if (lines.isEmpty()) return
        synchronized(lock) {
            for (line in lines) {
                val insertIndex = (head + count) % capacity
                if (count < capacity) {
                    buffer[insertIndex] = line
                    count++
                } else {
                    buffer[head] = line
                    head = (head + 1) % capacity
                }
            }
        }
    }

    /**
     * Clears all lines from the buffer.
     */
    fun clear() {
        synchronized(lock) {
            buffer.fill(null)
            head = 0
            count = 0
        }
    }

    /**
     * Returns an immutable ordered snapshot of current lines from oldest to newest.
     */
    fun toList(): List<String> {
        synchronized(lock) {
            if (count == 0) return emptyList()
            val result = ArrayList<String>(count)
            for (i in 0 until count) {
                val idx = (head + i) % capacity
                buffer[idx]?.let { result.add(it) }
            }
            return result
        }
    }

    companion object {
        const val DEFAULT_CAPACITY = 5_000
    }
}
