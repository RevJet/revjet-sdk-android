package com.revjet.sdk

import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revjet.sdk.internal.ExposureData
import com.revjet.sdk.internal.ExposureTracker
import com.revjet.sdk.internal.RectSubtraction
import com.revjet.sdk.internal.ViewExposure
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** How much of a view the SDK reports as visible, measured against a real window. */
@RunWith(AndroidJUnit4::class)
class ExposureTest {
    private lateinit var scenario: ActivityScenario<TestHostActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
    }

    @After
    fun tearDown() {
        RevJetSDK.clearFriendlyObstructions()
        scenario.close()
    }

    @Test
    fun a_view_on_screen_is_wholly_exposed() {
        val ad = addAd()

        assertEquals(100f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun a_view_whose_parent_is_faded_out_is_not_exposed() {
        val ad = addAd()
        onMainAndLaidOut { (ad.parent as View).alpha = 0f }

        assertEquals(0f, exposure(ad).exposedPercentage, 0f)
    }

    @Test
    fun the_visible_part_is_measured_from_the_view_itself() {
        val ad = addAd()
        cover(Part.TOP_HALF)

        val visible = exposure(ad).visibleRectangle!!

        // the ad sits AD_TOP pixels down the window, which the rectangle must not carry
        assertEquals(0f, visible.left, 1f)
        assertEquals(AD_HEIGHT / 2 / density(), visible.top, 1f)
    }

    @Test
    fun a_view_half_covered_is_half_exposed() {
        val ad = addAd()
        cover(Part.BOTTOM_HALF)

        val exposure = exposure(ad)

        assertEquals(50f, exposure.exposedPercentage, 1f)
        assertEquals(
            "the reported rectangle covers the visible half only",
            (AD_HEIGHT / 2).toFloat(),
            exposure.visibleRectangle!!.height() * density(),
            2f,
        )
    }

    @Test
    fun the_same_covered_area_only_counts_once() {
        val ad = addAd()
        cover(Part.BOTTOM_HALF)
        cover(Part.BOTTOM_HALF)

        assertEquals(50f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun a_wholly_covered_view_is_not_exposed() {
        val ad = addAd()
        cover(Part.ALL)

        val exposure = exposure(ad)

        assertEquals(0f, exposure.exposedPercentage, 0.01f)
        assertNull(exposure.visibleRectangle)
    }

    @Test
    fun a_cover_beside_an_ancestor_counts_too() {
        lateinit var ad: View
        withActivity { activity ->
            val container = FrameLayout(activity)
            activity.container.addView(container, params(top = AD_TOP, height = AD_HEIGHT))

            ad = View(activity).apply { setBackgroundColor(Color.BLUE) }
            container.addView(ad, params(top = 0, height = AD_HEIGHT))
        }
        // beside the container rather than beside the ad
        cover(Part.BOTTOM_HALF)

        assertEquals(50f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun a_view_that_paints_nothing_is_not_an_obstruction() {
        val ad = addAd()
        withActivity { activity ->
            // a grouping view over the ad, with nothing drawn in it
            activity.container.addView(FrameLayout(activity), params(top = AD_TOP, height = AD_HEIGHT))
        }

        assertEquals(100f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun a_label_counts_even_though_it_takes_no_touches() {
        val ad = addAd()
        lateinit var label: TextView
        withActivity { activity ->
            label = TextView(activity).apply { text = "Over the ad" }
            activity.container.addView(label, params(top = AD_TOP + AD_HEIGHT / 2, height = AD_HEIGHT / 2))
        }

        assertFalse("a label does not take touches, but it does cover", label.isClickable)
        assertEquals(50f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun a_declared_obstruction_is_not_counted() {
        val ad = addAd()
        val cover = cover(Part.BOTTOM_HALF)

        RevJetSDK.registerFriendlyObstruction(cover)
        assertEquals(100f, exposure(ad).exposedPercentage, 1f)

        RevJetSDK.unregisterFriendlyObstruction(cover)
        assertEquals(50f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun what_is_inside_a_declared_obstruction_is_not_counted_either() {
        val ad = addAd()
        lateinit var chrome: View
        withActivity { activity ->
            val group = FrameLayout(activity)
            group.addView(
                View(activity).apply { setBackgroundColor(Color.BLACK) },
                params(top = 0, height = AD_HEIGHT),
            )
            activity.container.addView(group, params(top = AD_TOP, height = AD_HEIGHT))
            chrome = group
        }

        RevJetSDK.registerFriendlyObstruction(chrome)

        assertEquals(100f, exposure(ad).exposedPercentage, 1f)
    }

    @Test
    fun the_tracker_reports_a_view_it_can_see() {
        val ad = addAd()
        val seen = CountDownLatch(1)
        var last = ExposureData.ZERO

        val tracker =
            ExposureTracker(ad) {
                last = it
                if (it.exposedPercentage > 0f) seen.countDown()
            }
        onMainAndLaidOut { tracker.start() }

        assertTrue(seen.await(5, TimeUnit.SECONDS))
        assertEquals(100f, last.exposedPercentage, 1f)
        onMainAndLaidOut { tracker.stop() }
    }

    @Test
    fun the_tracker_notices_a_change_nothing_reports() {
        val ad = addAd()
        val hidden = CountDownLatch(1)
        var isHidden = false

        val tracker =
            ExposureTracker(ad) {
                if (isHidden && it.exposedPercentage == 0f) hidden.countDown()
            }
        onMainAndLaidOut { tracker.start() }
        Thread.sleep(500)

        // the ad stops being visible without its own bounds changing
        onMainAndLaidOut {
            isHidden = true
            ad.visibility = View.INVISIBLE
        }

        assertTrue("the poll should notice", hidden.await(5, TimeUnit.SECONDS))
        onMainAndLaidOut { tracker.stop() }
    }

    @Test
    fun subtracting_keeps_the_largest_remaining_rectangle() {
        val rect = Rect(0, 0, 100, 100)

        assertEquals(
            "a cut across the full width leaves the slab above it",
            Rect(0, 0, 100, 50),
            RectSubtraction.subtract(rect, Rect(0, 50, 100, 100)),
        )
        assertEquals(
            "a cut across the full height leaves the slab beside it",
            Rect(30, 0, 100, 100),
            RectSubtraction.subtract(rect, Rect(0, 0, 30, 100)),
        )
        assertEquals(
            "a rectangle that does not overlap removes nothing",
            rect,
            RectSubtraction.subtract(rect, Rect(200, 200, 210, 210)),
        )
        assertNull(
            "a rectangle that covers everything leaves nothing",
            RectSubtraction.subtract(rect, Rect(-10, -10, 200, 200)),
        )
    }

    private enum class Part { ALL, TOP_HALF, BOTTOM_HALF }

    private fun addAd(): View {
        lateinit var ad: View
        withActivity { activity ->
            ad = View(activity).apply { setBackgroundColor(Color.BLUE) }
            activity.container.addView(ad, params(top = AD_TOP, height = AD_HEIGHT))
        }

        return ad
    }

    /** Adds a view painting over part of the ad, as a sibling drawn after it. */
    private fun cover(part: Part): View {
        lateinit var cover: View
        withActivity { activity ->
            cover = View(activity).apply { setBackgroundColor(Color.BLACK) }
            val height = if (part == Part.ALL) AD_HEIGHT else AD_HEIGHT / 2
            val top = if (part == Part.BOTTOM_HALF) AD_TOP + AD_HEIGHT / 2 else AD_TOP

            activity.container.addView(cover, params(top = top, height = height))
        }

        return cover
    }

    private fun params(
        top: Int,
        height: Int,
    ) = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { topMargin = top }

    private fun exposure(view: View): ExposureData {
        var result = ExposureData.ZERO
        onMainAndLaidOut { result = ViewExposure.calculate(view) }

        return result
    }

    private fun density(): Float =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext.resources.displayMetrics.density

    private fun withActivity(block: (TestHostActivity) -> Unit) {
        scenario.onActivity { block(it) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** Runs [block] on the main thread, then waits for the layout it causes. */
    private fun onMainAndLaidOut(block: () -> Unit) {
        onMain(block)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private companion object {
        const val AD_HEIGHT = 400

        /** Well clear of the status bar, so only what a test adds can cover the ad. */
        const val AD_TOP = 400
    }
}
