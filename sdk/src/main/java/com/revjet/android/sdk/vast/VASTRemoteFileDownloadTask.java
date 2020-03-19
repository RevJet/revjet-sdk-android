/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.commons.CustomURLConnection;
import com.revjet.android.sdk.commons.Utils;

import java.io.*;
import java.net.HttpURLConnection;

public class VASTRemoteFileDownloadTask extends AsyncTask<String, Void, String> {
    public static final String CACHE_VAST_MEDIA_FILE = "revjet_cache_vast_media_file";
    public static final String CACHE_VAST_COMPANION_AD = "revjet_cache_vast_companion_ad";
    public static final String REVJET_CACHE = "revjet_cache";

    public interface VASTRemoteFileDownloadTaskListener {
        void onComplete(String url);
    }

    @Nullable private final VASTRemoteFileDownloadTaskListener mListener;
    @NonNull private final Context mContext;

    public VASTRemoteFileDownloadTask(
      @Nullable final VASTRemoteFileDownloadTaskListener listener,
      @NonNull final Context context) {
        mListener = listener;
        mContext = context;
    }

    @Nullable
    @Override
    protected String doInBackground(final String... params) {
        if (null == params || null == params[0] || null == params[1]) {
            return null;
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        String absolutePath = null;
        HttpURLConnection urlConnection = null;

        try {
            String url = params[0];

            urlConnection = CustomURLConnection.openConnection(url);

            inputStream = urlConnection.getInputStream();

            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Wrong status code from the server for url: " + url);
            }

            File dir = new File (mContext.getCacheDir().getAbsolutePath() + File.separator + REVJET_CACHE);
            dir.mkdirs();
            File file = new File(dir, params[1]);

            outputStream = new FileOutputStream(file);
            Utils.copyContent(inputStream, outputStream);

            absolutePath = file.getAbsolutePath();
        } catch (Throwable e) {
            LOGGER.warning("Failed to download video: " + e.getMessage());
        } finally {
            Utils.closeStream(inputStream);
            Utils.closeStream(outputStream);

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return absolutePath;
    }

    @Override
    protected void onCancelled() {
        onPostExecute(null);
    }

    @Override
    protected void onPostExecute(String fileUrl) {
        if (null != mListener) {
            mListener.onComplete(fileUrl);
        }
    }
}
