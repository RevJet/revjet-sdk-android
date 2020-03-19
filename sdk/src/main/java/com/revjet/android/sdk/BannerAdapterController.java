/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.view.View;

import com.revjet.android.sdk.TagController.LoadingState;
import com.revjet.android.sdk.exceptions.AdapterException;

public final class BannerAdapterController extends AbstractAdapterController implements
        BannerAdapterListener {
    @Nullable private View mBannerView;

    public BannerAdapterController(@NonNull TagController tagController, @NonNull AdNetwork adNetwork) {
        super(tagController, adNetwork);
    }

    @UiThread
    @Override
    public void onReceiveAd(@NonNull View view) {
        TagController tagController = getTagController();
        TagView tagView = tagController.getTagView();

        if (tagView != null) {
            tagController.setLoadingState(LoadingState.LOADED);

            tagController.destroyCurrentAdapterController();
            tagController.setCurrentAdapterController(tagController.getNextAdapterController());
            tagController.setNextAdapterController(null);

            mBannerView = view;

            if (tagController.isShowAdsWhenReady()) {
                showAd();
            }

            TagListener listener = tagController.getTagListener();
            if (listener != null) {
                listener.onReceiveAd(getBannerAdapter(), view);
            }
        } else {
            onFailedToReceiveAd(view);
        }
    }

    @UiThread
    @Override
    public void onShowAd(@NonNull View view) {
        TagController tagController = getTagController();
        TagView tagView = tagController.getTagView();

        if (tagView != null) {
            TagListener listener = tagController.getTagListener();
            if (listener != null) {
                listener.onShowAd(getBannerAdapter(), view);
            }
        }
    }

    @UiThread
    @Override
    public void onFailedToReceiveAd(@Nullable View view) {
        TagController tagController = getTagController();
        boolean nextAdapterAvailable = tagController.isNextNetworkAvailable();

        onFailedToReceive();

        TagListener listener = tagController.getTagListener();
        if (listener != null && !nextAdapterAvailable) {
            listener.onFailedToReceiveAd(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public void onPresentScreen(@NonNull View view) {
        TagController tagController = getTagController();
        tagController.pauseAutoRefreshTimer();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onPresentScreen(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public void onDismissScreen(@NonNull View view) {
        TagController tagController = getTagController();
        tagController.startAutoRefreshTimer(false);
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onDismissScreen(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public void onLeaveApplication(@NonNull View view) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onLeaveApplication(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public void onClick(@NonNull View view) {
        TagController tagController = getTagController();
        AdNetworkController adNetworkController = getAdNetworkController();
        adNetworkController.trackClick();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onClick(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public boolean shouldOpenURL(@NonNull View view, @NonNull String url) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            return listener.shouldOpenURL(getBannerAdapter(), view, url);
        }

        return true;
    }

    @UiThread
    @Override
    public void onClose(@NonNull View view) {
        TagController tagController = getTagController();
        tagController.destroyCurrentAdapterController();

        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onClose(getBannerAdapter(), view);
        }
    }

    @UiThread
    @Override
    public void onDestroyCustomEventAdapter(@NonNull String method) {
        TagController tagController = getTagController();
        TagListener listener = tagController.getTagListener();
        if (listener != null) {
            listener.onDestroyCustomEventBannerAdapter(getBannerAdapter(), method);
        }
    }

    @Override
    public void requestAd() throws AdapterException {
        BannerAdapter<?> adapter = getBannerAdapter();
        if (adapter != null) {
            requestBannerAd(adapter);
        }
    }

    @Override
    public void showAd() {
        TagController tagController = getTagController();
        TagView tagView = tagController.getTagView();
        if (tagView != null) {
            super.showAd();
            tagView.transitionToView(mBannerView, getAdNetwork().getTransitionAnimation());
        }
    }

    @Override
    public void onRequestAdFailed() {
        onFailedToReceiveAd(null);
    }

    @Override
    public void onDestroy() {
        TagController tagController = getTagController();
        TagView tagView = tagController.getTagView();
        if (tagView != null && mBannerView != null) {
            tagView.removeView(mBannerView);
        }

        mBannerView = null;

        super.onDestroy();
    }

    private <T> void requestBannerAd(@NonNull BannerAdapter<T> adapter) throws AdapterException {
        TagController tagController = getTagController();
        TagContext tagContext = TagContext.newInstance(tagController);

        AdNetworkController adNetworkController = getAdNetworkController();
        T parameters = adNetworkController.mapParameters(adapter.getParametersClass(), tagController);
        adapter.getBannerAd(this, tagContext, parameters);
    }
}
