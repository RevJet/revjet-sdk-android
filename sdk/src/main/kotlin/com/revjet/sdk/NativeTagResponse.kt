package com.revjet.sdk

import android.net.Uri
import com.revjet.sdk.internal.VisibilityEvent

/**
 * The ad a native tag received, for the application to render.
 */
public class NativeTagResponse internal constructor(
    /** The version of the response format. */
    public val version: String,
    /**
     * The personalization payload, as the JSON text the ad server sent.
     *
     * Parse it with whatever the application already uses; its shape belongs to the creative.
     */
    public val data: String,
    internal val linkValue: String,
    /** The creative's width, as sent, which may carry a unit such as `300px`. */
    public val width: String,
    /** The creative's height, as sent, which may carry a unit such as `250px`. */
    public val height: String,
    /** Identifiers describing the ad that was served. */
    public val context: AdContext,
    internal val tracking: List<AdTracking>,
) {
    /** Where a click leads, before the server resolves its redirects. */
    public val link: Uri get() = Uri.parse(linkValue)
}

/** Identifiers describing the ad that was served. */
public data class AdContext(
    public val crv: String,
    public val eg: String,
    public val tag: String,
    public val adt: String,
)

/** The pixels to fire for one tracking event. */
internal class AdTracking(
    val id: Int,
    val type: VisibilityEvent,
    val pixels: List<String>,
)
