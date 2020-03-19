/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.webkit.WebView;

import com.revjet.android.sdk.commons.AsyncHttpTask;
import com.revjet.android.sdk.commons.AsyncHttpTaskListener;
import com.revjet.android.sdk.commons.AsyncHttpTaskResponse;
import com.revjet.android.sdk.commons.Timer;
import com.revjet.android.sdk.exceptions.AsyncHttpTaskCanceledException;
import com.revjet.android.sdk.exceptions.AsyncHttpTaskNetworkException;
import com.revjet.android.sdk.exceptions.TagException;
import com.revjet.android.sdk.exceptions.TagResponseException;
import com.revjet.android.sdk.exceptions.TagUncaughtExceptionHandler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TagController implements Timer.Event, AsyncHttpTaskListener {
    public static final String sSdkVersion = "1.13.1";
    public static final String sSdkRevision = "120048";
    public static final String sSdkType = "src"; // bin
    public static final String sSdkId = "RevJet SDK Version " + sSdkVersion + "-" + sSdkType + " ("
            + sSdkRevision + ")";
    public static final String sMraidVersion = "2";
    public static final int sDefaultRefreshRateInSecs = 40;
    @Nullable public static String sUserAgent = null;

    private static boolean sInitialized = false;
    private static final int sRetryLoadInSeconds = 30;

    @Nullable private WeakReference<TagView> mTagViewRef;
    @NonNull private final WeakReference<Context> mContextRef;
    @NonNull private final Timer mRefreshTimer = new Timer(this, sDefaultRefreshRateInSecs);
    @NonNull private final Timer mNotRespondingTimer = new Timer(this, Adapters.TIMEOUT_IN_SECS);

    @NonNull private final TagType mTagType;
    @NonNull private final DeviceInfo mDeviceInfo;
    private boolean mShowBannerCloseButton = false;
    private boolean mShowInterstitialCloseButton = true;

    @Nullable private String mTagUrl;
    @Nullable private TagTargeting mTargeting;
    @Nullable private Map<String, String> mAdditionalInfo;
    @Nullable private TagListener mTagListener;

    @Nullable private TagResponse mTagResponse;
    @Nullable private AdNetworkQueue mAdNetworkQueue;

    @Nullable private AdapterController mCurrentAdapterController;
    @Nullable private AdapterController mNextAdapterController;

    @Nullable private LoadingState mLoadingState;
    private boolean mAutoRefreshEnabled = false;
    private boolean mShowAdsWhenReady = true;
    private boolean mDestroyed = false;
    private boolean mPaused = false;

    @NonNull private IntegrationType mIntegrationType = IntegrationType.DIRECT;

    public enum LoadingState {
        NOT_LOADED, LOADING, LOADED, SHOWN
    }

    public TagController(@NonNull TagType tagType, @NonNull Context context) {
        mTagType = tagType;
        mDeviceInfo = new DeviceInfo(context);
        mContextRef = new WeakReference<>(context);

        setLoadingState(LoadingState.NOT_LOADED);
        initialize();
    }

    public void setTagView(@NonNull TagView tagView) {
        mTagViewRef = new WeakReference<>(tagView);
    }

    public synchronized void destroy() {
        mRefreshTimer.stop();
        mNotRespondingTimer.stop();

        mDestroyed = true;
        mTagResponse = null;
        mAdNetworkQueue = null;
        mAutoRefreshEnabled = false;

        destroyNextAdapterController();
        destroyCurrentAdapterController();

        mTagListener = null;
    }

    public synchronized void pauseTag() {
        if (!mPaused) {
            pauseAutoRefreshTimer();
            if (mLoadingState == LoadingState.LOADING) {
                mNotRespondingTimer.pause();
            }
            mPaused = true;
        }
    }

    public synchronized void resumeTag() {
        if (mPaused) {
            mPaused = false;
            startAutoRefreshTimer(false);
            if (mLoadingState == LoadingState.LOADING) {
                mNotRespondingTimer.start(false);
            }
        }
    }

    public synchronized void loadTag(boolean showAdsWhenReady) {
        if (mLoadingState == LoadingState.LOADING) {
            LOGGER.warning("Already requesting an Ad");
            return;
        }

        if (mPaused) {
            LOGGER.warning("The tag is paused");
            return;
        }

        setLoadingState(LoadingState.LOADING);
        mRefreshTimer.stop();

        mAdNetworkQueue = null;
        mShowAdsWhenReady = showAdsWhenReady;

        try {
            if (mTagType != TagType.INTERSTITIAL && getTagView() == null) {
                throw new TagException(ErrorCode.NO_ERROR, "Tag view can't be null",
                        TagContext.newInstance(this));
            }

            if (mTagUrl == null) {
                throw new TagException(ErrorCode.NO_TAG,
                        "You must call the setTagUrl method before loading an ad",
                        TagContext.newInstance(this));
            }

            mDeviceInfo.setOnAdvertisingIdInfoCompleted(new Runnable() {
                @Override
                public void run() {
                    loadTag();
                }
            });
        } catch (TagException e) {
            onAsyncHttpTaskFailed(e.getMessage(), e);
        }
    }

    private void loadTag() {
        String tagUrl = getTagUrl();
        LOGGER.info("Tag URL = " + tagUrl);
        AsyncHttpTask.execute(this, TagContext.newInstance(this), tagUrl, sUserAgent);
    }

    public synchronized void showTag() {
        if (!mPaused && !mShowAdsWhenReady) {
            if (mLoadingState == LoadingState.SHOWN) {
                LOGGER.warning("Ad is already shown");
            } else if (mLoadingState != LoadingState.LOADED) {
                LOGGER.warning("Ad is not loaded yet");
            } else if (mCurrentAdapterController != null) {
                mCurrentAdapterController.showAd();
            }
        }
    }

    public void enqueueNextNetwork() {
        if (mAdNetworkQueue != null) {
            mAdNetworkQueue.enqueueNextNetwork();
        }
    }

    public boolean isNextNetworkAvailable() {
        boolean nextNetworkAvailable = false;

        if (mAdNetworkQueue != null) {
            nextNetworkAvailable = mAdNetworkQueue.isNextNetworkAvailable();
        }

        return nextNetworkAvailable;
    }

    @Override
    public void onTimerEvent(Timer timer, boolean forceRefresh) {
        if (!mPaused) {
            AdapterController adapterController = (mNextAdapterController != null) ? mNextAdapterController
                    : mCurrentAdapterController;

            boolean isNotResponding = (timer == mNotRespondingTimer && adapterController != null
                    && mLoadingState == LoadingState.LOADING);

            boolean refreshEnabled = (forceRefresh || mAutoRefreshEnabled);

            if (timer == mRefreshTimer && refreshEnabled) {
                LOGGER.info("Reloading an Ad...");
                loadTag(mShowAdsWhenReady);
            } else if (isNotResponding) {
                LOGGER.warning("Current adapter is not responding, destroying...");
                adapterController.onNotResponding();
            }
        }
    }

    @Override
    public void onAsyncHttpTaskCompleted(AsyncHttpTaskResponse httpResponse) {
        if (mTagType == TagType.INTERSTITIAL || getTagView() != null) {
            try {
                final TagResponse tagResponse = AbstractTagResponse.newInstance(TagContext.newInstance(this),
                        httpResponse);
                final TagController controller = this;

                tagResponse.parse(new TagResponse.TagResponseParseListener() {
                    @Override
                    public void onParse(int response) {
                        if (response == TagResponse.TAG_RESPONSE_RESULT_FAIL) {
                            TagResponseException e = new TagResponseException(ErrorCode.NETWORK_INFO_INVALID,
                                    "Network Info Invalid");
                            onAsyncHttpTaskFailed(e.getMessage(), e);
                        } else {
                            mTagResponse = tagResponse;

                            mAdNetworkQueue = new AdNetworkQueue(mTagResponse.getNetworks(), controller);
                            mAdNetworkQueue.enqueueNextNetwork();
                        }
                    }
                });
            } catch (TagResponseException e) {
                onAsyncHttpTaskFailed(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onAsyncHttpTaskFailed(String message, Throwable throwable) {
        // Do not report about following exceptions
        if (throwable instanceof AsyncHttpTaskCanceledException
                || throwable instanceof AsyncHttpTaskNetworkException) {
            LOGGER.log(Level.WARNING, message);
        } else {
            LOGGER.log(Level.SEVERE, message, throwable);
        }

        destroyCurrentAdapterController();

        setLoadingState(LoadingState.NOT_LOADED);

        mRefreshTimer.setDelay(sRetryLoadInSeconds);
        startAutoRefreshTimer(false);

        if (mTagListener != null) {
            // TODO: onFailedToLoadInterstitialTag?
            mTagListener.onFailedToLoadTagView(getTagView());
        }
    }

    @Override
    public boolean shouldCancelAsyncHttpTask() {
        return mDestroyed;
    }

    @Nullable
    public Context getContext() {
        return mContextRef.get();
    }

    public void setTagUrl(@Nullable String tagUrl) {
        mTagUrl = tagUrl;
    }

    @NonNull
    public Map<String, String> getTagQueryParams() {
        return new HashMap<>();
    }

    public String getTagUrl() {
        return mTagUrl;
    }

    @Nullable
    public TagTargeting getTargeting() {
        return mTargeting;
    }

    public void setTargeting(@Nullable TagTargeting targeting) {
        mTargeting = targeting;
    }

    @Nullable
    public Map<String, String> getAdditionalInfo() {
        return mAdditionalInfo;
    }

    public void setAdditionalInfo(@Nullable Map<String, String> additionalInfo) {
        mAdditionalInfo = additionalInfo;
    }

    @Nullable
    public TagListener getTagListener() {
        return mTagListener;
    }

    public void setTagListener(@Nullable TagListener listener) {
        mTagListener = listener;
    }

    @NonNull
    public TagType getTagType() {
        return mTagType;
    }

    @NonNull
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    @Nullable
    public TagView getTagView() {
        return (mTagViewRef != null ? mTagViewRef.get() : null);
    }

    @Nullable
    public LoadingState getLoadingState() {
        return mLoadingState;
    }

    public void setLoadingState(@Nullable LoadingState loadingState) {
        if (loadingState == LoadingState.LOADING) {
            startNotRespondingTimer();
        } else {
            mNotRespondingTimer.stop();
        }

        mLoadingState = loadingState;
    }

    @Nullable
    public AdapterController getCurrentAdapterController() {
        return mCurrentAdapterController;
    }

    public void setCurrentAdapterController(@Nullable AdapterController adapterController) {
        mCurrentAdapterController = adapterController;
    }

    public void destroyCurrentAdapterController() {
        if (mCurrentAdapterController != null) {
            mCurrentAdapterController.onDestroy();
            mCurrentAdapterController = null;
        }
    }

    @Nullable
    public AdapterController getNextAdapterController() {
        return mNextAdapterController;
    }

    public void setNextAdapterController(@Nullable AdapterController adapterController) {
        mNextAdapterController = adapterController;
    }

    public void destroyNextAdapterController() {
        if (mNextAdapterController != null) {
            mNextAdapterController.onDestroy();
            mNextAdapterController = null;
        }
    }

    @Nullable
    public TagResponse getTagResponse() {
        return mTagResponse;
    }

    public boolean isAutoRefreshEnabled() {
        return mAutoRefreshEnabled;
    }

    public void startAutoRefreshTimer(boolean forceRefresh) {
        boolean refreshEnabled = (forceRefresh || mAutoRefreshEnabled);
        if (!mPaused && refreshEnabled) {
            mRefreshTimer.start(forceRefresh);
        }
    }

    public void pauseAutoRefreshTimer() {
        if (mAutoRefreshEnabled) {
            mRefreshTimer.pause();
        }
    }

    @NonNull
    public Timer getRefreshTimer() {
        return mRefreshTimer;
    }

    private void startNotRespondingTimer() {
        if (!mPaused) {
            mNotRespondingTimer.start(false);
        }
    }

    synchronized private void initialize() {
        if (!sInitialized) {
            sInitialized = true;

            LOGGER.info(sSdkId);

            if (TagUncaughtExceptionHandler.ENABLED) {
                Thread.setDefaultUncaughtExceptionHandler(new TagUncaughtExceptionHandler());
            }

            Context context = getContext();
            if (context != null) {
                WebView webView = new WebView(context.getApplicationContext());
                sUserAgent = webView.getSettings().getUserAgentString();
                webView.destroy();
                webView = null;
            }
        }
    }

    public synchronized void setAutoRefreshEnabled(boolean autoRefreshEnabled) {
        mAutoRefreshEnabled = autoRefreshEnabled;

        if (autoRefreshEnabled) {
            startAutoRefreshTimer(false);
        } else {
            mRefreshTimer.stop();
        }
    }

    public boolean isShowAdsWhenReady() {
        return mShowAdsWhenReady;
    }

    public void setShowCloseButton(boolean showCloseButton) {
        mShowBannerCloseButton = showCloseButton;
        mShowInterstitialCloseButton = showCloseButton;
    }

    public boolean isShowBannerCloseButton() {
        return mShowBannerCloseButton;
    }

    public boolean isShowInterstitialCloseButton() {
        return mShowInterstitialCloseButton;
    }

    public void setIntegrationType(@NonNull IntegrationType integrationType) {
        mIntegrationType = integrationType;
    }

    @NonNull
    public IntegrationType getIntegrationType() {
        return mIntegrationType;
    }
}
