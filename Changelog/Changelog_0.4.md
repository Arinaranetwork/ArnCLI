# Changelog - ArnTCLI 0.4

## [0.4.0-Beta] - 2026-10-22

### Added
- Port Forwarding Tunnel Engine (`TunnelRule` and `PortForwardingEngine`) with support for Local (-L), Remote (-R), and Dynamic SOCKS5 (-D) tunnels with validation and lifecycle tracking.
- Interactive Port Forwarding Manager Sheet (`PortForwardingSheet`) in Sessions / Tunnel module for creating, toggling, and monitoring active forwarding rules.
- Terminal Session Multiplexer (`TerminalSessionBar` in `TerminalScreen`) for multi-session switching, tab management, and zero-drop terminal backgrounding.
- Jump Host / Bastion Proxy configuration in `AddHostSheet` with intermediate proxy chaining.
- Comprehensive unit test suites for tunnel rule validation and session multiplexing state.
