/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.revjet.android.sdk.ads.CustomWebChromeClient;

public class MRAIDWebView extends WebView {

    public MRAIDWebView(@NonNull final Context context, @NonNull final WebViewClient webViewClient) {
        super(context);

        // Enable hardware acceleration
        // if (Build.VERSION.SDK_INT >= 11) {
        // try {
        // Method method = View.class.getMethod("setLayerType", int.class,
        // Paint.class);
        // method.invoke(this, /* View.LAYER_TYPE_HARDWARE*/ 2, null);
        // } catch (Exception ignored) { }
        // }

        try {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } catch (Exception ignored) {
        }

        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setWebViewClient(webViewClient);

        setWebChromeClient(new CustomWebChromeClient());

        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setBackgroundColor(Color.TRANSPARENT);

        WebSettings webSettings = getSettings();
        webSettings.setSupportMultipleWindows(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setSupportZoom(false);
        // webSettings.setUseWideViewPort(true);

        // Disable scrolling
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });
    }

    private boolean mWindowVisible = false;

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        LOGGER.info("onWindowVisibilityChanged: " + visibility);

        boolean visible = (visibility == VISIBLE);

        if (visible != mWindowVisible) {
            mWindowVisible = visible;
        }

        super.onWindowVisibilityChanged(visibility);
    }

    public boolean isWindowVisible() {
        return mWindowVisible;
    }
}
