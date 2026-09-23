package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
