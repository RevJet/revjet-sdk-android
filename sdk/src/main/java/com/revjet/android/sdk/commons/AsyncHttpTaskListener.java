/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

public interface AsyncHttpTaskListener {
    void onAsyncHttpTaskCompleted(AsyncHttpTaskResponse httpResponse);

    void onAsyncHttpTaskFailed(String message, Throwable throwable);

    boolean shouldCancelAsyncHttpTask();
}
