package com.revjet.sdk.sample

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens where a click leads, which may be outside the web and handled by no app on the device. */
fun openOrExplain(
    context: Context,
    url: Uri,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open $url", Toast.LENGTH_LONG).show()
    }
}
