// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpLabEngineTest {

    @Test
    fun substituteVariables_replacesDictionaryKeys() {
        val vars = mapOf(
            "baseUrl" to "https://api.homelab.local",
            "apiVersion" to "v2",
            "userId" to "42"
        )
        val template = "{{baseUrl}}/{{apiVersion}}/users/{{userId}}"
        val result = HttpLabEngine.substituteVariables(template, vars)

        assertEquals("https://api.homelab.local/v2/users/42", result)
    }

    @Test
    fun substituteVariables_preservesUnknownTokens() {
        val vars = mapOf("token" to "secret123")
        val template = "Bearer {{token}} for {{unknownVar}}"
        val result = HttpLabEngine.substituteVariables(template, vars)

        assertEquals("Bearer secret123 for {{unknownVar}}", result)
    }

    @Test
    fun substituteVariables_evaluatesDynamicMacros() {
        val template = "id={{\$guid}}&ts={{\$timestamp}}&iso={{\$isoTimestamp}}"
        val result = HttpLabEngine.substituteVariables(template, emptyMap())

        assertFalse(result.contains("{{\$guid}}"))
        assertFalse(result.contains("{{\$timestamp}}"))
        assertFalse(result.contains("{{\$isoTimestamp}}"))

        assertTrue(result.contains("id="))
        assertTrue(result.contains("ts="))
        assertTrue(result.contains("iso="))
        assertTrue(result.contains("Z"))
    }

    @Test
    fun buildUrlWithParams_appendsCorrectly() {
        val params = listOf(
            HttpParam("search", "network tools", enabled = true),
            HttpParam("limit", "25", enabled = true),
            HttpParam("disabledKey", "value", enabled = false)
        )

        val cleanUrl = HttpLabEngine.buildUrlWithParams("https://api.example.com/items", params)
        assertEquals("https://api.example.com/items?search=network+tools&limit=25", cleanUrl)

        val urlWithExistingQuery = HttpLabEngine.buildUrlWithParams("https://api.example.com/items?sort=asc", params)
        assertEquals("https://api.example.com/items?sort=asc&search=network+tools&limit=25", urlWithExistingQuery)
    }

    @Test
    fun httpResponseModel_formattingAndSuccess() {
        val success = HttpResponseModel(
            statusCode = 200,
            statusMessage = "OK",
            headers = mapOf("Content-Type" to listOf("application/json")),
            body = "{\"status\":\"ok\"}",
            durationMs = 45,
            sizeBytes = 15
        )
        assertTrue(success.isSuccess)
        assertEquals("15 B", success.formattedSize())

        val kbResponse = HttpResponseModel(
            statusCode = 404,
            statusMessage = "Not Found",
            headers = emptyMap(),
            body = "error",
            durationMs = 120,
            sizeBytes = 2048
        )
        assertFalse(kbResponse.isSuccess)
        assertEquals("2.0 KB", kbResponse.formattedSize())
    }
}
