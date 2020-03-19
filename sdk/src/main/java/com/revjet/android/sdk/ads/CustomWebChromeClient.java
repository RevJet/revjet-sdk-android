/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class CustomWebChromeClient extends WebChromeClient {
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        LOGGER.info("WebCore (" + lineNumber + "): " + message);
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        LOGGER.info(message);

        if (result != null) {
            result.cancel();
        }

        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        LOGGER.info(message);

        if (result != null) {
            result.cancel();
        }

        return true;
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message,
            String defaultValue, JsPromptResult result) {
        LOGGER.info(message);

        if (result != null) {
            result.cancel();
        }

        return true;
    }

    @Override
    public boolean onJsBeforeUnload(WebView view, String url, String message,
            JsResult result) {
        LOGGER.info(message);

        if (result != null) {
            result.cancel();
        }

        return true;
    }
}
