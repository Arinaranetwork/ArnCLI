// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.terminal.TabCompletionResult
import com.arinara.arntcli.core.terminal.TerminalAnsiKeyEngine
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.model.TerminalSession
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable

/**
 * TerminalScreen - Stateful interactive terminal emulator session workspace.
 * Hosted within AppScreen scaffold providing safeDrawing and statusBars insets.
 */
@Composable
fun TerminalScreen(
    contentPadding: PaddingValues,
    activeSpaceName: String,
    sessions: List<TerminalSession>,
    onStartLocalShell: () -> Unit,
    onExecuteCommand: (sessionId: String, command: String) -> Unit = { _, _ -> },
    onInterruptSession: (sessionId: String) -> Unit = {},
    onTabComplete: (sessionId: String, currentInput: String) -> TabCompletionResult = { _, input ->
        TabCompletionResult(input, emptyList())
    },
    onAppendLines: (sessionId: String, lines: List<String>) -> Unit = { _, _ -> },
    onCloseSession: (sessionId: String) -> Unit = {},
    onOpenAutomation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSessionIndex by remember { mutableIntStateOf(0) }
    var commandInputState by remember { mutableStateOf(TextFieldValue("")) }
    var historyCursor by remember { mutableIntStateOf(-1) }
    var draftBeforeHistory by remember { mutableStateOf("") }
    var isCtrlLatched by remember { mutableStateOf(false) }
    var isAltLatched by remember { mutableStateOf(false) }

    val quickCommands = remember {
        listOf(
            "help",
            "ll",
            "pwd",
            "env",
            "ip addr",
            "ping -c 4 1.1.1.1",
            "ps -A",
            "df -h",
            "uname -a",
            "clear"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        if (sessions.isEmpty()) {
            // Empty State
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                isGlow = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = ElectricSlateTokens.BorderSubtle,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = "No Terminal Sessions",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "No Active Terminal Sessions",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    Text(
                        text = "Spawn a stateful local shell with writable home directory or connect to a registered host.",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AppButton(
                        text = "Start Local Shell",
                        leadingIcon = Icons.Outlined.Add,
                        variant = AppButtonVariant.Primary,
                        onClick = onStartLocalShell
                    )
                }
            }
        } else {
            val safeIndex = selectedSessionIndex.coerceIn(0, sessions.size - 1)
            val currentSession = sessions[safeIndex]
            val terminalScrollState = rememberScrollState()

            LaunchedEffect(currentSession.id) {
                historyCursor = -1
                draftBeforeHistory = ""
            }

            LaunchedEffect(currentSession.lines.size) {
                terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
            }

            // Helper functions for terminal control sequences and line editing
            val submitCommand: () -> Unit = {
                val raw = commandInputState.text.trim()
                if (raw.isNotEmpty()) {
                    historyCursor = -1
                    draftBeforeHistory = ""
                    commandInputState = TextFieldValue("")
                    onExecuteCommand(currentSession.id, raw)
                }
            }

            val handleControlAction: (String) -> Unit = { signal ->
                when (signal) {
                    TerminalAnsiKeyEngine.CTRL_C -> {
                        if (currentSession.isRunning) {
                            onInterruptSession(currentSession.id)
                        } else if (commandInputState.text.isNotEmpty()) {
                            onAppendLines(
                                currentSession.id,
                                listOf("${currentSession.cwd} $ ${commandInputState.text}^C")
                            )
                            commandInputState = TextFieldValue("")
                            historyCursor = -1
                        } else {
                            onInterruptSession(currentSession.id)
                        }
                    }

                    TerminalAnsiKeyEngine.CTRL_L -> {
                        onExecuteCommand(currentSession.id, "clear")
                    }

                    TerminalAnsiKeyEngine.CTRL_D -> {
                        if (commandInputState.text.isEmpty()) {
                            val closingIndex = safeIndex
                            onCloseSession(currentSession.id)
                            if (closingIndex >= sessions.size - 1) {
                                selectedSessionIndex = (sessions.size - 2).coerceAtLeast(0)
                            }
                        } else {
                            val text = commandInputState.text
                            val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                            if (cursor < text.length) {
                                val updated = text.removeRange(cursor, cursor + 1)
                                commandInputState = TextFieldValue(updated, TextRange(cursor))
                            }
                        }
                    }

                    TerminalAnsiKeyEngine.CTRL_A -> {
                        commandInputState = commandInputState.copy(selection = TextRange(0))
                    }

                    TerminalAnsiKeyEngine.CTRL_E -> {
                        val end = commandInputState.text.length
                        commandInputState = commandInputState.copy(selection = TextRange(end))
                    }

                    TerminalAnsiKeyEngine.CTRL_U -> {
                        val text = commandInputState.text
                        val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                        val remaining = if (cursor > 0) text.substring(cursor) else ""
                        commandInputState = TextFieldValue(remaining, TextRange(0))
                    }

                    TerminalAnsiKeyEngine.CTRL_K -> {
                        val text = commandInputState.text
                        val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                        val remaining = text.substring(0, cursor)
                        commandInputState = TextFieldValue(remaining, TextRange(remaining.length))
                    }

                    TerminalAnsiKeyEngine.CTRL_W -> {
                        val text = commandInputState.text
                        val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                        val before = text.substring(0, cursor).trimEnd()
                        val lastSpace = before.lastIndexOf(' ')
                        val newPrefix = if (lastSpace == -1) "" else before.substring(0, lastSpace + 1)
                        val updated = newPrefix + text.substring(cursor)
                        commandInputState = TextFieldValue(updated, TextRange(newPrefix.length))
                    }
                }
            }

            // Session Quick Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "+ New Shell",
                        isSelected = false,
                        onClick = {
                            onStartLocalShell()
                            selectedSessionIndex = sessions.size
                        }
                    )
                }
                items(sessions.indices.toList()) { index ->
                    AppPillChip(
                        label = sessions[index].title,
                        isSelected = safeIndex == index,
                        onClick = { selectedSessionIndex = index },
                        leadingIcon = Icons.Outlined.Terminal
                    )
                }
            }

            // Primary Terminal Buffer Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                isGlow = true
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Terminal Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentSession.isConnected) ElectricSlateTokens.PrimaryAccent
                                        else ElectricSlateTokens.TextMuted
                                    )
                            )
                            Text(
                                text = currentSession.title,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ElectricSlateTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentSession.cwd,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when {
                                    currentSession.isRunning -> "Running"
                                    currentSession.isConnected -> "Ready"
                                    else -> "Disconnected"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (currentSession.isRunning) {
                                    ElectricSlateTokens.PrimaryAccent
                                } else {
                                    ElectricSlateTokens.TextMuted
                                }
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
                                    .jigglePressClickable {
                                        val closingIndex = safeIndex
                                        onCloseSession(currentSession.id)
                                        if (closingIndex >= sessions.size - 1) {
                                            selectedSessionIndex = (sessions.size - 2).coerceAtLeast(0)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.Destructive
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Terminal Output Area (Selectable for easy copying)
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(terminalScrollState),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (currentSession.lines.isEmpty()) {
                                Text(
                                    text = "ArnCLI Terminal Environment [Space: $activeSpaceName]",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Text(
                                    text = "Shell: ${currentSession.shell} | CWD: ${currentSession.cwd}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Type 'help' for built-in commands, aliases, and key shortcuts.",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            } else {
                                currentSession.lines.forEach { line ->
                                    val isPromptEcho = line.startsWith("$ ") || line.contains(" $ ")
                                    if (isPromptEcho && (line.startsWith("$ ") || line.startsWith("~") || line.startsWith("/"))) {
                                        val splitIdx = line.indexOf("$ ")
                                        val promptPrefix = if (splitIdx > 0) line.substring(0, splitIdx + 1) else "$"
                                        val cmdPart = if (splitIdx >= 0) line.substring(splitIdx + 2) else line
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = promptPrefix,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.PrimaryAccent
                                            )
                                            Text(
                                                text = cmdPart,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                        }
                                    } else {
                                        val isErrorLine = line.startsWith("Error:") ||
                                            line.startsWith("sh: ") ||
                                            line.startsWith("cd: ") ||
                                            line.startsWith("export: ") ||
                                            line.contains("Permission denied") ||
                                            line.contains("No such file or directory") ||
                                            line.contains("not found")
                                        Text(
                                            text = line,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = if (isErrorLine) {
                                                ElectricSlateTokens.Destructive
                                            } else {
                                                ElectricSlateTokens.TextPrimary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Interactive Prompt Input Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(
                                width = if (isCtrlLatched || isAltLatched) 2.dp else 1.dp,
                                color = if (isCtrlLatched || isAltLatched) {
                                    ElectricSlateTokens.PrimaryAccent
                                } else {
                                    ElectricSlateTokens.BorderSubtle
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${currentSession.cwd.takeLast(18)} $",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.PrimaryAccent
                                )
                                BasicTextField(
                                    value = commandInputState,
                                    onValueChange = { newValue ->
                                        // Intercept sticky CTRL latch if a single character was typed
                                        if (isCtrlLatched && newValue.text.length == commandInputState.text.length + 1) {
                                            val insertPos = (newValue.selection.start - 1).coerceIn(0, newValue.text.lastIndex)
                                            val typedChar = newValue.text[insertPos].lowercaseChar()
                                            isCtrlLatched = false
                                            when (typedChar) {
                                                'c' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_C)
                                                'l' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_L)
                                                'd' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_D)
                                                'a' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_A)
                                                'e' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_E)
                                                'u' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_U)
                                                'k' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_K)
                                                'w' -> handleControlAction(TerminalAnsiKeyEngine.CTRL_W)
                                                else -> commandInputState = newValue
                                            }
                                        } else if (isAltLatched && newValue.text.length == commandInputState.text.length + 1) {
                                            val insertPos = (newValue.selection.start - 1).coerceIn(0, newValue.text.lastIndex)
                                            val typedChar = newValue.text[insertPos].lowercaseChar()
                                            isAltLatched = false
                                            val text = commandInputState.text
                                            val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                                            when (typedChar) {
                                                'b' -> {
                                                    val before = text.substring(0, cursor).trimEnd()
                                                    val prevSpace = before.lastIndexOf(' ')
                                                    val target = if (prevSpace == -1) 0 else prevSpace + 1
                                                    commandInputState = commandInputState.copy(selection = TextRange(target))
                                                }
                                                'f' -> {
                                                    val after = text.substring(cursor).trimStart()
                                                    val leadingSpaces = text.substring(cursor).length - after.length
                                                    val nextSpace = after.indexOf(' ')
                                                    val target = if (nextSpace == -1) text.length else cursor + leadingSpaces + nextSpace
                                                    commandInputState = commandInputState.copy(selection = TextRange(target))
                                                }
                                                else -> commandInputState = newValue
                                            }
                                        } else {
                                            commandInputState = newValue
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = { submitCommand() }
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (commandInputState.text.isEmpty()) {
                                            Text(
                                                text = if (isCtrlLatched) {
                                                    "CTRL latched (press c, l, u, w, a, e, d)..."
                                                } else if (isAltLatched) {
                                                    "ALT latched (press b, f, or arrow)..."
                                                } else {
                                                    "Enter command (cd, ll, export, help)..."
                                                },
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextMuted
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Execute or Stop Button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentSession.isRunning) {
                                            ElectricSlateTokens.Destructive
                                        } else {
                                            ElectricSlateTokens.PrimaryAccent
                                        }
                                    )
                                    .jigglePressClickable {
                                        if (currentSession.isRunning && commandInputState.text.isBlank()) {
                                            onInterruptSession(currentSession.id)
                                        } else {
                                            submitCommand()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentSession.isRunning && commandInputState.text.isBlank()) {
                                        Icons.Outlined.Close
                                    } else {
                                        Icons.Outlined.PlayArrow
                                    },
                                    contentDescription = if (currentSession.isRunning && commandInputState.text.isBlank()) {
                                        "Stop Running Process"
                                    } else {
                                        "Execute Command"
                                    },
                                    tint = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Modifier & Macro Key Bar
            TerminalKeyBar(
                ctrlLatchedExternal = isCtrlLatched,
                onCtrlLatchedChange = { isCtrlLatched = it },
                altLatchedExternal = isAltLatched,
                onAltLatchedChange = { isAltLatched = it },
                onSendRawKey = { rawKey ->
                    when (rawKey) {
                        TerminalAnsiKeyEngine.CTRL_C,
                        TerminalAnsiKeyEngine.CTRL_D,
                        TerminalAnsiKeyEngine.CTRL_L,
                        TerminalAnsiKeyEngine.CTRL_A,
                        TerminalAnsiKeyEngine.CTRL_E,
                        TerminalAnsiKeyEngine.CTRL_U,
                        TerminalAnsiKeyEngine.CTRL_K,
                        TerminalAnsiKeyEngine.CTRL_W -> handleControlAction(rawKey)

                        TerminalAnsiKeyEngine.ESCAPE -> {
                            isCtrlLatched = false
                            isAltLatched = false
                            historyCursor = -1
                            commandInputState = TextFieldValue("")
                        }

                        TerminalAnsiKeyEngine.TAB -> {
                            val completion = onTabComplete(currentSession.id, commandInputState.text)
                            commandInputState = TextFieldValue(
                                text = completion.completedInput,
                                selection = TextRange(completion.completedInput.length)
                            )
                            if (completion.candidates.size > 1) {
                                onAppendLines(
                                    currentSession.id,
                                    listOf(
                                        "${currentSession.cwd} $ ${commandInputState.text}",
                                        completion.candidates.joinToString("  ")
                                    )
                                )
                            }
                        }

                        TerminalAnsiKeyEngine.ARROW_UP -> {
                            val history = currentSession.history
                            if (history.isNotEmpty()) {
                                if (historyCursor == -1) {
                                    draftBeforeHistory = commandInputState.text
                                    historyCursor = history.lastIndex
                                } else if (historyCursor > 0) {
                                    historyCursor -= 1
                                }
                                val recalled = history[historyCursor]
                                commandInputState = TextFieldValue(
                                    text = recalled,
                                    selection = TextRange(recalled.length)
                                )
                            }
                        }

                        TerminalAnsiKeyEngine.ARROW_DOWN -> {
                            val history = currentSession.history
                            if (historyCursor != -1) {
                                if (historyCursor < history.lastIndex) {
                                    historyCursor += 1
                                    val recalled = history[historyCursor]
                                    commandInputState = TextFieldValue(
                                        text = recalled,
                                        selection = TextRange(recalled.length)
                                    )
                                } else {
                                    historyCursor = -1
                                    commandInputState = TextFieldValue(
                                        text = draftBeforeHistory,
                                        selection = TextRange(draftBeforeHistory.length)
                                    )
                                }
                            }
                        }

                        TerminalAnsiKeyEngine.ARROW_LEFT -> {
                            val text = commandInputState.text
                            val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                            val newCursor = if (isAltLatched) {
                                isAltLatched = false
                                val before = text.substring(0, cursor).trimEnd()
                                val prevSpace = before.lastIndexOf(' ')
                                if (prevSpace == -1) 0 else prevSpace + 1
                            } else {
                                (cursor - 1).coerceAtLeast(0)
                            }
                            commandInputState = commandInputState.copy(selection = TextRange(newCursor))
                        }

                        TerminalAnsiKeyEngine.ARROW_RIGHT -> {
                            val text = commandInputState.text
                            val cursor = commandInputState.selection.start.coerceIn(0, text.length)
                            val newCursor = if (isAltLatched) {
                                isAltLatched = false
                                val after = text.substring(cursor).trimStart()
                                val leadingSpaces = text.substring(cursor).length - after.length
                                val nextSpace = after.indexOf(' ')
                                if (nextSpace == -1) text.length else cursor + leadingSpaces + nextSpace
                            } else {
                                (cursor + 1).coerceAtMost(text.length)
                            }
                            commandInputState = commandInputState.copy(selection = TextRange(newCursor))
                        }
                    }
                },
                onInsertText = { textToInsert ->
                    val currentText = commandInputState.text
                    val cursor = commandInputState.selection.start.coerceIn(0, currentText.length)
                    val updated = currentText.substring(0, cursor) + textToInsert + currentText.substring(cursor)
                    val newCursor = cursor + textToInsert.length
                    commandInputState = TextFieldValue(updated, TextRange(newCursor))
                },
                onOpenAutomation = onOpenAutomation
            )

            // Quick Diagnostic Commands
            Text(
                text = "Quick Commands",
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ElectricSlateTokens.TextPrimary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    AppPillChip(
                        label = cmd,
                        isSelected = false,
                        onClick = {
                            onExecuteCommand(currentSession.id, cmd)
                        }
                    )
                }
            }
        }

        BottomClearanceSpacer()
    }
}
