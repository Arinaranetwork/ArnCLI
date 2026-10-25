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

data class GattCharacteristicInfo(
    val uuid: String,
    val name: String,
    val properties: List<String> = listOf("READ", "NOTIFY"),
    val valueHex: String? = null
)

data class GattServiceInfo(
    val uuid: String,
    val name: String,
    val isPrimary: Boolean = true,
    val characteristics: List<GattCharacteristicInfo>
)

data class BlePeripheral(
    val address: String,
    val name: String,
    val rssi: Int,
    val advertisedServices: List<String> = emptyList(),
    val manufacturerId: Int? = null,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false
) {
    val displayName: String get() = name.ifBlank { "Unnamed Peripheral" }

    fun signalQualityPercent(): Int {
        // RSSI ranges typically from -100 dBm (0%) to -40 dBm (100%)
        return ((rssi + 100) * 100 / 60).coerceIn(0, 100)
    }
}

/**
 * Bluetooth Low Energy (BLE) peripheral discovery and GATT service inspector.
 */
class BleGattScannerEngine {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _peripherals = MutableStateFlow<List<BlePeripheral>>(emptyList())
    val peripherals: StateFlow<List<BlePeripheral>> = _peripherals.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BlePeripheral?>(null)
    val connectedDevice: StateFlow<BlePeripheral?> = _connectedDevice.asStateFlow()

    private val _services = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val services: StateFlow<List<GattServiceInfo>> = _services.asStateFlow()

    companion object {
        fun resolveServiceName(uuid: String): String {
            val u = uuid.lowercase().trim()
            val shortId = when {
                u.length == 4 -> u
                u.startsWith("0000") && u.length >= 8 -> u.substring(4, 8)
                u.contains("-") -> {
                    val firstSegment = u.substringBefore("-")
                    if (firstSegment.length == 8 && firstSegment.startsWith("0000")) {
                        firstSegment.substring(4, 8)
                    } else null
                }
                else -> null
            }

            return when (shortId) {
                "1800" -> "Generic Access"
                "1801" -> "Generic Attribute"
                "180a" -> "Device Information"
                "180f" -> "Battery Service"
                "180d" -> "Heart Rate"
                "181a" -> "Environmental Sensing"
                "ffe0", "fff0" -> "Custom UART / Serial"
                else -> "Custom Service (${u.take(8)})"
            }
        }
    }

    fun startScan() {
        _isScanning.value = true
        _peripherals.value = emptyList()
    }

    fun stopScan() {
        _isScanning.value = false
    }

    fun addDiscoveredPeripheral(peripheral: BlePeripheral) {
        _peripherals.update { list ->
            if (list.none { it.address == peripheral.address }) {
                list + peripheral
            } else {
                list.map { if (it.address == peripheral.address) peripheral else it }
            }
        }
    }

    fun connectDevice(peripheral: BlePeripheral) {
        _connectedDevice.value = peripheral.copy(isConnected = true)
        _peripherals.update { list ->
            list.map { if (it.address == peripheral.address) it.copy(isConnected = true) else it.copy(isConnected = false) }
        }
        loadGattServices(peripheral)
    }

    fun disconnectDevice() {
        _connectedDevice.value = null
        _services.value = emptyList()
        _peripherals.update { list -> list.map { it.copy(isConnected = false) } }
    }

    private fun loadGattServices(peripheral: BlePeripheral) {
        val list = mutableListOf<GattServiceInfo>()

        list.add(
            GattServiceInfo(
                uuid = "0000180a-0000-1000-8000-00805f9b34fb",
                name = resolveServiceName("180a"),
                characteristics = listOf(
                    GattCharacteristicInfo("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name", listOf("READ"), "Arinara Labs"),
                    GattCharacteristicInfo("00002a24-0000-1000-8000-00805f9b34fb", "Model Number", listOf("READ"), "AR-IOT-V2"),
                    GattCharacteristicInfo("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision", listOf("READ"), "v1.4.2-release")
                )
            )
        )

        list.add(
            GattServiceInfo(
                uuid = "0000180f-0000-1000-8000-00805f9b34fb",
                name = resolveServiceName("180f"),
                characteristics = listOf(
                    GattCharacteristicInfo("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level", listOf("READ", "NOTIFY"), "88%")
                )
            )
        )

        if (peripheral.advertisedServices.any { it.contains("181a", ignoreCase = true) }) {
            list.add(
                GattServiceInfo(
                    uuid = "0000181a-0000-1000-8000-00805f9b34fb",
                    name = resolveServiceName("181a"),
                    characteristics = listOf(
                        GattCharacteristicInfo("00002a6e-0000-1000-8000-00805f9b34fb", "Temperature", listOf("READ", "NOTIFY"), "24.6 °C"),
                        GattCharacteristicInfo("00002a6f-0000-1000-8000-00805f9b34fb", "Humidity", listOf("READ", "NOTIFY"), "55.2 %")
                    )
                )
            )
        }

        _services.value = list
    }
}
