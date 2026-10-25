// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.model

/**
 * Data model representing a configured remote host endpoint.
 *
 * @param id Unique identifier of the host.
 * @param name Display label (e.g. "Bastion Server", "Production Gateway").
 * @param address IP address or hostname.
 * @param port Port number (defaults to 22).
 * @param username Authentication username.
 * @param protocol Connection protocol (SSH, Mosh, Telnet).
 * @param keyAlias Reference to stored Vault credential, if any.
 * @param spaceId Identifier of the Space owning this host.
 */
data class HostItem(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 22,
    val username: String,
    val protocol: String = "SSH",
    val keyAlias: String? = null,
    val jumpHost: String? = null,
    val spaceId: String
)
