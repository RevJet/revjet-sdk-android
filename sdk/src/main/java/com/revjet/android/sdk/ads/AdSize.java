/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import java.util.ArrayList;
import java.util.List;

public class AdSize {
    public static final AdSize SIZE_768x1024 = new AdSize(768, 1024);
    public static final AdSize SIZE_1024x768 = new AdSize(1024, 768);
    public static final AdSize SIZE_728x90 = new AdSize(728, 90);
    public static final AdSize SIZE_480x320 = new AdSize(480, 320);
    public static final AdSize SIZE_320x480 = new AdSize(320, 480);
    public static final AdSize SIZE_160x600 = new AdSize(160, 600);
    public static final AdSize SIZE_300x250 = new AdSize(300, 250);
    public static final AdSize SIZE_320x50 = new AdSize(320, 50);
    public static final AdSize SIZE_320x64 = new AdSize(320, 64);
    public static final AdSize SIZE_1366x1024 = new AdSize(1366, 1024);
    public static final AdSize SIZE_1024x1366 = new AdSize(1024, 1366);
    public static final AdSize SIZE_736x414 = new AdSize(736, 414);
    public static final AdSize SIZE_414x736 = new AdSize(414, 736);
    public static final AdSize SIZE_667x375 = new AdSize(667, 375);
    public static final AdSize SIZE_375x667 = new AdSize(375, 667);
    public static final AdSize SIZE_568x320 = new AdSize(568, 320);
    public static final AdSize SIZE_320x568 = new AdSize(320, 568);
    public static final AdSize SIZE_1280x720 = new AdSize(1280, 720);
    public static final AdSize SIZE_1920x1080 = new AdSize(1920, 1080);
    public static final AdSize SIZE_970x250 = new AdSize(970, 250);
    public static final AdSize SIZE_300x600 = new AdSize(300, 600);

    // Sorted list
    private static final List<AdSize> sAllSizes = new ArrayList<>();

    static {
        sAllSizes.add(SIZE_320x50);
        sAllSizes.add(SIZE_320x64);
        sAllSizes.add(SIZE_728x90);
        sAllSizes.add(SIZE_300x250);
        sAllSizes.add(SIZE_160x600);
        sAllSizes.add(SIZE_480x320);
        sAllSizes.add(SIZE_320x480);
        sAllSizes.add(SIZE_300x600);
        sAllSizes.add(SIZE_568x320);
        sAllSizes.add(SIZE_320x568);
        sAllSizes.add(SIZE_970x250);
        sAllSizes.add(SIZE_667x375);
        sAllSizes.add(SIZE_375x667);
        sAllSizes.add(SIZE_736x414);
        sAllSizes.add(SIZE_414x736);
        sAllSizes.add(SIZE_768x1024);
        sAllSizes.add(SIZE_1024x768);
        sAllSizes.add(SIZE_1280x720);
        sAllSizes.add(SIZE_1366x1024);
        sAllSizes.add(SIZE_1024x1366);
        sAllSizes.add(SIZE_1920x1080);
    }

    private float mWidth;
    private float mHeight;

    public AdSize(float width, float height) {
        mWidth = width;
        mHeight = height;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
        mWidth = width;
    }

    public float getHeight() {
        return mHeight;
    }

    public int getIntWidth() {
        return (int) mWidth;
    }

    public int getIntHeight() {
        return (int) mHeight;
    }

    public static AdSize findAdSizeThatFits(float width, float height) {
        for (final AdSize size : sAllSizes) {
            if (size.getWidth() >= width && size.getHeight() >= height) {
                return size;
            }
        }

        return AdSize.SIZE_1920x1080;
    }

    public String toString() {
        return Math.round(this.mWidth) + "x" + Math.round(this.mHeight);
    }
}
