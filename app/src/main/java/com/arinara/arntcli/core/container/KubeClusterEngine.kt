// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KubeClusterEngine {

    companion object {

        /**
         * Parses standard ~/.kube/config YAML format without requiring external YAML libraries.
         */
        fun parseKubeconfig(yamlText: String): KubeClusterConfig? {
            if (yamlText.isBlank()) return null

            var server = ""
            var caData: String? = null
            var insecure = false
            var token: String? = null
            var clientCertData: String? = null
            var clientKeyData: String? = null
            var namespace = "default"

            val lines = yamlText.lines()
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("server:") -> {
                        server = trimmed.substringAfter("server:").trim().trim('"', '\'')
                    }
                    trimmed.startsWith("certificate-authority-data:") -> {
                        caData = trimmed.substringAfter("certificate-authority-data:").trim().trim('"', '\'')
                    }
                    trimmed.startsWith("insecure-skip-tls-verify:") -> {
                        insecure = trimmed.substringAfter("insecure-skip-tls-verify:").trim().toBoolean()
                    }
                    trimmed.startsWith("token:") -> {
                        token = trimmed.substringAfter("token:").trim().trim('"', '\'')
                    }
                    trimmed.startsWith("client-certificate-data:") -> {
                        clientCertData = trimmed.substringAfter("client-certificate-data:").trim().trim('"', '\'')
                    }
                    trimmed.startsWith("client-key-data:") -> {
                        clientKeyData = trimmed.substringAfter("client-key-data:").trim().trim('"', '\'')
                    }
                    trimmed.startsWith("namespace:") -> {
                        namespace = trimmed.substringAfter("namespace:").trim().trim('"', '\'')
                    }
                }
            }

            if (server.isBlank()) return null

            return KubeClusterConfig(
                server = server,
                caCertData = caData,
                insecureSkipTls = insecure,
                token = token,
                clientCertData = clientCertData,
                clientKeyData = clientKeyData,
                namespace = namespace
            )
        }

        /**
         * Parses Kubernetes /api/v1/pods JSON response.
         */
        fun parsePodsJson(jsonString: String): List<KubePodItem> {
            val results = mutableListOf<KubePodItem>()
            if (jsonString.isBlank()) return results

            try {
                val root = Json.parseToJsonElement(jsonString).jsonObject
                val items = root["items"]?.jsonArray ?: JsonArray(emptyList())

                for ((i, itemElem) in items.withIndex()) {
                    val pod = itemElem.jsonObject
                    val metadata = pod["metadata"]?.jsonObject ?: JsonObject(emptyMap())
                    val status = pod["status"]?.jsonObject ?: JsonObject(emptyMap())
                    val spec = pod["spec"]?.jsonObject ?: JsonObject(emptyMap())

                    val name = metadata["name"]?.jsonPrimitive?.contentOrNull ?: "pod-$i"
                    val ns = metadata["namespace"]?.jsonPrimitive?.contentOrNull ?: "default"
                    val phase = status["phase"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                    val podIp = status["podIP"]?.jsonPrimitive?.contentOrNull?.ifBlank { null }
                    val nodeName = spec["nodeName"]?.jsonPrimitive?.contentOrNull?.ifBlank { null }
                    val startTime = status["startTime"]?.jsonPrimitive?.contentOrNull?.ifBlank { null }

                    var restartCount = 0
                    var readyContainers = 0
                    val containerStatuses = status["containerStatuses"]?.jsonArray
                    val totalContainers = containerStatuses?.size
                        ?: spec["containers"]?.jsonArray?.size
                        ?: 1

                    if (containerStatuses != null) {
                        for (cElem in containerStatuses) {
                            val cStatus = cElem.jsonObject
                            restartCount += cStatus["restartCount"]?.jsonPrimitive?.intOrNull ?: 0
                            if (cStatus["ready"]?.jsonPrimitive?.booleanOrNull == true) {
                                readyContainers++
                            }
                        }
                    } else if (phase.equals("Running", ignoreCase = true)) {
                        readyContainers = totalContainers
                    }

                    results.add(
                        KubePodItem(
                            name = name,
                            namespace = ns,
                            statusPhase = phase,
                            podIp = podIp,
                            nodeName = nodeName,
                            restartCount = restartCount,
                            readyContainers = readyContainers,
                            totalContainers = totalContainers,
                            startTime = startTime
                        )
                    )
                }
            } catch (_: Exception) {}

            return results
        }

        /**
         * Parses Kubernetes /apis/apps/v1/deployments JSON response.
         */
        fun parseDeploymentsJson(jsonString: String): List<KubeDeploymentItem> {
            val results = mutableListOf<KubeDeploymentItem>()
            if (jsonString.isBlank()) return results

            try {
                val root = Json.parseToJsonElement(jsonString).jsonObject
                val items = root["items"]?.jsonArray ?: JsonArray(emptyList())

                for ((i, itemElem) in items.withIndex()) {
                    val dep = itemElem.jsonObject
                    val metadata = dep["metadata"]?.jsonObject ?: JsonObject(emptyMap())
                    val status = dep["status"]?.jsonObject ?: JsonObject(emptyMap())
                    val spec = dep["spec"]?.jsonObject ?: JsonObject(emptyMap())

                    val name = metadata["name"]?.jsonPrimitive?.contentOrNull ?: "deployment-$i"
                    val ns = metadata["namespace"]?.jsonPrimitive?.contentOrNull ?: "default"
                    val replicas = spec["replicas"]?.jsonPrimitive?.intOrNull ?: 1
                    val readyReplicas = status["readyReplicas"]?.jsonPrimitive?.intOrNull ?: 0
                    val updatedReplicas = status["updatedReplicas"]?.jsonPrimitive?.intOrNull ?: 0
                    val availableReplicas = status["availableReplicas"]?.jsonPrimitive?.intOrNull ?: 0

                    val images = mutableListOf<String>()
                    val template = spec["template"]?.jsonObject
                    val podSpec = template?.get("spec")?.jsonObject
                    val containers = podSpec?.get("containers")?.jsonArray
                    if (containers != null) {
                        for (cElem in containers) {
                            val cObj = cElem.jsonObject
                            val img = cObj["image"]?.jsonPrimitive?.contentOrNull ?: ""
                            if (img.isNotBlank()) images.add(img)
                        }
                    }

                    results.add(
                        KubeDeploymentItem(
                            name = name,
                            namespace = ns,
                            replicas = replicas,
                            readyReplicas = readyReplicas,
                            updatedReplicas = updatedReplicas,
                            availableReplicas = availableReplicas,
                            images = images
                        )
                    )
                }
            } catch (_: Exception) {}

            return results
        }
    }
}
