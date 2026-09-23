package com.revjet.sdk.internal

import com.revjet.sdk.RevJetSDK

/**
 * What the SDK reports about itself and the device, shared by the ad request and `MRAID_ENV`.
 */
internal data class Identity(
    val sdkName: String,
    val appId: String,
    val advertisingId: String,
    val isAdTrackingLimited: Boolean,
) {
    companion object {
        /**
         * @param advertisingId as read from the device, before any policy is applied.
         * @param isLimitAdTrackingEnabled as read from the device.
         */
        fun of(
            appId: String,
            advertisingId: String,
            isLimitAdTrackingEnabled: Boolean,
        ): Identity {
            val isCoppaCompliant = RevJetSDK.isCOPPACompliant
            // Google Play services answers with zeros when the user deleted the identifier, or the
            // app does not hold the AD_ID permission: there is no identifier to report
            val isWithheld = advertisingId.all { it == '0' || it == '-' }
            val isReported = !isCoppaCompliant && !isWithheld

            return Identity(
                sdkName = RevJetSDK.name,
                appId = appId,
                advertisingId = if (isReported) advertisingId else "",
                isAdTrackingLimited = isLimitAdTrackingEnabled || !isReported,
            )
        }
    }
}
