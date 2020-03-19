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
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.commons.Utils;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class InterstitialAdActivity extends AbstractAdActivity implements AdListener {
    public static final String CATEGORY_ADS = "com.revjet.category.ADS";

    @Nullable private AdView mAdView;
    @Nullable private FrameLayout mLayout;
    private ViewTreeObserver mObserver;

    public static void show(
      @NonNull final Context context,
      @Nullable final String baseUrl,
      @Nullable final String content,
      final boolean showCloseButton,
      final boolean isLandscape,
      @NonNull final String adWidth,
      @NonNull final String adHeight) {
        startActivity(context, InterstitialAdActivity.class, baseUrl, content, showCloseButton, isLandscape,
            adWidth, adHeight);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onEvent(ACTION_PRESENT_SCREEN);

        boolean showCloseButton = getIntent().getBooleanExtra(sShowCloseButton, false);

        mAdView = new AdView(this);
        mAdView.setBackgroundColor(Color.BLACK);
        mAdView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mAdView.setListener(this);
        mAdView.showCloseButton(showCloseButton);

        String baseUrl = getIntent().getStringExtra(sBaseUrl);
        String content = Utils.decompress(getIntent().getByteArrayExtra(sContent));

        if (content != null) {
            mAdView.loadHtmlWithBaseURL(baseUrl, content);
        }

        mLayout = new FrameLayout(this);
        mLayout.addView(mAdView);
        setContentView(mLayout);

        final Context context = this;
        final float adWidth = Float.parseFloat(getIntent().getStringExtra(sAdWidth));
        final float adHeight = Float.parseFloat(getIntent().getStringExtra(sAdHeight));

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float adWidthInPixels = Utils.dipsToPixels(adWidth, displayMetrics);
        final float adHeightInPixels = Utils.dipsToPixels(adHeight, displayMetrics);

        mObserver = mAdView.getViewTreeObserver();

        // In order to get the size of the mAdView we need to wait for first layout
        mObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean mLayouted = false;

            @Override
            public void onGlobalLayout() {
                if (mLayouted || mAdView == null) {
                    return;
                }

                AdWebView adWebView = mAdView.getAdWebView();
                if (adWebView != null) {
                    int viewWidthInPixels = mAdView.getWidth();
                    int viewHeightInPixels = mAdView.getHeight();
                    float adWebViewWidth =
                        adWidthInPixels > viewWidthInPixels ? viewWidthInPixels : adWidthInPixels;
                    float adWebViewHeight =
                        adHeightInPixels > viewHeightInPixels ? viewHeightInPixels : adHeightInPixels;

                    RelativeLayout.LayoutParams layoutParams =
                        new RelativeLayout.LayoutParams(Math.round(adWebViewWidth), Math.round(adWebViewHeight));
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                    adWebView.setLayoutParams(layoutParams);
                }

                mLayouted = true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        onDidClose();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mLayout != null && mAdView != null) {
            mObserver = null;

            mLayout.removeAllViews();
            mLayout = null;

            mAdView.destroy();
            mAdView = null;
        }

        onEvent(ACTION_DISMISS_SCREEN);

        super.onDestroy();
    }

    @Override
    public void onReceiveAd(@NonNull AdView view) {
    }

    @Override
    public void onShowAd(@NonNull AdView view) {
        LOGGER.info("onShowAd");
        onEvent(ACTION_SHOW_AD);
    }

    @Override
    public void onFailedToReceiveAd(AdView view, String errorMessage) {
        LOGGER.info("onFailedToReceiveAd: " + errorMessage);
        close();
    }

    @Override
    public void onLeaveApplication(@NonNull AdView view) {
        LOGGER.info("onLeaveApplication");
        onEvent(ACTION_LEAVE_APPLICATION);
    }

    @Override
    public void onPresentScreen(@NonNull AdView view) {
    }

    @Override
    public void onDismissScreen(@NonNull AdView view) {
        close();
    }

    @Override
    public void onClick() {
        LOGGER.info("onClick");
        onEvent(ACTION_CLICK);
    }

    @Override
    public void shouldOpenURL(@NonNull AdView view, @NonNull final String url,
        @NonNull final Function<Boolean, Void> callback) {
        shouldOpenURL(CATEGORY_ADS, url, callback);
    }

    @Override
    public void onClose() {
        LOGGER.info("onClose");
        close();
    }

    private void onEvent(String action) {
        onEvent(action, CATEGORY_ADS);
    }

    private void close() {
        onDidClose();
        finish();
    }

    private void onDidClose() {
        if (mAdView != null) {
            mAdView.webviewDidClose();
        }
    }
}
