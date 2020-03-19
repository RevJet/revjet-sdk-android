/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import com.revjet.android.sdk.commons.CustomURLConnection;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.mraid.MRAIDController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDNativeStorepictureEvent extends MRAIDNativeEvent {

    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);

        String uri = getStringForKey("uri", parameters);
        if (uri != null && !uri.equals("") && Utils.isURLValid(uri)) {
            Context context = controller.getContext();
            if (Utils.isStorePictureAvailable(context)) {
                if (context instanceof Activity) {
                    showUserDialog(uri, context, controller);
                } else {
                    downloadImage(uri, context, controller);
                }
            } else {
                String error = "The device does not have SD card, or the permission is not granted.";
                controller.reportError(error, "storePicture");
                LOGGER.info(error);
            }
        } else {
            controller.reportError("Invalid URI for Store Picture event", "storePicture");
            LOGGER.info("Invalid URI for MRAID Store Picture event.");
        }
    }

    private void downloadImage(String url, final Context context, MRAIDController controller) {
        Toast.makeText(context, "Downloading image to Picture gallery...", Toast.LENGTH_SHORT).show();

        final Context currentContext = context;
        final MRAIDController mraidController = controller;
        final String downloadURL = url;
        final File pictureStoragePath = getPictureStoragePath();

        pictureStoragePath.mkdirs();

        new Thread(new Runnable() {
            private HttpURLConnection urlConnection;
            private InputStream pictureInputStream;
            private OutputStream pictureOutputStream;
            private MediaScannerConnection mediaScannerConnection;

            @Override
            public void run() {
                try {
                    URI uri = URI.create(downloadURL);
                    urlConnection = CustomURLConnection.openConnection(downloadURL);
                    pictureInputStream = urlConnection.getInputStream();

                    final String pictureFileName = getFileNameForUriAndHttpResponse(uri, urlConnection);
                    File pictureFile = new File(pictureStoragePath, pictureFileName);
                    final String pictureFileFullPath = pictureFile.toString();
                    pictureOutputStream = new FileOutputStream(pictureFile);

                    Utils.copyContent(pictureInputStream, pictureOutputStream);

                    loadPictureIntoGalleryApp(pictureFileFullPath);
                } catch (Exception exception) {
                    MRAIDController.sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String error = "Failed to download image.";
                            Toast.makeText(currentContext, error, Toast.LENGTH_SHORT).show();
                            mraidController.reportError(error, "storePicture");
                            LOGGER.info(error);
                        }
                    });
                } finally {
                    Utils.closeStream(pictureInputStream);
                    Utils.closeStream(pictureOutputStream);
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            private void loadPictureIntoGalleryApp(final String filename) {
                CustomMediaScannerConnectionClient mediaScannerConnectionClient = new CustomMediaScannerConnectionClient(filename, null);
                mediaScannerConnection = new MediaScannerConnection(context.getApplicationContext(), mediaScannerConnectionClient);
                mediaScannerConnectionClient.setMediaScannerConnection(mediaScannerConnection);
                mediaScannerConnection.connect();
            }
        }).start();
    }

    private File getPictureStoragePath() {
        return new File(Environment.getExternalStorageDirectory(), "Pictures");
    }

    private String getFileNameForUriAndHttpResponse(final URI uri, final HttpURLConnection urlConnection) {
        final String path = uri.getPath();

        if (path == null) {
            return null;
        }

        String filename = new File(path).getName();
        String contentType = urlConnection.getContentType();
        if (contentType != null) {
            String[] fields = contentType.split(";");
            for (final String field : fields) {
                String extension;
                if (field.contains("image/")) {
                    extension = "." + field.split("/")[1];
                    if (!filename.endsWith(extension)) {
                        filename += extension;
                    }
                    break;
                }
            }
        }

        return filename;
    }

    private class CustomMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {
        private final String mFilename;
        private final String mMimeType;
        private MediaScannerConnection mMediaScannerConnection;

        private CustomMediaScannerConnectionClient(String filename, String mimeType) {
            mFilename = filename;
            mMimeType = mimeType;
        }

        private void setMediaScannerConnection(MediaScannerConnection connection) {
            mMediaScannerConnection = connection;
        }

        @Override
        public void onMediaScannerConnected() {
            if (mMediaScannerConnection != null) {
                mMediaScannerConnection.scanFile(mFilename, mMimeType);
            }
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (mMediaScannerConnection != null) {
                mMediaScannerConnection.disconnect();
            }
        }
    }

    private void showUserDialog(final String uri, final Context context, final MRAIDController controller) {
        AlertDialog.Builder alertDialogDownloadImage = new AlertDialog.Builder(context);
        alertDialogDownloadImage
                .setTitle("Save Image")
                .setMessage("Save image to Picture gallery?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadImage(uri, context, controller);
                    }
                })
                .setCancelable(true)
                .show();
    }
}
