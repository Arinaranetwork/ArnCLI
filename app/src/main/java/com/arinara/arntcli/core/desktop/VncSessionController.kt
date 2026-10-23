// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

enum class DesktopConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Error
}

enum class MouseButton(val mask: Int) {
    None(0),
    Left(1),
    Middle(2),
    Right(4),
    WheelUp(8),
    WheelDown(16)
}

/**
 * Controller managing low-level pointer kinematics, modifier keys, and frame state for VNC/RDP.
 */
class VncSessionController(
    val serverWidth: Int = 1920,
    val serverHeight: Int = 1080,
    var sensitivity: Float = 1.0f
) {

    private val _status = MutableStateFlow(DesktopConnectionStatus.Disconnected)
    val status: StateFlow<DesktopConnectionStatus> = _status.asStateFlow()

    private val _pointerX = MutableStateFlow(serverWidth / 2f)
    val pointerX: StateFlow<Float> = _pointerX.asStateFlow()

    private val _pointerY = MutableStateFlow(serverHeight / 2f)
    val pointerY: StateFlow<Float> = _pointerY.asStateFlow()

    private val _activeModifiers = MutableStateFlow<Set<String>>(emptySet())
    val activeModifiers: StateFlow<Set<String>> = _activeModifiers.asStateFlow()

    private val _lastAction = MutableStateFlow("Session Ready")
    val lastAction: StateFlow<String> = _lastAction.asStateFlow()

    suspend fun connect(host: String = "127.0.0.1", port: Int = 5900): Boolean = withContext(Dispatchers.IO) {
        _status.value = DesktopConnectionStatus.Connecting
        _lastAction.value = "Attempting RFB socket connection to $host:$port..."
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1200)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val version = reader.readLine() ?: ""
                if (version.startsWith("RFB")) {
                    _status.value = DesktopConnectionStatus.Connected
                    _lastAction.value = "Connected to $version (${serverWidth}x${serverHeight})"
                    return@withContext true
                }
            }
            _status.value = DesktopConnectionStatus.Error
            _lastAction.value = "RFB handshake failed on $host:$port"
            false
        } catch (e: Exception) {
            _status.value = DesktopConnectionStatus.Error
            _lastAction.value = "Unable to connect to $host:$port. Ensure VNC/RDP server or tunnel is active."
            false
        }
    }

    fun disconnect() {
        _status.value = DesktopConnectionStatus.Disconnected
        _lastAction.value = "Session disconnected"
    }

    /**
     * Updates pointer position using relative touch displacement delta.
     */
    fun movePointerDelta(deltaX: Float, deltaY: Float) {
        val newX = (_pointerX.value + deltaX * sensitivity).coerceIn(0f, serverWidth.toFloat())
        val newY = (_pointerY.value + deltaY * sensitivity).coerceIn(0f, serverHeight.toFloat())
        _pointerX.value = newX
        _pointerY.value = newY
        _lastAction.value = "Pointer moved: (${newX.toInt()}, ${newY.toInt()})"
    }

    /**
     * Simulates mouse click event.
     */
    fun sendClick(button: MouseButton) {
        _lastAction.value = "Click: ${button.name} at (${_pointerX.value.toInt()}, ${_pointerY.value.toInt()})"
    }

    /**
     * Toggles modifier key state (e.g. CTRL, ALT, SUPER).
     */
    fun toggleModifier(mod: String) {
        _activeModifiers.update { current ->
            if (current.contains(mod)) current - mod else current + mod
        }
        _lastAction.value = "Modifier toggled: $mod"
    }

    /**
     * Sends virtual key press.
     */
    fun sendKey(keyName: String) {
        val mods = _activeModifiers.value.joinToString("+")
        val full = if (mods.isNotEmpty()) "$mods+$keyName" else keyName
        _lastAction.value = "Key sent: $full"
    }
}
