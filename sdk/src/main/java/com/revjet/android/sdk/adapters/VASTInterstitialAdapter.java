/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.InterstitialAdapterListener;
import com.revjet.android.sdk.TagContext;
import com.revjet.android.sdk.VideoPlayerActivity;
import com.revjet.android.sdk.ads.AbstractAdActivity;
import com.revjet.android.sdk.ads.InterstitialAdActivity;
import com.revjet.android.sdk.annotations.NetworkAdapter;
import com.revjet.android.sdk.exceptions.AdapterException;
import com.revjet.android.sdk.vast.VASTRemoteFileDownloadTask;
import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;
import com.revjet.android.sdk.vast.representation.VASTCompanionAdRepresentation;
import com.revjet.android.sdk.vast.representation.VASTMediaFileRepresentation;
import com.revjet.android.sdk.vast.representation.VASTRepresentationUtilities;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

@NetworkAdapter(name = "VAST")
public class VASTInterstitialAdapter extends AbstractInterstitialAdapter<VASTParameters> {
    private VASTAdRepresentation mAdRepresentation;

    @Override
    public void getInterstitialAd(
      @Nullable final InterstitialAdapterListener listener,
      @NonNull final TagContext tagContext,
      @NonNull final VASTParameters parameters)
        throws AdapterException {
        super.getInterstitialAd(listener, tagContext, parameters);

        final Context context = tagContext.getContext();
        if (context == null) {
            onFailedToReceiveInterstitialAd(null);
            return;
        }

        mAdRepresentation = parameters.vastAdRepresentation;

        VASTMediaFileRepresentation mediaFileRepresentation =
                VASTRepresentationUtilities.chooseBestMediaFileRepresentation(
                        mAdRepresentation.getMediaFileRepresentations(), context);
        if (mediaFileRepresentation == null) {
            onFailedToReceiveInterstitialAd(null);
            return;
        }

        mAdRepresentation.setBestMediaFileRepresentation(mediaFileRepresentation);

        final VASTCompanionAdRepresentation companionAdRepresentation =
                VASTRepresentationUtilities.chooseBestCompanionAdRepresentation(
                        mAdRepresentation.getCompanionAdRepresentations(), context);

        if (companionAdRepresentation == null) {
            onFailedToReceiveInterstitialAd(null);
            return;
        }

        mAdRepresentation.setBestCompanionAdRepresentation(companionAdRepresentation);

        LOGGER.info("Downloading media file from URL: " + mediaFileRepresentation.getVideoUrl());

        new VASTRemoteFileDownloadTask(
                new VASTRemoteFileDownloadTask.VASTRemoteFileDownloadTaskListener() {
            @Override
            public void onComplete(String mediaFileUrl) {
                if (null != mediaFileUrl) {
                    LOGGER.info("Downloaded media file to path: " + mediaFileUrl);
                    mAdRepresentation.setMediaFileUrl(mediaFileUrl);
                    LOGGER.info("Downloading companion ad from URL: " + companionAdRepresentation.getImageUrl());
                    new VASTRemoteFileDownloadTask(new VASTRemoteFileDownloadTask.VASTRemoteFileDownloadTaskListener() {
                        @Override
                        public void onComplete(String imageUrl) {
                            if (null != imageUrl) {
                                LOGGER.info("Downloaded companion ad to path: " + imageUrl);
                                mAdRepresentation.setCompanionAdUrl(imageUrl);
                            } else {
                                LOGGER.info("Failed to download companion ad");
                            }
                            invokeActionDelayed(AbstractAdActivity.ACTION_RECEIVE_AD, listener);
                        }
                    }, context).execute(companionAdRepresentation.getImageUrl(),
                            VASTRemoteFileDownloadTask.CACHE_VAST_COMPANION_AD);

                } else {
                    LOGGER.info("Failed to download media file");
                    onFailedToReceiveInterstitialAd(null);
                }
            }
        }, context).execute(mediaFileRepresentation.getVideoUrl(), VASTRemoteFileDownloadTask.CACHE_VAST_MEDIA_FILE);
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy");
    }

    @Override
    public void onNotResponding() {
        LOGGER.info("onNotResponding");
    }

    private void onFailedToReceiveInterstitialAd(@Nullable Object ad) {
        InterstitialAdapterListener listener = getListener();
        if (listener != null) {
            listener.onFailedToReceiveInterstitialAd(ad);
        }
    }

    @Override
    public void showInterstitial() {
        TagContext tagContext = getTagContext();
        if (tagContext != null) {
            Context context = tagContext.getContext();
            if (context != null) {
                BroadcastReceiver receiver = getReceiver();

                if (receiver != null) {
                    context.unregisterReceiver(receiver);
                }

                if (mAdRepresentation.getMediaFileUrl() != null) {
                    setReceiver(this);
                    registerBroadcastReceiver(context, this, InterstitialAdActivity.CATEGORY_ADS);
                    VideoPlayerActivity.startVast(mAdRepresentation, context);
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onReceive(context, intent, InterstitialAdActivity.CATEGORY_ADS);
    }

    @NonNull
    @Override
    public Class<VASTParameters> getParametersClass() {
        return VASTParameters.class;
    }
}
