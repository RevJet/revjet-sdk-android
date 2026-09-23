package com.revjet.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.core.graphics.Insets
import com.revjet.sdk.internal.NativeTag
import com.revjet.sdk.internal.NativeTagUiView
import com.revjet.sdk.internal.TagContentView
import com.revjet.sdk.internal.TagEvents
import com.revjet.sdk.internal.WebBasedTag
import com.revjet.sdk.internal.WebBasedTagUiView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Shows an ad.
 *
 * The view loads the tag when it is created, unless the tag was preloaded, and reports what
 * happens to its [listener].
 */
@SuppressLint("ViewConstructor")
public class RevJetTagView
    @JvmOverloads
    constructor(
        context: Context,
        public val tag: Tag,
        public var listener: RevJetTagViewListener? = null,
    ) : FrameLayout(context) {
        private val content: TagContentView =
            when (val model = tag.model) {
                is NativeTag ->
                    NativeTagUiView(context, model) { response ->
                        listener?.onNativeResponse(this, response, tag)
                    }

                is WebBasedTag -> WebBasedTagUiView(context, model)
            }

        private var scope: CoroutineScope? = null
        private var mountPreset: MountPreset? = null

        init {
            addView(
                content.view,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
            )

            bindHandlers()
            loadAd()
        }

        /**
         * Places this view in [into].
         *
         * @param preset where it sits, and whether it follows the height a creative asks for.
         */
        @MainThread
        public fun mount(
            into: ViewGroup,
            preset: MountPreset,
        ) {
            mountPreset = preset
            (parent as? ViewGroup)?.removeView(this)
            into.addView(this, layoutParamsFor(preset))
        }

        private fun layoutParamsFor(preset: MountPreset): ViewGroup.LayoutParams =
            when (preset) {
                is MountPreset.Custom -> preset.layoutParams

                is MountPreset.FillParent ->
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT,
                    ).withPadding(preset.padding)

                is MountPreset.Top ->
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.TOP,
                    ).withPadding(preset.padding)

                is MountPreset.Bottom ->
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM,
                    ).withPadding(preset.padding)
            }

        private fun LayoutParams.withPadding(padding: Insets) =
            apply {
                leftMargin = padding.left
                topMargin = padding.top
                rightMargin = padding.right
                bottomMargin = padding.bottom
            }

        /**
         * Loads unless the tag already carries an ad, so that a preloaded tag is shown rather than
         * requested again.
         */
        private fun loadAd() {
            if (tag.isLoaded.value || tag.isLoading.value) return

            tag.model.load()
        }

        /** The tag outlives the view, so what it keeps must not hold on to the view. */
        private fun bindHandlers() {
            tag.viewEvents = ListenerEvents(WeakReference(this))
        }

        /** Reports the tag's events to the view's listener, for as long as the view is alive. */
        private class ListenerEvents(
            private val view: WeakReference<RevJetTagView>,
        ) : TagEvents {
            override fun onBeforeLoad() {
                view.get()?.run { listener?.onBeforeLoad(this, tag) }
            }

            override fun onLoad() {
                view.get()?.run { listener?.onLoad(this, tag) }
            }

            override fun onClick(url: Uri) {
                view.get()?.run { listener?.onClick(this, url, tag) }
            }

            override fun onTrackingEvent(event: Map<String, Any?>) {
                view.get()?.run { listener?.onTrackingEvent(this, event, tag) }
            }

            override fun onError(error: Throwable) {
                view.get()?.run { listener?.onError(this, error, tag) }
            }

            override fun onClose() {
                view.get()?.run { listener?.onClose(this, tag) }
            }
        }

        /**
         * A creative measured without a height limit reports the height of its whole document,
         * which a scrolling container would then grant it. The window is the limit instead.
         */
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val heightSpec =
                if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                    MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, MeasureSpec.AT_MOST)
                } else {
                    heightMeasureSpec
                }

            super.onMeasure(widthMeasureSpec, heightSpec)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()

            content.onHostAttached()

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { this.scope = it }
            scope.launch {
                tag.responsiveHeight.filterNotNull().collect { height -> applyHeight(height) }
            }
        }

        override fun onDetachedFromWindow() {
            content.onHostDetached()
            scope?.cancel()
            scope = null

            super.onDetachedFromWindow()
        }

        /** The creative's height is in density-independent pixels, the layout wants pixels. */
        private fun applyHeight(heightDp: Float) {
            if (!tag.updateDynamicHeight) return
            when (mountPreset) {
                is MountPreset.Top, is MountPreset.Bottom -> Unit
                else -> return
            }

            val params = layoutParams ?: return
            val height = (heightDp * resources.displayMetrics.density).toInt()

            if (height <= 0 || params.height == height) return

            params.height = height
            layoutParams = params
        }
    }
