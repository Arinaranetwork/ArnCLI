// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class WolResult(
    val macAddress: String,
    val broadcastIp: String,
    val port: Int,
    val bytesSent: Int,
    val durationMs: Double,
    val formattedMac: String
)

/**
 * Wake-on-LAN (WoL) Magic Packet transmission engine.
 * Assembles standard 102-byte payloads (6x 0xFF + 16x MAC repetitions) and broadcasts via UDP.
 */
class WolEngine {

    companion object {
        const val DEFAULT_PORT = 9
        const val ALTERNATE_PORT = 7
        const val DEFAULT_BROADCAST_IP = "255.255.255.255"
        const val MAGIC_PACKET_LENGTH = 102

        fun parseMac(macStr: String): ByteArray {
            val clean = macStr.trim().replace(":", "").replace("-", "")
            require(clean.length == 12) { "MAC address must be 12 hexadecimal characters (e.g. AA:BB:CC:DD:EE:FF)" }
            require(clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                "MAC address contains invalid hexadecimal characters"
            }
            val bytes = ByteArray(6)
            for (i in 0 until 6) {
                bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            return bytes
        }

        fun formatMac(macBytes: ByteArray): String {
            return macBytes.joinToString(":") { String.format("%02X", it) }
        }

        fun buildMagicPacket(macBytes: ByteArray): ByteArray {
            require(macBytes.size == 6) { "MAC bytes must be exactly 6 bytes" }
            val packet = ByteArray(MAGIC_PACKET_LENGTH)
            // 6 bytes of 0xFF
            for (i in 0 until 6) {
                packet[i] = 0xFF.toByte()
            }
            // 16 repetitions of MAC
            for (rep in 0 until 16) {
                System.arraycopy(macBytes, 0, packet, 6 + rep * 6, 6)
            }
            return packet
        }
    }

    suspend fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = DEFAULT_BROADCAST_IP,
        port: Int = DEFAULT_PORT
    ): Result<WolResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(port in 1..65535) { "Port must be between 1 and 65535" }
            val cleanIp = broadcastIp.trim()
            require(cleanIp.isNotBlank()) { "Broadcast IP cannot be blank" }

            val startNano = System.nanoTime()
            val macBytes = parseMac(macAddress)
            val formatted = formatMac(macBytes)
            val packetData = buildMagicPacket(macBytes)
            val targetAddress = InetAddress.getByName(cleanIp)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                val packet = DatagramPacket(packetData, packetData.size, targetAddress, port)
                socket.send(packet)
            }

            val durationMs = (System.nanoTime() - startNano) / 1_000_000.0
            WolResult(
                macAddress = macAddress,
                broadcastIp = cleanIp,
                port = port,
                bytesSent = packetData.size,
                durationMs = durationMs,
                formattedMac = formatted
            )
        }
    }
}
