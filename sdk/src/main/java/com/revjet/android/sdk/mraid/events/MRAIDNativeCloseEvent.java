/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import com.revjet.android.sdk.mraid.MRAIDController;

import java.util.Map;

public class MRAIDNativeCloseEvent extends MRAIDNativeEvent {

    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);

        controller.closeExpandedView();
    }
}
