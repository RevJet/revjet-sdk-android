/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;

public interface Adapter<T> {
    @NonNull Class<T> getParametersClass();

    // public void onPause();

    // public void onResume();

    void onDestroy();

    void onNotResponding();

    void onShowAd();
}
