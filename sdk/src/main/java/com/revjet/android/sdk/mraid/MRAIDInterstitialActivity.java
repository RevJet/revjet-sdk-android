/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.ads.AbstractAdActivity;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.mraid.MRAIDController.MRAIDPlacementType;

import static com.revjet.android.sdk.ads.AdWebView.WEBVIEW_DID_CLOSE;
import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDInterstitialActivity extends AbstractAdActivity implements
        MRAIDViewListener, View.OnClickListener {
    public static final String CATEGORY_MRAID = "com.revjet.category.MRAID";

    private MRAIDView mAdView;
    private RelativeLayout mContainerView;
    private MRAIDCloseButton mCloseButton;

    public static void show(Context context, String baseUrl, String html, boolean isLandscape) {
        startActivity(context, MRAIDInterstitialActivity.class, baseUrl, html, isLandscape);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onEvent(ACTION_PRESENT_SCREEN);

        final int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
        mContainerView = new RelativeLayout(this);
        mContainerView.setLayoutParams(new RelativeLayout.LayoutParams(matchParent, matchParent));
        mContainerView.setBackgroundColor(Color.TRANSPARENT);

        mAdView = new MRAIDView(this, matchParent, matchParent, MRAIDPlacementType.INTERSTITIAL);
        mAdView.setListener(this);

        mContainerView.addView(mAdView);

        mCloseButton = new MRAIDCloseButton(this);
        mCloseButton.setOnClickListener(this);
        mContainerView.addView(mCloseButton, mCloseButton.getLayout());

        setContentView(mContainerView);

        String baseUrl = getIntent().getStringExtra(sBaseUrl);
        String html = Utils.decompress(getIntent().getByteArrayExtra(sContent));

        // Load the ad
        mAdView.loadHTML(baseUrl, html);
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }

        onEvent(ACTION_DISMISS_SCREEN);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        close(false);
        super.onBackPressed();
    }

    @Override
    public void onReceiveAd(MRAIDView view) {
        LOGGER.info("onReceiveAd");
    }

    @Override
    public void onShowAd(MRAIDView view) {
        LOGGER.info("onShowAd");
        onEvent(ACTION_SHOW_AD);
    }

    @Override
    public void onFailedToReceiveAd(MRAIDView view) {
        LOGGER.info("onFailedToReceiveAd");
        finish();
    }

    @Override
    public void onLeaveApplication(MRAIDView view) {
        onEvent(ACTION_LEAVE_APPLICATION);
        close(true);
    }

    @Override
    public void onClick(MRAIDView view) {
        onEvent(ACTION_CLICK);
    }

    @Override
    public void shouldOpenURL(final MRAIDView view, String url, final Function<Boolean, Void> callback) {
        shouldOpenURL(CATEGORY_MRAID, url, callback);
    }

    @Override
    public void onExpand(MRAIDView view) {
        LOGGER.warning("Impossible to expand interstitial ad");
    }

    @Override
    public void onClose(MRAIDView view) {
        LOGGER.info("onClose");
        close(true);
    }

    @Override
    public void onClick(View v) {
        LOGGER.info("onClick");
        close(true);
    }

    private void onEvent(String action) {
        onEvent(action, CATEGORY_MRAID);
    }

    private void close(boolean finishActivity) {
        if (mAdView != null) {
            mAdView.evaluateJavaScriptString(WEBVIEW_DID_CLOSE);
        }

        if (finishActivity) {
            finish();
        }
    }
}
