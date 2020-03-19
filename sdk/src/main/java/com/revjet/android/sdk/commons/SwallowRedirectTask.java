/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import android.os.AsyncTask;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.revjet.android.sdk.TagController;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public final class SwallowRedirectTask extends AsyncTask<String, Void, String> {
    private static final String sCloseUrl2 = "revjet://#close";

    private final WeakReference<WebView> mWebViewRef;
    private final WeakReference<WebViewClient> mWebViewClientRef;
    private boolean mLateCloseInterstitial = false;

    public SwallowRedirectTask(WebView webView, WebViewClient webViewClient) {
        mWebViewRef = new WeakReference<>(webView);
        mWebViewClientRef = new WeakReference<>(webViewClient);
    }

    public SwallowRedirectTask(WebView webView, WebViewClient webViewClient,
            boolean lateCloseInterstitial) {
        mWebViewRef = new WeakReference<>(webView);
        mWebViewClientRef = new WeakReference<>(webViewClient);
        mLateCloseInterstitial = lateCloseInterstitial;
    }

    @Override
    protected String doInBackground(String... params) {
        final String url = params[0];

        HttpURLConnection urlConnection = null;
        String redirectLocation = null;

        try {
            urlConnection = CustomURLConnection.openConnection(url);
            urlConnection.setInstanceFollowRedirects(false);

            if (TagController.sUserAgent != null) {
                urlConnection.setRequestProperty("User-Agent", TagController.sUserAgent);
            }

            final int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
                statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
                for (String headerName : headerFields.keySet()) {
                    if (headerName != null && headerName.equalsIgnoreCase("Location")) {
                        List<String> location = headerFields.get(headerName);
                        if (location != null) {
                            redirectLocation = location.get(0);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (mLateCloseInterstitial && redirectLocation == null) {
            redirectLocation = sCloseUrl2;
        }

        return redirectLocation;
    }

    @Override
    protected void onPostExecute(String url) {
        WebView webView = getWebView();
        WebViewClient webViewClient = getWebViewClient();

        if (webViewClient != null && webView != null && url != null) {
            webViewClient.shouldOverrideUrlLoading(webView, url);
        }
    }

    private WebView getWebView() {
        return (mWebViewRef != null ? mWebViewRef.get() : null);
    }

    private WebViewClient getWebViewClient() {
        return (mWebViewClientRef != null ? mWebViewClientRef.get() : null);
    }
}
