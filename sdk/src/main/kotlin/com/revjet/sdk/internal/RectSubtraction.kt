package com.revjet.sdk.internal

import android.graphics.Rect

internal object RectSubtraction {
    /**
     * The largest rectangle left after removing [other].
     *
     * What remains of a corner overlap is L-shaped, so the largest remaining rectangle is returned
     * instead. That under-reports what is left rather than over-reporting it.
     *
     * @return `null` when nothing is left.
     */
    fun subtract(
        rect: Rect,
        other: Rect,
    ): Rect? {
        val overlap = Rect(rect)
        if (!overlap.intersect(other)) return rect
        if (overlap.contains(rect)) return null

        val remainders =
            listOf(
                Rect(rect.left, rect.top, rect.right, overlap.top),
                Rect(rect.left, overlap.bottom, rect.right, rect.bottom),
                Rect(rect.left, rect.top, overlap.left, rect.bottom),
                Rect(overlap.right, rect.top, rect.right, rect.bottom),
            )

        return remainders.filter { it.width() > 0 && it.height() > 0 }.maxByOrNull { it.area }
    }
}

internal val Rect.area: Long get() = width().toLong() * height().toLong()
