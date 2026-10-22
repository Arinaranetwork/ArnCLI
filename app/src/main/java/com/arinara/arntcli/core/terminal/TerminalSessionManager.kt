// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import com.arinara.arntcli.ui.model.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for terminal session buffers and active states.
 * Backed by fixed 5,000-line circular buffers (CircularLogBuffer) to eliminate
 * memory pressure and GC pauses during high-volume logging.
 */
class TerminalSessionManager(
    private val bufferCapacity: Int = CircularLogBuffer.DEFAULT_CAPACITY
) {
    private val buffers = ConcurrentHashMap<String, CircularLogBuffer>()
    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    /**
     * Registers a new terminal session with its dedicated circular ring buffer.
     */
    fun registerSession(session: TerminalSession) {
        val buffer = CircularLogBuffer(bufferCapacity)
        buffer.appendAll(session.lines)
        buffers[session.id] = buffer

        val updated = _sessions.value.filterNot { it.id == session.id } + session.copy(lines = buffer.toList())
        _sessions.value = updated
    }

    /**
     * Appends an output line to the session's circular buffer and updates state.
     */
    fun appendOutput(sessionId: String, line: String) {
        val buffer = buffers[sessionId] ?: return
        buffer.append(line)

        val currentList = _sessions.value
        val index = currentList.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            val updated = currentList.toMutableList()
            updated[index] = updated[index].copy(lines = buffer.toList())
            _sessions.value = updated
        }
    }

    /**
     * Clears lines for a session.
     */
    fun clearSession(sessionId: String) {
        buffers[sessionId]?.clear()
        val currentList = _sessions.value
        val index = currentList.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            val updated = currentList.toMutableList()
            updated[index] = updated[index].copy(lines = emptyList())
            _sessions.value = updated
        }
    }

    /**
     * Closes and unregisters a session.
     */
    fun closeSession(sessionId: String) {
        buffers.remove(sessionId)?.clear()
        _sessions.value = _sessions.value.filterNot { it.id == sessionId }
    }

    /**
     * Returns an immutable snapshot of lines for a session.
     */
    fun getLines(sessionId: String): List<String> {
        return buffers[sessionId]?.toList() ?: emptyList()
    }
}
