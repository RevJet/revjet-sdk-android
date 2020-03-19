/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.arch.core.util.Function;
import com.revjet.android.sdk.commons.SwallowRedirectTask;
import com.revjet.android.sdk.mraid.MRAIDController.MRAIDPlacementType;

import java.net.URI;
import java.util.logging.Level;

public class MRAIDView extends FrameLayout {
    private static final String sDefaultEncoding = "UTF-8";
    private static final String sDefaultMimeType = "text/html";
    private static final String sRevJetDomain = "revjet.com";

    @Nullable private MRAIDWebView mWebView;
    @Nullable private MRAIDWebView mTwoPartWebView;

    @Nullable private MRAIDViewListener mListener;
    @NonNull private final MRAIDController mMraidController;
    private boolean mDestroyed = false;

    public enum ViewState {
        LOADING,
        DEFAULT,
        EXPANDED,
        HIDDEN
    }

    public MRAIDView(
      @NonNull final Activity activity,
      final float width,
      final float height,
      @NonNull final MRAIDPlacementType placementType) {
        super(activity);

        mMraidController = new MRAIDController(activity, this);
        mMraidController.setPlacementType(placementType);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                Math.round(width), Math.round(height));
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        setLayoutParams(layoutParams);
        setBackgroundColor(Color.TRANSPARENT);
        mWebView = new MRAIDWebView(activity.getApplicationContext(),
                new CustomWebViewClient(false));
        addView(mWebView);
    }

    public void destroy() {
        if (!mDestroyed) {
            mDestroyed = true;

            mMraidController.destroy();
            mWebView = null;
            mTwoPartWebView = null;

            removeAllViews();
        }

        mListener = null;
    }

    public void loadHTML(@NonNull final String baseUrl, @NonNull final String html) {
        if (mWebView != null && !mDestroyed) {
            String htmlAd = mMraidController.prepareHtml(html);
            mWebView.loadDataWithBaseURL(baseUrl, htmlAd, sDefaultMimeType, sDefaultEncoding, null);
        }
    }

    public void evaluateJavaScriptString(@NonNull final String script) {
        LOGGER.info("evaluateJavaScriptString: " + script);

        post(new Runnable() {
            @Override
            public void run() {
                WebView view = (mTwoPartWebView == null) ? mWebView : mTwoPartWebView;
                if (view != null && !mDestroyed) {
                    view.loadUrl("javascript:" + script);
                }
            }
        });
    }

    public boolean isWindowVisible() {
        MRAIDWebView view = (mTwoPartWebView == null) ? mWebView : mTwoPartWebView;

        return view != null && !mDestroyed && view.isWindowVisible();
    }

    public void expandToSizeWithContent(@NonNull final String content, final int width, final int height) {
        if (mWebView != null && !mMraidController.isInterstitial() && !mDestroyed) {
            mWebView.setVisibility(INVISIBLE);

            mTwoPartWebView = new MRAIDWebView(getContext().getApplicationContext(),
                    new CustomWebViewClient(true));

            mMraidController.expandToSize(mTwoPartWebView, width, height);

            mTwoPartWebView.loadDataWithBaseURL(null, content, sDefaultMimeType, sDefaultEncoding,
                    null);
        }
    }

    public void setListener(@Nullable final MRAIDViewListener listener) {
        mListener = listener;
    }

    @Nullable
    public MRAIDViewListener getListener() {
        return mListener;
    }

    @Nullable
    public WebView getWebView() {
        return mWebView;
    }

    private final class CustomWebViewClient extends WebViewClient {
        private final boolean mTwoPartCreative;

        public CustomWebViewClient(boolean twoPartCreative) {
            super();
            mTwoPartCreative = twoPartCreative;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            LOGGER.info("onPageFinished");
            super.onPageFinished(view, url);

            if (!mTwoPartCreative && !mMraidController.isInterstitial()) {
                mMraidController.setDefaultExpandProperties();
            }

            mMraidController.initializeMRAIDView();
            mMraidController.setPlacementType();
            mMraidController.ready();

            if (!mTwoPartCreative) {
                MRAIDViewListener listener = getListener();
                if (listener != null) {
                    MRAIDView mraidView = mMraidController.getMraidView();
                    if (mraidView != null) {
                        listener.onReceiveAd(mraidView);
                    }
                    mMraidController.fireViewableChangeJS();
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if (!mTwoPartCreative) {
                if (mListener != null) {
                    mListener.onFailedToReceiveAd(MRAIDView.this);
                }
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            LOGGER.info(url);

            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (null != scheme) {
                if (scheme.equals("mraid")) {
                    mMraidController.callNativeEvent(URI.create(url));
                    return true;
                }
            }

            if (url.contains(sRevJetDomain)) {
                new SwallowRedirectTask(view, this).execute(url);
            } else {
                if (mListener != null) {
                    mListener.onClick(MRAIDView.this);
                }

                if (mListener == null) {
                    openURL(url);
                } else {
                    mListener.shouldOpenURL(MRAIDView.this, url, new Function<Boolean, Void>() {
                        @Override
                        public Void apply(Boolean input) {
                            if (input) {
                                openURL(url);
                            }

                            return null;
                        }
                    });
                }
            }

            return true;
        }

        private void openURL(final String url) {
            try {
                mMraidController.startActivityWithUrl(url);
                if (mListener != null) {
                    mListener.onLeaveApplication(MRAIDView.this);
                }
            } catch (ActivityNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Activity not found for URL: " + url, e);
            }
        }
    }
}
