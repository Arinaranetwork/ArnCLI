// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.desktop.DesktopConnectionStatus
import com.arinara.arntcli.core.desktop.MouseButton
import com.arinara.arntcli.core.desktop.VncSessionController
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans

/**
 * DesktopViewerSheet - Mobile VNC & RDP session controller with virtual trackpad and modifier bar.
 */
@Composable
fun DesktopViewerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hostLabel: String = "workstation.home.local:5900",
    modifier: Modifier = Modifier
) {
    val controller = remember { VncSessionController() }
    val status by controller.status.collectAsState()
    val pointerX by controller.pointerX.collectAsState()
    val pointerY by controller.pointerY.collectAsState()
    val activeModifiers by controller.activeModifiers.collectAsState()
    val lastAction by controller.lastAction.collectAsState()

    val modifierKeys = listOf("CTRL", "ALT", "SUPER", "SHIFT", "ESC", "TAB")

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Remote Desktop (VNC / RDP)",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hostLabel,
                        fontFamily = FontFamily.Monospace,
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

            // Connection State & Coordinates Beacon
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = status == DesktopConnectionStatus.Connected
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (status == DesktopConnectionStatus.Connected) ElectricSlateTokens.PrimaryAccent
                                    else ElectricSlateTokens.TextMuted
                                )
                        )
                        Text(
                            text = if (status == DesktopConnectionStatus.Connected) "Connected (1920x1080)" else "Disconnected",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }

                    AppButton(
                        text = if (status == DesktopConnectionStatus.Connected) "Disconnect" else "Connect",
                        variant = if (status == DesktopConnectionStatus.Connected) AppButtonVariant.Secondary else AppButtonVariant.Primary,
                        onClick = {
                            if (status == DesktopConnectionStatus.Connected) controller.disconnect()
                            else controller.connect()
                        }
                    )
                }
            }

            // Virtual Trackpad Surface (AR-002: Stable PointerInput Key `Unit`)
            Text(
                text = "Virtual Trackpad & Gesture Surface",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElectricSlateTokens.BgCanvas)
                    .border(
                        width = 1.dp,
                        color = ElectricSlateTokens.BorderSubtle,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            controller.movePointerDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mouse,
                        contentDescription = "Trackpad Surface",
                        tint = ElectricSlateTokens.PrimaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Touch & drag to pan pointer",
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                    Text(
                        text = "Pointer: (${pointerX.toInt()}, ${pointerY.toInt()})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                }
            }

            // Mouse Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppButton(
                    text = "Left Click",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { controller.sendClick(MouseButton.Left) }
                )
                AppButton(
                    text = "Middle Click",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { controller.sendClick(MouseButton.Middle) }
                )
                AppButton(
                    text = "Right Click",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { controller.sendClick(MouseButton.Right) }
                )
            }

            // Modifier Keys Bar
            Text(
                text = "Hardware Key Modifiers",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(modifierKeys) { key ->
                    AppPillChip(
                        label = key,
                        isSelected = activeModifiers.contains(key),
                        onClick = { controller.toggleModifier(key) }
                    )
                }
            }

            // Macro Shortcuts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = "Ctrl+Alt+Del",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        controller.toggleModifier("CTRL")
                        controller.toggleModifier("ALT")
                        controller.sendKey("DELETE")
                        controller.toggleModifier("CTRL")
                        controller.toggleModifier("ALT")
                    }
                )
                AppButton(
                    text = "Win+D",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        controller.toggleModifier("SUPER")
                        controller.sendKey("D")
                        controller.toggleModifier("SUPER")
                    }
                )
                AppButton(
                    text = "Alt+F4",
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        controller.toggleModifier("ALT")
                        controller.sendKey("F4")
                        controller.toggleModifier("ALT")
                    }
                )
            }

            // Event Audit Log Beacon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricSlateTokens.SurfacePanel)
                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Event: $lastAction",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ElectricSlateTokens.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
