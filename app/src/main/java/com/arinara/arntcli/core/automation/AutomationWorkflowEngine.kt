// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.automation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Terminal Workflow Automation Engine.
 * Executes sequential runbooks across active terminal sessions,
 * performing regex assertions against captured output, respecting delays and step branch rules.
 */
class AutomationWorkflowEngine {

    /**
     * Executes a complete workflow runbook.
     * @param workflow The workflow definition.
     * @param commandExecutor Lambda executing a command on the target session, returning captured output.
     * @param onStepProgress Optional callback reporting progress on each step.
     */
    suspend fun runWorkflow(
        workflow: WorkflowDefinition,
        commandExecutor: suspend (sessionId: String?, cmd: String) -> String,
        onStepProgress: ((StepExecutionResult) -> Unit)? = null
    ): WorkflowRunReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stepResults = mutableListOf<StepExecutionResult>()
        var overallSuccess = true
        var failureSummary: String? = null

        for (step in workflow.steps) {
            var attempt = 0
            var stepSuccess = false
            var lastResult: StepExecutionResult? = null

            while (attempt < step.maxRetries && !stepSuccess) {
                attempt++
                val stepStartTime = System.currentTimeMillis()

                try {
                    val rawOutput = withTimeoutOrNull(step.timeoutMs) {
                        commandExecutor(step.targetSessionId, step.command)
                    }

                    val duration = System.currentTimeMillis() - stepStartTime

                    if (rawOutput == null) {
                        // Timeout occurred
                        lastResult = StepExecutionResult(
                            stepId = step.id,
                            stepName = step.name,
                            commandExecuted = step.command,
                            outputCaptured = "",
                            durationMs = duration,
                            isAssertionMatched = false,
                            isSuccess = false,
                            error = "Step timed out after ${step.timeoutMs}ms"
                        )
                    } else {
                        val isMatched = if (!step.assertionRegex.isNullOrBlank()) {
                            Regex(step.assertionRegex, RegexOption.IGNORE_CASE).containsMatchIn(rawOutput)
                        } else {
                            true
                        }

                        stepSuccess = isMatched
                        lastResult = StepExecutionResult(
                            stepId = step.id,
                            stepName = step.name,
                            commandExecuted = step.command,
                            outputCaptured = rawOutput,
                            durationMs = duration,
                            isAssertionMatched = isMatched,
                            isSuccess = isMatched,
                            error = if (!isMatched) "Assertion failed for regex: ${step.assertionRegex}" else null
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val duration = System.currentTimeMillis() - stepStartTime
                    lastResult = StepExecutionResult(
                        stepId = step.id,
                        stepName = step.name,
                        commandExecuted = step.command,
                        outputCaptured = "",
                        durationMs = duration,
                        isAssertionMatched = false,
                        isSuccess = false,
                        error = e.message ?: "Execution error"
                    )
                }

                if (!stepSuccess && attempt < step.maxRetries && step.onFail == StepBranchAction.Retry) {
                    delay(500)
                }
            }

            val finalStepResult = lastResult ?: StepExecutionResult(
                stepId = step.id,
                stepName = step.name,
                commandExecuted = step.command,
                outputCaptured = "",
                durationMs = 0L,
                isAssertionMatched = false,
                isSuccess = false,
                error = "Unexecuted step"
            )

            stepResults.add(finalStepResult)
            onStepProgress?.invoke(finalStepResult)

            if (!finalStepResult.isSuccess) {
                if (step.onFail == StepBranchAction.Halt) {
                    overallSuccess = false
                    failureSummary = "Step '${step.name}' failed: ${finalStepResult.error}"
                    break
                }
            }

            if (step.delayAfterMs > 0L) {
                delay(step.delayAfterMs)
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val summaryText = failureSummary
            ?: if (overallSuccess) "All ${stepResults.size} steps completed successfully."
            else "Workflow finished with failures."

        WorkflowRunReport(
            workflowId = workflow.id,
            workflowName = workflow.name,
            isOverallSuccess = overallSuccess,
            stepResults = stepResults,
            totalDurationMs = totalDuration,
            summary = summaryText
        )
    }
}
