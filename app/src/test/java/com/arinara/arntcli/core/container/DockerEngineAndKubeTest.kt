// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockerEngineAndKubeTest {

    @Test
    fun dockerRemoteEngine_parseContainersJson() {
        val json = """
            [
              {
                "Id": "8dfafdbc3a401c2264c9d9d4d9b43e5c9b8f2c2b3f1e9c8b7a6d5e4f3a2b1c0d",
                "Names": ["/web-nginx"],
                "Image": "nginx:alpine",
                "ImageID": "sha256:bb27825",
                "Command": "/docker-entrypoint.sh nginx -g 'daemon off;'",
                "Created": 1698000000,
                "State": "running",
                "Status": "Up 4 days",
                "Ports": [
                  { "IP": "0.0.0.0", "PrivatePort": 80, "PublicPort": 8080, "Type": "tcp" }
                ],
                "Mounts": [
                  { "Type": "bind", "Source": "/data/html", "Destination": "/usr/share/nginx/html", "Mode": "ro", "RW": false }
                ],
                "SizeRw": 40960,
                "SizeRootFs": 23456789
              }
            ]
        """.trimIndent()

        val containers = DockerRemoteEngine.parseContainersJson(json)
        assertEquals(1, containers.size)

        val c = containers[0]
        assertEquals("web-nginx", c.displayName)
        assertEquals("8dfafdbc3a40", c.shortId)
        assertEquals(ContainerState.Running, c.state)
        assertTrue(c.isRunning)
        assertEquals(1, c.ports.size)
        assertEquals("8080:80/tcp", c.ports[0].formatDisplay())
        assertEquals(1, c.mounts.size)
        assertFalse(c.mounts[0].rw)
    }

    @Test
    fun dockerRemoteEngine_parseStatsJson() {
        val statsJson = """
            {
              "cpu_stats": {
                "cpu_usage": { "total_usage": 5000000000 },
                "system_cpu_usage": 10000000000,
                "online_cpus": 4
              },
              "precpu_stats": {
                "cpu_usage": { "total_usage": 4500000000 },
                "system_cpu_usage": 9000000000
              },
              "memory_stats": {
                "usage": 209715200,
                "limit": 1073741824,
                "stats": { "cache": 20971520 }
              },
              "networks": {
                "eth0": { "rx_bytes": 10485760, "tx_bytes": 5242880 }
              }
            }
        """.trimIndent()

        val stats = DockerRemoteEngine.parseStatsJson(statsJson, "c-123")
        assertNotNull(stats)
        // deltaCpu = 500M, deltaSystem = 1000M -> (500/1000)*4*100 = 200%
        assertEquals(200.0f, stats!!.cpuUsagePercent, 0.5f)
        // usedMem = 200MB - 20MB cache = 180MB -> 180MB / 1024MB = ~17.57%
        assertEquals(188743680L, stats.memoryUsageBytes)
        assertEquals(17.57f, stats.memoryUsagePercent, 0.5f)
        assertEquals(10485760L, stats.networkRxBytes)
    }

    @Test
    fun podmanLibpodEngine_socketDiscovery() {
        val paths = PodmanLibpodEngine.discoverSocketPaths(1000)
        assertEquals(3, paths.size)
        assertEquals("/run/user/1000/podman/podman.sock", paths[0])
        assertEquals("/run/podman/podman.sock", paths[1])
        assertEquals("/var/run/docker.sock", paths[2])
    }

    @Test
    fun kubeClusterEngine_parseKubeconfigYaml() {
        val yaml = """
            apiVersion: v1
            clusters:
            - cluster:
                certificate-authority-data: Y2EtY2VydC1kYXRh
                server: https://192.168.1.50:6443
              name: homelab-cluster
            contexts:
            - context:
                cluster: homelab-cluster
                namespace: production
                user: cluster-admin
              name: homelab-admin
            current-context: homelab-admin
            users:
            - name: cluster-admin
              user:
                token: eyJhbGciOiJSUzI1NiIsImtpZCI6InRva2VuIn0.test-token
        """.trimIndent()

        val config = KubeClusterEngine.parseKubeconfig(yaml)
        assertNotNull(config)
        assertEquals("https://192.168.1.50:6443", config!!.server)
        assertEquals("Y2EtY2VydC1kYXRh", config.caCertData)
        assertEquals("production", config.namespace)
        assertFalse(config.insecureSkipTls)
        assertEquals("eyJhbGciOiJSUzI1NiIsImtpZCI6InRva2VuIn0.test-token", config.token)
    }

    @Test
    fun kubeClusterEngine_parsePodsJson() {
        val podsJson = """
            {
              "items": [
                {
                  "metadata": { "name": "web-frontend-7f9b8c-1234", "namespace": "default" },
                  "spec": { "nodeName": "worker-node-1" },
                  "status": {
                    "phase": "Running",
                    "podIP": "10.244.1.15",
                    "startTime": "2026-10-22T08:00:00Z",
                    "containerStatuses": [
                      { "name": "nginx", "ready": true, "restartCount": 2 }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val pods = KubeClusterEngine.parsePodsJson(podsJson)
        assertEquals(1, pods.size)
        val p = pods[0]
        assertEquals("web-frontend-7f9b8c-1234", p.name)
        assertEquals("Running", p.statusPhase)
        assertTrue(p.isRunning)
        assertTrue(p.isReady)
        assertEquals(2, p.restartCount)
        assertEquals("10.244.1.15", p.podIp)
        assertEquals("worker-node-1", p.nodeName)
    }
}
