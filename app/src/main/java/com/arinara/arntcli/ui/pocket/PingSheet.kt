// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.pocket

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.net.IcmpPingEngine
import com.arinara.arntcli.core.net.PingSample
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun PingSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetTargets = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "127.0.0.1")
    var targetHost by remember { mutableStateOf(presetTargets.first()) }
    var isPinging by remember { mutableStateOf(false) }

    val samples = remember { mutableStateListOf<PingSample>() }
    val pingEngine = remember { IcmpPingEngine() }
    val coroutineScope = rememberCoroutineScope()
    var pingJob by remember { mutableStateOf<Job?>(null) }

    val summary = remember(samples.size) {
        pingEngine.calculateSummary(samples)
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = {
            pingJob?.cancel()
            isPinging = false
            onDismiss()
        },
        title = "ICMP Ping & Jitter",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Target Host or IP",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Target Input Field
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
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {}),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Preset Target Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetTargets) { target ->
                    AppPillChip(
                        label = target,
                        isSelected = targetHost == target,
                        onClick = {
                            targetHost = target
                        }
                    )
                }
            }

            // Action Button: Start or Stop
            AppButton(
                text = if (isPinging) "Stop Diagnostic Ping" else "Start ICMP Ping",
                leadingIcon = if (isPinging) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                variant = if (isPinging) AppButtonVariant.Destructive else AppButtonVariant.Primary,
                onClick = {
                    if (isPinging) {
                        pingJob?.cancel()
                        isPinging = false
                    } else {
                        samples.clear()
                        isPinging = true
                        pingJob = coroutineScope.launch {
                            try {
                                pingEngine.startPing(targetHost, maxCount = 8).collect { sample ->
                                    samples.add(sample)
                                }
                            } finally {
                                isPinging = false
                            }
                        }
                    }
                }
            )

            if (samples.isNotEmpty()) {
                // Metric Badges Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latency Overview",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Loss: ${String.format(java.util.Locale.US, "%.0f", summary.packetLossPercent)}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (summary.packetLossPercent > 0) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricColumn(title = "Min", value = "${String.format(java.util.Locale.US, "%.1f", summary.minTimeMs)}ms")
                            MetricColumn(title = "Avg", value = "${String.format(java.util.Locale.US, "%.1f", summary.avgTimeMs)}ms")
                            MetricColumn(title = "Max", value = "${String.format(java.util.Locale.US, "%.1f", summary.maxTimeMs)}ms")
                            MetricColumn(title = "Jitter", value = "${String.format(java.util.Locale.US, "%.1f", summary.jitterMs)}ms")
                        }
                    }
                }

                Text(
                    text = "Probe Timeline (${samples.size} packets)",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                // Timeline of individual ping samples
                samples.takeLast(10).reversed().forEach { sample ->
                    AppListItem(
                        title = "seq=${sample.sequence} • ${sample.ip}",
                        subtitle = if (sample.isSuccess) "${sample.bytes} bytes • ${sample.timeMs}ms • ttl=${sample.ttl}" else (sample.errorMessage ?: "Timeout"),
                        leadingIcon = if (sample.isSuccess) Icons.Outlined.Check else Icons.Outlined.Close,
                        leadingIconTint = if (sample.isSuccess) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.Destructive,
                        action = AppListItemAction.Chevron
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String) {
    Column {
        Text(
            text = title,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = ElectricSlateTokens.TextMuted
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ElectricSlateTokens.TextPrimary
        )
    }
}
