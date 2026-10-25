# Phase 9 - ContainersAndCloudNative
## Goal
Provide a native, lightweight mobile workstation for inspecting and orchestrating Docker containers, Podman pods, and Kubernetes workloads over SSH and UNIX domain sockets with zero heavy cgo/Go binaries and 100% offline-first reliability.

## Scope
- `DockerRemoteEngine`: UNIX domain socket (`/var/run/docker.sock`) and streamlocal port-forward client implementing container lifecycle (Start, Stop, Restart, Pause, Kill, Remove), image inspection, and disk usage reclamation (`/v1.43/system/df`).
- `DockerDemuxParser`: 8-byte binary frame demuxing engine for streaming stdout and stderr logs without TTY corruption.
- `PodmanLibpodEngine`: Rootless Podman detection (`/run/user/<UID>/podman/podman.sock`), pod grouping, and Libpod REST API adapter.
- `KubeClusterEngine`: Lightweight in-situ Kubeconfig parser (`~/.kube/config`), client mTLS/Bearer token authentication, namespace query, Pod inspector, and Deployment status monitor.
- `ContainerExplorerSheet`: Full-featured container, pod, and service explorer in Electric Slate dark theme with 0 `Android.md` violations.
- `ContainerLogsSheet`: Real-time streaming log viewer with Monospace font, stream filters (stdout/stderr), and auto-scroll.
- Comprehensive unit test suites for 8-byte binary demuxing, Docker JSON parsing, Kubeconfig decoding, and Podman socket discovery.

## Out Of Scope
- Compiling heavy `client-go` or Go-compiled shared libraries (`.so`).
- Building Docker images locally on Android (inspection and remote orchestration only).
- Running nested rootful container daemons directly on Android OS.

## Features
### Docker & Podman Lifecycle Management
- Start, Stop (SIGTERM with 10s fallback), Restart, Pause, Unpause, Force Kill, and Remove containers.
- Real-time CPU & memory stats calculation with delta total vs delta system usage formulas.
- Port mappings and volume mount inspections.

### Real-Time 8-Byte Binary Log Demuxing
- Decodes Docker's binary header: `[stream_type (1=stdout, 2=stderr), 0, 0, 0, size_b1, size_b2, size_b3, size_b4]`.
- Non-blocking coroutine flow streaming with memory-bounded ring buffer (5,000 lines max).

### Kubernetes In-Situ Cluster Inspector
- Parses cluster CA, client cert, key, and auth token directly from standard Kubeconfig YAML.
- Custom `SSLContext` pinning cluster CA without touching Android system trust store.
- List namespaces, pods, container states, restart counts, and deployment rollout health.

## UI Mockup
```
+-------------------------------------------------------+
| Docker & K8s: prod-swarm-01                [ Refresh ]|
| [ All (8) ]  [ Running (6) ]  [ Stopped (2) ]          |
+-------------------------------------------------------+
| [● Running] web-nginx:alpine                          |
|  ID: 3a9f01bc  •  Up 6 days  •  Ports: 80:80, 443:443  |
|  CPU: 1.2%  •  RAM: 18.4 MB / 1.0 GB (1.8%)           |
|  [ Restart ]   [ Logs ]   [ Stop ]                    |
+-------------------------------------------------------+
| [● Running] redis-cluster-node-1                      |
|  ID: 88c21a0f  •  Up 14 days •  Ports: 6379:6379      |
|  CPU: 0.4%  •  RAM: 42.1 MB / 2.0 GB (2.1%)           |
|  [ Restart ]   [ Logs ]   [ Stop ]                    |
+-------------------------------------------------------+
```

## Logic Notes
- Communicates through existing `PortForwardingEngine` or SSH direct-streamlocal forwarding to local ephemeral ports.
- Backpressure-regulated log flows pause immediately when sheets are dismissed or minimized.

## Risks
- Target machine without Docker socket permissions (`EACCES`) -> Mitigation: prompt with one-tap copyable `sudo usermod -aG docker $USER` command.
- Self-signed Kubernetes cluster certificates -> Mitigation: explicit in-memory X.509 TrustManager constructed from Kubeconfig `certificate-authority-data`.

## Dependencies
- Standard OkHttp 4.12, Kotlin Coroutines, Jetpack Compose.

## Acceptance Criteria
- `DockerDemuxParser` separates stdout and stderr frames without losing bytes.
- `DockerRemoteEngine` correctly parses container JSON and computes CPU/RAM percentages.
- `KubeClusterEngine` parses standard Kubeconfig YAML and queries Pods.
- UI passes `verifyAndroidRules` with 0 violations.
