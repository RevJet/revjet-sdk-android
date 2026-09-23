package com.revjet.sdk.internal

import com.revjet.sdk.logDebug
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
     * Sends a GET and discards the response.
     *
     * Used for tracking pixels, where the SDK reports the attempt and never the outcome.
     */
    suspend fun fire(url: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = open(url)

                try {
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }.onFailure { logDebug { "Pixel request failed: $url (${it.message})" } }
        }
    }

    fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
}
