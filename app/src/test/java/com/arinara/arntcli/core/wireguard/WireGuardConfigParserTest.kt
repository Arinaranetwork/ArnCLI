// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.core.wireguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigParserTest {

    private val validPrivKey = "aA11bB22cC33dD44eE55fF66gG77hH88iI99jJ00kK8="
    private val validPubKey1 = "88Hh77Gg66Ff55Ee44Dd33Cc22Bb11Aa00Jj99Ii88M="
    private val validPubKey2 = "11Aa22Bb33Cc44Dd55Ee66Ff77Gg88Hh99Ii00Jj11K="

    @Test
    fun isValidKey_detectsCorrectKeys() {
        assertTrue(WireGuardConfigParser.isValidKey(validPrivKey))
        assertTrue(WireGuardConfigParser.isValidKey(validPubKey1))
        assertFalse(WireGuardConfigParser.isValidKey("too-short"))
        assertFalse(WireGuardConfigParser.isValidKey("invalid-base64-character-@@@@@@@@@@@@@@@@=="))
        assertFalse(WireGuardConfigParser.isValidKey(""))
    }

    @Test
    fun parse_standardSinglePeerConfig_succeeds() {
        val conf = """
            # Home WireGuard Gateway configuration
            [Interface]
            PrivateKey = $validPrivKey
            Address = 10.8.0.2/24, fd00::2/64
            DNS = 1.1.1.1, 8.8.8.8
            MTU = 1420

            [Peer]
            PublicKey = $validPubKey1
            Endpoint = vpn.home.example.com:51820
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
        """.trimIndent()

        val result = WireGuardConfigParser.parse("wg0-home", conf)
        assertTrue(result.isSuccess)

        val config = result.getOrThrow()
        assertEquals("wg0-home", config.name)
        assertEquals(validPrivKey, config.iface.privateKey)
        assertEquals(listOf("10.8.0.2/24", "fd00::2/64"), config.iface.addresses)
        assertEquals(listOf("1.1.1.1", "8.8.8.8"), config.iface.dnsServers)
        assertEquals(1420, config.iface.mtu)

        assertEquals(1, config.peers.size)
        val peer = config.peers.first()
        assertEquals(validPubKey1, peer.publicKey)
        assertEquals("vpn.home.example.com:51820", peer.endpoint)
        assertEquals(listOf("0.0.0.0/0", "::/0"), peer.allowedIps)
        assertEquals(25, peer.persistentKeepalive)
    }

    @Test
    fun parse_multiplePeers_succeeds() {
        val conf = """
            [Interface]
            PrivateKey = $validPrivKey
            Address = 10.10.0.1/24
            ListenPort = 51820

            [Peer]
            PublicKey = $validPubKey1
            AllowedIPs = 10.10.0.2/32

            [Peer]
            PublicKey = $validPubKey2
            AllowedIPs = 10.10.0.3/32
            PersistentKeepalive = 30
        """.trimIndent()

        val result = WireGuardConfigParser.parse("server", conf).getOrThrow()
        assertEquals(2, result.peers.size)
        assertEquals(validPubKey1, result.peers[0].publicKey)
        assertEquals(validPubKey2, result.peers[1].publicKey)
    }

    @Test
    fun roundtrip_toConfString_preservesData() {
        val conf = """
            [Interface]
            PrivateKey = $validPrivKey
            Address = 10.8.0.2/24
            DNS = 1.1.1.1

            [Peer]
            PublicKey = $validPubKey1
            Endpoint = 198.51.100.1:51820
            AllowedIPs = 0.0.0.0/0
            PersistentKeepalive = 25
        """.trimIndent()

        val parsed = WireGuardConfigParser.parse("test", conf).getOrThrow()
        val confStr = parsed.toConfString()

        val reParsed = WireGuardConfigParser.parse("reparsed", confStr).getOrThrow()
        assertEquals(parsed.iface.privateKey, reParsed.iface.privateKey)
        assertEquals(parsed.iface.addresses, reParsed.iface.addresses)
        assertEquals(parsed.peers.first().publicKey, reParsed.peers.first().publicKey)
        assertEquals(parsed.peers.first().endpoint, reParsed.peers.first().endpoint)
    }

    @Test
    fun parse_missingInterface_fails() {
        val conf = """
            [Peer]
            PublicKey = $validPubKey1
        """.trimIndent()

        val result = WireGuardConfigParser.parse("bad", conf)
        assertTrue(result.isFailure)
    }

    @Test
    fun parse_invalidListenPort_fails() {
        val conf = """
            [Interface]
            PrivateKey = $validPrivKey
            Address = 10.0.0.1/24
            ListenPort = not-a-number

            [Peer]
            PublicKey = $validPubKey1
        """.trimIndent()

        val result = WireGuardConfigParser.parse("bad-port", conf)
        assertTrue(result.isFailure)
    }
}
