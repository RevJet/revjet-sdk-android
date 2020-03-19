/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Map;

public final class AdapterMappingItem {
    @NonNull private final String mNetworkName;
    private final Type mAdapterType;
    private final Class<?> mAdapterClass;

    public enum Type {
        UNKNOWN, BANNER, INTERSTITIAL, BOTH
    }

    public AdapterMappingItem(@NonNull String networkName, Type adapterType, Class<?> adapterClass) {
        mNetworkName = networkName.toUpperCase(Locale.US);
        mAdapterType = adapterType;
        mAdapterClass = adapterClass;
    }

    public void putToAppropriateMapping(@NonNull Map<String, AdapterMappingItem> bannerMapping,
            @NonNull Map<String, AdapterMappingItem> interstitialMapping) {
        switch (mAdapterType) {
            case BANNER:
                bannerMapping.put(mNetworkName, this);
                break;

            case INTERSTITIAL:
                interstitialMapping.put(mNetworkName, this);
                break;

            case BOTH:
                bannerMapping.put(mNetworkName, this);
                interstitialMapping.put(mNetworkName, this);
                break;

            default:
                break;
        }
    }

    public String getNetworkName() {
        return mNetworkName;
    }

    public Type getAdapterType() {
        return mAdapterType;
    }

    public Class<?> getAdapterClass() {
        return mAdapterClass;
    }

    @Override
    public String toString() {
        return mAdapterClass.getName();
    }
}
