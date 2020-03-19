package com.revjet.android.sdk.commons;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CustomURLConnection {
    private static final int sHttpTimeoutMilliseconds = 30 * 1000; // 30 seconds

    @NonNull
    public static HttpURLConnection openConnection(final String url) throws IOException {
        final HttpURLConnection urlConnection =
            (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setConnectTimeout(sHttpTimeoutMilliseconds);
        urlConnection.setReadTimeout(sHttpTimeoutMilliseconds);

        return urlConnection;
    }
}
