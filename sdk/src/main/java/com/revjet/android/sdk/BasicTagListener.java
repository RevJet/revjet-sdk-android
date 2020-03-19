/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.view.View;
import androidx.annotation.Nullable;

public class BasicTagListener implements TagListener {
    @Override
    public void onFailedToLoadTagView(TagView tagView) {
    }

    @Override
    public void onReceiveAd(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onShowAd(@Nullable BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onFailedToReceiveAd(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onPresentScreen(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onDismissScreen(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onLeaveApplication(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public boolean shouldOpenURL(BannerAdapter<?> adapter, View view, String url) {
        return true;
    }

    @Override
    public void onClick(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onClose(BannerAdapter<?> adapter, View view) {
    }

    @Override
    public void onDestroyCustomEventBannerAdapter(BannerAdapter<?> adapter, String name) {
    }

    @Override
    public void onReceiveInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onShowInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onFailedToReceiveInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onPresentInterstitialScreen(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onDismissInterstitialScreen(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onLeaveApplicationInterstitial(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public boolean shouldOpenURLInterstitial(@Nullable InterstitialAdapter<?> adapter, Object ad, String url) {
        return true;
    }

    @Override
    public void onClickInterstitialAd(InterstitialAdapter<?> adapter, Object ad) {
    }

    @Override
    public void onDestroyCustomEventInterstitialAdapter(InterstitialAdapter<?> adapter, String name) {
    }
}
