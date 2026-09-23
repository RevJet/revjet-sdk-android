package com.revjet.sdk

import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.revjet.sdk.internal.WebBasedTag
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Mounts a web-based tag in an activity and drives it, the way the view layer will. */
internal class WebTagHost(
    val tag: Tag,
    private val timeoutMs: Long = 30_000,
) {
    private val scenario: ActivityScenario<TestHostActivity> =
        ActivityScenario.launch(TestHostActivity::class.java)

    internal val model: WebBasedTag get() = tag.model as WebBasedTag

    /**
     * @param documentStartScript runs before the SDK's own script, so it polls for `window.mraid`
     *   rather than assuming it is there.
     */
    fun mountAndLoad(documentStartScript: String? = null) {
        scenario.onActivity { activity ->
            val view = model.webView

            if (documentStartScript != null) {
                androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                    view,
                    documentStartScript,
                    setOf(model.baseUrl),
                )
            }

            activity.container.addView(
                view,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            tag.reload()
        }
    }

    /** Waits for MRAID to have been initialized in the document. */
    fun awaitReady(): Boolean = awaitUntil(timeoutMs) { model.isReady }

    fun awaitLoaded(): Boolean = awaitUntil(timeoutMs) { tag.isLoaded.value }

    /** Evaluates the script in the ad document and returns its result as a string. */
    fun evaluate(script: String): String {
        val latch = CountDownLatch(1)
        var result = ""

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView().evaluateJavascript("String($script)") { value ->
                // The result comes back JSON encoded, so inner quotes arrive escaped
                result =
                    runCatching { org.json.JSONTokener(value).nextValue() as String }
                        .getOrElse { value.trim('"') }
                latch.countDown()
            }
        }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)

        return result
    }

    /** Evaluates until the script returns [expected], or the timeout elapses. */
    fun evaluateUntil(
        script: String,
        expected: String,
    ): Boolean = awaitUntil(timeoutMs, intervalMs = 200) { evaluate(script) == expected }

    private fun webView(): WebView = model.webView

    companion object {
        /** Kotlin's own list rendering is not a JavaScript array literal. */
        fun jsArray(values: List<String>): String = values.joinToString(", ", "[", "]") { "'" + it + "'" }
    }

    fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { tag.destroy() }
        scenario.close()
    }
}
