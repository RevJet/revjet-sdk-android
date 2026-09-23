package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class RedirectResolverTest {
    private lateinit var server: ServerSocket
    private val requests = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        server = ServerSocket(0, 0, java.net.InetAddress.getByName("127.0.0.1"))

        thread(isDaemon = true) {
            while (!server.isClosed) {
                runCatching {
                    server.accept().use { socket ->
                        val line = socket.getInputStream().bufferedReader().readLine()
                        if (line != null) requests += line

                        socket.getOutputStream().write(
                            "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray(),
                        )
                    }
                }
            }
        }
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `a click into the device is refused before anything is sent`() =
        runBlocking {
            val url = "http://127.0.0.1:${server.localPort}/admin"

            try {
                RedirectResolver.resolve(url)
                fail("the loopback interface should not be reachable")
            } catch (error: RevJetError.BlockedDestination) {
                assertEquals(url, error.url)
            }

            Thread.sleep(300)
            assertTrue("no request should reach the listener", requests.isEmpty())
        }

    @Test
    fun `a public destination that does not redirect is returned as it is`() =
        runBlocking {
            assertEquals("https://example.com/", RedirectResolver.resolve("https://example.com/"))
        }

    @Test
    fun `a destination outside the web is handed over without being requested`() =
        runBlocking {
            for (url in listOf(
                "market://details?id=com.example",
                "myapp://product/42",
                "tel:+15551234567",
                "mailto:a@b.c",
            )) {
                assertEquals(url, RedirectResolver.resolve(url))
            }
        }

    @Test
    fun `a scheme that is not a destination is refused`() =
        runBlocking {
            val refused =
                listOf(
                    "javascript:alert(1)",
                    "file:///data/data/com.example/secrets",
                    "content://com.example.provider/secrets",
                    "intent://scan/#Intent;scheme=zxing;end",
                    "data:text/html,hi",
                )

            for (url in refused) {
                try {
                    RedirectResolver.resolve(url)
                    fail("$url should be refused")
                } catch (error: RevJetError.BlockedDestination) {
                    assertEquals(url, error.url)
                }
            }
        }

    @Test
    fun `a redirect leads to an absolute location whatever its scheme, and resolves a relative one`() {
        val current = "https://ads.revjet.com/click/abc"

        assertEquals(
            "market://details?id=com.example",
            RedirectResolver.next(current, "market://details?id=com.example"),
        )
        assertEquals("https://ads.revjet.com/landing?x=1", RedirectResolver.next(current, "/landing?x=1"))
        assertEquals("https://cdn.example.com/a", RedirectResolver.next(current, "//cdn.example.com/a"))
    }

    @Test
    fun `a pixel into the device is not sent`() =
        runBlocking {
            assertFalse(Http.fire("http://127.0.0.1:${server.localPort}/pixel"))

            Thread.sleep(300)
            assertTrue("no request should reach the listener", requests.isEmpty())
        }
}
