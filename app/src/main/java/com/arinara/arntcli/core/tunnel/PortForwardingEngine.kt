// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.tunnel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Port Forwarding Engine responsible for local port availability checking,
 * rule lifecycle orchestration, and traffic metrics accounting.
 */
class PortForwardingEngine {

    private val _rules = MutableStateFlow<List<TunnelRule>>(emptyList())
    val rules: StateFlow<List<TunnelRule>> = _rules.asStateFlow()

    companion object {
        /**
         * Checks whether a TCP port on localhost is available for loopback binding.
         */
        suspend fun isPortAvailable(port: Int): Boolean = withContext(Dispatchers.IO) {
            try {
                ServerSocket(port, 50, InetAddress.getByName("127.0.0.1")).use { true }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun addRule(rule: TunnelRule): Boolean {
        var added = false
        _rules.update { current ->
            if (current.any { it.id == rule.id }) {
                current
            } else {
                added = true
                current + rule
            }
        }
        return added
    }

    fun removeRule(ruleId: String) {
        _rules.update { current -> current.filterNot { it.id == ruleId } }
    }

    fun updateRule(updated: TunnelRule) {
        _rules.update { current -> current.map { if (it.id == updated.id) updated else it } }
    }

    /**
     * Toggles tunnel active state with local port availability checks.
     */
    suspend fun toggleTunnel(ruleId: String): TunnelRule = withContext(Dispatchers.IO) {
        val currentRule = _rules.value.firstOrNull { it.id == ruleId }
            ?: throw IllegalArgumentException("Tunnel rule $ruleId not found")

        val newRule = if (currentRule.status == TunnelStatus.Active) {
            // Stop tunnel
            currentRule.copy(
                status = TunnelStatus.Inactive,
                errorMessage = null
            )
        } else {
            // Validate port availability before activating
            val available = isPortAvailable(currentRule.localPort)
            if (!available) {
                currentRule.copy(
                    status = TunnelStatus.Error,
                    errorMessage = "Local port ${currentRule.localPort} is already in use"
                )
            } else {
                currentRule.copy(
                    status = TunnelStatus.Active,
                    errorMessage = null
                )
            }
        }

        updateRule(newRule)
        newRule
    }
}
