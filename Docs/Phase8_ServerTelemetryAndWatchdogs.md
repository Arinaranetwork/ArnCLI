# Phase 8 - ServerTelemetryAndWatchdogs
## Goal
Equip ArnTCLI with zero-agent remote Linux server telemetry, a multi-probe uptime watchdog, and an automated SSL/TLS certificate expiry inspector, giving sysadmins and homelabbers live infrastructure health directly on mobile with zero remote software installation.

## Scope
- `ServerTelemetryEngine`: Single-roundtrip multiplexed SSH `/proc` polling (`/proc/stat`, `/proc/meminfo`, `/proc/loadavg`, `/proc/net/dev`, `df -k -P`).
- `ServerMetrics`: Structured models for CPU utilization (differential total vs idle, per-core topology), memory/swap breakdown (MemAvailable vs MemFree+Buffers+Cached), load saturation ($Load_{1min} / vCPUs$), POSIX disk mount capacities, and network TX/RX throughput.
- `UptimeWatchdogEngine`: Multi-probe daemon (HTTP/HTTPS status & body check, TCP raw port reachability, ICMP ping latency) with consecutive-failure flap damping and notification triggers.
- `SslWatchdogEngine`: Certificate validity and expiration auditor calculating remaining days, SAN coverage, and alerting on warning (<30d) or critical (<7d) states.
- `ServerTelemetrySheet`: Interactive live dashboard with circular metric gauges, sparkline graphs, disk mount bars, and watchdog probe monitor.
- Adherence to `Android.md` Electric Slate dark tokens, spring physics, and zero rule violations.

## Out Of Scope
- Remote agent installation (Telegraf, Node Exporter, or Python daemons).
- Windows WMI/WinRM remote telemetry (Linux/BSD POSIX targets only).
- Cloud metrics aggregation (AWS CloudWatch, Datadog APIs).

## Features
### Agentless Multiplexed Server Telemetry
- Atomic single-roundtrip execution:
  `cat /proc/stat; echo "---EOF-STAT---"; cat /proc/meminfo; echo "---EOF-MEM---"; cat /proc/loadavg; echo "---EOF-LOAD---"; cat /proc/net/dev; echo "---EOF-NET---"; df -k -P; echo "---EOF-DF---"`
- Instant parsing on `Dispatchers.Default` with differential sampling.

### Multi-Probe Uptime Watchdog
- User configures probe targets with interval, timeout, and consecutive failure threshold ($N=2$).
- Ring buffer for latest 30 latency samples and smooth sparkline visualization.

### SSL Certificate Expiry Auditor
- Inspects remote host SSL certs, alerts on impending expiration, and warns when domain names do not match SAN entries.

## UI Mockup
```
+-------------------------------------------------------+
| Live Server Telemetry: prod-node-01        [ Refresh ]|
+-------------------------------------------------------+
| CPU: 18.4% (4 cores)  |  RAM: 3.4 GB / 16.0 GB (21%)  |
| Load: 0.24, 0.31, 0.28 (Sat: 6%) [ Healthy ]          |
| [ Disk Mounts ]                                       |
|  /     : [====----------------] 24% (18 GB / 80 GB)   |
|  /data : [============--------] 62% (310 GB / 500 GB) |
| [ Network IO ]                                        |
|  eth0: RX 1.2 MB/s  •  TX 480 KB/s                    |
+-------------------------------------------------------+
| Uptime Watchdog: 4 / 4 Services Up                    |
|  api.homelab.local : HTTP 200 • 38ms [● Up]           |
|  db.homelab.local  : TCP 5432 • 4ms  [● Up]           |
+-------------------------------------------------------+
```

## Logic Notes
- Single-command batch avoids SSH session re-negotiation and high latency on cellular connections.
- `df -k -P` avoids word-wrapping on long LVM device names and normalizes units to 1024-byte blocks.
- Flap damping prevents transient network blips from triggering false outage alerts.

## Risks
- Target device running minimal BusyBox or restricted shell -> Mitigation: POSIX `/proc` and `df -P` fallback parsing.
- Mobile battery drain from aggressive polling -> Mitigation: Workstation mode (active while sheet open), WorkManager periodic (15m) when in background.

## Dependencies
- JSch / Apache Mina SSH connection or local process pipe.
- Jetpack Compose Canvas for sparkline graphs.

## Acceptance Criteria
- `ServerTelemetryEngine` parses all 5 `/proc` sections from a single output stream.
- CPU utilization correctly computes delta total vs delta idle across samples.
- Memory correctly falls back if `MemAvailable` is absent on legacy kernels.
- `UptimeWatchdogEngine` verifies HTTP, TCP, and ICMP endpoints.
- UI passes `verifyAndroidRules` with 0 violations.
