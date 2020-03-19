# RevJet Android SDK

## License

RevJetSDK is available under the MIT license. See the LICENSE file for more info.

## Documentation

### Available macros

* Bundle ID: ```{bundleid}```
* Connection Type: ```{contype}```
* DNT Flag: ```{dnt}```
* Device Language: ```{language}```
* Device Model: ```{device}```
* Device Type: ```{dtype}```
* Area Code: ```{areacode}```
* City Name: ```{city}```
* Country: ```{country}```
* Latitude: ```{lat}```
* Longitude: ```{long}```
* Metro Code: ```{metro}```
* Region: ```{region}```
* Zip Code: ```{zip}```
* Android AID: ```{_aaid}```
* Video Ad Type format: ```{_video_type}```
* Linearity: ```{_video_linearity}```
* MIME Types: ```{_video_mime_types}```
* Min Duration: ```{_video_mindur}```
* OS Version: ```{osver}```
* Site Name: ```{appname}```
* User Gender: ```{gender}```
* User locale: ```{locale}```
* OS Build Version: ```{_os_build}```
* OS Api Version: ```{_os_api}```
* Screen width: ```{_device_w}```
* Screen height: ```{_device_h}```

### Available SDK parameters

_(SDK parameters are defined in ```<meta>``` tags.)_

1. ```<meta name="Parameter-AdType" content="Banner">```

   Ad Type. "Banner" or "Interstitial". Default: "Banner".

* ```<meta name="Parameter-NetworkType" content="RJ">```

  Adapter/Network type. "RJ" or "MRAID". Default: "RJ".

* ```<meta name="Parameter-WIDTH" content="320">```

  The width of the banner. Can be omitted.

* ```<meta name="Parameter-HEIGHT" content="64">```

  The height of the banner. Can be omitted.

### Supported ad sizes

* 320x50
* 320x64
* 728x90
* 300x250
* 160x600
* 480x320
* 320x480
* 300x600
* 568x320
* 320x568
* 970x250
* 667x375
* 375x667
* 736x414
* 414x736
* 768x1024
* 1024x768
* 1280x720
* 1366x1024
* 1024x1366
* 1920x1080

To support additional ad sizes you need to add the new size into ```sAllSizes``` list (```AdSize``` class)
in [AdSize.java](sdk/src/main/java/com/revjet/android/sdk/ads/AdSize.java) file.
 
### Overriding LP URL handling

By default, any LP URLs will be opened in the external browser.
It is possible to override this behaviour by overriding following methods:
```java
boolean shouldOpenURL(@Nullable BannerAdapter<?> adapter, View view, String url);
```

and/or (for interstitial ads):

```java
boolean shouldOpenURLInterstitial(@Nullable InterstitialAdapter<?> adapter, Object ad, String url);
```

Example

```java
@Override
public boolean shouldOpenURL(@Nullable BannerAdapter<?> adapter, View view, String url) {
    // Here we test “url” to some value ...
 
    // Return true to open in the external browser
    // Return false to cancel opening the url in the external browser (here we can show something in-app)
    return true;
}
```

### Pre-cache ads

It's possible to pre-cache the ad first instead of rendering it immediately.
You should call ```void fetchAd()``` method to load the ad and then call ```void showAd()``` method to render
the ad when it's necessary (see [TagView.java](sdk/src/main/java/com/revjet/android/sdk/TagView.java)).

### Load and View events

The SDK can inform you when the ad becomes visible to the end user.
Implement
```java
void onShowAd(@Nullable BannerAdapter<?> adapter, View view)
```
and/or
```java
void onShowInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad)
```
method from [TagListener](sdk/src/main/java/com/revjet/android/sdk/TagListener.java).

Also, if you need to execute any special behaviour when the ad is loaded but not rendered yet (like fire load pixel)
implement
```java
void onReceiveAd(BannerAdapter<?> adapter, View view)
```
and/or
```java
void onReceiveInterstitialAd(InterstitialAdapter<?> adapter, Object ad)
```
method (see [TagListener](sdk/src/main/java/com/revjet/android/sdk/TagListener.java)).
