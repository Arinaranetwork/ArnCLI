# Phase 4 - SecureConnectAndTunnels
## Goal
Deliver production-grade remote SSH connection orchestration, background persistent port forwarding (Local, Remote, Dynamic SOCKS5), Jump Host bastion chaining, and multi-session terminal multiplexing within the Electric Slate interface.

## Scope
- SSH Connection Profile & Parameter Engine (`SshConnectionConfig`, `SshAuthMethod`, `SshKeyFormat`).
- Port Forwarding Tunnel Engine (`TunnelRule`, `TunnelType`: Local -L, Remote -R, Dynamic SOCKS5 -D, status tracking, byte accounting).
- Jump Host / Bastion Proxy configuration and chained tunnel resolution.
- Interactive Tunnel Manager Sheet (`PortForwardingSheet`) allowing creation, toggle activation, and live throughput monitoring.
- Terminal Session Multiplexer (`TerminalSessionBar` in `TerminalScreen`) supporting fluid switching between multiple active sessions, session naming, and clean process termination.
- Unit tests for tunnel rule parsing, configuration validation, and session state management.
- Zero `Android.md` violations and adherence to battery-friendly non-blocking coroutines.

## Out Of Scope
- WireGuard userspace TUN engine (Phase 5).
- Graphical RDP/VNC frame rendering (Phase 6).
- External cloud synchronization or remote relays.

## Features
### SSH Connection Configuration & Authentication
Configures remote terminal sessions with flexible credentials:
- Authentication via Password, Vault-stored SSH Key (Ed25519/RSA), or interactive keyboard-interactive.
- Jump Host / Bastion chaining: specifying an intermediate host through which the target server is accessed.
- Keepalive interval pacing (configurable 10-60s) to prevent cellular/Wi-Fi NAT drops.

### Persistent Port Forwarding Engine
Manages three standardized tunnel typologies:
- **Local Forwarding (-L)**: Forwards local Android port `127.0.0.1:localPort` to remote server or private subnet `remoteHost:remotePort`.
- **Remote Forwarding (-R)**: Exposes a local Android service to a remote listening port on the server.
- **Dynamic SOCKS5 (-D)**: Creates a local SOCKS5 proxy server on Android routing arbitrary browser/app traffic through the encrypted SSH tunnel.

### Terminal Session Multiplexer
Enhances the terminal experience to handle simultaneous tasks:
- Multi-session tab bar / floating capsule dock at the top of the terminal viewport.
- 1-tap switching between active sessions without dropping connection sockets or clearing screen buffers.
- Session indicator pills showing connection state (Connected, Disconnected, Reconnecting).
- Dedicated "+ New Session" launcher directly from the multiplexer bar.

## UI Mockup
```
+-------------------------------------------------------+
| [ local:sh ]  [ * srv-db ]  [ web-prod ]     [ + ]    |
+-------------------------------------------------------+
| $ ssh user@10.0.0.5 -L 5432:localhost:5432            |
| Port forwarding active: 127.0.0.1:5432 -> :5432       |
| Authenticated via id_ed25519 [Vault]                  |
| 10.0.0.5:~$ _                                         |
+-------------------------------------------------------+
| [Esc] [Tab] [Ctrl] [Alt] [/] [-] [|] [^] [v] [<] [>]  |
+-------------------------------------------------------+
```

## Logic Notes
- Tunnel rules use immutable data models with validation: valid port ranges `1..65535`, non-blank target hosts.
- State transitions: `Inactive` -> `Connecting` -> `Active` -> `Error` -> `Terminated`.
- Active tunnels track uptime and transfer counters (bytes sent / bytes received).
- Multiplexer maintains isolated command history and output line buffers (capped at 500 lines per session to prevent memory inflation).

## Risks
- Background tunnel socket killing by Android OEM battery savers -> Mitigation: prepare architecture for foreground service pacing and `SO_KEEPALIVE` keepalive pings.
- Local port collision on Android (port already in use) -> Mitigation: pre-bind socket check and clear error messaging.

## Dependencies
- Standard Java Sockets, Kotlin Coroutines `Dispatchers.IO`, existing `VaultEngine` and `DesignTokens`.

## Acceptance Criteria
- `TunnelRule` data model validates port numbers, hostnames, and tunnel types.
- `PortForwardingEngine` manages tunnel lifecycle states and error handling.
- `PortForwardingSheet` allows adding, editing, toggling, and deleting tunnel rules with Electric Slate presets.
- `TerminalScreen` renders session multiplexer tabs, supports switching active session, adding new sessions, and closing tabs.
- Full unit test coverage passing with 0 `Android.md` violations.
