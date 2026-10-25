# Phase 7 - DevLabAndIotProtocols
## Goal
Provide on-device developer protocol testing tools (Mini-Postman HTTP/GraphQL client, real-time WebSocket client, MQTT IoT broker explorer, and BLE GATT scanner) with zero external server dependencies.

## Scope
- `HttpLabEngine`: Lightweight HTTP/REST and GraphQL request executor with header management, query parameters, method selection (GET, POST, PUT, DELETE, PATCH, HEAD), environment variable substitution (`{{baseUrl}}`), and response timing metrics.
- `WebSocketLabEngine`: Duplex WebSocket connection engine supporting real-time text/binary frame sending, incoming message timeline, connection status monitoring, and ping/pong heartbeats.
- `MqttLabEngine`: MQTT v3.1.1/v5 protocol client supporting broker connection, topic wildcard subscription (`home/+/temperature`, `sensors/#`), message publishing with QoS (0, 1, 2) and retain flags, and message stream history.
- `BleGattScannerEngine`: Android BLE peripheral scanner with RSSI reporting, manufacturer data parsing, and GATT service/characteristic tree explorer.
- `DevLabScreen`: Interactive Dev Lab dashboard with tabbed protocol sheets (`HttpLabSheet`, `WebSocketSheet`, `MqttSheet`, `BleGattSheet`).
- Comprehensive unit test suites for URL/header templating, MQTT topic matching, WebSocket framing, and BLE UUID parsing.
- 100% adherence to Arinara Electric Slate design tokens and `Android.md`.

## Out Of Scope
- Heavy Node.js or Python runtime execution on-device.
- Compiling C/C++ native gRPC bindings at runtime.

## Features
### Mini-Postman HTTP & GraphQL Lab
- HTTP Methods: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS.
- Headers & Query Parameters: Key-value pair editor with quick toggles.
- Environment Variables: `{{variable}}` substitution from active Space environment.
- Response Inspector: Status code badge (`200 OK`, `404 Not Found`), latency timer (ms), response headers, and formatted JSON body.

### Real-Time WebSocket Lab
- Duplex messaging: Live bidirectional stream with timestamped frame cards.
- Message composer: Send custom text or JSON payloads.
- Reconnection state machine: Visual status indicator (`Connected`, `Connecting`, `Disconnected`).

### MQTT IoT Broker Lab
- Broker connection: `tcp://broker.hivemq.com:1883`, `mqtt://192.168.1.50:1883` or SSL.
- Topic subscriptions: Supports standard MQTT wildcards (`+` single-level, `#` multi-level).
- Publishing: Payload text, target topic, QoS 0/1/2, retain flag.
- Feed viewer: Stream of incoming messages tagged with topic and timestamp.

### BLE GATT Peripheral Explorer
- Low Energy scan: Discovers nearby BLE devices with live RSSI signal meter.
- Device inspection: Advertised services, manufacturer OUI/ID, device name, and MAC.
- GATT service tree: Primary services (Battery, Device Info, Custom UART) with characteristic UUIDs and read/write descriptors.

## UI Mockup
```
+-------------------------------------------------------+
| Dev Lab: HTTP / REST Client               [ Send ⚡ ] |
+-------------------------------------------------------+
| [ GET ▼ ] https://api.homelab.local/v1/health         |
| Headers (3) | Params (1) | Body (JSON)                |
| Status: 200 OK • 42ms • 1.2 KB                        |
| +---------------------------------------------------+ |
| | {                                                 | |
| |   "status": "healthy",                            | |
| |   "uptime": 86400,                                | |
| |   "version": "1.4.0"                              | |
| | }                                                 | |
| +---------------------------------------------------+ |
+-------------------------------------------------------+
```

## Logic Notes
- Variable substitution matches `\{\{([a-zA-Z0-9_-]+)\}\}` and replaces with Space or session variable dictionary.
- MQTT topic matching uses recursive segment comparison for `+` and terminal matching for `#`.
- All network and Bluetooth operations run on `Dispatchers.IO`.

## Risks
- Android 12+ Bluetooth runtime permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`) -> Mitigation: graceful permission check with user guidance banner.
- High memory usage on large WebSocket/HTTP payloads -> Mitigation: cap response preview to 500 KB and stream large outputs.

## Dependencies
- Standard Java/Kotlin networking, OkHttp, Jetpack Compose.

## Acceptance Criteria
- `HttpLabEngine` substitutes environment variables and parses response codes correctly.
- `MqttLabEngine` validates topic syntax and correctly matches wildcard subscriptions.
- `WebSocketLabEngine` manages connection states and frame buffers.
- Interactive sheets render in Electric Slate dark theme with 0 Android.md violations.
