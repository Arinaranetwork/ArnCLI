// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.model

/**
 * Data model representing an operational context Workspace (Space) in ArnTCLI.
 *
 * @param id Unique identifier of the Space.
 * @param name Display label (e.g. "Homelab", "Office Network", "Client Corp").
 * @param description Brief description of the operational boundary.
 * @param hostCount Total number of hosts registered in this Space.
 * @param tunnelCount Active tunnels associated with this Space.
 * @param isVpnActive Whether an automated VPN (WireGuard/OpenVPN) is engaged.
 * @param isSelected Whether this Space is currently active.
 */
data class Space(
    val id: String,
    val name: String,
    val description: String,
    val hostCount: Int,
    val tunnelCount: Int = 0,
    val isVpnActive: Boolean = false,
    val isSelected: Boolean = false
)
