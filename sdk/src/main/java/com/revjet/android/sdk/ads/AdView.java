/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;
import com.revjet.android.sdk.mraid.MRAIDCloseButton;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class AdView extends RelativeLayout implements View.OnClickListener {
    @Nullable
    private AdWebView mAdWebView;
    @Nullable
    private MRAIDCloseButton mCloseButton;
    @Nullable
    private AdListener mListener;

    public AdView(@NonNull final Context context) {
        super(context.getApplicationContext());

        setBackgroundColor(Color.TRANSPARENT);

        mAdWebView = new AdWebView(context, this);
        addView(mAdWebView);

        mCloseButton = MRAIDCloseButton.newInstance(context);
        mCloseButton.setVisibility(View.GONE);
        mCloseButton.setOnClickListener(this);
        addView(mCloseButton, mCloseButton.getLayout());
    }

    public void showCloseButton(boolean showCloseButton) {
        if (mCloseButton != null) {
            mCloseButton.setVisibility(showCloseButton ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onClose();
        }
    }

    public void destroy() {
        LOGGER.info("destroy");

        if (mAdWebView != null) {
            mAdWebView.destroy();
            mAdWebView = null;
        }

        removeAllViews();

        mListener = null;
        mCloseButton = null;
    }

    public void webviewDidClose() {
        if (mAdWebView != null) {
            mAdWebView.webviewDidClose();
        }
    }

    public void loadHtmlWithBaseURL(@NonNull String baseUrl, @NonNull String html) {
        if (mAdWebView != null) {
            mAdWebView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
        }
    }

    @Nullable
    public AdListener getListener() {
        return mListener;
    }

    public void setListener(@Nullable final AdListener listener) {
        mListener = listener;
    }

    @Nullable
    public AdWebView getAdWebView() {
        return mAdWebView;
    }
}
