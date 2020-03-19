/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.BannerAdapter;
import com.revjet.android.sdk.BannerAdapterListener;
import com.revjet.android.sdk.TagContext;
import com.revjet.android.sdk.ads.AdListener;
import com.revjet.android.sdk.ads.AdSize;
import com.revjet.android.sdk.ads.AdView;
import com.revjet.android.sdk.annotations.NetworkAdapter;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.exceptions.AdapterException;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

@NetworkAdapter(name = "RJ")
public class RevJetAdapter implements BannerAdapter<RevJetParameters>, AdListener {
    @Nullable private AdView mAdView;
    @Nullable private FrameLayout mContainerView;
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
        if (context == null) {
            throw new AdapterException("Context can't be null");
        }

        if ("nobid".equalsIgnoreCase(parameters.content)) {
            if (mListener != null) {
                mListener.onFailedToReceiveAd(null);
            }
        } else {
            mAdView = createAdView(context, tagContext, parameters);
            mContainerView = createContainerView(context, tagContext, mAdView);

            String baseUrl = tagContext.getTagUrl();
            if (baseUrl != null) {
                mAdView.loadHtmlWithBaseURL(baseUrl, parameters.content);
            }
        }
    }

    @NonNull
    private AdView createAdView(@NonNull final Context context, @NonNull final TagContext tagContext,
      @NonNull final RevJetParameters parameters) {
        AdView adView = new AdView(context);
        adView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        adView.setListener(this);

        boolean showCloseButton = tagContext.isShowBannerCloseButton();

        if (parameters.close_button != null) {
            if ("true".equalsIgnoreCase(parameters.close_button)) {
                showCloseButton = true;
            } else if ("false".equalsIgnoreCase(parameters.close_button)) {
                showCloseButton = false;
            }
        }

        adView.showCloseButton(showCloseButton);

        return adView;
    }

    @NonNull
    private FrameLayout createContainerView(@NonNull final Context context, @NonNull final TagContext tagContext,
      @NonNull final AdView adView) {
        float slotWidth = tagContext.getWidthInDips();
        float slotHeight = tagContext.getHeightInDips();

        AdSize adSize = AdSize.findAdSizeThatFits(slotWidth, slotHeight);

        LOGGER.info("Slot size is: " + slotWidth + "x" + slotHeight);
        LOGGER.info("Calculated best size for slot: " + adSize.toString());

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float width = Utils.dipsToPixels(adSize.getWidth(), dm);
        float height = Utils.dipsToPixels(adSize.getHeight(), dm);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                Math.round(width), Math.round(height));
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        FrameLayout containerView = new FrameLayout(context);
        containerView.setLayoutParams(layoutParams);
        containerView.addView(adView);

        return containerView;
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

        if (mContainerView != null) {
            mContainerView.removeAllViews();
            mContainerView = null;
        }

        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }

    @Override
    public void onNotResponding() {
        LOGGER.info("onNotResponding");

        if (mListener != null) {
            mListener.onFailedToReceiveAd(mContainerView);
        }
    }

    @Override
    public void onShowAd() {
    }

    @Override
    public void onReceiveAd(@NonNull AdView view) {
        LOGGER.info("onReceiveAd");

        if (mListener != null && mContainerView != null) {
            mListener.onReceiveAd(mContainerView);
        }
    }

    @Override
    public void onShowAd(@NonNull AdView view) {
        LOGGER.info("onShowAd");

        if (mListener != null && mContainerView != null) {
            mListener.onShowAd(mContainerView);
        }
    }

    @Override
    public void onFailedToReceiveAd(AdView view, String errorMessage) {
        LOGGER.info("onFailedToReceiveAd: " + errorMessage);

        if (mListener != null) {
            mListener.onFailedToReceiveAd(mContainerView);
        }
    }

    @Override
    public void onPresentScreen(@NonNull AdView view) {
        LOGGER.info("onPresentScreen");

        if (mListener != null && mContainerView != null) {
            mListener.onPresentScreen(mContainerView);
        }
    }

    @Override
    public void onDismissScreen(@NonNull AdView view) {
        LOGGER.info("onDismissScreen");

        if (mListener != null && mContainerView != null) {
            mListener.onDismissScreen(mContainerView);
        }
    }

    @Override
    public void onLeaveApplication(@NonNull AdView view) {
        LOGGER.info("onLeaveApplication");

        if (mListener != null && mContainerView != null) {
            mListener.onLeaveApplication(mContainerView);
        }
    }

    @Override
    public void onClick() {
        LOGGER.info("onClick");

        if (mListener != null && mContainerView != null) {
            mListener.onClick(mContainerView);
        }
    }

    @Override
    public void shouldOpenURL(@NonNull AdView view, @NonNull String url, @NonNull Function<Boolean, Void> callback) {
        if (mListener != null && mContainerView != null) {
            callback.apply(mListener.shouldOpenURL(mContainerView, url));
        } else {
            callback.apply(true);
        }
    }

    @Override
    public void onClose() {
        LOGGER.info("onClose");

        if (mListener != null && mContainerView != null) {
            mListener.onClose(mContainerView);
        }
    }
}
