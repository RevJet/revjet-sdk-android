package com.revjet.sdk

import android.util.Log

private const val TAG = "RevJetSDK"

/**
 * Logs a debug message when debug output is enabled.
 *
 * The message is a lambda so that it is not built when logging is off.
 */
internal inline fun logDebug(message: () -> String) {
    if (RevJetSDK.isDebugEnabled) {
        Log.d(TAG, message())
    }
}
