package com.revjet.sdk.internal

import com.revjet.sdk.AdContext
import com.revjet.sdk.AdTracking
import com.revjet.sdk.NativeTagResponse
import com.revjet.sdk.logDebug
import org.json.JSONObject

internal object NativeTagResponseParser {
    /**
     * @throws org.json.JSONException when a field the SDK needs is missing or of the wrong type.
     */
    fun parse(json: String): NativeTagResponse {
        val root = JSONObject(json)
        val context = root.getJSONObject("context")

        return NativeTagResponse(
            version = root.getString("version"),
            // Kept as text: the shape below `data` belongs to the creative, not to the SDK
            data = root.getJSONObject("data").toString(),
            linkValue = root.getString("link"),
            width = root.getString("width"),
            height = root.getString("height"),
            context =
                AdContext(
                    crv = context.getString("crv"),
                    eg = context.getString("eg"),
                    tag = context.getString("tag"),
                    adt = context.getString("adt"),
                ),
            tracking = parseTracking(root),
        )
    }

    /**
     * Entries with an event the SDK does not know are skipped rather than failing the response,
     * so that a new event type on the server cannot stop an ad from rendering.
     */
    private fun parseTracking(root: JSONObject): List<AdTracking> {
        val array = root.optJSONArray("tracking") ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val entry = array.getJSONObject(index)
            val type = VisibilityEvent.from(entry.getString("type"))

            if (type == null) {
                logDebug { "Ignoring tracking entry of unknown type: ${entry.optString("type")}" }
                return@mapNotNull null
            }

            val pixels = entry.optJSONArray("pixels")
            AdTracking(
                id = entry.getInt("id"),
                type = type,
                pixels = pixels?.let { array -> (0 until array.length()).map(array::getString) }.orEmpty(),
            )
        }
    }
}
