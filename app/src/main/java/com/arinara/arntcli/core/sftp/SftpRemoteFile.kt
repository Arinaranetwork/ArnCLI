// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.sftp

import java.util.Locale

/**
 * File type categories supported by the SFTP subsystem.
 */
enum class SftpFileType {
    Regular,
    Directory,
    Symlink,
    Other
}

/**
 * Data model representing a remote filesystem node retrieved via SFTP.
 */
data class SftpRemoteFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val type: SftpFileType,
    val permissionsOctal: Int = 493, // 0755 in decimal
    val modifiedTimestamp: Long = System.currentTimeMillis(),
    val targetLink: String? = null
) {
    init {
        require(name.isNotBlank()) { "Filename cannot be blank" }
        require(path.isNotBlank()) { "File path cannot be blank" }
        require(sizeBytes >= 0) { "File size cannot be negative (was $sizeBytes)" }
    }

    val isDirectory: Boolean get() = type == SftpFileType.Directory

    /**
     * Returns human-readable size string (e.g. "4.2 KB", "18.5 MB").
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
     * Decodes integer mode into standard POSIX symbolic permissions string (e.g. "-rwxr-xr-x").
     */
    fun toPermissionsString(): String {
        val typeChar = when (type) {
            SftpFileType.Directory -> 'd'
            SftpFileType.Symlink -> 'l'
            SftpFileType.Regular -> '-'
            SftpFileType.Other -> 'c'
        }

        val mode = permissionsOctal and 0x1FF // lowest 9 bits
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

    /**
     * Formats permissions as octal string (e.g. "0755").
     */
    fun toOctalString(): String {
        val oct = permissionsOctal and 0x1FF
        return "0" + Integer.toOctalString(oct)
    }
}
