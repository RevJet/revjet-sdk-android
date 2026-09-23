package com.revjet.sdk.internal

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.revjet.sdk.NativeTagResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Shows a native ad: whatever view the application returns for the response. */
@SuppressLint("ViewConstructor")
internal class NativeTagUiView(
    context: Context,
    private val model: NativeTag,
    private val onResponse: (NativeTagResponse) -> View?,
) : FrameLayout(context),
    TagContentView {
    override val view: View get() = this

    private val progress =
        ProgressBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

    private var adContent: View? = null
    private var tracker: ExposureTracker? = null
    private var scope: CoroutineScope? = null

    /**
     * A tap, never a touch that turns into a scroll, and observed rather than consumed so the ad's
     * own controls keep working.
     */
    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    model.handleClick(event.x to event.y, width.toFloat() to height.toFloat())

                    return false
                }
            },
        )

    init {
        addView(progress)
        // Consumes the touch when the ad's own view does not, so the gesture is seen through to its end
        isClickable = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        return super.dispatchTouchEvent(event)
    }

    override fun onHostAttached() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { this.scope = it }

        scope.launch {
            model.isLoading.collect { isLoading ->
                progress.visibility = if (isLoading) VISIBLE else GONE
            }
        }
        scope.launch {
            model.response.filterNotNull().collect { response -> show(onResponse(response)) }
        }
        scope.launch {
            model.isLoaded.collect { if (it) startTracking() }
        }

        startTracking()
    }

    override fun onHostDetached() {
        tracker?.stop()
        tracker = null
        scope?.cancel()
        scope = null
    }

    private fun show(content: View?) {
        adContent?.let { removeView(it) }
        adContent = content ?: return

        addView(content, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        progress.visibility = GONE

        // The ad now has its own view, so that is what is measured
        tracker?.stop()
        tracker = null
        startTracking()
    }

    /**
     * Tracks the ad's own view when the application provided one, and this view otherwise — the ad
     * may be rendered elsewhere, as the Compose wrapper does.
     */
    private fun startTracking() {
        if (!isAttachedToWindow || !model.isLoaded.value) return
        if (tracker != null) return

        tracker = ExposureTracker(adContent ?: this) { model.updateExposure(it) }.also { it.start() }
    }
}
