// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetInterpolationEngineTest {

    @Test
    fun interpolate_standardVariables() {
        val context = SnippetInterpolationEngine.Context(
            host = "192.168.1.50",
            username = "admin",
            port = 2222,
            space = "Production"
        )

        val template = "ssh {{user}}@{{host}} -p {{port}} # space={{space}}"
        val result = SnippetInterpolationEngine.interpolate(template, context)

        assertEquals("ssh admin@192.168.1.50 -p 2222 # space=Production", result)
    }

    @Test
    fun interpolate_vaultSecretResolver() {
        val secrets = mapOf("db_prod_pass" to "SuperP@ssw0rd!2026")
        val context = SnippetInterpolationEngine.Context(
            vaultSecretResolver = { alias -> secrets[alias] }
        )

        val template = "export DB_KEY='{{vault_secret:db_prod_pass}}'"
        val result = SnippetInterpolationEngine.interpolate(template, context)

        assertEquals("export DB_KEY='SuperP@ssw0rd!2026'", result)
    }

    @Test
    fun interpolate_preservesUnrecognizedTokens() {
        val context = SnippetInterpolationEngine.Context(host = "localhost")
        val template = "curl http://{{host}}:{{custom_port}}/api"
        val result = SnippetInterpolationEngine.interpolate(template, context)

        assertEquals("curl http://localhost:{{custom_port}}/api", result)
    }

    @Test
    fun extractTokens_identifiesAllPlaceholders() {
        val template = "docker run -e USER={{user}} -e DB={{vault_secret:pass}} -p {{port}}:{{port}}"
        val tokens = SnippetInterpolationEngine.extractTokens(template)

        assertEquals(listOf("user", "vault_secret:pass", "port"), tokens)
    }

    @Test
    fun interpolateMasked_masksVaultSecrets() {
        val context = SnippetInterpolationEngine.Context(
            host = "api.arinara.io",
            vaultSecretResolver = { "RawSecretToken12345" }
        )
        val template = "curl -H 'Authorization: Bearer {{vault_secret:api_token}}' https://{{host}}/v1"
        val masked = SnippetInterpolationEngine.interpolateMasked(template, context)

        assertEquals("curl -H 'Authorization: Bearer [SECRET:api_token]' https://api.arinara.io/v1", masked)
    }
}
