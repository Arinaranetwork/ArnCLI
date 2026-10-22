// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.sftp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SftpEngineTest {

    @Test
    fun sftpRemoteFile_formattedSize() {
        val dir = SftpRemoteFile("var", "/var", 4096, SftpFileType.Directory)
        assertEquals("<DIR>", dir.formattedSize())

        val smallFile = SftpRemoteFile("test.txt", "/test.txt", 512, SftpFileType.Regular)
        assertEquals("512 B", smallFile.formattedSize())

        val kbFile = SftpRemoteFile("image.png", "/image.png", 2048, SftpFileType.Regular)
        assertEquals("2.0 KB", kbFile.formattedSize())

        val mbFile = SftpRemoteFile("archive.tar.gz", "/archive.tar.gz", 10_485_760, SftpFileType.Regular)
        assertEquals("10.0 MB", mbFile.formattedSize())
    }

    @Test
    fun sftpRemoteFile_permissionsFormatting() {
        // 0755 in decimal is 493 -> rwxr-xr-x
        val dir = SftpRemoteFile("etc", "/etc", 4096, SftpFileType.Directory, permissionsOctal = 493)
        assertEquals("drwxr-xr-x", dir.toPermissionsString())
        assertEquals("0755", dir.toOctalString())

        // 0644 in decimal is 420 -> rw-r--r--
        val regular = SftpRemoteFile("file.conf", "/file.conf", 100, SftpFileType.Regular, permissionsOctal = 420)
        assertEquals("-rw-r--r--", regular.toPermissionsString())
        assertEquals("0644", regular.toOctalString())

        // 0600 in decimal is 384 -> rw-------
        val key = SftpRemoteFile("id_rsa", "/id_rsa", 1800, SftpFileType.Regular, permissionsOctal = 384)
        assertEquals("-rw-------", key.toPermissionsString())
        assertEquals("0600", key.toOctalString())
    }

    @Test
    fun sftpEngine_normalizePath() {
        assertEquals("/", SftpEngine.normalizePath("/"))
        assertEquals("/var/log", SftpEngine.normalizePath("/var/log/"))
        assertEquals("/var", SftpEngine.normalizePath("/var/log/.."))
        assertEquals("/", SftpEngine.normalizePath("/var/.."))
        assertEquals("/home/user/docs", SftpEngine.normalizePath("/home/user/./docs"))
    }

    @Test
    fun sftpEngine_navigation() {
        val engine = SftpEngine(initialPath = "/home")
        assertEquals("/home", engine.currentPath.value)

        engine.cd("user")
        assertEquals("/home/user", engine.currentPath.value)

        assertTrue(engine.cdUp())
        assertEquals("/home", engine.currentPath.value)

        assertTrue(engine.cdUp())
        assertEquals("/", engine.currentPath.value)

        assertFalse(engine.cdUp()) // Already at root
        assertEquals("/", engine.currentPath.value)
    }

    @Test
    fun sftpEngine_sortingDirectoriesFirst() {
        val files = listOf(
            SftpRemoteFile("zebra.txt", "/zebra.txt", 100, SftpFileType.Regular),
            SftpRemoteFile("apple.txt", "/apple.txt", 100, SftpFileType.Regular),
            SftpRemoteFile("bin", "/bin", 4096, SftpFileType.Directory),
            SftpRemoteFile("etc", "/etc", 4096, SftpFileType.Directory)
        )

        val sorted = SftpEngine.sortFiles(files)
        assertEquals("bin", sorted[0].name)
        assertEquals("etc", sorted[1].name)
        assertEquals("apple.txt", sorted[2].name)
        assertEquals("zebra.txt", sorted[3].name)
    }
}
