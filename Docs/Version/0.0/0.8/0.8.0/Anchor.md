# Anchor - ArnTCLI 0.8.0
Status: Open    Opened: 2026-10-22    Finished: 

| ID    | Request                                                                                  | Phase | Acceptance                                                                                                            | Status  | Evidence |
|-------|------------------------------------------------------------------------------------------|-------|-----------------------------------------------------------------------------------------------------------------------|---------|----------|
| A-001 | Implement Agentless Linux `/proc` Telemetry Engine (`ServerTelemetryEngine`)             | 8     | Single-command batch extracts CPU, RAM, Loadavg, Net, Disk via `/proc` and `df -k -P`, differential metrics calculate | Pending |          |
| A-002 | Implement Multi-Probe Uptime Watchdog Engine (`UptimeWatchdogEngine`)                    | 8     | Probes HTTP, TCP, and ICMP targets, enforces consecutive-failure flap damping, stores 30-sample ring buffer          | Pending |          |
| A-003 | Implement SSL/TLS Certificate Expiry Watchdog (`SslWatchdogEngine`)                      | 8     | Extracts remote cert chains, validates SAN matches, computes days until expiration with warning/critical alerts       | Pending |          |
| A-004 | Implement Telemetry Gauges & Sparkline UI Components (`MetricGaugeCard`, `SparklineGraph`) | 8     | Canvas-based sparklines and gauge indicators adhering strictly to `Android.md` tokens and spring physics             | Pending |          |
| A-005 | Implement Server Health & Watchdog Dashboard Sheet (`ServerTelemetrySheet`)              | 8     | Interactive live dashboard wired into Sessions and Pocket with zero `Android.md` violations                           | Pending |          |
