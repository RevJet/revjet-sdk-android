package com.revjet.sdk.internal

import android.net.Uri

/** What a tag reports while its ad loads and is shown. */
internal interface TagEvents {
    fun onBeforeLoad() {}

    fun onLoad() {}

    fun onClick(url: Uri) {}

    fun onTrackingEvent(event: Map<String, Any?>) {}

    fun onError(error: Throwable) {}

    fun onClose() {}
}
