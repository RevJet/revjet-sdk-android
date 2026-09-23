package com.revjet.sdk.internal

import com.revjet.sdk.RevJetError
import java.net.URI
import java.net.URISyntaxException

internal object LandingPage {
    /**
     * Where a click on a native ad leads, before the ad server resolves it: the response's link,
     * with the tap's position filled in and the parameters appended.
     *
     * @throws RevJetError.InvalidLPFormat when that is not a URL.
     */
    fun url(
        link: String,
        tapLocation: Pair<Float, Float>?,
        parameters: List<Pair<String, String>>,
    ): String {
        val url = link.withTapLocation(tapLocation).withParameters(parameters)

        try {
            URI(url)
        } catch (error: URISyntaxException) {
            throw RevJetError.InvalidLPFormat(base = link, location = tapLocation?.toString() ?: "none")
        }

        return url
    }

    /** The ad server asks for the tap's coordinates through placeholders in the link. */
    private fun String.withTapLocation(location: Pair<Float, Float>?): String {
        location ?: return this

        return replace("\$\$CX\$\$", location.first.toInt().toString())
            .replace("\$\$CY\$\$", location.second.toInt().toString())
    }
}

/** The URL with the parameters appended to its query. */
internal fun String.withParameters(parameters: List<Pair<String, String>>): String {
    if (parameters.isEmpty()) return this

    val query =
        parameters.joinToString("&") { (name, value) ->
            "${name.percentEncoded()}=${value.percentEncoded()}"
        }

    return if (contains("?")) "$this&$query" else "$this?$query"
}
