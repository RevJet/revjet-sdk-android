/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.ads.AbstractAdActivity;
import com.revjet.android.sdk.ads.InterstitialAdActivity;
import com.revjet.android.sdk.vast.VASTVideoViewController;
import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;

import java.io.Serializable;

public class VideoPlayerActivity extends AbstractAdActivity {
    public static final String VAST_AD_REPRESENTATION = "VAST_AD_REPRESENTATION";

    private VASTVideoViewController mViewController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onEvent(ACTION_PRESENT_SCREEN);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Serializable serializable = getIntent().getExtras().getSerializable(VAST_AD_REPRESENTATION);
        VASTAdRepresentation adRepresentation = null;
        if (serializable != null && serializable instanceof VASTAdRepresentation) {
            adRepresentation = (VASTAdRepresentation) serializable;
        } else {
            throw new IllegalStateException("VASTAdRepresentation is invalid");
        }

        mViewController = new VASTVideoViewController(this, adRepresentation, new VASTVideoViewController.VASTVideoViewControllerListener() {
            @Override
            public void onSetContentView(View contentView) {
                setContentView(contentView);
            }

            @Override
            public void onStartActivity() {
                onEvent(ACTION_LEAVE_APPLICATION);
                finish();
            }

            @Override
            public void onClick() {
                onEvent(ACTION_CLICK);
            }

            @Override
            public void shouldOpenURL(String url, Function<Boolean, Void> callback) {
                VideoPlayerActivity.this.shouldOpenURL(InterstitialAdActivity.CATEGORY_ADS, url, callback);
            }

            @Override
            public void onFinish() {
                finish();
            }

            @Override
            public void onFail() {
                onEvent(ACTION_FAILED_TO_RECEIVE_AD);
            }

            @Override
            public void onSetRequestedOrientation(int orientation) {
                setRequestedOrientation(orientation);
            }
        });

        mViewController.onCreate();
    }

    @Override
    protected void onResume() {
        if (null != mViewController) {
            mViewController.onResume();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (null != mViewController) {
            mViewController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (null != mViewController) {
            mViewController.onDestroy();
        }

        onEvent(ACTION_DISMISS_SCREEN);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mViewController != null && mViewController.backButtonEnabled()) {
            super.onBackPressed();
        }
    }

    public static void startVast(VASTAdRepresentation adRepresentation, Context context) {
        Intent videoPlayerIntent = new Intent(context, VideoPlayerActivity.class);
        videoPlayerIntent.putExtra(VAST_AD_REPRESENTATION, adRepresentation);
        context.startActivity(videoPlayerIntent);
    }

    private void onEvent(String action) {
        onEvent(action, InterstitialAdActivity.CATEGORY_ADS);
    }
}
