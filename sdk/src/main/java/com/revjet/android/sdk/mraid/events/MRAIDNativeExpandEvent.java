/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import com.revjet.android.sdk.mraid.MRAIDController;
import com.revjet.android.sdk.mraid.MRAIDController.MRAIDPlacementType;

import java.util.Map;

public class MRAIDNativeExpandEvent extends MRAIDNativeEvent {

    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);

        if (MRAIDPlacementType.INTERSTITIAL == controller.getPlacementType()) {
            controller.reportError("Can't expand interstitial ad", "expand");
            return;
        }

        int width = getIntForKey("width", parameters);
        int height = getIntForKey("height", parameters);
        String url = getStringForKey("url", parameters);
        boolean shouldUseCustomClose = getBooleanForKey("shouldUseCustomClose", parameters);

        if (width <= 0) {
            width = controller.mScreenWidth;
        }
        if (height <= 0) {
            height = controller.mScreenHeight;
        }

        controller.expandToSize(url, width, height, !shouldUseCustomClose);
    }
}
