// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularLogBufferTest {

    @Test
    fun buffer_appendsUnderCapacity() {
        val buffer = CircularLogBuffer(capacity = 5)
        buffer.append("line 1")
        buffer.append("line 2")
        buffer.append("line 3")

        assertEquals(3, buffer.size)
        assertFalse(buffer.isFull)
        assertEquals(listOf("line 1", "line 2", "line 3"), buffer.toList())
    }

    @Test
    fun buffer_overwritesOldestWhenCapacityExceeded() {
        val buffer = CircularLogBuffer(capacity = 3)
        buffer.append("line 1")
        buffer.append("line 2")
        buffer.append("line 3")
        assertTrue(buffer.isFull)

        buffer.append("line 4") // Overwrites line 1
        buffer.append("line 5") // Overwrites line 2

        assertEquals(3, buffer.size)
        assertTrue(buffer.isFull)
        assertEquals(listOf("line 3", "line 4", "line 5"), buffer.toList())
    }

    @Test
    fun buffer_appendAllBatchAndClear() {
        val buffer = CircularLogBuffer(capacity = 4)
        buffer.appendAll(listOf("a", "b", "c", "d", "e"))

        assertEquals(4, buffer.size)
        assertEquals(listOf("b", "c", "d", "e"), buffer.toList())

        buffer.clear()
        assertEquals(0, buffer.size)
        assertTrue(buffer.toList().isEmpty())
    }
}
