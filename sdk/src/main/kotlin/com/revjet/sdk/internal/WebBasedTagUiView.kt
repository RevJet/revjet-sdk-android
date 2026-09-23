package com.revjet.sdk.internal

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.revjet.sdk.RevJetError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Shows a web-based ad: the web view the tag owns. */
@SuppressLint("ViewConstructor")
internal class WebBasedTagUiView(
    context: Context,
    private val model: WebBasedTag,
) : FrameLayout(context),
    TagContentView {
    override val view: View get() = this

    private val progress =
        ProgressBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

    private var tracker: ExposureTracker? = null
    private var scope: CoroutineScope? = null

    /** Null where the WebView cannot host an ad safely; the tag reports why through `onError`. */
    private val webView: WebView? =
        try {
            model.webView
        } catch (_: RevJetError.WebViewUnsupported) {
            null
        }

    init {
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(progress)
    }

    override fun onHostAttached() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { this.scope = it }

        scope.launch {
            model.isLoading.collect { isLoading ->
                progress.visibility = if (isLoading) VISIBLE else GONE
            }
        }

        webView?.let { view ->
            tracker = ExposureTracker(view) { model.notifyExposureChange(it) }.also { it.start() }
        }
    }

    override fun onHostDetached() {
        tracker?.stop()
        tracker = null
        scope?.cancel()
        scope = null
    }
}
