# Phase 6 - RemoteStorageAndDesktops
## Goal
Provide unified remote file systems and storage integrations (SMB 2/3, WebDAV, AWS S3 / MinIO bucket explorer) and remote desktop connection protocols (VNC and RDP session controllers) within contextual workspaces.

## Scope
- `StorageProvider` domain architecture: Standardized abstractions for remote file listing, upload/download progress streaming, and credential binding.
- `SmbEngine` & `S3BucketEngine`: Pure Kotlin/Android rootless client protocols for accessing network shares and S3-compatible buckets.
- `StorageBrowserSheet`: Interactive multi-cloud/NAS remote explorer bottom sheet supporting folder navigation, file details, and POSIX permissions.
- `VncSessionController`: Virtual trackpad gesture input mapping and VNC RFB frame streaming state machine.
- Comprehensive unit test suites for path resolution, S3 URI parsing, and SMB share formatting.
- 100% adherence to Arinara Electric Slate design tokens and `Android.md`.

## Out Of Scope
- Root-level kernel FUSE filesystem mounts (rootless pure user-space only).
- Proprietary closed-source remote desktop daemons.

## Features
### Multi-Protocol Remote Storage Explorer
Unified explorer across enterprise and homelab protocols:
- **SMB 2/3**: Windows Network Shares (`\\host\share`), Active Directory/NTLM auth, domain workgroups.
- **AWS S3 / MinIO**: Object buckets, prefix listing, object sizes, etags, and metadata.
- **WebDAV**: Nextcloud, ownCloud, and NAS web shares.

### Remote Desktop Session Controller (VNC / RDP)
Mobile-first desktop management:
- Low-latency RFB / RDP frame rendering viewport.
- Virtual trackpad mode with tactile two-finger scrolling, right-click tap, and drag-and-drop.
- Hardware keyboard mapping with mobile modifier bar (Ctrl, Alt, Super/Win, Function keys).

## UI Mockup
```
+-------------------------------------------------------+
| Storage Explorer: Homelab NAS             [ Close ]   |
+-------------------------------------------------------+
| Location: smb://nas.home.local/media/backups          |
| +---------------------------------------------------+ |
| | [DIR]  docker-volumes/                 0755  <DIR> | |
| | [DIR]  postgres-dumps/                 0750  <DIR> | |
| | [FILE] weekly-backup.tar.gz            0644 1.4 GB | |
| | [FILE] environment.env                 0600 4.2 KB | |
| +---------------------------------------------------+ |
| [ Upload File ] [ Download Selected ] [ Refresh ]     |
+-------------------------------------------------------+
```

## Logic Notes
- S3 client signs requests using AWS SigV4 authorization without heavy AWS SDK dependencies.
- Storage transfers use Android Storage Access Framework (SAF) streams with 32 KB chunk pacing.
- Virtual trackpad gesture events convert delta offsets with `SpringPhysics.JigglePressSpec`.

## Risks
- WAN latency on SMB tree walking -> Mitigation: asynchronous chunked directory caching on `Dispatchers.IO`.
- High memory usage on large image or video downloads -> Mitigation: stream directly to SAF Uri without buffering entire payloads in RAM.

## Dependencies
- Standard Java security providers, HmacSHA256, Kotlin Coroutines, Jetpack Compose.

## Acceptance Criteria
- Storage engines correctly parse `smb://`, `s3://`, and `webdav://` URIs.
- `StorageBrowserSheet` renders storage nodes with correct icons, sizes, and permissions in Electric Slate dark theme.
- S3 SigV4 canonical request builder formats deterministic authorization headers matching RFC standards.
- Zero `Android.md` violations.
