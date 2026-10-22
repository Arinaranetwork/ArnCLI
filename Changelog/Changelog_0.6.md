# Changelog - ArnTCLI 0.6

## [0.6.0-Beta] - 2026-10-22

### Added
- Multi-Protocol Remote Storage Engine (`StorageEngine`, `StorageNode`, `StorageType`) supporting SMB 2/3 shares, WebDAV servers, and AWS S3 / MinIO buckets.
- Lightweight AWS SigV4 Canonical Signature Engine (`S3SigV4Signer`) for signing S3 REST queries with zero AWS SDK bloat.
- Interactive Remote Storage Browser (`StorageBrowserSheet`) in Sessions and Space modules with breadcrumb navigation and POSIX permission inspection.
- Mobile VNC / Remote Desktop Controller (`VncSessionController`) with tactile virtual trackpad gesture mapping and modifier controls.
- Comprehensive unit test suites for storage URI resolution, S3 SigV4 generation, and virtual pointer delta math.
