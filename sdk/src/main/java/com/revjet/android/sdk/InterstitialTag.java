/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.TagController.LoadingState;

import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public final class InterstitialTag {
    @NonNull private final TagController mTagController;

    public InterstitialTag(@NonNull final Context context) {
        mTagController = new TagController(TagType.INTERSTITIAL, context);
        mTagController.setAutoRefreshEnabled(false);
    }

    /**
     * Destroys any resources associated with this slot. Call this method when
     * the slot is no longer needed.
     */
    public void destroy() {
        LOGGER.info("Destroy");

        mTagController.destroy();
    }

    /**
     * You must call this method before loading or fetching an ad.
     */
    public void setTagUrl(@Nullable String tagUrl) {
        mTagController.setTagUrl(tagUrl);
    }

    @Nullable
    public String getTagUrl() {
        return mTagController.getTagUrl();
    }

    /**
     * Load and display a new ad.
     */
    public synchronized void loadAd() {
        mTagController.loadTag(true);
    }

    /**
     * Fetch a new ad. You need to call {@link #showAd()} method in order to
     * display the fetched ad.
     * <p>
     * To determine whether an ad is successfully loaded use
     * {@link TagListener} method:
     * {@link TagListener#onReceiveInterstitialAd(InterstitialAdapter, Object)}.
     *
     * @see #showAd()
     * @see #setListener(TagListener)
     */
    public synchronized void fetchAd() {
        mTagController.loadTag(false);
    }

    /**
     * Display an ad. The ad must be fetched before it can be displayed.
     *
     * @see #fetchAd()
     */
    public void showAd() {
        mTagController.showTag();
    }

    @Nullable
    public TagListener getListener() {
        return mTagController.getTagListener();
    }

    /**
     * Register a listener to be notified about various slot events.
     *
     * @see TagListener
     * @see #getListener()
     */
    public void setListener(@Nullable TagListener listener) {
        mTagController.setTagListener(listener);
    }

    @Nullable
    public TagTargeting getTargeting() {
        return mTagController.getTargeting();
    }

    /**
     * Slot targeting can improve the quality of ads and increase your revenue
     * (All fields are optional).
     */
    public void setTargeting(@Nullable TagTargeting targeting) {
        mTagController.setTargeting(targeting);
    }

    @Nullable
    public Map<String, String> getAdditionalInfo() {
        return mTagController.getAdditionalInfo();
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        mTagController.setAdditionalInfo(additionalInfo);
    }

    public boolean isAutoRefreshEnabled() {
        return mTagController.isAutoRefreshEnabled();
    }

    /**
     * Returns <code>true</code> if the ad is ready to be displayed. Makes sense
     * only in conjunction with {@link #fetchAd()} method.
     */
    public boolean isAdReady() {
        return (mTagController.getLoadingState() == LoadingState.LOADED);
    }

    public void setShowCloseButton(boolean showCloseButton) {
        mTagController.setShowCloseButton(showCloseButton);
    }

    public void setIntegrationType(@NonNull IntegrationType integrationType) {
        mTagController.setIntegrationType(integrationType);
    }
}
