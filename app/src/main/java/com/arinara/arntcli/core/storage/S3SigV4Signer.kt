// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.storage

import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure Kotlin/Android RFC-compliant AWS Signature Version 4 (SigV4) request signer.
 * Enables direct zero-dependency queries to AWS S3, MinIO, Cloudflare R2, and Ceph.
 */
class S3SigV4Signer {

    companion object {
        const val ALGORITHM = "AWS4-HMAC-SHA256"
        const val EMPTY_PAYLOAD_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        fun sha256Hex(data: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(data)
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun hmacSha256(key: ByteArray, data: String): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(data.toByteArray(Charsets.UTF_8))
        }

        /**
         * Derives hierarchical signing key according to AWS SigV4 specification.
         */
        fun deriveSigningKey(
            secretKey: String,
            dateStamp: String,
            regionName: String,
            serviceName: String = "s3"
        ): ByteArray {
            val kSecret = ("AWS4$secretKey").toByteArray(Charsets.UTF_8)
            val kDate = hmacSha256(kSecret, dateStamp)
            val kRegion = hmacSha256(kDate, regionName)
            val kService = hmacSha256(kRegion, serviceName)
            return hmacSha256(kService, "aws4_request")
        }

        /**
         * Computes canonical query string with alphabetical sorting and URL encoding.
         */
        fun buildCanonicalQuery(queryParams: Map<String, String>): String {
            if (queryParams.isEmpty()) return ""
            return queryParams.toSortedMap()
                .map { (k, v) ->
                    val encodedKey = URLEncoder.encode(k, "UTF-8").replace("+", "%20")
                    val encodedVal = URLEncoder.encode(v, "UTF-8").replace("+", "%20")
                    "$encodedKey=$encodedVal"
                }
                .joinToString("&")
        }

        /**
         * Formats and signs an AWS S3 REST request, returning the Authorization header value.
         */
        fun generateAuthorizationHeader(
            method: String,
            canonicalUri: String,
            queryParams: Map<String, String> = emptyMap(),
            headers: Map<String, String>,
            payloadSha256: String = EMPTY_PAYLOAD_SHA256,
            accessKey: String,
            secretKey: String,
            region: String = "us-east-1",
            dateTimeStamp: String, // e.g. "20261022T100000Z"
            dateStamp: String     // e.g. "20261022"
        ): String {
            require(accessKey.isNotBlank()) { "Access key cannot be blank" }
            require(secretKey.isNotBlank()) { "Secret key cannot be blank" }

            val canonicalUriFormatted = if (canonicalUri.startsWith("/")) canonicalUri else "/$canonicalUri"
            val canonicalQuery = buildCanonicalQuery(queryParams)

            // Normalize and sort headers (lowercase keys)
            val normalizedHeaders = headers.mapKeys { it.key.lowercase().trim() }
                .mapValues { it.value.trim().replace(Regex("\\s+"), " ") }
                .toSortedMap()

            val canonicalHeaders = normalizedHeaders.entries.joinToString("") { "${it.key}:${it.value}\n" }
            val signedHeaders = normalizedHeaders.keys.joinToString(";")

            // 1. Canonical Request
            val canonicalRequest = listOf(
                method.uppercase(),
                canonicalUriFormatted,
                canonicalQuery,
                canonicalHeaders,
                signedHeaders,
                payloadSha256
            ).joinToString("\n")

            val hashedCanonicalRequest = sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))

            // 2. String to Sign
            val credentialScope = "$dateStamp/$region/s3/aws4_request"
            val stringToSign = listOf(
                ALGORITHM,
                dateTimeStamp,
                credentialScope,
                hashedCanonicalRequest
            ).joinToString("\n")

            // 3. Compute Signature
            val signingKey = deriveSigningKey(secretKey, dateStamp, region, "s3")
            val signature = hmacSha256(signingKey, stringToSign).joinToString("") { "%02x".format(it) }

            // 4. Construct Authorization Header
            return "$ALGORITHM Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        }
    }
}
