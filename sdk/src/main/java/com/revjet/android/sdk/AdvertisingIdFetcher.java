/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class AdvertisingIdFetcher implements Runnable {
    private static final String sAdvertisingIdClientClass = "com.google.android.gms.ads.identifier.AdvertisingIdClient";
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    @NonNull private final WeakReference<Context> mContextRef;
    @NonNull private final WeakReference<AdvertisingIdListener> mListenerRef;

    public AdvertisingIdFetcher(@NonNull Context context, @NonNull AdvertisingIdListener listener) {
        mContextRef = new WeakReference<>(context);
        mListenerRef = new WeakReference<>(listener);
    }

    public static void fetch(
      @NonNull final Context context,
      @NonNull final AdvertisingIdListener listener) {
        new Thread(new AdvertisingIdFetcher(context, listener)).start();
    }

    @Override
    public void run() {
        Class<?> advertisingIdClientClass = null;

        try {
            advertisingIdClientClass = Class.forName(sAdvertisingIdClientClass);
        } catch (Throwable ignored) {
        }

        Context context = mContextRef.get();

        if (advertisingIdClientClass == null || context == null) {
            onAdvertisingIdInfoFailed();
            return;
        }

        try {
            Object advertisingIdClientInfo = advertisingIdClientClass.getMethod("getAdvertisingIdInfo",
                    Context.class).invoke(null, context);
            if (advertisingIdClientInfo != null) {
                Class<?> infoClass = advertisingIdClientInfo.getClass();
                String advertisingId = (String) infoClass.getMethod("getId").invoke(advertisingIdClientInfo);
                Boolean isLimitAdTrackingEnabled = (Boolean) infoClass.getMethod("isLimitAdTrackingEnabled")
                    .invoke(advertisingIdClientInfo);

                onAdvertisingIdInfoCompleted(advertisingId, isLimitAdTrackingEnabled);
            }
        } catch (Throwable ignored) {
            onAdvertisingIdInfoFailed();
        }
    }

    private void onAdvertisingIdInfoCompleted(final String advertisingId, final Boolean isLimitAdTrackingEnabled) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                AdvertisingIdListener listener = mListenerRef.get();
                if (listener != null) {
                    listener.onAdvertisingIdInfoCompleted(advertisingId, isLimitAdTrackingEnabled);
                }
            }
        });
    }

    private void onAdvertisingIdInfoFailed() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                AdvertisingIdListener listener = mListenerRef.get();
                if (listener != null) {
                    listener.onAdvertisingIdInfoFailed();
                }
            }
        });
    }
}
