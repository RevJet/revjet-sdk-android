/*
 * RevJet Android SDK Sample
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.sample;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.BannerAdapter;
import com.revjet.android.sdk.InterstitialAdapter;
import com.revjet.android.sdk.TagListener;
import com.revjet.android.sdk.TagTargeting;
import com.revjet.android.sdk.TagView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MainActivity extends Activity implements TagListener {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        final TagView tagView = findViewById(R.id.tagView);
        tagView.setListener(this); // Optional
        tagView.setTargeting(targeting); // Optional
        tagView.setAdditionalInfo(additionalInfo);
        // tagView.setAutoRefreshEnabled(true); // Disabled by default
        tagView.loadAd();

        findViewById(R.id.interstitialButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("interstitialButton: onClick");
                Intent intent = new Intent(MainActivity.this, InterstitialActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.loadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("loadButton: onClick");
                findViewById(R.id.showButton).setEnabled(false);
                findViewById(R.id.fetchButton).setEnabled(false);
                tagView.loadAd();
            }
        });

        findViewById(R.id.fetchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("fetchButton: onClick");
                findViewById(R.id.loadButton).setEnabled(false);
                findViewById(R.id.showButton).setEnabled(false);
                tagView.fetchAd();
            }
        });

        findViewById(R.id.showButton).setEnabled(false);
        findViewById(R.id.showButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGGER.info("showButton: onClick");
                findViewById(R.id.loadButton).setEnabled(true);
                findViewById(R.id.fetchButton).setEnabled(true);
                tagView.showAd();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TagView tagView = findViewById(R.id.tagView);
        if (tagView != null) {
            tagView.destroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        TagView tagView = findViewById(R.id.tagView);
        if (tagView != null) {
            tagView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        TagView tagView = findViewById(R.id.tagView);
        if (tagView != null) {
            tagView.resume();
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
        findViewById(R.id.loadButton).setEnabled(true);
        findViewById(R.id.fetchButton).setEnabled(true);
        findViewById(R.id.showButton).setEnabled(false);
    }

    private void disableLoadButtons() {
        TagView tagView = (TagView) findViewById(R.id.tagView);
        findViewById(R.id.loadButton).setEnabled(!tagView.isAdReady());
        findViewById(R.id.fetchButton).setEnabled(!tagView.isAdReady());
        findViewById(R.id.showButton).setEnabled(tagView.isAdReady());
    }
}
