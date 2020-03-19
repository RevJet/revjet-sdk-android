/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.commons.Utils;

import java.util.HashMap;
import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public abstract class AbstractAdActivity extends Activity {
    public static final String ACTION_CLICK = "com.revjet.action.CLICK";
    public static final String ACTION_SHOULD_OPEN_URL = "com.revjet.action.SHOULD_OPEN_URL";
    public static final String ACTION_RECEIVE_AD = "com.revjet.action.RECEIVE_AD";
    public static final String ACTION_SHOW_AD = "com.revjet.action.SHOW_AD";
    public static final String ACTION_FAILED_TO_RECEIVE_AD = "com.revjet.action.FAILED_TO_RECEIVE_AD";
    public static final String ACTION_PRESENT_SCREEN = "com.revjet.action.PRESENT_SCREEN";
    public static final String ACTION_DISMISS_SCREEN = "com.revjet.action.DISMISS_SCREEN";
    public static final String ACTION_LEAVE_APPLICATION = "com.revjet.action.LEAVE_APPLICATION";

    public static final String EXTRA_CLICK_URL = "com.revjet.ClickUrl";

    public static final Map<String, String> BROADCAST_ACTIONS;
    static {
        BROADCAST_ACTIONS = new HashMap<>();
        BROADCAST_ACTIONS.put(ACTION_CLICK, "onClickInterstitialAd");
        BROADCAST_ACTIONS.put(ACTION_RECEIVE_AD, "onReceiveInterstitialAd");
        BROADCAST_ACTIONS.put(ACTION_SHOW_AD, "onShowInterstitialAd");
        BROADCAST_ACTIONS.put(ACTION_FAILED_TO_RECEIVE_AD, "onFailedToReceiveInterstitialAd");
        BROADCAST_ACTIONS.put(ACTION_PRESENT_SCREEN, "onPresentInterstitialScreen");
        BROADCAST_ACTIONS.put(ACTION_DISMISS_SCREEN, "onDismissInterstitialScreen");
        BROADCAST_ACTIONS.put(ACTION_LEAVE_APPLICATION, "onLeaveApplicationInterstitial");
    }

    protected static final String sBaseUrl = "com.revjet.BaseUrl";
    protected static final String sContent = "com.revjet.Content";
    protected static final String sShowCloseButton = "com.revjet.ShowCloseButton";
    protected static final String sIsLandscape = "com.revjet.IsLandscape";
    protected static final String sActivityDestroyed = "com.revjet.ActivityDestroyed";
    protected static final String sAdWidth = "com.revjet.AdWidth";
    protected static final String sAdHeight = "com.revjet.AdHeight";

    protected static void startActivity(
      @NonNull final Context context,
      @NonNull final Class<?> activityClass,
      @Nullable final String baseUrl,
      @Nullable final String content,
      boolean isLandscape) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(sBaseUrl, baseUrl);
        intent.putExtra(sContent, Utils.compress(content));
        intent.putExtra(sIsLandscape, isLandscape);
        context.startActivity(intent);
    }

    protected static void startActivity(
      @NonNull final Context context,
      @NonNull final Class<?> activityClass,
      @Nullable final String baseUrl,
      @Nullable final String content,
      final boolean showCloseButton,
      final boolean isLandscape,
      @NonNull final String adWidth,
      @NonNull final String adHeight) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(sBaseUrl, baseUrl);
        intent.putExtra(sContent, Utils.compress(content));
        intent.putExtra(sShowCloseButton, showCloseButton);
        intent.putExtra(sIsLandscape, isLandscape);
        intent.putExtra(sAdWidth, adWidth);
        intent.putExtra(sAdHeight, adHeight);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean(sActivityDestroyed, false)) {
            finish();
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            boolean isLandscape = getIntent().getBooleanExtra(sIsLandscape, false);
            lockScreenToOrientation9(isLandscape);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(sActivityDestroyed, true);
        super.onSaveInstanceState(outState);
    }

    protected void lockScreenToOrientation9(boolean isLandscape) {
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        LOGGER.info("onDestroy");
        super.onDestroy();
    }

    protected void onEvent(String action, String category) {
        sendBroadcast(new Intent(action).addCategory(category));
    }

    public void shouldOpenURL(final String category, String url, final Function<Boolean, Void> callback) {
        Intent intent = new Intent(ACTION_SHOULD_OPEN_URL);
        intent.addCategory(category);
        intent.putExtra(EXTRA_CLICK_URL, url);

        sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String resultData = getResultData();
                if (resultData != null && resultData.length() > 0) {
                    callback.apply(Boolean.valueOf(resultData));
                } else {
                    callback.apply(true);
                }
            }
        }, null, Activity.RESULT_OK, null, null);
    }
}
