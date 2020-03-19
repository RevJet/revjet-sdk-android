/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import static com.revjet.android.sdk.TagController.LoadingState.LOADING;
import static com.revjet.android.sdk.TagController.LoadingState.NOT_LOADED;
import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.AdapterException;

import java.util.List;
import java.util.logging.Level;

public final class AdNetworkQueue {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    @Nullable private final List<AdNetwork> mAdNetworks;
    @NonNull private final TagController mTagController;
    @NonNull private final AdapterMapping mAdapterMapping;

    private int mCurrentNetworkIndex = -1;

    public AdNetworkQueue(@Nullable List<AdNetwork> adNetworks, @NonNull TagController tagController) {
        mAdNetworks = adNetworks;
        mTagController = tagController;
        mAdapterMapping = AdapterMapping.getInstance();
    }

    // Prevent recursive calls of loadNextNetwork() method
    public void enqueueNextNetwork() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                loadNextNetwork();
            }
        });
    }

    public boolean isNextNetworkAvailable() {
        boolean nextNetworkAvailable = false;

        if (mAdNetworks != null) {
            int nextNetworkIndex = mCurrentNetworkIndex + 1;
            if (nextNetworkIndex >= 0) {
                nextNetworkAvailable = (nextNetworkIndex < mAdNetworks.size());
            }
        }

        return nextNetworkAvailable;
    }

    private void loadNextNetwork() {
        boolean nextNetworkAvailable = isNextNetworkAvailable()
                && !mTagController.shouldCancelAsyncHttpTask();
        mTagController.setLoadingState(nextNetworkAvailable ? LOADING : NOT_LOADED);

        AdNetwork network = null;
        try {
            if (nextNetworkAvailable) {
                mCurrentNetworkIndex++;

                network = getCurrentNetwork();
                if (network != null) {
                    mTagController.setNextAdapterController(null);
                    loadNetwork(network);
                    mTagController.getRefreshTimer().setDelay(network.getRefreshRate());
                }
            }
        } catch (AdapterException e) {
            e.setTagContext(TagContext.newInstance(mTagController));
            if (network != null) {
                e.setErrorCode(network.getAdType());
            }

            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            AdapterController nextAdapterController = mTagController.getNextAdapterController();
            if (nextAdapterController != null) {
                nextAdapterController.onRequestAdFailed();
                mTagController.setNextAdapterController(null);
            }
        }
    }

    private void loadNetwork(@NonNull AdNetwork network) throws AdapterException {
        LOGGER.info(network.toString());

        AdapterController adapterController = AbstractAdapterController.newInstance(
                mTagController, network);
        mTagController.setNextAdapterController(adapterController);

        Adapter<?> adapter = Adapters.createAdapterInstance(network, mAdapterMapping);
        adapterController.setAdapter(adapter);
        adapterController.requestAd();
    }

    @Nullable
    private AdNetwork getCurrentNetwork() {
        AdNetwork network = null;

        if (mCurrentNetworkIndex >= 0 && mAdNetworks != null) {
            network = mAdNetworks.get(mCurrentNetworkIndex);
        }

        return network;
    }
}
