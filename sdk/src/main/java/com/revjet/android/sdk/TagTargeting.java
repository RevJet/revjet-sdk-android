/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.commons.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TagTargeting {
    @Nullable private String mAreaCode;
    @Nullable private String mZip;
    @Nullable private String mCity;
    @Nullable private String mMetro;
    @Nullable private String mRegion;
    @Nullable private String mLatitude;
    @Nullable private String mLongitude;
    @Nullable private Gender mGender;
    private boolean mAllowAutoLocation = true;

    @Nullable
    public String getAreaCode() {
        return mAreaCode;
    }

    public void setAreaCode(@Nullable String areaCode) {
        mAreaCode = areaCode;
    }

    @Nullable
    public String getZip() {
        return mZip;
    }

    public void setZip(@Nullable String zip) {
        mZip = zip;
    }

    @Nullable
    public Gender getGender() {
        return mGender;
    }

    public void setGender(@Nullable Gender gender) {
        mGender = gender;
    }

    @Nullable
    public String getCity() {
        return mCity;
    }

    public void setCity(@Nullable String city) {
        mCity = city;
    }

    @Nullable
    public String getMetro() {
        return mMetro;
    }

    public void setMetro(@Nullable String metro) {
        mMetro = metro;
    }

    @Nullable
    public String getRegion() {
        return mRegion;
    }

    public void setRegion(@Nullable String region) {
        mRegion = region;
    }

    @Nullable
    public String getLatitude() {
        return mLatitude;
    }

    public void setLatitude(@Nullable String latitude) {
        mLatitude = latitude;
    }

    @Nullable
    public String getLongitude() {
        return mLongitude;
    }

    public void setLongitude(@Nullable String longitude) {
        mLongitude = longitude;
    }

    @Nullable
    public Location getLocationInstance() {
        Location location = null;

        if (mLatitude != null && mLongitude != null) {
            location = new Location("Current");
            location.setLatitude(Double.parseDouble(mLatitude));
            location.setLongitude(Double.parseDouble(mLongitude));
        }

        return location;
    }

    public boolean isAllowAutoLocation() {
        return mAllowAutoLocation;
    }

    public void setAllowAutoLocation(boolean allowAutoLocation) {
        mAllowAutoLocation = allowAutoLocation;
    }

    @NonNull
    public Map<String, String> toMap(@Nullable Context context) {
        Map<String, String> targetingMap = new HashMap<>();

        targetingMap.put("areacode", mAreaCode);
        targetingMap.put("zip", mZip);
        targetingMap.put("city", mCity);
        targetingMap.put("metro", mMetro);
        targetingMap.put("region", mRegion);

        if (mGender != null) {
            targetingMap.put("gender", mGender.toString().toLowerCase(Locale.US));
        }

        String latitude = mLatitude;
        String longitude = mLongitude;

        if (mAllowAutoLocation && mLatitude == null && mLongitude == null && context != null) {
            Location location = Utils.getLocation(context);
            if (location != null) {
                latitude = String.valueOf(location.getLatitude());
                longitude = String.valueOf(location.getLongitude());
            }
        }

        targetingMap.put("lat", latitude);
        targetingMap.put("long", longitude);

        return targetingMap;
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }
}
