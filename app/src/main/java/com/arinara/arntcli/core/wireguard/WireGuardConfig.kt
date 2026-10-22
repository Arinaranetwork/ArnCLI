// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.wireguard

import java.util.Base64

/**
 * Interface parameters for a WireGuard tunnel.
 */
data class WireGuardInterface(
    val privateKey: String,
    val addresses: List<String>,
    val dnsServers: List<String> = emptyList(),
    val listenPort: Int? = null,
    val mtu: Int? = null
) {
    init {
        require(WireGuardConfigParser.isValidKey(privateKey)) { "Invalid WireGuard private key format" }
        require(addresses.isNotEmpty()) { "WireGuard interface must have at least one assigned address" }
        listenPort?.let {
            require(it in 1..65535) { "ListenPort must be in range 1..65535 (was $it)" }
        }
        mtu?.let {
            require(it in 576..9000) { "MTU must be in range 576..9000 (was $it)" }
        }
    }
}

/**
 * Peer parameters for a remote WireGuard endpoint.
 */
data class WireGuardPeer(
    val publicKey: String,
    val endpoint: String? = null,
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int? = 25,
    val presharedKey: String? = null
) {
    init {
        require(WireGuardConfigParser.isValidKey(publicKey)) { "Invalid WireGuard public key format" }
        presharedKey?.let {
            require(WireGuardConfigParser.isValidKey(it)) { "Invalid WireGuard preshared key format" }
        }
        persistentKeepalive?.let {
            require(it in 0..3600) { "PersistentKeepalive must be between 0 and 3600 seconds" }
        }
    }
}

/**
 * Complete WireGuard profile representation.
 */
data class WireGuardConfig(
    val name: String,
    val iface: WireGuardInterface,
    val peers: List<WireGuardPeer>
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(peers.isNotEmpty()) { "WireGuard profile must define at least one Peer" }
    }

    /**
     * Serializes this profile into standard wg-quick .conf file syntax.
     */
    fun toConfString(): String {
        val sb = StringBuilder()
        sb.append("[Interface]\n")
        sb.append("PrivateKey = ${iface.privateKey}\n")
        sb.append("Address = ${iface.addresses.joinToString(", ")}\n")
        if (iface.dnsServers.isNotEmpty()) {
            sb.append("DNS = ${iface.dnsServers.joinToString(", ")}\n")
        }
        iface.listenPort?.let { sb.append("ListenPort = $it\n") }
        iface.mtu?.let { sb.append("MTU = $it\n") }

        for (peer in peers) {
            sb.append("\n[Peer]\n")
            sb.append("PublicKey = ${peer.publicKey}\n")
            if (peer.presharedKey != null) {
                sb.append("PresharedKey = ${peer.presharedKey}\n")
            }
            if (peer.endpoint != null) {
                sb.append("Endpoint = ${peer.endpoint}\n")
            }
            sb.append("AllowedIPs = ${peer.allowedIps.joinToString(", ")}\n")
            if (peer.persistentKeepalive != null) {
                sb.append("PersistentKeepalive = ${peer.persistentKeepalive}\n")
            }
        }

        return sb.toString()
    }
}
