/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.revjet.android.sdk.InterstitialAdapter;
import com.revjet.android.sdk.InterstitialAdapterListener;
import com.revjet.android.sdk.TagContext;
import com.revjet.android.sdk.ads.AbstractAdActivity;
import com.revjet.android.sdk.ads.CustomWebChromeClient;
import com.revjet.android.sdk.exceptions.AdapterException;

import java.lang.reflect.Method;
import java.util.Set;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public abstract class AbstractInterstitialAdapter<T> extends BroadcastReceiver implements InterstitialAdapter<T> {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    @Nullable private TagContext mTagContext;
    @Nullable private InterstitialAdapterListener mListener;
    @Nullable private T mParameters;
    @Nullable private BroadcastReceiver mReceiver;

    @Override
    public void getInterstitialAd(
        @Nullable final InterstitialAdapterListener listener,
        @NonNull final TagContext tagContext,
        @NonNull final T parameters)
        throws AdapterException {
        LOGGER.info("getInterstitialAd");

        mListener = listener;
        mTagContext = tagContext;
        mParameters = parameters;
    }

    protected void registerBroadcastReceiver(
      @NonNull final Context context,
      @NonNull final BroadcastReceiver receiver,
      @NonNull final String category) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AbstractAdActivity.ACTION_CLICK);
        filter.addAction(AbstractAdActivity.ACTION_SHOW_AD);
        filter.addAction(AbstractAdActivity.ACTION_PRESENT_SCREEN);
        filter.addAction(AbstractAdActivity.ACTION_DISMISS_SCREEN);
        filter.addAction(AbstractAdActivity.ACTION_LEAVE_APPLICATION);
        filter.addAction(AbstractAdActivity.ACTION_SHOULD_OPEN_URL);
        filter.addCategory(category);

        context.registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy");

        if (mListener != null && mTagContext != null) {
            Context context = mTagContext.getContext();
            if (context != null && mReceiver != null) {
                context.unregisterReceiver(mReceiver);
                mReceiver = null;
            }

            mListener = null;
            mTagContext = null;
            mParameters = null;
        }
    }

    @Override
    public void onNotResponding() {
        LOGGER.info("onNotResponding");

        if (mListener != null) {
            mListener.onFailedToReceiveInterstitialAd(null);
        }
    }

    @Override
    public void onShowAd() {
    }

    protected void onReceive(Context context, Intent intent, String category) {
        if (intent != null) {
            String action = intent.getAction();
            Set<String> categories = intent.getCategories();

            if (action != null && categories.contains(category)) {
                if (AbstractAdActivity.BROADCAST_ACTIONS.containsKey(action)) {
                    invokeActionDelayed(action, mListener);
                } else if (AbstractAdActivity.ACTION_SHOULD_OPEN_URL.equals(action) && mListener != null) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        String clickUrl = extras.getString(AbstractAdActivity.EXTRA_CLICK_URL);
                        String shouldOpenUrl = Boolean.toString(clickUrl == null ||
                            mListener.shouldOpenURLInterstitial((Object) null, clickUrl));
                        setResultData(shouldOpenUrl);
                    }
                }
            }
        }
    }

    protected void invokeActionDelayed(
      @NonNull final String action,
      @Nullable final InterstitialAdapterListener listener) {
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    invokeAction(action, listener);
                }
            }
        }, 100);
    }

    protected void invokeAction(@NonNull final String action, @NonNull final InterstitialAdapterListener listener) {
        LOGGER.info("invokeAction: " + action);

        if (AbstractAdActivity.ACTION_DISMISS_SCREEN.equals(action)) {
            if (mTagContext != null) {
                Context context = mTagContext.getContext();
                if (context != null && mReceiver != null) {
                    context.unregisterReceiver(mReceiver);
                    mReceiver = null;
                }
            }
        }

        try {
            Method method = listener.getClass().getMethod(
                    AbstractAdActivity.BROADCAST_ACTIONS.get(action), Object.class);
            method.invoke(listener, (Object) null);
        } catch (Exception e) {
            LOGGER.warning("Error running action: " + action + " (" + e.getMessage() + ")");
        }
    }

    protected void preloadHtml(@NonNull final String content) {
        if ("nobid".equalsIgnoreCase(content)) {
            if (mListener != null) {
                mListener.onFailedToReceiveInterstitialAd(null);
            }
        } else if (mTagContext != null) {
            Context context = mTagContext.getContext();
            String baseUrl = mTagContext.getTagUrl();
            if (baseUrl != null && context != null) {
                preloadHtml(baseUrl, content, mListener, mTagContext.getContext());
            }
        }
    }

    protected void preloadHtml(
      @NonNull final String baseUrl,
      @NonNull final String content,
      @Nullable final InterstitialAdapterListener listener,
      @NonNull final Context context) {
        WebView webView = new WebView(context);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new CustomWebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            private boolean mPageFinished = false;
            private boolean mReceivedError = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                LOGGER.info("onPageFinished (preloadHtml)");

                mPageFinished = true;
                if (!mReceivedError) {
                    invokeActionDelayed(AbstractAdActivity.ACTION_RECEIVE_AD, listener);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                    String failingUrl) {
                LOGGER.info("onReceivedError (preloadHtml)");

                mReceivedError = true;
                if (!mPageFinished) {
                    invokeActionDelayed(AbstractAdActivity.ACTION_FAILED_TO_RECEIVE_AD, listener);
                }
            }
        });

        webView.loadDataWithBaseURL(baseUrl, content, "text/html", "UTF-8", null);
    }

    @Nullable
    protected TagContext getTagContext() {
        return mTagContext;
    }

    protected void setTagContext(@Nullable TagContext tagContext) {
        mTagContext = tagContext;
    }

    @Nullable
    protected InterstitialAdapterListener getListener() {
        return mListener;
    }

    protected void setListener(@Nullable InterstitialAdapterListener listener) {
        mListener = listener;
    }

    @Nullable
    protected T getParameters() {
        return mParameters;
    }

    protected void setParameters(@Nullable T parameters) {
        mParameters = parameters;
    }

    @Nullable
    protected BroadcastReceiver getReceiver() {
        return mReceiver;
    }

    protected void setReceiver(@Nullable final BroadcastReceiver receiver) {
        mReceiver = receiver;
    }
}
