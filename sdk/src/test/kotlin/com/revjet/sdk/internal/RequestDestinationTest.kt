package com.revjet.sdk.internal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which destinations the SDK is willing to request.
 *
 * Content shown by the SDK chooses click URLs, so reaching the device or its network from one is
 * the vector this closes.
 */
class RequestDestinationTest {
    @Test
    fun `destinations inside the device or its network are refused`() =
        runBlocking {
            val refused =
                listOf(
                    "http://127.0.0.1:8080/admin",
                    "http://localhost:8080/admin",
                    "http://[::1]:8080/admin",
                    "http://[::ffff:127.0.0.1]/admin",
                    "http://10.0.0.5/",
                    "http://172.16.0.5/",
                    "http://192.168.1.1/",
                    "http://169.254.169.254/latest/meta-data/",
                    "http://100.64.0.1/",
                    "http://224.0.0.1/",
                    "http://255.255.255.255/",
                    "http://0.0.0.0/",
                )

            refused.forEach { assertFalse(it, RequestDestination.isAllowed(it)) }
        }

    @Test
    fun `the numeric forms of loopback are refused as well`() =
        runBlocking {
            // The same address, written the ways a URL parser still accepts
            listOf("http://127.1/", "http://2130706433/", "http://0x7f.0.0.1/")
                .forEach { assertFalse(it, RequestDestination.isAllowed(it)) }
        }

    @Test
    fun `a host that resolves inward is refused, whatever it is called`() =
        runBlocking {
            assertFalse(RequestDestination.isAllowed("http://127.0.0.1.nip.io/"))
        }

    @Test
    fun `schemes other than http are refused`() =
        runBlocking {
            listOf("file:///etc/passwd", "ftp://example.com/", "tel:+15550100", "about:blank")
                .forEach { assertFalse(it, RequestDestination.isAllowed(it)) }
        }

    @Test
    fun `publicly routable destinations are allowed`() =
        runBlocking {
            listOf("https://ads.revjet.com/click/tag355022/1", "https://example.com/")
                .forEach { assertTrue(it, RequestDestination.isAllowed(it)) }
        }
}
