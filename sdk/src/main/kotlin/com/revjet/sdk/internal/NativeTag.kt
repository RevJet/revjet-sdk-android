package com.revjet.sdk.internal

import android.content.Context
import com.revjet.sdk.DebugMode
import com.revjet.sdk.NativeTagResponse
import com.revjet.sdk.Option
import com.revjet.sdk.RevJetError
import com.revjet.sdk.logDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A tag whose ad the application renders itself, from the response reported to it.
 */
internal class NativeTag(
    context: Context,
    events: TagEvents,
    private val tag: String,
    private val key: String,
    private val plcId: String?,
    private val debugMode: DebugMode?,
    private val options: List<Option>,
    private val customParameters: Map<String, String>,
    reloadsOnReachable: Boolean,
    scope: CoroutineScope = mainScope(),
) : TagModel(context, events, reloadsOnReachable, scope) {
    private val _response = MutableStateFlow<NativeTagResponse?>(null)

    /** The ad that was received, for the view layer to render. */
    val response: StateFlow<NativeTagResponse?> = _response.asStateFlow()

    /**
     * The events already reported, so that each fires once per load.
     *
     * Read and written on the main thread only, alongside the timers below.
     */
    private val firedEvents = mutableSetOf<VisibilityEvent>()

    private var viewableJob: Job? = null
    private var invisibilityJob: Job? = null
    private var viewableMillis = 0L
    private var lastExposure: ExposureData? = null
    private var lastTapLocation: Pair<Float, Float>? = null

    override fun load() {
        // The reset has to be in place before the response can arrive
        scope.launch {
            firedEvents.clear()

            // Each ad is measured on its own: what the previous one accrued, and where it was
            // tapped, say nothing about this one
            stopTimers()
            viewableMillis = 0
            lastTapLocation = null

            _response.value = null
            state.isLoaded.value = false
            state.isLoading.value = true
            events.onBeforeLoad()

            fetch()

            // An ad replacing one already on screen is visible from the moment it arrives
            lastExposure?.let { updateExposure(it) }
        }
    }

    override fun destroy() {
        stopTimers()
        super.destroy()
    }

    private suspend fun fetch() {
        try {
            val (advertisingId, isLimitAdTrackingEnabled) = AdvertisingId.read(context)
            val url =
                AdRequest.nativeUrl(
                    tag = tag,
                    key = key,
                    plcId = plcId,
                    debugMode = debugMode,
                    options = options,
                    customParameters = customParameters,
                    identity = Identity.of(context.packageName, advertisingId, isLimitAdTrackingEnabled),
                )

            logDebug { "Requesting $url" }
            val response = NativeTagResponseParser.parse(Http.get(url))

            // Every mutation below happens on the main thread, alongside the timers
            _response.value = response
            trackEvent(VisibilityEvent.AD_LOAD_START)
            state.isLoaded.value = true
            trackEvent(VisibilityEvent.AD_LOADED)

            // Settled before the application hears of it, as a web-based tag does
            state.isLoading.value = false
            events.onLoad()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logDebug { "Loading failed: ${error.message}" }
            state.isLoaded.value = false
            state.isLoading.value = false
            events.onError(error)
        } finally {
            state.isLoading.value = false
        }
    }

    fun updateExposure(exposure: ExposureData) {
        lastExposure = exposure

        if (exposure.isHalfVisible) {
            startViewableTimer()
            stopInvisibilityTimer()
        } else {
            stopViewableTimer()
        }

        if (exposure.isInvisible) {
            stopViewableTimer()
            startInvisibilityTimer()
        } else {
            stopInvisibilityTimer()
        }
    }

    private fun startViewableTimer() {
        if (viewableJob != null) return

        viewableJob =
            scope.launch {
                while (isActive) {
                    delay(TICK_MS)
                    trackViewableDuration()
                }
            }
    }

    private fun trackViewableDuration() {
        if (viewableMillis >= COMPLETE_MS) return

        viewableMillis += TICK_MS

        when (viewableMillis) {
            VIEWABLE_MS -> {
                trackEvent(VisibilityEvent.AD_VIEWABLE_SHOWN)
                trackEvent(VisibilityEvent.AD_VIEWABLE)
            }
            FIRST_QUARTILE_MS -> trackEvent(VisibilityEvent.FIRST_QUARTILE)
            MIDPOINT_MS -> trackEvent(VisibilityEvent.MIDPOINT)
            THIRD_QUARTILE_MS -> trackEvent(VisibilityEvent.THIRD_QUARTILE)
            COMPLETE_MS -> {
                trackEvent(VisibilityEvent.COMPLETE)
                stopViewableTimer()
            }
        }
    }

    private fun startInvisibilityTimer() {
        if (invisibilityJob != null) return

        invisibilityJob =
            scope.launch {
                delay(INVISIBILITY_MS)

                if (lastExposure?.isInvisible == true) {
                    trackEvent(VisibilityEvent.AD_INVISIBLE)
                }

                invisibilityJob = null
            }
    }

    private fun stopViewableTimer() {
        viewableJob?.cancel()
        viewableJob = null
    }

    private fun stopInvisibilityTimer() {
        invisibilityJob?.cancel()
        invisibilityJob = null
    }

    private fun stopTimers() {
        stopViewableTimer()
        stopInvisibilityTimer()
    }

    fun trackEvent(
        event: VisibilityEvent,
        parameters: List<Pair<String, String>> = emptyList(),
    ) {
        if (!shouldEmit(event)) return

        val tracking = _response.value?.tracking?.firstOrNull { it.type == event } ?: return

        scope.launch {
            tracking.pixels.forEach { pixel ->
                if (Http.fire(pixel.withParameters(parameters))) {
                    logDebug { "Pixel request was sent: ${event.value}, parameters: $parameters" }
                }
            }
        }

        events.onTrackingEvent(
            mapOf("id" to tracking.id, "type" to tracking.type.value, "pixels" to tracking.pixels),
        )
    }

    private fun shouldEmit(event: VisibilityEvent): Boolean {
        if (_response.value == null) return false
        if (firedEvents.contains(event)) return false

        return when (event) {
            // A user can click, and move, more than once
            VisibilityEvent.AD_CLICKED, VisibilityEvent.HEATMAP_PIXEL -> true
            else -> {
                firedEvents.add(event)
                true
            }
        }
    }

    /** @param location and [viewSize] in pixels, as the view reports them. */
    fun handleClick(
        location: Pair<Float, Float>,
        viewSize: Pair<Float, Float>,
    ) {
        trackEvent(VisibilityEvent.AD_CLICKED)
        trackHeatmapPixel(location, viewSize)
    }

    private fun trackHeatmapPixel(
        location: Pair<Float, Float>,
        viewSize: Pair<Float, Float>,
    ) {
        lastTapLocation = location

        val width = maxOf(1f, viewSize.first)
        val height = maxOf(1f, viewSize.second)
        val x = (location.first / width * 100).coerceIn(0f, 100f).toInt()
        val y = (location.second / height * 100).coerceIn(0f, 100f).toInt()

        trackEvent(
            VisibilityEvent.HEATMAP_PIXEL,
            listOf(
                "_cx" to x.toString(),
                "_cy" to y.toString(),
                "_cw" to width.toInt().toString(),
                "_ch" to height.toInt().toString(),
            ),
        )
    }

    fun goToLP(url: String?) = openLandingPage(listOfNotNull(url?.let { "lp" to it }))

    fun goToLPEvent(eventTag: String) = openLandingPage(listOf("prm_tag" to eventTag))

    fun goToLPEventByName(eventName: String) = openLandingPage(listOf("prm_name" to eventName))

    private fun openLandingPage(parameters: List<Pair<String, String>>) {
        val response = _response.value ?: return

        try {
            openClick(LandingPage.url(response.linkValue, lastTapLocation, parameters))
        } catch (error: RevJetError.InvalidLPFormat) {
            logDebug { error.message.orEmpty() }
            events.onError(error)
        }
    }

    private companion object {
        const val TICK_MS = 500L
        const val VIEWABLE_MS = 1_000L
        const val FIRST_QUARTILE_MS = 7_500L
        const val MIDPOINT_MS = 15_000L
        const val THIRD_QUARTILE_MS = 22_500L
        const val COMPLETE_MS = 30_000L
        const val INVISIBILITY_MS = 10_000L
    }
}
