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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.arinara.arntcli.core.net.RouteHop
import com.arinara.arntcli.core.net.TracerouteEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TracerouteSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("1.1.1.1", "8.8.8.8", "google.com", "cloudflare.com")
    val hopLimits = listOf(10, 15, 20, 30)

    var targetHost by remember { mutableStateOf("1.1.1.1") }
    var maxHops by remember { mutableIntStateOf(15) }
    var isTracing by remember { mutableStateOf(false) }
    var hops by remember { mutableStateOf(emptyList<RouteHop>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var traceJob by remember { mutableStateOf<Job?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val engine = remember { TracerouteEngine() }

    fun stopTrace() {
        traceJob?.cancel()
        traceJob = null
        isTracing = false
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = {
            stopTrace()
            onDismiss()
        },
        title = "Route Traceroute",
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

            // Target Host Input
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
                    onValueChange = {
                        targetHost = it
                        errorMessage = null
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    ),
                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Presets
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    AppPillChip(
                        label = preset,
                        isSelected = targetHost == preset,
                        onClick = {
                            targetHost = preset
                            errorMessage = null
                        }
                    )
                }
            }

            Text(
                text = "Max Hops Limit",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            // Max Hops Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hopLimits) { limit ->
                    AppPillChip(
                        label = "$limit hops",
                        isSelected = maxHops == limit,
                        onClick = { maxHops = limit }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Start/Stop Button
            if (isTracing) {
                AppButton(
                    text = "Stop Traceroute",
                    leadingIcon = Icons.Outlined.Stop,
                    variant = AppButtonVariant.Secondary,
                    onClick = { stopTrace() }
                )
            } else {
                AppButton(
                    text = "Start Traceroute",
                    leadingIcon = Icons.Outlined.PlayArrow,
                    variant = AppButtonVariant.Primary,
                    onClick = {
                        val clean = targetHost.trim()
                        if (clean.isBlank()) {
                            errorMessage = "Target host cannot be blank"
                            return@AppButton
                        }
                        hops = emptyList()
                        isTracing = true
                        errorMessage = null

                        traceJob = coroutineScope.launch {
                            try {
                                engine.traceStreaming(clean, maxHops).collect { hop ->
                                    hops = hops + hop
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Traceroute failed"
                            } finally {
                                isTracing = false
                            }
                        }
                    }
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = ElectricSlateTokens.Destructive
                )
            }

            // Hop Route Ladder
            if (hops.isNotEmpty()) {
                Text(
                    text = "Route Hops (${hops.size}/$maxHops)",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = false
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hops.forEach { hop ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (hop.isDestination) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Hop Number Badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hop.isDestination) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${hop.hopNumber}",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    )
                                }

                                // IP and Hostname
                                Column(modifier = Modifier.weight(1f)) {
                                    val ipText = when {
                                        hop.isTimedOut -> "* * *"
                                        hop.ipAddress != null -> hop.ipAddress
                                        else -> "Unknown hop"
                                    }

                                    Text(
                                        text = ipText,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (hop.isTimedOut) ElectricSlateTokens.TextMuted else ElectricSlateTokens.TextPrimary
                                    )

                                    if (!hop.hostname.isNullOrBlank() && hop.hostname != hop.ipAddress) {
                                        Text(
                                            text = hop.hostname,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                }

                                // Latency or Status Pill
                                if (hop.isDestination) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Destination Reached",
                                            tint = ElectricSlateTokens.PrimaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        if (hop.rttMs != null) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f ms", hop.rttMs),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.PrimaryAccent
                                            )
                                        }
                                    }
                                } else if (hop.isTimedOut) {
                                    Text(
                                        text = "Timed out",
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                } else if (hop.rttMs != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(ElectricSlateTokens.SurfacePanel)
                                            .border(
                                                width = 1.dp,
                                                color = ElectricSlateTokens.BorderSubtle,
                                                shape = RoundedCornerShape(999.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f ms", hop.rttMs),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
