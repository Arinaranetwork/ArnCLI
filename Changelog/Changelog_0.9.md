# Changelog - ArnTCLI 0.9

## [0.9.0-Beta] - 2026-10-22

### Added
- Docker Remote API Engine (`DockerRemoteEngine`, `DockerModels`) supporting remote container lifecycle management (Start, Stop, Restart, Pause, Force Kill, Remove), image registry queries, real-time CPU & memory stats calculation, and port/volume mount inspection over SSH UNIX socket forwarders.
- Real-Time Docker 8-Byte Binary Log Demux Parser (`DockerDemuxParser`) cleanly separating stdout and stderr streams with coroutine flow pipelines and ANSI-safe rendering.
- Podman Libpod Adapter (`PodmanLibpodEngine`) supporting rootless socket auto-discovery (`/run/user/1000/podman/podman.sock`) and multi-container Pod inspection.
- Lightweight In-Situ Kubernetes Cluster Engine (`KubeClusterEngine`, `KubeModels`) parsing standard Kubeconfig YAML, pinning cluster CA certificates, and inspecting namespaces, pods, container states, and deployment health without any external Go binaries.
- Interactive Container Explorer and Streaming Log Viewer Sheets (`ContainerExplorerSheet`, `ContainerLogsSheet`) fully adhering to Electric Slate dark theme, spring physics, and 0 `Android.md` violations.
- Comprehensive unit test suites for 8-byte frame demuxing, Docker stats differential calculation, Kubeconfig decoding, and Podman socket discovery.
