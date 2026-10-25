# Changelog - ArnTCLI 0.5

## [0.5.0-Beta] - 2026-10-22

### Added
- WireGuard Configuration Parser & Profile Engine (`WireGuardConfig`, `WireGuardInterface`, `WireGuardPeer`, `WireGuardConfigParser`) with support for parsing, validating 32-byte Base64 Curve25519 keys, and exporting standard `wg-quick` `.conf` files.
- Interactive WireGuard Profile Inspector Sheet (`WireGuardSheet`) in Pocket / Tunnel module.
- SFTP Remote File Explorer Engine (`SftpRemoteFile`, `SftpFileType`, `SftpEngine`) with POSIX octal permission decoding (`0755`), human-readable file sizes, and directory breadcrumb management.
- Air-Gapped Vault Sync Engine (`BcUrSyncEngine`) and interactive sheet (`AirGappedSyncSheet`) in Vault module, supporting multipart BC-UR animated QR stream generation with CRC32 integrity verification.
- Unit test coverage for `WireGuardConfigParserTest`, `SftpEngineTest`, and `BcUrSyncEngineTest`.
