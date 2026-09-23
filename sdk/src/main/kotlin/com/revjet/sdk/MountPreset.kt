package com.revjet.sdk

import android.view.ViewGroup
import androidx.core.graphics.Insets

/**
 * Where a [RevJetTagView] sits in the view it is mounted into.
 *
 * [Top] and [Bottom] follow the height a creative asks for, when the tag was created with
 * `updateDynamicHeight`.
 */
public sealed class MountPreset {
    /** Fills the parent. */
    public data class FillParent(
        public val padding: Insets = Insets.NONE,
    ) : MountPreset()

    /** Sits at the top of the parent. */
    public data class Top(
        public val padding: Insets = Insets.NONE,
    ) : MountPreset()

    /** Sits at the bottom of the parent. */
    public data class Bottom(
        public val padding: Insets = Insets.NONE,
    ) : MountPreset()

    /**
     * Placed by the application.
     *
     * The height a creative asks for is not followed here: the layout is the application's.
     */
    public data class Custom(
        public val layoutParams: ViewGroup.LayoutParams,
    ) : MountPreset()
}
