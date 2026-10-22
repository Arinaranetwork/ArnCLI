// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evaluates template expressions inside command snippets and macro pills.
 * Supports context variables ({{host}}, {{user}}, {{port}}, {{space}}, {{date}})
 * and in-memory Vault secret resolution ({{vault_secret:key}}).
 */
object SnippetInterpolationEngine {

    private val TOKEN_REGEX = Regex("""\{\{([a-zA-Z0-9_:\-.]+)\}\}""")

    data class Context(
        val host: String = "",
        val username: String = "",
        val port: Int = 22,
        val space: String = "default",
        val customVars: Map<String, String> = emptyMap(),
        val vaultSecretResolver: (String) -> String? = { null }
    )

    /**
     * Replaces template expressions in the template string using the provided context.
     */
    fun interpolate(template: String, context: Context): String {
        if (!template.contains("{{")) return template

        return TOKEN_REGEX.replace(template) { matchResult ->
            val token = matchResult.groupValues[1].trim()

            when {
                token.equals("host", ignoreCase = true) || token.equals("host.address", ignoreCase = true) -> {
                    context.host
                }
                token.equals("user", ignoreCase = true) || token.equals("username", ignoreCase = true) -> {
                    context.username
                }
                token.equals("port", ignoreCase = true) -> {
                    context.port.toString()
                }
                token.equals("space", ignoreCase = true) -> {
                    context.space
                }
                token.equals("date", ignoreCase = true) -> {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                }
                token.equals("time", ignoreCase = true) -> {
                    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                }
                token.equals("isoTimestamp", ignoreCase = true) -> {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                }
                token.startsWith("vault_secret:", ignoreCase = true) -> {
                    val secretKey = token.substringAfter("vault_secret:").trim()
                    context.vaultSecretResolver(secretKey) ?: matchResult.value
                }
                context.customVars.containsKey(token) -> {
                    context.customVars[token] ?: ""
                }
                else -> {
                    // Preserve unrecognized tokens intact for user inspection
                    matchResult.value
                }
            }
        }
    }

    /**
     * Extracts all unfilled variable tokens found in a template.
     */
    fun extractTokens(template: String): List<String> {
        return TOKEN_REGEX.findAll(template).map { it.groupValues[1].trim() }.distinct().toList()
    }
}
