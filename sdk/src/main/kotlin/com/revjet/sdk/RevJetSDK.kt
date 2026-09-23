package com.revjet.sdk

import android.view.View
import androidx.annotation.MainThread
import java.util.Collections
import java.util.WeakHashMap

/** SDK-wide configuration. */
public object RevJetSDK {
    /** The SDK version. */
    public val version: String = "2.1.0"

    /** The name the SDK reports to the ad server, which tells it apart from the iOS SDK. */
    internal val name: String = "RevJetSDK-Android"

    @Volatile
    internal var isDebugEnabled: Boolean = false
        private set

    /** Read as each request is built, so it has to be set before any tag loads. */
    @Volatile
    internal var isCOPPACompliant: Boolean = false
        private set

    /** Views that overlay ads on purpose, held weakly. */
    private val friendlyObstructions: MutableSet<View> =
        Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    /**
     * Declares a view that is expected to overlay ads, so that it is not counted against the
     * viewability of an ad underneath it.
     *
     * Use it for app chrome that cannot be avoided, such as a floating action button or a player's
     * controls. Views inside a registered view are covered as well.
     *
     * @param view is held weakly, so a view that goes away needs no clean-up.
     */
    @MainThread
    public fun registerFriendlyObstruction(view: View) {
        friendlyObstructions.add(view)
        logDebug { "Registered friendly obstruction: ${view.javaClass.simpleName}" }
    }

    /** Stops treating a view as a friendly obstruction. */
    @MainThread
    public fun unregisterFriendlyObstruction(view: View) {
        friendlyObstructions.remove(view)
    }

    /** Stops treating any view as a friendly obstruction. */
    @MainThread
    public fun clearFriendlyObstructions() {
        friendlyObstructions.clear()
    }

    /** Whether the view was declared as overlaying ads on purpose. */
    internal fun isFriendlyObstruction(view: View): Boolean = friendlyObstructions.contains(view)

    /**
     * Logs what the SDK does to Logcat, under the tag `RevJetSDK`.
     *
     * It also makes every WebView in the application debuggable through `chrome://inspect` —
     * WebView debugging is process-wide — so leave it off in release builds.
     */
    public fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    /**
     * Configures compliance with the Children's Online Privacy Protection Act.
     *
     * When enabled, the SDK reports no advertising identifier and limited ad tracking, for native
     * and web-based tags alike, and tells the creative that the app is child-directed. Requests are
     * built as a tag loads, so set it before any does.
     *
     * @param enabled `true` for a child-directed app.
     */
    public fun setCOPPACompliance(enabled: Boolean) {
        isCOPPACompliant = enabled
        logDebug {
            if (enabled) {
                "COPPA compliance enabled - restricted data collection activated"
            } else {
                "COPPA compliance disabled"
            }
        }
    }
}
