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

    fun cd(directoryName: String) {
        val target = when {
            directoryName.startsWith("/") -> directoryName
            _currentPath.value == "/" -> "/$directoryName"
            else -> "${_currentPath.value}/$directoryName"
        }
        val normalized = normalizePath(target)
        _currentPath.value = normalized
    }

    fun cdUp(): Boolean {
        if (_currentPath.value == "/") return false
        val parent = normalizePath("${_currentPath.value}/..")
        _currentPath.value = parent
        return true
    }

    fun setDirectoryFiles(fileList: List<SftpRemoteFile>) {
        _files.value = sortFiles(fileList)
    }

    fun clearDirectoryFiles() {
        _files.value = emptyList()
    }
}
