/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.AdapterException;

public interface InterstitialAdapter<T> extends Adapter<T> {
    void getInterstitialAd(
      @Nullable final InterstitialAdapterListener listener,
      @NonNull final TagContext tagContext,
      @NonNull final T parameters)
        throws AdapterException;

    void showInterstitial();
}
