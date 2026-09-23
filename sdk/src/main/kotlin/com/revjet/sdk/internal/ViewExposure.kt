package com.revjet.sdk.internal

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.revjet.sdk.RevJetSDK

/**
 * How much of a view the user can see.
 *
 * The visible area is the view's own rectangle, clipped to what the window shows and with anything
 * drawn on top of it removed. The percentage follows from that rectangle's area, rather than being
 * accumulated by subtraction, so the two can never disagree.
 */
internal object ViewExposure {
    fun calculate(view: View): ExposureData {
        if (!view.isAttachedToWindow || !view.isShown || view.isTransparent) return ExposureData.ZERO
        if (view.windowVisibility != View.VISIBLE) return ExposureData.ZERO
        if (view.width <= 0 || view.height <= 0) return ExposureData.ZERO

        val content = view.frameInWindow()
        val onScreen = Rect(content)
        if (!onScreen.intersect(visibleWindow(view))) return ExposureData.ZERO

        val visible = subtractObstructions(view, onScreen) ?: return ExposureData.ZERO
        val percentage = (visible.area.toFloat() / content.area.toFloat() * 100f).coerceIn(0f, 100f)

        if (percentage <= 0f) return ExposureData.ZERO

        val density = view.resources.displayMetrics.density

        // MRAID reports the visible part from the ad's own top-left corner
        return ExposureData(
            exposedPercentage = percentage,
            visibleRectangle =
                RectF(
                    (visible.left - content.left) / density,
                    (visible.top - content.top) / density,
                    (visible.right - content.left) / density,
                    (visible.bottom - content.top) / density,
                ),
        )
    }

    /** A parent faded out takes the ad with it, as `isShown` does for a parent that is hidden. */
    private val View.isTransparent: Boolean
        get() = generateSequence(this) { it.parent as? View }.any { it.alpha <= 0f }

    /** What the window shows, once the system bars are taken off it. */
    private fun visibleWindow(view: View): Rect {
        val root = view.rootView
        val window = Rect(0, 0, root.width, root.height)
        val insets =
            ViewCompat
                .getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
                ?: return window

        return Rect(
            window.left + insets.left,
            window.top + insets.top,
            window.right - insets.right,
            window.bottom - insets.bottom,
        )
    }

    /** Removes what is drawn on top of the ad, at any level up to the root. */
    private fun subtractObstructions(
        tracked: View,
        rect: Rect,
    ): Rect? {
        var remaining: Rect? = rect
        var view: View = tracked
        var parent = view.parent

        while (parent is ViewGroup) {
            val siblings = parent.childrenInDrawOrder()
            val index = siblings.indexOfFirst { it === view }

            for (position in (index + 1) until siblings.size) {
                remaining = subtractPaintedArea(siblings[position], remaining ?: return null)
            }

            view = parent
            parent = view.parent
        }

        return remaining
    }

    /**
     * Removes the area a view paints over.
     *
     * A view that only groups others paints nothing itself, so its children are the ones that can
     * obscure the ad.
     */
    private fun subtractPaintedArea(
        view: View,
        rect: Rect,
    ): Rect? {
        if (!view.isShown || view.alpha <= 0f) return rect
        if (RevJetSDK.isFriendlyObstruction(view)) return rect

        val frame = view.frameInWindow()
        if (!Rect.intersects(frame, rect)) return rect

        if (view.paintsOverContent) return RectSubtraction.subtract(rect, frame)

        var remaining: Rect? = rect
        if (view is ViewGroup) {
            for (child in view.childrenInDrawOrder()) {
                remaining = subtractPaintedArea(child, remaining ?: return null)
            }
        }

        return remaining
    }

    /** Whether the view draws over what is behind it. */
    private val View.paintsOverContent: Boolean
        get() {
            background?.let { background ->
                // A cleared background is the common way to make a grouping view paint nothing
                val paints = if (background is ColorDrawable) Color.alpha(background.color) > 0 else true
                if (paints) return true
            }

            return when (this) {
                is ImageView -> drawable != null
                is TextView -> !text.isNullOrEmpty()
                is SurfaceView, is TextureView, is WebView -> true
                else -> false
            }
        }

    /** Later in the list is drawn later, so a higher `z` comes after a higher index. */
    private fun ViewGroup.childrenInDrawOrder(): List<View> =
        (0 until childCount).map { getChildAt(it) }.sortedBy { it.z }

    private fun View.frameInWindow(): Rect {
        val location = IntArray(2)
        getLocationInWindow(location)

        return Rect(location[0], location[1], location[0] + width, location[1] + height)
    }
}
