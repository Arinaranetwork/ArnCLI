# Phase 3 - NetworkReconnaissance
## Goal
Expand Pocket diagnostics and network reconnaissance capabilities by delivering:
1. Wake-on-LAN magic packet dispatcher (`WolEngine` & `WolSheet`) for booting local servers and workstations over the network.
2. Wi-Fi live network inspector (`WifiInspectorEngine` & `WifiInspectorSheet`) reporting SSID, BSSID, link speed, frequency band (2.4 / 5 / 6 GHz), and signal strength.
3. Fast on-device TCP Port Scanner (`LanPortScanner` & `PortScannerSheet`) probing common infrastructure ports with latency tracking.
4. Local Subnet Host Discovery (`LanDiscoveryEngine` & `LanDiscoverySheet`) scanning active IP addresses and resolving hostnames with 1-tap host import.
5. Host Registration and Space Management (`AddHostSheet` in `SessionsScreen`) for creating and launching remote terminal host sessions.

## Scope
- `WolEngine` constructing RFC-standard 102-byte magic packets (6x 0xFF prefix followed by 16x MAC repetitions) and broadcasting via UDP socket.
- `WolSheet` interactive bottom sheet with MAC formatting validation, target broadcast IP, port selection (7 / 9), and dispatch history.
- `WifiInspectorEngine` extracting active Wi-Fi link parameters (SSID, BSSID, RSSI dBm, Link Speed, Frequency, IP, Gateway).
- `WifiInspectorSheet` displaying live wireless statistics, signal quality badge, and connection parameters.
- `LanPortScanner` testing TCP connection establishment across user-defined ports or common service presets (SSH, HTTP, HTTPS, RDP, MySQL, Postgres, etc.) with timeout pacing.
- `PortScannerSheet` interactive scanner in Pocket allowing single-target or multi-port sweep with live status indicators.
- `LanDiscoveryEngine` performing fast parallel subnet sweep across `/24` network ranges, detecting responsive IP addresses and hostnames.
- `LanDiscoverySheet` interactive subnet scanner in Pocket with 1-tap "Add to Space" action.
- `AddHostSheet` modal dialog in Sessions to configure host label, IP/address, port, protocol (SSH/Telnet), and username.
- Full unit test coverage for `WolEngine`, `WifiInspectorEngine`, `LanPortScanner`, and `LanDiscoveryEngine`.
- 100% compliance with `Android.md` tokens, physics, and presets via `VerifyAndroidRulesTask`.

## Out Of Scope
- Remote SSH tunnel execution (Phase 4).
- Rootless VpnService PCAP packet capture (Phase 10).
- External cloud lookups or remote APIs.

## Features
### Wake-on-LAN Magic Packet Dispatcher
Sends UDP broadcast datagrams across local subnet or directed IP.
- Input: MAC address in `AA:BB:CC:DD:EE:FF`, `AA-BB-CC-DD-EE-FF`, or contiguous hex.
- Target IP: defaults to `255.255.255.255` (or subnet broadcast).
- Target Port: standard ports 9 or 7.
- Validation: enforces exactly 6 hexadecimal bytes.
- Feedback: returns transmission status, packet size (102 bytes), and destination.

### Wi-Fi Live Inspector
Inspects connected Wi-Fi adapter properties:
- Frequency band detection: 2.4 GHz, 5 GHz, or 6 GHz (Wi-Fi 6E/7).
- Signal level mapping from raw RSSI (dBm) to percentage and qualitative rating (Excellent, Good, Fair, Poor).
- Link speed in Mbps and local IP/gateway configuration.

### Fast TCP Port Scanner
Non-blocking coroutine-based port prober:
- Tests TCP 3-way handshake with configurable timeout.
- Identifies common service names (22 -> SSH, 80 -> HTTP, 443 -> HTTPS, etc.).
- Computes connect latency in milliseconds for open ports.
- Categorizes port state (Open, Closed, Timeout).

## UI Mockup
```
+-------------------------------------------------------+
| Wake-on-LAN                               [ Close ]   |
+-------------------------------------------------------+
| MAC Address: [ 00:11:22:33:44:55                    ] |
| Broadcast IP: [ 255.255.255.255     ] Port: [ 9     ] |
| [ Send Magic Packet ]                                 |
| Status: Packet sent (102 bytes to 255.255.255.255:9) |
+-------------------------------------------------------+
```

## Logic Notes
- `WolEngine` runs UDP datagram transmission on `Dispatchers.IO`.
- `LanPortScanner` uses bounded coroutine parallelism (e.g. `semaphore` or chunked concurrency) to prevent socket exhaustion.
- UI sheets use `AppBottomSheet`, `AppButton`, `AppListItem`, `AppPillChip`, and `BottomClearanceSpacer`.

## Risks
- Android broadcast restrictions on some cellular/Wi-Fi interfaces -> Mitigation: fallback to directed broadcast address and catch socket exceptions gracefully.
- Android permission constraints for Wi-Fi SSID access (requires location on Android 8+) -> Mitigation: fallback gracefully to interface IP and link properties if SSID is restricted.

## Dependencies
- Standard Java `DatagramSocket`, `Socket`, `InetAddress`, `NetworkInterface`, Android `ConnectivityManager` / `WifiManager`.

## Acceptance Criteria
- `WolEngine` builds valid 102-byte payload and successfully transmits UDP datagram.
- Invalid MAC formats throw clear, descriptive validation errors.
- `WifiInspectorEngine` parses Wi-Fi link speed, RSSI, and frequency cleanly.
- `LanPortScanner` accurately identifies open vs closed ports and measures connect latency.
- UI sheets render in Electric Slate dark theme with zero Android.md rule violations.
- `./gradlew testDebugUnitTest` and `./gradlew verifyAndroidRules` pass with 0 errors.
