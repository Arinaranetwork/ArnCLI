// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

data class SubnetResult(
    val ipAddress: String,
    val prefixLength: Int,
    val netmask: String,
    val wildcardMask: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val firstUsableIp: String,
    val lastUsableIp: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val binaryNetmask: String,
    val ipClass: String
)

/**
 * Pure on-device IPv4 Subnet and CIDR calculation engine.
 */
object SubnetCalculator {

    fun calculate(cidrNotation: String): Result<SubnetResult> = runCatching {
        val trimmed = cidrNotation.trim()
        val parts = trimmed.split('/')
        require(parts.size == 2) { "Invalid CIDR format. Expected IP/prefix (e.g. 192.168.1.1/24)" }

        val ipStr = parts[0].trim()
        val prefix = parts[1].trim().toIntOrNull()
            ?: throw IllegalArgumentException("Prefix must be a valid integer between 0 and 32")

        require(prefix in 0..32) { "Prefix length must be between 0 and 32" }

        val ipInt = parseIpToInt(ipStr)
        val maskInt = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val wildcardInt = 0xFFFFFFFFL xor maskInt
        val networkInt = ipInt and maskInt
        val broadcastInt = networkInt or wildcardInt

        val totalHosts = if (prefix == 0) 4294967296L else 1L shl (32 - prefix)
        val usableHosts = when (prefix) {
            32 -> 1L
            31 -> 2L // RFC 3021 point-to-point
            else -> (totalHosts - 2).coerceAtLeast(0)
        }

        val firstUsableInt = when (prefix) {
            32 -> ipInt
            31 -> networkInt
            else -> networkInt + 1
        }

        val lastUsableInt = when (prefix) {
            32 -> ipInt
            31 -> broadcastInt
            else -> broadcastInt - 1
        }

        val ipClass = determineIpClassification(ipInt)
        val binaryMask = formatBinary(maskInt)

        SubnetResult(
            ipAddress = ipStr,
            prefixLength = prefix,
            netmask = intToIp(maskInt),
            wildcardMask = intToIp(wildcardInt),
            networkAddress = intToIp(networkInt),
            broadcastAddress = intToIp(broadcastInt),
            firstUsableIp = intToIp(firstUsableInt),
            lastUsableIp = intToIp(lastUsableInt),
            totalHosts = totalHosts,
            usableHosts = usableHosts,
            binaryNetmask = binaryMask,
            ipClass = ipClass
        )
    }

    private fun parseIpToInt(ipStr: String): Long {
        val octets = ipStr.split('.')
        require(octets.size == 4) { "IP address must contain exactly 4 octets" }

        var result = 0L
        for (octet in octets) {
            val num = octet.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid octet '$octet'. Must be numeric")
            require(num in 0..255) { "Octet $num out of range (0-255)" }
            result = (result shl 8) or (num.toLong() and 0xFFL)
        }
        return result
    }

    private fun intToIp(ipInt: Long): String {
        val octet1 = (ipInt shr 24) and 0xFFL
        val octet2 = (ipInt shr 16) and 0xFFL
        val octet3 = (ipInt shr 8) and 0xFFL
        val octet4 = ipInt and 0xFFL
        return "$octet1.$octet2.$octet3.$octet4"
    }

    private fun formatBinary(maskInt: Long): String {
        val b1 = ((maskInt shr 24) and 0xFFL).toString(2).padStart(8, '0')
        val b2 = ((maskInt shr 16) and 0xFFL).toString(2).padStart(8, '0')
        val b3 = ((maskInt shr 8) and 0xFFL).toString(2).padStart(8, '0')
        val b4 = (maskInt and 0xFFL).toString(2).padStart(8, '0')
        return "$b1.$b2.$b3.$b4"
    }

    private fun determineIpClassification(ipInt: Long): String {
        val firstOctet = ((ipInt shr 24) and 0xFFL).toInt()
        val secondOctet = ((ipInt shr 16) and 0xFFL).toInt()

        return when {
            firstOctet == 10 -> "Class A Private (RFC 1918)"
            firstOctet == 172 && secondOctet in 16..31 -> "Class B Private (RFC 1918)"
            firstOctet == 192 && secondOctet == 168 -> "Class C Private (RFC 1918)"
            firstOctet == 127 -> "Loopback Interface (RFC 1122)"
            firstOctet in 1..126 -> "Class A Public"
            firstOctet in 128..191 -> "Class B Public"
            firstOctet in 192..223 -> "Class C Public"
            firstOctet in 224..239 -> "Class D Multicast"
            else -> "Class E Reserved"
        }
    }
}
