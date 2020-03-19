/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.TagController;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;
import com.revjet.android.sdk.vast.toolbar.VASTVideoToolbar;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.logging.Level;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

public class VASTVideoViewController {

    private static final long VIDEO_PROGRESS_TIMER_DELAY = 50;
    private static final int DEFAULT_SHOW_CLOSE_BUTTON_DELAY = 5 * 1000;
    private static final int MAX_SHOW_CLOSE_BUTTON_DELAY = 16 * 1000;

    private static final float FIRST_QUARTILE_PERCENTAGE = 0.25f;
    private static final float MIDPOINT_PERCENTAGE = 0.50f;
    private static final float THIRD_QUARTILE_PERCENTAGE = 0.75f;

    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;

    public interface VASTVideoViewControllerListener {
        void onSetContentView(final View contentView);
        void onStartActivity();
        void onClick();
        void shouldOpenURL(String url, Function<Boolean, Void> callback);
        void onFinish();
        void onFail();
        void onSetRequestedOrientation(final int orientation);
    }

    private final Context mContext;
    private final RelativeLayout mLayout;
    private final VideoView mVideoView;
    private final VASTVideoToolbar mToolbar;
    private final ImageView mCompanionAdImageView;
    private final VASTAdRepresentation mAdRepresentation;
    private final VASTVideoViewControllerListener mListener;

    private final Runnable mVideoProgressRunnable;
    private final Handler mVideoProgressHandler;
    private boolean shouldCheckVideoProgress;

    private int mShowCloseButtonDelay = DEFAULT_SHOW_CLOSE_BUTTON_DELAY;
    private boolean shouldShowCloseButton;

    private int mViewViewPausedPosition;
    private boolean shouldPlayVideo;

    private boolean startEventTracked;
    private boolean firstQuartileEventTracked;
    private boolean midpointEventTracked;
    private boolean thirdQuartileEventTracked;

    private int mVideoRetries;

    private final View.OnTouchListener mClickThroughListener;

    public VASTVideoViewController(Context context, final VASTAdRepresentation adRepresentation, VASTVideoViewControllerListener listener) {
        mVideoProgressHandler = new Handler();
        shouldCheckVideoProgress = false;
        mViewViewPausedPosition = -1;
        shouldPlayVideo = true;
        mVideoRetries = 0;

        mListener = listener;
        mContext = context.getApplicationContext();
        mLayout = new RelativeLayout(mContext);
        mLayout.setBackgroundColor(Color.BLACK);
        mAdRepresentation = adRepresentation;

        mClickThroughListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && shouldShowCloseButton) {
                    handleClick(
                            adRepresentation.getClickTrackingEvents(),
                            adRepresentation.getClickThrough()
                    );
                }
                return true;
            }
        };

        mVideoView = createVideoView(context);
        mVideoView.requestFocus();

        mToolbar = createToolBar(context);
        mLayout.addView(mToolbar);

        mCompanionAdImageView = createCompanionAdImageView(context);

        mVideoProgressRunnable = createVideoProgressRunnable();

        Utils.httpGetUrls(mAdRepresentation.getImpressions(), TagController.sUserAgent);
    }

    public void onCreate() {
        RelativeLayout.LayoutParams adViewLayout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        adViewLayout.addRule(RelativeLayout.CENTER_IN_PARENT);
        mLayout.addView(mVideoView, 0, adViewLayout);

        if (null != mListener) {
            mListener.onSetContentView(mLayout);
        }
        if (null != mListener) {
            mListener.onSetRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public void onPause() {
        stopProgress();
        mVideoView.pause();
        mViewViewPausedPosition = mVideoView.getCurrentPosition();
    }

    public void onResume() {
        mVideoRetries = 0;
        startProgress();

        mVideoView.seekTo(mViewViewPausedPosition);
        if (shouldPlayVideo) {
            mVideoView.start();
        }
    }

    public void onDestroy() {
        stopProgress();
        if (null != mAdRepresentation.getMediaFileUrl()) {
            File mediaFile = new File(mAdRepresentation.getMediaFileUrl());
            if (mediaFile.exists()) {
                if (!mediaFile.delete()) {
                    LOGGER.info("Did not delete media file at path: " + mAdRepresentation.getMediaFileUrl());
                }
            }
        }

        if (null != mAdRepresentation.getCompanionAdUrl()) {
            File companionAdFile = new File(mAdRepresentation.getCompanionAdUrl());
            if (companionAdFile.exists()) {
                if (!companionAdFile.delete()) {
                    LOGGER.info("Did not delete companion ad file at path: " + mAdRepresentation.getCompanionAdUrl());
                }
            }
        }
    }

    public boolean backButtonEnabled() {
        return shouldShowCloseButton;
    }

    private VideoView createVideoView(Context context) {
        VideoView videoView = new VideoView(context);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Called when media source is ready for playback
                if (mVideoView.getDuration() < MAX_SHOW_CLOSE_BUTTON_DELAY) {
                    mShowCloseButtonDelay = mVideoView.getDuration();
                }
            }
        });

        videoView.setOnTouchListener(mClickThroughListener);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopProgress();
                makeVideoInteractable();

                Utils.httpGetUrls(mAdRepresentation.getCompleteTrackingEvents(), TagController.sUserAgent);
                shouldPlayVideo = false;

                mVideoView.setVisibility(View.GONE);
                if (null != mCompanionAdImageView.getDrawable()) {
                    mCompanionAdImageView.setVisibility(View.VISIBLE);
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
                if (!retryMediaPlayer(mediaPlayer, what, extra)) {
                    stopProgress();
                    makeVideoInteractable();
                    if (null != mListener) {
                        mListener.onFail();
                    }
                    mVideoView.setVisibility(View.GONE);
                    if (null != mCompanionAdImageView.getDrawable()) {
                        mCompanionAdImageView.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });

        videoView.setVideoPath(mAdRepresentation.getMediaFileUrl());

        return videoView;
    }

    private VASTVideoToolbar createToolBar(final Context context) {
        final VASTVideoToolbar vastVideoToolbar = new VASTVideoToolbar(context);
        vastVideoToolbar.setCloseButtonOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (null != mListener) {
                        mListener.onFinish();
                    }
                }
                return true;
            }
        });
        vastVideoToolbar.setLearnMoreButtonOnTouchListener(mClickThroughListener);
        return vastVideoToolbar;
    }

    private ImageView createCompanionAdImageView(Context context) {
        RelativeLayout layout = new RelativeLayout(context);
        layout.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.BELOW, mToolbar.getId());
        mLayout.addView(layout, layoutParams);

        ImageView imageView = new ImageView(context);
        imageView.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams imageViewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layout.addView(imageView, imageViewLayoutParams);
        if (null == mAdRepresentation.getCompanionAdUrl()) {
            return imageView;
        }

        File imageFile = new File(mAdRepresentation.getCompanionAdUrl());
        if (imageFile.exists()) {
            Bitmap companionAdBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            float density = mContext.getResources().getDisplayMetrics().density;
            final int width = (int)(companionAdBitmap.getWidth() * density + 0.5);
            final int height = (int)(companionAdBitmap.getHeight() * density + 0.5);
            final int imageViewWidth = imageView.getMeasuredWidth();
            final int imageViewHeight = imageView.getMeasuredHeight();
            if (width < imageViewWidth && height < imageViewHeight) {
                imageView.getLayoutParams().width = width;
                imageView.getLayoutParams().height = height;
            }
            imageView.setImageBitmap(companionAdBitmap);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mAdRepresentation.getBestCompanionAdRepresentation() != null) {
                        handleClick(
                                mAdRepresentation.getBestCompanionAdRepresentation().getClickTrackers(),
                                mAdRepresentation.getBestCompanionAdRepresentation().getClickThroughUrl()
                        );
                    }
                }
            });
        }

        return imageView;
    }

    private Runnable createVideoProgressRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                float duration = mVideoView.getDuration();
                float position = mVideoView.getCurrentPosition();
                if (duration > 0) {
                    float percentage = position / duration;

                    if (!startEventTracked && position >= 1000) {
                        startEventTracked = true;
                        Utils.httpGetUrls(mAdRepresentation.getStartTrackingEvents(), TagController.sUserAgent);
                    }

                    if (!firstQuartileEventTracked && percentage > FIRST_QUARTILE_PERCENTAGE) {
                        firstQuartileEventTracked = true;
                        Utils.httpGetUrls(mAdRepresentation.getFirstQuartileTrackingEvents(), TagController.sUserAgent);
                    }

                    if (!midpointEventTracked && percentage > MIDPOINT_PERCENTAGE) {
                        midpointEventTracked = true;
                        Utils.httpGetUrls(mAdRepresentation.getMidpointTrackingEvents(), TagController.sUserAgent);
                    }

                    if (!thirdQuartileEventTracked && percentage > THIRD_QUARTILE_PERCENTAGE) {
                        thirdQuartileEventTracked = true;
                        Utils.httpGetUrls(mAdRepresentation.getThirdQuartileTrackingEvents(), TagController.sUserAgent);
                    }

                    if (duration >= MAX_SHOW_CLOSE_BUTTON_DELAY) {
                        mToolbar.updateCountdownToolbarElement(mShowCloseButtonDelay - mVideoView.getCurrentPosition());
                    }

                    if (!shouldShowCloseButton && mVideoView.getCurrentPosition() > mShowCloseButtonDelay) {
                        makeVideoInteractable();
                    }

                }
                mToolbar.updateDurationToolbarElement(mVideoView.getDuration() - mVideoView.getCurrentPosition());

                if (shouldCheckVideoProgress) {
                    mVideoProgressHandler.postDelayed(mVideoProgressRunnable, VIDEO_PROGRESS_TIMER_DELAY);
                }
            }
        };
    }

    private void startProgress() {
        if (!shouldCheckVideoProgress) {
            shouldCheckVideoProgress = true;
            mVideoProgressHandler.post(mVideoProgressRunnable);
        }
    }

    private void stopProgress() {
        if (shouldCheckVideoProgress) {
            shouldCheckVideoProgress = false;
            mVideoProgressHandler.removeCallbacks(mVideoProgressRunnable);
        }
    }

    private void makeVideoInteractable() {
        shouldShowCloseButton = true;
        mToolbar.makeInteractable();
    }

    private void handleClick(final List<String> clickThroughTrackers, final String clickThroughUrl) {
        Utils.httpGetUrls(clickThroughTrackers, TagController.sUserAgent);
        if (null != mListener) {
            mListener.onClick();
        }

        if (mListener == null) {
            openURL(clickThroughUrl);
        } else {
            mListener.shouldOpenURL(clickThroughUrl, new Function<Boolean, Void>() {
                @Override
                public Void apply(Boolean input) {
                    if (input) {
                        openURL(clickThroughUrl);
                    }

                    return null;
                }
            });
        }
    }

    private void openURL(@NonNull final String clickThroughUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
            if (null != mListener) {
                mListener.onStartActivity();
            }
        } catch (ActivityNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Activity not found for URL: " + clickThroughUrl, e);
        }
    }

    boolean retryMediaPlayer(final MediaPlayer mediaPlayer, final int what, final int extra) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                && what == MediaPlayer.MEDIA_ERROR_UNKNOWN
                && extra == VIDEO_VIEW_FILE_PERMISSION_ERROR
                && mVideoRetries < MAX_VIDEO_RETRIES) {

            FileInputStream inputStream = null;
            try {
                mediaPlayer.reset();
                final File file = new File(mAdRepresentation.getMediaFileUrl());
                inputStream = new FileInputStream(file);
                mediaPlayer.setDataSource(inputStream.getFD());

                mediaPlayer.prepareAsync();
                mVideoView.start();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                Utils.closeStream(inputStream);
                mVideoRetries++;
            }
        }
        return false;
    }
}
