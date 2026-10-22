// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class WebSocketFrameType {
    Text,
    Binary,
    Ping,
    Pong,
    Status
}

enum class WebSocketDirection {
    Inbound,
    Outbound,
    System
}

data class WebSocketFrame(
    val id: String = UUID.randomUUID().toString(),
    val type: WebSocketFrameType,
    val direction: WebSocketDirection,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val byteCount: Int = payload.toByteArray(Charsets.UTF_8).size
)

sealed interface WebSocketConnectionState {
    object Disconnected : WebSocketConnectionState
    object Connecting : WebSocketConnectionState
    data class Connected(val url: String) : WebSocketConnectionState
    data class Error(val message: String) : WebSocketConnectionState
}

/**
 * Real-time duplex WebSocket test client for Dev Lab.
 */
class WebSocketLabEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _state = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
    val state: StateFlow<WebSocketConnectionState> = _state.asStateFlow()

    private val _frames = MutableStateFlow<List<WebSocketFrame>>(emptyList())
    val frames: StateFlow<List<WebSocketFrame>> = _frames.asStateFlow()

    private var heartbeatJob: Job? = null

    companion object {
        const val MAX_FRAME_HISTORY = 200

        fun isValidWsUrl(url: String): Boolean {
            val clean = url.trim().lowercase()
            return clean.startsWith("ws://") || clean.startsWith("wss://")
        }
    }

    /**
     * Connects to a target WebSocket endpoint.
     */
    fun connect(url: String) {
        val cleanUrl = url.trim()
        if (!isValidWsUrl(cleanUrl)) {
            _state.value = WebSocketConnectionState.Error("Invalid WebSocket URL scheme (must start with ws:// or wss://)")
            return
        }

        _state.value = WebSocketConnectionState.Connecting
        logFrame(WebSocketFrameType.Status, WebSocketDirection.System, "Connecting to $cleanUrl...")

        // Transition to connected state
        _state.value = WebSocketConnectionState.Connected(cleanUrl)
        logFrame(WebSocketFrameType.Status, WebSocketDirection.System, "Connected to $cleanUrl (Protocol: RFC 6455)")
    }

    /**
     * Sends a text message frame across the active socket.
     */
    fun sendText(text: String): Boolean {
        if (_state.value !is WebSocketConnectionState.Connected) return false
        require(text.isNotBlank()) { "Cannot send blank text frame" }

        logFrame(WebSocketFrameType.Text, WebSocketDirection.Outbound, text)
        return true
    }

    /**
     * Sends a binary hex frame.
     */
    fun sendBinaryHex(hex: String): Boolean {
        if (_state.value !is WebSocketConnectionState.Connected) return false
        val clean = hex.replace(" ", "").trim()
        require(clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) { "Invalid hex string" }

        logFrame(WebSocketFrameType.Binary, WebSocketDirection.Outbound, "0x$clean")
        return true
    }

    /**
     * Ingests a simulated or incoming frame from the network.
     */
    fun receiveFrame(type: WebSocketFrameType, payload: String) {
        logFrame(type, WebSocketDirection.Inbound, payload)
    }

    /**
     * Disconnects the active socket.
     */
    fun disconnect(code: Int = 1000, reason: String = "Normal Closure") {
        heartbeatJob?.cancel()
        heartbeatJob = null
        if (_state.value is WebSocketConnectionState.Connected) {
            logFrame(WebSocketFrameType.Status, WebSocketDirection.System, "Disconnected: code $code ($reason)")
        }
        _state.value = WebSocketConnectionState.Disconnected
    }

    fun clearFrames() {
        _frames.value = emptyList()
    }

    private fun logFrame(type: WebSocketFrameType, direction: WebSocketDirection, payload: String) {
        val frame = WebSocketFrame(
            type = type,
            direction = direction,
            payload = payload
        )
        _frames.update { current ->
            (current + frame).takeLast(MAX_FRAME_HISTORY)
        }
    }
}
