package com.revjet.sdk.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revjet.sdk.DebugMode
import com.revjet.sdk.Option
import com.revjet.sdk.ResponsiveDimension
import com.revjet.sdk.RevJetSDK
import com.revjet.sdk.ScaleMode
import com.revjet.sdk.Tag
import com.revjet.sdk.TagType
import com.revjet.sdk.compose.RevJetTag

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RevJetSDK.setDebugEnabled(true)

        setContent {
            MaterialTheme {
                SampleScreen()
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val context = LocalContext.current
    var clickedUrl by remember { mutableStateOf<Uri?>(null) }

    val nativeTag =
        rememberTag {
            Tag(
                context = context,
                type = TagType.NATIVE,
                tag = "tag355022",
                key = "b37",
                reloadsOnReachable = true,
                debugMode = DebugMode.EMULATE,
            )
        }

    val responsiveTag =
        rememberTag {
            Tag(
                context = context,
                type = TagType.WEB_BASED,
                tag = "tag390451",
                key = "7c2",
                plcId = "266512461",
                isScrollEnabled = true,
                debugMode = DebugMode.EMULATE,
                options = listOf(Option.Responsive(true), Option.ResponsiveHeight(ResponsiveDimension.DYNAMIC)),
            )
        }

    val preloadedTag =
        rememberTag {
            Tag(
                context = context,
                type = TagType.WEB_BASED,
                tag = "tag291317",
                key = "05c",
                plcId = "181905075",
                debugMode = DebugMode.EMULATE,
                options = listOf(Option.Autoscale(true, ScaleMode.BEST_FIT)),
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("RevJet SDK ${RevJetSDK.version}", style = MaterialTheme.typography.titleLarge)

        OutlinedButton(onClick = { context.startActivity(Intent(context, ViewsActivity::class.java)) }) {
            Text("Open the view-based sample")
        }

        Section("Native tag, rendered by the application") {
            RevJetTag(
                tag = nativeTag,
                onClick = { clickedUrl = it },
                nativeContent = { response -> WildfirePreventionAd(response) { nativeTag.goToLP(it) } },
            )
        }

        // This creative is responsive: it lays itself out for the width it is given
        Section("JS tag, responsive") {
            RevJetTag(tag = responsiveTag, onClick = { clickedUrl = it }, modifier = Modifier.height(400.dp))
        }

        Section("JS tag, preloaded") {
            PreloadedTag(preloadedTag) { clickedUrl = it }
        }
    }

    clickedUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { clickedUrl = null },
            title = { Text("Open URL?") },
            text = { Text(url.toString()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        openOrExplain(context, url)
                        clickedUrl = null
                    },
                ) { Text("Open") }
            },
            dismissButton = { TextButton(onClick = { clickedUrl = null }) { Text("Cancel") } },
        )
    }
}

/** The ad is only requested once the application asks for it, and shown when it is ready. */
@Composable
private fun PreloadedTag(
    tag: Tag,
    onClick: (Uri) -> Unit,
) {
    val isLoaded by tag.isLoaded.collectAsStateWithLifecycle()
    val isLoading by tag.isLoading.collectAsStateWithLifecycle()

    when {
        isLoaded -> RevJetTag(tag = tag, onClick = onClick, modifier = Modifier.height(270.dp))

        isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))

        else -> Button(onClick = { tag.preload() }) { Text("Load tag") }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
        content()
    }
}

/** Keeps one tag for as long as the screen is shown, and releases its web view with it. */
@Composable
private fun rememberTag(create: () -> Tag): Tag {
    val tag = remember { create() }
    DisposableEffect(tag) {
        onDispose { tag.destroy() }
    }

    return tag
}
