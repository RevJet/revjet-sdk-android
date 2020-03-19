/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.revjet.android.sdk.TagController.LoadingState;
import com.revjet.android.sdk.exceptions.AdapterException;

public final class InterstitialAdapterController extends AbstractAdapterController implements
        InterstitialAdapterListener {
    public InterstitialAdapterController(@NonNull TagController tagController, @NonNull AdNetwork adNetwork) {
        super(tagController, adNetwork);
    }

    @UiThread
    @Override
    public void onReceiveInterstitialAd(@NonNull final Object ad) {
        TagController tagController = getTagController();
        TagView tagView = tagController.getTagView();
        if (tagView != null) {
            tagView.cleanup();
        }

        tagController.setLoadingState(LoadingState.LOADED);

        tagController.destroyCurrentAdapterController();
        tagController.setCurrentAdapterController(tagController.getNextAdapterController());
        tagController.setNextAdapterController(null);

        if (tagController.isShowAdsWhenReady()) {
            showAd();
        }

        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onReceiveInterstitialAd(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onShowInterstitialAd(@NonNull Object ad) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onShowInterstitialAd(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onFailedToReceiveInterstitialAd(@Nullable Object ad) {
        TagController tagController = getTagController();
        boolean nextAdapterAvailable = tagController.isNextNetworkAvailable();

        onFailedToReceive();

        TagListener listener = tagController.getTagListener();
        if (listener != null && !nextAdapterAvailable) {
            listener.onFailedToReceiveInterstitialAd(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onPresentInterstitialScreen(@NonNull final Object ad) {
        TagController tagController = getTagController();
        tagController.pauseAutoRefreshTimer();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onPresentInterstitialScreen(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onDismissInterstitialScreen(@NonNull final Object ad) {
        TagController tagController = getTagController();
        tagController.startAutoRefreshTimer(false);
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onDismissInterstitialScreen(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onLeaveApplicationInterstitial(@NonNull final Object ad) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onLeaveApplicationInterstitial(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public void onClickInterstitialAd(@NonNull final Object ad) {
        TagController tagController = getTagController();
        AdNetworkController adNetworkController = getAdNetworkController();
        adNetworkController.trackClick();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onClickInterstitialAd(getInterstitialAdapter(), ad);
        }
    }

    @UiThread
    @Override
    public boolean shouldOpenURLInterstitial(@NonNull Object ad, @NonNull String url) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            return listener.shouldOpenURLInterstitial(getInterstitialAdapter(), ad, url);
        }

        return true;
    }

    @UiThread
    @Override
    public void onDestroyCustomEventAdapter(@NonNull String method) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onDestroyCustomEventInterstitialAdapter(getInterstitialAdapter(), method);
        }
    }

    @Override
    public void requestAd() throws AdapterException {
        InterstitialAdapter<?> adapter = getInterstitialAdapter();
        if (adapter != null) {
            requestInterstitialAd(adapter);
        }
    }

    @Override
    public void showAd() {
        InterstitialAdapter<?> adapter = getInterstitialAdapter();
        if (adapter != null) {
            super.showAd();
            adapter.showInterstitial();
        }
    }

    @Override
    public void onRequestAdFailed() {
        onFailedToReceiveInterstitialAd(null);
    }

    private <T> void requestInterstitialAd(@NonNull InterstitialAdapter<T> adapter) throws AdapterException {
        TagController tagController = getTagController();
        TagContext tagContext = TagContext.newInstance(tagController);

        AdNetworkController adNetworkController = getAdNetworkController();
        T parameters = adNetworkController.mapParameters(adapter.getParametersClass(), tagController);
        adapter.getInterstitialAd(this, tagContext, parameters);
    }
}
