package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import com.revjet.sdk.logDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal object Http {
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    /** Reads the body of a GET, on [Dispatchers.IO]. */
    suspend fun get(url: String): String =
        withContext(Dispatchers.IO) {
            val connection = open(url)

            try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    /**
     * Sends a GET and discards the response, for a tracking pixel.
     *
     * A pixel's URL, and wherever it redirects, are chosen by the ad's campaign, so they are held to
     * the same destinations as a click. The SDK reports the attempt and never the outcome.
     *
     * @return whether the pixel was sent, rather than refused.
     */
    suspend fun fire(url: String): Boolean =
        try {
            RedirectResolver.resolve(url)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: RevJetError.BlockedDestination) {
            logDebug { "Refusing a pixel request to ${error.url}" }
            false
        } catch (error: Exception) {
            logDebug { "Pixel request failed: $url (${error.message})" }
            true
        }

    fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
}
