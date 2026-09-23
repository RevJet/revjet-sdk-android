package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A tag the application configured wrongly is reported, whichever kind it is. */
@RunWith(AndroidJUnit4::class)
class ConfigurationTest {
    @Test
    fun a_native_tag_without_a_tag_name_is_reported() {
        assertReportedAsInvalid(TagType.NATIVE)
    }

    @Test
    fun a_web_based_tag_without_a_tag_name_is_reported() {
        assertReportedAsInvalid(TagType.WEB_BASED)
    }

    private fun assertReportedAsInvalid(type: TagType) {
        val tag = Tag(InstrumentationRegistry.getInstrumentation().targetContext, type, tag = "", key = "b37")
        val reported = CountDownLatch(1)
        var error: Throwable? = null
        tag.onError = {
            error = it
            reported.countDown()
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync { tag.reload() }

        assertTrue("the application should be told", reported.await(10, TimeUnit.SECONDS))
        assertTrue("that the configuration is wrong: $error", error is RevJetError.InvalidConfiguration)
        assertFalse("and the tag should not be left loading", tag.isLoading.value)

        InstrumentationRegistry.getInstrumentation().runOnMainSync { tag.destroy() }
    }
}
