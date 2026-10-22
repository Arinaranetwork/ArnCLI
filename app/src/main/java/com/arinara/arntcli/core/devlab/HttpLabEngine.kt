// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Pure Kotlin/Android HTTP, REST, and GraphQL execution engine.
 * Supports environment variable interpolation, query parameter injection,
 * and microsecond latency measurement.
 */
class HttpLabEngine {

    companion object {
        private val VARIABLE_REGEX = Regex("""\{\{\s*([a-zA-Z0-9_.$:-]+)\s*\}\}""")

        /**
         * Replaces {{variable}} tokens using the provided dictionary and dynamic macros.
         */
        fun substituteVariables(template: String, variables: Map<String, String>): String {
            if (!template.contains("{{")) return template

            return VARIABLE_REGEX.replace(template) { matchResult ->
                val key = matchResult.groupValues[1]
                when {
                    variables.containsKey(key) -> variables[key] ?: ""
                    key == "\$guid" -> UUID.randomUUID().toString()
                    key == "\$timestamp" -> System.currentTimeMillis().toString()
                    key == "\$isoTimestamp" -> {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        sdf.format(Date())
                    }
                    else -> matchResult.value // preserve unresolvable token
                }
            }
        }

        /**
         * Appends enabled query parameters with RFC-compliant URL encoding.
         */
        fun buildUrlWithParams(baseUrl: String, params: List<HttpParam>): String {
            val enabledParams = params.filter { it.enabled && it.key.isNotBlank() }
            if (enabledParams.isEmpty()) return baseUrl

            val queryString = enabledParams.joinToString("&") { p ->
                val k = URLEncoder.encode(p.key.trim(), "UTF-8")
                val v = URLEncoder.encode(p.value.trim(), "UTF-8")
                "$k=$v"
            }

            return if (baseUrl.contains("?")) {
                if (baseUrl.endsWith("?") || baseUrl.endsWith("&")) "$baseUrl$queryString"
                else "$baseUrl&$queryString"
            } else {
                "$baseUrl?$queryString"
            }
        }
    }

    /**
     * Executes an HTTP request on Dispatchers.IO with timing and error handling.
     */
    suspend fun execute(
        request: HttpRequestModel,
        variables: Map<String, String> = emptyMap()
    ): HttpResponseModel = withContext(Dispatchers.IO) {
        val resolvedRawUrl = substituteVariables(request.url, variables)
        val finalUrlString = buildUrlWithParams(resolvedRawUrl, request.params)

        val startNano = System.nanoTime()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(finalUrlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                useCaches = false

                // Method handling (PATCH requires X-HTTP-Method-Override on standard HttpURLConnection)
                if (request.method == HttpMethod.PATCH) {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    requestMethod = request.method.name
                }

                // Apply headers
                request.headers.filter { it.enabled && it.key.isNotBlank() }.forEach { h ->
                    val resolvedVal = substituteVariables(h.value, variables)
                    setRequestProperty(h.key.trim(), resolvedVal.trim())
                }

                // Write body if permitted
                if (request.method.hasBody && !request.body.isNullOrBlank()) {
                    doOutput = true
                    val resolvedBody = substituteVariables(request.body, variables)
                    val bodyBytes = resolvedBody.toByteArray(Charsets.UTF_8)
                    outputStream.use { os ->
                        os.write(bodyBytes)
                        os.flush()
                    }
                }
            }

            val statusCode = connection.responseCode
            val statusMessage = connection.responseMessage ?: ""
            val durationMs = (System.nanoTime() - startNano) / 1_000_000

            val stream = if (statusCode in 200..399) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val maxBytes = 512 * 1024
            val responseBody = stream?.use { s ->
                val reader = BufferedReader(InputStreamReader(s, Charsets.UTF_8))
                val buffer = CharArray(4096)
                val sb = java.lang.StringBuilder()
                var totalRead = 0
                var read: Int
                while (reader.read(buffer).also { read = it } != -1) {
                    if (totalRead + read > maxBytes) {
                        val toAppend = maxBytes - totalRead
                        if (toAppend > 0) sb.append(buffer, 0, toAppend)
                        sb.append("\n... [Response truncated at 512 KB]")
                        break
                    }
                    sb.append(buffer, 0, read)
                    totalRead += read
                }
                sb.toString()
            } ?: ""

            val headerFields = connection.headerFields.filterKeys { it != null }

            HttpResponseModel(
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headerFields,
                body = responseBody,
                durationMs = durationMs,
                sizeBytes = responseBody.toByteArray(Charsets.UTF_8).size.toLong()
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val durationMs = (System.nanoTime() - startNano) / 1_000_000
            val errMsg = "Request Error: ${e.message ?: e.javaClass.simpleName}"
            HttpResponseModel(
                statusCode = 0,
                statusMessage = "Network Failure",
                headers = emptyMap(),
                body = errMsg,
                durationMs = durationMs,
                sizeBytes = errMsg.toByteArray(Charsets.UTF_8).size.toLong()
            )
        } finally {
            connection?.disconnect()
        }
    }
}
