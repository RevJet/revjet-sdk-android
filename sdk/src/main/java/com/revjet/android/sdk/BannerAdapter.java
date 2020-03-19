/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.AdapterException;

public interface BannerAdapter<T> extends Adapter<T> {
    void getBannerAd(@Nullable BannerAdapterListener listener, @NonNull TagContext tagContext, @NonNull T parameters)
            throws AdapterException;
}
