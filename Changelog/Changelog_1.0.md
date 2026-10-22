# Changelog - ArnTCLI 1.0

All notable changes to the ArnTCLI workstation for the 1.0 major release line.

## 1.0.0 - 2026-10-22
### Added
- **Interactive Terminal Macro Key Bar**: Sticky `CTRL` and `ALT` latches, single-tap `ESC`, `TAB`, `|`, `~`, `/`, and VT100 ANSI navigation arrows (`\u001b[A`..`\u001b[D`).
- **Snippet Parameter Interpolation Engine**: In-memory dynamic variable substitution (`{{host}}`, `{{user}}`, `{{port}}`, `{{space}}`) and secure Vault secret resolution (`{{vault_secret:key}}`).
- **Terminal Automation & Runbook Engine**: Sequential cross-session execution with regex assertion pattern matching, step delays, timeout guards, and retry/halt branching.
- **Zero-GC Ring Buffer Session Multiplexer**: Dedicated 5,000-line circular buffer (`CircularLogBuffer`) per terminal session eliminating memory spikes during streaming output.
- **Containers & Cloud-Native (Phase 9)**: Docker Remote REST API, stateful 8-byte binary demux accumulator, rootless Podman Libpod adapter, and lightweight in-situ Kubernetes pod inspector.
- **Server Health & Watchdogs (Phase 8)**: Agentless Linux `/proc` multiplexed telemetry over SSH, multi-probe uptime watchdog ($N=2$ flap damping), and SSL certificate expiry monitor.
- **Dev & Protocol Lab (Phase 7)**: Mini-Postman REST/GraphQL client, real-time WebSocket duplex tester, MQTT IoT broker client, and BLE GATT scanner.
- **Remote Storage & Desktops (Phase 6)**: Multi-protocol storage explorer (SFTP, WebDAV, SMB, S3 SigV4) and remote desktop viewer (VNC / RDP).
- **Secure Tunnels & Air-Gapped Sync (Phase 4 & 5)**: WireGuard profile parser, Local/Remote/Dynamic port forwarding engine, and air-gapped QR sync (BC-UR).
- **Network Reconnaissance & Pocket Diagnostics (Phase 2 & 3)**: ICMP ping & jitter, DNS resolver, CIDR subnet calculator, Wi-Fi inspector, SSL inspector, Wake-on-LAN, TCP port scanner, LAN host discovery, and route traceroute.
- **Hardened Production Release**: 100% offline-first architecture with zero telemetry, zero analytics SDKs, and R8 minification optimization.
