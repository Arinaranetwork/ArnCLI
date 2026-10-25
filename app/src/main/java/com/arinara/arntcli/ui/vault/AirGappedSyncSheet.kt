// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.vault

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.vault.BcUrSyncEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppNoticeBanner
import com.arinara.arntcli.ui.components.presets.AppNoticeVariant
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import kotlinx.coroutines.delay

enum class AirGappedSyncMode {
    Transmit,
    Receive
}

enum class CycleSpeed(val label: String, val intervalMs: Long) {
    Fast("Fast (150ms)", 150L),
    Standard("Normal (250ms)", 250L),
    Dense("Dense (400ms)", 400L)
}

/**
 * AirGappedSyncSheet - Multipart BC-UR fountain sync sheet for zero-network air-gapped data transfers.
 */
@Composable
fun AirGappedSyncSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    samplePayload: String = """{"vaultVersion":1,"exportedAt":1774218000,"credentials":[{"title":"Bastion Host","type":"SshKey"},{"title":"Home WG","type":"WireGuardConfig"}]}""",
    onImportPayload: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AirGappedSyncMode.Transmit) }
    val clipboardManager = LocalClipboardManager.current

    // Transmit State
    val syncEngine = remember { BcUrSyncEngine(chunkSizeBytes = 80) }
    val frames = remember(samplePayload) {
        try {
            syncEngine.encode(samplePayload.ifBlank { "Arinara Keyring Export" })
        } catch (_: Exception) {
            listOf("ur:bytes/1-1/00000000/empty")
        }
    }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(CycleSpeed.Standard) }
    var isCopied by remember { mutableStateOf(false) }

    // Automatic Frame Cycling (guarded by visible to prevent background battery drain)
    LaunchedEffect(visible, isPlaying, speed, frames.size) {
        if (visible && isPlaying && frames.isNotEmpty()) {
            while (true) {
                delay(speed.intervalMs)
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }
    }

    // Receive State
    val receiver = remember { BcUrSyncEngine.Receiver() }
    var inputFrameText by remember { mutableStateOf("") }
    var ingestedCount by remember { mutableIntStateOf(0) }
    var receiverProgress by remember { mutableStateOf(0f) }
    var isReceiverComplete by remember { mutableStateOf(false) }
    var assembledPayload by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Air-Gapped Vault Sync",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "BC-UR Multipart Optical Transfer",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                }

                AppButton(
                    text = "Close",
                    variant = AppButtonVariant.Secondary,
                    onClick = onDismiss
                )
            }

            // Mode Selector Pill Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppPillChip(
                    label = "Transmit (Fountain)",
                    isSelected = mode == AirGappedSyncMode.Transmit,
                    leadingIcon = Icons.Outlined.Upload,
                    onClick = { mode = AirGappedSyncMode.Transmit }
                )
                AppPillChip(
                    label = "Receive (Reassemble)",
                    isSelected = mode == AirGappedSyncMode.Receive,
                    leadingIcon = Icons.Outlined.Download,
                    onClick = { mode = AirGappedSyncMode.Receive }
                )
            }

            if (mode == AirGappedSyncMode.Transmit) {
                // TRANSMIT VIEW
                val currentFrame = frames.getOrElse(currentFrameIndex) { "" }

                // Optical Screen Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    isGlow = isPlaying
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // High-density QR simulation frame container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ElectricSlateTokens.BgCanvas)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = "Optical Stream",
                                    tint = ElectricSlateTokens.PrimaryAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "FRAME ${currentFrameIndex + 1} / ${frames.size}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = currentFrame.take(36) + "...",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Progress bar across frames
                        LinearProgressIndicator(
                            progress = {
                                if (frames.isNotEmpty()) {
                                    (currentFrameIndex + 1).toFloat() / frames.size.toFloat()
                                } else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = ElectricSlateTokens.PrimaryAccent,
                            trackColor = ElectricSlateTokens.TrackInactive
                        )

                        // Playback and Frame Stepper Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Previous
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable {
                                            isPlaying = false
                                            currentFrameIndex = (currentFrameIndex - 1 + frames.size) % frames.size
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SkipPrevious,
                                        contentDescription = "Previous Frame",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Play / Pause
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable { isPlaying = !isPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Next
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                                        .jigglePressClickable {
                                            isPlaying = false
                                            currentFrameIndex = (currentFrameIndex + 1) % frames.size
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SkipNext,
                                        contentDescription = "Next Frame",
                                        tint = ElectricSlateTokens.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Copy Frame URI Button
                            AppButton(
                                text = if (isCopied) "Copied" else "Copy URI",
                                leadingIcon = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                variant = AppButtonVariant.Secondary,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentFrame))
                                    isCopied = true
                                }
                            )
                        }
                    }
                }

                // Speed Selector
                Text(
                    text = "Transmission Speed",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CycleSpeed.values().forEach { s ->
                        AppPillChip(
                            label = s.label,
                            isSelected = speed == s,
                            onClick = { speed = s }
                        )
                    }
                }

                // Raw Frame URI Box
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Active Frame Uniform Resource (BC-UR)",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        Text(
                            text = currentFrame,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }

            } else {
                // RECEIVE VIEW
                AppNoticeBanner(
                    visible = errorMessage != null,
                    variant = AppNoticeVariant.ERROR,
                    message = errorMessage ?: "",
                    onDismiss = { errorMessage = null }
                )

                AppNoticeBanner(
                    visible = assembledPayload != null,
                    variant = AppNoticeVariant.SUCCESS,
                    message = "BC-UR stream verified! CRC32 checksum passed.",
                    onDismiss = { }
                )

                // Reception Progress Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Receiver Progress",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isReceiverComplete) "All frames received and ready" else "$ingestedCount frames ingested",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isReceiverComplete) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${(receiverProgress * 100).toInt()}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { receiverProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = ElectricSlateTokens.PrimaryAccent,
                            trackColor = ElectricSlateTokens.TrackInactive
                        )
                    }
                }

                // Frame Ingestion Input
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Ingest BC-UR Frame",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )

                        BasicTextField(
                            value = inputFrameText,
                            onValueChange = { inputFrameText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            ),
                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricSlateTokens.BgCanvas)
                                .border(
                                    width = 1.dp,
                                    color = ElectricSlateTokens.BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (inputFrameText.isEmpty()) {
                                        Text(
                                            text = "Paste ur:bytes/<seq>-<total>/<crc>/<payload>",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        AppButton(
                            text = "Submit Frame",
                            variant = AppButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val accepted = receiver.ingestFrame(inputFrameText)
                                if (accepted) {
                                    ingestedCount++
                                    receiverProgress = receiver.progress
                                    isReceiverComplete = receiver.isComplete
                                    inputFrameText = ""
                                    errorMessage = null
                                } else {
                                    errorMessage = "Malformed or invalid BC-UR frame rejected."
                                }
                            }
                        )
                    }
                }

                // Assembled Payload Section
                if (isReceiverComplete) {
                    AppButton(
                        text = "Assemble & Verify Payload",
                        leadingIcon = Icons.Outlined.Check,
                        variant = AppButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val result = receiver.assemble()
                            if (result.isSuccess) {
                                assembledPayload = result.getOrThrow()
                                errorMessage = null
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Reassembly failure"
                            }
                        }
                    )
                }

                if (assembledPayload != null) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Reconstructed Vault JSON",
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Text(
                                text = assembledPayload!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AppButton(
                                text = "Import to Vault Keyring",
                                variant = AppButtonVariant.Primary,
                                onClick = {
                                    onImportPayload(assembledPayload!!)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                // Reset Button
                AppButton(
                    text = "Reset Receiver",
                    leadingIcon = Icons.Outlined.Refresh,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        receiver.reset()
                        ingestedCount = 0
                        receiverProgress = 0f
                        isReceiverComplete = false
                        assembledPayload = null
                        errorMessage = null
                        inputFrameText = ""
                    }
                )
            }

            BottomClearanceSpacer()
        }
    }
}
