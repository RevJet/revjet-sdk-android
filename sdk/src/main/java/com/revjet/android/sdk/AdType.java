/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

public enum AdType {
    BANNER, INTERSTITIAL;

    public static final AdType DEFAULT = BANNER;

    public static boolean doesNotExist(String adTypeName) {
        boolean notFound = true;

        for (final AdType adType : AdType.values()) {
            if (adType.name().equals(adTypeName)) {
                notFound = false;
                break;
            }
        }

        return notFound;
    }
}
