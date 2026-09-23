package com.revjet.sdk.internal

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import com.revjet.sdk.logDebug

/**
 * Tells a tag when the network comes back, so an ad that could not load is requested again.
 *
 * The state at registration is read up front rather than taken from the first callback: Android
 * sends none while there is no network, so the first one may be the very change being waited for.
 */
internal class Reachability(
    context: Context,
    private val onRestored: () -> Unit,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val transitions = NetworkTransitions(isAvailable = isConnected())
    private var isRegistered = false

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = report(isAvailable = true)

            override fun onLost(network: Network) = report(isAvailable = false)
        }

    fun start() {
        try {
            connectivity?.registerDefaultNetworkCallback(callback)
            isRegistered = true
        } catch (error: SecurityException) {
            logDebug { "Not watching the network, ACCESS_NETWORK_STATE was removed: ${error.message}" }
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        if (!isRegistered) return

        connectivity?.unregisterNetworkCallback(callback)
        isRegistered = false
    }

    /** Callbacks arrive on a binder thread, and the tag is confined to the main one. */
    private fun report(isAvailable: Boolean) {
        handler.post {
            if (transitions.update(isAvailable)) onRestored()
        }
    }

    private fun isConnected(): Boolean =
        try {
            connectivity?.activeNetwork != null
        } catch (error: SecurityException) {
            false
        }
}

/** Whether a change of connectivity brought the network back. */
internal class NetworkTransitions(
    private var isAvailable: Boolean,
) {
    /** @return `true` when the network was down and is now available. */
    fun update(isAvailable: Boolean): Boolean {
        val isRestored = isAvailable && !this.isAvailable
        this.isAvailable = isAvailable

        return isRestored
    }
}
