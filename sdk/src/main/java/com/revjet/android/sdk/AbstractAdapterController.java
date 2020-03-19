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

import java.lang.reflect.Method;

public abstract class AbstractAdapterController implements AdapterController, AdapterListener {
    @NonNull private final AdNetwork mAdNetwork;
    @NonNull private final AdNetworkController mAdNetworkController;
    @NonNull private final TagController mTagController;

    @NonNull private final Class<?> mAdapterListenerClass;
    @Nullable private Adapter<?> mAdapter;

    public AbstractAdapterController(@NonNull TagController tagController, @NonNull AdNetwork adNetwork) {
        mAdNetwork = adNetwork;
        mAdNetworkController = adNetwork.getAdNetworkController();
        mAdapterListenerClass = (AdType.INTERSTITIAL == adNetwork.getAdType()) ? InterstitialAdapterListener.class
                : BannerAdapterListener.class;

        mTagController = tagController;
    }

    @NonNull
    protected AdNetwork getAdNetwork() {
        return mAdNetwork;
    }

    @NonNull
    protected AdNetworkController getAdNetworkController() {
        return mAdNetworkController;
    }

    @NonNull
    protected TagController getTagController() {
        return mTagController;
    }

    @Override
    public void setAdapter(Adapter<?> adapter) {
        mAdapter = adapter;
    }

    @Nullable
    protected BannerAdapter<?> getBannerAdapter() {
        return (BannerAdapter<?>) mAdapter;
    }

    @Nullable
    protected InterstitialAdapter<?> getInterstitialAdapter() {
        return (InterstitialAdapter<?>) mAdapter;
    }

    @NonNull
    public static AdapterController newInstance(@NonNull TagController tagController, @NonNull AdNetwork adNetwork) {
        AdapterController adapterController;

        if (AdType.INTERSTITIAL == adNetwork.getAdType()) {
            adapterController = new InterstitialAdapterController(tagController, adNetwork);
        } else {
            adapterController = new BannerAdapterController(tagController, adNetwork);
        }

        return adapterController;
    }

    @Override
    public void showAd() {
        mTagController.setLoadingState(LoadingState.SHOWN);
        mAdNetworkController.trackImpression();
        mTagController.startAutoRefreshTimer(false);

        if (mAdapter != null) {
            mAdapter.onShowAd();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
    }

    @Override
    public void onNotResponding() {
        if (mAdapter != null) {
            mAdapter.onNotResponding();
        }
    }

    @UiThread
    @Override
    public void onReceiveCustomMethod(@NonNull String method, @NonNull String data) throws AdapterException {
        TagListener listener = mTagController.getTagListener();
        if (listener == null) {
            throw new AdapterException("Tag listener can't be null");
        }

        try {
            Method customMethod = listener.getClass().getMethod(method, mAdapterListenerClass,
                    String.class);
            customMethod.invoke(listener, this, data);
        } catch (Exception e) {
            throw new AdapterException("Error invoking custom method: " + e.getMessage());
        }
    }

    protected void onFailedToReceive() {
        mTagController.destroyNextAdapterController();

        if (!mTagController.isNextNetworkAvailable()) {
            mTagController.destroyCurrentAdapterController();
        }

        mTagController.setLoadingState(LoadingState.NOT_LOADED);
        mAdNetworkController.trackNoBid();

        mTagController.startAutoRefreshTimer(false);
        mTagController.enqueueNextNetwork();
    }
}
