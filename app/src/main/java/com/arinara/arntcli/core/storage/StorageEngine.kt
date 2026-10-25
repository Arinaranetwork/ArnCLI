// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Universal remote storage session engine supporting SMB, S3, and WebDAV.
 */
class StorageEngine(
    val initialType: StorageType = StorageType.Smb,
    val endpoint: String = "nas.home.local/media"
) {

    private val _storageType = MutableStateFlow(initialType)
    val storageType: StateFlow<StorageType> = _storageType.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _nodes = MutableStateFlow<List<StorageNode>>(emptyList())
    val nodes: StateFlow<List<StorageNode>> = _nodes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

        fun sortNodes(nodes: List<StorageNode>): List<StorageNode> {
            return nodes.sortedWith(
                compareByDescending<StorageNode> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    fun setStorageType(type: StorageType) {
        _storageType.value = type
        _currentPath.value = "/"
        _nodes.value = emptyList()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setNodes(nodes: List<StorageNode>) {
        _nodes.value = sortNodes(nodes)
    }

    fun cd(directoryName: String) {
        val target = if (_currentPath.value == "/") {
            "/$directoryName"
        } else {
            "${_currentPath.value}/$directoryName"
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

    fun currentUri(): String {
        val scheme = _storageType.value.scheme
        val path = if (_currentPath.value == "/") "" else _currentPath.value
        return "$scheme://$endpoint$path"
    }
}
