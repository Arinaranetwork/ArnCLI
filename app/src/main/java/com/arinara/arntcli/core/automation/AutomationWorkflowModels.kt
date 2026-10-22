// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.automation

enum class StepBranchAction {
    Continue,
    Halt,
    Retry
}

data class WorkflowStep(
    val id: String,
    val name: String,
    val command: String,
    val targetSessionId: String? = null,
    val timeoutMs: Long = 15_000L,
    val assertionRegex: String? = null,
    val onMatch: StepBranchAction = StepBranchAction.Continue,
    val onFail: StepBranchAction = StepBranchAction.Halt,
    val delayAfterMs: Long = 0L,
    val maxRetries: Int = 1
)

data class WorkflowDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val steps: List<WorkflowStep> = emptyList()
)

data class StepExecutionResult(
    val stepId: String,
    val stepName: String,
    val commandExecuted: String,
    val outputCaptured: String,
    val durationMs: Long,
    val isAssertionMatched: Boolean,
    val isSuccess: Boolean,
    val error: String? = null
)

data class WorkflowRunReport(
    val workflowId: String,
    val workflowName: String,
    val isOverallSuccess: Boolean,
    val stepResults: List<StepExecutionResult>,
    val totalDurationMs: Long,
    val summary: String
)
