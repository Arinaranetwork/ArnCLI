// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.wireguard

import java.util.Base64

/**
 * Robust parser for standard wg-quick WireGuard configuration (.conf) files.
 */
class WireGuardConfigParser {

    companion object {
        private const val WG_KEY_LENGTH_BYTES = 32

        /**
         * Validates whether a given key string is a valid 32-byte Base64-encoded WireGuard key.
         */
        fun isValidKey(key: String): Boolean {
            val clean = key.trim()
            if (clean.length !in 42..45) return false
            return try {
                val decoded = Base64.getDecoder().decode(clean)
                decoded.size == WG_KEY_LENGTH_BYTES
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Parses a standard WireGuard configuration text into a structured WireGuardConfig.
         */
        fun parse(name: String, confContent: String): Result<WireGuardConfig> = runCatching {
            require(name.isNotBlank()) { "Profile name cannot be blank" }

            var currentSection: String? = null
            var privateKey: String? = null
            val addresses = mutableListOf<String>()
            val dnsServers = mutableListOf<String>()
            var listenPort: Int? = null
            var mtu: Int? = null

            val peers = mutableListOf<WireGuardPeer>()
            var currentPeerPublicKey: String? = null
            var currentPeerEndpoint: String? = null
            var currentPeerPresharedKey: String? = null
            val currentPeerAllowedIps = mutableListOf<String>()
            var currentPeerKeepalive: Int? = null

            fun commitCurrentPeer() {
                if (currentPeerPublicKey != null) {
                    peers.add(
                        WireGuardPeer(
                            publicKey = currentPeerPublicKey!!,
                            endpoint = currentPeerEndpoint,
                            allowedIps = if (currentPeerAllowedIps.isNotEmpty()) currentPeerAllowedIps.toList() else listOf("0.0.0.0/0"),
                            persistentKeepalive = currentPeerKeepalive,
                            presharedKey = currentPeerPresharedKey
                        )
                    )
                    currentPeerPublicKey = null
                    currentPeerEndpoint = null
                    currentPeerPresharedKey = null
                    currentPeerAllowedIps.clear()
                    currentPeerKeepalive = null
                }
            }

            for (rawLine in confContent.lines()) {
                val line = rawLine.substringBefore('#').trim()
                if (line.isBlank()) continue

                if (line.startsWith("[") && line.endsWith("]")) {
                    val sectionName = line.substring(1, line.length - 1).trim()
                    if (currentSection.equals("Peer", ignoreCase = true)) {
                        commitCurrentPeer()
                    }
                    currentSection = sectionName
                    continue
                }

                if (!line.contains('=')) continue

                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim()

                when (currentSection?.lowercase()) {
                    "interface" -> {
                        when (key.lowercase()) {
                            "privatekey" -> privateKey = value
                            "address" -> addresses.addAll(value.split(',').map { it.trim() }.filter { it.isNotBlank() })
                            "dns" -> dnsServers.addAll(value.split(',').map { it.trim() }.filter { it.isNotBlank() })
                            "listenport" -> listenPort = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid ListenPort: '$value'")
                            "mtu" -> mtu = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid MTU: '$value'")
                        }
                    }
                    "peer" -> {
                        when (key.lowercase()) {
                            "publickey" -> currentPeerPublicKey = value
                            "endpoint" -> currentPeerEndpoint = value
                            "presharedkey" -> currentPeerPresharedKey = value
                            "allowedips" -> currentPeerAllowedIps.addAll(value.split(',').map { it.trim() }.filter { it.isNotBlank() })
                            "persistentkeepalive" -> currentPeerKeepalive = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid PersistentKeepalive: '$value'")
                        }
                    }
                }
            }

            commitCurrentPeer()

            requireNotNull(privateKey) { "Missing PrivateKey in [Interface] section" }
            require(addresses.isNotEmpty()) { "Missing Address in [Interface] section" }
            require(peers.isNotEmpty()) { "Profile must contain at least one valid [Peer] section" }

            val iface = WireGuardInterface(
                privateKey = privateKey,
                addresses = addresses,
                dnsServers = dnsServers,
                listenPort = listenPort,
                mtu = mtu
            )

            WireGuardConfig(
                name = name.trim(),
                iface = iface,
                peers = peers
            )
        }
    }
}
