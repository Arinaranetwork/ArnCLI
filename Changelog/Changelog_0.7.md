# Changelog - ArnTCLI 0.7

## [0.7.0-Beta] - 2026-10-22

### Added
- Mini-Postman HTTP/REST and GraphQL Engine (`HttpLabEngine`, `HttpRequestModel`, `HttpResponseModel`) supporting environment variable substitution (`{{var}}`), custom headers, query parameters, latency timing, and status code badges.
- Real-Time WebSocket Lab Engine (`WebSocketLabEngine`, `WebSocketFrame`) with duplex text/binary frame logging, reconnection state machine, and heartbeat monitoring.
- MQTT IoT Protocol Engine (`MqttLabEngine`, `MqttMessage`, `MqttQos`) with full wildcard topic subscription matching (`+`, `#`), QoS 0/1/2 publishing, and message history feed.
- BLE GATT Peripheral Scanner & Explorer (`BleGattScannerEngine`, `BlePeripheral`, `GattServiceInfo`) for discovering nearby Bluetooth Low Energy devices, advertised services, and GATT characteristic trees.
- Interactive Dev Lab Interface (`DevLabScreen`, `HttpLabSheet`, `WebSocketSheet`, `MqttSheet`, `BleGattSheet`) with Electric Slate dark theme and 0 `Android.md` violations.
- Comprehensive unit test suites for HTTP template expansion, MQTT wildcard matching, WebSocket frame buffering, and BLE service discovery.
