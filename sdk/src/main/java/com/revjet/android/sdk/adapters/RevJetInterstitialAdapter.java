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
import com.revjet.android.sdk.ads.InterstitialAdActivity;
import com.revjet.android.sdk.annotations.NetworkAdapter;
import com.revjet.android.sdk.exceptions.AdapterException;

@NetworkAdapter(name = "RJ")
public class RevJetInterstitialAdapter extends AbstractInterstitialAdapter<RevJetParameters> {
    @Override
    public void getInterstitialAd(
      @Nullable final InterstitialAdapterListener listener,
      @NonNull final TagContext tagContext,
      @NonNull final RevJetParameters parameters)
        throws AdapterException {

        super.getInterstitialAd(listener, tagContext, parameters);
        preloadHtml(parameters.content);
    }

    @NonNull
    @Override
    public Class<RevJetParameters> getParametersClass() {
        return RevJetParameters.class;
    }

    @Override
    public void showInterstitial() {
        TagContext tagContext = getTagContext();
        if (tagContext == null) {
            return;
        }

        RevJetParameters parameters = getParameters();
        Context context = tagContext.getContext();
        if (context == null || parameters == null) {
            return;
        }

        BroadcastReceiver receiver = getReceiver();
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }

        setReceiver(this);
        registerBroadcastReceiver(context, this, InterstitialAdActivity.CATEGORY_ADS);

        boolean showCloseButton = tagContext.isShowInterstitialCloseButton();
        boolean isLandscape = (Integer.valueOf(parameters.width) > Integer.valueOf(parameters.height));

        if (parameters.close_button != null) {
            if ("true".equalsIgnoreCase(parameters.close_button)) {
                showCloseButton = true;
            } else if ("false".equalsIgnoreCase(parameters.close_button)) {
                showCloseButton = false;
            }
        }

        String baseUrl = tagContext.getTagUrl();
        if (baseUrl != null) {
            InterstitialAdActivity.show(context, baseUrl, parameters.content, showCloseButton,
                isLandscape, parameters.width, parameters.height);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onReceive(context, intent, InterstitialAdActivity.CATEGORY_ADS);
    }
}
