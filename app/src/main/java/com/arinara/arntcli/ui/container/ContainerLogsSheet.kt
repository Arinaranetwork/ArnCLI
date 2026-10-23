// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.container

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.container.DockerLogEntry
import com.arinara.arntcli.core.container.LogStreamType
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

enum class LogFilter {
    All,
    Stdout,
    Stderr
}

@Composable
fun ContainerLogsSheet(
    visible: Boolean,
    containerName: String,
    logs: List<DockerLogEntry>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(LogFilter.All) }
    var showClearConfirm by remember(visible) { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, activeFilter) {
        when (activeFilter) {
            LogFilter.All -> logs
            LogFilter.Stdout -> logs.filter { it.streamType == LogStreamType.Stdout }
            LogFilter.Stderr -> logs.filter { it.streamType == LogStreamType.Stderr }
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Logs: $containerName",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppPillChip(
                        label = "All (${logs.size})",
                        isSelected = activeFilter == LogFilter.All,
                        onClick = { activeFilter = LogFilter.All }
                    )
                    AppPillChip(
                        label = "Stdout",
                        isSelected = activeFilter == LogFilter.Stdout,
                        onClick = { activeFilter = LogFilter.Stdout }
                    )
                    AppPillChip(
                        label = "Stderr",
                        isSelected = activeFilter == LogFilter.Stderr,
                        onClick = { activeFilter = LogFilter.Stderr }
                    )
                }

                if (!showClearConfirm) {
                    AppButton(
                        text = "Clear",
                        variant = AppButtonVariant.Secondary,
                        onClick = { showClearConfirm = true }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppButton(
                            text = "Cancel",
                            variant = AppButtonVariant.Secondary,
                            onClick = { showClearConfirm = false }
                        )
                        AppButton(
                            text = "Confirm",
                            variant = AppButtonVariant.Destructive,
                            onClick = {
                                onClearLogs()
                                showClearConfirm = false
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.BgCanvas)
                    .border(
                        width = 1.dp,
                        color = ElectricSlateTokens.BorderSubtle,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "No log output recorded.",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val streamColor = when (entry.streamType) {
                                    LogStreamType.Stdout -> ElectricSlateTokens.TextPrimary
                                    LogStreamType.Stderr -> ElectricSlateTokens.Destructive
                                    else -> ElectricSlateTokens.TextMuted
                                }

                                Text(
                                    text = entry.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = streamColor
                                )
                            }
                        }
                    }
                }
            }

            BottomClearanceSpacer()
        }
    }
}
