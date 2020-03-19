/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import com.revjet.android.sdk.mraid.MRAIDController;

import java.util.Map;

public class MRAIDNativeSetorientationpropertiesEvent extends MRAIDNativeEvent {
    
    private static final String ALLOW_ORIENTATION_CHANGE = "allowOrientationChange";
    private static final String FORCE_ORIENTATION = "forceOrientation";
    private static final String PORTRAIT_ORIENTATION = "portrait";
    private static final String LANDSCAPE_ORIENTATION = "landscape";
    
    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);
        
        boolean allowOrientationChange = true;
        if (parameters.containsKey(ALLOW_ORIENTATION_CHANGE)) {
            allowOrientationChange = getBooleanForKey(ALLOW_ORIENTATION_CHANGE, parameters);
        }
        
        MRAIDExpandOrientation orientation = MRAIDExpandOrientation.NONE;
        if (parameters.containsKey(FORCE_ORIENTATION)) {
            String orientationString = parameters.get(FORCE_ORIENTATION);
            if (orientationString.equals(PORTRAIT_ORIENTATION)) {
                orientation = MRAIDExpandOrientation.PORTRAIT;
            } else if (orientationString.equals(LANDSCAPE_ORIENTATION)) {
                orientation = MRAIDExpandOrientation.LANDSCAPE;
            }
        }

        controller.handleSetOrientationProperties(allowOrientationChange, orientation);
    }
}
