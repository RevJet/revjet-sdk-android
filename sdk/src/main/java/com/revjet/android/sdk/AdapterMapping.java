/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.annotations.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class AdapterMapping {
    private static final String sAdapterPackage = "com.revjet.android.sdk.adapters.";

    private static final String[] sAdapterClasses = {
            "CustomEventAdapter",
            "RevJetAdapter",
            "RevJetInterstitialAdapter",
            "MRAIDAdapter",
            "MRAIDInterstitialAdapter",
            "VASTInterstitialAdapter"
    };

    private static AdapterMapping sAdapterMapping = null;
    private final Map<String, AdapterMappingItem> mBannerAdapterMapping = new HashMap<>();
    private final Map<String, AdapterMappingItem> mInterstitialAdapterMapping = new HashMap<>();

    private AdapterMapping() {
        for (String className : sAdapterClasses) {
            addToAdapterMapping(mBannerAdapterMapping, mInterstitialAdapterMapping, sAdapterPackage
                    + className);
        }
    }

    @NonNull
    public static synchronized AdapterMapping getInstance() {
        if (sAdapterMapping == null) {
            sAdapterMapping = new AdapterMapping();
            LOGGER.info("Adapters found: " + sAdapterMapping);
        }

        return sAdapterMapping;
    }

    @NonNull
    public Map<String, AdapterMappingItem> getAdapterMapping(@Nullable AdType adType) {
        Map<String, AdapterMappingItem> mapping;

        if (adType == AdType.INTERSTITIAL) {
            mapping = mInterstitialAdapterMapping;
        } else {
            mapping = mBannerAdapterMapping;
        }

        return mapping;
    }

    @Override
    public String toString() {
        return "{Banner=" + mBannerAdapterMapping.toString() + ", Interstitial="
                + mInterstitialAdapterMapping.toString() + "}";
    }

    private void addToAdapterMapping(
      @NonNull final Map<String, AdapterMappingItem> bannerMapping,
      @NonNull final Map<String, AdapterMappingItem> interstitialMapping,
      @NonNull final String className) {
        AdapterMappingItem mappingItem = createAdapterMappingItem(className);
        if (mappingItem != null) {
            mappingItem.putToAppropriateMapping(bannerMapping, interstitialMapping);
        }
    }

    @Nullable
    private AdapterMappingItem createAdapterMappingItem(@NonNull String className) {
        Class<?> adapterClass = null;
        AdapterMappingItem mappingItem = null;

        try {
            adapterClass = Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        } catch (LinkageError e) {
            LOGGER.warning("Error while loading adapter: " + e.getMessage());
        }

        if (adapterClass != null) {
            NetworkAdapter annotation = adapterClass.getAnnotation(NetworkAdapter.class);
            if (annotation != null) {
                String networkName = annotation.name();
                if (networkName.length() > 0) {
                    AdapterMappingItem.Type adapterType = getAdapterType(adapterClass);
                    mappingItem = (adapterType == AdapterMappingItem.Type.UNKNOWN ? null
                            : new AdapterMappingItem(networkName, adapterType, adapterClass));
                }
            }
        }

        return mappingItem;
    }

    @NonNull
    private AdapterMappingItem.Type getAdapterType(@NonNull Class<?> adapterClass) {
        AdapterMappingItem.Type adapterType = AdapterMappingItem.Type.UNKNOWN;

        boolean isBannerAdapter = BannerAdapter.class.isAssignableFrom(adapterClass);
        boolean isInterstitialAdapter = InterstitialAdapter.class.isAssignableFrom(adapterClass);

        if (isBannerAdapter && isInterstitialAdapter) {
            adapterType = AdapterMappingItem.Type.BOTH;
        } else if (isBannerAdapter) {
            adapterType = AdapterMappingItem.Type.BANNER;
        } else if (isInterstitialAdapter) {
            adapterType = AdapterMappingItem.Type.INTERSTITIAL;
        }

        return adapterType;
    }
}
