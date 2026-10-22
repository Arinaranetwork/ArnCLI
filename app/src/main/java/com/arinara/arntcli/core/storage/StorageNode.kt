// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.storage

import java.util.Locale

/**
 * Supported remote storage protocol types.
 */
enum class StorageType(val scheme: String, val displayName: String, val defaultPort: Int) {
    Smb("smb", "Windows Share (SMB 2/3)", 445),
    S3("s3", "Amazon S3 / MinIO", 443),
    WebDAV("webdav", "WebDAV / Nextcloud", 443),
    Nfs("nfs", "Network File System (NFS)", 2049)
}

/**
 * Data node representing a remote file, object, or directory.
 */
data class StorageNode(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val storageType: StorageType,
    val permissionsOctal: Int = 493, // 0755
    val lastModified: Long = System.currentTimeMillis(),
    val etag: String? = null,
    val mimeType: String? = null
) {
    init {
        require(name.isNotBlank()) { "Node name cannot be blank" }
        require(path.isNotBlank()) { "Node path cannot be blank" }
        require(sizeBytes >= 0) { "Size cannot be negative (was $sizeBytes)" }
    }

    /**
     * Formats node byte size into human-readable string (e.g. "<DIR>", "4.5 KB", "1.2 GB").
     */
    fun formattedSize(): String {
        if (isDirectory) return "<DIR>"
        if (sizeBytes < 1024) return "$sizeBytes B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    /**
     * Converts permissions integer into POSIX symbolic representation (e.g. "drwxr-xr-x").
     */
    fun toPermissionsString(): String {
        val typeChar = if (isDirectory) 'd' else '-'
        val mode = permissionsOctal and 0x1FF
        val r1 = if (mode and 256 != 0) 'r' else '-'
        val w1 = if (mode and 128 != 0) 'w' else '-'
        val x1 = if (mode and 64 != 0) 'x' else '-'

        val r2 = if (mode and 32 != 0) 'r' else '-'
        val w2 = if (mode and 16 != 0) 'w' else '-'
        val x2 = if (mode and 8 != 0) 'x' else '-'

        val r3 = if (mode and 4 != 0) 'r' else '-'
        val w3 = if (mode and 2 != 0) 'w' else '-'
        val x3 = if (mode and 1 != 0) 'x' else '-'

        return "$typeChar$r1$w1$x1$r2$w2$x2$r3$w3$x3"
    }
}
