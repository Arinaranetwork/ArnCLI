// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageEngineTest {

    @Test
    fun storageNode_formattedSize() {
        val dir = StorageNode("d1", "media", "/media", 0, true, StorageType.Smb)
        assertEquals("<DIR>", dir.formattedSize())

        val bytesFile = StorageNode("f1", "small.txt", "/small.txt", 420, false, StorageType.Smb)
        assertEquals("420 B", bytesFile.formattedSize())

        val kbFile = StorageNode("f2", "config.yml", "/config.yml", 4096, false, StorageType.Smb)
        assertEquals("4.0 KB", kbFile.formattedSize())

        val mbFile = StorageNode("f3", "dump.sql", "/dump.sql", 52_428_800, false, StorageType.Smb)
        assertEquals("50.0 MB", mbFile.formattedSize())

        val gbFile = StorageNode("f4", "backup.iso", "/backup.iso", 1_610_612_736, false, StorageType.Smb)
        assertEquals("1.50 GB", gbFile.formattedSize())
    }

    @Test
    fun storageNode_permissionsFormatting() {
        val dir = StorageNode("d1", "etc", "/etc", 0, true, StorageType.Smb, permissionsOctal = 493)
        assertEquals("drwxr-xr-x", dir.toPermissionsString())

        val file = StorageNode("f1", "doc.txt", "/doc.txt", 100, false, StorageType.Smb, permissionsOctal = 420)
        assertEquals("-rw-r--r--", file.toPermissionsString())

        val secret = StorageNode("f2", "key.pem", "/key.pem", 1800, false, StorageType.Smb, permissionsOctal = 384)
        assertEquals("-rw-------", secret.toPermissionsString())
    }

    @Test
    fun s3SigV4Signer_emptyPayloadSha256() {
        val emptyHash = S3SigV4Signer.sha256Hex(ByteArray(0))
        assertEquals(S3SigV4Signer.EMPTY_PAYLOAD_SHA256, emptyHash)
    }

    @Test
    fun s3SigV4Signer_canonicalQuerySorting() {
        val params = mapOf(
            "prefix" to "logs/",
            "max-keys" to "100",
            "delimiter" to "/"
        )
        val canonical = S3SigV4Signer.buildCanonicalQuery(params)
        assertEquals("delimiter=%2F&max-keys=100&prefix=logs%2F", canonical)
    }

    @Test
    fun s3SigV4Signer_generateAuthorizationHeader() {
        val authHeader = S3SigV4Signer.generateAuthorizationHeader(
            method = "GET",
            canonicalUri = "/my-bucket/logs",
            queryParams = mapOf("max-keys" to "50"),
            headers = mapOf(
                "Host" to "s3.us-east-1.amazonaws.com",
                "x-amz-date" to "20261022T120000Z"
            ),
            accessKey = "AKIAEXAMPLEKEY12345",
            secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            region = "us-east-1",
            dateTimeStamp = "20261022T120000Z",
            dateStamp = "20261022"
        )

        assertNotNull(authHeader)
        assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256 Credential=AKIAEXAMPLEKEY12345/20261022/us-east-1/s3/aws4_request"))
        assertTrue(authHeader.contains("SignedHeaders=host;x-amz-date"))
        assertTrue(authHeader.contains("Signature="))
    }

    @Test
    fun storageEngine_normalizePath() {
        assertEquals("/", StorageEngine.normalizePath(""))
        assertEquals("/", StorageEngine.normalizePath("/"))
        assertEquals("/var/log", StorageEngine.normalizePath("/var/log"))
        assertEquals("/var", StorageEngine.normalizePath("/var/log/.."))
        assertEquals("/a/b/c", StorageEngine.normalizePath("///a//b/./c//"))
        assertEquals("/", StorageEngine.normalizePath("/a/.."))
    }

    @Test
    fun storageEngine_cdAndCdUp() {
        val engine = StorageEngine(StorageType.Smb, "nas.local/share")
        assertEquals("/", engine.currentPath.value)

        engine.cd("backups")
        assertEquals("/backups", engine.currentPath.value)
        assertEquals("smb://nas.local/share/backups", engine.currentUri())

        assertTrue(engine.cdUp())
        assertEquals("/", engine.currentPath.value)
        assertFalse(engine.cdUp()) // already at root
    }

    @Test
    fun storageEngine_sortNodes_placesDirectoriesFirst() {
        val fileA = StorageNode("1", "alpha.txt", "/alpha.txt", 100, false, StorageType.Smb)
        val dirZ = StorageNode("2", "zebra", "/zebra", 0, true, StorageType.Smb)
        val dirB = StorageNode("3", "beta", "/beta", 0, true, StorageType.Smb)
        val fileC = StorageNode("4", "charlie.log", "/charlie.log", 200, false, StorageType.Smb)

        val sorted = StorageEngine.sortNodes(listOf(fileA, dirZ, dirB, fileC))
        assertEquals(listOf("beta", "zebra", "alpha.txt", "charlie.log"), sorted.map { it.name })
    }
}
