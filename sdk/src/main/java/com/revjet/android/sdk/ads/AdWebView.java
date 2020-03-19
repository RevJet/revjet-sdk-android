/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.arch.core.util.Function;
import com.revjet.android.sdk.commons.StringUtils;
import com.revjet.android.sdk.commons.SwallowRedirectTask;

import java.util.logging.Level;

public class AdWebView extends WebView {
    public static final String WEBVIEW_DID_APPEAR = "(typeof webviewDidAppear == 'function' ? webviewDidAppear : Function)();";
    public static final String WEBVIEW_DID_CLOSE = "(typeof webviewDidClose == 'function' ? webviewDidClose : Function)();";

    private static final String sRevJetDomain = "revjet.com";
    private static final String sTestLp = "TESTLP.html";
    private static final String sCloseUrl1 = "revjet:closeInterstitialAd";
    private static final String sCloseUrl2 = "revjet://#close";
    private static final String sBlankUrl = "about:blank";

    private AdView mAdView;

    public AdWebView(Context context, AdView adView) {
        super(context.getApplicationContext());

        mAdView = adView;

        setBackgroundColor(Color.TRANSPARENT);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setWebViewClient(new AdViewClient());
        setWebChromeClient(new CustomWebChromeClient());

        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        WebSettings webSettings = getSettings();
        webSettings.setSupportMultipleWindows(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);

        // Disable scrolling
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy");

        setWebViewClient(new WebViewClient());
        mAdView = null;

        super.destroy();
    }

    private boolean mActivityStarted = false;
    private boolean mWebviewDidAppear = false;
    private boolean mPageFinished = false;
    private boolean mWindowVisible = false;

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if (visibility == VISIBLE) {
            mWindowVisible = true;
        }

        webviewDidAppear();

        if (mActivityStarted && visibility == VISIBLE) {
            mActivityStarted = false;

            AdListener listener = (mAdView != null) ? mAdView.getListener() : null;
            if (listener != null) {
                listener.onDismissScreen(mAdView);
            }
        }
    }

    private synchronized void webviewDidAppear() {
        if (!mWebviewDidAppear && mWindowVisible && mPageFinished) {
            mWebviewDidAppear = true;

            LOGGER.info("webviewDidAppear");

            evaluateJavaScriptString(WEBVIEW_DID_APPEAR, true);

            AdListener listener = (mAdView != null) ? mAdView.getListener() : null;
            if (listener != null) {
                listener.onShowAd(mAdView);
            }
        }
    }

    public void webviewDidClose() {
        LOGGER.info("webviewDidClose");

        evaluateJavaScriptString(WEBVIEW_DID_CLOSE, false);
    }

    private void evaluateJavaScriptString(String script, boolean doPost) {
        loadUrl("javascript:" + script);
    }

    private boolean mRedirectInProgress = false;
    private boolean mLateCloseInterstitial = false;

    private final class AdViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            LOGGER.fine("URL: " + url);

            final AdListener listener = (mAdView != null) ? mAdView.getListener() : null;

            if (url != null && !sBlankUrl.equalsIgnoreCase(url)) {
                boolean closeUrl = (StringUtils.containsIgnoreCase(url, sCloseUrl1) || StringUtils
                        .containsIgnoreCase(url, sCloseUrl2));
                boolean revjetUrl = (StringUtils.containsIgnoreCase(url, sRevJetDomain) && !StringUtils
                        .containsIgnoreCase(url, sTestLp));

                if (revjetUrl) {
                    mRedirectInProgress = true;
                    new SwallowRedirectTask(view, this, mLateCloseInterstitial).execute(url);
                } else if (closeUrl && listener != null) {
                    if (mRedirectInProgress && !mLateCloseInterstitial) {
                        mRedirectInProgress = false;
                        mLateCloseInterstitial = true;
                    } else {
                        listener.onClose();
                    }
                } else if (listener != null) {
                    mRedirectInProgress = false;
                    listener.onClick();

                    listener.shouldOpenURL(mAdView, url, new Function<Boolean, Void>() {
                        @Override
                        public Void apply(Boolean input) {
                            if (input) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    listener.onPresentScreen(mAdView);
                                    getContext().startActivity(intent);
                                    mActivityStarted = true;
                                    listener.onLeaveApplication(mAdView);
                                } catch (ActivityNotFoundException e) {
                                    LOGGER.log(Level.SEVERE, "Activity not found for URL: " + url, e);
                                }
                            }

                            return null;
                        }
                    });
                }
            }

            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mPageFinished = true;
            webviewDidAppear();

            AdListener listener = (mAdView != null) ? mAdView.getListener() : null;
            if (listener != null) {
                listener.onReceiveAd(mAdView);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            AdListener listener = (mAdView != null) ? mAdView.getListener() : null;
            if (listener != null) {
                listener.onFailedToReceiveAd(mAdView, description + ": " + failingUrl);
            }
        }
    }
}
