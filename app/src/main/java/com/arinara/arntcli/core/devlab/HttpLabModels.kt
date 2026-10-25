// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import java.util.Locale

enum class HttpMethod(val hasBody: Boolean) {
    GET(false),
    POST(true),
    PUT(true),
    DELETE(true),
    PATCH(true),
    HEAD(false),
    OPTIONS(false)
}

data class HttpHeader(
    val key: String,
    val value: String,
    val enabled: Boolean = true
) {
    init {
        require(key.isNotBlank()) { "Header key cannot be blank" }
    }
}

data class HttpParam(
    val key: String,
    val value: String,
    val enabled: Boolean = true
) {
    init {
        require(key.isNotBlank()) { "Parameter key cannot be blank" }
    }
}

data class HttpRequestModel(
    val id: String = "req-${System.currentTimeMillis()}",
    val name: String = "New Request",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "https://httpbin.org/get",
    val headers: List<HttpHeader> = listOf(
        HttpHeader("Accept", "application/json"),
        HttpHeader("User-Agent", "ArnTCLI/0.7.0")
    ),
    val params: List<HttpParam> = emptyList(),
    val body: String? = null
)

data class HttpResponseModel(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val body: String,
    val durationMs: Long,
    val sizeBytes: Long
) {
    val isSuccess: Boolean get() = statusCode in 200..299

    fun formattedSize(): String {
        if (sizeBytes < 1024) return "$sizeBytes B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
