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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.ui.components.presets.AppCard
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

        // Pocket Header Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            isGlow = true
        ) {
            Text(
                text = "Pocket Tool Belt",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ElectricSlateTokens.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Zero-setup diagnostic tools ready for instant action. Operates 100% offline without remote cloud dependency.",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted
            )
        }

        Text(
            text = "Diagnostic Tool Suite",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "ICMP Ping & Jitter",
            subtitle = "Measure round-trip latency, packet loss, and jitter to any target",
            leadingIcon = Icons.Outlined.NetworkPing,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("ping") }
        )

        AppListItem(
            title = "DNS Deep Resolver",
            subtitle = "Query A, AAAA, MX, TXT, SRV, SOA records via standard, DoH, and DoT",
            leadingIcon = Icons.Outlined.Dns,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("dns") }
        )

        AppListItem(
            title = "CIDR & Subnet Calculator",
            subtitle = "Calculate IPv4/IPv6 netmasks, wildcard bits, and usable IP boundaries",
            leadingIcon = Icons.Outlined.Calculate,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("subnet") }
        )

        AppListItem(
            title = "Wi-Fi 6E/7 Live Inspector",
            subtitle = "Monitor unthrottled 1Hz RSSI signal graphs and channel interference",
            leadingIcon = Icons.Outlined.Wifi,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("wifi") }
        )

        AppListItem(
            title = "SSL / TLS Certificate Inspector",
            subtitle = "Verify expiration dates, certificate chains, and cipher negotiation",
            leadingIcon = Icons.Outlined.Security,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("ssl") }
        )

        AppListItem(
            title = "Wake-on-LAN Magic Packet",
            subtitle = "Dispatch UDP magic packets across local subnets or directed IP addresses",
            leadingIcon = Icons.Outlined.Power,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { onToolSelected("wol") }
        )

        BottomClearanceSpacer()
    }
}
