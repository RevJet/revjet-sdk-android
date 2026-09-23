package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** What a tag created with `reloadsOnReachable` does when the network comes back. */
@RunWith(AndroidJUnit4::class)
class ReachabilityTest {
    private val tag =
        Tag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            type = TagType.NATIVE,
            tag = "tag355022",
            key = "b37",
            reloadsOnReachable = true,
            debugMode = DebugMode.EMULATE,
        )

    @After
    fun tearDown() {
        onMain { tag.destroy() }
    }

    @Test
    fun an_ad_that_never_arrived_is_requested_once_the_network_is_back() {
        val loaded = CountDownLatch(1)
        tag.onLoad = { loaded.countDown() }

        onMain { tag.model.onNetworkRestored() }

        assertTrue("the ad should be requested again", loaded.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun an_ad_already_shown_is_not_requested_again() {
        val loaded = CountDownLatch(1)
        var requests = 0
        tag.onBeforeLoad = { requests += 1 }
        tag.onLoad = { loaded.countDown() }

        onMain { tag.reload() }
        assertTrue(loaded.await(TIMEOUT_S, TimeUnit.SECONDS))

        onMain { tag.model.onNetworkRestored() }
        Thread.sleep(2_000)

        assertEquals("the network coming back should leave a loaded ad alone", 1, requests)
    }

    private companion object {
        const val TIMEOUT_S = 20L
    }
}
