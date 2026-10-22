// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.tunnel

/**
 * Standardized port forwarding tunnel modes.
 */
enum class TunnelType {
    /**
     * Local Port Forwarding (-L localPort:remoteHost:remotePort).
     * Binds a local port on Android, forwarding connections through the SSH server.
     */
    Local,

    /**
     * Remote Port Forwarding (-R remotePort:localHost:localPort).
     * Binds a remote port on the server, forwarding inbound connections back to Android.
     */
    Remote,

    /**
     * Dynamic SOCKS5 Proxy (-D localPort).
     * Creates a local SOCKS5 proxy server routing arbitrary application traffic through the SSH tunnel.
     */
    DynamicSocks5
}

/**
 * Lifecycle state of a port forwarding tunnel rule.
 */
enum class TunnelStatus {
    Inactive,
    Connecting,
    Active,
    Error
}

/**
 * Model representing a configured port forwarding tunnel rule.
 */
data class TunnelRule(
    val id: String,
    val name: String,
    val type: TunnelType,
    val localPort: Int,
    val remoteHost: String = "localhost",
    val remotePort: Int = 80,
    val hostId: String = "",
    val status: TunnelStatus = TunnelStatus.Inactive,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val errorMessage: String? = null
) {
    init {
        require(localPort in 1..65535) { "Local port must be in range 1..65535 (was $localPort)" }
        if (type != TunnelType.DynamicSocks5) {
            require(remotePort in 1..65535) { "Remote port must be in range 1..65535 (was $remotePort)" }
            require(remoteHost.isNotBlank()) { "Remote target host cannot be blank" }
        }
    }

    /**
     * Formats SSH flag representation (e.g. "-L 8080:localhost:80").
     */
    fun toCliSyntax(): String = when (type) {
        TunnelType.Local -> "-L $localPort:$remoteHost:$remotePort"
        TunnelType.Remote -> "-R $remotePort:$remoteHost:$localPort"
        TunnelType.DynamicSocks5 -> "-D $localPort"
    }

    /**
     * Human-readable summary of the tunnel routing endpoints.
     */
    fun toEndpointSummary(): String = when (type) {
        TunnelType.Local -> "127.0.0.1:$localPort → $remoteHost:$remotePort"
        TunnelType.Remote -> "remote:$remotePort → 127.0.0.1:$localPort"
        TunnelType.DynamicSocks5 -> "SOCKS5 Proxy on 127.0.0.1:$localPort"
    }
}
