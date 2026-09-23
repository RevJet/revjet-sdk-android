package com.revjet.sdk.sample

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revjet.sdk.NativeTagResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Renders the creative behind the sample's native tag.
 *
 * A native response carries the campaign's own fields, so an application knows the ids it asked
 * for; this one shows a logo, a headline and a button.
 */
@Composable
fun WildfirePreventionAd(
    response: NativeTagResponse,
    onAction: (String) -> Unit,
) {
    val ad = remember(response) { NativeAd.from(response) } ?: return
    val logo = rememberRemoteImage(ad.logoUrl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color.Black),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                logo?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(80.dp),
                    )
                }

                Text(ad.headline, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Button(onClick = { onAction("https://www.google.com") }) {
                Text(ad.buttonTitle)
            }
        }
    }
}

private class NativeAd(
    val headline: String,
    val buttonTitle: String,
    val logoUrl: String,
) {
    companion object {
        private const val BUTTON = "el250348828"
        private const val HEADLINE = "el250343824"
        private const val LOGO = "el250343827"

        fun from(response: NativeTagResponse): NativeAd? {
            val values =
                runCatching {
                    val items = JSONObject(response.data).getJSONArray("personalization")

                    (0 until items.length()).associate { index ->
                        val item = items.getJSONObject(index)

                        item.getString("id") to item.getString("value")
                    }
                }.getOrNull() ?: return null

            return NativeAd(
                headline = values[HEADLINE] ?: return null,
                buttonTitle = values[BUTTON] ?: return null,
                logoUrl = values[LOGO] ?: return null,
            )
        }
    }
}

@Composable
private fun rememberRemoteImage(url: String): ImageBitmap? {
    var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        image =
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                }.getOrNull()
            }
    }

    return image
}
