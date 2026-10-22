// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.automation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationWorkflowEngineTest {

    private val engine = AutomationWorkflowEngine()

    @Test
    fun runWorkflow_allStepsSucceed() = runTest {
        val workflow = WorkflowDefinition(
            id = "wf-1",
            name = "Health Check & Restart",
            steps = listOf(
                WorkflowStep(
                    id = "s1",
                    name = "Ping Gateway",
                    command = "ping -c 1 127.0.0.1",
                    assertionRegex = "1 packets transmitted"
                ),
                WorkflowStep(
                    id = "s2",
                    name = "Check Service",
                    command = "systemctl status nginx",
                    assertionRegex = "active \\(running\\)"
                )
            )
        )

        val report = engine.runWorkflow(
            workflow = workflow,
            commandExecutor = { _, cmd ->
                when {
                    cmd.contains("ping") -> "1 packets transmitted, 1 received, 0% packet loss"
                    cmd.contains("systemctl") -> "Active: active (running) since Thu 2026-10-22"
                    else -> "unknown"
                }
            }
        )

        assertTrue(report.isOverallSuccess)
        assertEquals(2, report.stepResults.size)
        assertTrue(report.stepResults[0].isSuccess)
        assertTrue(report.stepResults[1].isSuccess)
    }

    @Test
    fun runWorkflow_stepFailsAndHalts() = runTest {
        val workflow = WorkflowDefinition(
            id = "wf-2",
            name = "Failing Workflow",
            steps = listOf(
                WorkflowStep(
                    id = "s1",
                    name = "Assert Process",
                    command = "pgrep redis-server",
                    assertionRegex = "^[0-9]+$",
                    onFail = StepBranchAction.Halt
                ),
                WorkflowStep(
                    id = "s2",
                    name = "Unreachable Step",
                    command = "echo 'should not run'"
                )
            )
        )

        val report = engine.runWorkflow(
            workflow = workflow,
            commandExecutor = { _, _ -> "" } // Empty output, failing regex
        )

        assertFalse(report.isOverallSuccess)
        assertEquals(1, report.stepResults.size) // Step 2 was halted
        assertFalse(report.stepResults[0].isSuccess)
    }
}
