package com.revjet.sdk.compose

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revjet.sdk.NativeTagResponse
import com.revjet.sdk.RevJetTagView
import com.revjet.sdk.RevJetTagViewListener
import com.revjet.sdk.Tag

/**
 * Shows an ad.
 *
 * A native tag renders through [nativeContent], which is handed the response as it arrives; a
 * web-based tag renders itself.
 *
 * The handlers here are the screen's. They are called alongside those the application set on the
 * [Tag] itself, and stop being called once the composable leaves the composition.
 *
 * @param onClick receives the destination of a click, once the ad server has resolved it. Opening
 *   it is the application's decision.
 * @param modifier sizes the ad, unless the tag was created with `updateDynamicHeight`, in which
 *   case the height the creative reports replaces the one given here.
 */
@Composable
public fun RevJetTag(
    tag: Tag,
    onClick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onBeforeLoad: (() -> Unit)? = null,
    onLoad: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    onTrackingEvent: ((Map<String, Any?>) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    nativeContent: (@Composable (NativeTagResponse) -> Unit)? = null,
) {
    // Updated while composing, so a load the view starts as it is created already sees them
    val handlers = rememberUpdatedState(Handlers(onClick, onBeforeLoad, onLoad, onClose, onTrackingEvent, onError))
    val listener = remember(tag) { ComposeListener(handlers) }
    DisposableEffect(listener) {
        onDispose { listener.release() }
    }

    val responsiveHeight by tag.responsiveHeight.collectAsStateWithLifecycle()
    val response by tag.nativeResponse.collectAsStateWithLifecycle()

    // The reported height comes first in the chain, so it wins over a height the caller passed
    val sized =
        responsiveHeight
            ?.takeIf { tag.updateDynamicHeight && it > 0f }
            ?.let { Modifier.fillMaxWidth().height(it.dp).then(modifier) }
            ?: modifier

    Box(modifier = sized) {
        val content = nativeContent
        val ad = response
        val drawsItsOwnAd = content != null && ad != null

        // A different tag needs a view of its own: the view is bound to one for its lifetime
        key(tag) {
            AndroidView(
                factory = { context -> RevJetTagView(context, tag, listener) },
                // Where the application draws the ad itself, the host has to cover exactly what is
                // drawn, since that is what viewability is measured from. A creative renders in the
                // host itself, so there it takes the size it was given, and its own where it has none
                modifier = if (drawsItsOwnAd) Modifier.matchParentSize() else Modifier.fillMaxSize(),
            )
        }

        if (content != null && ad != null) {
            Box(
                modifier =
                    Modifier.fillMaxWidth().pointerInput(tag) {
                        detectTapGestures { offset ->
                            tag.reportNativeTap(
                                x = offset.x,
                                y = offset.y,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                        }
                    },
            ) {
                content(ad)
            }
        }
    }
}

/** The handlers the composable was last composed with. */
private class Handlers(
    val click: (Uri) -> Unit,
    val beforeLoad: (() -> Unit)?,
    val load: (() -> Unit)?,
    val close: (() -> Unit)?,
    val trackingEvent: ((Map<String, Any?>) -> Unit)?,
    val error: ((Throwable) -> Unit)?,
)

/**
 * Reports the view's events to the composable, until it leaves the composition.
 *
 * The tag can outlive the screen, so once released nothing here reaches the screen's lambdas.
 */
private class ComposeListener(
    private var handlers: State<Handlers>?,
) : RevJetTagViewListener {
    fun release() {
        handlers = null
    }

    override fun onClick(
        view: RevJetTagView,
        url: Uri,
        tag: Tag,
    ) {
        handlers?.value?.click?.invoke(url)
    }

    override fun onBeforeLoad(
        view: RevJetTagView,
        tag: Tag,
    ) {
        handlers?.value?.beforeLoad?.invoke()
    }

    override fun onLoad(
        view: RevJetTagView,
        tag: Tag,
    ) {
        handlers?.value?.load?.invoke()
    }

    override fun onClose(
        view: RevJetTagView,
        tag: Tag,
    ) {
        handlers?.value?.close?.invoke()
    }

    override fun onTrackingEvent(
        view: RevJetTagView,
        event: Map<String, Any?>,
        tag: Tag,
    ) {
        handlers?.value?.trackingEvent?.invoke(event)
    }

    override fun onError(
        view: RevJetTagView,
        error: Throwable,
        tag: Tag,
    ) {
        handlers?.value?.error?.invoke(error)
    }
}
