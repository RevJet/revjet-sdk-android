/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import android.content.pm.ActivityInfo;

public enum MRAIDExpandOrientation {
    PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    NONE(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    private final int mActivityInfoOrientation;

    MRAIDExpandOrientation(final int activityInfoOrientation) {
        mActivityInfoOrientation = activityInfoOrientation;
    }

    public int getActivityInfoOrientation() {
        return mActivityInfoOrientation;
    }
}
