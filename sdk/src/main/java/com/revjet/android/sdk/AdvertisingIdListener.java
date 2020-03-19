/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

public interface AdvertisingIdListener {
    void onAdvertisingIdInfoCompleted(String advertisingId, Boolean isLimitAdTrackingEnabled);

    void onAdvertisingIdInfoFailed();
}
