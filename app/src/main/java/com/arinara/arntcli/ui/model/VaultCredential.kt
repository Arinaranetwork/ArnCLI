// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.model

/**
 * Supported cryptographic credential categories stored in the Vault.
 */
enum class CredentialType {
    SshKey,
    WireGuardConfig,
    Password,
    ApiToken
}

/**
 * Data model representing a credential entry in the encrypted Vault.
 *
 * @param id Unique identifier.
 * @param title Credential label (e.g. "id_ed25519", "wireguard-home").
 * @param type Category of the stored secret.
 * @param description Informational metadata or key fingerprint.
 * @param spaceId Identifier of the Space owning this credential.
 * @param createdAt Creation timestamp in milliseconds.
 */
data class VaultCredential(
    val id: String,
    val title: String,
    val type: CredentialType,
    val description: String,
    val spaceId: String,
    val createdAt: Long = System.currentTimeMillis()
)
