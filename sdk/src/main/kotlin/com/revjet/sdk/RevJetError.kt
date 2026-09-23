package com.revjet.sdk

/** An error the SDK reports to the application. */
public sealed class RevJetError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The ad request could not be built from the tag's configuration. */
    public class InvalidConfiguration :
        RevJetError(
            "The configuration required to build a valid ad request is incorrect or incomplete. " +
                "Check the tag, key and options.",
        )

    /** The ad reported a click without a destination. */
    public class NoClickURL :
        RevJetError(
            "The ad reported a click without a destination URL.",
        )

    /** The redirect chain of a click could not be followed. */
    public class NoRedirectURL(
        cause: Throwable,
    ) : RevJetError(
            "Resolving the click URL failed: ${cause.message}",
            cause,
        )

    /** A landing page URL could not be built from the response. */
    public class InvalidLPFormat(
        public val base: String,
        public val location: String,
    ) : RevJetError("URL construction failed for base: $base, with location: $location")

    /** The ad reported a tracking event without its details. */
    public class TrackingEventDetailsUnavailable :
        RevJetError(
            "Tracking event details are unavailable.",
        )

    /**
     * The SDK refused to request a destination that is not publicly routable.
     *
     * Content shown by the SDK chooses the URL of a click, so it must not be able to reach the
     * loopback interface or a private network.
     */
    public class BlockedDestination(
        public val url: String,
    ) : RevJetError(
            "The SDK refused to request '$url'. Only publicly routable destinations are requested.",
        )

    /**
     * The device's WebView is too old to host a web-based tag safely.
     *
     * The SDK needs `WEB_MESSAGE_LISTENER` and `DOCUMENT_START_SCRIPT` to tell the ad document apart
     * from content inside it; a WebView that updates through Google Play has both.
     */
    public class WebViewUnsupported :
        RevJetError(
            "This device's WebView lacks WEB_MESSAGE_LISTENER or DOCUMENT_START_SCRIPT, which the SDK " +
                "needs to tell the ad document apart from content inside it.",
        )
}
