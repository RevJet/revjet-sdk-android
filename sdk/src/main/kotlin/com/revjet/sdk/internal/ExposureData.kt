package com.revjet.sdk.internal

import android.graphics.RectF

/**
 * How much of a view the user can see.
 *
 * Measured in density-independent pixels, which is what MRAID expects. Android measures in
 * pixels, so the conversion happens where this is produced.
 */
internal data class ExposureData(
    /** From 0 for out of sight, to 100 for wholly visible. */
    val exposedPercentage: Float,
    /** The visible part of the view, from the view's own top-left corner, or `null` when there is none. */
    val visibleRectangle: RectF? = null,
) {
    val isHalfVisible: Boolean get() = exposedPercentage >= 50f

    val isInvisible: Boolean get() = exposedPercentage == 0f

    companion object {
        val ZERO = ExposureData(exposedPercentage = 0f)
    }
}
