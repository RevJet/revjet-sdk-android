/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.mraid.MRAIDController;

import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDNativePlayvideoEvent extends MRAIDNativeEvent {

    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);

        String uri = getStringForKey("uri", parameters);
        if (uri != null && !uri.equals("") && Utils.isURLValid(uri)) {
            Context context = controller.getContext();
            if (Utils.isInlineVideoAvailable(context)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(uri), "video/*");
                context.startActivity(intent);
            } else {
                String error = "Does not support MRAID play video.";
                controller.reportError(error, "playVideo");
                LOGGER.info(error);
            }
        } else {
            controller.reportError("Invalid URI for Play Video event", "playVideo");
            LOGGER.info("Invalid URI for MRAID Play Video event.");
        }
    }
}
