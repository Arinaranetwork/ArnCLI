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

## 1.0.1 - 2026-10-23
### Fixed
- **Purged Mock & Fake Data**: Eliminated simulated fallback payloads and debug buttons across Wi-Fi (`-58 dBm` fallback removed), Container Explorer (mock containers removed), Storage Engine (dummy SMB/S3/WebDAV files removed), SFTP Engine, and BLE GATT scanner.
- **Transparent Session Routing**: Eliminated ambiguous execution routing where commands clicked under remote SSH banners piped to the local Android device shell (`/system/bin/sh`). Remote sessions now explicitly display remote channel standby notice.
- **Vault Secret Masking**: Added `SnippetInterpolationEngine.interpolateMasked()` to mask sensitive vault credentials (`[SECRET:key]`) in terminal command views and execution logs.
- **Form Error Feedback**: Integrated prominent `AppNoticeBanner` feedback in `AddHostSheet` and `AddCredentialSheet` to prevent silent validation drops.
- **Destructive Action Guards**: Added confirmation cards before permanently deleting vault credentials and clearing container log buffers.
- **Security & Privacy Hardening**: Disabled Android cloud backup (`android:allowBackup="false"`), configured root exclusions in data extraction rules, and scoped internal file provider paths to `arntcli`.
- **Soft Keyboard Insets**: Integrated `.imePadding()` into `AppBottomSheet` to ensure input fields remain unobstructed when the virtual keyboard is open.
- **Enforced Emoji Prohibition**: Enforced strict project-wide emoji prohibition via Gradle `VerifyNoEmojisTask` bound to build and test lifecycles.

## 1.0.2 - 2026-10-23
### Fixed
- **Stateful Shell Execution Engine (`LocalProcessEngine`)**: Replaced stateless root (`/`) process spawning with per-session stateful shell contexts rooted in the app's writable `filesDir/home` sandbox. Added persistent working directory tracking (`cd`, `cd -`, `cd ~`, compound subshell `cd`), persistent environment variables (`export KEY=VAL`, `unset`, `env`, `printenv`), command history (`history`, `history -c`), built-in command discovery (`which`, `type`, `help`), and shell aliases (`ll`, `la`, `cls`).
- **Interactive Terminal Key Bar & Line Editing (`TerminalScreen`, `TerminalKeyBar`)**: Fixed directional arrows (`▲`/`▼` now cycle session command history; `◀`/`▶` move the input text cursor), wired `TAB` to real command and file-path autocomplete (`completeInput`), connected sticky `CTRL` and `ALT` latches to character input (`Ctrl+C`, `Ctrl+L`, `Ctrl+D`, `Ctrl+U`, `Ctrl+K`, `Ctrl+W`, `Ctrl+A`, `Ctrl+E`, `Alt+B`, `Alt+F`), and wired `^C` / Stop button to immediately terminate running foreground processes (`killActiveProcess`).
- **Live CWD Prompt & Host Session Handshake (`MainScaffold`)**: Added live working directory pills and prompt prefixes (`~/... $`), auto-initialized a ready `local:sh` session on launch, and upgraded registered host sessions to perform real TCP reachability/banner probes while binding `$HOST`, `$PORT`, `$TARGET_USER`, and `$PROTOCOL` into an active shell session.
- **Persistent Workspace & Shell State (`WorkspaceStateStore`, `LocalProcessEngine`)**: Implemented atomic on-device disk persistence for Spaces, active Space selection, Hosts, encrypted Vault Credentials, Port Forwarding Tunnel Rules, and open Terminal Sessions (preserving scrollback lines, `cwd`, `previousCwd`, exported `env` variables, and command `history` across app restarts), plus full backup export/import in `SettingsScreen`.
- **SELinux-Safe POSIX & Network-Connected Developer Toolchain (`BuiltinToolbox`)**: Added built-in SELinux-safe POSIX commands (`ls`, `tree`, `cat`, `head`, `tail`, `mkdir`, `touch`, `rm`, `cp`, `mv`, `find`, `grep`, `wc`, `du`, `df`, `chmod`, `zip`, `unzip`, `sha256sum`, `md5sum`, `base64`, `jq`, `curl`, `wget`, `dig`, `nslookup`, `nc`, `nmap`, `ip`, `ifconfig`, `free`) plus network-connected package & runtime managers (`pkg`/`apt`/`apk`/`install`, `node`/`nodejs` v20.17.0, `npm init -y`, `npm install <pkg>` fetching real tarballs from `registry.npmjs.org`, `npx`, `python`/`python3` 3.12.0, `pip install <pkg>` fetching from `pypi.org`, and `git init`/`status`/`clone` fetching from GitHub).
- **Shell Deletion vs. Data Preservation & Live Workspace Explorer (`StorageBrowserSheet`, `ServerTelemetrySheet`)**: Deleting a shell tab now removes only that shell session while preserving all files, directories, `node_modules`, and installed packages in `~/`. Upgraded `StorageBrowserSheet` to a live on-device Workspace File Explorer (`~`) and connected `ServerTelemetrySheet` to real `/proc/meminfo`, `StatFs`, and `NetworkInterface`/`TrafficStats` metrics.
