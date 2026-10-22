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

    init {
        loadSampleData(_currentPath.value)
    }

    fun setStorageType(type: StorageType) {
        _storageType.value = type
        _currentPath.value = "/"
        loadSampleData("/")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cd(directoryName: String) {
        val target = if (_currentPath.value == "/") {
            "/$directoryName"
        } else {
            "${_currentPath.value}/$directoryName"
        }
        val normalized = normalizePath(target)
        _currentPath.value = normalized
        loadSampleData(normalized)
    }

    fun cdUp(): Boolean {
        if (_currentPath.value == "/") return false
        val parent = normalizePath("${_currentPath.value}/..")
        _currentPath.value = parent
        loadSampleData(parent)
        return true
    }

    fun currentUri(): String {
        val scheme = _storageType.value.scheme
        val path = if (_currentPath.value == "/") "" else _currentPath.value
        return "$scheme://$endpoint$path"
    }

    private fun loadSampleData(path: String) {
        val type = _storageType.value
        val sample = when (type) {
            StorageType.Smb -> when (path) {
                "/" -> listOf(
                    StorageNode("smb-1", "backups", "/backups", 0, true, type, 493),
                    StorageNode("smb-2", "docker-volumes", "/docker-volumes", 0, true, type, 493),
                    StorageNode("smb-3", "media", "/media", 0, true, type, 493),
                    StorageNode("smb-4", "server-setup.sh", "/server-setup.sh", 2450, false, type, 493)
                )
                "/backups" -> listOf(
                    StorageNode("smb-5", "postgres-daily.sql.gz", "$path/postgres-daily.sql.gz", 125_829_120, false, type, 420),
                    StorageNode("smb-6", "vault-export.json.enc", "$path/vault-export.json.enc", 12_400, false, type, 384)
                )
                else -> listOf(
                    StorageNode("smb-7", "notes.txt", "$path/notes.txt", 1024, false, type, 420)
                )
            }
            StorageType.S3 -> when (path) {
                "/" -> listOf(
                    StorageNode("s3-1", "production-logs", "/production-logs", 0, true, type, 493),
                    StorageNode("s3-2", "user-assets", "/user-assets", 0, true, type, 493),
                    StorageNode("s3-3", "terraform.tfstate", "/terraform.tfstate", 35_800, false, type, 420, etag = "\"9b2c1a8f9\"")
                )
                "/production-logs" -> listOf(
                    StorageNode("s3-4", "app-2026-10-22.log.zst", "$path/app-2026-10-22.log.zst", 45_200_000, false, type, 420, etag = "\"4d1e2f3a\""),
                    StorageNode("s3-5", "audit-events.json", "$path/audit-events.json", 8_900_000, false, type, 420, etag = "\"7c8b9a0d\"")
                )
                else -> listOf(
                    StorageNode("s3-6", "manifest.json", "$path/manifest.json", 512, false, type, 420)
                )
            }
            StorageType.WebDAV -> listOf(
                StorageNode("dav-1", "Documents", "$path/Documents", 0, true, type, 493),
                StorageNode("dav-2", "Photos", "$path/Photos", 0, true, type, 493),
                StorageNode("dav-3", "Nextcloud-Manual.pdf", "$path/Nextcloud-Manual.pdf", 4_200_000, false, type, 420)
            )
            StorageType.Nfs -> listOf(
                StorageNode("nfs-1", "export", "$path/export", 0, true, type, 493),
                StorageNode("nfs-2", "shared-data", "$path/shared-data", 0, true, type, 493)
            )
        }
        _nodes.value = sortNodes(sample)
    }
}
