/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.DeviceInfo;
import com.revjet.android.sdk.IntegrationType;
import com.revjet.android.sdk.TagController;
import com.revjet.android.sdk.TagTargeting;
import com.revjet.android.sdk.mraid.events.MRAIDNativeEventFactory;
import com.revjet.android.sdk.vast.representation.VASTRepresentationUtilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Environment.MEDIA_MOUNTED;
import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public final class Utils {
    private static final String sJsonpKey = "jsonp";
    private static final String sSdkVersionKey = "_imp_libver";
    private static final String sSdkTypeKey = "_imp_libtype";
    private static final String sMraidVersionKey = "_imp_banner_api";
    private static final String sVastVersionKey = "_video_type";
    private static final String sVideoMimeTypesKey = "_video_mime_types";
    private static final String sVideoLinearityKey = "_video_linearity";
    private static final String sVideoMindurKey = "_video_mindur";
    private static final String sImpSrcKey = "_imp_src";

    private static final AtomicLong sNextGeneratedId = new AtomicLong(1);
    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

//    private static final Pattern sAdSizeUrlPattern = Pattern.compile("ad_size=([0-9]+)x([0-9]+)",
//            Pattern.CASE_INSENSITIVE);
//    private static final Pattern sAdSizeHtmlPattern = Pattern.compile(
//                    "container[\\s]*\\{[\\s]*width[\\s]*:[\\s]*([0-9]+)px[\\s]*;[\\s]*height[\\s]*:[\\s]*([0-9]+)px",
//                    Pattern.CASE_INSENSITIVE);

    @NonNull
    public static Uri getUriWithQueryParams(@Nullable String url, @Nullable Map<String, String> queryParams) {
        String uriString = url;
        if (uriString == null) {
            uriString = "about:blank";
        }

        Uri.Builder uriBuilder = Uri.parse(uriString).buildUpon();
        if (queryParams != null) {
            for (String key : queryParams.keySet()) {
                String val = queryParams.get(key);
                if (key != null && val != null) {
                    uriBuilder.appendQueryParameter(key, val);
                }
            }
        }

        return uriBuilder.build();
    }

    @NonNull
    public static Map<String, String> getMapWithQueryParams(
      @Nullable TagTargeting targeting,
      @Nullable DeviceInfo deviceInfo,
      @Nullable IntegrationType integrationType,
      @Nullable Context context) {
        Map<String, String> params = new HashMap<>();

        params.put(sJsonpKey, "REVJET_SDK");
        params.put(sSdkVersionKey, TagController.sSdkVersion);
        params.put(sSdkTypeKey, TagController.sSdkType);
        params.put(sMraidVersionKey, TagController.sMraidVersion);

        params.put(sVastVersionKey, VASTRepresentationUtilities.VAST_VERSION);

        List<String> mimes = new ArrayList<>();
        for (String mime : VASTRepresentationUtilities.VIDEO_TYPES) {
            String mimeSuffix = mime.substring(mime.lastIndexOf('/') + 1);
            if (null != mimeSuffix) {
                mimes.add(mimeSuffix);
            }
        }

        params.put(sVideoMimeTypesKey, TextUtils.join(",", mimes.toArray()));
        params.put(sVideoLinearityKey, "0");
        params.put(sVideoMindurKey, "1");

        if (integrationType != null) {
            params.put(sImpSrcKey, integrationType.getName());
        }

        if (deviceInfo != null) {
            params.putAll(deviceInfo.toMap());
        }

        if (targeting != null) {
            params.putAll(targeting.toMap(context));
        }

        return params;
    }

    public static void consumeStream(@NonNull InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        for (;;) {
            final int res = inputStream.read(buffer);
            if (res == -1) {
                return;
            }
        }
    }

    private static class HttpGetUrlTask implements Runnable {
        @NonNull private final String mUrl;
        @Nullable private final String mUserAgent;

        public HttpGetUrlTask(@NonNull String url, @Nullable String userAgent) {
            mUrl = url;
            mUserAgent = userAgent;
        }

        public void run() {
            HttpURLConnection urlConnection = null;

            try {
                urlConnection = CustomURLConnection.openConnection(mUrl);
                urlConnection.setConnectTimeout(5 * 1000);
                urlConnection.setReadTimeout(5 * 1000);

                if (mUserAgent != null) {
                    urlConnection.setRequestProperty("User-Agent", mUserAgent);
                }

                consumeStream(urlConnection.getInputStream());
            } catch (Exception ignored) {
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    }

    public static void httpGetUrl(@Nullable String url, @Nullable String userAgent) {
        if (url != null && url.length() > 0) {
            THREAD_POOL.execute(new HttpGetUrlTask(url, userAgent));
        }
    }

    public static void httpGetUrls(@Nullable List<String> urls, @Nullable String userAgent) {
        if (null != urls && urls.size() > 0) {
            for (String url : urls) {
                httpGetUrl(url, userAgent);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Nullable
    public static Location getLocation(@NonNull Context context) {
        Location location = null;

        LocationManager manager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (manager != null) {
            try {
                location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {
            }

            if (location == null) {
                try {
                    location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (Exception ignored) {
                }
            }
        }

        return location;
    }

//    public static boolean isConnectionAvailable(@NonNull Context context) {
//        boolean connectionAvailable = false;
//
//        ConnectivityManager manager = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (manager != null) {
//            NetworkInfo info = manager.getActiveNetworkInfo();
//            if (info != null) {
//                connectionAvailable = info.isConnected();
//            }
//        }
//
//        return connectionAvailable;
//    }

    public static float pixelsToDips(float pixels, @NonNull DisplayMetrics dm) {
        float density = dm.density;
        if (density > 0) {
            return pixels / density;
        } else {
            return pixels;
        }
    }

    public static float dipsToPixels(float dips, @NonNull DisplayMetrics dm) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, dm);
    }

//    @Nullable
//    public static int[] getAdSizeFromHtml(@Nullable String html) {
//        int[] adSize = null;
//
//        if (html != null) {
//            Matcher matcher = sAdSizeHtmlPattern.matcher(html);
//            if (matcher.find()) {
//                adSize = new int[] {
//                        Integer.valueOf(matcher.group(1)),
//                        Integer.valueOf(matcher.group(2))
//                };
//            }
//        }
//
//        return adSize;
//    }

    @Nullable
    public static byte[] compress(@Nullable final String data) {
        if (data == null) {
            return null;
        }

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream outputStream = new GZIPOutputStream(byteOutputStream)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
        }

        return byteOutputStream.toByteArray();
    }

    @Nullable
    public static String decompress(@Nullable byte[] data) {
        if (data == null) {
            return null;
        }

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);

        String rawData = null;
        GZIPInputStream inputStream = null;

        try {
            inputStream = new GZIPInputStream(byteInputStream);

            int bytesRead;
            byte[] buffer = new byte[4096];

            while ((bytesRead = inputStream.read(buffer)) > 0) {
                byteOutputStream.write(buffer, 0, bytesRead);
            }

            rawData = byteOutputStream.toString("UTF-8");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }

        return rawData;
    }

    public static boolean isSmsAvailable(@NonNull Context context) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("sms:"));

        return deviceCanHandleIntent(context, smsIntent);
    }

    public static boolean isTelAvailable(@NonNull Context context) {
        Intent telIntent = new Intent(Intent.ACTION_DIAL);
        telIntent.setData(Uri.parse("tel:"));

        return deviceCanHandleIntent(context, telIntent);
    }

    public static boolean isCalendarAvailable(@NonNull Context context) {
        Intent calendarIntent = new Intent(Intent.ACTION_INSERT)
            .setType(MRAIDNativeEventFactory.ANDROID_CALENDAR_CONTENT_TYPE);

        return Integer.valueOf(android.os.Build.VERSION.SDK) >= 14
                && deviceCanHandleIntent(context, calendarIntent);
    }

    public static boolean isInlineVideoAvailable(@NonNull Context context) {
        Intent mraidVideoIntent = new Intent(Intent.ACTION_VIEW);

        return deviceCanHandleIntent(context, mraidVideoIntent);
    }

    public static boolean isStorePictureAvailable(@NonNull Context context) {
        return MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                && context.checkCallingOrSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean deviceCanHandleIntent(@NonNull final Context context, @NonNull final Intent intent) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            return !activities.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static void copyContent(@Nullable final InputStream inputStream, @Nullable final OutputStream outputStream)
        throws IOException {
        if (inputStream == null || outputStream == null) {
            throw new IOException("Unable to copy from or to a null stream.");
        }

        byte[] buffer = new byte[16384];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    public static void closeStream(@Nullable Closeable stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (IOException e) {
            // Unable to close the stream
        }
    }

    public static long generateUniqueId() {
        for (;;) {
            final long result = sNextGeneratedId.get();
            long newValue = result + 1;
            if (newValue > Long.MAX_VALUE - 1) {
                newValue = 1;
            }
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static boolean isURLValid(@NonNull String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    public static boolean bitMaskContainsFlag(final int bitMask, final int flag) {
        return (bitMask & flag) != 0;
    }

    public static int getScreenOrientation(@Nullable final Activity activity) {
        if (null == activity) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        final int width = displayMetrics.widthPixels;
        final int height = displayMetrics.heightPixels;

        final boolean isPortrait =
                (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)) &&
                        height > width) ||
                        (((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) &&
                                width > height);

        if (isPortrait) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_180:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                default:
                    LOGGER.info("Unknown screen orientation. Defaulting to portrait.");
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_180:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                default:
                    LOGGER.info("Unknown screen orientation. Defaulting to landscape.");
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
        }
    }
}
