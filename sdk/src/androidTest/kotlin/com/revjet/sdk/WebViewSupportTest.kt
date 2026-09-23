package com.revjet.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revjet.sdk.internal.TagEvents
import com.revjet.sdk.internal.WebBasedTag
import com.revjet.sdk.internal.WebBasedTagUiView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** What a web-based tag does on a device whose WebView cannot tell frames apart. */
@RunWith(AndroidJUnit4::class)
class WebViewSupportTest {
    @Test
    fun the_view_that_would_have_shown_the_ad_is_still_usable() {
        val model = unsupportedModel()

        // Building and mounting the ad's view must not fail either: the error is what the
        // application is told, not an exception from a constructor
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = WebBasedTagUiView(InstrumentationRegistry.getInstrumentation().targetContext, model)
            view.onHostAttached()
            view.onHostDetached()
        }

        model.destroy()
    }

    @Test
    fun a_web_view_without_the_features_reports_an_error_rather_than_failing_the_app() {
        val reported = CountDownLatch(1)

        val model =
            unsupportedModel(
                object : TagEvents {
                    override fun onError(error: Throwable) {
                        // stored before the test thread is released to read it
                        this@WebViewSupportTest.error = error
                        reported.countDown()
                    }
                },
            )

        InstrumentationRegistry.getInstrumentation().runOnMainSync { model.load() }

        assertTrue("the application should be told", reported.await(20, TimeUnit.SECONDS))
        assertTrue("it should say why: $error", error is RevJetError.WebViewUnsupported)
        assertFalse("and the tag should not be left loading", model.isLoading.value)

        model.destroy()
    }

    @Volatile
    private var error: Throwable? = null

    private fun unsupportedModel(events: TagEvents = object : TagEvents {}) =
        WebBasedTag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            events = events,
            tag = "tag390451",
            key = "7c2",
            plcId = "266512461",
            isScrollEnabled = false,
            updateDynamicHeight = false,
            debugMode = DebugMode.EMULATE,
            options = emptyList(),
            customParameters = emptyMap(),
            reloadsOnReachable = false,
            hasRequiredWebViewFeatures = { false },
        )
}
