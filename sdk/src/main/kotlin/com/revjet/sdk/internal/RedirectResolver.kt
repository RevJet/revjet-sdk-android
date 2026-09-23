package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import com.revjet.sdk.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Follows a click URL to the destination the ad server resolves it to.
 *
 * The redirects are followed here rather than by the platform, so that every hop can be checked:
 * the chain is chosen by whoever answers the request.
 */
internal object RedirectResolver {
    private const val MAX_HOPS = 10

    /**
     * @return the URL the chain ends at.
     * @throws RevJetError.BlockedDestination when the URL, or a hop of its chain, is not publicly
     *   routable.
     * @throws RevJetError.NoRedirectURL when the chain cannot be followed.
     */
    suspend fun resolve(url: String): String {
        if (!RequestDestination.isAllowed(url)) throw RevJetError.BlockedDestination(url)

        return withContext(Dispatchers.IO) {
            var current = url

            repeat(MAX_HOPS) {
                val location =
                    runCatching { locationOf(current) }
                        .getOrElse { throw RevJetError.NoRedirectURL(it) }
                        ?: return@withContext current

                val next =
                    runCatching { URL(URL(current), location).toString() }
                        .getOrElse { throw RevJetError.NoRedirectURL(it) }

                if (!RequestDestination.isAllowed(next)) {
                    logDebug { "Refusing to follow a redirect to $next" }
                    throw RevJetError.BlockedDestination(next)
                }

                current = next
            }

            current
        }
    }

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
