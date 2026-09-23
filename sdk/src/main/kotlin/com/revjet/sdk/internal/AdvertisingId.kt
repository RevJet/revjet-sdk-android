package com.revjet.sdk.internal

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.revjet.sdk.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The device's advertising identifier, as reported by Google Play services. */
internal object AdvertisingId {
    private data class Reading(
        val id: String,
        val isLimitAdTrackingEnabled: Boolean,
    )

    @Volatile
    private var cached: Reading? = null

    /**
     * Reads the identifier, blocking, so it is confined to [Dispatchers.IO] and cached per process.
     *
     * A device without Play services reports no identifier and limited tracking, rather than
     * failing the ad request.
     */
    suspend fun read(context: Context): Pair<String, Boolean> =
        withContext(Dispatchers.IO) {
            cached?.let { return@withContext it.id to it.isLimitAdTrackingEnabled }

            val reading =
                runCatching {
                    val info = AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
                    Reading(info.id.orEmpty(), info.isLimitAdTrackingEnabled)
                }.getOrElse { error ->
                    logDebug { "Advertising ID unavailable: ${error.message}" }
                    Reading(id = "", isLimitAdTrackingEnabled = true)
                }

            cached = reading
            reading.id to reading.isLimitAdTrackingEnabled
        }
}
