// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.automation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.automation.AutomationWorkflowEngine
import com.arinara.arntcli.core.automation.StepExecutionResult
import com.arinara.arntcli.core.automation.WorkflowDefinition
import com.arinara.arntcli.core.automation.WorkflowRunReport
import com.arinara.arntcli.core.automation.WorkflowStep
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import kotlinx.coroutines.launch

/**
 * Interactive Runbook & Workflow Automation Sheet.
 * Executes sequential diagnosis and remediation steps with live regex assertion feedback.
 */
@Composable
fun WorkflowRunnerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onExecuteCommand: suspend (cmd: String) -> String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val engine = remember { AutomationWorkflowEngine() }

    val defaultWorkflows = remember {
        listOf(
            WorkflowDefinition(
                id = "wf-health",
                name = "Server Health Sweep",
                description = "Ping gateway, check memory headroom, verify disk space",
                steps = listOf(
                    WorkflowStep(
                        id = "s1",
                        name = "Gateway Reachability",
                        command = "ping -c 2 127.0.0.1",
                        assertionRegex = "0% packet loss"
                    ),
                    WorkflowStep(
                        id = "s2",
                        name = "Memory Available",
                        command = "free -m",
                        assertionRegex = "Mem:"
                    ),
                    WorkflowStep(
                        id = "s3",
                        name = "Disk Headroom Check",
                        command = "df -h /",
                        assertionRegex = "/dev/"
                    )
                )
            ),
            WorkflowDefinition(
                id = "wf-containers",
                name = "Docker Diagnostic & Restart",
                description = "Inspect Docker daemon and list active containers",
                steps = listOf(
                    WorkflowStep(
                        id = "c1",
                        name = "Docker Daemon State",
                        command = "systemctl is-active docker || true",
                        assertionRegex = "active|unknown"
                    ),
                    WorkflowStep(
                        id = "c2",
                        name = "Container Inventory",
                        command = "docker ps -a --format '{{.Names}}\t{{.Status}}'",
                        assertionRegex = ".*"
                    )
                )
            )
        )
    }

    var selectedWorkflow by remember { mutableStateOf(defaultWorkflows.first()) }
    var isRunning by remember { mutableStateOf(false) }
    var executionReport by remember { mutableStateOf<WorkflowRunReport?>(null) }
    var latestStepResult by remember { mutableStateOf<StepExecutionResult?>(null) }

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Workflow Automation",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workflow Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                defaultWorkflows.forEach { wf ->
                    AppPillChip(
                        label = wf.name,
                        isSelected = selectedWorkflow.id == wf.id,
                        onClick = {
                            if (!isRunning) {
                                selectedWorkflow = wf
                                executionReport = null
                                latestStepResult = null
                            }
                        }
                    )
                }
            }

            // Description Card
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = selectedWorkflow.name,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )
                    Text(
                        text = selectedWorkflow.description,
                        fontFamily = ElmsSans,
                        fontSize = 12.sp,
                        color = ElectricSlateTokens.TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${selectedWorkflow.steps.size} sequential steps with regex assertions",
                        fontFamily = ElmsSans,
                        fontSize = 11.sp,
                        color = ElectricSlateTokens.PrimaryAccent
                    )
                }
            }

            // Steps Overview
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Runbook Steps",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ElectricSlateTokens.TextPrimary
                )

                selectedWorkflow.steps.forEachIndexed { idx, step ->
                    val matchingResult = executionReport?.stepResults?.firstOrNull { it.stepId == step.id }
                        ?: if (latestStepResult?.stepId == step.id) latestStepResult else null

                    val statusText = when {
                        matchingResult == null -> "Pending"
                        matchingResult.isSuccess -> "Passed"
                        else -> "Failed"
                    }

                    val badgeBg = when {
                        matchingResult == null -> ElectricSlateTokens.TrackInactive
                        matchingResult.isSuccess -> ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f)
                        else -> ElectricSlateTokens.Destructive.copy(alpha = 0.2f)
                    }

                    val badgeColor = when {
                        matchingResult == null -> ElectricSlateTokens.TextMuted
                        matchingResult.isSuccess -> ElectricSlateTokens.PrimaryAccent
                        else -> ElectricSlateTokens.Destructive
                    }

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
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${step.name}",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "$ ${step.command}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
            }

            // Execution Report Summary
            executionReport?.let { report ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (report.isOverallSuccess) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.1f)
                            else ElectricSlateTokens.Destructive.copy(alpha = 0.1f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (report.isOverallSuccess) ElectricSlateTokens.PrimaryAccent
                            else ElectricSlateTokens.Destructive,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (report.isOverallSuccess) "Workflow Succeeded" else "Workflow Failed",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (report.isOverallSuccess) ElectricSlateTokens.PrimaryAccent
                            else ElectricSlateTokens.Destructive
                        )
                        Text(
                            text = "${report.summary} (Total: ${report.totalDurationMs}ms)",
                            fontFamily = ElmsSans,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )
                    }
                }
            }

            // Action Trigger
            AppButton(
                text = if (isRunning) "Executing Runbook..." else "Run Workflow",
                variant = AppButtonVariant.Primary,
                enabled = !isRunning,
                onClick = {
                    isRunning = true
                    executionReport = null
                    coroutineScope.launch {
                        try {
                            val rep = engine.runWorkflow(
                                workflow = selectedWorkflow,
                                commandExecutor = { _, cmd -> onExecuteCommand(cmd) },
                                onStepProgress = { stepRes -> latestStepResult = stepRes }
                            )
                            executionReport = rep
                        } finally {
                            isRunning = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            BottomClearanceSpacer()
        }
    }
}
