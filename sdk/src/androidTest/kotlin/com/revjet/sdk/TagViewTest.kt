package com.revjet.sdk

import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The view that shows an ad, against the live native test tag. */
@RunWith(AndroidJUnit4::class)
class TagViewTest {
    private lateinit var scenario: ActivityScenario<TestHostActivity>
    private var tagView: RevJetTagView? = null

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
    }

    @After
    fun tearDown() {
        onMain { tagView?.tag?.destroy() }
        scenario.close()
    }

    @Test
    fun the_listener_renders_the_native_response() {
        val rendered = CountDownLatch(1)
        var content: View? = null

        mount(
            listener =
                object : Listener() {
                    override fun onNativeResponse(
                        view: RevJetTagView,
                        response: NativeTagResponse,
                        tag: Tag,
                    ): View? {
                        content =
                            TextView(view.context).apply {
                                text = response.context.tag
                                setBackgroundColor(Color.WHITE)
                            }
                        rendered.countDown()

                        return content
                    }
                },
        )

        assertTrue("the response should reach the listener", rendered.await(20, TimeUnit.SECONDS))
        onMain {
            assertNotNull(content)
            assertEquals("the ad's view is shown", tagView, content!!.parent.parent)
        }
    }

    @Test
    fun a_preloaded_tag_is_not_requested_again_when_it_is_shown() {
        val tag = nativeTag()
        val loaded = CountDownLatch(1)
        tag.preload(onLoad = { loaded.countDown() })
        assertTrue("the preload should finish", loaded.await(20, TimeUnit.SECONDS))

        var loadsAfterPreload = 0
        tag.onBeforeLoad = { loadsAfterPreload += 1 }

        mount(tag = tag)
        Thread.sleep(2_000)

        assertEquals("the preloaded ad should be shown, not requested again", 0, loadsAfterPreload)
    }

    @Test
    fun a_tap_is_a_click_and_a_scroll_is_not() {
        val events = CopyOnWriteArrayList<String>()
        val clicked = CountDownLatch(1)
        val tag = nativeTag()
        tag.onTrackingEvent = { event ->
            val type = event["type"] as String
            events += type
            if (type == "ad_clicked") clicked.countDown()
        }
        mount(tag = tag)
        assertTrue("the ad should load first", awaitLoaded(tag))

        // a touch that travels is a scroll, not a click
        onMain { touch(travel = 600f) }
        Thread.sleep(1_000)
        assertTrue("a scroll must not count as a click: $events", events.none { it == "ad_clicked" })

        onMain { touch(travel = 0f) }

        assertTrue("a tap should be reported", clicked.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun an_ad_on_screen_becomes_viewable() {
        val viewable = CountDownLatch(1)
        val tag = nativeTag()
        tag.onTrackingEvent = { event ->
            if (event["type"] == "ad_viewable") viewable.countDown()
        }

        // no listener, so nothing is rendered over the host: the host view is what gets measured
        mount(tag = tag)

        assertTrue(
            "exposure should reach the viewable timer",
            viewable.await(30, TimeUnit.SECONDS),
        )
    }

    @Test
    fun a_preloaded_web_tag_is_shown_rather_than_loaded_again() {
        WebViewFeatures.assumeAdsCanBeHosted()

        val tag = webTag()
        val loaded = CountDownLatch(1)
        tag.preload(onLoad = { loaded.countDown() })
        assertTrue("the preload should finish", loaded.await(30, TimeUnit.SECONDS))

        var loadsAfterPreload = 0
        tag.onBeforeLoad = { loadsAfterPreload += 1 }

        mount(tag = tag)
        Thread.sleep(2_000)

        assertEquals("the preloaded creative should be kept", 0, loadsAfterPreload)
        onMain {
            val webView = (tag.model as com.revjet.sdk.internal.WebBasedTag).webView
            assertEquals("the preloaded web view is what is shown", tagView, webView.parent.parent)
        }
    }

    @Test
    fun a_responsive_web_tag_sizes_the_view_it_is_mounted_in() {
        WebViewFeatures.assumeAdsCanBeHosted()

        val tag = webTag(updateDynamicHeight = true)
        mount(tag = tag)

        var height = 0
        val followed =
            awaitUntil(timeoutMs = 40_000, intervalMs = 200) {
                val reported = tag.responsiveHeight.value ?: return@awaitUntil false
                height = tagView!!.layoutParams.height

                reported > 0f && height == (reported * tagView!!.resources.displayMetrics.density).toInt()
            }

        assertTrue(
            "the view should follow the height the creative asks for, was $height " +
                "for ${tag.responsiveHeight.value}dp",
            followed,
        )
    }

    @Test
    fun a_tag_does_not_hold_on_to_the_view_that_showed_it() {
        val tag = nativeTag()
        var reference: java.lang.ref.WeakReference<RevJetTagView>? = null

        onMain {
            scenarioActivity { activity ->
                val view = RevJetTagView(activity, tag)
                view.mount(activity.container, MountPreset.FillParent())
                reference = java.lang.ref.WeakReference(view)

                activity.container.removeAllViews()
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // The tag is still alive here: it is what would keep the view, and its activity, alive
        assertTrue("the view should be collectable once it is gone: $tag", awaitCollected(reference!!))
    }

    private open class Listener : RevJetTagViewListener {
        override fun onClick(
            view: RevJetTagView,
            url: android.net.Uri,
            tag: Tag,
        ) = Unit
    }

    private fun webTag(updateDynamicHeight: Boolean = false) =
        Tag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            type = TagType.WEB_BASED,
            tag = "tag390451",
            key = "7c2",
            plcId = "266512461",
            updateDynamicHeight = updateDynamicHeight,
            debugMode = DebugMode.EMULATE,
            options = listOf(Option.Responsive(true), Option.ResponsiveHeight(ResponsiveDimension.DYNAMIC)),
        )

    private fun nativeTag() =
        Tag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            type = TagType.NATIVE,
            tag = "tag355022",
            key = "b37",
            debugMode = DebugMode.EMULATE,
        )

    private fun mount(
        tag: Tag = nativeTag(),
        listener: RevJetTagViewListener? = null,
    ) {
        onMain {
            scenarioActivity { activity ->
                tagView =
                    RevJetTagView(activity, tag, listener).also {
                        it.mount(activity.container, MountPreset.Top(Insets.of(0, 400, 0, 0)))
                        if (!tag.updateDynamicHeight) {
                            it.layoutParams = it.layoutParams.also { params -> params.height = 600 }
                        }
                    }
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun awaitLoaded(tag: Tag): Boolean = awaitUntil { tag.isLoaded.value }

    /** Sends a touch to the ad view, travelling [travel] pixels before it is lifted. */
    private fun touch(travel: Float) {
        val view = tagView ?: return
        val time = SystemClock.uptimeMillis()
        val x = view.width / 2f
        val y = view.height / 2f

        view.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0))
        if (travel > 0f) {
            view.dispatchTouchEvent(
                MotionEvent.obtain(time, time + 50, MotionEvent.ACTION_MOVE, x, y - travel, 0),
            )
        }
        view.dispatchTouchEvent(
            MotionEvent.obtain(time, time + 80, MotionEvent.ACTION_UP, x, y - travel, 0),
        )
    }

    private fun scenarioActivity(block: (TestHostActivity) -> Unit) {
        scenario.onActivity(block)
    }

    private fun awaitCollected(reference: java.lang.ref.WeakReference<*>): Boolean =
        awaitUntil(timeoutMs = 10_000, intervalMs = 200) {
            Runtime.getRuntime().gc()
            // pressure makes a collection likelier than the request alone
            @Suppress("UNUSED_EXPRESSION")
            ByteArray(4 * 1024 * 1024)
            reference.get() == null
        }
}
