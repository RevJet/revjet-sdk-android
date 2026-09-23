package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * The boundary between creative content and the SDK's own networking and navigation.
 *
 * Each test covers a way creative JavaScript could misuse the SDK: driving a native request to an
 * address it should not reach, reaching the bridge from a frame the SDK does not control, or
 * navigating the ad document away.
 */
@RunWith(AndroidJUnit4::class)
class SecurityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var host: WebTagHost
    private lateinit var listener: LoopbackListener

    @Before
    fun setUp() {
        WebViewFeatures.assumeAdsCanBeHosted()
        listener = LoopbackListener()
        host =
            WebTagHost(
                Tag(
                    context = context,
                    type = TagType.WEB_BASED,
                    tag = "tag390451",
                    key = "7c2",
                    plcId = "266512461",
                    debugMode = DebugMode.EMULATE,
                ),
            )
        host.mountAndLoad()
        assertTrue("MRAID should initialize", host.awaitReady())
    }

    @After
    fun tearDown() {
        // The set-up is skipped where the WebView cannot host an ad at all
        if (::listener.isInitialized) listener.close()
        if (::host.isInitialized) host.close()
    }

    @Test
    fun a_creative_cannot_reach_the_loopback_interface() {
        val refused = CountDownLatch(1)
        var reported: Throwable? = null
        host.tag.onError = {
            reported = it
            refused.countDown()
        }

        host.evaluate("(window.mraid.open('http://127.0.0.1:${listener.port}/admin'), 'called')")

        assertTrue("the click should be refused", refused.await(20, TimeUnit.SECONDS))
        assertTrue(
            "the publisher should be told the destination was refused, was $reported",
            reported is RevJetError.BlockedDestination,
        )
        Thread.sleep(500)
        assertTrue("no request should reach the loopback interface", listener.requests.isEmpty())
    }

    @Test
    fun a_creative_cannot_reach_the_private_network() {
        val refused = CountDownLatch(1)
        var reported: Throwable? = null
        host.tag.onError = {
            reported = it
            refused.countDown()
        }

        host.evaluate("(window.mraid.open('http://10.0.2.2:8080/admin'), 'called')")

        assertTrue("the click should be refused", refused.await(20, TimeUnit.SECONDS))
        assertTrue("was $reported", reported is RevJetError.BlockedDestination)
    }

    @Test
    fun the_bridge_is_not_injected_into_an_opaque_frame() {
        val reached =
            host.evaluate(
                """
                (function() {
                  var blank = document.createElement('iframe');
                  document.body.appendChild(blank);
                  return typeof blank.contentWindow.RevJetBridge;
                })()
                """.trimIndent(),
            )

        assertEquals("an about:blank frame has no origin the rules match", "undefined", reached)
    }

    @Test
    fun messages_from_a_srcdoc_frame_are_ignored() {
        val clicked = CountDownLatch(1)
        host.tag.onClick = { clicked.countDown() }

        // A srcdoc frame inherits the ad document's origin, so it does receive the bridge: which
        // frame a message came from is what stops it being acted on. A public destination is used
        // so that only the frame decides the outcome.
        host.evaluate(
            """
            (function() {
              var frame = document.createElement('iframe');
              frame.srcdoc = "<script>parent.__srcdocBridge = typeof window.RevJetBridge;" +
                "window.RevJetBridge.postMessage(JSON.stringify(" +
                "{ name: 'onClick', body: 'https://example.com/' }))<\/script>";
              document.body.appendChild(frame);
              return 'appended';
            })()
            """.trimIndent(),
        )

        assertTrue(
            "the frame can reach the bridge",
            host.evaluateUntil("window.__srcdocBridge", "object"),
        )
        assertEquals("but the SDK ignores what it says", false, clicked.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun messages_from_a_same_origin_frame_are_ignored() {
        val clicked = CountDownLatch(1)
        host.tag.onClick = { clicked.countDown() }

        // A frame of the ad document's own origin does receive the bridge, so which frame a
        // message came from is what stops it being acted on
        host.evaluate(
            """
            (function() {
              var frame = document.createElement('iframe');
              frame.src = 'https://ads.revjet.com/bg';
              document.body.appendChild(frame);
              window.__frame = frame;
              return 'appended';
            })()
            """.trimIndent(),
        )
        assertTrue(
            "the same-origin frame should receive the bridge",
            host.evaluateUntil("typeof window.__frame.contentWindow.RevJetBridge", "object"),
        )

        // A public destination, so that only the frame decides the outcome
        val posted =
            host.evaluate(
                """
                (function() {
                  try {
                    window.__frame.contentWindow.RevJetBridge.postMessage(
                      JSON.stringify({ name: 'onClick', body: 'https://example.com/' }));
                    return 'posted';
                  } catch (error) {
                    return 'threw: ' + error.message;
                  }
                })()
                """.trimIndent(),
            )

        assertEquals("the frame can reach the bridge", "posted", posted)
        assertEquals("but the SDK ignores what it says", false, clicked.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun mraid_is_reachable_only_through_the_main_frame() {
        val report =
            host.evaluate(
                """
                (function() {
                  var frame = document.createElement('iframe');
                  document.body.appendChild(frame);
                  return [typeof frame.contentWindow.mraid, typeof frame.contentWindow.top.mraid].join(',');
                })()
                """.trimIndent(),
            )

        assertEquals("a creative in a frame reaches MRAID through the top window", "undefined,object", report)
    }

    @Test
    fun a_click_from_the_ad_document_still_resolves() {
        val clicked = CountDownLatch(1)
        var destination: String? = null
        host.tag.onClick = {
            destination = it.toString()
            clicked.countDown()
        }

        host.evaluate("(window.mraid.open('https://example.com/'), 'called')")

        assertTrue("a public destination is still resolved", clicked.await(20, TimeUnit.SECONDS))
        assertEquals("https://example.com/", destination)
    }

    @Test
    fun a_creative_cannot_navigate_the_ad_document_away() {
        host.evaluate("(location.href = 'https://example.com/', 'navigating')")
        Thread.sleep(3_000)

        assertEquals(
            "the ad document should still be in place",
            "object",
            host.evaluate("typeof window.mraid"),
        )
        assertEquals("ads.revjet.com", host.evaluate("location.hostname"))
    }

    @Test
    fun creative_frames_can_still_navigate() {
        host.evaluate(
            """
            (function() {
              window.__frameLoaded = false;
              var frame = document.createElement('iframe');
              frame.srcdoc = "<script>parent.__frameLoaded = true<\/script>";
              document.body.appendChild(frame);
              return 'appended';
            })()
            """.trimIndent(),
        )

        assertTrue(
            "a creative's own frames must keep loading",
            host.evaluateUntil("window.__frameLoaded", "true"),
        )
    }

    /** An HTTP listener on the loopback interface, to observe requests that should never arrive. */
    private class LoopbackListener {
        private val server = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
        private val received = CopyOnWriteArrayList<String>()

        val port: Int get() = server.localPort
        val requests: List<String> get() = received

        init {
            thread(isDaemon = true) {
                while (!server.isClosed) {
                    runCatching {
                        server.accept().use { socket ->
                            socket
                                .getInputStream()
                                .bufferedReader()
                                .readLine()
                                ?.let { received += it }
                            socket.getOutputStream().write(
                                "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray(),
                            )
                        }
                    }
                }
            }
        }

        fun close() = server.close()
    }
}
