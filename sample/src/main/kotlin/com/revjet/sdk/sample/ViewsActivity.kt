package com.revjet.sdk.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.Insets
import com.revjet.sdk.DebugMode
import com.revjet.sdk.MountPreset
import com.revjet.sdk.NativeTagResponse
import com.revjet.sdk.RevJetTagView
import com.revjet.sdk.RevJetTagViewListener
import com.revjet.sdk.Tag
import com.revjet.sdk.TagType
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors

/** The same native ad as the Compose screen, built with views. */
class ViewsActivity :
    Activity(),
    RevJetTagViewListener {
    private lateinit var tag: Tag
    private lateinit var tagView: RevJetTagView

    private val images = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this)
        setContentView(container)

        tag =
            Tag(
                context = this,
                type = TagType.NATIVE,
                tag = "tag355022",
                key = "b37",
                debugMode = DebugMode.EMULATE,
            )

        tagView = RevJetTagView(this, tag, listener = this)
        tagView.mount(container, MountPreset.Bottom(Insets.of(16, 0, 16, 16)))
    }

    override fun onDestroy() {
        images.shutdownNow()
        tag.destroy()

        super.onDestroy()
    }

    override fun onClick(
        view: RevJetTagView,
        url: Uri,
        tag: Tag,
    ) {
        AlertDialog
            .Builder(this)
            .setTitle("Open URL?")
            .setMessage(url.toString())
            .setPositiveButton("Open") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, url)) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onNativeResponse(
        view: RevJetTagView,
        response: NativeTagResponse,
        tag: Tag,
    ): View? {
        val values = personalization(response) ?: return null

        val container =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(dp(20), dp(20), dp(20), dp(20))
            }

        val heading =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        heading.addView(
            ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
                values["el250343827"]?.let { load(it, into = this) }
            },
        )
        heading.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                text = values["el250343824"]
                textSize = 20f
                setTextColor(Color.BLACK)
                setPadding(dp(10), 0, 0, 0)
            },
        )

        container.addView(heading, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        container.addView(
            Button(this).apply {
                text = values["el250348828"]
                // The creative's own call to action, rather than a tap anywhere on the ad
                setOnClickListener { tag.goToLP("https://www.google.com") }
            },
        )

        return container
    }

    private fun personalization(response: NativeTagResponse): Map<String, String>? =
        runCatching {
            val items = JSONObject(response.data).getJSONArray("personalization")

            (0 until items.length()).associate { index ->
                val item = items.getJSONObject(index)

                item.getString("id") to item.getString("value")
            }
        }.getOrNull()

    private fun load(
        url: String,
        into: ImageView,
    ) {
        images.execute {
            val bitmap =
                runCatching {
                    URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull() ?: return@execute

            into.post { into.setImageBitmap(bitmap) }
        }
    }

    private fun dp(value: Int) =
        TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
            .toInt()
}
