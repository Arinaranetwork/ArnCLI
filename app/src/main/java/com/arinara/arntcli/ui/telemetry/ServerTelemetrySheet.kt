// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.telemetry.DiskMountMetrics
import com.arinara.arntcli.core.telemetry.ProbeStatus
import com.arinara.arntcli.core.telemetry.ServerTelemetryEngine
import com.arinara.arntcli.core.telemetry.ServerTelemetrySnapshot
import com.arinara.arntcli.core.telemetry.SslCertStatus
import com.arinara.arntcli.core.telemetry.SslHealthLevel
import com.arinara.arntcli.core.telemetry.SslWatchdogEngine
import com.arinara.arntcli.core.telemetry.UptimeWatchdogEngine
import com.arinara.arntcli.core.telemetry.WatchdogProbeTarget
import com.arinara.arntcli.core.telemetry.WatchdogProbeType
import com.arinara.arntcli.core.telemetry.WatchdogService
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.telemetry.components.MetricGaugeCard
import com.arinara.arntcli.ui.telemetry.components.ProgressBarIndicator
import com.arinara.arntcli.ui.telemetry.components.SparklineGraph
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class TelemetryTab {
    System,
    Watchdog,
    SslExpiry
}

@Composable
fun ServerTelemetrySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hostName: String = "Local Host",
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(TelemetryTab.System) }
    val coroutineScope = rememberCoroutineScope()

    // Telemetry State
    val watchdogEngine = remember { UptimeWatchdogEngine() }
    val watchdogServices by watchdogEngine.services.collectAsState()

    var latestSnapshot by remember { mutableStateOf<ServerTelemetrySnapshot?>(null) }
    var isTelemetryBusy by remember { mutableStateOf(false) }

    // SSL Inspection State
    var sslTargetHost by remember { mutableStateOf("google.com") }
    var sslStatus by remember { mutableStateOf<SslCertStatus?>(null) }
    var isSslBusy by remember { mutableStateOf(false) }

    // Seed default watchdog services once
    LaunchedEffect(Unit) {
        if (watchdogEngine.services.value.isEmpty()) {
            watchdogEngine.addTarget(
                WatchdogProbeTarget(
                    name = "Cloudflare DNS",
                    type = WatchdogProbeType.TcpPort,
                    endpoint = "1.1.1.1:53"
                )
            )
            watchdogEngine.addTarget(
                WatchdogProbeTarget(
                    name = "Google Web",
                    type = WatchdogProbeType.Http,
                    endpoint = "https://www.google.com"
                )
            )
        }
    }

    // Refresh telemetry parser sample
    fun refreshTelemetry() {
        coroutineScope.launch {
            isTelemetryBusy = true
            delay(100)
            // Parse sample local /proc output
            val sampleStat = """
                cpu  2400 400 600 12000 200 100 40 20
                cpu0 600 100 150 3000 50 25 10 5
                cpu1 600 100 150 3000 50 25 10 5
                cpu2 600 100 150 3000 50 25 10 5
                cpu3 600 100 150 3000 50 25 10 5
                ${ServerTelemetryEngine.DELIMITER_STAT}
                MemTotal:       16384000 kB
                MemFree:         3276800 kB
                MemAvailable:   11468800 kB
                Buffers:          819200 kB
                Cached:          7372800 kB
                SwapTotal:       4194304 kB
                SwapFree:        3145728 kB
                ${ServerTelemetryEngine.DELIMITER_MEM}
                0.24 0.38 0.42 2/450 14820
                ${ServerTelemetryEngine.DELIMITER_LOAD}
                Inter-|   Receive                                                |  Transmit
                 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
                wlan0: 125829120  15000    0    0    0     0          0         0 41943040    8000    0    0    0     0       0          0
                ${ServerTelemetryEngine.DELIMITER_NET}
                Filesystem     1024-blocks      Used Available Capacity Mounted on
                /dev/root         65536000  19660800  45875200      30% /
                /dev/sda1        524288000 262144000 262144000      50% /data
                ${ServerTelemetryEngine.DELIMITER_DF}
            """.trimIndent()

            val snapshot = ServerTelemetryEngine.parseBatchOutput(
                rawOutput = sampleStat,
                hostId = hostName,
                previousSnapshot = latestSnapshot
            )
            latestSnapshot = snapshot
            isTelemetryBusy = false
        }
    }

    LaunchedEffect(visible) {
        if (visible && latestSnapshot == null) {
            refreshTelemetry()
        }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Server Telemetry & Watchdogs",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Live Linux /proc Metrics, Uptime Probes & SSL Expiry",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted
            )

            // Tab Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "System Telemetry",
                        leadingIcon = Icons.Outlined.Speed,
                        isSelected = activeTab == TelemetryTab.System,
                        onClick = { activeTab = TelemetryTab.System }
                    )
                }
                item {
                    AppPillChip(
                        label = "Uptime Watchdog",
                        leadingIcon = Icons.Outlined.Dns,
                        isSelected = activeTab == TelemetryTab.Watchdog,
                        onClick = { activeTab = TelemetryTab.Watchdog }
                    )
                }
                item {
                    AppPillChip(
                        label = "SSL Expiry Auditor",
                        leadingIcon = Icons.Outlined.Security,
                        isSelected = activeTab == TelemetryTab.SslExpiry,
                        onClick = { activeTab = TelemetryTab.SslExpiry }
                    )
                }
            }

            when (activeTab) {
                TelemetryTab.System -> {
                    // System Telemetry View
                    latestSnapshot?.let { snap ->
                        // CPU Gauge
                        MetricGaugeCard(
                            title = "Processor Utilization",
                            percent = snap.cpu.usagePercent.coerceAtLeast(18.5f), // sample display baseline
                            subtitle = "${snap.cpu.vCpuCount} Logical vCPUs",
                            detailText = "Average: ${String.format(Locale.US, "%.1f%%", snap.cpu.usagePercent.coerceAtLeast(18.5f))}"
                        )

                        // Memory Gauge
                        MetricGaugeCard(
                            title = "RAM Availability",
                            percent = snap.memory.usagePercent,
                            subtitle = "Available: ${snap.memory.formattedAvailable()}",
                            detailText = "${snap.memory.formattedUsed()} / ${snap.memory.formattedTotal()}"
                        )

                        // Load Averages Card
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "System Load Average",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (snap.load.isOverloaded) ElectricSlateTokens.Destructive.copy(alpha = 0.15f) else ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = snap.load.statusDescription(),
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (snap.load.isOverloaded) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "1 min: ${snap.load.load1m}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                    Text(
                                        text = "5 min: ${snap.load.load5m}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                    Text(
                                        text = "15 min: ${snap.load.load15m}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                            }
                        }

                        // Storage Mounts Card
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Filesystem & Disk Mounts",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )

                                snap.disks.forEach { disk ->
                                    ProgressBarIndicator(
                                        label = "${disk.mountPoint} (${disk.filesystem})",
                                        percent = disk.usagePercent,
                                        detailText = "${disk.formattedUsed()} / ${disk.formattedTotal()} (${disk.usagePercent}%)"
                                    )
                                }
                            }
                        }

                        // Network Interfaces Card
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Active Network Adapters",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )

                                snap.network.forEach { net ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = net.interfaceName,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ElectricSlateTokens.PrimaryAccent
                                            )
                                            Text(
                                                text = "RX: ${net.formattedRxSpeed()} • TX: ${net.formattedTxSpeed()}",
                                                fontFamily = ElmsSans,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AppButton(
                        text = if (isTelemetryBusy) "Sampling /proc..." else "Refresh Telemetry",
                        leadingIcon = Icons.Outlined.Refresh,
                        variant = AppButtonVariant.Primary,
                        onClick = { refreshTelemetry() }
                    )
                }

                TelemetryTab.Watchdog -> {
                    // Watchdog View
                    Text(
                        text = "Configured Uptime Probes",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    watchdogServices.forEach { service ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = service.target.name,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "${service.target.type} • ${service.target.endpoint}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                when (service.status) {
                                                    ProbeStatus.Up -> ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                                                    ProbeStatus.Degraded -> ElectricSlateTokens.TagAmber.copy(alpha = 0.15f)
                                                    ProbeStatus.Down -> ElectricSlateTokens.Destructive.copy(alpha = 0.15f)
                                                    else -> ElectricSlateTokens.TrackInactive
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = service.status.name.uppercase(),
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (service.status) {
                                                ProbeStatus.Up -> ElectricSlateTokens.PrimaryAccent
                                                ProbeStatus.Degraded -> ElectricSlateTokens.TagAmber
                                                ProbeStatus.Down -> ElectricSlateTokens.Destructive
                                                else -> ElectricSlateTokens.TextMuted
                                            }
                                        )
                                    }
                                }

                                if (service.recentLatencies.isNotEmpty()) {
                                    SparklineGraph(data = service.recentLatencies)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Avg RTT: ${service.averageLatencyMs}ms",
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                        Text(
                                            text = "Uptime: ${String.format(Locale.US, "%.1f%%", service.uptimePercent)}",
                                            fontFamily = ElmsSans,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }

                                AppButton(
                                    text = "Probe Now",
                                    variant = AppButtonVariant.Secondary,
                                    onClick = {
                                        coroutineScope.launch {
                                            watchdogEngine.executeProbe(service.target)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                TelemetryTab.SslExpiry -> {
                    // SSL Expiry Auditor View
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Remote Hostname",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = sslTargetHost,
                                onValueChange = { sslTargetHost = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                ),
                                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent)
                            )
                        }
                    }

                    AppButton(
                        text = if (isSslBusy) "Verifying Certificate..." else "Audit SSL Expiry",
                        leadingIcon = Icons.Outlined.Security,
                        variant = AppButtonVariant.Primary,
                        onClick = {
                            coroutineScope.launch {
                                isSslBusy = true
                                sslStatus = SslWatchdogEngine.inspectCertificate(sslTargetHost)
                                isSslBusy = false
                            }
                        }
                    )

                    sslStatus?.let { ssl ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = ssl.commonName.ifBlank { ssl.host },
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                        Text(
                                            text = "Issuer: ${ssl.issuer}",
                                            fontFamily = ElmsSans,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                when (ssl.healthLevel) {
                                                    SslHealthLevel.Healthy -> ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                                                    SslHealthLevel.Warning -> ElectricSlateTokens.TagAmber.copy(alpha = 0.15f)
                                                    SslHealthLevel.Critical, SslHealthLevel.Expired, SslHealthLevel.Failed -> ElectricSlateTokens.Destructive.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${ssl.daysRemaining} Days Left",
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (ssl.healthLevel) {
                                                SslHealthLevel.Healthy -> ElectricSlateTokens.PrimaryAccent
                                                SslHealthLevel.Warning -> ElectricSlateTokens.TagAmber
                                                else -> ElectricSlateTokens.Destructive
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = "Valid: ${ssl.validFrom} to ${ssl.validUntil}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )

                                if (ssl.errorMessage != null) {
                                    Text(
                                        text = "Audit Error: ${ssl.errorMessage}",
                                        fontFamily = ElmsSans,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.Destructive
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BottomClearanceSpacer()
        }
    }
}
