# Changelog - ArnTCLI 0.2

## [0.2.0-Beta] - 2026-10-19

### Added
- On-device local Linux shell execution engine (`LocalProcessEngine`) executing `/system/bin/sh` rootlessly with non-blocking stream capture.
- Interactive terminal command prompt input pipeline with monospace font styling, auto-scrolling viewport, quick diagnostic command chips (`uname -a`, `id`, `pwd`, `date`, `uptime`, `clear`), and auxiliary keyboard buttons (`ESC`, `TAB`, `CTRL`, `ALT`, `|`, `/`, `~`, `-`, `>`).
- Pure on-device IPv4 Subnet and CIDR calculation engine (`SubnetCalculator`) and modal interactive sheet (`SubnetCalculatorSheet`) with real-time mask, wildcard, broadcast, usable range, binary bitmask, and RFC 1918 classification breakdown.
- On-device ICMP Ping and round-trip latency engine (`IcmpPingEngine`) and interactive sheet (`PingSheet`) streaming live packets with rolling Min, Avg, Max, Jitter, and packet loss metrics.
- Pure on-device DNS Deep Resolver (`DnsResolverEngine`) and interactive sheet (`DnsSheet`) parsing IPv4 (A), IPv6 (AAAA), and reverse PTR records using local system resolvers.
- On-device SSL/TLS Certificate Inspector (`SslInspectorEngine`) and interactive sheet (`SslInspectorSheet`) negotiating TLS handshakes via SNI and extracting X.509 certificate subject, issuer, validity window, days remaining, SANs, protocol version, cipher suite, and SHA-256 fingerprint.
- Full unit test suites `LocalProcessEngineTest`, `SubnetCalculatorTest`, `IcmpPingEngineTest`, `DnsResolverEngineTest`, and `SslInspectorEngineTest`.

### Changed
- Refactored `MainScaffold` to route active execution pipelines for local terminal sessions and Pocket diagnostic sheets.
- Purged all hardcoded mock simulations, placeholder data, and dummy servers across the application layer.

### Fixed
- Fixed IP address regex parsing in ICMP ping outputs across Linux and Windows formats.
- Handled Server Name Indication (SNI) host parameter configuration for virtual-hosted domain inspection.
