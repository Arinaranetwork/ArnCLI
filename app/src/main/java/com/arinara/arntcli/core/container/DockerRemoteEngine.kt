// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DockerRemoteEngine {

    companion object {
        const val DEFAULT_API_VERSION = "v1.43"

        /**
         * Parses a Docker JSON array of containers from /v1.43/containers/json?all=1
         */
        fun parseContainersJson(jsonString: String): List<ContainerItem> {
            val results = mutableListOf<ContainerItem>()
            if (jsonString.isBlank()) return results

            try {
                val array = Json.parseToJsonElement(jsonString).jsonArray
                for (item in array) {
                    val obj = item.jsonObject
                    val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: ""
                    val image = obj["Image"]?.jsonPrimitive?.contentOrNull ?: ""
                    val imageId = obj["ImageID"]?.jsonPrimitive?.contentOrNull ?: ""
                    val command = obj["Command"]?.jsonPrimitive?.contentOrNull ?: ""
                    val created = obj["Created"]?.jsonPrimitive?.longOrNull ?: 0L

                    val stateStr = when (val stateElem = obj["State"]) {
                        is JsonPrimitive -> stateElem.content
                        is JsonObject -> stateElem["Status"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                        else -> obj["Status"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                    }
                    val statusText = obj["Status"]?.jsonPrimitive?.contentOrNull ?: ""

                    val names = obj["Names"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                    val ports = mutableListOf<ContainerPortMapping>()
                    obj["Ports"]?.jsonArray?.forEach { pElem ->
                        val pObj = pElem.jsonObject
                        val ip = pObj["IP"]?.jsonPrimitive?.contentOrNull ?: "0.0.0.0"
                        val privatePort = pObj["PrivatePort"]?.jsonPrimitive?.intOrNull ?: 0
                        val publicPort = pObj["PublicPort"]?.jsonPrimitive?.intOrNull
                        val type = pObj["Type"]?.jsonPrimitive?.contentOrNull ?: "tcp"
                        if (privatePort > 0) {
                            ports.add(ContainerPortMapping(ip, privatePort, publicPort, type))
                        }
                    }

                    val mounts = mutableListOf<ContainerMount>()
                    obj["Mounts"]?.jsonArray?.forEach { mElem ->
                        val mObj = mElem.jsonObject
                        val type = mObj["Type"]?.jsonPrimitive?.contentOrNull ?: "bind"
                        val src = mObj["Source"]?.jsonPrimitive?.contentOrNull ?: ""
                        val dst = mObj["Destination"]?.jsonPrimitive?.contentOrNull ?: ""
                        val mode = mObj["Mode"]?.jsonPrimitive?.contentOrNull ?: "rw"
                        val rw = mObj["RW"]?.jsonPrimitive?.booleanOrNull ?: true
                        mounts.add(ContainerMount(type, src, dst, mode, rw))
                    }

                    val sizeRw = obj["SizeRw"]?.jsonPrimitive?.longOrNull ?: 0L
                    val sizeRootFs = obj["SizeRootFs"]?.jsonPrimitive?.longOrNull ?: 0L

                    results.add(
                        ContainerItem(
                            id = id,
                            names = names,
                            image = image,
                            imageId = imageId,
                            command = command,
                            created = created,
                            state = ContainerState.fromString(stateStr),
                            statusText = statusText,
                            ports = ports,
                            mounts = mounts,
                            sizeRw = sizeRw,
                            sizeRootFs = sizeRootFs
                        )
                    )
                }
            } catch (_: Exception) {}

            return results
        }

        /**
         * Computes accurate CPU & Memory percentages from Docker /containers/{id}/stats JSON.
         */
        fun parseStatsJson(jsonString: String, containerId: String = ""): ContainerStatsSnapshot? {
            if (jsonString.isBlank()) return null

            try {
                val root = Json.parseToJsonElement(jsonString).jsonObject
                val cpuStats = root["cpu_stats"]?.jsonObject ?: JsonObject(emptyMap())
                val preCpuStats = root["precpu_stats"]?.jsonObject ?: JsonObject(emptyMap())

                val cpuUsage = cpuStats["cpu_usage"]?.jsonObject ?: JsonObject(emptyMap())
                val preCpuUsage = preCpuStats["cpu_usage"]?.jsonObject ?: JsonObject(emptyMap())

                val totalUsage = cpuUsage["total_usage"]?.jsonPrimitive?.longOrNull ?: 0L
                val preTotalUsage = preCpuUsage["total_usage"]?.jsonPrimitive?.longOrNull ?: 0L

                val systemUsage = cpuStats["system_cpu_usage"]?.jsonPrimitive?.longOrNull ?: 0L
                val preSystemUsage = preCpuStats["system_cpu_usage"]?.jsonPrimitive?.longOrNull ?: 0L

                val onlineCpus = cpuStats["online_cpus"]?.jsonPrimitive?.intOrNull
                    ?: cpuUsage["percpu_usage"]?.jsonArray?.size
                    ?: 1
                val safeOnlineCpus = onlineCpus.coerceAtLeast(1)

                val cpuDelta = totalUsage - preTotalUsage
                val systemDelta = systemUsage - preSystemUsage

                val cpuPercent = if (systemDelta > 0L && cpuDelta > 0L) {
                    ((cpuDelta.toDouble() / systemDelta.toDouble()) * safeOnlineCpus.toDouble() * 100.0).toFloat()
                } else 0f

                val memStats = root["memory_stats"]?.jsonObject ?: JsonObject(emptyMap())
                val rawMemUsage = memStats["usage"]?.jsonPrimitive?.longOrNull ?: 0L
                val memLimit = (memStats["limit"]?.jsonPrimitive?.longOrNull ?: 1L).coerceAtLeast(1L)

                val memSubStats = memStats["stats"]?.jsonObject
                val cache = memSubStats?.get("inactive_file")?.jsonPrimitive?.longOrNull
                    ?: memSubStats?.get("cache")?.jsonPrimitive?.longOrNull
                    ?: 0L
                val actualUsedMem = (rawMemUsage - cache).coerceAtLeast(0L)

                val memPercent = ((actualUsedMem.toDouble() / memLimit.toDouble()) * 100.0).toFloat()

                var rxBytes = 0L
                var txBytes = 0L
                root["networks"]?.jsonObject?.forEach { (_, netElem) ->
                    val netObj = netElem.jsonObject
                    rxBytes += netObj["rx_bytes"]?.jsonPrimitive?.longOrNull ?: 0L
                    txBytes += netObj["tx_bytes"]?.jsonPrimitive?.longOrNull ?: 0L
                }

                return ContainerStatsSnapshot(
                    containerId = containerId,
                    cpuUsagePercent = cpuPercent.coerceIn(0f, safeOnlineCpus * 100f),
                    memoryUsageBytes = actualUsedMem,
                    memoryLimitBytes = memLimit,
                    memoryUsagePercent = memPercent.coerceIn(0f, 100f),
                    networkRxBytes = rxBytes,
                    networkTxBytes = txBytes
                )
            } catch (_: Exception) {
                return null
            }
        }

        /**
         * Parses Docker Image JSON from /v1.43/images/json
         */
        fun parseImagesJson(jsonString: String): List<DockerImageItem> {
            val results = mutableListOf<DockerImageItem>()
            if (jsonString.isBlank()) return results

            try {
                val array = Json.parseToJsonElement(jsonString).jsonArray
                for (item in array) {
                    val obj = item.jsonObject
                    val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: ""
                    val created = obj["Created"]?.jsonPrimitive?.longOrNull ?: 0L
                    val size = obj["Size"]?.jsonPrimitive?.longOrNull ?: 0L
                    val tags = obj["RepoTags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

                    results.add(
                        DockerImageItem(
                            id = id,
                            repoTags = tags,
                            created = created,
                            sizeBytes = size
                        )
                    )
                }
            } catch (_: Exception) {}

            return results
        }

        /**
         * Parses Docker System DF JSON from /v1.43/system/df
         */
        fun parseSystemDfJson(jsonString: String): DockerSystemDf {
            if (jsonString.isBlank()) {
                return DockerSystemDf(0L, 0L, 0L, 0L, 0L)
            }

            return try {
                val root = Json.parseToJsonElement(jsonString).jsonObject
                var imgTotal = 0L
                var imgReclaim = 0L
                root["Images"]?.jsonArray?.forEach { imgElem ->
                    val img = imgElem.jsonObject
                    val s = img["Size"]?.jsonPrimitive?.longOrNull ?: 0L
                    imgTotal += s
                    if ((img["Containers"]?.jsonPrimitive?.intOrNull ?: 0) == 0) {
                        imgReclaim += s
                    }
                }

                var contTotal = 0L
                root["Containers"]?.jsonArray?.forEach { contElem ->
                    contTotal += contElem.jsonObject["SizeRw"]?.jsonPrimitive?.longOrNull ?: 0L
                }

                var volTotal = 0L
                root["Volumes"]?.jsonArray?.forEach { volElem ->
                    val vUsage = volElem.jsonObject["UsageData"]?.jsonObject
                    volTotal += vUsage?.get("Size")?.jsonPrimitive?.longOrNull ?: 0L
                }

                var cacheTotal = 0L
                root["BuildCache"]?.jsonArray?.forEach { bElem ->
                    cacheTotal += bElem.jsonObject["Size"]?.jsonPrimitive?.longOrNull ?: 0L
                }

                DockerSystemDf(imgTotal, imgReclaim, contTotal, volTotal, cacheTotal)
            } catch (_: Exception) {
                DockerSystemDf(0L, 0L, 0L, 0L, 0L)
            }
        }
    }

    /**
     * Executes a remote Docker API call through the specified base HTTP URL (e.g. forwarder).
     */
    suspend fun executeAction(
        baseUrl: String,
        containerId: String,
        action: String, // start, stop, restart, pause, unpause, kill
        apiVersion: String = DEFAULT_API_VERSION,
        timeoutMs: Int = 10000
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val endpoint = "${baseUrl.trimEnd('/')}/$apiVersion/containers/$containerId/$action"
            val url = URL(endpoint)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "POST"
            conn.doOutput = false

            val code = conn.responseCode
            code in 200..304
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Fetches raw container logs buffer for demuxing.
     */
    suspend fun fetchLogs(
        baseUrl: String,
        containerId: String,
        tailLines: Int = 200,
        apiVersion: String = DEFAULT_API_VERSION,
        timeoutMs: Int = 10000
    ): ByteArray = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val endpoint = "${baseUrl.trimEnd('/')}/$apiVersion/containers/$containerId/logs?stdout=true&stderr=true&tail=$tailLines&timestamps=true"
            val url = URL(endpoint)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"

            if (conn.responseCode in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else {
                ByteArray(0)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ByteArray(0)
        } finally {
            conn?.disconnect()
        }
    }
}
