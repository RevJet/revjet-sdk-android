/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;
import com.revjet.android.sdk.exceptions.AsyncHttpTaskCanceledException;
import com.revjet.android.sdk.exceptions.AsyncHttpTaskException;
import com.revjet.android.sdk.exceptions.AsyncHttpTaskNetworkException;
import com.revjet.android.sdk.exceptions.TagException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AsyncHttpTask extends AsyncTask<String, Void, AsyncHttpTaskResponse> {
    private static final String sDefaultCharset = "UTF-8";
    private static final int sMaxResponseLength = 640 * 1024; // 640 Kb
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    @NonNull private final WeakReference<AsyncHttpTaskListener> mListenerRef;
    private final TagContext mTagContext;
    @Nullable private final String mUserAgent;

    private AsyncHttpTask(AsyncHttpTaskListener listener, TagContext tagContext, @Nullable String userAgent) {
        mUserAgent = userAgent;
        mTagContext = tagContext;
        mListenerRef = new WeakReference<>(listener);
    }

    public static void execute(AsyncHttpTaskListener listener, TagContext tagContext, String url,
                               @Nullable String userAgent) {
        new AsyncHttpTask(listener, tagContext, userAgent).execute(url);
    }

    @Nullable
    @Override
    protected AsyncHttpTaskResponse doInBackground(@NonNull String... params) {
        String url = params[0];

        AsyncHttpTaskResponse httpResponse = null;

        try {
            httpResponse = getAsyncHttpTaskResponse(url);
        } catch (TagException e) {
            postOnAsyncHttpTaskFailed(e.getMessage(), e);
        }

        return httpResponse;
    }

    @Override
    protected void onPostExecute(AsyncHttpTaskResponse httpResponse) {
        AsyncHttpTaskListener listener = getListener();

        if (listener != null && httpResponse != null) {
            listener.onAsyncHttpTaskCompleted(httpResponse);
        }
    }

    @NonNull
    private AsyncHttpTaskResponse getAsyncHttpTaskResponse(@NonNull String url)
            throws AsyncHttpTaskException {
        HttpURLConnection urlConnection;
        try {
            urlConnection = CustomURLConnection.openConnection(url);
        } catch (IOException e) {
            throw new AsyncHttpTaskException("Can't open URL connection: " + e.getMessage(), mTagContext);
        }

        if (mUserAgent != null) {
            urlConnection.setRequestProperty("User-Agent", mUserAgent);
        }

        String responseBody;
        String contentType;
        Map<String, List<String>> responseHeaders;

        try {
            int statusCode;
            InputStream inputStream;
            try {
                statusCode = urlConnection.getResponseCode();
                inputStream = urlConnection.getInputStream();
            } catch (IllegalStateException e) {
                throw new AsyncHttpTaskException(e.getMessage(), mTagContext);
            } catch (IOException e) {
                throw new AsyncHttpTaskNetworkException(e.getMessage());
            }

            if (inputStream == null) {
                throw new AsyncHttpTaskNetworkException("Invalid response");
            }

            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new AsyncHttpTaskException(ErrorCode.BAD_STATUS_CODE, "Bad status code",
                        mTagContext);
            }

            String rawContentType = urlConnection.getContentType();
            if (rawContentType == null) {
                throw new AsyncHttpTaskException(ErrorCode.UNKNOWN_CONTENT_TYPE,
                        "Invalid content-type", mTagContext);
            }

            contentType = rawContentType.toLowerCase(Locale.US);
            responseHeaders = urlConnection.getHeaderFields();

            AsyncHttpTaskListener listener = getListener();

            try {
                StringBuilder stringBuilder = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    inputStream, sDefaultCharset), 8192);

                int character;
                do {
                    boolean shouldCancelTask = (listener != null && listener
                            .shouldCancelAsyncHttpTask());
                    if (shouldCancelTask || isCancelled()) {
                        throw new AsyncHttpTaskCanceledException();
                    }

                    if (stringBuilder.length() > sMaxResponseLength) {
                        throw new AsyncHttpTaskException(ErrorCode.NO_ERROR,
                                "The http response is too large (max is " + sMaxResponseLength
                                        + ")", mTagContext);
                    }

                    character = bufferedReader.read();
                    if (character != -1) {
                        stringBuilder.append((char) character);
                    }
                } while (character != -1);

                responseBody = stringBuilder.toString();
            } catch (IOException e) {
                throw new AsyncHttpTaskNetworkException(e.getMessage());
            }

            if (responseBody == null) {
                throw new AsyncHttpTaskException(ErrorCode.EMPTY_RESPONSE, "Empty response body",
                        mTagContext);
            }
        } finally {
            urlConnection.disconnect();
        }

        return new AsyncHttpTaskResponse(responseBody, contentType, responseHeaders);
    }

    private void postOnAsyncHttpTaskFailed(final String message, final Throwable throwable) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                AsyncHttpTaskListener listener = getListener();
                if (listener != null) {
                    listener.onAsyncHttpTaskFailed(message, throwable);
                }
            }
        });
    }

    @Nullable
    private AsyncHttpTaskListener getListener() {
        return mListenerRef.get();
    }
}
