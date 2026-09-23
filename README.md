# RevJet SDK for Android

RevJet SDK is a mobile advertising SDK for Android that supports both View-based and Compose
applications. It renders native and JavaScript ad tags, reports impressions, clicks and
viewability, and leaves the decision of where a click goes to the application.

It is the Android counterpart of [revjet-sdk-ios](https://github.com/RevJet/revjet-sdk-ios), and
follows it closely: the same tag configuration, the same events, the same MRAID surface.

## Overview

```kotlin
import com.revjet.sdk.RevJetTagView      // Views
import com.revjet.sdk.compose.RevJetTag  // Compose
```

## Requirements

- **Android 7.0 (API 24)** or later.
- For JavaScript tags, a WebView that supports `WebMessageListener` and document start scripts —
  the two things that let the SDK tell the ad document apart from content inside it. A WebView
  that updates through Google Play has both; one frozen at an old version may not (WebView 91, for
  instance, has the listener but not document start scripts). There, a web-based tag reports
  `RevJetError.WebViewUnsupported` through `onError` and shows nothing, rather than falling back
  to a bridge that cannot tell frames apart. Native tags are unaffected.
- Kotlin 2.4, Android Gradle Plugin 9, JDK 17.

## Features

- **Native and JavaScript tags**: `TagType.NATIVE` for ads the application renders itself,
  `TagType.WEB_BASED` for creatives rendered in a WebView.
- **Views and Compose**: `RevJetTagView` for View hierarchies, `RevJetTag` for Compose.
- **Flexible layout**: mount presets with padding, and support for the height a creative asks for.
- **Native views**: render a native ad with your own layout or composable.
- **Tracking**: impressions, clicks and the creative's own tracking events.
- **Viewability**: exposure measured from what the user can actually see, with friendly
  obstructions for app chrome that overlays ads on purpose.
- **Privacy**: COPPA mode, and the advertising identifier only when the device allows it.
- **Security**: the bridge only accepts the ad document's main frame; neither creatives nor tracking
  pixels can reach private or loopback addresses; and creatives cannot navigate the ad document
  away.

## Installation

The SDK ships as source from GitHub, so an application builds it alongside its own code and can
read and step through it.

### As a Gradle module

Add the repository next to your project — a git submodule keeps it updatable:

```sh
git submodule add https://github.com/RevJet/revjet-sdk-android.git vendor/revjet-sdk-android
```

Then include the modules you need in `settings.gradle.kts`:

```kotlin
include(":sdk")
project(":sdk").projectDir = file("vendor/revjet-sdk-android/sdk")

// only for Compose applications
include(":sdk-compose")
project(":sdk-compose").projectDir = file("vendor/revjet-sdk-android/sdk-compose")
```

```kotlin
dependencies {
    implementation(project(":sdk"))
    implementation(project(":sdk-compose"))
}
```

The modules build with the Android Gradle Plugin and Kotlin versions in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml); an application on older ones should
either align, or build the SDK separately and depend on the artifacts below.

### As artifacts

The modules also publish as `com.revjet:revjet-sdk` and `com.revjet:revjet-sdk-compose`, which is
the simpler route for an application that does not want the sources in its build:

```sh
./gradlew publishToMavenLocal   # or publish, against your own repository
```

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.revjet:revjet-sdk:2.1.0")
    implementation("com.revjet:revjet-sdk-compose:2.1.0")
}
```

### Permissions

The SDK's manifest declares what it needs, so nothing has to be added to the application's:

- `android.permission.INTERNET` — requesting ads and reporting tracking.
- `android.permission.ACCESS_NETWORK_STATE` — noticing the network come back, for tags created
  with `reloadsOnReachable`. Removing it only turns that off.
- `com.google.android.gms.permission.AD_ID` — reading the advertising identifier. Remove it with a
  `tools:node="remove"` override if the application must not request it; the SDK then reports no
  identifier and limited ad tracking.

It also declares `<queries>` for `tel:` and `sms:`, so that MRAID can tell a creative whether the
device can make a call or send a text. It needs no permission.

## Products

- **`revjet-sdk`**: the SDK. `Tag`, `RevJetTagView`, mount presets, tracking and viewability.
- **`revjet-sdk-compose`**: the `RevJetTag` composable, which wraps the view above. It depends on
  `revjet-sdk`, so an application that uses Compose needs only this one.

## Public interface

### `RevJetSDK`

SDK-wide configuration.

- `val version: String` — the SDK version.
- `fun setDebugEnabled(enabled: Boolean)` — logs what the SDK does to Logcat, under the tag
  `RevJetSDK`. It also makes the application's WebViews debuggable through `chrome://inspect`, since
  WebView debugging is process-wide, so keep it to debug builds.
- `fun setCOPPACompliance(enabled: Boolean)` — see below.
- `fun registerFriendlyObstruction(view: View)` / `unregisterFriendlyObstruction(view: View)` /
  `clearFriendlyObstructions()` — see below.

```kotlin
RevJetSDK.setCOPPACompliance(true) // a child-directed app
RevJetSDK.setDebugEnabled(BuildConfig.DEBUG)
```

### Friendly obstructions

Viewability is measured from what the user can actually see, so a view drawn on top of an ad
reduces the ad's reported exposure. App chrome that unavoidably overlays ads — a floating action
button, a player's controls — can be declared instead, and is then left out of that measurement:

```kotlin
RevJetSDK.registerFriendlyObstruction(floatingActionButton)
```

Views inside a registered view are covered as well. Views are held weakly, so a registered view
that goes away needs no clean-up. Register only chrome that genuinely belongs to your app — an
overlay declared here still hides the ad from the user.

### COPPA compliance

When enabled, the SDK complies with the
[Children's Online Privacy Protection Act](https://www.ftc.gov/enforcement/rules/rulemaking-regulatory-reform-proceedings/childrens-online-privacy-protection-rule):
it reports no advertising identifier for native or web-based tags, marks ad tracking as limited in
every request, and tells the creative that the app is child-directed.

It is read as each request is built, so set it **before any tag loads** — from `Application.onCreate`,
typically:

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        RevJetSDK.setCOPPACompliance(true)
    }
}
```

### `Tag`

An ad tag: what to request, and the state and events of the ad that comes back. A tag is
independent of the view showing it, so it can be loaded before there is one.

```kotlin
Tag(
    context: Context,
    type: TagType,
    tag: String,
    key: String,
    plcId: String? = null,
    isScrollEnabled: Boolean = false,      // web-based tags only
    updateDynamicHeight: Boolean = false,  // follow the height the creative asks for
    reloadsOnReachable: Boolean = false,   // load again once the network is back, if no ad arrived
    debugMode: DebugMode? = null,
    options: List<Option> = emptyList(),
    customParameters: Map<String, String> = emptyMap(),
)
```

State is exposed as `StateFlow`, so it can be collected in Compose with
`collectAsStateWithLifecycle()` or in a `lifecycleScope`:

- `isLoaded: StateFlow<Boolean>` — an ad has arrived.
- `isLoading: StateFlow<Boolean>` — a request is in flight.
- `isClosedByUser: StateFlow<Boolean>` — the creative closed itself.
- `responsiveHeight: StateFlow<Float?>` — the height the creative asked for, in density-independent
  pixels, when `updateDynamicHeight` is set.
- `nativeResponse: StateFlow<NativeTagResponse?>` — the native ad to render; always `null` for a
  web-based tag.

Events are assignable properties. `RevJetTagView` reports to its listener alongside them, so both
can be used at once:

`onBeforeLoad`, `onLoad`, `onClick`, `onClose`, `onError`, `onTrackingEvent`.

Methods:

- `reload()` — loads the ad again.
- `preload(onBeforeLoad, onLoad): Tag` — loads ahead of showing. The handlers stay in place for
  later loads. A view created for a tag that already carries an ad shows it rather than requesting
  another one.
- `goToLP(url: String? = null)` — opens the landing page of a native ad, optionally overriding its
  destination. The resolved URL arrives through `onClick`.
- `goToLPEvent(eventTag: String)` / `goToLPEventByName(eventName: String)` — report an event, then
  open the landing page.
- `destroy()` — releases the timers, requests and the WebView. A WebView is not reclaimed on its
  own, so a tag that will not be shown again has to be destroyed, from `onDestroy` or an
  `onDispose`.

> Native ads do not navigate on their own. Call one of the `goToLP` methods from your own tap
> handling, then open the URL reported to `onClick`. Where a click goes is the application's
> decision; the SDK resolves the destination and hands it over.

### Click destinations

The SDK follows a click's redirects itself, so that it only ever requests publicly routable
addresses. The destination it reports to `onClick` is usually a web page, but can be anywhere the
ad server sends the user outside the web — the Play Store (`market://`), another app through a deep
link, `tel:`, `sms:` or `mailto:`. Those are handed over as they are, without being requested.

Nothing on the device may handle such a URL, so open it expecting that:

```kotlin
try {
    startActivity(Intent(Intent.ACTION_VIEW, url))
} catch (error: ActivityNotFoundException) {
    // no app can open it
}
```

URLs that are never a place a click leads are refused with `BlockedDestination`: `javascript:`,
`file:`, `content:`, `data:`, `blob:`, `about:`, and `intent:`, which `ACTION_VIEW` cannot open and
which, parsed into an `Intent`, would let an ad start the application's private components.

### `Option`

Options configure how the ad server and the creative behave. They are passed as `options` when
creating a `Tag`, and reach native and web-based tags alike.

| Option | Sent as | Description |
| --- | --- | --- |
| `Option.ImpBannerSize(width, height)` | `_imp_banner_size` | Requested creative size, e.g. `300`×`250`. |
| `Option.Autoscale(enabled, mode = ScaleMode.FIT_TO_WIDTH)` | `autoscale`, `autoscale_mode` | Scales the creative to its container. |
| `Option.Autohide(enabled)` | `autohide` | Hides the ad slot when no creative is served. |
| `Option.Responsive(enabled)` | `responsive` | Lets the creative size itself to the container. |
| `Option.ResponsiveHeight(dimension)` | `responsive_height` | How the height is determined. |
| `Option.ResponsiveWidth(dimension)` | `responsive_width` | How the width is determined. |
| `Option.AdaptiveSizes(sizes)` | `adaptive_sizes` | Sizes the server may choose from, e.g. `listOf("300x250", "320x50")`. |
| `Option.Delivery(method)` | `delivery_method` | Delivery format of the ad. |
| `Option.InApp(value)` | `in_app` | Identifies the hosting app to the ad server. |
| `Option.NoSession(enabled)` | `no_session` | Excludes the request from session tracking. |
| `Option.Content(value)` | `content` | Contextual content passed to the creative. |
| `Option.CustomDomain(domain)` | — | Serves the tag from another domain instead of `ads.revjet.com`. |
| `Option.Custom(values)` | each key | Any further options the ad server accepts. |

Supporting types:

- `ScaleMode`: `BEST_FIT`, `FIT_TO_WIDTH` (default), `FIT_TO_HEIGHT`
- `ResponsiveDimension`: `FIXED`, `DYNAMIC`, `FIT`
- `DeliveryMethod`: `BANNER`
- `DebugMode`: `EMULATE` — keeps test runs out of production statistics
- `OptionValue`: `OptionValue.of(…)` for a `String`, `Int`, `Double` or `Boolean`, e.g.
  `Option.Custom(mapOf("tier" to OptionValue.of("gold"), "score" to OptionValue.of(7)))`

`customParameters` differ from options: they are sent to the ad server as request parameters of
their own, rather than configuring the tag.

## Showing an ad with views

```kotlin
val tagView = RevJetTagView(context, tag, listener = this)
tagView.mount(container, MountPreset.Bottom(Insets.of(16, 0, 16, 16)))
```

`RevJetTagView` loads the tag when it is created, unless the tag was preloaded, and reports what
happens to its `listener`.

### `MountPreset`

| Preset | Where the ad sits |
| --- | --- |
| `MountPreset.FillParent(padding)` | Fills the parent. |
| `MountPreset.Top(padding)` | At the top, following the height the creative asks for. |
| `MountPreset.Bottom(padding)` | At the bottom, following the height the creative asks for. |
| `MountPreset.Custom(layoutParams)` | Placed by the application; the height a creative asks for is not followed. |

Padding is `androidx.core.graphics.Insets`, in pixels.

### Dynamic height

The creative reports the height it wants when the tag is created with `updateDynamicHeight = true`.
`Top` and `Bottom` then resize the view as it changes, and `responsiveHeight` carries the same value
for an application that lays the ad out itself.

A creative measured in a container that imposes no height limit — a scrolling column, for
instance — reports the height of its whole document, which can be far larger than a screen. The
view caps such a measurement at the window's height.

### `RevJetTagViewListener`

Only `onClick` has to be implemented:

- `onClick(view, url, tag)` — the ad was clicked and the destination resolved. Opening it is the
  application's decision.
- `onNativeResponse(view, response, tag): View?` — a native ad arrived; return the view that
  renders it, or `null` to render it elsewhere.
- `onBeforeLoad(view, tag)`, `onLoad(view, tag)`, `onError(view, error, tag)`,
  `onClose(view, tag)`, `onTrackingEvent(view, event, tag)`.

## Showing an ad with Compose

```kotlin
RevJetTag(
    tag = tag,
    onClick = { url -> /* open it, or not */ },
    modifier = Modifier.height(250.dp),
    nativeContent = { response -> MyNativeAd(response) },
)
```

A native tag renders through `nativeContent`, which is handed the response as it arrives; a
web-based tag renders itself. `modifier` sizes the ad, unless the tag was created with
`updateDynamicHeight`, in which case the height the creative reports replaces the one given here.

Taps on `nativeContent` are reported as clicks, so a native ad tracks the same way whether the
application renders it with views or with Compose.

The composable's handlers are called alongside any the application set on the `Tag` itself, and
stop being called once the composable leaves the composition.

## Custom native ad views

A native tag hands the creative's own fields to the application, which decides what to draw. The
response carries the campaign's data as JSON, along with the size and context the ad server
reported:

```kotlin
@Composable
fun MyNativeAd(tag: Tag, response: NativeTagResponse) {
    // Which fields a creative carries is set by the campaign
    val fields = remember(response) { JSONObject(response.data).getJSONArray("personalization") }

    Column {
        Text(fields.getJSONObject(0).getString("value"))

        Button(onClick = { tag.goToLP() }) { Text("Learn more") }
    }
}
```

The ad's viewability is measured from the view that renders it, so nothing else is needed to track
a custom layout.

## Viewability

The SDK measures how much of an ad the user can actually see: the part of it on screen, less the
system bars, anything drawn over it and anything clipping it. A parent that is hidden or faded out
hides the ad, and so does the ad's activity being paused. Friendly obstructions are left out, and
dialogs over the activity are not detected.

- **Web-based tags** report it to the creative through MRAID 3.0's `exposureChange`: the percentage
  on screen and the visible rectangle, measured from the ad's top-left corner in density-independent
  pixels, or `null` when nothing is visible. A change reaches the creative within 200 ms, as MRAID
  3.0 requires. The creative decides what counts as viewable.
- **Native tags** fire the ad server's viewability pixels: once at least half of the ad has been
  on screen for a second in total, then at 7.5, 15, 22.5 and 30 seconds, and an invisibility pixel
  after 10 seconds out of sight.

## Errors

`onError` receives a `RevJetError` for what the SDK refused or could not do, and the underlying
exception — a network or parsing failure — for anything else.

| Error | When |
| --- | --- |
| `InvalidConfiguration` | The tag or key is blank, or a custom domain is not a host name. |
| `WebViewUnsupported` | The device's WebView cannot host a web-based tag safely. |
| `BlockedDestination` | A click, or a hop of its redirects, leads to an address that is not publicly routable, or to a URL that is not a destination at all, such as `javascript:`. |
| `NoRedirectURL` | The redirects of a click could not be followed. |
| `InvalidLPFormat` | A native ad's landing page is not a valid URL. |
| `NoClickURL` | The ad reported a click without a destination. |
| `TrackingEventDetailsUnavailable` | The ad reported a tracking event without its details. |

## Example integration

### Views

```kotlin
class AdActivity : Activity(), RevJetTagViewListener {

    private lateinit var tag: Tag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this)
        setContentView(container)

        tag = Tag(
            context = this,
            type = TagType.NATIVE,
            tag = "tag355022",
            key = "b37",
            debugMode = DebugMode.EMULATE,
        )

        RevJetTagView(this, tag, listener = this)
            .mount(container, MountPreset.Bottom())
    }

    override fun onDestroy() {
        tag.destroy()

        super.onDestroy()
    }

    override fun onClick(view: RevJetTagView, url: Uri, tag: Tag) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (error: ActivityNotFoundException) {
            // a destination outside the web that no app on the device handles
        }
    }

    override fun onNativeResponse(view: RevJetTagView, response: NativeTagResponse, tag: Tag): View? =
        MyAdView(this, response)
}
```

### Compose

```kotlin
@Composable
fun AdScreen() {
    val context = LocalContext.current
    val tag = remember {
        Tag(
            context = context,
            type = TagType.WEB_BASED,
            tag = "tag390451",
            key = "7c2",
            plcId = "266512461",
            debugMode = DebugMode.EMULATE,
            options = listOf(Option.Responsive(true)),
        )
    }

    DisposableEffect(tag) {
        onDispose { tag.destroy() }
    }

    RevJetTag(
        tag = tag,
        onClick = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url)) } },
        modifier = Modifier.fillMaxWidth().height(400.dp),
    )
}
```

The `sample` module holds both, against live test tags: a Compose screen with a native tag, a
responsive JavaScript tag and a preloaded one, and `ViewsActivity` with the same native tag built
from views.

## Threading

Everything public is used from the main thread, and state is published there. Requests and the
advertising identifier are read on background dispatchers inside the SDK.

## Running tests

```sh
./gradlew test                                   # JVM: options, requests, parsing, privacy, destinations
./gradlew :sdk:connectedDebugAndroidTest         # instrumented: the SDK against live creatives
./gradlew :sdk-compose:connectedDebugAndroidTest # instrumented: the composable
```

The instrumented tests need a running emulator or a device, and a network connection: they load
real creatives from `ads.revjet.com` and assert on what the tag script reports back. The JVM tests
run offline.

The tests that need a WebView able to host an ad skip themselves where it cannot, naming the
WebView version and what it is missing. That is the case on emulator images without Google Play,
whose WebView never updates; use an image with Google Play, or a device, to run them all.

Sources are formatted with [ktlint](https://github.com/JLLeitschuh/ktlint-gradle):

```sh
./gradlew ktlintFormat   # or ktlintCheck, which `build` runs
```

## Coming from the iOS SDK

The two SDKs take the same configuration and report the same events. What differs is what the
platforms differ in:

| iOS | Android |
| --- | --- |
| `Tag(type:tag:key:...)` | `Tag(context, type, tag, key, ...)`, which needs a `Context` |
| `.native`, `.webBased` | `TagType.NATIVE`, `TagType.WEB_BASED` |
| `@Published` state | `StateFlow` |
| — | `Tag.destroy()`: a WebView is not reclaimed on its own |
| `RevJetTagView(tag:delegate:)` | `RevJetTagView(context, tag, listener)` |
| `mountAdView(in:preset:)` with `ConstraintPreset` | `mount(into, preset)` with `MountPreset` |
| `RevJetTagViewDelegate` | `RevJetTagViewListener` |
| `revJetTagView(_:respondedWith:for:) -> UIView` | `onNativeResponse(view, response, tag): View?` |
| SwiftUI `RevJetTag` with `.withNativeTagContent { }` | Compose `RevJetTag(..., nativeContent = { })` |
| Options such as `.autoscale(true, mode: .bestFit)` | `Option.Autoscale(true, ScaleMode.BEST_FIT)` |
| App Tracking Transparency | the `AD_ID` permission, declared by the SDK |

## License

The SDK is available under the MIT license. See [`LICENSE`](LICENSE) for details.
