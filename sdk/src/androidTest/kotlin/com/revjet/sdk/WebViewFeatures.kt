package com.revjet.sdk

import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.junit.Assume.assumeTrue

/**
 * A web-based ad needs a WebView that can tell the ad document apart from content inside it.
 *
 * Where it cannot, the SDK refuses to show the ad at all, so there is nothing for these tests to
 * assert: they are skipped, naming what the device is missing.
 */
internal object WebViewFeatures {
    fun assumeAdsCanBeHosted() {
        assumeTrue(
            "${describe()} has no WEB_MESSAGE_LISTENER",
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER),
        )
        assumeTrue(
            "${describe()} has no DOCUMENT_START_SCRIPT",
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT),
        )
    }

    private fun describe(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val webView = WebViewCompat.getCurrentWebViewPackage(context)

        return "WebView ${webView?.versionName ?: "unknown"}"
    }
}
