/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.BannerAdapter;
import com.revjet.android.sdk.BannerAdapterListener;
import com.revjet.android.sdk.TagContext;
import com.revjet.android.sdk.ads.AdSize;
import com.revjet.android.sdk.annotations.NetworkAdapter;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.exceptions.AdapterException;
import com.revjet.android.sdk.mraid.MRAIDController.MRAIDPlacementType;
import com.revjet.android.sdk.mraid.MRAIDView;
import com.revjet.android.sdk.mraid.MRAIDViewListener;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

@NetworkAdapter(name = "MRAID")
public class MRAIDAdapter implements BannerAdapter<RevJetParameters>, MRAIDViewListener {
    @Nullable private MRAIDView mAdView;
    @Nullable private BannerAdapterListener mListener;

    @Override
    public void getBannerAd(
      @Nullable final BannerAdapterListener listener,
      @NonNull final TagContext tagContext,
      @NonNull final RevJetParameters parameters)
        throws AdapterException {
        LOGGER.info("getBannerAd");

        mListener = listener;

        Context context = tagContext.getContext();
        if (!(context instanceof Activity)) {
            throw new AdapterException("Context should be an instance of activity");
        }

        AdSize adSize = AdSize.findAdSizeThatFits(tagContext.getWidthInDips(),
                tagContext.getHeightInDips());

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float width = Utils.dipsToPixels(adSize.getWidth(), dm);
        float height = Utils.dipsToPixels(adSize.getHeight(), dm);

        String baseUrl = tagContext.getTagUrl();
        if (baseUrl != null) {
            mAdView = new MRAIDView((Activity) context, width, height, MRAIDPlacementType.INLINE);
            mAdView.setListener(this);
            mAdView.loadHTML(baseUrl, parameters.content);
        }
    }

    @NonNull
    @Override
    public Class<RevJetParameters> getParametersClass() {
        return RevJetParameters.class;
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy");

        mListener = null;

        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }

    @Override
    public void onNotResponding() {
        LOGGER.info("onNotResponding");

        if (mListener != null) {
            mListener.onFailedToReceiveAd(mAdView);
        }
    }

    @Override
    public void onShowAd() {
    }

    @Override
    public void onReceiveAd(MRAIDView view) {
        LOGGER.info("onReceiveAd");

        if (mListener != null) {
            mListener.onReceiveAd(view);
        }
    }

    @Override
    public void onShowAd(MRAIDView view) {
        LOGGER.info("onShowAd");

        if (mListener != null) {
            mListener.onShowAd(view);
        }
    }

    @Override
    public void onFailedToReceiveAd(MRAIDView view) {
        LOGGER.info("onFailedToReceiveAd");

        if (mListener != null) {
            mListener.onFailedToReceiveAd(view);
        }
    }

    @Override
    public void onLeaveApplication(MRAIDView view) {
        LOGGER.info("onLeaveApplication");

        if (mListener != null) {
            mListener.onLeaveApplication(view);
        }
    }

    @Override
    public void onClick(MRAIDView view) {
        LOGGER.info("onClick");

        if (mListener != null) {
            mListener.onClick(view);
        }
    }

    @Override
    public void shouldOpenURL(MRAIDView view, String url, Function<Boolean, Void> callback) {
        if (mListener != null) {
            callback.apply(mListener.shouldOpenURL(view, url));
        } else {
            callback.apply(true);
        }
    }

    @Override
    public void onExpand(MRAIDView view) {
        LOGGER.info("onExpand");

        if (mListener != null) {
            mListener.onPresentScreen(view);
        }
    }

    @Override
    public void onClose(MRAIDView view) {
        LOGGER.info("onClose");

        if (mListener != null) {
            mListener.onDismissScreen(view);
        }
    }
}
