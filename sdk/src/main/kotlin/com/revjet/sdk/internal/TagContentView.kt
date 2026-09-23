package com.revjet.sdk.internal

import android.view.View

/** What a [com.revjet.sdk.RevJetTagView] shows: a native ad's own view, or a web view. */
internal interface TagContentView {
    /** The view to add to the tag view. */
    val view: View

    /** Called when the tag view is attached to a window, and its ad can be measured. */
    fun onHostAttached()

    fun onHostDetached()
}
