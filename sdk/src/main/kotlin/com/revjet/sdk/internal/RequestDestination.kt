package com.revjet.sdk.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URI

/**
 * Decides whether the SDK may send a request to a URL.
 *
 * Content shown by the SDK chooses the URL of a click, so the SDK refuses to reach anything that
 * is not publicly routable. Hosts are resolved rather than compared as text, which also covers the
 * alternative numeric forms of an address, such as `127.1` or `2130706433`.
 */
internal object RequestDestination {
    private val allowedSchemes = setOf("http", "https")

    /** Whether a request may be sent to the URL. */
    suspend fun isAllowed(url: String): Boolean =
        withContext(Dispatchers.IO) {
            val host =
                runCatching { URI(url) }.getOrNull()?.let { uri ->
                    if (uri.scheme?.lowercase() !in allowedSchemes) return@withContext false
                    uri.host
                }

            if (host.isNullOrEmpty()) return@withContext false

            val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull()
            if (addresses.isNullOrEmpty()) return@withContext false

            addresses.all { isPubliclyRoutable(it) }
        }

    private fun isPubliclyRoutable(address: InetAddress): Boolean {
        val bytes = address.address

        return when (bytes.size) {
            4 -> isPubliclyRoutable(bytes.toIPv4())
            16 -> isPubliclyRoutable(bytes)
            else -> false
        }
    }

    private fun ByteArray.toIPv4(): Long {
        var value = 0L
        for (byte in this) {
            value = (value shl 8) or (byte.toLong() and 0xFF)
        }

        return value
    }

    private fun isPubliclyRoutable(value: Long): Boolean {
        when ((value ushr 24).toInt()) {
            0, 10, 127 -> return false // this network, private, loopback
        }

        when ((value ushr 28).toInt()) {
            0xE, 0xF -> return false // multicast, reserved, broadcast
        }

        if (value and 0xFFC0_0000L == 0x6440_0000L) return false // 100.64/10, carrier grade NAT
        if (value and 0xFFF0_0000L == 0xAC10_0000L) return false // 172.16/12, private
        if (value and 0xFFFF_0000L == 0xC0A8_0000L) return false // 192.168/16, private
        if (value and 0xFFFF_0000L == 0xA9FE_0000L) return false // 169.254/16, link local
        if (value and 0xFFFE_0000L == 0xC612_0000L) return false // 198.18/15, benchmarking

        return true
    }

    private fun isPubliclyRoutable(bytes: ByteArray): Boolean {
        // An IPv4 mapped or compatible address carries the address in its last four bytes
        val prefix = bytes.take(12)
        val isMapped =
            prefix.take(10).all { it == 0.toByte() } &&
                prefix[10] == 0xFF.toByte() &&
                prefix[11] == 0xFF.toByte()

        if (isMapped || prefix.all { it == 0.toByte() }) {
            val value = bytes.copyOfRange(12, 16).toIPv4()

            // `::` and `::1` are the unspecified and loopback addresses
            return if (value <= 1L) false else isPubliclyRoutable(value)
        }

        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF

        if (first == 0xFF) return false // multicast
        if (first and 0xFE == 0xFC) return false // unique local
        if (first == 0xFE && second and 0xC0 == 0x80) return false // link local

        return true
    }
}
