/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import java.util.Locale;
import java.util.Map;

public final class AdNetwork {
    private final String mNetworkType;
    private final AdType mAdType;
    private final TransitionAnimation mTransitionAnimation;

    private final String mImpressionUrl;
    private final String mNoBidUrl;
    private final String mClickUrl;

    private final int mRefreshRate;
    private final Map<String, ?> mParameters;

    private final AdNetworkController mAdNetworkController;

    public AdNetwork(String networkType, AdType adType, TransitionAnimation transitionAnimation,
            String impressionUrl, String noBidUrl, String clickUrl, Map<String, ?> parameters,
            int refreshRate) {
        mNetworkType = networkType.toUpperCase(Locale.US);
        mAdType = adType;
        mTransitionAnimation = transitionAnimation;
        mImpressionUrl = impressionUrl;
        mNoBidUrl = noBidUrl;
        mClickUrl = clickUrl;
        mParameters = parameters;
        mRefreshRate = refreshRate;
        mAdNetworkController = new AdNetworkController(this);
    }

    public String getNetworkType() {
        return mNetworkType;
    }

    public AdType getAdType() {
        return mAdType;
    }

    public TransitionAnimation getTransitionAnimation() {
        return mTransitionAnimation;
    }

    public String getImpressionUrl() {
        return mImpressionUrl;
    }

    public String getNoBidUrl() {
        return mNoBidUrl;
    }

    public String getClickUrl() {
        return mClickUrl;
    }

    public int getRefreshRate() {
        return mRefreshRate;
    }

    public Map<String, ?> getParameters() {
        return mParameters;
    }

    public AdNetworkController getAdNetworkController() {
        return mAdNetworkController;
    }

    @Override
    public String toString() {
        return "\nNetworkType = " + mNetworkType + "\n" + "AdType = " + mAdType + "\n"
                + "TransitionAnimation = " + mTransitionAnimation + "\n" + "RefreshRate = "
                + mRefreshRate + " secs\n" + "ImpressionUrl = " + mImpressionUrl
                + "\n" + "NoBidUrl = " + mNoBidUrl + "\n" + "ClickUrl = " + mClickUrl;
    }
}
