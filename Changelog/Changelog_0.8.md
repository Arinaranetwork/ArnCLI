# Changelog - ArnTCLI 0.8

## [0.8.0-Beta] - 2026-10-22

### Added
- Agentless Linux Server Telemetry Engine (`ServerTelemetryEngine`, `ServerMetrics`) executing single-roundtrip multiplexed `/proc` commands over SSH with differential CPU utilization, memory availability, load saturation ratios, disk mount usage (`df -k -P`), and network IO rates.
- Multi-Probe Uptime Watchdog Engine (`UptimeWatchdogEngine`, `WatchdogProbeTarget`, `ProbeResult`) supporting HTTP/HTTPS, TCP port ping, and ICMP latency probes with consecutive-failure flap damping and sparkline sample history.
- SSL/TLS Certificate Expiry Watchdog (`SslWatchdogEngine`, `SslCertStatus`) calculating remaining days, SAN domain matching, and alert grading.
- Live Telemetry Dashboard Sheet (`ServerTelemetrySheet`) with tactile circular gauges and smooth sparklines adhering 100% to Electric Slate design tokens and `Android.md`.
