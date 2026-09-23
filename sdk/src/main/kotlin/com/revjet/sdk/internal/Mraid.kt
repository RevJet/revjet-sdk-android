package com.revjet.sdk.internal

import android.content.Context
import com.revjet.sdk.RevJetSDK

/** The MRAID environment and shim injected into the ad document. */
internal object Mraid {
    /** The state of the ad container, as MRAID names them. */
    enum class ViewState(
        val value: String,
    ) {
        LOADING("loading"),
        DEFAULT("default"),
        EXPANDED("expanded"),
        HIDDEN("hidden"),
    }

    /** Where the ad sits, as MRAID names it. */
    enum class PlacementType(
        val value: String,
    ) {
        INLINE("inline"),
        INTERSTITIAL("interstitial"),
    }

    private const val SHIM_ASSET = "revjet/mraid.js"

    /**
     * The script to run at document start: the environment the tag reads, then the shim.
     *
     * The environment is built here rather than in the asset because its values are read from the
     * device at load time.
     */
    fun script(
        context: Context,
        identity: Identity,
    ): String {
        val environment =
            """
            window.MRAID_ENV = {
                version: '3.0',
                sdk: ${JsLiteral.string(identity.sdkName)},
                sdkVersion: ${JsLiteral.string(RevJetSDK.version)},
                appId: ${JsLiteral.string(identity.appId)},
                ifa: ${JsLiteral.string(identity.advertisingId)},
                limitAdTracking: ${identity.isAdTrackingLimited},
                coppa: ${RevJetSDK.isCOPPACompliant}
            };
            """.trimIndent()

        return environment + "\n\n" + context.assets.open(SHIM_ASSET).use { it.readBytes().decodeToString() }
    }
}
