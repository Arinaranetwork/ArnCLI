# Changelog - ArnTCLI 0.3

## [0.3.0-Beta] - 2026-10-21

### Added
- Wake-on-LAN magic packet engine (`WolEngine`) and interactive modal sheet (`WolSheet`) in Pocket module, supporting standard 102-byte UDP broadcast transmissions across local subnets and directed IPs.
- Wi-Fi Live Inspector engine (`WifiInspectorEngine`) and interactive sheet (`WifiInspectorSheet`) in Pocket module reporting link speed, RSSI dBm, frequency band (2.4 / 5 / 6 GHz), gateway, and IP properties.
- High-performance on-device TCP Port Scanner (`LanPortScanner`) and interactive sheet (`PortScannerSheet`) probing common infrastructure ports with latency tracking.
- Integration of Wake-on-LAN and Wi-Fi Inspector sheets into `PocketScreen` and `MainScaffold`.
- LAN Host Discovery engine (`LanDiscoveryEngine`) and interactive sheet (`LanDiscoverySheet`) for sweeping local subnets and discovering active hosts.
- Host configuration dialog (`AddHostSheet`) in `SessionsScreen` with direct 1-tap remote terminal session launcher in `TerminalScreen`.
- Unit test suites `WolEngineTest`, `WifiInspectorEngineTest`, `LanPortScannerTest`, and `LanDiscoveryEngineTest`.
