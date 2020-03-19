/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

public enum IntegrationType {
    ADMOB("Admob_RevJetSDK"),
    DIRECT("RevJetSDK");

    private final String mName;

    private IntegrationType(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
