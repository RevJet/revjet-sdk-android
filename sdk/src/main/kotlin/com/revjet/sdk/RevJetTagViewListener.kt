package com.revjet.sdk

import android.net.Uri
import android.view.View

/**
 * Receives what happens to the ad in a [RevJetTagView].
 *
 * Only [onClick] has to be implemented. A native tag also needs [onNativeResponse], since the
 * application renders that ad itself.
 */
public interface RevJetTagViewListener {
    /**
     * The ad was clicked, and the destination has been resolved.
     *
     * Opening it is the application's decision.
     */
    public fun onClick(
        view: RevJetTagView,
        url: Uri,
        tag: Tag,
    )

    /** The ad is about to load. */
    public fun onBeforeLoad(
        view: RevJetTagView,
        tag: Tag,
    ) {}

    /** The ad has loaded. */
    public fun onLoad(
        view: RevJetTagView,
        tag: Tag,
    ) {}

    /** Loading or rendering failed. */
    public fun onError(
        view: RevJetTagView,
        error: Throwable,
        tag: Tag,
    ) {}

    /** The creative closed itself. */
    public fun onClose(
        view: RevJetTagView,
        tag: Tag,
    ) {}

    /** A tracking event was reported. */
    public fun onTrackingEvent(
        view: RevJetTagView,
        event: Map<String, Any?>,
        tag: Tag,
    ) {}

    /**
     * A native ad arrived, and the application renders it.
     *
     * @return the view to show, or `null` to show nothing — which is what the Compose wrapper does,
     *   since it renders the response itself.
     */
    public fun onNativeResponse(
        view: RevJetTagView,
        response: NativeTagResponse,
        tag: Tag,
    ): View? = null
}
