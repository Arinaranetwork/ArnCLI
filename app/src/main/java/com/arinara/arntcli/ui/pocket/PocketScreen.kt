// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.pocket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

/**
 * PocketScreen - Instant zero-setup diagnostic tools.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun PocketScreen(
    contentPadding: PaddingValues,
    onToolSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Diagnostic Tools",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "ICMP Ping & Jitter",
            subtitle = "Measure latency, packet loss, and jitter",
            leadingIcon = Icons.Outlined.NetworkPing,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("ping") }
        )

        AppListItem(
            title = "DNS Resolver",
            subtitle = "Query A, AAAA, MX, TXT, SRV, SOA records",
            leadingIcon = Icons.Outlined.Dns,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("dns") }
        )

        AppListItem(
            title = "CIDR & Subnet Calculator",
            subtitle = "Calculate netmasks, broadcast, and usable IP ranges",
            leadingIcon = Icons.Outlined.Calculate,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("subnet") }
        )

        AppListItem(
            title = "Wi-Fi Inspector",
            subtitle = "Signal strength graphs and channel interference",
            leadingIcon = Icons.Outlined.Wifi,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("wifi") }
        )

        AppListItem(
            title = "SSL / TLS Inspector",
            subtitle = "Certificate chains, expiration, and cipher negotiation",
            leadingIcon = Icons.Outlined.Security,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("ssl") }
        )

        AppListItem(
            title = "Wake-on-LAN",
            subtitle = "Send UDP magic packets to target MAC",
            leadingIcon = Icons.Outlined.Power,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("wol") }
        )

        AppListItem(
            title = "TCP Port Scanner",
            subtitle = "Probe service reachability and port latency",
            leadingIcon = Icons.Outlined.Search,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("portscan") }
        )

        AppListItem(
            title = "LAN Host Discovery",
            subtitle = "Sweep local subnet and discover active devices",
            leadingIcon = Icons.Outlined.Dns,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("discovery") }
        )

        AppListItem(
            title = "Route Traceroute",
            subtitle = "Trace network hops and hop-by-hop latency",
            leadingIcon = Icons.Outlined.AltRoute,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("traceroute") }
        )

        Text(
            text = "Developer & Protocol Lab",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "Protocol & API Lab",
            subtitle = "REST/GraphQL, WebSocket, MQTT broker & BLE GATT",
            leadingIcon = Icons.Outlined.DeveloperMode,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("devlab") }
        )

        AppListItem(
            title = "Server Health & Watchdogs",
            subtitle = "Agentless Linux /proc telemetry, uptime probes & SSL expiry",
            leadingIcon = Icons.Outlined.Speed,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("telemetry") }
        )

        AppListItem(
            title = "Containers & Kubernetes",
            subtitle = "Docker Remote API, Podman libpod & Kube pods explorer",
            leadingIcon = Icons.Outlined.Layers,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("containers") }
        )

        BottomClearanceSpacer()
    }
}
