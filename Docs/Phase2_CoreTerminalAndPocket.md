# Phase 2 - CoreTerminalAndPocket
## Goal
Implement ArnTCLI's foundational functional capabilities: an on-device local Linux shell execution engine (`/system/bin/sh`) with non-blocking coroutine streaming in `TerminalScreen`, and the first fully interactive diagnostic tool in the Pocket tool belt: the CIDR & Subnet Calculator.

## Scope
- Local process spawning engine (`LocalProcessEngine`) executing `/system/bin/sh` with environment configuration.
- Coroutine-based asynchronous stream reader capturing stdout and stderr into active session buffers.
- Interactive terminal input pipeline: typing commands in the bottom prompt bar, executing via the play button or enter key, and updating the viewport.
- Auxiliary keyboard keys (`ESC`, `TAB`, `CTRL`, `ALT`, `|`, `/`, `~`, `-`, `>`) injecting characters into the input stream.
- Pure on-device Subnet & CIDR calculator engine (`SubnetCalculator`) parsing IPv4 CIDR notation (e.g. `192.168.1.1/24`) and calculating network IP, broadcast IP, netmask, wildcard mask, usable host range, total hosts, and binary representation.
- Interactive Subnet Calculator UI (`SubnetCalculatorSheet` / dialog) accessible directly from the Pocket screen.
- On-device ICMP Ping and latency/jitter diagnostics engine (`IcmpPingEngine`) streaming real-time sequence numbers, RTT, TTL, jitter, and packet loss stats.
- Interactive Ping UI (`PingSheet`) in Pocket with target presets, continuous stream, and live metric badges.
- Native DNS Deep Resolver (`DnsResolverEngine`) parsing IPv4 (A), IPv6 (AAAA), and reverse PTR records.
- Interactive DNS UI (`DnsSheet`) in Pocket with hostname presets and record breakdown.

## Out Of Scope
- Remote SSH v2 network socket connection (scheduled in v0.3 / Phase 3).
- Full xterm/vt100 terminal emulation escape sequence parser (bold, cursor movement, full-screen curses apps).
- Package management ecosystems (apt/pkg).

## Features
### Local Process Terminal Execution Engine
Direct on-device shell session creation. Uses Android's native Linux kernel execution (`/system/bin/sh`), redirects standard streams, reads output line-by-line using Kotlin Coroutines on `Dispatchers.IO`, and emits updates via reactive state. Handles shell errors, exit codes, and process termination cleanly without crashing.

### Interactive Command Input & Key Injector
Tactile command prompt input allowing users to type commands, inject shell control characters from auxiliary buttons, view execution history, and scroll through real terminal stdout/stderr lines.

### Pocket Subnet & CIDR Calculator
Deterministic IPv4/IPv6 subnet calculation utility. Computes:
- Netmask (dotted decimal e.g. `255.255.255.0`)
- Wildcard mask (e.g. `0.0.0.255`)
- Network address (e.g. `192.168.1.0`)
- Broadcast address (e.g. `192.168.1.255`)
- Usable host range (e.g. `192.168.1.1` - `192.168.1.254`)
- Usable host count (e.g. `254`)
- Binary bitmask breakdown

### Pocket ICMP Ping & Jitter
Real-time network reachability and latency profiler:
- Sequence tracking with millisecond round-trip times
- Rolling statistics: Min, Avg, Max latency, and packet loss rate
- Clean Electric Slate timeline entries with status badges

### Pocket DNS Deep Resolver
Pure on-device DNS resolution tool:
- Resolves A (IPv4) and AAAA (IPv6) records via system resolver
- Reverse DNS lookup (PTR) for IP-to-hostname mapping
- Measures resolution round-trip latency in milliseconds

## UI Mockup
```
+-------------------------------------------------------+
| ICMP Ping & Jitter                        [ Stop (X) ]
+-------------------------------------------------------+
| Target: [ 1.1.1.1                                   ] |
| Min: 14ms | Avg: 18ms | Max: 25ms | Loss: 0%         |
+-------------------------------------------------------+
| seq=1 | 1.1.1.1: bytes=64 time=15.2ms ttl=57          |
| seq=2 | 1.1.1.1: bytes=64 time=16.8ms ttl=57          |
| seq=3 | 1.1.1.1: bytes=64 time=14.9ms ttl=57          |
+-------------------------------------------------------+
```

## Logic Notes
- `LocalProcessEngine` manages `Process` lifecycle and dispatches to coroutine scopes.
- `SubnetCalculator` validates IPv4 address octets (0-255) and prefix lengths (0-32).
- `IcmpPingEngine` runs native ping executable or socket latency probes on `Dispatchers.IO`.
- `DnsResolverEngine` performs multi-address DNS lookups asynchronously.
- Zero external network lookups or cloud dependencies.

## Risks
- Android SELinux process isolation restricts certain binaries -> Mitigation: Fallback to standard `/system/bin/sh` which is globally accessible on rootless Android.

## Dependencies
- Standard Java `ProcessBuilder`, `InetAddress` & Kotlin Coroutines.

## Acceptance Criteria
- `LocalProcessEngine` executes standard shell commands (`uname -a`, `id`, `echo`, `ls -l`) and streams real output.
- `SubnetCalculator` mathematically computes correct network parameters across /8, /16, /24, /29, /30, and /32 subnets.
- `IcmpPingEngine` streams sequential ping samples and calculates correct min/avg/max/loss metrics.
- `DnsResolverEngine` resolves valid hostnames to IPv4/IPv6 addresses and returns clear errors on invalid domains.
- UI passes `./gradlew verifyAndroidRules` with 0 violations.
- `./gradlew testDebugUnitTest` passes all test cases.
