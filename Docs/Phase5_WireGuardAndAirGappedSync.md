# Phase 5 - WireGuardAndAirGappedSync
## Goal
Provide native on-device WireGuard configuration management, rootless SFTP remote file management, and air-gapped Vault synchronization via high-density multipart BC-UR fountain codes.

## Scope
- `WireGuardConfigParser`: Parses and validates RFC/standard `wg-quick` `.conf` files (`[Interface]` and `[Peer]` blocks, Curve25519 32-byte Base64 key validation, CIDR ranges, keepalive intervals).
- `WireGuardSheet`: Interactive bottom sheet in Tunnel/Pocket module for viewing, configuring, and verifying WireGuard profiles.
- `SftpEngine` & `SftpRemoteFile`: Data models and parser for SFTP remote directory listings, POSIX permissions (`0755`), file sizes, and breadcrumb navigation.
- `BcUrSyncEngine`: Zero-network air-gapped vault synchronizer encoding encrypted keyring payloads into multipart BC-UR fountain frames (`ur:bytes/<seq>-<total>/...`).
- `AirGappedSyncSheet`: Animated QR stream visualizer and receiver in Vault module.
- Full unit test suites for `WireGuardConfigParser`, `SftpEngine`, and `BcUrSyncEngine`.
- 100% compliance with `Android.md` design tokens, presets, and universal spring physics.

## Out Of Scope
- Kernel-level WireGuard module injection (requires root, strictly prohibited).
- Remote cloud relay synchronization.

## Features
### WireGuard Configuration Engine
Parses standard WireGuard profiles:
- `[Interface]` section: PrivateKey, Address (IPv4/IPv6 CIDR), DNS servers, ListenPort, MTU.
- `[Peer]` section: PublicKey, Endpoint (`host:port`), AllowedIPs, PersistentKeepalive, PresharedKey.
- Validation: Enforces 32-byte Base64 encoded public/private keys and valid port numbers.
- Serializes configurations back to standard `.conf` file format for sharing.

### SFTP Remote File Explorer Architecture
Provides remote filesystem inspection over SSH:
- File metadata: Name, path, size, file type (Regular, Directory, Symlink), last modified date.
- POSIX permissions: Octal and symbolic representations (`drwxr-xr-x` -> `0755`).
- Directory breadcrumbs for fast parent navigation.

### Air-Gapped Vault Sync (BC-UR)
Transmits encrypted credentials between devices using optical QR streams:
- Encodes encrypted vault JSON into partitioned BC-UR URIs (`ur:bytes/1-N/...`).
- Cycles through frames at a steady rate (200-400ms) for high-density camera capture.
- Verifies checksum integrity upon reassembly.

## UI Mockup
```
+-------------------------------------------------------+
| WireGuard Profile Inspector               [ Close ]   |
+-------------------------------------------------------+
| wg0-home.conf                                         |
| Endpoint: vpn.home.arntcli.net:51820                  |
| Assigned IP: 10.8.0.2/24                              |
| Allowed IPs: 0.0.0.0/0 (Full Tunnel)                  |
| Public Key: 8fA2b...d9K1=                             |
| Keepalive: 25s                                        |
| [ Copy Configuration ]  [ Export .conf ]              |
+-------------------------------------------------------+
```

## Logic Notes
- `WireGuardConfigParser` processes line-by-line key-value pairs ignoring comments (`#`) and whitespace.
- `BcUrSyncEngine` partitions payloads into maximum 200-character chunks for camera readability.
- All operations run asynchronously on `Dispatchers.IO`.

## Risks
- Corrupted `.conf` syntax -> Mitigation: line-level validation with descriptive error reporting.
- Camera focal length limits on QR scanning -> Mitigation: configurable frame chunk size and cycle speed.

## Dependencies
- Standard Java Base64, CRC32, Kotlin Coroutines `Dispatchers.IO`.

## Acceptance Criteria
- `WireGuardConfigParser` parses valid `wg-quick` configurations and rejects malformed keys.
- `BcUrSyncEngine` partitions and reassembles payloads with verified checksum equality.
- `SftpRemoteFile` formats file sizes (`KB`, `MB`, `GB`) and decodes POSIX octal permissions.
- Interactive sheets render in Electric Slate dark theme with 0 Android.md violations.
