// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolsLabEngineTest {

    @Test
    fun webSocketLabEngine_urlValidation() {
        assertTrue(WebSocketLabEngine.isValidWsUrl("ws://192.168.1.100:8080/ws"))
        assertTrue(WebSocketLabEngine.isValidWsUrl("wss://echo.websocket.events"))
        assertFalse(WebSocketLabEngine.isValidWsUrl("http://echo.websocket.events"))
        assertFalse(WebSocketLabEngine.isValidWsUrl("ftp://server"))
        assertFalse(WebSocketLabEngine.isValidWsUrl(""))
    }

    @Test
    fun webSocketLabEngine_connectAndSend() {
        val engine = WebSocketLabEngine()
        assertEquals(WebSocketConnectionState.Disconnected, engine.state.value)

        engine.connect("wss://echo.websocket.events")
        assertTrue(engine.state.value is WebSocketConnectionState.Connected)

        assertTrue(engine.sendText("{\"action\":\"ping\"}"))
        assertEquals(3, engine.frames.value.size) // Status connect + Status connected + Text Outbound

        engine.receiveFrame(WebSocketFrameType.Text, "{\"action\":\"pong\"}")
        assertEquals(4, engine.frames.value.size)

        engine.disconnect()
        assertEquals(WebSocketConnectionState.Disconnected, engine.state.value)
    }

    @Test
    fun mqttLabEngine_wildcardTopicMatching() {
        // Exact
        assertTrue(MqttLabEngine.topicMatches("home/living/temp", "home/living/temp"))
        assertFalse(MqttLabEngine.topicMatches("home/living/temp", "home/kitchen/temp"))

        // Single-level +
        assertTrue(MqttLabEngine.topicMatches("home/+/temp", "home/living/temp"))
        assertTrue(MqttLabEngine.topicMatches("home/+/temp", "home/kitchen/temp"))
        assertFalse(MqttLabEngine.topicMatches("home/+/temp", "home/living/sub/temp"))

        // Multi-level #
        assertTrue(MqttLabEngine.topicMatches("home/#", "home/living/temp"))
        assertTrue(MqttLabEngine.topicMatches("home/#", "home"))
        assertTrue(MqttLabEngine.topicMatches("home/#", "home/a/b/c/d/e"))
        assertTrue(MqttLabEngine.topicMatches("#", "any/random/topic"))

        assertFalse(MqttLabEngine.topicMatches("office/#", "home/living/temp"))

        // MQTT Spec: $SYS topics cannot be matched by # or + at root level
        assertFalse(MqttLabEngine.topicMatches("#", "\$SYS/broker/uptime"))
        assertFalse(MqttLabEngine.topicMatches("+/broker", "\$SYS/broker"))
        assertTrue(MqttLabEngine.topicMatches("\$SYS/#", "\$SYS/broker/uptime"))
    }

    @Test
    fun mqttLabEngine_topicValidation() {
        assertTrue(MqttLabEngine.isValidTopicFilter("sensors/temperature"))
        assertTrue(MqttLabEngine.isValidTopicFilter("sensors/+/temp"))
        assertTrue(MqttLabEngine.isValidTopicFilter("sensors/#"))
        assertTrue(MqttLabEngine.isValidTopicFilter("#"))

        assertFalse(MqttLabEngine.isValidTopicFilter(""))
        assertFalse(MqttLabEngine.isValidTopicFilter("sensors#bad"))
        assertFalse(MqttLabEngine.isValidTopicFilter("home/+room/temp"))
        assertFalse(MqttLabEngine.isValidTopicFilter("sensors/#/#"))
    }

    @Test
    fun bleGattScannerEngine_signalAndServiceResolution() {
        val p1 = BlePeripheral("AA:BB:CC:11:22:33", "Sensor", -40)
        assertEquals(100, p1.signalQualityPercent())

        val p2 = BlePeripheral("DD:EE:FF:44:55:66", "FarSensor", -100)
        assertEquals(0, p2.signalQualityPercent())

        assertEquals("Battery Service", BleGattScannerEngine.resolveServiceName("0000180f-0000-1000"))
        assertEquals("Device Information", BleGattScannerEngine.resolveServiceName("0000180a-0000-1000"))
        assertEquals("Environmental Sensing", BleGattScannerEngine.resolveServiceName("0000181a-0000-1000"))
        assertEquals("Battery Service", BleGattScannerEngine.resolveServiceName("0000180f-0000-1000-8000-00805f9b34fb"))
        assertEquals("Custom Service (e1a81800)", BleGattScannerEngine.resolveServiceName("e1a81800-4567-dead-beef-001122334455"))
    }
}
