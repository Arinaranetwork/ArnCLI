// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.sftp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SFTP remote navigation and directory state manager.
 */
class SftpEngine(
    initialPath: String = "/home/user"
) {

    private val _currentPath = MutableStateFlow(normalizePath(initialPath))
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<SftpRemoteFile>>(emptyList())
    val files: StateFlow<List<SftpRemoteFile>> = _files.asStateFlow()

    companion object {
        fun normalizePath(path: String): String {
            val clean = path.trim()
            if (clean.isEmpty() || clean == "/") return "/"
            val segments = clean.split('/').filter { it.isNotBlank() && it != "." }
            val resolved = mutableListOf<String>()
            for (seg in segments) {
                if (seg == "..") {
                    if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
                } else {
                    resolved.add(seg)
                }
            }
            return "/" + resolved.joinToString("/")
        }

        fun sortFiles(files: List<SftpRemoteFile>): List<SftpRemoteFile> {
            return files.sortedWith(
                compareByDescending<SftpRemoteFile> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    init {
        loadDefaultSampleFiles(_currentPath.value)
    }

    fun cd(directoryName: String) {
        val target = if (_currentPath.value == "/") {
            "/$directoryName"
        } else {
            "${_currentPath.value}/$directoryName"
        }
        val normalized = normalizePath(target)
        _currentPath.value = normalized
        loadDefaultSampleFiles(normalized)
    }

    fun cdUp(): Boolean {
        if (_currentPath.value == "/") return false
        val parent = normalizePath("${_currentPath.value}/..")
        _currentPath.value = parent
        loadDefaultSampleFiles(parent)
        return true
    }

    fun setDirectoryFiles(fileList: List<SftpRemoteFile>) {
        _files.value = sortFiles(fileList)
    }

    private fun loadDefaultSampleFiles(path: String) {
        val sample = when (path) {
            "/", "/root" -> listOf(
                SftpRemoteFile("etc", "$path/etc", 4096, SftpFileType.Directory, 493),
                SftpRemoteFile("home", "$path/home", 4096, SftpFileType.Directory, 493),
                SftpRemoteFile("var", "$path/var", 4096, SftpFileType.Directory, 493),
                SftpRemoteFile("boot.log", "$path/boot.log", 12450, SftpFileType.Regular, 420)
            )
            "/home/user" -> listOf(
                SftpRemoteFile(".ssh", "$path/.ssh", 4096, SftpFileType.Directory, 448),
                SftpRemoteFile("projects", "$path/projects", 4096, SftpFileType.Directory, 493),
                SftpRemoteFile("docker-compose.yml", "$path/docker-compose.yml", 1850, SftpFileType.Regular, 420),
                SftpRemoteFile("server.conf", "$path/server.conf", 412, SftpFileType.Regular, 420)
            )
            else -> listOf(
                SftpRemoteFile("config.json", "$path/config.json", 980, SftpFileType.Regular, 420),
                SftpRemoteFile("data", "$path/data", 4096, SftpFileType.Directory, 493)
            )
        }
        setDirectoryFiles(sample)
    }
}
