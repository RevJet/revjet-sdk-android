/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import android.content.ActivityNotFoundException;
import androidx.arch.core.util.Function;
import com.revjet.android.sdk.mraid.MRAIDController;
import com.revjet.android.sdk.mraid.MRAIDView;
import com.revjet.android.sdk.mraid.MRAIDViewListener;

import java.util.Map;
import java.util.logging.Level;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDNativeOpenEvent extends MRAIDNativeEvent {

    @Override
    public void execute(final Map<String, String> parameters, final MRAIDController controller) {
        super.execute(parameters, controller);

        final String url = getStringForKey("url", parameters);
        if (null == url) {
            controller.reportError("URL cannot be null", "open");
            return;
        }

        try {
            final MRAIDView mraidView = controller.getMraidView();
            final MRAIDViewListener listener = mraidView != null ? mraidView.getListener() : null;

            controller.closeExpandedView();

            if (listener == null) {
                controller.startActivityWithUrl(url);
            } else {
                listener.shouldOpenURL(controller.getMraidView(), url, new Function<Boolean, Void>() {
                    @Override
                    public Void apply(Boolean input) {
                        if (input) {
                            controller.startActivityWithUrl(url);
                            listener.onLeaveApplication(controller.getMraidView());
                        }

                        return null;
                    }
                });
            }
        } catch (ActivityNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Activity not found for URL: " + url, e);
        }
    }
}
