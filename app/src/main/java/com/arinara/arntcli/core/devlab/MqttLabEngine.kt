// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.devlab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class MqttQos(val value: Int) {
    AtMostOnce(0),
    AtLeastOnce(1),
    ExactlyOnce(2)
}

data class MqttSubscription(
    val topicFilter: String,
    val qos: MqttQos = MqttQos.AtLeastOnce
) {
    init {
        require(topicFilter.isNotBlank()) { "Topic filter cannot be blank" }
        require(MqttLabEngine.isValidTopicFilter(topicFilter)) { "Invalid MQTT topic filter: $topicFilter" }
    }
}

data class MqttMessage(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val payload: String,
    val qos: MqttQos = MqttQos.AtLeastOnce,
    val isRetained: Boolean = false,
    val isOutbound: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface MqttConnectionState {
    object Disconnected : MqttConnectionState
    object Connecting : MqttConnectionState
    data class Connected(val broker: String, val clientId: String) : MqttConnectionState
    data class Error(val message: String) : MqttConnectionState
}

/**
 * MQTT IoT Broker client engine supporting v3.1.1/v5 wildcard routing and QoS levels.
 */
class MqttLabEngine {

    private val _state = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val state: StateFlow<MqttConnectionState> = _state.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<MqttSubscription>>(emptyList())
    val subscriptions: StateFlow<List<MqttSubscription>> = _subscriptions.asStateFlow()

    private val _messages = MutableStateFlow<List<MqttMessage>>(emptyList())
    val messages: StateFlow<List<MqttMessage>> = _messages.asStateFlow()

    companion object {
        const val MAX_MESSAGE_HISTORY = 300

        fun isValidTopicFilter(filter: String): Boolean {
            val clean = filter.trim()
            if (clean.isEmpty()) return false
            if (clean == "#") return true

            // # can only appear once, and must be at the end following a '/'
            val hashCount = clean.count { it == '#' }
            if (hashCount > 1) return false
            if (hashCount == 1 && !clean.endsWith("/#")) return false

            // + can only appear as an entire level (between / or alone)
            val segments = clean.split('/')
            for (seg in segments) {
                if (seg.contains("+") && seg != "+") return false
                if (seg.contains("#") && seg != "#") return false
            }

            return true
        }

        /**
         * Checks whether a concrete publish topic matches an MQTT wildcard filter.
         */
        fun topicMatches(filter: String, topic: String): Boolean {
            if (filter == topic) return true

            // MQTT Spec 4.7.2: Topics starting with '$' cannot be matched by '#' or '+' at root level
            if (topic.startsWith("$") && (filter.startsWith("#") || filter.startsWith("+"))) {
                return false
            }

            if (filter == "#") return true

            val filterSegments = filter.split('/')
            val topicSegments = topic.split('/')

            for (i in filterSegments.indices) {
                val fSeg = filterSegments[i]
                if (fSeg == "#") return true
                if (i >= topicSegments.size) return false

                val tSeg = topicSegments[i]
                if (fSeg != "+" && fSeg != tSeg) {
                    return false
                }
            }

            return filterSegments.size == topicSegments.size
        }
    }

    fun connect(brokerUri: String, clientId: String = "ArnTCLI-${System.currentTimeMillis().toString().takeLast(4)}") {
        val clean = brokerUri.trim()
        require(clean.isNotBlank()) { "Broker URI cannot be blank" }

        _state.value = MqttConnectionState.Connecting
        _state.value = MqttConnectionState.Connected(clean, clientId)
    }

    fun disconnect() {
        _state.value = MqttConnectionState.Disconnected
        _subscriptions.value = emptyList()
    }

    fun subscribe(topicFilter: String, qos: MqttQos = MqttQos.AtLeastOnce): Boolean {
        if (_state.value !is MqttConnectionState.Connected) return false
        val clean = topicFilter.trim()
        if (_subscriptions.value.any { it.topicFilter == clean }) return false

        val sub = MqttSubscription(clean, qos)
        _subscriptions.update { it + sub }
        return true
    }

    fun unsubscribe(topicFilter: String) {
        val clean = topicFilter.trim()
        _subscriptions.update { current -> current.filterNot { it.topicFilter == clean } }
    }

    fun publish(topic: String, payload: String, qos: MqttQos = MqttQos.AtLeastOnce, retain: Boolean = false): Boolean {
        if (_state.value !is MqttConnectionState.Connected) return false
        val cleanTopic = topic.trim()
        require(!cleanTopic.contains("+") && !cleanTopic.contains("#")) { "Publish topic cannot contain wildcards" }

        val msg = MqttMessage(
            topic = cleanTopic,
            payload = payload,
            qos = qos,
            isRetained = retain,
            isOutbound = true
        )
        _messages.update { (it + msg).takeLast(MAX_MESSAGE_HISTORY) }
        return true
    }

    fun receiveMessage(topic: String, payload: String, qos: MqttQos = MqttQos.AtLeastOnce) {
        val msg = MqttMessage(
            topic = topic,
            payload = payload,
            qos = qos,
            isRetained = false,
            isOutbound = false
        )
        _messages.update { (it + msg).takeLast(MAX_MESSAGE_HISTORY) }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
