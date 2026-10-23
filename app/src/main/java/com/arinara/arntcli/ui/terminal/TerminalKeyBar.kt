// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.arinara.arntcli.core.terminal.TerminalAnsiKeyEngine
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable

/**
 * Interactive Terminal Modifier & Macro Key Bar.
 * Positioned above the soft keyboard with sticky Ctrl/Alt latches,
 * ANSI navigation arrows, and instant sysadmin macro pills.
 */
@Composable
fun TerminalKeyBar(
    onSendRawKey: (String) -> Unit,
    onInsertText: (String) -> Unit,
    onOpenAutomation: () -> Unit = {},
    ctrlLatchedExternal: Boolean? = null,
    onCtrlLatchedChange: ((Boolean) -> Unit)? = null,
    altLatchedExternal: Boolean? = null,
    onAltLatchedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var internalCtrlLatched by remember { mutableStateOf(false) }
    var internalAltLatched by remember { mutableStateOf(false) }

    val isCtrlLatched = ctrlLatchedExternal ?: internalCtrlLatched
    val isAltLatched = altLatchedExternal ?: internalAltLatched

    val setCtrlLatched: (Boolean) -> Unit = { newValue ->
        internalCtrlLatched = newValue
        onCtrlLatchedChange?.invoke(newValue)
    }
    val setAltLatched: (Boolean) -> Unit = { newValue ->
        internalAltLatched = newValue
        onAltLatchedChange?.invoke(newValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElectricSlateTokens.SurfacePanel)
            .border(
                width = 1.dp,
                color = ElectricSlateTokens.BorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sticky CTRL Key
        KeyChip(
            label = "CTRL",
            isLatched = isCtrlLatched,
            onClick = { setCtrlLatched(!isCtrlLatched) }
        )

        // Sticky ALT Key
        KeyChip(
            label = "ALT",
            isLatched = isAltLatched,
            onClick = { setAltLatched(!isAltLatched) }
        )

        // ESC Key
        KeyChip(
            label = "ESC",
            onClick = {
                setCtrlLatched(false)
                setAltLatched(false)
                onSendRawKey(TerminalAnsiKeyEngine.ESCAPE)
            }
        )

        // TAB Key (Path & Command Autocomplete)
        KeyChip(
            label = "TAB",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.TAB) }
        )

        // Directional Arrows (History & Cursor Navigation)
        KeyChip(
            label = "▲",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.ARROW_UP) }
        )
        KeyChip(
            label = "▼",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.ARROW_DOWN) }
        )
        KeyChip(
            label = "◀",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.ARROW_LEFT) }
        )
        KeyChip(
            label = "▶",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.ARROW_RIGHT) }
        )

        // Quick Line Navigation (Home / End)
        KeyChip(
            label = "HOME",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.CTRL_A) }
        )
        KeyChip(
            label = "END",
            onClick = { onSendRawKey(TerminalAnsiKeyEngine.CTRL_E) }
        )

        // Fast Shell Operators
        KeyChip(
            label = "|",
            onClick = { onInsertText(" | ") }
        )
        KeyChip(
            label = "&&",
            onClick = { onInsertText(" && ") }
        )
        KeyChip(
            label = "~",
            onClick = { onInsertText("~") }
        )
        KeyChip(
            label = "/",
            onClick = { onInsertText("/") }
        )
        KeyChip(
            label = "-",
            onClick = { onInsertText("-") }
        )
        KeyChip(
            label = "$",
            onClick = { onInsertText("$") }
        )

        // Quick Signal Pills
        KeyChip(
            label = "^C",
            onClick = {
                setCtrlLatched(false)
                onSendRawKey(TerminalAnsiKeyEngine.CTRL_C)
            }
        )
        KeyChip(
            label = "^L",
            onClick = {
                setCtrlLatched(false)
                onSendRawKey(TerminalAnsiKeyEngine.CTRL_L)
            }
        )
        KeyChip(
            label = "^U",
            onClick = {
                setCtrlLatched(false)
                onSendRawKey(TerminalAnsiKeyEngine.CTRL_U)
            }
        )
        KeyChip(
            label = "^D",
            onClick = {
                setCtrlLatched(false)
                onSendRawKey(TerminalAnsiKeyEngine.CTRL_D)
            }
        )

        // Workflow Automation Trigger Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    color = ElectricSlateTokens.PrimaryAccent,
                    shape = RoundedCornerShape(999.dp)
                )
                .jigglePressClickable { onOpenAutomation() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Runbook",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ElectricSlateTokens.PrimaryAccent
            )
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLatched: Boolean = false
) {
    val bgColor = if (isLatched) {
        ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.25f)
    } else {
        ElectricSlateTokens.SurfaceElevated
    }
    val borderColor = if (isLatched) {
        ElectricSlateTokens.PrimaryAccent
    } else {
        ElectricSlateTokens.BorderSubtle
    }
    val textColor = if (isLatched) {
        ElectricSlateTokens.PrimaryAccent
    } else {
        ElectricSlateTokens.TextPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isLatched) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .jigglePressClickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = textColor
        )
    }
}
