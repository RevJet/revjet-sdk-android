/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.AdapterException;

import java.lang.reflect.Constructor;
import java.util.Map;

public final class Adapters {
    public static final int TIMEOUT_IN_SECS = 5 * 60;

    private Adapters() {
        /* empty */
    }

    public static Adapter<?> createAdapterInstance(@NonNull final AdNetwork adNetwork,
      @Nullable final AdapterMapping adapterMapping)
        throws AdapterException {
        Class<?> adapterClass = null;
        String networkType = adNetwork.getNetworkType();

        if (adapterMapping != null) {
            Map<String, AdapterMappingItem> mapping = adapterMapping.getAdapterMapping(adNetwork
                    .getAdType());
            AdapterMappingItem mappingItem = mapping.get(networkType);
            if (mappingItem != null) {
                adapterClass = mappingItem.getAdapterClass();
            }
        }

        if (adapterClass == null) {
            throw new AdapterException("Adapter class for network '" + networkType + "' not found");
        }

        try {
            Constructor<?> constructor = adapterClass.getConstructor();
            return (Adapter<?>) constructor.newInstance();
        } catch (Exception e) {
            throw new AdapterException(e.getMessage());
        }
    }
}
