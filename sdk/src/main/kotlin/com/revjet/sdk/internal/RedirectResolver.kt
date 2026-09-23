package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import com.revjet.sdk.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Follows a click URL to the destination the ad server resolves it to.
 *
 * The redirects are followed here rather than by the platform, so that every hop can be checked:
 * the chain is chosen by whoever answers the request. A destination outside the web — an app
 * store, a deep link, a phone number — ends the chain, and is handed over without being requested.
 */
internal object RedirectResolver {
    private const val MAX_HOPS = 10

    private val webSchemes = setOf("http", "https")

    /**
     * Schemes that are never where a click leads. `intent:` among them: `ACTION_VIEW` cannot open
     * it, and parsing it into an `Intent` is how an app lets an ad start its private components.
     */
    private val refusedSchemes = setOf("javascript", "file", "content", "data", "blob", "about", "intent")

    /**
     * @return the URL the chain ends at.
     * @throws RevJetError.BlockedDestination when the URL, or a hop of its chain, is not publicly
     *   routable, or is not a destination at all.
     * @throws RevJetError.NoRedirectURL when the chain cannot be followed.
     */
    suspend fun resolve(url: String): String {
        if (!isWeb(url)) return outsideTheWeb(url)
        if (!RequestDestination.isAllowed(url)) throw RevJetError.BlockedDestination(url)

        return withContext(Dispatchers.IO) {
            var current = url

            repeat(MAX_HOPS) {
                val location =
                    runCatching { locationOf(current) }
                        .getOrElse { throw RevJetError.NoRedirectURL(it) }
                        ?: return@withContext current

                val next = next(current, location)
                if (!isWeb(next)) return@withContext outsideTheWeb(next)

                if (!RequestDestination.isAllowed(next)) {
                    logDebug { "Refusing to follow a redirect to $next" }
                    throw RevJetError.BlockedDestination(next)
                }

                current = next
            }

            current
        }
    }

    /**
     * Where a redirect from [current] to [location] leads: `location` itself when it is absolute,
     * whatever its scheme, and resolved against `current` otherwise.
     *
     * @throws RevJetError.NoRedirectURL when `location` is not a URL.
     */
    fun next(
        current: String,
        location: String,
    ): String =
        runCatching { URI(current).resolve(location.trim()).toString() }
            .getOrElse { throw RevJetError.NoRedirectURL(it) }

    private fun isWeb(url: String): Boolean = scheme(url) in webSchemes

    /** A destination the SDK does not request: handed over, unless it is not a destination at all. */
    private fun outsideTheWeb(url: String): String {
        val scheme = scheme(url)
        if (scheme == null || scheme in refusedSchemes) {
            logDebug { "Refusing a click to $url" }
            throw RevJetError.BlockedDestination(url)
        }

        return url
    }

    private fun scheme(url: String): String? = runCatching { URI(url.trim()).scheme?.lowercase() }.getOrNull()

    /** The `Location` of a redirect, or `null` when the response is the destination. */
    private fun locationOf(url: String): String? {
        val connection = Http.open(url).apply { instanceFollowRedirects = false }

        return try {
            val code = connection.responseCode
            if (code in 300..399) connection.getHeaderField("Location") else null
        } finally {
            connection.disconnect()
        }
    }
}
