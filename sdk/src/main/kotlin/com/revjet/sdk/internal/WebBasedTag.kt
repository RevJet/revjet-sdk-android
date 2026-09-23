package com.revjet.sdk.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revjet.sdk.Constants
import com.revjet.sdk.DebugMode
import com.revjet.sdk.Option
import com.revjet.sdk.RevJetError
import com.revjet.sdk.RevJetSDK
import com.revjet.sdk.domain
import com.revjet.sdk.hasCustomDomain
import com.revjet.sdk.jsLiteral
import com.revjet.sdk.logDebug
import com.revjet.sdk.queryItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A tag whose creative renders in a web view, driven through MRAID.
 */
internal class WebBasedTag(
    context: Context,
    events: TagEvents,
    private val tag: String,
    private val key: String,
    private val plcId: String?,
    private val isScrollEnabled: Boolean,
    private val updateDynamicHeight: Boolean,
    private val debugMode: DebugMode?,
    private val options: List<Option>,
    private val customParameters: Map<String, String>,
    reloadsOnReachable: Boolean,
    scope: CoroutineScope = mainScope(),
    private val hasRequiredWebViewFeatures: () -> Boolean = {
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    },
) : TagModel(context, events, reloadsOnReachable, scope) {
    /** Whether MRAID has told the creative it is ready. */
    var isReady: Boolean = false
        private set

    private var placementType = Mraid.PlacementType.INLINE
    private var viewState = Mraid.ViewState.DEFAULT

    private var lastExposure: ExposureData? = null
    private var lastNotifiedExposure: ExposureData? = null
    private var lastNotifiedPosition: Rect? = null

    private val bridge = WebTagBridge(this)

    /** The origin the ad document is given, and the only one the bridge accepts messages from. */
    val baseUrl: String get() = "${Constants.HTTPS}://${options.domain}"

    private var createdWebView: WebView? = null

    /**
     * The web view the ad renders in, created on first use so that a tag can be preloaded before
     * any view exists.
     */
    @get:SuppressLint("SetJavaScriptEnabled")
    val webView: WebView
        get() = createdWebView ?: createWebView().also { createdWebView = it }

    private fun createWebView(): WebView {
        // Without these the SDK cannot tell the ad document apart from content inside it
        if (!hasRequiredWebViewFeatures()) throw RevJetError.WebViewUnsupported()

        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            isVerticalScrollBarEnabled = isScrollEnabled
            isHorizontalScrollBarEnabled = false
            isScrollContainer = isScrollEnabled
            setBackgroundColor(0)

            webViewClient = bridge

            WebViewCompat.addWebMessageListener(this, BRIDGE_OBJECT, setOf(baseUrl), bridge)
            // Process-wide, so only ever turned on: turning it off would override the application's
            if (RevJetSDK.isDebugEnabled) WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun load() {
        scope.launch {
            state.isClosedByUser.value = false
            isReady = false
            lastExposure = null
            lastNotifiedExposure = null
            lastNotifiedPosition = null
            state.isLoaded.value = false
            state.isLoading.value = true

            val (advertisingId, isLimitAdTrackingEnabled) = AdvertisingId.read(context)
            val identity = Identity.of(context.packageName, advertisingId, isLimitAdTrackingEnabled)

            val view =
                try {
                    AdRequest.validate(tag, key, options)
                    webView
                } catch (error: RevJetError) {
                    // A request that cannot be made, or an ad that cannot be shown safely here, is
                    // the application's to report
                    state.isLoading.value = false
                    events.onError(error)

                    return@launch
                }

            WebViewCompat.addDocumentStartJavaScript(
                view,
                Mraid.script(context, identity),
                setOf(baseUrl),
            )

            events.onBeforeLoad()
            view.loadDataWithBaseURL("$baseUrl/", script(), "text/html", "utf-8", null)
        }
    }

    override fun destroy() {
        createdWebView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        createdWebView = null
        super.destroy()
    }

    /** The HTML the web view loads, which asks the JavaScript tag for a creative. */
    fun script(): String {
        val debug = debugMode?.let { "debug: ${JsLiteral.string(it.value)}," }.orEmpty()
        val customDomain =
            if (options.hasCustomDomain) {
                "custom_domain: ${JsLiteral.string(options.domain)},"
            } else {
                ""
            }

        return template()
            .replace("{{DOMAIN}}", options.domain)
            .replace("{{TAG}}", JsLiteral.string(tag))
            .replace("{{KEY}}", JsLiteral.string(key))
            .replace("{{DEBUG}}", debug)
            .replace("{{PLC_ID}}", JsLiteral.string(plcId.orEmpty()))
            .replace("{{CUSTOM_DOMAIN}}", customDomain)
            .replace("{{OPTIONS}}", formattedOptions())
            .replace("{{CUSTOM_PARAMETERS}}", formattedCustomParameters())
    }

    private fun template(): String = context.assets.open(TEMPLATE_ASSET).use { it.readBytes().decodeToString() }

    /**
     * Each entry carries its own trailing comma, so an empty set of options leaves no dangling one
     * behind in the generated object literal.
     */
    private fun formattedOptions(): String =
        options
            .filterNot { it is Option.CustomDomain }
            .flatMap { it.queryItems }
            .joinToString("\n            ") { (name, value) ->
                "${JsLiteral.string(name)}: ${value.jsLiteral},"
            }

    private fun formattedCustomParameters(): String =
        customParameters.entries.joinToString(",\n        ") { (name, value) ->
            "${JsLiteral.string(name)}: ${JsLiteral.string(value)}"
        }

    fun onDocumentLoaded() {
        state.isLoaded.value = true
        state.isLoading.value = false
        events.onLoad()
        logDebug { "onLoad called" }
    }

    fun onClosedByCreative() {
        state.isClosedByUser.value = true
        events.onClose()
        logDebug { "MRAID: close" }
    }

    fun onHeightReported(height: Float) {
        if (!updateDynamicHeight) return

        state.responsiveHeight.value = height
        logDebug { "Dynamic height: $height" }
    }

    fun onTrackingEventReported(event: Map<String, Any?>) {
        events.onTrackingEvent(event)
    }

    /** A message from the ad document that could not be acted on. */
    fun reportError(error: RevJetError) {
        events.onError(error)
    }

    /**
     * An ad registering late checks `getState()`, so the state has to leave `loading` before
     * `ready` is fired.
     */
    fun onMraidReady() {
        initializeView()
        notifyReadyEvent()
    }

    private fun fireChangeEvent(properties: String) {
        evaluate("window.mraid._fireChangeEvent({$properties});")
    }

    private fun notifyReadyEvent() {
        evaluate("window.mraid._notifyReadyEvent();") {
            isReady = true
            notifyExposureChange(lastExposure)
        }
    }

    private fun initializeView() {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels.toDp()
        val screenHeight = metrics.heightPixels.toDp()
        val current = position()

        // `calendar`, `storePicture`, `vpaid` and `location` are the features this container does
        // not implement, and an ad is expected to compensate for them
        fireChangeEvent(
            """
            placementType: ${JsLiteral.string(placementType.value)},
            state: ${JsLiteral.string(viewState.value)},
            sizeChange: { width: $screenWidth, height: $screenHeight },
            maxSize: { width: $screenWidth, height: $screenHeight },
            currentAppOrientation: $appOrientation,
            currentPosition: $current,
            defaultPosition: $current,
            supports: {
                sms: ${canOpen("sms:")},
                tel: ${canOpen("tel:")},
                calendar: false,
                storePicture: false,
                inlineVideo: true,
                vpaid: false,
                location: false
            }
            """.trimIndent(),
        )
        lastNotifiedPosition = containerPosition()
    }

    /** Reports the ad container's position, whenever it has changed. */
    fun updatePosition() {
        if (!isReady) return

        val position = containerPosition()
        if (position == lastNotifiedPosition) return

        lastNotifiedPosition = position

        val metrics = context.resources.displayMetrics
        val maxSize = maxSize()
        val reported = position(position)
        fireChangeEvent(
            """
            sizeChange: { width: ${metrics.widthPixels.toDp()}, height: ${metrics.heightPixels.toDp()} },
            maxSize: { width: ${maxSize.first}, height: ${maxSize.second} },
            currentAppOrientation: $appOrientation,
            currentPosition: $reported,
            defaultPosition: $reported
            """.trimIndent(),
        )
    }

    /**
     * Reports how much of the ad the user can see.
     *
     * The data is kept even before MRAID is ready, so that [notifyReadyEvent] can deliver it as
     * soon as that happens.
     */
    fun notifyExposureChange(exposure: ExposureData?) {
        val data = exposure ?: return

        lastExposure = data
        if (!isReady) return
        if (lastNotifiedExposure == data) return

        lastNotifiedExposure = data
        updatePosition()

        // MRAID 3.0 §7.5 passes `null`, not a missing value, for what is not visible or not measured
        val rectangle =
            data.visibleRectangle?.let {
                """{ "x": ${it.left}, "y": ${it.top}, "width": ${it.width()}, "height": ${it.height()} }"""
            } ?: "null"

        evaluate(
            "window.mraid._notifyExposureChangeEvent({ " +
                "\"exposedPercentage\": ${data.exposedPercentage}, " +
                "\"visibleRectangle\": $rectangle, " +
                "\"occlusionRectangles\": null });",
        )
    }

    private fun containerPosition(): Rect {
        val view = createdWebView ?: return Rect()
        val location = IntArray(2)
        view.getLocationInWindow(location)

        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    private fun maxSize(): Pair<Int, Int> {
        val view = createdWebView
        val root = view?.rootView

        return if (root != null && root.width > 0) {
            root.width.toDp() to root.height.toDp()
        } else {
            val metrics = context.resources.displayMetrics
            metrics.widthPixels.toDp() to metrics.heightPixels.toDp()
        }
    }

    private fun position(rect: Rect = containerPosition()): String =
        "{ x: ${rect.left.toDp()}, y: ${rect.top.toDp()}, " +
            "width: ${rect.width().toDp()}, height: ${rect.height().toDp()} }"

    /** `locked` reports that `forceOrientation` is not acted on. */
    private val appOrientation: String
        get() {
            val metrics = context.resources.displayMetrics
            val orientation = if (metrics.widthPixels > metrics.heightPixels) "landscape" else "portrait"

            return "{ orientation: ${JsLiteral.string(orientation)}, locked: true }"
        }

    private fun Int.toDp(): Int = (this / context.resources.displayMetrics.density).toInt()

    private fun canOpen(scheme: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme))

        return intent.resolveActivity(context.packageManager) != null ||
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    private fun evaluate(
        script: String,
        onDone: (() -> Unit)? = null,
    ) {
        scope.launch {
            createdWebView?.evaluateJavascript(script) { onDone?.invoke() }
        }
    }

    private companion object {
        const val BRIDGE_OBJECT = "RevJetBridge"
        const val TEMPLATE_ASSET = "revjet/tag.html"
    }
}
