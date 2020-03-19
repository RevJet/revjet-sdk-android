/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import com.revjet.android.sdk.mraid.MRAIDController;

import java.util.Map;

public class MRAIDNativeEvent {

    public void execute(Map<String, String> parameters, MRAIDController controller) {

    }

    public int getIntForKey(String key, Map<String, String> parameters) {
        String s = parameters.get(key);
        if (s == null) return -1;
        else {
            try {
                return Integer.parseInt(s, 10);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    public String getStringForKey(String key, Map<String, String> parameters) {
        return parameters.get(key);
    }

    public boolean getBooleanForKey(String key, Map<String, String> parameters) {
        return "true".equals(parameters.get(key));
    }
}
