/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import java.util.Map;

public final class AdNetworkBuilder {
    private String mNetworkType;
    private AdType mAdType;
    private TransitionAnimation mTransitionAnimation;

    private String mImpressionUrl;
    private String mNoBidUrl;
    private String mClickUrl;

    private int mRefreshRate;
    private Map<String, ?> mParameters;

    public AdNetwork build() {
        return new AdNetwork(mNetworkType, mAdType, mTransitionAnimation, mImpressionUrl,
                mNoBidUrl, mClickUrl, mParameters, mRefreshRate);
    }

    public AdNetworkBuilder networkType(String networkType) {
        mNetworkType = networkType;
        return this;
    }

    public AdNetworkBuilder adType(AdType adType) {
        mAdType = adType;
        return this;
    }

    public AdNetworkBuilder transitionAnimation(TransitionAnimation transitionAnimation) {
        mTransitionAnimation = transitionAnimation;
        return this;
    }

    public AdNetworkBuilder impressionUrl(String impressionUrl) {
        mImpressionUrl = impressionUrl;
        return this;
    }

    public AdNetworkBuilder noBidUrl(String noBidUrl) {
        mNoBidUrl = noBidUrl;
        return this;
    }

    public AdNetworkBuilder clickUrl(String clickUrl) {
        mClickUrl = clickUrl;
        return this;
    }

    public AdNetworkBuilder parameters(Map<String, ?> parameters) {
        mParameters = parameters;
        return this;
    }

    public AdNetworkBuilder refreshRate(int refreshRate) {
        mRefreshRate = refreshRate;
        return this;
    }
}
