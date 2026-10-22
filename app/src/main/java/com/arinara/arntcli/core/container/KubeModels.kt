// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

data class KubeClusterConfig(
    val server: String,
    val caCertData: String? = null,
    val insecureSkipTls: Boolean = false,
    val token: String? = null,
    val clientCertData: String? = null,
    val clientKeyData: String? = null,
    val namespace: String = "default"
)

data class KubePodItem(
    val name: String,
    val namespace: String,
    val statusPhase: String,
    val podIp: String? = null,
    val nodeName: String? = null,
    val restartCount: Int = 0,
    val readyContainers: Int = 0,
    val totalContainers: Int = 0,
    val startTime: String? = null
) {
    val isRunning: Boolean get() = statusPhase.equals("Running", ignoreCase = true)
    val isReady: Boolean get() = totalContainers > 0 && readyContainers == totalContainers
}

data class KubeDeploymentItem(
    val name: String,
    val namespace: String,
    val replicas: Int = 1,
    val readyReplicas: Int = 1,
    val updatedReplicas: Int = 1,
    val availableReplicas: Int = 1,
    val images: List<String> = emptyList()
) {
    val isHealthy: Boolean get() = readyReplicas >= replicas && replicas > 0
}
