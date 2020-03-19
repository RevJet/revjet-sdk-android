/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.Nullable;
import android.view.View;

public interface TagListener {
    void onFailedToLoadTagView(@Nullable TagView tagView);

    void onReceiveAd(@Nullable BannerAdapter<?> adapter, View view);

    void onShowAd(@Nullable BannerAdapter<?> adapter, View view);

    void onFailedToReceiveAd(@Nullable BannerAdapter<?> adapter, @Nullable View view);

    void onPresentScreen(@Nullable BannerAdapter<?> adapter, View view);

    void onDismissScreen(@Nullable BannerAdapter<?> adapter, View view);

    void onLeaveApplication(@Nullable BannerAdapter<?> adapter, View view);

    boolean shouldOpenURL(@Nullable BannerAdapter<?> adapter, View view, String url);

    void onClick(@Nullable BannerAdapter<?> adapter, View view);

    void onClose(@Nullable BannerAdapter<?> adapter, View view);

    void onDestroyCustomEventBannerAdapter(@Nullable BannerAdapter<?> adapter, String name);

    void onReceiveInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad);

    void onShowInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad);

    void onFailedToReceiveInterstitialAd(@Nullable InterstitialAdapter<?> adapter, @Nullable Object ad);

    void onPresentInterstitialScreen(@Nullable InterstitialAdapter<?> adapter, Object ad);

    void onDismissInterstitialScreen(@Nullable InterstitialAdapter<?> adapter, Object ad);

    void onLeaveApplicationInterstitial(@Nullable InterstitialAdapter<?> adapter, Object ad);

    boolean shouldOpenURLInterstitial(@Nullable InterstitialAdapter<?> adapter, Object ad, String url);

    void onClickInterstitialAd(@Nullable InterstitialAdapter<?> adapter, Object ad);

    void onDestroyCustomEventInterstitialAdapter(@Nullable InterstitialAdapter<?> adapter, String name);
}
