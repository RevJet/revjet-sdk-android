package com.revjet.sdk

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.revjet.sdk.internal.NativeTag
import com.revjet.sdk.internal.TagEvents
import com.revjet.sdk.internal.TagModel
import com.revjet.sdk.internal.WebBasedTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An ad tag: what to request, and the state and events of the ad that comes back.
 *
 * A tag is independent of the view showing it, so it can be loaded before there is one. Call
 * [destroy] when it will not be shown again.
 *
 * Used from the main thread, where its state is published and its handlers are called.
 */
public class Tag
    @JvmOverloads
    constructor(
        context: Context,
        public val type: TagType,
        public val tag: String,
        public val key: String,
        /** The placement the ad is requested for. */
        public val plcId: String? = null,
        /** Allows scrolling inside a web-based ad. */
        public val isScrollEnabled: Boolean = false,
        /** Follows the height the creative asks for. */
        public val updateDynamicHeight: Boolean = false,
        /** Loads again once the network is available, if the ad never arrived. */
        public val reloadsOnReachable: Boolean = false,
        /** Keeps test runs out of production statistics. */
        public val debugMode: DebugMode? = null,
        /** Passed to the tag script, and to the ad server with the request. */
        public val options: List<Option> = emptyList(),
        /** Sent to the ad server as request parameters of their own. */
        public val customParameters: Map<String, String> = emptyMap(),
    ) {
        private val noResponse: StateFlow<NativeTagResponse?> = MutableStateFlow(null).asStateFlow()

        /**
         * The view showing this tag.
         *
         * Kept apart from the application's handlers below so that neither can overwrite the
         * other, whichever is set first.
         */
        internal var viewEvents: TagEvents? = null

        /** Reports each of the model's events to the application, then to the view. */
        private val events =
            object : TagEvents {
                override fun onBeforeLoad() {
                    this@Tag.onBeforeLoad?.invoke()
                    viewEvents?.onBeforeLoad()
                }

                override fun onLoad() {
                    this@Tag.onLoad?.invoke()
                    viewEvents?.onLoad()
                }

                override fun onClick(url: Uri) {
                    this@Tag.onClick?.invoke(url)
                    viewEvents?.onClick(url)
                }

                override fun onTrackingEvent(event: Map<String, Any?>) {
                    this@Tag.onTrackingEvent?.invoke(event)
                    viewEvents?.onTrackingEvent(event)
                }

                override fun onError(error: Throwable) {
                    this@Tag.onError?.invoke(error)
                    viewEvents?.onError(error)
                }

                override fun onClose() {
                    this@Tag.onClose?.invoke()
                    viewEvents?.onClose()
                }
            }

        internal val model: TagModel =
            when (type) {
                TagType.NATIVE ->
                    NativeTag(
                        context = context.applicationContext,
                        events = events,
                        tag = tag,
                        key = key,
                        plcId = plcId,
                        debugMode = debugMode,
                        options = options,
                        customParameters = customParameters,
                        reloadsOnReachable = reloadsOnReachable,
                    )

                TagType.WEB_BASED ->
                    WebBasedTag(
                        context = context.applicationContext,
                        events = events,
                        tag = tag,
                        key = key,
                        plcId = plcId,
                        isScrollEnabled = isScrollEnabled,
                        updateDynamicHeight = updateDynamicHeight,
                        debugMode = debugMode,
                        options = options,
                        customParameters = customParameters,
                        reloadsOnReachable = reloadsOnReachable,
                    )
            }

        /** Whether the ad has finished loading. */
        public val isLoaded: StateFlow<Boolean> get() = model.isLoaded

        /** Whether the ad is being loaded. */
        public val isLoading: StateFlow<Boolean> get() = model.isLoading

        /** Whether the creative closed itself. */
        public val isClosedByUser: StateFlow<Boolean> get() = model.isClosedByUser

        /** The height the creative asked for, when [updateDynamicHeight] is set. */
        public val responsiveHeight: StateFlow<Float?> get() = model.responsiveHeight

        /** Called before the ad starts loading. */
        public var onBeforeLoad: (() -> Unit)? = null

        /** Called once the ad has loaded. */
        public var onLoad: (() -> Unit)? = null

        /**
         * Called with the destination of a click, after the ad server has resolved it.
         *
         * It may lie outside the web — the Play Store, a deep link, `tel:` — where no app on the
         * device may handle it.
         */
        public var onClick: ((Uri) -> Unit)? = null

        /** Called when a tracking event is reported. */
        public var onTrackingEvent: ((Map<String, Any?>) -> Unit)? = null

        /** Called when loading or rendering fails. */
        public var onError: ((Throwable) -> Unit)? = null

        /** Called when the creative closes itself. */
        public var onClose: (() -> Unit)? = null

        /**
         * The native ad that was received, for an application that renders it itself.
         *
         * Always `null` for a web-based tag.
         */
        public val nativeResponse: StateFlow<NativeTagResponse?>
            get() = (model as? NativeTag)?.response ?: noResponse

        /**
         * Reports a tap on a native ad rendered by the application.
         *
         * @param x and [y] where the tap landed, [width] and [height] the size of what was tapped,
         *   both in pixels.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @MainThread
        public fun reportNativeTap(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
        ) {
            (model as? NativeTag)?.handleClick(x to y, width to height)
        }

        /** Loads the ad again. */
        @MainThread
        public fun reload() {
            model.load()
        }

        /**
         * Loads the ad ahead of showing it.
         *
         * The handlers given here stay in place for later loads, and are reported alongside those
         * of the view once there is one.
         */
        @JvmOverloads
        @MainThread
        public fun preload(
            onBeforeLoad: (() -> Unit)? = null,
            onLoad: (() -> Unit)? = null,
        ): Tag {
            onBeforeLoad?.let { this.onBeforeLoad = it }
            onLoad?.let { this.onLoad = it }

            model.load()

            return this
        }

        /**
         * Opens the landing page of a native ad.
         *
         * Native ads do not navigate on their own: call this from your own tap handling, then open
         * the URL reported to [onClick].
         *
         * @param url an optional destination to override the creative's own.
         */
        @JvmOverloads
        @MainThread
        public fun goToLP(url: String? = null) {
            (model as? NativeTag)?.goToLP(url)
        }

        /** Reports an event by ID, then opens the landing page. */
        @MainThread
        public fun goToLPEvent(eventTag: String) {
            (model as? NativeTag)?.goToLPEvent(eventTag)
        }

        /** Reports an event by name, then opens the landing page. */
        @MainThread
        public fun goToLPEventByName(eventName: String) {
            (model as? NativeTag)?.goToLPEventByName(eventName)
        }

        /**
         * Releases the timers, requests and views this tag holds.
         *
         * A web view is not reclaimed on its own, so a tag that will not be shown again has to be
         * destroyed — from `onDestroy`, typically.
         */
        @MainThread
        public fun destroy() {
            model.destroy()
        }
    }
