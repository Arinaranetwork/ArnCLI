// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.net

/**
 * On-device offline IEEE Organizationally Unique Identifier (OUI) hardware vendor resolver.
 * Maps MAC address prefixes to manufacturers with zero cloud or network overhead.
 */
class IeeeOuiLookupEngine {

    companion object {
        private val OUI_DATABASE = mapOf(
            // Raspberry Pi
            "B827EB" to "Raspberry Pi Foundation",
            "DCA632" to "Raspberry Pi Foundation",
            "E45F01" to "Raspberry Pi Foundation",
            "28CDC1" to "Raspberry Pi Foundation",
            "D83ADD" to "Raspberry Pi Foundation",

            // Espressif (ESP32 / ESP8266 IoT)
            "240AC4" to "Espressif Inc.",
            "2462AB" to "Espressif Inc.",
            "246F28" to "Espressif Inc.",
            "24B2DE" to "Espressif Inc.",
            "2C3AE8" to "Espressif Inc.",
            "2CF432" to "Espressif Inc.",
            "30AEA4" to "Espressif Inc.",
            "3C6105" to "Espressif Inc.",
            "3C71BF" to "Espressif Inc.",
            "483FDA" to "Espressif Inc.",
            "485519" to "Espressif Inc.",
            "4C11AE" to "Espressif Inc.",
            "545AA6" to "Espressif Inc.",
            "5CCF7F" to "Espressif Inc.",
            "600194" to "Espressif Inc.",
            "68C63A" to "Espressif Inc.",
            "70039F" to "Espressif Inc.",
            "7CDFA1" to "Espressif Inc.",
            "807D3A" to "Espressif Inc.",
            "840D8E" to "Espressif Inc.",
            "84F3EB" to "Espressif Inc.",
            "9097D5" to "Espressif Inc.",
            "94B97E" to "Espressif Inc.",
            "A4CF12" to "Espressif Inc.",
            "A4E57C" to "Espressif Inc.",
            "AC67B2" to "Espressif Inc.",
            "B4E62D" to "Espressif Inc.",
            "BCDC2" to "Espressif Inc.",
            "C44F33" to "Espressif Inc.",
            "C82B96" to "Espressif Inc.",
            "CC50E3" to "Espressif Inc.",
            "D4D4DA" to "Espressif Inc.",
            "D8A01D" to "Espressif Inc.",
            "DC4F22" to "Espressif Inc.",
            "E09806" to "Espressif Inc.",
            "E831CD" to "Espressif Inc.",
            "ECFABC" to "Espressif Inc.",

            // Apple
            "000393" to "Apple, Inc.",
            "000502" to "Apple, Inc.",
            "000A27" to "Apple, Inc.",
            "000A95" to "Apple, Inc.",
            "000D93" to "Apple, Inc.",
            "001124" to "Apple, Inc.",
            "001451" to "Apple, Inc.",
            "0016CB" to "Apple, Inc.",
            "0017F2" to "Apple, Inc.",
            "0019E3" to "Apple, Inc.",
            "001B63" to "Apple, Inc.",
            "001CB3" to "Apple, Inc.",
            "001E52" to "Apple, Inc.",
            "001EC2" to "Apple, Inc.",
            "0021E9" to "Apple, Inc.",
            "002241" to "Apple, Inc.",
            "002312" to "Apple, Inc.",
            "002332" to "Apple, Inc.",
            "00236C" to "Apple, Inc.",
            "002436" to "Apple, Inc.",
            "002500" to "Apple, Inc.",
            "00254B" to "Apple, Inc.",
            "002608" to "Apple, Inc.",
            "00264A" to "Apple, Inc.",
            "0026B0" to "Apple, Inc.",
            "3C0754" to "Apple, Inc.",
            "3C15C2" to "Apple, Inc.",
            "406C8F" to "Apple, Inc.",
            "442A60" to "Apple, Inc.",
            "48605F" to "Apple, Inc.",
            "6476BA" to "Apple, Inc.",
            "705681" to "Apple, Inc.",
            "7C04D0" to "Apple, Inc.",
            "88665A" to "Apple, Inc.",
            "9C207B" to "Apple, Inc.",
            "A483E7" to "Apple, Inc.",
            "ACBC32" to "Apple, Inc.",
            "B8782E" to "Apple, Inc.",
            "BC5436" to "Apple, Inc.",
            "C82A14" to "Apple, Inc.",
            "D89695" to "Apple, Inc.",
            "DC2B61" to "Apple, Inc.",
            "F40F24" to "Apple, Inc.",

            // Cisco Systems
            "00000C" to "Cisco Systems",
            "000142" to "Cisco Systems",
            "000143" to "Cisco Systems",
            "000163" to "Cisco Systems",
            "000164" to "Cisco Systems",
            "000196" to "Cisco Systems",
            "000197" to "Cisco Systems",
            "0001C7" to "Cisco Systems",
            "0001C9" to "Cisco Systems",
            "000216" to "Cisco Systems",
            "000217" to "Cisco Systems",
            "00024A" to "Cisco Systems",
            "00024B" to "Cisco Systems",
            "00027D" to "Cisco Systems",
            "00027E" to "Cisco Systems",
            "0002B9" to "Cisco Systems",
            "0002BA" to "Cisco Systems",
            "0002FC" to "Cisco Systems",
            "0002FD" to "Cisco Systems",
            "001A2B" to "Cisco Systems",

            // Intel
            "001E67" to "Intel Corporate",
            "00215A" to "Intel Corporate",
            "0024D7" to "Intel Corporate",
            "0026C6" to "Intel Corporate",
            "002710" to "Intel Corporate",
            "3C5282" to "Intel Corporate",
            "4851B7" to "Intel Corporate",
            "6805CA" to "Intel Corporate",
            "A44CC8" to "Intel Corporate",

            // Ubiquiti Networks (UniFi)
            "00156D" to "Ubiquiti Networks",
            "0418D6" to "Ubiquiti Networks",
            "24A43C" to "Ubiquiti Networks",
            "60E327" to "Ubiquiti Networks",
            "788A20" to "Ubiquiti Networks",
            "802AA8" to "Ubiquiti Networks",
            "DC9FDB" to "Ubiquiti Networks",
            "F09FC2" to "Ubiquiti Networks",

            // MikroTik
            "000C42" to "MikroTik",
            "2CC81B" to "MikroTik",
            "488F5A" to "MikroTik",
            "64D154" to "MikroTik",
            "CC2DE0" to "MikroTik",
            "D4CA6D" to "MikroTik",
            "E48D8C" to "MikroTik",

            // TP-Link
            "0014D1" to "TP-Link Corporation",
            "0018E7" to "TP-Link Corporation",
            "001C10" to "TP-Link Corporation",
            "002191" to "TP-Link Corporation",
            "0024A5" to "TP-Link Corporation",
            "00265A" to "TP-Link Corporation",
            "14D64D" to "TP-Link Corporation",
            "1868CB" to "TP-Link Corporation",
            "5065F3" to "TP-Link Corporation",
            "6038E0" to "TP-Link Corporation",
            "74DA38" to "TP-Link Corporation",
            "A42BB0" to "TP-Link Corporation",
            "B0487A" to "TP-Link Corporation",
            "C04A00" to "TP-Link Corporation",
            "C46E1F" to "TP-Link Corporation",
            "CC32E5" to "TP-Link Corporation",
            "CC3429" to "TP-Link Corporation",
            "E4BEED" to "TP-Link Corporation",
            "EC086B" to "TP-Link Corporation",

            // Virtualization & Containers
            "000C29" to "VMware, Inc.",
            "005056" to "VMware, Inc.",
            "000569" to "VMware, Inc.",
            "080027" to "Oracle VirtualBox",
            "525400" to "QEMU / KVM",

            // Storage / NAS
            "001132" to "Synology Inc.",
            "00270C" to "Synology Inc.",
            "00089B" to "QNAP Systems",
            "000ACD" to "Western Digital",

            // Smart Home / IoT
            "001788" to "Signify (Philips Hue)",
            "ECB5FA" to "Signify (Philips Hue)",
            "001DA5" to "Netgear Inc.",
            "002682" to "Netgear Inc.",
            "2816A8" to "Netgear Inc.",
            "9C8E99" to "Netgear Inc."
        )

        fun extractOui(macAddress: String): String? {
            val cleanHex = macAddress.filter { it.isLetterOrDigit() }.uppercase()
            if (cleanHex.length >= 6) {
                return cleanHex.substring(0, 6)
            }
            return null
        }
    }

    /**
     * Resolves the hardware vendor name for a given MAC address.
     * Returns null if unmapped.
     */
    fun lookupVendor(macAddress: String): String? {
        val oui = extractOui(macAddress) ?: return null
        return OUI_DATABASE[oui]
    }
}
