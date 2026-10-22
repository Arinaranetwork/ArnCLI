// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class PodmanPodItem(
    val id: String,
    val name: String,
    val status: String,
    val infraId: String = "",
    val cgroup: String = "",
    val created: Long = 0L,
    val containersCount: Int = 0,
    val containerNames: List<String> = emptyList()
) {
    val shortId: String get() = id.take(12)
    val isRunning: Boolean get() = status.equals("Running", ignoreCase = true)
}

class PodmanLibpodEngine {

    companion object {
        /**
         * Resolves standard Podman & Docker socket paths in prioritized discovery order.
         */
        fun discoverSocketPaths(userId: Int = 1000): List<String> {
            return listOf(
                "/run/user/$userId/podman/podman.sock", // Rootless Podman
                "/run/podman/podman.sock",              // Rootful Podman
                "/var/run/docker.sock"                  // Rootful Docker
            )
        }

        /**
         * Parses Libpod /v1.40/libpod/pods/json
         */
        fun parsePodsJson(jsonString: String): List<PodmanPodItem> {
            val results = mutableListOf<PodmanPodItem>()
            if (jsonString.isBlank()) return results

            try {
                val array = Json.parseToJsonElement(jsonString).jsonArray
                for (item in array) {
                    val obj = item.jsonObject
                    val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: ""
                    val name = obj["Name"]?.jsonPrimitive?.contentOrNull ?: ""
                    val status = obj["Status"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val infraId = obj["InfraContainerID"]?.jsonPrimitive?.contentOrNull ?: ""
                    val cgroup = obj["Cgroup"]?.jsonPrimitive?.contentOrNull ?: ""
                    val created = obj["Created"]?.jsonPrimitive?.longOrNull ?: 0L

                    val containerNames = mutableListOf<String>()
                    val contArr = obj["Containers"]?.jsonArray
                    val count = contArr?.size ?: 0

                    contArr?.forEach { cElem ->
                        val cObj = cElem.jsonObject
                        val cName = cObj["Names"]?.jsonPrimitive?.contentOrNull
                            ?: cObj["Id"]?.jsonPrimitive?.contentOrNull
                            ?: ""
                        if (cName.isNotBlank()) containerNames.add(cName.removePrefix("/"))
                    }

                    results.add(
                        PodmanPodItem(
                            id = id,
                            name = name,
                            status = status,
                            infraId = infraId,
                            cgroup = cgroup,
                            created = created,
                            containersCount = count,
                            containerNames = containerNames
                        )
                    )
                }
            } catch (_: Exception) {}

            return results
        }
    }
}
