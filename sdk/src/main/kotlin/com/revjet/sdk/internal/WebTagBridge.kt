package com.revjet.sdk.internal

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.revjet.sdk.RevJetError
import com.revjet.sdk.logDebug
import org.json.JSONObject

/** The events the ad document reports to the SDK. */
internal enum class BridgeEvent(
    val eventName: String,
) {
    ON_LOAD("onLoad"),
    ON_TRACKING_EVENT("onTrackingEvent"),
    ON_CLICK("onClick"),
    CLOSE("close"),
    LOG_DEBUG("logDebug"),
    ON_RESIZE("onResize"),
    ON_MRAID_INIT("onMRAIDInit"),
    ;

    companion object {
        fun from(name: String): BridgeEvent? = entries.firstOrNull { it.eventName == name }
    }
}

/**
 * Carries what the ad document reports, and decides what it is allowed to do.
 *
 * Both halves are security boundaries: a message is accepted from the ad document's main frame
 * only, and navigation away from that document is refused.
 */
internal class WebTagBridge(
    private val tag: WebBasedTag,
) : WebViewClient(),
    WebViewCompat.WebMessageListener {
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy,
    ) {
        if (!isFromAdDocument(sourceOrigin, isMainFrame)) {
            logDebug { "Ignoring a message from a frame that is not the ad document: $sourceOrigin" }
            return
        }

        val envelope = runCatching { JSONObject(message.data.orEmpty()) }.getOrNull() ?: return
        val event = BridgeEvent.from(envelope.optString("name"))

        if (event == null) {
            logDebug { "Unknown message from the ad document: ${envelope.optString("name")}" }
            return
        }

        handle(event, envelope.opt("body"))
    }

    /**
     * A subframe inherits the ad document's origin, so the frame itself is what separates the
     * SDK's own script from creative content running in an iframe.
     */
    private fun isFromAdDocument(
        sourceOrigin: Uri,
        isMainFrame: Boolean,
    ): Boolean {
        if (!isMainFrame) return false

        return isAdDocument(sourceOrigin)
    }

    private fun handle(
        event: BridgeEvent,
        body: Any?,
    ) {
        when (event) {
            BridgeEvent.ON_LOAD -> tag.onDocumentLoaded()

            BridgeEvent.ON_TRACKING_EVENT -> {
                val properties = body as? JSONObject
                if (properties == null) {
                    logDebug { "Tracking event without details: $body" }
                    tag.reportError(RevJetError.TrackingEventDetailsUnavailable())
                    return
                }

                tag.onTrackingEventReported(properties.toMap())
                logDebug { "Tracking event: $properties" }
            }

            BridgeEvent.ON_CLICK -> {
                val url = body as? String
                if (url.isNullOrEmpty()) {
                    logDebug { "Invalid URL in onClick: $body" }
                    tag.reportError(RevJetError.NoClickURL())
                    return
                }

                tag.openClick(url)
            }

            BridgeEvent.CLOSE -> tag.onClosedByCreative()

            BridgeEvent.LOG_DEBUG -> logDebug { "MRAID: $body" }

            BridgeEvent.ON_RESIZE -> {
                val height = (body as? Number)?.toFloat() ?: return
                tag.onHeightReported(height)
            }

            BridgeEvent.ON_MRAID_INIT -> tag.onMraidReady()
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url?.toString().orEmpty()

        // A tap on a link is the click channel, and Android reports the gesture
        if (request.hasGesture() && request.isForMainFrame) {
            tag.openClick(url)
            return true
        }

        // Creative content renders in subframes, only the ad document itself is restricted
        if (!request.isForMainFrame) return false

        if (!isAdDocument(request.url)) {
            logDebug { "Refusing to navigate the ad away to $url" }
            return true
        }

        return false
    }

    /** Whether the URL is the document the SDK loaded, rather than somewhere a creative points to. */
    private fun isAdDocument(url: Uri?): Boolean {
        val expected = Uri.parse(tag.baseUrl)

        return url?.scheme == expected.scheme && url?.host == expected.host
    }

    private fun JSONObject.toMap(): Map<String, Any?> =
        keys().asSequence().associateWith { key -> if (isNull(key)) null else get(key) }
}
