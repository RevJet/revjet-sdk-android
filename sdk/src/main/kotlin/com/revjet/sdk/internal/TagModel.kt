package com.revjet.sdk.internal

import android.content.Context
import android.net.Uri
import com.revjet.sdk.logDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What a [com.revjet.sdk.Tag] drives, whichever kind of ad it serves.
 *
 * Confined to the main thread: the state, the events and the timers are all read and written there.
 */
internal sealed class TagModel(
    protected val context: Context,
    protected val events: TagEvents,
    reloadsOnReachable: Boolean,
    protected val scope: CoroutineScope,
) {
    /** What the subclasses write; everything else reads the flows below. */
    protected class State {
        val isLoaded = MutableStateFlow(false)
        val isLoading = MutableStateFlow(false)
        val isClosedByUser = MutableStateFlow(false)
        val responsiveHeight = MutableStateFlow<Float?>(null)
    }

    protected val state = State()

    val isLoaded: StateFlow<Boolean> = state.isLoaded.asStateFlow()
    val isLoading: StateFlow<Boolean> = state.isLoading.asStateFlow()
    val isClosedByUser: StateFlow<Boolean> = state.isClosedByUser.asStateFlow()

    /** The height the creative asked for, for a web-based tag that reports one. */
    val responsiveHeight: StateFlow<Float?> = state.responsiveHeight.asStateFlow()

    private val reachability =
        if (reloadsOnReachable) Reachability(context, ::onNetworkRestored).also { it.start() } else null

    /** Requests an ad, replacing the one the tag carries. The handlers stay in place. */
    abstract fun load()

    /** Requests the ad again once the network is back, unless one arrived or is on its way. */
    fun onNetworkRestored() {
        if (isLoaded.value || isLoading.value) return

        logDebug { "The network is back, loading again" }
        load()
    }

    /** Releases the timers, requests and views the model holds. */
    open fun destroy() {
        reachability?.stop()
        scope.cancel()
    }

    /** Resolves a click to where the ad server sends it, and reports that or why it could not be. */
    fun openClick(url: String) {
        scope.launch {
            try {
                events.onClick(Uri.parse(RedirectResolver.resolve(url)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logDebug { "Click was not opened: ${error.message}" }
                events.onError(error)
            }
        }
    }

    companion object {
        fun mainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
