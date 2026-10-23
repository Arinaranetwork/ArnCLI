// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.devlab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.arntcli.core.devlab.BleGattScannerEngine
import com.arinara.arntcli.core.devlab.HttpLabEngine
import com.arinara.arntcli.core.devlab.HttpMethod
import com.arinara.arntcli.core.devlab.HttpRequestModel
import com.arinara.arntcli.core.devlab.HttpResponseModel
import com.arinara.arntcli.core.devlab.MqttConnectionState
import com.arinara.arntcli.core.devlab.MqttLabEngine
import com.arinara.arntcli.core.devlab.MqttQos
import com.arinara.arntcli.core.devlab.WebSocketConnectionState
import com.arinara.arntcli.core.devlab.WebSocketDirection
import com.arinara.arntcli.core.devlab.WebSocketLabEngine
import com.arinara.arntcli.ui.components.presets.AppBottomSheet
import com.arinara.arntcli.ui.components.presets.AppButton
import com.arinara.arntcli.ui.components.presets.AppButtonVariant
import com.arinara.arntcli.ui.components.presets.AppCard
import com.arinara.arntcli.ui.components.presets.AppListItem
import com.arinara.arntcli.ui.components.presets.AppListItemAction
import com.arinara.arntcli.ui.components.presets.AppPillChip
import com.arinara.arntcli.ui.components.presets.BottomClearanceSpacer
import com.arinara.arntcli.ui.theme.ElectricSlateTokens
import com.arinara.arntcli.ui.theme.ElmsSans
import com.arinara.arntcli.ui.theme.jigglePressClickable
import kotlinx.coroutines.launch

enum class DevLabTab {
    Http,
    WebSocket,
    Mqtt,
    Ble
}

/**
 * DevLabSheet - Unified protocol and IoT developer workbench.
 */
@Composable
fun DevLabSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DevLabTab.Http) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Engines
    val httpEngine = remember { HttpLabEngine() }
    val wsEngine = remember { WebSocketLabEngine() }
    val mqttEngine = remember { MqttLabEngine() }
    val bleEngine = remember { BleGattScannerEngine() }

    // HTTP State
    var httpUrl by remember { mutableStateOf("https://httpbin.org/get") }
    var httpMethod by remember { mutableStateOf(HttpMethod.GET) }
    var httpBody by remember { mutableStateOf("{\n  \"device\": \"ArnTCLI-Station\",\n  \"status\": \"active\"\n}") }
    var httpResponse by remember { mutableStateOf<HttpResponseModel?>(null) }
    var isHttpBusy by remember { mutableStateOf(false) }

    // WebSocket State
    val wsState by wsEngine.state.collectAsState()
    val wsFrames by wsEngine.frames.collectAsState()
    var wsUrl by remember { mutableStateOf("wss://echo.websocket.events") }
    var wsInputMessage by remember { mutableStateOf("{\"type\":\"ping\",\"seq\":1}") }

    // MQTT State
    val mqttState by mqttEngine.state.collectAsState()
    val mqttSubs by mqttEngine.subscriptions.collectAsState()
    val mqttMessages by mqttEngine.messages.collectAsState()
    var mqttBrokerUri by remember { mutableStateOf("tcp://broker.hivemq.com:1883") }
    var mqttSubTopic by remember { mutableStateOf("sensors/#") }
    var mqttPubTopic by remember { mutableStateOf("sensors/arntcli/telemetry") }
    var mqttPubPayload by remember { mutableStateOf("{\"temp\":24.2,\"battery\":94}") }

    // BLE State
    val bleScanning by bleEngine.isScanning.collectAsState()
    val blePeripherals by bleEngine.peripherals.collectAsState()
    val bleConnected by bleEngine.connectedDevice.collectAsState()
    val bleServices by bleEngine.services.collectAsState()

    AppBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Dev & Protocol Lab",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "REST, WebSocket, MQTT & BLE Workbench",
                fontFamily = ElmsSans,
                fontSize = 12.sp,
                color = ElectricSlateTokens.TextMuted
            )

            // Protocol Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppPillChip(
                        label = "HTTP / REST",
                        isSelected = activeTab == DevLabTab.Http,
                        leadingIcon = Icons.Outlined.Http,
                        onClick = { activeTab = DevLabTab.Http }
                    )
                }
                item {
                    AppPillChip(
                        label = "WebSocket",
                        isSelected = activeTab == DevLabTab.WebSocket,
                        leadingIcon = Icons.Outlined.SyncAlt,
                        onClick = { activeTab = DevLabTab.WebSocket }
                    )
                }
                item {
                    AppPillChip(
                        label = "MQTT Broker",
                        isSelected = activeTab == DevLabTab.Mqtt,
                        leadingIcon = Icons.Outlined.Hub,
                        onClick = { activeTab = DevLabTab.Mqtt }
                    )
                }
                item {
                    AppPillChip(
                        label = "BLE Scanner",
                        isSelected = activeTab == DevLabTab.Ble,
                        leadingIcon = Icons.Outlined.Bluetooth,
                        onClick = { activeTab = DevLabTab.Ble }
                    )
                }
            }

            when (activeTab) {
                DevLabTab.Http -> {
                    // HTTP / REST VIEW
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Method selector
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)) { m ->
                                    AppPillChip(
                                        label = m.name,
                                        isSelected = httpMethod == m,
                                        onClick = { httpMethod = m }
                                    )
                                }
                            }

                            // URL input
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = httpUrl,
                                    onValueChange = { httpUrl = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (httpMethod.hasBody) {
                                Text(
                                    text = "JSON Request Body",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    BasicTextField(
                                        value = httpBody,
                                        onValueChange = { httpBody = it },
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        ),
                                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            AppButton(
                                text = if (isHttpBusy) "Executing..." else "Send Request",
                                leadingIcon = Icons.Outlined.Send,
                                variant = AppButtonVariant.Primary,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    isHttpBusy = true
                                    coroutineScope.launch {
                                        val req = HttpRequestModel(
                                            method = httpMethod,
                                            url = httpUrl,
                                            body = if (httpMethod.hasBody) httpBody else null
                                        )
                                        httpResponse = httpEngine.execute(req)
                                        isHttpBusy = false
                                    }
                                }
                            )
                        }
                    }

                    // HTTP Response Card
                    httpResponse?.let { resp ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            isGlow = resp.isSuccess
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${resp.statusCode} ${resp.statusMessage}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (resp.isSuccess) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.Destructive
                                    )
                                    Text(
                                        text = "${resp.durationMs}ms • ${resp.formattedSize()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }

                                Text(
                                    text = resp.body.take(2000),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary,
                                    maxLines = 15,
                                    overflow = TextOverflow.Ellipsis
                                )

                                AppButton(
                                    text = "Copy Body",
                                    leadingIcon = Icons.Outlined.ContentCopy,
                                    variant = AppButtonVariant.Secondary,
                                    onClick = { clipboardManager.setText(AnnotatedString(resp.body)) }
                                )
                            }
                        }
                    }
                }

                DevLabTab.WebSocket -> {
                    // WEBSOCKET VIEW
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (wsState is WebSocketConnectionState.Connected) "Connected" else "Disconnected",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (wsState is WebSocketConnectionState.Connected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                )

                                AppButton(
                                    text = if (wsState is WebSocketConnectionState.Connected) "Disconnect" else "Connect",
                                    variant = if (wsState is WebSocketConnectionState.Connected) AppButtonVariant.Secondary else AppButtonVariant.Primary,
                                    onClick = {
                                        if (wsState is WebSocketConnectionState.Connected) wsEngine.disconnect()
                                        else wsEngine.connect(wsUrl)
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = wsUrl,
                                    onValueChange = { wsUrl = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (wsState is WebSocketConnectionState.Connected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = wsInputMessage,
                                            onValueChange = { wsInputMessage = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            ),
                                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    AppButton(
                                        text = "Send",
                                        variant = AppButtonVariant.Primary,
                                        onClick = { wsEngine.sendText(wsInputMessage) }
                                    )
                                }
                            }
                        }
                    }

                    // WebSocket Frame Stream
                    Text(
                        text = "Frame Timeline (${wsFrames.size})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    wsFrames.takeLast(10).reversed().forEach { frame ->
                        val dirLabel = when (frame.direction) {
                            WebSocketDirection.Inbound -> "↓ IN"
                            WebSocketDirection.Outbound -> "↑ OUT"
                            WebSocketDirection.System -> "• SYS"
                        }
                        val tint = when (frame.direction) {
                            WebSocketDirection.Inbound -> ElectricSlateTokens.PrimaryAccent
                            WebSocketDirection.Outbound -> ElectricSlateTokens.TextPrimary
                            WebSocketDirection.System -> ElectricSlateTokens.TextMuted
                        }

                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dirLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = tint
                                )
                                Text(
                                    text = frame.payload,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${frame.byteCount}B",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }
                        }
                    }
                }

                DevLabTab.Mqtt -> {
                    // MQTT VIEW
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (mqttState is MqttConnectionState.Connected) "Broker Connected" else "Disconnected",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (mqttState is MqttConnectionState.Connected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted
                                )

                                AppButton(
                                    text = if (mqttState is MqttConnectionState.Connected) "Disconnect" else "Connect",
                                    variant = if (mqttState is MqttConnectionState.Connected) AppButtonVariant.Secondary else AppButtonVariant.Primary,
                                    onClick = {
                                        if (mqttState is MqttConnectionState.Connected) mqttEngine.disconnect()
                                        else mqttEngine.connect(mqttBrokerUri)
                                    }
                                )
                            }

                            // Broker input
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = mqttBrokerUri,
                                    onValueChange = { mqttBrokerUri = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (mqttState is MqttConnectionState.Connected) {
                                // Subscription input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = mqttSubTopic,
                                            onValueChange = { mqttSubTopic = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            ),
                                            cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    AppButton(
                                        text = "Subscribe",
                                        variant = AppButtonVariant.Primary,
                                        onClick = { mqttEngine.subscribe(mqttSubTopic) }
                                    )
                                }

                                // Publish Form
                                Text(
                                    text = "Publish Message",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = mqttPubTopic,
                                        onValueChange = { mqttPubTopic = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        ),
                                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    BasicTextField(
                                        value = mqttPubPayload,
                                        onValueChange = { mqttPubPayload = it },
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ElectricSlateTokens.TextPrimary
                                        ),
                                        cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                AppButton(
                                    text = "Publish QoS 1",
                                    variant = AppButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { mqttEngine.publish(mqttPubTopic, mqttPubPayload, MqttQos.AtLeastOnce) }
                                )
                            }
                        }
                    }

                    // MQTT Subscriptions Chips
                    if (mqttSubs.isNotEmpty()) {
                        Text(
                            text = "Active Subscriptions",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricSlateTokens.TextMuted
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(mqttSubs) { sub ->
                                AppPillChip(
                                    label = sub.topicFilter,
                                    isSelected = true,
                                    onClick = { mqttEngine.unsubscribe(sub.topicFilter) }
                                )
                            }
                        }
                    }

                    // Message Feed
                    Text(
                        text = "MQTT Feed (${mqttMessages.size})",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ElectricSlateTokens.TextPrimary
                    )

                    mqttMessages.takeLast(10).reversed().forEach { msg ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg.topic,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                    Text(
                                        text = if (msg.isOutbound) "PUB" else "SUB",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )
                                }
                                Text(
                                    text = msg.payload,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                DevLabTab.Ble -> {
                    // BLE GATT VIEW
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (bleScanning) "Scanning BLE Peripherals..." else "BLE Scanner Idle",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = "${blePeripherals.size} devices discovered",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted
                                )
                            }

                            AppButton(
                                text = if (bleScanning) "Stop" else "Scan",
                                leadingIcon = if (bleScanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                                variant = if (bleScanning) AppButtonVariant.Secondary else AppButtonVariant.Primary,
                                onClick = {
                                    if (bleScanning) bleEngine.stopScan()
                                    else bleEngine.startScan()
                                }
                            )
                        }
                    }

                    // Peripherals Listing & Empty State
                    if (blePeripherals.isEmpty()) {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (bleScanning) "Scanning for BLE Peripherals..." else "No BLE Peripherals Discovered",
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ElectricSlateTokens.TextPrimary
                                )
                                Text(
                                    text = if (bleScanning) "Listening for nearby Bluetooth Low Energy advertisements..." else "Tap Scan to detect nearby BLE beacons, GATT peripherals, and sensors.",
                                    fontFamily = ElmsSans,
                                    fontSize = 12.sp,
                                    color = ElectricSlateTokens.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        blePeripherals.forEach { device ->
                            AppListItem(
                                title = device.displayName,
                                subtitle = "${device.address} • RSSI ${device.rssi} dBm (${device.signalQualityPercent()}%)",
                                leadingIcon = Icons.Outlined.Bluetooth,
                                leadingIconTint = if (device.isConnected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                action = if (device.isConnected) AppListItemAction.Chevron else null,
                                onClick = {
                                    if (device.isConnected) bleEngine.disconnectDevice()
                                    else bleEngine.connectDevice(device)
                                }
                            )
                        }
                    }

                    // GATT Services Inspector Card
                    if (bleConnected != null && bleServices.isNotEmpty()) {
                        Text(
                            text = "GATT Services: ${bleConnected!!.displayName}",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ElectricSlateTokens.TextPrimary
                        )

                        bleServices.forEach { s ->
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = s.name,
                                        fontFamily = ElmsSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ElectricSlateTokens.PrimaryAccent
                                    )
                                    Text(
                                        text = s.uuid,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = ElectricSlateTokens.TextMuted
                                    )

                                    s.characteristics.forEach { c ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${c.name} (${c.properties.joinToString(",")})",
                                                fontFamily = ElmsSans,
                                                fontSize = 11.sp,
                                                color = ElectricSlateTokens.TextPrimary
                                            )
                                            if (c.valueHex != null) {
                                                Text(
                                                    text = c.valueHex,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = ElectricSlateTokens.TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BottomClearanceSpacer()
        }
    }
}
