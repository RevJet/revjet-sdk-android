package com.revjet.sdk.internal

import com.revjet.sdk.Constants
import com.revjet.sdk.DebugMode
import com.revjet.sdk.Option
import com.revjet.sdk.RevJetError
import com.revjet.sdk.domain
import com.revjet.sdk.queryItems
import com.revjet.sdk.stringValue

internal object AdRequest {
    /** A host name, optionally with a port: what [Option.CustomDomain] has to be. */
    private val HOST = Regex("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(:\\d{1,5})?")

    private const val PLC_ID = "_plc_id"
    private const val AD_KEY = "adkey"
    private const val DEBUG = "debug"
    private const val SDK = "_js_sdk"
    private const val APP_ID = "_js_app_id"
    private const val IFA = "_js_ifa"

    /**
     * Checks what every request is built from, which the application supplies.
     *
     * @throws RevJetError.InvalidConfiguration when the tag or key is blank, or the domain is not a
     *   host name.
     */
    fun validate(
        tag: String,
        key: String,
        options: List<Option>,
    ) {
        if (tag.isBlank() || key.isBlank() || !HOST.matches(options.domain)) {
            throw RevJetError.InvalidConfiguration()
        }
    }

    /**
     * The URL a native tag requests its ad from.
     *
     * Option values are sent as they are: the quoting in [com.revjet.sdk.jsLiteral] belongs to the
     * tag script, not to a query string.
     *
     * @throws RevJetError.InvalidConfiguration as [validate] does.
     */
    fun nativeUrl(
        tag: String,
        key: String,
        plcId: String?,
        debugMode: DebugMode?,
        options: List<Option>,
        customParameters: Map<String, String>,
        identity: Identity,
    ): String {
        validate(tag, key, options)

        val parameters = mutableListOf<Pair<String, String>>()

        parameters += PLC_ID to (plcId ?: "")
        parameters += AD_KEY to key
        debugMode?.let { parameters += DEBUG to it.value }

        // The same identity the JavaScript tag reports through `MRAID_ENV`
        parameters += SDK to identity.sdkName
        parameters += APP_ID to identity.appId
        if (identity.advertisingId.isNotEmpty()) {
            parameters += IFA to identity.advertisingId
        }

        options
            .filterNot { it is Option.CustomDomain }
            .flatMap { it.queryItems }
            .forEach { (name, value) -> parameters += name to value.stringValue }

        customParameters.toSortedMap().forEach { (name, value) -> parameters += name to value }

        val query =
            parameters.joinToString("&") { (name, value) ->
                "${name.percentEncoded()}=${value.percentEncoded()}"
            }

        // The tag is one segment of the path, whatever it contains
        return "${Constants.HTTPS}://${options.domain}/${tag.percentEncoded()}?$query"
    }
}
