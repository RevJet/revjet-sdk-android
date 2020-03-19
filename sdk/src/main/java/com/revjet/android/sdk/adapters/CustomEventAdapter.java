/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.*;
import com.revjet.android.sdk.annotations.NetworkAdapter;
import com.revjet.android.sdk.exceptions.AdapterException;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

@NetworkAdapter(name = "Custom")
public final class CustomEventAdapter implements BannerAdapter<CustomEventParameters>,
        InterstitialAdapter<CustomEventParameters> {
    @Nullable private String mMethodName;
    @Nullable private BannerAdapterListener mListener;
    @Nullable private InterstitialAdapterListener mInterstitialListener;

    @Override
    public void getBannerAd(
      @Nullable final BannerAdapterListener listener,
      @NonNull final TagContext context,
      @NonNull final CustomEventParameters parameters)
        throws AdapterException {
        LOGGER.info("getBannerAd");

        mListener = listener;
        mMethodName = parameters.function;

        if (listener != null && mMethodName != null) {
            LOGGER.info("Function: " + mMethodName);
            listener.onReceiveCustomMethod(mMethodName, parameters.data);
        }
    }

    @Override
    public void getInterstitialAd(
      @Nullable final InterstitialAdapterListener listener,
      @NonNull final TagContext context,
      @NonNull final CustomEventParameters parameters)
        throws AdapterException {
        LOGGER.info("getInterstitialAd");

        mInterstitialListener = listener;
        mMethodName = parameters.function;

        if (listener != null && mMethodName != null) {
            LOGGER.info("Function: " + mMethodName);
            listener.onReceiveCustomMethod(mMethodName, parameters.data);
        }
    }

    @Override
    public void showInterstitial() {
    }

    @NonNull
    @Override
    public Class<CustomEventParameters> getParametersClass() {
        return CustomEventParameters.class;
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy");

        if (mListener != null && mMethodName != null) {
            mListener.onDestroyCustomEventAdapter(mMethodName);
        }

        mListener = null;

        if (mInterstitialListener != null && mMethodName != null) {
            mInterstitialListener.onDestroyCustomEventAdapter(mMethodName);
        }

        mInterstitialListener = null;
        mMethodName = null;
    }

    @Override
    public void onNotResponding() {
        LOGGER.info("onNotResponding");

        if (mListener != null) {
            mListener.onFailedToReceiveAd(null);
        }

        if (mInterstitialListener != null) {
            mInterstitialListener.onFailedToReceiveInterstitialAd(null);
        }
    }

    @Override
    public void onShowAd() {
    }
}
