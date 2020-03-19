/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface InterstitialAdapterListener extends AdapterListener {
    void onReceiveInterstitialAd(@NonNull Object ad);

    void onShowInterstitialAd(@NonNull Object ad);

    void onFailedToReceiveInterstitialAd(@Nullable Object ad);

    void onPresentInterstitialScreen(@NonNull Object ad);

    void onDismissInterstitialScreen(@NonNull Object ad);

    void onLeaveApplicationInterstitial(@NonNull Object ad);

    void onClickInterstitialAd(@NonNull Object ad);

    boolean shouldOpenURLInterstitial(@NonNull Object ad, @NonNull String url);
}
