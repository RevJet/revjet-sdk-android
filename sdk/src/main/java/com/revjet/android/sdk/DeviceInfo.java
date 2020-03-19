/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DeviceInfo implements AdvertisingIdListener {
    private static final String sDevicePlatformKey = "_device_platform";
    private static final String sDeviceKey = "device";
    private static final String sDeviceTypeKey = "dtype";
    private static final String sDeviceModelKey = "_device_model";
    private static final String sOsBuildKey = "_os_build";
    private static final String sOsApiKey = "_os_api";
    private static final String sOsVersionKey = "osver";
    private static final String sLocaleKey = "locale";
    private static final String sCountryKey = "country";
    private static final String sLanguageKey = "language";
    private static final String sPackageKey = "bundleid";
    private static final String sConnectionTypeKey = "contype";
    private static final String sDspWidthKey = "_device_w";
    private static final String sDspHeightKey = "_device_h";
    private static final String sDoNotTrackKey = "dnt";
    private static final String sAdvertisingIdKey = "_aaid";

    private static final String sDevicePlatform = "InApp";
    private static final String sDevice = Build.DEVICE;
    private static final String sDeviceType = "Android";
    private static final String sDeviceModel = Build.MODEL + " (" + Build.PRODUCT + ")";
    private static final String sOsBuild = Build.VERSION.INCREMENTAL;
    private static final String sOsApi = String.valueOf(Build.VERSION.SDK_INT);
    private static final String sOsVersion = Build.VERSION.RELEASE;
    private static final String sLocale = Locale.getDefault().toString();
    private static final String sCountry = Locale.getDefault().getCountry();
    private static final String sLanguage = Locale.getDefault().getLanguage();

    private static final String sUnknown = "unknown";

    private static String sDspWidth = null;
    private static String sDspHeight = null;

    private static boolean sAdvertisingIdInfoCompleted = false;
    private static String sAdvertisingId = sUnknown;
    private static boolean sLimitAdTrackingEnabled = false;

    @NonNull private final WeakReference<Context> mContextRef;
    @Nullable private Runnable mOnAdvertisingIdInfoCompleted;

    public DeviceInfo(@NonNull Context context) {
        mContextRef = new WeakReference<>(context);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (displayMetrics != null) {
            if (sDspWidth == null) {
                sDspWidth = String.valueOf(displayMetrics.widthPixels);
            }
            if (sDspHeight == null) {
                sDspHeight = String.valueOf(displayMetrics.heightPixels);
            }
        }

        AdvertisingIdFetcher.fetch(context, this);
    }

    public void setOnAdvertisingIdInfoCompleted(@NonNull final Runnable onAdvertisingIdInfoCompleted) {
        if (sAdvertisingIdInfoCompleted) {
            mOnAdvertisingIdInfoCompleted = null;
            onAdvertisingIdInfoCompleted.run();
        } else {
            mOnAdvertisingIdInfoCompleted = onAdvertisingIdInfoCompleted;
        }
    }

    private String getPackage() {
        Context context = mContextRef.get();
        return (context != null ? context.getPackageName() : sUnknown);
    }

    @SuppressLint("MissingPermission")
    private String getConnectionType() {
        NetworkInfo networkInfo = null;

        // Obtain an NetworkInfo instance
        Context context = mContextRef.get();
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                networkInfo = manager.getActiveNetworkInfo();
            }
        }

        String connectionType;
        if (networkInfo != null) {
            connectionType = (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) ? "wifi"
                    : "carrier";
        } else {
            connectionType = sUnknown;
        }

        return connectionType;
    }

    public boolean isAdvertisingIdInfoCompleted() {
        return sAdvertisingIdInfoCompleted;
    }

    public Map<String, String> toMap() {
        Map<String, String> deviceInfoMap = new HashMap<>();

        deviceInfoMap.put(sDevicePlatformKey, sDevicePlatform);
        deviceInfoMap.put(sDeviceKey, sDevice);
        deviceInfoMap.put(sDeviceTypeKey, sDeviceType);
        deviceInfoMap.put(sDeviceModelKey, sDeviceModel);
        deviceInfoMap.put(sOsBuildKey, sOsBuild);
        deviceInfoMap.put(sOsApiKey, sOsApi);
        deviceInfoMap.put(sOsVersionKey, sOsVersion);
        deviceInfoMap.put(sLocaleKey, sLocale);
        deviceInfoMap.put(sCountryKey, sCountry);
        deviceInfoMap.put(sLanguageKey, sLanguage);
        deviceInfoMap.put(sPackageKey, getPackage());
        deviceInfoMap.put(sConnectionTypeKey, getConnectionType());
        deviceInfoMap.put(sDspWidthKey, sDspWidth);
        deviceInfoMap.put(sDspHeightKey, sDspHeight);
        deviceInfoMap.put(sDoNotTrackKey, sLimitAdTrackingEnabled ? "1" : "0");

        if (sAdvertisingId != null && !sAdvertisingId.equals(sUnknown)) {
            deviceInfoMap.put(sAdvertisingIdKey, sAdvertisingId);
        } /*else {
            deviceInfoMap.put(sAndroidIdKey, sRawAndroidId);
        }*/

        return deviceInfoMap;
    }

    @Override
    public void onAdvertisingIdInfoCompleted(String advertisingId, Boolean isLimitAdTrackingEnabled) {
        sAdvertisingId = advertisingId;
        sLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
        sAdvertisingIdInfoCompleted = true;

        if (mOnAdvertisingIdInfoCompleted != null) {
            mOnAdvertisingIdInfoCompleted.run();
        }
    }

    @Override
    public void onAdvertisingIdInfoFailed() {
        sAdvertisingIdInfoCompleted = true;

        if (mOnAdvertisingIdInfoCompleted != null) {
            mOnAdvertisingIdInfoCompleted.run();
        }
    }
}
