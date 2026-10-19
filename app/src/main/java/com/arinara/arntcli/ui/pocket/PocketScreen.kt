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
                text = "Zero-setup diagnostic tools ready for instant action. Accessible from widgets and Quick Settings tiles.",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted
            )
        }

        Text(
            text = "Active Probes & Diagnostics",
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = ElectricSlateTokens.TextPrimary
        )

        AppListItem(
            title = "ICMP Ping & Jitter",
            subtitle = "Target: 1.1.1.1 • Min: 12ms Avg: 15ms Jitter: 1.2ms",
            leadingIcon = Icons.Outlined.NetworkPing,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "DNS Deep Resolver",
            subtitle = "DoH & DoT • Query A, AAAA, MX, TXT, SRV, SOA records",
            leadingIcon = Icons.Outlined.Dns,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "CIDR & Subnet Calculator",
            subtitle = "IPv4 / IPv6 netmask, wildcard, and usable address boundaries",
            leadingIcon = Icons.Outlined.Calculate,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Wi-Fi 6E/7 Live Inspector",
            subtitle = "Continuous 1Hz RSSI walk-test graph & channel rating",
            leadingIcon = Icons.Outlined.Wifi,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "SSL / TLS Certificate Inspector",
            subtitle = "Verify expiry date, cipher suite, and certificate chains",
            leadingIcon = Icons.Outlined.Security,
            leadingIconTint = ElectricSlateTokens.PrimaryAccent,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        AppListItem(
            title = "Wake-on-LAN Magic Packet",
            subtitle = "Broadcast magic packets across LAN or directed subnet IP",
            leadingIcon = Icons.Outlined.Power,
            leadingIconTint = ElectricSlateTokens.TextMuted,
            action = AppListItemAction.Chevron,
            onClick = { }
        )

        BottomClearanceSpacer()
    }
}
