/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;

public final class TagContext {
    private final float mWidthInDips;
    private final float mHeightInDips;

    @Nullable private final String mTagUrl;
    @Nullable private final TagType mTagType;
    @Nullable private final TagTargeting mTargeting;
    @Nullable private final Map<String, String> mAdditionalInfo;
    @NonNull private final WeakReference<Context> mContextRef;
    @Nullable private final Map<String, String> mTagQueryParams;
    @Nullable private final DeviceInfo mDeviceInfo;

    private final boolean mShowBannerCloseButton;
    private final boolean mShowInterstitialCloseButton;

    @Nullable private final IntegrationType mIntegrationType;

    public TagContext(
      @Nullable final Context context,
      @Nullable final TagType tagType,
      @Nullable final String tagUrl,
      @Nullable final Map<String, String> tagQueryParams,
      @Nullable final DeviceInfo deviceInfo,
      @Nullable final TagTargeting targeting,
      @Nullable final Map<String, String> additionalInfo,
      final boolean showBannerCloseButton,
      final boolean showInterstitialCloseButton,
      final float widthInDips,
      final float heightInDips,
      @Nullable final IntegrationType integrationType) {
        mWidthInDips = widthInDips;
        mHeightInDips = heightInDips;

        mTagUrl = tagUrl;
        mTagType = tagType;
        mTargeting = targeting;
        mAdditionalInfo = additionalInfo;
        mTagQueryParams = tagQueryParams;
        mDeviceInfo = deviceInfo;
        mContextRef = new WeakReference<>(context);

        mShowBannerCloseButton = showBannerCloseButton;
        mShowInterstitialCloseButton = showInterstitialCloseButton;
        mIntegrationType = integrationType;
    }

    @NonNull
    public static TagContext newInstance(@Nullable final TagController tagController) {
        Context context = null;
        String tagUrl = null;
        TagType tagType = null;
        TagTargeting targeting = null;
        Map<String, String> additionalInfo = null;
        Map<String, String> tagQueryParams = null;
        DeviceInfo deviceInfo = null;

        float widthInDips = 0;
        float heightInDips = 0;

        boolean showBannerCloseButton = false;
        boolean showInterstitialCloseButton = true;
        IntegrationType integrationType = null;

        if (tagController != null) {
            context = tagController.getContext();
            tagUrl = tagController.getTagUrl();
            tagType = tagController.getTagType();
            targeting = tagController.getTargeting();
            additionalInfo = tagController.getAdditionalInfo();
            tagQueryParams = tagController.getTagQueryParams();
            deviceInfo = tagController.getDeviceInfo();

            showBannerCloseButton = tagController.isShowBannerCloseButton();
            showInterstitialCloseButton = tagController.isShowInterstitialCloseButton();
            integrationType = tagController.getIntegrationType();

            TagView tagView = tagController.getTagView();
            if (tagView != null) {
                widthInDips = tagView.getWidthInDips();
                heightInDips = tagView.getHeightInDips();
            }
        }

        return new TagContext(context, tagType, tagUrl, tagQueryParams, deviceInfo, targeting, additionalInfo,
                showBannerCloseButton, showInterstitialCloseButton, widthInDips, heightInDips,
                integrationType);
    }

    @Nullable
    public Context getContext() {
        return mContextRef.get();
    }

    @Nullable
    public TagType getTagType() {
        return mTagType;
    }

    @Nullable
    public String getTagUrl() {
        return mTagUrl;
    }

    @Nullable
    public Map<String, String> getTagQueryParams() {
        return mTagQueryParams;
    }

    @Nullable
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    @Nullable
    public TagTargeting getTargeting() {
        return mTargeting;
    }

    @Nullable
    public Map<String, String> getAdditionalInfo() {
        return mAdditionalInfo;
    }

    public boolean isShowBannerCloseButton() {
        return mShowBannerCloseButton;
    }

    public boolean isShowInterstitialCloseButton() {
        return mShowInterstitialCloseButton;
    }

    public float getWidthInDips() {
        return mWidthInDips;
    }

    public float getHeightInDips() {
        return mHeightInDips;
    }

    @Nullable
    public IntegrationType getIntegrationType() {
        return mIntegrationType;
    }
}
