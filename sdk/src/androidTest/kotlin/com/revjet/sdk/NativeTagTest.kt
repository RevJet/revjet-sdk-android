package com.revjet.sdk

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Loads the live native test tag. Needs a network connection. */
@RunWith(AndroidJUnit4::class)
class NativeTagTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var tag: Tag

    @Before
    fun setUp() {
        RevJetSDK.setDebugEnabled(true)
        tag =
            Tag(
                context = context,
                type = TagType.NATIVE,
                tag = "tag355022",
                key = "b37",
                debugMode = DebugMode.EMULATE,
            )
    }

    @After
    fun tearDown() {
        tag.destroy()
        RevJetSDK.setDebugEnabled(false)
    }

    @Test
    fun loads_an_ad_and_reports_it() =
        runBlocking {
            tag.reload()

            withTimeout(TIMEOUT_MS) { tag.isLoaded.first { it } }
            // `isLoaded` flips before the load settles, so wait for both
            withTimeout(TIMEOUT_MS) { tag.isLoading.first { !it } }

            val response = requireNotNull(nativeModel.response.value)
            assertEquals("the response carries the tag it was requested for", "tag355022", response.context.tag)
            assertTrue("a width is reported", response.width.isNotEmpty())
            assertTrue(
                "the click URL is the ad server's",
                response.linkValue.startsWith("https://ads.revjet.com/click/"),
            )
            assertNotNull("the personalization payload is handed over", response.data)
            assertEquals("loading has finished", false, tag.isLoading.value)
        }

    @Test
    fun reports_loading_on_the_main_thread() {
        val onMainThread = CountDownLatch(1)
        tag.onLoad = { if (Looper.myLooper() == Looper.getMainLooper()) onMainThread.countDown() }

        tag.reload()

        assertTrue(
            "the state the timers also touch must change on the main thread",
            onMainThread.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
        )
    }

    @Test
    fun reports_the_tracking_events_of_a_load() {
        val reported = mutableListOf<String>()
        val received = CountDownLatch(2)
        tag.onTrackingEvent = { event ->
            reported += event["type"] as String
            received.countDown()
        }

        tag.reload()

        assertTrue(received.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue("$reported", reported.containsAll(listOf("ad_load_start", "ad_loaded")))
    }

    @Test
    fun resolves_a_click_to_where_the_ad_server_points() =
        runBlocking {
            tag.reload()
            withTimeout(TIMEOUT_MS) { tag.isLoaded.first { it } }

            val clicked = CountDownLatch(1)
            var destination: String? = null
            tag.onClick = {
                destination = it.toString()
                clicked.countDown()
            }

            tag.goToLP()

            assertTrue("the click is reported", clicked.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
            assertTrue("a resolved destination: $destination", destination.orEmpty().startsWith("http"))
        }

    @Test
    fun a_second_ad_reports_its_own_viewability() {
        val seen = java.util.concurrent.CopyOnWriteArrayList<String>()
        tag.onTrackingEvent = { event -> seen += event["type"] as String }

        loadAndWatch()
        assertTrue("the first ad should become viewable: $seen", awaitUntil(10_000) { seen.contains("ad_viewable") })

        seen.clear()
        loadAndWatch()

        assertTrue("the ad shown after a reload should too: $seen", awaitUntil(10_000) { seen.contains("ad_viewable") })
    }

    /** Loads, then tells the tag the ad is wholly on screen, as a mounted view would. */
    private fun loadAndWatch() =
        runBlocking {
            tag.reload()
            withTimeout(TIMEOUT_MS) { tag.isLoaded.first { it } }

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                nativeModel.updateExposure(
                    com.revjet.sdk.internal
                        .ExposureData(exposedPercentage = 100f),
                )
            }
        }

    private val nativeModel get() = tag.model as com.revjet.sdk.internal.NativeTag

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}
