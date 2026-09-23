package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** What the JavaScript tag reports back through the bridge, against the live test tag. */
@RunWith(AndroidJUnit4::class)
class WebTagTest {
    @Before
    fun requireAWebViewThatCanHostAnAd() {
        WebViewFeatures.assumeAdsCanBeHosted()
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var host: WebTagHost? = null

    private fun host(updateDynamicHeight: Boolean = false): WebTagHost {
        val tag =
            Tag(
                context = context,
                type = TagType.WEB_BASED,
                tag = "tag390451",
                key = "7c2",
                plcId = "266512461",
                updateDynamicHeight = updateDynamicHeight,
                debugMode = DebugMode.EMULATE,
                options = listOf(Option.Responsive(true), Option.ResponsiveHeight(ResponsiveDimension.DYNAMIC)),
            )

        return WebTagHost(tag).also { host = it }
    }

    @After
    fun tearDown() {
        host?.close()
    }

    @Test
    fun the_creative_reports_that_it_loaded() {
        val host = host()
        host.mountAndLoad()

        assertTrue("the tag script's onload should reach the SDK", host.awaitLoaded())
    }

    @Test
    fun a_responsive_tag_reports_the_height_it_asks_for() {
        val host = host(updateDynamicHeight = true)
        host.mountAndLoad()
        assertTrue(host.awaitLoaded())

        awaitUntil(timeoutMs = 30_000, intervalMs = 200) { host.tag.responsiveHeight.value != null }

        val height = host.tag.responsiveHeight.value
        assertTrue("a height should be reported, was $height", (height ?: 0f) > 0f)
    }

    @Test
    fun a_tag_that_did_not_ask_for_dynamic_height_reports_none() {
        val host = host(updateDynamicHeight = false)
        host.mountAndLoad()
        assertTrue(host.awaitLoaded())
        Thread.sleep(3_000)

        assertNull("the height is only reported when it was asked for", host.tag.responsiveHeight.value)
    }

    @Test
    fun a_click_without_a_destination_is_reported() {
        assertTrue(postAndAwaitError("onClick", "42") is RevJetError.NoClickURL)
    }

    @Test
    fun a_tracking_event_without_details_is_reported() {
        assertTrue(
            postAndAwaitError("onTrackingEvent", "'not an object'") is RevJetError.TrackingEventDetailsUnavailable,
        )
    }

    /** Posts a message to the SDK as the ad document would, and returns the error it reports. */
    private fun postAndAwaitError(
        name: String,
        body: String,
    ): Throwable? {
        val host = host()
        val reported = CountDownLatch(1)
        var error: Throwable? = null
        host.tag.onError = {
            error = it
            reported.countDown()
        }
        host.mountAndLoad()
        assertTrue(host.awaitLoaded())

        host.evaluate("window.__revjetPost('$name', $body)")

        assertTrue("an error should be reported", reported.await(10, TimeUnit.SECONDS))
        return error
    }
}
