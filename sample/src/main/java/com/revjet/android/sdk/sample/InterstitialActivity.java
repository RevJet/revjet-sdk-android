/*
 * RevJet Android SDK Sample
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.sample;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.BannerAdapter;
import com.revjet.android.sdk.InterstitialAdapter;
import com.revjet.android.sdk.InterstitialTag;
import com.revjet.android.sdk.TagListener;
import com.revjet.android.sdk.TagTargeting;
import com.revjet.android.sdk.TagView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class InterstitialActivity extends Activity implements TagListener {
    private InterstitialTag mInterstitialTag;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interstitial);

//        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
//            WebView.setWebContentsDebuggingEnabled(true);
//        }

        Map<String, String> additionalInfo = new HashMap<>();
        additionalInfo.put("storeId", "12345");

        TagTargeting targeting = new TagTargeting();
        targeting.setAreaCode("925");
        targeting.setCity("Walnut Creek");
        targeting.setGender(TagTargeting.Gender.MALE);
        targeting.setMetro("807");
        targeting.setRegion("CA");
        targeting.setZip("94598");

        targeting.setLatitude("37.9136962890625");
        targeting.setLongitude("-122.01170349121094");

        LOGGER.setLevel(Level.INFO); // Level.OFF
        LOGGER.info("onCreate");

        // Initialize a new slot
        mInterstitialTag = new InterstitialTag(this);
        mInterstitialTag.setTagUrl("https://cdn.revjet.com/~cdn/Ads/ad_shared/test/thd/tag-function.html");
        mInterstitialTag.setListener(this); // Optional
        mInterstitialTag.setTargeting(targeting); // Optional
        mInterstitialTag.setAdditionalInfo(additionalInfo);
        mInterstitialTag.loadAd();

        findViewById(R.id.loadIntButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("loadButton: onClick");
                findViewById(R.id.showIntButton).setEnabled(false);
                findViewById(R.id.fetchIntButton).setEnabled(false);
                mInterstitialTag.loadAd();
            }
        });

        findViewById(R.id.fetchIntButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("fetchButton: onClick");
                findViewById(R.id.loadIntButton).setEnabled(false);
                findViewById(R.id.showIntButton).setEnabled(false);
                mInterstitialTag.fetchAd();
            }
        });

        findViewById(R.id.showIntButton).setEnabled(false);
        findViewById(R.id.showIntButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("showButton: onClick");
                findViewById(R.id.loadIntButton).setEnabled(true);
                findViewById(R.id.fetchIntButton).setEnabled(true);
                mInterstitialTag.showAd();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mInterstitialTag != null) {
            mInterstitialTag.destroy();
        }
    }

    @Override
    public void onFailedToLoadTagView(TagView tagView) {
        Toast.makeText(this, "onFailedToLoadTagView", Toast.LENGTH_SHORT).show();
        enableLoadButtons();
    }

    @Override
    public void onReceiveAd(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onReceiveAd", Toast.LENGTH_SHORT).show();
        disableLoadButtons();
    }

    @Override
    public void onShowAd(@Nullable BannerAdapter<?> adapter, View view) {
        LOGGER.info("onShowAd");
    }

    @Override
    public void onFailedToReceiveAd(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onFailedToReceiveAd", Toast.LENGTH_SHORT).show();
        enableLoadButtons();
    }

    @Override
    public void onPresentScreen(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onPresentScreen", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDismissScreen(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onDismissScreen", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeaveApplication(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onLeaveApplication", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean shouldOpenURL(@Nullable BannerAdapter<?> adapter, View view, String url) {
        LOGGER.info("shouldOpenURL: " + url);

        return true;
    }

    @Override
    public void onClick(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onClick", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClose(BannerAdapter<?> adapter, View view) {
        Toast.makeText(this, "onClose", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceiveInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onReceiveInterstitialAd", Toast.LENGTH_SHORT).show();
        disableLoadButtons();
    }

    @Override
    public void onShowInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad) {
        LOGGER.info("onShowInterstitialAd");
    }

    @Override
    public void onFailedToReceiveInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onFailedToReceiveInterstitialAd", Toast.LENGTH_SHORT).show();
        enableLoadButtons();
    }

    @Override
    public void onPresentInterstitialScreen(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onPresentInterstitialScreen", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDismissInterstitialScreen(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onDismissInterstitialScreen", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeaveApplicationInterstitial(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onLeaveApplicationInterstitial", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean shouldOpenURLInterstitial(@Nullable InterstitialAdapter<?> adapter, Object ad, String url) {
        LOGGER.info("shouldOpenURLInterstitial: " + url);

        return true;
    }

    @Override
    public void onClickInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
        Toast.makeText(this, "onClickInterstitialAd", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyCustomEventBannerAdapter(BannerAdapter<?> adapter, String name) {
        Toast.makeText(this, "onDestroyCustomEventBannerAdapter", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyCustomEventInterstitialAdapter(InterstitialAdapter<?> adapter, String name) {
        Toast.makeText(this, "onDestroyCustomEventInterstitialAdapter", Toast.LENGTH_SHORT).show();
    }

    private void enableLoadButtons() {
        findViewById(R.id.loadIntButton).setEnabled(true);
        findViewById(R.id.fetchIntButton).setEnabled(true);
        findViewById(R.id.showIntButton).setEnabled(false);
    }

    private void disableLoadButtons() {
        findViewById(R.id.loadIntButton).setEnabled(!mInterstitialTag.isAdReady());
        findViewById(R.id.fetchIntButton).setEnabled(!mInterstitialTag.isAdReady());
        findViewById(R.id.showIntButton).setEnabled(mInterstitialTag.isAdReady());
    }
}
