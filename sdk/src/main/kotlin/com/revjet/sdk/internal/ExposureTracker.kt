package com.revjet.sdk.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.lang.ref.WeakReference

/**
 * Watches how much of a view the user can see, and reports it when it changes.
 *
 * Three things drive a check: the view tree redrawing or scrolling, a poll for changes nothing
 * reports — a view can stop being visible because something above it moved — and the ad's
 * activity leaving the foreground, which makes the ad invisible however the view tree looks.
 *
 * MRAID 3.0 §4.1.1 asks for a change to reach the creative within 200 ms, and for a poll at least
 * five times a second; §7.5 counts an activity as in the foreground from `onResume` to `onPause`.
 * A change can wait a whole period for its check, then travel to the creative, so the period is
 * half the allowance rather than all of it.
 */
internal class ExposureTracker(
    view: View,
    private val periodMs: Long = PERIOD_MS,
    private val onChange: (ExposureData) -> Unit,
) {
    private val trackedView = WeakReference(view)
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Whose lifecycle decides the foreground; the process's where the view has no activity.
     *
     * Found through the view's parents once it is attached: a web view is built on the application
     * context, so that a tag can be preloaded before there is a screen for it.
     */
    private var host: WeakReference<Activity>? = null

    private var isTracking = false
    private var isForeground = true
    private var lastCheckMs = 0L

    private val preDrawListener =
        ViewTreeObserver.OnPreDrawListener {
            checkIfDue()
            true
        }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { checkIfDue() }

    private val activityCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity === host?.get()) enterForeground()
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity === host?.get()) leaveForeground()
            }

            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        }

    private val processObserver =
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = enterForeground()

            override fun onStop(owner: LifecycleOwner) = leaveForeground()
        }

    private val poll =
        object : Runnable {
            override fun run() {
                check()
                handler.postDelayed(this, periodMs)
            }
        }

    /** Starts watching. Repeated calls are ignored, so the observers are installed once. */
    fun start() {
        if (isTracking) return

        val view = trackedView.get() ?: return
        isTracking = true

        view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
        view.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        val activity = view.hostActivity()
        host = activity?.let(::WeakReference)
        if (activity != null) {
            activity.application.registerActivityLifecycleCallbacks(activityCallbacks)
        } else {
            ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        }

        startPolling()
        check()
    }

    fun stop() {
        if (!isTracking) return
        isTracking = false

        trackedView.get()?.viewTreeObserver?.let { observer ->
            if (observer.isAlive) {
                observer.removeOnPreDrawListener(preDrawListener)
                observer.removeOnScrollChangedListener(scrollListener)
            }
        }
        val activity = host?.get()
        if (activity != null) {
            activity.application.unregisterActivityLifecycleCallbacks(activityCallbacks)
        } else {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        }
        stopPolling()
    }

    private fun enterForeground() {
        isForeground = true
        startPolling()
        check()
    }

    private fun leaveForeground() {
        isForeground = false
        stopPolling()

        // Whatever the view tree says, an activity that is not in the foreground shows nothing
        onChange(ExposureData.ZERO)
    }

    private fun startPolling() {
        stopPolling()
        handler.postDelayed(poll, periodMs)
    }

    private fun stopPolling() = handler.removeCallbacks(poll)

    /** A pre-draw fires per frame, so checks are spaced out by [periodMs]; the poll catches the rest. */
    private fun checkIfDue() {
        val now = SystemClock.uptimeMillis()
        if (now - lastCheckMs < periodMs) return

        check()
    }

    private fun check() {
        if (!isTracking || !isForeground) return

        lastCheckMs = SystemClock.uptimeMillis()
        val view = trackedView.get() ?: return

        onChange(ViewExposure.calculate(view))
    }

    private companion object {
        const val PERIOD_MS = 100L
    }
}

/** The activity showing the view, from the nearest of its parents whose context is one. */
private fun View.hostActivity(): Activity? =
    generateSequence(this) { it.parent as? View }.firstNotNullOfOrNull { it.context.findActivity() }

/** The activity a context belongs to, through any wrappers around it. */
private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.firstNotNullOfOrNull { it as? Activity }
