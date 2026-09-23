package com.revjet.sdk.compose

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.webkit.WebViewFeature
import com.revjet.sdk.DebugMode
import com.revjet.sdk.Option
import com.revjet.sdk.RevJetTagView
import com.revjet.sdk.Tag
import com.revjet.sdk.TagType
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The ad an application shows in Compose, against the live native test tag. */
@RunWith(AndroidJUnit4::class)
class RevJetTagTest {
    @get:Rule
    val compose = createComposeRule()

    private var tag: Tag? = null

    @After
    fun tearDown() {
        compose.runOnUiThread { tag?.destroy() }
    }

    @Test
    fun an_ad_the_application_draws_itself_still_becomes_viewable() {
        var isViewable = false
        val tag = nativeTag().also { this.tag = it }
        tag.onTrackingEvent = { event ->
            if (event["type"] == "ad_viewable") isViewable = true
        }

        compose.setContent {
            RevJetTag(
                tag = tag,
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(300.dp),
                nativeContent = {
                    Box(Modifier.fillMaxWidth().height(300.dp).background(Color.White))
                },
            )
        }

        // Nothing here hands the SDK a view, so exposure has to come from the host the ad is drawn over
        compose.waitUntil(timeoutMillis = 60_000) { isViewable }
    }

    @Test
    fun a_creative_is_given_the_space_the_ad_was_given() {
        assumeTrue(
            "this WebView cannot host an ad",
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT),
        )

        val tag = webTag().also { this.tag = it }

        compose.setContent {
            RevJetTag(tag = tag, onClick = {}, modifier = Modifier.fillMaxWidth().height(300.dp))
        }
        compose.waitUntil(timeoutMillis = 60_000) { tag.isLoaded.value }
        compose.waitForIdle()

        // A creative renders in the view itself, so a view of no height paints no ad
        var height = 0
        compose.runOnUiThread { height = adViewHeight() }

        assertTrue("the ad's view should have the height it was given, was $height", height > 0)
    }

    @Test
    fun the_screen_hears_about_the_first_load() {
        var beforeLoad = false
        var loaded = false
        val tag = nativeTag().also { this.tag = it }

        compose.setContent {
            RevJetTag(
                tag = tag,
                onClick = {},
                onBeforeLoad = { beforeLoad = true },
                onLoad = { loaded = true },
            )
        }

        // The view starts loading as it is created, before any effect of the composition has run
        compose.waitUntil(timeoutMillis = 30_000) { loaded }
        assertTrue("the load the view started should be announced to the screen", beforeLoad)
    }

    @Test
    fun a_screen_that_is_gone_is_not_called_back() {
        var screenClicked = false
        val applicationClicked = CountDownLatch(1)
        val tag = nativeTag().also { this.tag = it }
        tag.onClick = { applicationClicked.countDown() }

        var shown by mutableStateOf(true)
        compose.setContent {
            if (shown) {
                RevJetTag(tag = tag, onClick = { screenClicked = true })
            }
        }
        compose.waitUntil(timeoutMillis = 30_000) { tag.isLoaded.value }

        compose.runOnUiThread { shown = false }
        compose.waitForIdle()

        // The tag outlives the screen and still reports the click, to the application only
        compose.runOnUiThread { tag.goToLP() }

        assertTrue("the click should still reach the application", applicationClicked.await(30, TimeUnit.SECONDS))
        compose.waitForIdle()
        assertFalse("but not the screen that is gone", screenClicked)
    }

    private fun webTag() =
        Tag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            type = TagType.WEB_BASED,
            tag = "tag390451",
            key = "7c2",
            plcId = "266512461",
            debugMode = DebugMode.EMULATE,
            options = listOf(Option.Responsive(true)),
        )

    /** The height of the SDK's own view, wherever Compose put it. */
    private fun adViewHeight(): Int {
        val activity =
            ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .first()

        fun find(view: View): RevJetTagView? =
            when {
                view is RevJetTagView -> view
                view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
                else -> null
            }

        return find(activity.window.decorView)?.height ?: 0
    }

    private fun nativeTag() =
        Tag(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            type = TagType.NATIVE,
            tag = "tag355022",
            key = "b37",
            debugMode = DebugMode.EMULATE,
        )
}
