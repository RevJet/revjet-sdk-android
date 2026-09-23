package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revjet.sdk.internal.TagEvents
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The application's handlers and the view's reach the same events, whichever is set first, so
 * neither the preload callbacks nor the view's listener are dropped.
 */
@RunWith(AndroidJUnit4::class)
class TagEventsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun tag() =
        Tag(
            context = context,
            type = TagType.NATIVE,
            tag = "tag355022",
            key = "b37",
            debugMode = DebugMode.EMULATE,
        )

    @Test
    fun both_the_application_and_the_view_are_told_about_a_load() {
        val tag = tag()
        val application = CountDownLatch(1)
        val view = CountDownLatch(1)

        tag.onLoad = { application.countDown() }
        tag.viewEvents = viewTold(view)

        tag.reload()

        assertTrue("the application is told", application.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue("the view is told", view.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        tag.destroy()
    }

    @Test
    fun a_view_registering_later_does_not_replace_the_preload_handlers() {
        val tag = tag()
        val preloaded = CountDownLatch(1)
        val view = CountDownLatch(1)

        tag.preload(onLoad = { preloaded.countDown() })
        // as a view does once it is created for an already preloading tag
        tag.viewEvents = viewTold(view)

        assertTrue("the preload handler survives", preloaded.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue("the view is told as well", view.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        tag.destroy()
    }

    @Test
    fun preloading_after_a_view_exists_keeps_the_view_informed() {
        val tag = tag()
        val preloaded = CountDownLatch(1)
        val view = CountDownLatch(1)

        tag.viewEvents = viewTold(view)
        tag.preload(onLoad = { preloaded.countDown() })

        assertTrue("the view is still told", view.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertTrue("and so is the preload handler", preloaded.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
        tag.destroy()
    }

    /** Stands in for a view registering with the tag. */
    private fun viewTold(latch: CountDownLatch) =
        object : TagEvents {
            override fun onLoad() {
                latch.countDown()
            }
        }

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}
